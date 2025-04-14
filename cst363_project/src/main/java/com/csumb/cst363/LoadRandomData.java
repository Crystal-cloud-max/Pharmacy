/**
 * @author Deniz Erisgen ©
 **/
package com.csumb.cst363;

import net.datafaker.Faker;

import java.sql.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.sql.Types.NULL;

/**
 * This is an example of a JDBC Java application.
 * Run this program as a Java application.
 */
public class LoadRandomData {

	static final String DBURL = "jdbc:mysql://localhost:3306/pharmadb";  // database URL
	static final String USERID = "root";
	static final String PASSWORD = "Mysqluser.2022";

	public static void main(String[] args) {
		Random random = new Random();
		RandomDataGenerator peopleGenerator = new RandomDataGenerator("src/main/resources/static/census.csv", 100);//path
		Faker faker = new Faker();//object

		// connect to mysql server
		try (Connection con = DriverManager.getConnection(DBURL, USERID, PASSWORD)) {
			PreparedStatement ps;
			PreparedStatement ps2;
			ResultSet rs;
			int id;
			int row_count;
			int row_count2 = 0;
			
			//statement ps2 to update all foreign keys to zero
			ps2 = con.prepareStatement("SET foreign_key_checks = 0;");
			ps2.executeUpdate();
			
			// statement ps to delete all doctor rows
			ps = con.prepareStatement("delete from Doctor");
			row_count = ps.executeUpdate();
			System.out.println( row_count + " rows deleted from Doctor");
			
			//statement ps2 to update each doctor by incrementing each one
			ps2 = con.prepareStatement("ALTER TABLE Doctor AUTO_INCREMENT=1;");
			ps2.executeUpdate();
			
			// statement ps to delete all patient rows
			ps = con.prepareStatement("delete from Patient");
			row_count = ps.executeUpdate();
			System.out.println(row_count + " rows deleted Patient");
			
			//statement ps2 to update each patient by incrementing each one
			ps2 = con.prepareStatement("ALTER TABLE Patient AUTO_INCREMENT=1;");
			ps2.executeUpdate();
			
			// statement ps to delete all address rows over 461
			ps = con.prepareStatement("delete from Address WHERE id>461");
			row_count = ps.executeUpdate();
			System.out.println( row_count + " rows deleted Address" );
			
			//statement ps2 to update address by incrementing
			ps2 = con.prepareStatement("ALTER TABLE Address AUTO_INCREMENT=462;");
			ps2.executeUpdate();
			
			//statement ps to delete all prescription details
			ps = con.prepareStatement("delete from Prescription_detail");
			row_count = ps.executeUpdate();
			System.out.println(row_count + " rows deleted Prescription_detail" );
			
			//statement ps2 to update prescription's details by incrementing
			ps2 = con.prepareStatement("ALTER TABLE Prescription_detail AUTO_INCREMENT=1;");
			ps2.executeUpdate();
			
			//statement ps to delete all prescriptions
			ps = con.prepareStatement("delete from Prescription");
			row_count = ps.executeUpdate();
			System.out.println(row_count + " rows deleted Prescriptions");
			
			//statement 2 to update each prescription
//			ps2 = con.prepareStatement("ALTER TABLE Prescription AUTO_INCREMENT=1;");
//			ps2.executeUpdate();
//			ps2 = con.prepareStatement("SET foreign_key_checks = 1;");
//			ps2.executeUpdate();
			row_count = 0;
			
			// generate doctor data by adding a new one We want to generated column "id" value to be returned as a generated key
			String sqlInsertToDoctorTable = "insert into Doctor(last_name, first_name, specialty, practice_since, ssn) values(?,?,?,?,?)";
			String[] keycols = {"id"};  // columns that must return from sql Doctor.id
			ps = con.prepareStatement(sqlInsertToDoctorTable, keycols);
			
			//Add 10 random doctors by looping through each doctor
			RandomDoctor randomDoctor;
			for (int k = 1; k <= 10; k++) {
			   //order 1-5 from sql fields Doctor table
				randomDoctor = peopleGenerator.nextDoctor();
				ps.setString(1, randomDoctor.getLastName());
				ps.setString(2, randomDoctor.getFirstName());
				ps.setString(3, randomDoctor.getSpeciality());
				ps.setString(4, randomDoctor.getPractice_since());
				ps.setString(5, randomDoctor.getSsn());
				row_count += ps.executeUpdate();
			}
			System.out.println(row_count + " row inserted to doctor table");
			row_count = 0;

			//adding new patient into the database
			String sqlInsertToPatientTable = "insert into Patient (ssn,first_name,last_name,birthday,address_id,doctor_id) values (?,?,?,?,?,?)";
			ps = con.prepareStatement(sqlInsertToPatientTable, keycols);//keycols is sql Patient.id

			//adding new address into the database
			String sqlInsertToAddressTable = "insert into Address (street,unitNo,city,state,zip) values (?,?,?,?,?)";
			ps2 = con.prepareStatement(sqlInsertToAddressTable, keycols);//keycols is sql Address.id
			
			//adding 99 random people order 1-5 for sql fields for Address table
			Person person;
			for (int i = 0; i < 100; i++) {
				person = peopleGenerator.nextPerson();
				ps2.setString(1, faker.address().streetAddress(false).toUpperCase());
				//unit no is optional not required
				if (random.nextBoolean()) {
					ps2.setString(2, faker.address().secondaryAddress().toUpperCase());
				} else {
					ps2.setNull(2, NULL);
				}
				ps2.setString(3, faker.address().city().toUpperCase());
				ps2.setString(4, faker.address().stateAbbr());
				ps2.setInt(5, Integer.parseInt(faker.address().zipCode()));
				row_count += ps2.executeUpdate();

				// retrieve and print the generated primary key
				rs = ps2.getGeneratedKeys();
				rs.next();
				//order 1-6 from sql fields for Patient table
				id = rs.getInt(1);
				ps.setString(1, person.getSsn());
				ps.setString(2, person.getFirstName());
				ps.setString(3, person.getLastName());
				ps.setInt(4, person.getBirthYear());
				ps.setInt(5, id);
				ps.setInt(6, random.nextInt(10) + 1);
				row_count2 += ps.executeUpdate();
			}
			
			System.out.println(row_count + " row inserted to Address");
			System.out.println(row_count2 + " row inserted to Patients");
			row_count = 0;
			row_count2 = 0;

			//adding prescription detail information to the database
			String sqlInsertToPrescriptionDetailTable = "insert into Prescription_detail (issued_date,quantity,pharmacy_id,last_fill_date)" + " values (?,?,?,?)";
			ps = con.prepareStatement(sqlInsertToPrescriptionDetailTable, keycols);//keycols is sql Prescription_detail.id
			
			//adding prescription to the database
			String sqlInsertToPrescriptionTable = "insert into Prescription (doctor_id, patient_id ,pdetail_id,drug_id) values (?,?,?,?)";
			ps2 = con.prepareStatement(sqlInsertToPrescriptionTable, keycols);//keycols is sql Prescription.id
			
			//adding 99 random prescriptions order 1-4 for sql fields of Prescription_detail table
			for (int i = 0; i < 100; i++) {
				ps.setDate(1, Date.valueOf(faker.date().past(90, TimeUnit.DAYS, "YYYY-MM-dd")));
				ps.setInt(2, (random.nextInt(15) + 1));
				if (random.nextBoolean()) {
					ps.setInt(3, random.nextInt(461) + 1);
					ps.setDate(4, Date.valueOf(faker.date().past(90, 2, TimeUnit.DAYS, "YYYY-MM-dd")));
				} 
				else {//set last fill date to empty
					ps.setNull(3, NULL);
					ps.setNull(4, NULL);
				}
				row_count += ps.executeUpdate();
				rs = ps.getGeneratedKeys();
				rs.next();
				
				//order 1-4 from sql fields for Prescription table
				id = rs.getInt(1);
				ps2.setInt(1, random.nextInt(10) + 1);
				ps2.setInt(2, random.nextInt(100) + 1);
				ps2.setInt(3, id);
				ps2.setInt(4, random.nextInt(100) + 1);
				row_count2 += ps2.executeUpdate();
			}
			System.out.println(row_count + " row inserted to Prescription details");
			System.out.println(row_count2 + " row inserted to Prescription");

//			------------------------------------------------------
			// display all doctor rows
			System.out.println("-----------------------------------------");
			System.out.println("All doctors:");
			String sqlSelectDoctors = "select id, last_name, first_name, specialty, practice_since, ssn from Doctor";
			ps = con.prepareStatement(sqlSelectDoctors);
			rs = ps.executeQuery();
			while (rs.next()) {//loop through each doctor to next one
			   //retrieve each sql field from Doctor table
				id = rs.getInt("id");
				String last_name = rs.getString("last_name");
				String first_name = rs.getString("first_name");
				String specialty = rs.getString("specialty");
				String practice_since = rs.getString("practice_since");
				String ssn = rs.getString("ssn");
				//print doctor information in format digit,string,string,string,string,string
				System.out.printf("%-4d %-30s %-23s %20s %13s \n", id, last_name + ", " + first_name, specialty, "years of exp:" + practice_since, ssn);
			}
			
			//display all patient rows
			System.out.println("-----------------------------------------");
			System.out.println("All patients:");
			String sqlSelectPatients = "select Patient.id, last_name, first_name, birthday, "
				                           + "ssn,Address.street,Address.city,Address.state,Address.zip"
				                           + " FROM Patient JOIN Address ON Address.id = Patient.address_id";
			ps = con.prepareStatement(sqlSelectPatients);
			// there are no parameter markers to set
			rs = ps.executeQuery();
			while (rs.next()) {//loop through each patient to next one
			 //get each sql table from Patient table
				id = rs.getInt("id");
				String last_name = rs.getString("last_name");
				String first_name = rs.getString("first_name");
				String street = rs.getString("street");
				String city = rs.getString("city");
				String state = rs.getString("state");
				int zip = rs.getInt("zip");
				int birthday = rs.getInt("birthday");
				String ssn = rs.getString("ssn");
				//print patient information formatted
				System.out.printf("%-4d %-23s %-20s %17s %32s %20s %-3s %6d \n", id, last_name + ", " + first_name, " Birth year : " + birthday, "SSN : " + ssn, "Address : " + street, city, state, zip);
			}
			
			//display all prescription rows
			System.out.println("-----------------------------------------");
			System.out.println("All prescriptions:");
			String sqlSelectPrescriptions = "SELECT *  FROM Prescription JOIN Drug D on Prescription.drug_id = D.id JOIN Prescription_detail Pd on Pd.id = Prescription.pdetail_id JOIN Doctor Dr on Dr.id = Prescription.doctor_id JOIN Patient P on P.id = Prescription.patient_id ORDER BY Prescription.rx";
			ps = con.prepareStatement(sqlSelectPrescriptions);
			rs = ps.executeQuery();
			while (rs.next()) {//loop through each patient to next one
			   //get each sql table from Prescription table
				id = rs.getInt("rx");
				String issueDate = rs.getString("issued_date");
				int refillsAllowed = rs.getInt("quantity");
				String drLastName = rs.getString("Dr.last_name");
				String drFirstName = rs.getString("Dr.first_name");
				String specialty = rs.getString("specialty");
				String drugName = rs.getString("D.name");
				String pLastName = rs.getString("P.last_name");
				String pFirstName = rs.getString("P.first_name");
				//print prescription table formatted
				System.out.printf("%-4d %-10s  %-23s %20s %25s %13d %20s \n", id, issueDate, drLastName + ", " + drFirstName, specialty, pLastName + ", " + pFirstName, refillsAllowed, drugName);
			}

		} 
		catch (SQLException e) {//error
			System.out.println("Error: SQLException " + e.getMessage());
		}
	}//end of main
}//end of class