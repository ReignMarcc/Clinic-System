package com.clinic;

import com.clinic.exception.*;
import com.clinic.model.*;
import com.clinic.service.ClinicManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final ClinicManager clinic = new ClinicManager();
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    public static void main(String[] args) {
        boolean running = true;

        while (running) {
            System.out.println("\n==========================================");
            System.out.println("      CLINIC MANAGEMENT SYSTEM MENU       ");
            System.out.println("==========================================");
            System.out.println("1. Patient Registration (Normal)");
            System.out.println("2. Emergency Registration (Walk-in)");
            System.out.println("3. Medical Record Edits");
            System.out.println("4. Cancellation / Remove Patient");
            System.out.println("5. Patient Search (ID / Name)");
            System.out.println("6. Pending Appointments");
            System.out.println("7. Reports (Master List / Daily)");
            System.out.println("8. Exit");
            System.out.print("Choice: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> registerPatientUI();
                case "2" -> emergencyRegisterUI();
                case "3" -> editRecordUI();
                case "4" -> cancelOrRemoveUI();
                case "5" -> searchUI();
                case "6" -> viewPendingUI();
                case "7" -> reportsUI();
                case "8" -> {
                    running = false;
                    System.out.println("Exiting system. Goodbye!");
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void registerPatientUI() {
        try {
            System.out.print("Enter Name: ");
            String name = scanner.nextLine();
            System.out.print("Enter Age: ");
            int age = Integer.parseInt(scanner.nextLine());
            System.out.print("Enter Contact (09XXXXXXXXX): ");
            String contact = scanner.nextLine();
            System.out.print("Enter Schedule (MM/DD/YYYY HH:mm e.g., 10/15/2026 09:00): ");
            String schedStr = scanner.nextLine();
            System.out.print("Enter Reason for Visit: ");
            String reason = scanner.nextLine();

            LocalDateTime sched = LocalDateTime.parse(schedStr, DATE_FMT);
            Patient p = clinic.registerPatient(name, age, contact, sched, reason);
            System.out.println("\nSUCCESS! Patient Registered. Generated ID: " + p.getPatientId());
        } catch (Exception e) {
            System.out.println("\nERROR: " + e.getMessage());
        }
    }

    private static void emergencyRegisterUI() {
        try {
            System.out.print("Enter Emergency Patient Name: ");
            String name = scanner.nextLine();
            Patient p = clinic.registerEmergencyPatient(name);
            System.out.println("\nEMERGENCY SUCCESS! Patient ID: " + p.getPatientId());
        } catch (Exception e) {
            System.out.println("\nERROR: " + e.getMessage());
        }
    }

    private static void editRecordUI() {
        try {
            System.out.print("Enter Patient ID to edit: ");
            String id = scanner.nextLine();
            System.out.print("New Age (Enter 0 to skip): ");
            int age = Integer.parseInt(scanner.nextLine());
            System.out.print("New Contact (Press Enter to skip): ");
            String contact = scanner.nextLine();
            System.out.print("Diagnosis: ");
            String diag = scanner.nextLine();
            System.out.print("Clinical Notes: ");
            String notes = scanner.nextLine();

            clinic.updateRecord(id, age, diag, notes, contact);
            System.out.println("\nSUCCESS! Record updated.");
        } catch (Exception e) {
            System.out.println("\nERROR: " + e.getMessage());
        }
    }

    private static void cancelOrRemoveUI() {
        System.out.println("1. Cancel Appointment");
        System.out.println("2. Remove Patient Record");
        System.out.print("Choice: ");
        String sub = scanner.nextLine();

        try {
            if (sub.equals("1")) {
                System.out.print("Enter Appointment ID to cancel: ");
                String aId = scanner.nextLine();
                clinic.cancelAppointment(aId);
                System.out.println("\nSUCCESS! Appointment cancelled.");
            } else if (sub.equals("2")) {
                System.out.print("Enter Patient ID to REMOVE: ");
                String pId = scanner.nextLine();
                clinic.removePatient(pId);
                System.out.println("\nSUCCESS! Patient and linked appointments removed.");
            }
        } catch (Exception e) {
            System.out.println("\nERROR: " + e.getMessage());
        }
    }

    private static void searchUI() {
        System.out.println("1. Search by ID");
        System.out.println("2. Search by Partial Name");
        System.out.print("Choice: ");
        String sub = scanner.nextLine();

        try {
            if (sub.equals("1")) {
                System.out.print("Enter Exact Patient ID: ");
                String id = scanner.nextLine();
                Patient p = clinic.searchById(id);
                System.out.println("\nRECORD FOUND:");
                System.out.println(p.displayInfo());
            } else if (sub.equals("2")) {
                System.out.print("Enter Partial Name: ");
                String name = scanner.nextLine();
                List<Patient> list = clinic.searchByName(name);
                System.out.println("\nMATCHING RECORDS (" + list.size() + "):");
                for (Patient p : list) {
                    System.out.println(p.displayInfo());
                }
            }
        } catch (Exception e) {
            System.out.println("\nERROR: " + e.getMessage());
        }
    }

    private static void viewPendingUI() {
        List<Appointment> pending = clinic.getPendingAppointments();
        System.out.println("\n=== PENDING APPOINTMENTS (" + pending.size() + ") ===");
        for (Appointment a : pending) {
            System.out.println("App ID: " + a.getAppointmentId() + " | Patient ID: " + a.getPatientId() +
                    " | Date: " + a.getDateTime() + " | Reason: " + a.getReason());
        }
    }

    private static void reportsUI() {
        System.out.println("1. Master Patient List");
        System.out.println("2. Daily Appointments");
        System.out.print("Choice: ");
        String sub = scanner.nextLine();

        if (sub.equals("1")) {
            System.out.println("\n" + clinic.generateMasterPatientList());
        } else if (sub.equals("2")) {
            System.out.println("\n" + clinic.generateDailyAppointments(LocalDate.now()));
        }
    }
}