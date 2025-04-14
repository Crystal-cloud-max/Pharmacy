package com.csumb.cst363;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.sql.*;
import java.util.regex.Pattern;

/**
 * Controller class for doctor registration and profile update.
 */
@Controller
public class ControllerDoctor {
   	//string alphabetical format pattern
	private final Pattern pattern = Pattern.compile("[^a-zA-Z\s\\d,#]");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Request to add new doctor to registration form.
	 */
	@GetMapping("/doctor/register")
	public String newDoctor(Model model) {
		// return blank form for new doctor registration
		model.addAttribute("doctor", new Doctor());//obj from html
		return "doctor_register";//back to html page
	}//end of newDoctor function

	/**
	 * Process doctor registration. to add new doctor to the database table
	 */
	@PostMapping("/doctor/register")
	public String createDoctor(Doctor doctor, Model model) {
		try (Connection con = getConnection()) {
		   //call function to verify user input by last name
			checkInput(doctor, 1);
			
			//Insertion statement order 1-5 refer to sql table fields
			PreparedStatement ps = con.prepareStatement("insert into Doctor(last_name, first_name, specialty, practice_since,  ssn )"
			      + " values(?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, doctor.getLast_name().toUpperCase());//LAST NAME
			String first = doctor.getFirst_name();
			//first name input will be Crystal for example
			first = first.substring(0, 1).toUpperCase() + first.substring(1).toLowerCase();
			ps.setString(2, first);
			ps.setString(3, doctor.getSpecialty());
			ps.setString(4, doctor.getPractice_since_year());
			ps.setString(5, doctor.getSsn());
			ps.executeUpdate();

			//doctor results if doctor can't be added
			ResultSet rs = ps.getGeneratedKeys();//get keys
			if (rs.next()) doctor.setId(String.valueOf(rs.getInt(1)));//returns last name as a string
			else throw new SQLException("Couldn't add doctor");
			
			// display message and doctor information
			model.addAttribute("message", "Registration successful.");//text from html
			model.addAttribute("doctor", doctor);//obj from html
			return "doctor_show";//html page
		} 
		catch (SQLException e) {//error
			model.addAttribute("message", "SQL Error." + e.getMessage());//text from html
			model.addAttribute("doctor", doctor);//obj from html
			return "doctor_register";//html page
		}
	}//end of createDoctor function

	/**
	 * Request blank form for doctor search.
	 */
	@GetMapping("/doctor/get")
	public String getDoctor(Model model) {
		// return form to enter doctor id and name
		model.addAttribute("doctor", new Doctor());//obj from html
		return "doctor_get";//html page
	}//end of getDoctor function

	/**
	 * Search for doctor by id and name.
	 */
	@PostMapping("/doctor/get")
	public String getDoctor(Doctor doctor, Model model) {
		try (Connection con = getConnection()) {
		   //call function to verify user input by doctor's last name
			checkInput(doctor, 2);
			
			//Select statement to get Doctor from table 1 for id=?,2 for last_name=?
			PreparedStatement ps = con.prepareStatement("select last_name, first_name, specialty, practice_since from Doctor where id=? and last_name=?");
			ps.setInt(1, Integer.parseInt(doctor.getId()));//id converted to whole integer
			ps.setString(2, doctor.getLast_name().toUpperCase().trim());//LASTNAME
			
			//doctor results to add new doctor warn if not found
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {//strings refer to sql table fields
				doctor.setLast_name(rs.getString("last_name"));
				doctor.setFirst_name(rs.getString("first_name"));
				doctor.setPractice_since_year(rs.getString("practice_since"));
				doctor.setSpecialty(rs.getString("specialty"));
				
				model.addAttribute("doctor", doctor);//obj from html
				return "doctor_show";//back to html page
			} 
			else throw new SQLException("Doctor not found.");
		} 
		catch (SQLException e) {//error
			model.addAttribute("message", e.getMessage());//text from html
			model.addAttribute("doctor", doctor);//obj from html
			return "doctor_get";//back to html page
		}
	}//end of getDoctor function

	/**
	 * search for doctor by id for editing
	 */
	@GetMapping("/doctor/edit/{id}")
	public String getDoctor(@PathVariable String id, Model model) {
		Doctor doctor = new Doctor();//object
		doctor.setId(id);//set doctor id
		
		try (Connection con = getConnection()) {
		   //sql statement to get doctor information
			PreparedStatement ps = con.prepareStatement("select last_name, first_name, specialty, practice_since from Doctor where id=?");
			ps.setInt(1, Integer.parseInt(id));//1 for id in sql table field
			
			//doctor results to search doctor warn if not found
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {//order 1-4 refer to sql table fields
				doctor.setLast_name(rs.getString(1));
				doctor.setFirst_name(rs.getString(2));
				doctor.setPractice_since_year(rs.getString(4));
				doctor.setSpecialty(rs.getString(3));
				
				model.addAttribute("doctor", doctor);//obj from html
				return "doctor_edit";//back to html page
			} 
			else throw new SQLException("Doctor not found.");
		} 
		catch (SQLException e) {//error
			model.addAttribute("message", e.getMessage());//text from html
			model.addAttribute("doctor", doctor);//obj from html
			return "doctor_get";//html page
		}
	}//end of getDoctor function

	/**
	 * process profile update for doctor.  Change specialty or year of practice.
	 */
	@PostMapping("/doctor/edit")
	public String updateDoctor(Doctor doctor, Model model) {
		try (Connection con = getConnection()) {
		   //call function to verify user input
			checkInput(doctor, 3);
			
			//code to update doctor order 1-3 for sql table fields
			PreparedStatement ps = con.prepareStatement("update Doctor set specialty=?, practice_since=? where id=?");
			ps.setString(1, doctor.getSpecialty());
			ps.setString(2, doctor.getPractice_since_year());
			ps.setInt(3, Integer.parseInt(doctor.getId()));

			//row count for DML to update
			int rc = ps.executeUpdate();
			if (rc == 1) {//1 for specialty sql table field to update 
				model.addAttribute("message", "Update successful");//text from html
				model.addAttribute("doctor", doctor);//obj from html
				return "doctor_show";//html page
			} 
			else {
				model.addAttribute("message", "Error. Update was not successful");//text from html
				model.addAttribute("doctor", doctor);//obj from html
				return "doctor_edit";//html page
			}
		} 
		catch (SQLException e) {//error
			model.addAttribute("message", "SQL Error." + e.getMessage());//text from html
			model.addAttribute("doctor", doctor);//obj from html
			return "doctor_edit";//html page
		}
	}

	/**
	 * return JDBC Connection using jdbcTemplate in Spring Server
	 * java database connection
	 */
	private Connection getConnection() throws SQLException {
		return jdbcTemplate.getDataSource().getConnection();
	}

	//private function to validate user input
	private void checkInput(Doctor doctor, int condition) throws SQLException {
		switch (condition) {
			case 1://verify doctor info input for illegal input ssn must be 9 digits,year 4 digits
				if (pattern.matcher(doctor.getFirst_name()).find() ||
					    pattern.matcher(doctor.getLast_name()).find() ||
					    pattern.matcher(doctor.getSsn()).find() ||
					    pattern.matcher(doctor.getSpecialty()).find() ||
					    pattern.matcher(doctor.getPractice_since_year()).find() ||
					    doctor.getSsn().length() != 9 || doctor.getPractice_since_year().length() != 4) {
					throw new SQLException("Illegal characters in form");
				} 
				//verify if no user input fields
				else if (doctor.getFirst_name().isBlank() || doctor.getLast_name().isBlank()
					           || doctor.getSsn().isBlank() || doctor.getSpecialty().isBlank()
					           || doctor.getPractice_since_year().isBlank()) {
					throw new SQLException("Missing field in form");
				}
				//Checking if social security number,years are in correct format
				try {
					Integer.parseInt(doctor.getPractice_since_year());
					if (Integer.parseInt(doctor.getSsn()) < 100000000) throw new SQLException("SSN problem");
					if (Integer.parseInt(doctor.getSsn()) < 1975) throw new SQLException("Experience years is wrong");
				} 
				catch (NumberFormatException e) {//error
					throw new SQLException("Number format problem");
				}
				break;
				
			case 2:
				//	Checking form for invalid characters and no user input
				if (pattern.matcher(doctor.getLast_name()).find() ||
					    pattern.matcher(doctor.getId()).find()) {
					throw new SQLException("Illegal characters in form");
				} 
				//verify if no user input fields
				else if (doctor.getLast_name().isBlank() || doctor.getId().isBlank()) {
					throw new SQLException("Missing field in form");
				}
				
				//Checking if number is in correct format for id 0 is not a valid id
				try {
					if (Integer.parseInt(doctor.getId()) == 0) throw new SQLException("Doctor ID can not be zero");
				} 
				catch (NumberFormatException e) {//error
					throw new SQLException("Number format problem");
				}
				break;
				
			case 3:
			   // Checking form for illegal data and no user input
				if (pattern.matcher(doctor.getSpecialty()).find() ||
					    pattern.matcher(doctor.getPractice_since_year()).find()) {
					throw new SQLException("Illegal data in a field");
				}
				//verify if no user input fields
				if (doctor.getPractice_since_year().isBlank() || doctor.getSpecialty().isBlank()) {
					throw new SQLException("Missing data in form");
				}
				//Checking if numbers are in correct format for years
				try {
					if (Integer.parseInt(doctor.getPractice_since_year()) < 1975)
						throw new SQLException("Practise years is wrong");
				} 
				catch (NumberFormatException e) {//error
					throw new SQLException("Number format problem");
				}
			default:
				break;
		}//end of switch statement
	}//end of checkInput function
}//end of class