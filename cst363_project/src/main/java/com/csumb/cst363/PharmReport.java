package com.csumb.cst363;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class PharmReport {

	static final String DBURL = "jdbc:mysql://localhost:3306/pharmadb";  // database URL
	static final String USERID = "root";
	static final String PASSWORD = ""; // change password

	public static void main(String[] args) {
		// connect to mysql server
		try (Connection conn = DriverManager.getConnection(DBURL, USERID, PASSWORD);) {
			Scanner in = new Scanner(System.in);
			PreparedStatement ps;
			ResultSet rs;
			
			//initializing variable to empty
			String pharmID = "";
			LocalDate startDate = null;
			LocalDate endDate = null;

			while (true) {//loop
				String errorMessage = "";
				System.out.println("Please input pharmacy ID or exit to stop: ");
				pharmID = in.nextLine();//user input

				if (pharmID.equalsIgnoreCase("exit")) {//if input exit terminate input
					in.close();
					conn.close();
					System.exit(0);
				}
				
				//prompt user for a starting date
				System.out.println("\nPlease input start date formatted (yyyy-mm-dd): ");
				try {
					startDate = LocalDate.parse(in.nextLine());//input to converted date format
				} 
				catch (DateTimeParseException e) {//error
					System.out.println("Start date was in the wrong format try again\n");
				}
				
				if (errorMessage.equals("")) {//if no error
					if (startDate.compareTo(LocalDate.now()) > 0) {//if input later date warn
						System.out.println("Start Date is set after today's date!\n");
					}
				}
				
				if (errorMessage.equals("")) {//if no error
				   //prompt user for a ending date
					System.out.println("\nPlease input end date formatted (yyyy-mm-dd)");
					try {
						endDate = LocalDate.parse(in.nextLine());//input to converted date format
					} 
					catch (DateTimeParseException e) {//error
						System.out.println("End date was in the wrong format try again\n");
					}
					if (errorMessage.equals("")) {//if no error
						if (startDate.compareTo(endDate) > 0) {//if input later date warn
							System.out.println("End date is set after start date!\n");
						}
					}
				}

				if (errorMessage.equals("")) {//no error
				   //statement to get pharmacy id,drug,total quanity for prescription details
				   //order 1-3 from sql table fields for pharmacy id and dates ?
					String sqlSELECT = (
						"SELECT phar.id,Drug.name, SUM(pd.quantity) Total FROM Prescription pre " +
							"JOIN Drug ON pre.drug_id = Drug.id " +
							"JOIN Prescription_detail pd ON pre.pdetail_id = pd.id " +
							"JOIN Pharmacy phar ON pd.pharmacy_id = phar.id " +
							"WHERE phar.id = ? AND pd.last_fill_date BETWEEN ? and ? GROUP BY phar.id,Drug.name;");
					ps = conn.prepareStatement(sqlSELECT);
					int integerID = Integer.parseInt(pharmID);//convert to whole integer
					ps.setInt(1, integerID);
					ps.setDate(2, java.sql.Date.valueOf(startDate));//2024-02-14
					ps.setDate(3, java.sql.Date.valueOf(endDate));//2024-02-14
					rs = ps.executeQuery();
					
					//display output to the screen
					System.out.println("\nPharmacy ID: " + pharmID + " Date range: " + startDate + " until " + endDate);
					if (rs.next()) {//if next result
					   //printing phamacy id + start date + end date from sql fields
						System.out.println(rs.getInt(1) + "---" + rs.getString(2) +"---" + rs.getInt(3));
					}
				}
			}
		} //end of try
		catch (SQLException e) {//error
			System.out.println(e.getMessage());
		}
	}//end of main
}//end of class