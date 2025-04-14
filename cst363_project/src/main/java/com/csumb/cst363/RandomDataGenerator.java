/**
 * @author Deniz Erisgen ©
 * Random People Generator Class no duplicate values
 **/

package com.csumb.cst363;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

class RandomDataGenerator {
	private static final int LIST_SIZE = 1000;
	private final Random random = new Random();
	private final Set<Integer> generatedSSNS = new HashSet<>(LIST_SIZE);
	private final Set<Person> generatedPeople = new HashSet<>(LIST_SIZE);
	private final String[] lastnames = new String[LIST_SIZE];
	private final ArrayList<String> maleNames = new ArrayList<>(100);
	private final ArrayList<String> femaleNames = new ArrayList<>(100);
	private Iterator<Person> personIterator;
	boolean isDoctorList = false;

	/**
	 * Public constructor
	 * @param filename need to include the path from project source
	 * @param size list size
	 */
	public RandomDataGenerator(String filename, int size) {
		try {
			// use files path starting from project file
			File file = new File(System.getProperty("user.dir")+'/'+filename);
			Scanner scanner = new Scanner(file);
			scanner.nextLine(); //user input
			
			int count = 0;
			while (scanner.hasNextLine()) {//while user input next line
				String data = scanner.nextLine();//user input
				String[] line = data.split(",");//separate last and first name by comma
				lastnames[count] = line[0];//surname is first character
				maleNames.add(line[1]);//male name is 2nd character
				femaleNames.add(line[2]);//female name is 3rd character
				count++;//increment
			}
			scanner.close();//terminate
		} 
		catch (FileNotFoundException e) {//error
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
		populatePeopleList(size);//call function holding size of people
		personIterator = generatedPeople.iterator();//return elements in the set list of generated people
	}//end of RandomDataGenerator function

	/**
	 * Populates the generatedPeople list
	 * @param listSize size for the list
	 */
	private void populatePeopleList(int listSize) {
			Person randomPerson;
			for (int i = 0; i < listSize; i++) {//loop up to size of about 1000 people
				randomPerson = generatePerson(isDoctorList);//call function passing false
				//while not generated people add random person
				while (!generatedPeople.add(randomPerson)) randomPerson = generatePerson(isDoctorList);
			}
	}

	/**
	 * Generates a person obj
	 * @param needDoctor bool controls out put obj
	 * @return Person obj
	 */
	private Person generatePerson(boolean needDoctor){
		String lastname = lastnames[random.nextInt(LIST_SIZE)];//random list of 1000 last names
		String firstname = random.nextBoolean() ?//if next random name return male else female name
			                   maleNames.get(random.nextInt(maleNames.size()))
			                   : femaleNames.get(random.nextInt(femaleNames.size()));
		String ssn = String.valueOf(generateSSN());//social security no call function parsed to string
		//if doctor necessary return born in 1962-1987 randomly else born 1903-2023
		int birthYear = needDoctor ? 1962+random.nextInt(25) : 2023 - random.nextInt(100); 
		//if doctor necessary return new random doctor with information
		if (needDoctor) return new RandomDoctor(firstname,lastname, ssn, birthYear);
		else return new Person(firstname,lastname, ssn, birthYear);//else return new person with information
	}

	/**
	 * 9 digit ssn generator
	 * @return a unique random ssn as INT
	 */
	private int generateSSN() {
		int ssn =(random.nextInt(800)+100) * (int) Math.pow(10,6) +//random between 899 * 10^6
			         (random.nextInt(99)+1) * (int) Math.pow(10,4) +//random between 99 * 10^4
			         (random.nextInt(9999)+1);//random 9999
		while (!generatedSSNS.add(ssn)) ssn = generateSSN();//add social security number
		return ssn;
	}

	/**
	 * Delivers the next Person
	 * @return next unique Person in line
	 */
	public Person nextPerson() {
		if (!personIterator.hasNext()) {//if not next person removed
			populatePeopleList(100);//call function of 100 people
			System.out.println("No more data left creating more");//print
			personIterator = generatedPeople.iterator();
		}
		return personIterator.next();//return next person in the iterator to remove
	}

	/**
	 * Delivers a new unique doctor
	 * @return creates a new unique doctor
	 */
	public RandomDoctor nextDoctor() {
		return (RandomDoctor) generatePerson(true);//call function converted to random doctor class
	}
/*
//	debug print
	public static void main(String[] args) {
		RandomDataGenerator generator = new RandomDataGenerator("src/main/resources/static/census.csv",200 );
		for (int i = 0; i < 100; i++) {
			System.out.println(generator.nextPerson());
			System.out.println(generator.nextDoctor());
		}
	}
*/
}

/**
 * Person class holds data
 */
 class Person {
    //attributes
	private final String firstName;
	private final String lastName;
	private final String ssn;
	private final int birthYear;

	//working constructor
	 public Person(String firstName, String lastName, String ssn, int birthyear) {
		 this.firstName = firstName;
		 this.lastName = lastName;
		 this.ssn = ssn;
		 this.birthYear = birthyear;
	 }
	 
	 //getters and setters
	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getSsn() {
		return ssn;
	}

	public int getBirthYear() {
		return birthYear;
	}

	//stringifier
	@Override
	 public String toString() {
		 return  lastName +", "+ firstName + ", ssn=" + ssn+" , birthyear= "+ birthYear;
	 }
 }

 class RandomDoctor extends Person{
    //attributes
	 private final String speciality;
	 private final int practice_since;
	 
	 //working constructor
	 public RandomDoctor(String firstName, String lastName, String ssn,int birthyear) {
		 super(firstName, lastName, ssn, birthyear);//calling working constructor from Person class
		 
		 Random rand = new Random();//object for random
		 
		 this.practice_since = (birthyear +25 +rand.nextInt(11));//born + 25 + random 11 years
		 
		 //array of specialities
		 String[] specialties = { "Family medicine","Allergy and immunology", "Anesthesiology",
			 "Dermatology", "Diagnostic radiology", "Emergency medicine",
			 "Internal medicine", "Medical genetics", "Neurology", "Nuclear medicine",
			 "Obstetrics & Gynecology", "Ophthalmology", "Pathology", "Pediatrics",
			 "Physical medicine", "Preventive medicine", "Psychiatry",
			 "Radiation oncology", "Surgery", "Urology"};
		 
		 //random speciality if next T or F 1st speciality else next random speciality
		 speciality = rand.nextBoolean() ? specialties[0]:specialties[rand.nextInt(specialties.length - 2) + 1];
	 }
	 
	 //getters
	 public String getSpeciality() {
		 return speciality;
	 }

	 public String getPractice_since() {
		 return String.valueOf(practice_since);//integer parsed to string
	 }

	 //stringifier
	 @Override
	 public String toString() {
		 return  super.toString()+", speciality: " + speciality+", Years of experience: " + practice_since;
	 }
 }