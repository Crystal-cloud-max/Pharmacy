package com.csumb.cst363;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class ControllerPrescriptionFill {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Patient requests form to search for prescription.
	 */
	@GetMapping("/prescription/fill")
	public String getfillForm(Model model) {
		model.addAttribute("prescription", new Prescription());//obj from html
		return "prescription_fill";//html page
	}

	//function to fill pharmacy presciption
	@PostMapping("/prescription/fill")
	public String processFillForm(Prescription p, Model model) {
		try (Connection con = getConnection()) {
			checkInput(p);//call function to validate user input
			
			//get information 1 from sql table field for rx as primary key
			p.setPatientLastName(p.getPatientLastName().toUpperCase());//LASTNAME
			p.setPharmacyName(p.getPharmacyName().toUpperCase());//CVS
			p.setPharmacyAddress(p.getPharmacyAddress().toUpperCase());//ADDRESS
			PreparedStatement ps = con.prepareStatement(
				"SELECT rx,pat.last_name,pd.quantity,dr.ssn,dr.first_name," +
					"dr.last_name,pat.last_name,pat.first_name,pat.ssn,p.drug_id,d.name,pd.id " +
					"FROM Prescription p JOIN Prescription_detail pd ON pd.id = p.pdetail_id " +
					"JOIN Patient pat ON p.patient_id = pat.id " +
					"JOIN Doctor dr ON p.doctor_id = dr.id " +
					"JOIN Drug d ON p.drug_id = d.id WHERE rx = ?"
			);
			ps.setInt(1, p.getRxid());
			
			//get pharmacy information including address 1-2 in order for sql table fields ?
			PreparedStatement ps2 = con.prepareStatement(
				"SELECT p.name,p.phoneNo, a.street, a.city, a.state, a.zip,p.id " +
					"FROM Pharmacy p JOIN Address a ON a.id = p.address_id " +
					"WHERE name LIKE ? and a.street LIKE ?");
			String pharmaName = p.getPharmacyName();
			String pharmaAddress = p.getPharmacyAddress();
			pharmaName = pharmaName + '%';//search name plus wild character
			pharmaAddress = pharmaAddress + '%';//search address plus wild character
			ps2.setString(1, pharmaName);
			ps2.setString(2, pharmaAddress);
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {//loop to get quantity
				int quant = rs.getInt("pd.quantity");//quantity from sql table field
				
				//Checking if patient's last name matching the data on form  then warn
				if (rs.getString("pat.last_name").compareTo(p.getPatientLastName()) != 0)
					throw new SQLException("Please check your RX number");
				
				//Checking if used all refill if quantity is 0 then warn
				if (quant == 0) {
					throw new SQLException("No more refills, please call your doctor for a new prescription");
				}
				
				//saving Prescription detail for updating get sql table fields
				int pDetailId = rs.getInt("pd.id");
				p.setDoctorFirstName(rs.getString("dr.first_name"));
				p.setDoctorLastName(rs.getString("dr.last_name"));
				p.setDoctor_ssn(rs.getString("dr.ssn"));
				p.setPatient_ssn(rs.getString("pat.ssn"));
				p.setPatientFirstName(rs.getString("pat.first_name"));
				p.setDrugName(rs.getString("d.name"));
				p.setQuantity(quant);
				p.setDateFilled(String.valueOf(new Date(System.currentTimeMillis())));//ex.2022-05-31 today's date
				p.setCost("$50"); // WE do not have a price table to calculate
				
				//Running second statement to find Pharmacy details for prescription sql fields from table
				ResultSet rs2 = ps2.executeQuery();
				if (rs2.next()) {
					p.setPharmacyID(rs2.getString("p.id"));//pharmacy id to update prescription detail
					String address = rs2.getString("a.street") + " " + rs2.getString("a.city") + " " 
					+ rs2.getString("a.state") + " " + rs2.getString("a.zip");//full address to view
					p.setPharmacyAddress(address);
					p.setPharmacyName(rs2.getString("p.name"));
					p.setPharmacyPhone(rs2.getString("p.phoneNo"));
					
					//Preparing an update statement for prescription_detail 1-4 in order for sql table by ?
					ps = con.prepareStatement(
						"UPDATE Prescription_detail pd " +
							"SET pd.quantity = ?,pd.pharmacy_id = ?," +
							"pd.last_fill_date = ? WHERE pd.id = ?");
					ps.setInt(1, quant);
					ps.setInt(2, Integer.parseInt(p.getPharmacyID()));//pharmacy id as a whole integer
					ps.setDate(3, Date.valueOf(p.getDateFilled()));//format date 2022-05-31
					ps.setInt(4, pDetailId);
					ps.executeUpdate();	
				} 
				else throw new SQLException("Pharmacy details are wrong");//warning
			} 
			else throw new SQLException("Prescription not found.");//warning
			model.addAttribute("message", "Prescription has been filled.");//text from html
			model.addAttribute("prescription", p);//obj from html
			return "prescription_show";//html page
		} 
		catch (SQLException e) {//error
			model.addAttribute("message", e.getMessage());//text from html
			model.addAttribute("prescription", p);//obj from html
			return "prescription_fill";//html page
		}
	}//end of processFillPrescription function

	/**
	 * return JDBC Connection using jdbcTemplate in Spring Server
	 * java database connection
	 */
	private Connection getConnection() throws SQLException {
		return jdbcTemplate.getDataSource().getConnection();
	}
	/*verifying correct user input invalid if character error string input a-z or if rx id is 0 or no input*/
	private void checkInput(Prescription p) throws SQLException{
		String pattern ="[^a-zA-Z\s\\d]";
		if (p.getRxid() == 0 ||p.getPharmacyName().matches(pattern) || p.getPharmacyName().isBlank()||
			    p.getPharmacyAddress().matches(pattern) ||p.getPharmacyAddress().isBlank()||
			    p.getPatientLastName().matches(pattern) || p.getPatientLastName().isBlank()){
			throw new SQLException("Illegal characters in form");
		}
	}
}//end of class