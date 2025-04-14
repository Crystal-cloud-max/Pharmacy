package com.csumb.cst363;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class GovernmentOfficial
{
   static final String DBURL = "jdbc:mysql://localhost:3306/pharmaDB";  // database URL
   static final String USERID = "root";
   static final String PASSWORD = "Mysqluser.2022";


   public static void main(String[] args)
   {
      //Properties properties = readProperties("src/main/resources/application.properties");
     
      // connect to mysql server  
      try (Connection conn = DriverManager.getConnection(DBURL, USERID, PASSWORD);)
      {
         Scanner in = new Scanner(System.in);//user input
         PreparedStatement ps;
         ResultSet rs;
         
         //initialize variables to empty
         String drugName = "";
         LocalDate startDate = null;
         LocalDate endDate = null;
             
         while(true) 
         { 
            String errorMessage = "";
            
            //prompt user for drug
            System.out.println("Please input name of the drug or exit to stop: ");
            drugName = in.nextLine();//user input
            
            if(drugName.equalsIgnoreCase("exit"))//if user inputs exit terminate
            {
               in.close();
               conn.close();
               System.exit(0);
            }
            
            //prompt user for a starting date
            System.out.println("\nPlease input start date formatted (yyyy-mm-dd): ");
            try {
               startDate = LocalDate.parse(in.nextLine());//input converted to date format
            }
            catch(DateTimeParseException e) {//error
               System.out.println("Start date was in the wrong format try again\n");
               return;
            }
            
            if (errorMessage.equals("")) //if no error
            {
               if (startDate.compareTo(LocalDate.now()) > 0) 
               {//if input after todays's date 
                   System.out.println("Start Date is set after today's date!\n");
               }
            }
            
            if(errorMessage.equals("")) {//if no error
               //prompt user for ending date
               System.out.println("\nPlease input end date formatted (yyyy-mm-dd)");
               try {
                  endDate = LocalDate.parse(in.nextLine());//input to converted date format
               }
               catch(DateTimeParseException e) {//error
                  System.out.println("End date was in the wrong format try again\n");
                  return;
               }
               if(errorMessage.equals(""))//if no error
               {
                  if(startDate.compareTo(endDate) > 0)
                  {  //if input after todays's date 
                     System.out.println("End date is set after start date!\n");
                  }
               }
            }
            
            if(errorMessage.equals(""))//if no error
            {  //get doctor's name and count of how many drugs by id order 1-3 from sql table fields ?
               String sqlSELECT = ("select d.last_name, d.first_name, count(drug.id) Total_Drugs from "
                     + "doctor d join prescription p join drug join prescription_detail pd on "
                     + "d.id = p.doctor_id and p.drug_id = drug.id and p.pdetail_id = pd.id"
                     + " where drug.name=? and pd.issued_date between ? and ?"
                     + "group by d.id");
               ps = conn.prepareStatement(sqlSELECT);
               ps.setString(1, drugName);
               ps.setDate(2, java.sql.Date.valueOf(startDate));//2024-02-14
               ps.setDate(3, java.sql.Date.valueOf(endDate));//2024-02-14
               rs = ps.executeQuery();
               
               //display drug name and dates to the screen
               System.out.println("\nPrescription Data: Drug name: " + drugName + " | Date range: "  + startDate + " until " + endDate);
               System.out.println();//print line
               
               //print head information formatted
               System.out.format("%20s  %20s  %10s\n", "Doctor's Surname ", " First Name ", " Total Drugs");
               System.out.println("****************************************************************************");
               while(rs.next()){//loop through information order 1-3 from sql table fields
                                //for d.last_name,d.first_name,count(drug.id)
                  System.out.format("%20s  %20s  %10d\n", rs.getString(1), rs.getString(2), rs.getInt(3)); 
               } 
               System.out.println("****************************************************************************\n");
            }  
         }   //while
      }//try
      catch (SQLException e){//error
         System.out.println(e.getMessage());
      }
   } //end of main
}//end of GovernmentOfficial class