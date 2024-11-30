package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Create patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again");
            return;
        }
        // check3: password strength
        password = passwordHelper(password);

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Create patient failed.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Create patient failed");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        // check3: password strength
        password = passwordHelper(password);

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static String passwordHelper(String currentPassword) {
        String password = currentPassword;
        boolean done = false;
        while(!done) {
            boolean length = false;
            boolean lowerCase = false;
            boolean upperCase = false;
            boolean number = false;
            boolean special = false;
            List<Character> special_characters = new ArrayList<>();
            special_characters.add('!');
            special_characters.add('@');
            special_characters.add('#');
            special_characters.add('?');
            if(password.length()>7) {
                length = true;
            }
            for(int i=0; i<password.length(); i++) {
                char ch = password.charAt(i);
                if(Character.isDigit(ch)){
                    number = true;
                } else if (Character.isUpperCase(ch)) {
                    upperCase = true;
                } else if (Character.isLowerCase(ch)) {
                    lowerCase = true;
                } else if (special_characters.contains(ch)){
                    special = true;
                }
            }
            if (!length) {
                System.out.println("Password has to be at least 8 characters long.");
            }
            if (!lowerCase || !upperCase) {
                System.out.println("Password has to contain both uppercase and lowercase letter.");
            }
            if (!number) {
                System.out.println("Password has to contain both letters and numbers");
            }
            if (!special) {
                System.out.println("Password has to contain at least one special character from !, @, #, ?");
            }
            if (length && lowerCase && upperCase && number && special) {
                done = true;
            } else {
                System.out.println("Please enter new password:");
                Scanner input = new Scanner(System.in);
                password = input.nextLine();
            }
        }
        return password;
    }


    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in, try again");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login patient failed");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login patient failed");
        } else {
            System.out.println("Logged in as " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }
        String date = tokens[1];
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String getCaregiver = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username";
        String getVaccine = "SELECT Name, Doses FROM Vaccines";
        try {
            Date d = Date.valueOf(date);
            PreparedStatement statement1 = con.prepareStatement(getCaregiver);
            statement1.setDate(1, d);
            ResultSet resultSet1 = statement1.executeQuery();
            while(resultSet1.next()) {
                String username = resultSet1.getString("Username");
                System.out.println(username);
            }
            PreparedStatement statement2 = con.prepareStatement(getVaccine);
            ResultSet resultSet2 = statement2.executeQuery();
            while(resultSet2.next()) {
                System.out.println(resultSet2.getString("Name") + " " +
                        resultSet2.getInt("Doses"));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    } // TODO: Part 2

    private static void reserve(String[] tokens) {
        // Pre-Checks
        if (currentCaregiver != null) {
            System.out.println("Please login as a patient");
            return;
        }
        if (currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again");
            return;
        }
        // Parse Tokens
        String date = tokens[1];
        String vaccine = tokens[2];
        String patient = currentPatient.getUsername();
        // Create connection
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        // SQL Statements
        String getCaregiver = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username";
        String getID = "SELECT id From Appointment ORDER BY id DESC";
        String removeAvailability = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?";
        String addAppointment = "INSERT INTO Appointment VALUES (?,?,?,?,?)";
        try {
            // Get caregiver
            Date d = Date.valueOf(date);
            PreparedStatement GCStatement = con.prepareStatement(getCaregiver);
            GCStatement.setDate(1,d);
            ResultSet GCResultSet = GCStatement.executeQuery();
            if(!GCResultSet.next()){
                System.out.println("No caregiver is available");
                return;
            }
            String caregiver = GCResultSet.getString("Username");
            // Check Vaccine availability
            Vaccine helperVaccine = new Vaccine.VaccineGetter(vaccine).get();
            if (helperVaccine == null) {
                System.out.println("Please try again");
                return;
            }
            if (helperVaccine.getAvailableDoses()==0) {
                System.out.println("Not enough available doses");
                return;
            }
            // Get largest id number
            int currentID;
            PreparedStatement GIDStatement = con.prepareStatement(getID);
            ResultSet GIDResultSet = GIDStatement.executeQuery();
            if(!GIDResultSet.next()){
                currentID = 1;
            } else {
                currentID = GIDResultSet.getInt("id") + 1;
            }
            // Remove caregiver availability
            PreparedStatement RAStatement = con.prepareStatement(removeAvailability);
            RAStatement.setDate(1, d);
            RAStatement.setString(2, caregiver);
            RAStatement.executeUpdate();
            // Change vaccine dose
            helperVaccine.decreaseAvailableDoses(1);
            // Update Appointment
            PreparedStatement APStatement = con.prepareStatement(addAppointment);
            APStatement.setInt(1, currentID);
            APStatement.setString(2, patient);
            APStatement.setString(3, caregiver);
            APStatement.setString(4, helperVaccine.getVaccineName());
            APStatement.setDate(5, d);
            APStatement.executeUpdate();
            System.out.println("Appointment ID "+currentID+", Caregiver username "+caregiver);
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
        } catch (SQLException e) {
            System.out.println("PLease try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    } // TODO: Part 2

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // Pre-Checks
        if (currentCaregiver==null && currentPatient==null) {
            System.out.println("Please login first");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Invalid command statement, please try again.");
            return;
        }
        String id = tokens[1];
        // Create SQL Statement
        String getAppointment = "SELECT PName, CName, VName, Time FROM Appointment WHERE id = "+id;
        String removeAppointment = "DELETE FROM Appointment WHERE id = "+id;
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            // Gather appointment information
            PreparedStatement statement = con.prepareStatement(getAppointment);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                System.out.println("Invalid id, please try again.");
                return;
            }
            String PName = resultSet.getString("PName");
            String CName = resultSet.getString("CName");
            String VName = resultSet.getString("VName");
            Date date = resultSet.getDate("Time");
            // Check if appointment belongs to the user logged in
            if (currentPatient != null) {
                if (!currentPatient.getUsername().equals(PName)) {
                    System.out.println("You can only cancel your own schedule!");
                    return;
                }
            }
            if (currentCaregiver != null) {
                if(!currentCaregiver.getUsername().equals(CName)) {
                    System.out.println("You can only cancel your own schedule!");
                    return;
                }
            }
            // Cancel the appointment
            PreparedStatement statement2 = con.prepareStatement(removeAppointment);
            statement2.executeUpdate();
            // Put back vaccine dose
            Vaccine vaccine = new Vaccine.VaccineGetter(VName).get();
            vaccine.increaseAvailableDoses(1);
            // Put back caregiver availability
            String addAvailability = "INSERT INTO Availabilities VALUES (?,?)";
            PreparedStatement statement3 = con.prepareStatement(addAvailability);
            statement3.setDate(1, date);
            statement3.setString(2,CName);
            statement3.executeUpdate();
            // Finish Statement
            System.out.println("Successfully canceled appointment "+id);
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    } // TODO: Extra credit

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // Pre-Check
        if (currentCaregiver==null && currentPatient==null) {
            System.out.println("Please login first");
            return;
        }
        if (tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }
        if (currentCaregiver != null) {
            showAppointmentsHelper("caregiver");
        } else {
            showAppointmentsHelper("patient");
        }
    } // TODO: Part 2

    private static void showAppointmentsHelper(String type) {
        // Set SQL Statement
        String loginAs;
        String toGet;
        String username;
        if (type.equals("caregiver")) {
            loginAs = "CName";
            toGet = "PName";
            username = currentCaregiver.getUsername();
        } else {
            loginAs = "PName";
            toGet = "CName";
            username = currentPatient.getUsername();
        }
        String getAppointment = "SELECT id, VName, Time, "+toGet+" FROM Appointment WHERE "+
                loginAs+" = "+"'"+username+"'";
        // Get data and print data
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            PreparedStatement statement = con.prepareStatement(getAppointment);
            ResultSet resultSet = statement.executeQuery();
            while(resultSet.next()) {
                System.out.println(resultSet.getInt("id")+" "+
                                resultSet.getString("VName")+" "+
                                resultSet.getDate("Time")+" "+
                                resultSet.getString(toGet));
            }
        } catch (SQLException e) {
            System.out.println("PLease try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        if (currentCaregiver==null && currentPatient==null) {
            System.out.println("Please login first");
        } else {
            currentCaregiver = null;
            currentPatient = null;
            System.out.println("Successfully logged out");
        }
    } // TODO: Part 2
}
