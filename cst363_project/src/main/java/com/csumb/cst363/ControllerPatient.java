package com.csumb.cst363;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.*;
import java.util.regex.Pattern;

import static java.sql.Types.NULL;

/**
 * Controller class for patient interactions.
 * register as a new patient.
 * update patient profile.
 */
@Controller
public class ControllerPatient {
   //string alphabetical format
	private final Pattern pattern = Pattern.compile("[^a-zA-Z\s\\d,#]");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Request blank patient registration form.
	 */
	@GetMapping("/patient/new")
	public String newPatient(Model model) {
		model.addAttribute("patient", new Patient());//add new patient
		return "patient_register";//html page
	}

	/**
	 * Process new patient registration
	 */
	@PostMapping("/patient/new")
	public String newPatient(Patient p, Model model) {
		try (Connection con = getConnection()) {
			checkInput(p);//call function to validate user input for patient
			
			//first statement for patient order 1-5 from sql table fields
			PreparedStatement ps = con.prepareStatement("insert into Patient(ssn, first_name, last_name, birthday, address_id, doctor_id) "
			      + "values(?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, p.getSsn());
			String first = p.getFirst_name();
			first = first.substring(0, 1).toUpperCase() + first.substring(1).toLowerCase();//Crystal
			ps.setString(2, first);
			ps.setString(3, p.getLast_name().toUpperCase().trim());//LASTNAME
			ps.setInt(4, Integer.parseInt(p.getBirthdate()));//birthday converted to integer for year
			
			//2nd statement for address order 1-5 from sql table fields
			PreparedStatement ps2 = con.prepareStatement("insert into Address(street, unitNo, city, state, zip) "
			      + "values(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
			ps2.setString(1, p.getStreet().toUpperCase());//STREET
			//if unit number not input is optional not required
			if (!(p.getUnitNo().isBlank())) ps2.setString(2, p.getUnitNo().toUpperCase());
			else ps2.setNull(2, NULL);
			ps2.setString(3, p.getCity().toUpperCase());//CITY
			ps2.setString(4, p.getState().toUpperCase().trim());//WV
			ps2.setInt(5, Integer.parseInt(p.getZipcode().trim()));//92586
			ps2.executeUpdate();
			
			//get result keys if zip code input error get street else throw error
			ResultSet rs = ps2.getGeneratedKeys();
			if (rs.next()) ps.setInt(5, rs.getInt(1));
			else throw new SQLException("Address problem");
			
			//3rd statement for primary physician name order 1-2 for sql table fields last and first name
			PreparedStatement ps3 = con.prepareStatement("select id,specialty from Doctor where last_name = ? and first_name = ?");
			String[] name = p.getPrimaryName().split(",");//LAST, Crystal
			first = name[1].trim();//last character in name string
			first = first.substring(0, 1).toUpperCase() + first.substring(1).toLowerCase();//Crystal
			ps3.setString(1, name[0].toUpperCase().trim());//LASTNAME 
			ps3.setString(2, first);//Crystal
			rs = ps3.executeQuery();
			
			if (rs.next()) {//if year error input for 2023-birth year
				String special = ((2023 - Integer.parseInt(p.getBirthdate())) > 12) ? "Family medicine" : "Pediatrics";
				//if physician last name not matching throw error
				if (rs.getString(2).compareTo(special) != 0) throw new SQLException("Doctor specialty not matching");
				ps.setInt(6, rs.getInt(1));//set sql field doctor_id from ps
			} 
			else throw new SQLWarning("Doctor not found");//error
			ps.executeUpdate();
			
			//get result keys for new patient added
			rs = ps.getGeneratedKeys();
			if (rs.next()) {//loop if patient added show message
				p.setPatientID(String.valueOf(rs.getInt(1)));//social security no as a string
				model.addAttribute("message", "Registration successful.");//text from html
				model.addAttribute("patient", p);//obj from html
				return "patient_show";//html page
			}
			else throw new SQLException("Couldn't add patient");//error
		} 
		catch (SQLException e) {//error
			model.addAttribute("message", e.getMessage());//text from html
			model.addAttribute("patient", p);//obj from html
			return "patient_register";//html page
		}
	}//end of newPatient function
	
	/**
	 * Request blank form to search for patient by id
	 */
	@GetMapping("/patient/edit")
	public String getPatientForm(Model model) {
//		model.addAttribute("patient", new Patient());
		return "patient_get";//html page
	}

	/**
	 * return JDBC Connection using jdbcTemplate in Spring Server
	 * java database connection
	 */
	private Connection getConnection() throws SQLException {
		return jdbcTemplate.getDataSource().getConnection();
	}

   //private function for validating input user input
	private void checkInput(Patient p) throws SQLException {
	   //check if information is a-z format
		if (pattern.matcher(p.getFirst_name()).find() ||
			    pattern.matcher(p.getLast_name()).find() ||
			    pattern.matcher(p.getSsn()).find() ||
			    pattern.matcher(p.getStreet()).find() ||
			    pattern.matcher(p.getUnitNo()).find() ||
			    pattern.matcher(p.getCity()).find() ||
			    pattern.matcher(p.getState()).find() ||
			    pattern.matcher(p.getZipcode()).find() ||
			    pattern.matcher(p.getPrimaryName()).find() ||
			    //zip code has to have 5 chars or state has to have 2 chars
			    p.getZipcode().length() != 5 || p.getState().length() != 2) {
			throw new SQLException("Illegal characters in form");
		} 
		//if input fields have no user input then warn
		else if (p.getFirst_name().isBlank() || p.getLast_name().isBlank() ||
			           p.getSsn().isBlank() || p.getStreet().isBlank() ||
			           p.getCity().isBlank() || p.getState().isBlank()) {
			throw new SQLException("Missing field in form");
		}
		
		try {//try converting birthday,ssn,zip code to whole integers
			Integer.parseInt(p.getBirthdate());
			Integer.parseInt(p.getSsn());
			Integer.parseInt(p.getZipcode());
		} 
		catch (NumberFormatException e) {//error
			throw new SQLException("Number format problem");
		}
		//validate social security number can't start with 0, has to have 9 digits,can't start with 9, 
		//can't start with 00, or end with 0000 if so throw error
		if (p.getSsn().startsWith("0") || p.getSsn().length() != 9 || p.getSsn().startsWith("9") ||
			    p.getSsn().startsWith("00", 3) || p.getSsn().endsWith("0000")) throw new SQLException("SSN problem");
	}//end of checkInput function


	/*
	 * Perform search for patient by patient id and name.
	 */
	//written by Crystal member 3
	@PostMapping("/patient/show")
	public String getPatientForm(@RequestParam("patientId") String patientId, @RequestParam("last_name") String last_name, Model model) {
		Patient patient = new Patient();//object
		/*
		 * code to search for patient by id and name retrieve patient data and primary
		 * doctor
		 */
		//validating user input
		try (Connection con = getConnection();) {
			//System.out.println("start getPatient "+ patient);
		   
		   //patient id can't be 0 or negative patientId converted to integer
			if (Integer.valueOf(patientId) == 0 || Integer.valueOf(patientId) < 0) {
				throw new SQLException("Patient ID error!!");
			}
			//checking if can't leave patient last name empty or no special characters 
			if (last_name.isBlank() || last_name.matches("[^a-zA-Z\s\\d]")) {
				throw new SQLException("Last name required!!");
			}

			//order 1-2 from sql table fields to retrieve patient information 1 for id and 2 for last name
			PreparedStatement ps = con.prepareStatement("select p.id,p.ssn, p.first_name, p.last_name,"
				                                            + " p.birthday, a.street, a.unitNo, a.city, a.state, a.zip, d.first_name, d.last_name "
				                                            + "from Address a join Patient p join Doctor d on"
				                                            + " a.id = p.address_id and p.doctor_id = d.id "
				                                            + "where p.id=? and p.last_name =?");
			ps.setString(1, patientId);
			ps.setString(2, last_name);
			ResultSet rs = ps.executeQuery();

			//loop for next patient
			if (rs.next()) {//get sql table fields from ps above
				patient.setPatientID(rs.getString("p.id"));
				patient.setSsn(rs.getString("p.ssn"));//int
				patient.setFirst_name(rs.getString("p.first_name"));
				patient.setLast_name(rs.getString("p.last_name"));
				patient.setBirthdate(rs.getString("p.birthday"));
				patient.setStreet(rs.getString("a.street"));
				patient.setUnitNo(rs.getString("a.unitNo"));
				patient.setCity(rs.getString("a.city"));
				patient.setState(rs.getString("a.state"));
				patient.setZipcode(rs.getString("a.zip"));
				patient.setPrimaryName(rs.getString("d.last_name") + ", " + rs.getString("d.first_name"));
				
				model.addAttribute("patient", patient);//obj from html
				System.out.println("End getPatientForm" + patient);
				return "patient_show";//html page
			} 
			else {//warn patient not found
				model.addAttribute("message", "Patient not found");//text from html
				return "patient_get";//html page
			}
		} 
		catch (SQLException e) {//error
			System.out.println("SQL error in getPatientForm " + e.getMessage());
			model.addAttribute("message", e.getMessage());//text from html
			model.addAttribute("patient", patient);//obj from html
			return "patient_get";//html page
		}
	}//end of getPatientForm function
	
// If you encounter any problem please replace methods with ones below...
/**
 * Perform search for patient by patient id and name
 * @author Deniz Erisgen ©
 *//*

   @PostMapping("/patient/show")
   public String getPatientForm(@RequestParam("patientId") String patientId, @RequestParam("last_name") String last_name, Model model) {
      Patient patient = new Patient();
      try (Connection con = getConnection()) {
         if (pattern.matcher(patientId).find() || pattern.matcher(last_name).find())
            throw new SQLException("Illegal text");
         if (patientId.isBlank() || last_name.isBlank()) throw new SQLException("A patient ID and Lastname is required");
         PreparedStatement ps = con.prepareStatement("select * from Patient where id=? and last_name=?");
         ps.setInt(1, Integer.parseInt(patientId));
         ps.setString(2, last_name.toUpperCase().trim());
         patient.setPatientID(patientId);
         ResultSet rs = ps.executeQuery();
         if (rs.next()) {
            ps = con.prepareStatement("select * FROM Address where id=?");
            ps.setInt(1, rs.getInt("address_id"));
            patient.setLast_name(rs.getString("last_name"));
            patient.setFirst_name(rs.getString("first_name"));
            patient.setBirthdate(rs.getString("birthday"));
            patient.setPrimaryID(rs.getInt("doctor_id"));
            patient.setAddressID(rs.getInt("address_id"));
            rs = ps.executeQuery();
//           Getting Address from DB
            if (rs.next()) {
               patient.setStreet(rs.getString("street"));
               patient.setUnitNo((rs.getString("unitNo") != null) ? rs.getString("unitNo").toUpperCase() : "");
               patient.setCity(rs.getString("city"));
               patient.setState(rs.getString("state"));
               patient.setZipcode(String.valueOf(rs.getInt("zip")));
            }
//           Getting Doctor from DB
            ps = con.prepareStatement("select last_name,first_name,specialty from Doctor where id=?");
            ps.setInt(1, patient.getPrimaryID());
            rs = ps.executeQuery();
            if (rs.next()) {
               patient.setPrimaryName(rs.getString(1) + ", " + rs.getString(2) + " - " + rs.getString(3));
            }
            model.addAttribute("patient", patient);
            return "patient_show";
         } else throw new SQLException("Patient not found.");
      } catch (SQLException e) {
         model.addAttribute("message", e.getMessage());
         model.addAttribute("patient", patient);
         return "patient_get";
      }
   }

   */

	/*
	 *  Display patient profile for patient id for Updating
	 */
	//written by Crystal member 3
	@GetMapping("/patient/edit/{patientId}")
	public String updatePatient(@PathVariable String patientId, Model model) {

		// TODO Complete database logic search for patient by id.
		Patient patient = new Patient();//object
		patient.setPatientID(patientId);//set id to update
		
		try (Connection con = getConnection();) {
		   //patient id can't be 0 or negative 
			if (Integer.valueOf(patientId) == 0 || Integer.valueOf(patientId) < 0) {
				throw new SQLException("Patient ID error!!");
			}
			
			//1 from sql field for patient id to get patient information
			PreparedStatement ps = con.prepareStatement("select p.id, p.ssn, p.first_name, p.last_name, "
				                                            + "p.birthday, a.street, a.unitNo, a.city, a.state, a.zip, d.first_name, d.last_name "
				                                            + "from Address a join Patient p join Doctor d on "
				                                            + "a.id = p.address_id and p.doctor_id = d.id where p.id=?");
			ps.setString(1, patientId);
			ResultSet rs = ps.executeQuery();
			
			//results order from sql fields from ps above
			if (rs.next()) {//loop for next patient
				patient.setPatientID(rs.getString("p.id"));
				patient.setSsn(rs.getString("p.ssn"));
				patient.setFirst_name(rs.getString("p.first_name"));
				patient.setLast_name(rs.getString("p.last_name"));
				patient.setBirthdate(rs.getString("p.birthday"));
				patient.setStreet(rs.getString("a.street"));
				patient.setUnitNo(rs.getString("a.unitNo"));
				patient.setCity(rs.getString("a.city"));
				patient.setState(rs.getString("a.state"));
				patient.setZipcode(rs.getString("a.zip"));
				patient.setPrimaryName(rs.getString("d.last_name") + ", " + rs.getString("d.first_name"));
				model.addAttribute("patient", patient);//obj from html
				return "patient_edit";//html page
			} 
			else {//warn if not found
				model.addAttribute("message", "Patient not found");//text from html
				model.addAttribute("patient", patient);//obj from html
				return "patient_get";//html page
			}
		} 
		catch (SQLException e) {//error
			model.addAttribute("message", e.getMessage());//text from html
			model.addAttribute("patient", patient);//obj from html
			return "patient_get";//html page
		}
	}//end of updatePatient function

	/**
	 * Display patient profile for patient id.
	 * @author Deniz Erisgen ©
	 *//*

	   @GetMapping("/patient/edit/{patientId}")
	   public String updatePatient(@PathVariable String patientId, Model model) {
	      Patient patient = new Patient();
	      try (Connection con = getConnection()) {
	         PreparedStatement ps, ps2, ps3;
	         ps = con.prepareStatement("select * from Patient where id=?");
	         ps.setInt(1, Integer.parseInt(patientId));
	         ResultSet rs = ps.executeQuery();
	         rs.next();
	         patient.setPatientID(patientId);
	         patient.setLast_name(rs.getString("last_name"));
	         patient.setFirst_name(rs.getString("first_name"));
	         patient.setBirthdate(rs.getString("birthday"));
	         patient.setPrimaryID(rs.getInt("doctor_id"));
	         patient.setAddressID(patient.getAddressID());
	         ps2 = con.prepareStatement("select street,unitNo,city,state,zip FROM Address where id=?");
	         ps2.setInt(1, rs.getInt("address_id"));
	         rs = ps2.executeQuery();
//	          Getting Address from DB
	         rs.next();
	         patient.setStreet(rs.getString("street"));
	         patient.setUnitNo((rs.getString("unitNo") != null) ? rs.getString("unitNo").toUpperCase() : "");
	         patient.setCity(rs.getString("city"));
	         patient.setState(rs.getString("state"));
	         patient.setZipcode(String.valueOf(rs.getInt("zip")));
	         //           Getting Doctor from DB
	         ps3 = con.prepareStatement("select last_name,first_name from Doctor where id=?");
	         ps3.setInt(1, patient.getPrimaryID());
	         rs = ps3.executeQuery();
	         rs.next();
	         patient.setPrimaryName(rs.getString(1) + ", " + rs.getString(2));

	         model.addAttribute("patient", patient);
	         return "patient_edit";
	      } catch (SQLException e) {
	         model.addAttribute("message", e.getMessage());
	         model.addAttribute("patient", patient);
	         return "patient_edit";
	      }
	   }

	   */
	/*
	 * Process changes to patient profile.
	 */
	//written by Crystal member 3
	@PostMapping("/patient/edit")
	public String updatePatient(Patient patient, Model model) {

		try (Connection con = getConnection();) {
			ResultSet rs;
			//get patient's address 1 from sql table field for id
			PreparedStatement ps = con.prepareStatement("select address_id from Patient where id=?");
			ps.setInt(1, Integer.parseInt(patient.getPatientID()));
			rs = ps.executeQuery();

			//loop for updating patient's address information
			if (rs.next()) ;
			ps = con.prepareStatement("update Address set street=?, unitNo=?, city=?, state=?, zip=? where id=?");

			/*validating street,unitNo,city,state,zip or if left user input blank*/
			if (patient.getStreet().isBlank() || patient.getStreet().matches("[^a-zA-Z\s\\d]")) {
				throw new SQLException("Street required!!");
			}
			if (patient.getCity().isBlank() || patient.getCity().matches("[^a-zA-Z\s\\d]")) {
				throw new SQLException("City required!!");
			}
			if (patient.getState().isBlank() || patient.getState().matches("[^a-zA-Z\s\\d]")) {
				throw new SQLException("State required!!");
			}
			if (patient.getZipcode().isBlank() || patient.getZipcode().matches("[^a-zA-Z\\d]")
				    || patient.getZipcode().length() != 5) {
				throw new SQLException("Zip code required!!");
			}
			try {
				Integer.parseInt(patient.getZipcode());//zip as a whole integer
			} 
			catch (NumberFormatException e) {//error
				throw new SQLException("Number format problem!!");
			}
			//doctor's name can't be empty or different characters
			//doctor is required to update patient
			if (patient.getPrimaryName().isBlank() || patient.getPrimaryName().matches("[^a-zA-Z\\d]")) {
				throw new SQLException("Doctor required!!");
			}
			/*--------------------------------------------------------------------------------------*/
			
			//order 1-6 for sql table fields from ps above for address
			ps.setString(1, patient.getStreet());
			ps.setString(2, patient.getUnitNo());
			ps.setString(3, patient.getCity());
			ps.setString(4, patient.getState());
			ps.setString(5, patient.getZipcode());
			ps.setInt(6, rs.getInt(1));//address id
			int row = ps.executeUpdate();
			
			//if row count to update address is not one warn
			if (row != 1) throw new SQLException("address problem");

			//get doctor's id for the first and last name
			ps = con.prepareStatement("select id from Doctor where last_name=? and first_name=?");

			//separate doctor last and first name by a comma order 1-2 from sql table fields
			String[] name = patient.getPrimaryName().split(",");//last,name
			ps.setString(1, name[0].trim());//first
			ps.setString(2, name[1].trim());//last 
			rs = ps.executeQuery();

			//updating patient's doctor order 1-2 from sql table fields for first and last name =?
			if (rs.next()) {
				ps = con.prepareStatement("update Patient set doctor_id=? where id=?");
				ps.setInt(1, rs.getInt(1));
				ps.setInt(2, Integer.parseInt(patient.getPatientID()));//convert to integer
				row = ps.executeUpdate(); //executing patients name
			}
			model.addAttribute("message", "Update successfully");//text from html
			model.addAttribute("patient", patient);//obj from html
			return "patient_show";//html page
		} 
		catch (SQLException e) {//error
			model.addAttribute("message", e.getMessage());//text from html
			model.addAttribute("patient", patient);//obj from html
			return "patient_edit";//html page
		}
	}//end of updatePatient function

/**
 * Process changes to patient profile.
 * @author Deniz Erisgen ©
 *//*

	@PostMapping("/patient/edit")
	public String updatePatient(Patient p, Model model) {
		try (Connection con = getConnection()) {
			int address;
			PreparedStatement ps;
			ps = con.prepareStatement("SELECT address_id FROM Patient where id=?");
			ps.setInt(1, Integer.parseInt(p.getPatientID()));
			ResultSet rs = ps.executeQuery();
			rs.next();
			address = rs.getInt(1);
			ps = con.prepareStatement("UPDATE Address SET street = ? , unitNo = ?,city = ? , state = ? , zip = ? WHERE id = ?");
			ps.setString(1, p.getStreet().toUpperCase());
			ps.setString(2, p.getUnitNo().toUpperCase());
			ps.setString(3, p.getCity().toUpperCase());
			ps.setString(4, p.getState().toUpperCase());
			ps.setInt(5, Integer.parseInt(p.getZipcode()));
			ps.setInt(6, address);
			String message = "No rows were updated";
			if (ps.executeUpdate() == 1) message = "Update Successful";
			model.addAttribute("message", message);
			model.addAttribute("patient", p);
			return "patient_show";
		} catch (SQLException e) {
			model.addAttribute("message", e.getMessage());
			model.addAttribute("patient", p);
			return "patient_show";
		}
	}
*/
}//end of class