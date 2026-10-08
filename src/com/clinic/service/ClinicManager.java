package com.clinic.service;

import com.clinic.exception.*;
import com.clinic.model.*;
import com.clinic.util.InputValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClinicManager {
    private final ArrayList<Patient> patients = new ArrayList<>();
    private final ArrayList<Doctor> doctors = new ArrayList<>();
    private final ArrayList<Appointment> appointments = new ArrayList<>();
    private final Random random = new Random();

    public ClinicManager() {
        // Sample seed data para may maipakitang records agad
        doctors.add(new Doctor("D001", "Dr. Juan Cruz", 45, "09171234567", "General Physician", "Morning Shift"));
        doctors.add(new Doctor("D002", "Dr. Maria Santos", 38, "09189876543", "Pediatrics", "Night Shift"));
    }

    // --- Private Helpers ---
    private Patient locatePatient(String id) throws PatientNotFoundException {
        for (Patient p : patients) {
            if (p.getPatientId().equalsIgnoreCase(id)) return p;
        }
        throw new PatientNotFoundException("Patient with ID " + id + " not found!");
    }

    private String generatePatientId() {
        String id;
        do {
            int num = 10000 + random.nextInt(90000); // 5-digit random
            id = "P" + num;
        } while (isPatientIdExists(id));
        return id;
    }

    private String generateAppointmentId() {
        return "A" + (appointments.size() + 1001);
    }

    private boolean isPatientIdExists(String id) {
        for (Patient p : patients) {
            if (p.getPatientId().equalsIgnoreCase(id)) return true;
        }
        return false;
    }

    private boolean isDuplicate(String name, String contact) {
        for (Patient p : patients) {
            if (p.getName().equalsIgnoreCase(name) && p.getContactNumber().equals(contact)) {
                return true;
            }
        }
        return false;
    }

    // --- Feature 1: Patient Registration ---
    public Patient registerPatient(String name, int age, String contact, LocalDateTime schedule, String reason)
            throws InvalidInputException, DuplicatePatientException, PatientNotFoundException {
        if (!InputValidator.isNotBlank(name)) throw new InvalidInputException("Name cannot be blank!");
        if (!InputValidator.isValidContact(contact)) throw new InvalidInputException("Contact must start with 09 and be 11 digits!");
        if (isDuplicate(name, contact)) throw new DuplicatePatientException("Patient with same name and contact already exists!");

        String pId = generatePatientId();
        Patient p = new Patient(pId, name, age, contact);
        patients.add(p);

        if (schedule != null) {
            scheduleAppointment(pId, schedule, reason);
        }
        return p;
    }

    // Extra: Emergency Registration (Name only)
    public Patient registerEmergencyPatient(String name) throws InvalidInputException, PatientNotFoundException {
        if (!InputValidator.isNotBlank(name)) throw new InvalidInputException("Name cannot be blank!");

        String pId = generatePatientId();
        Patient p = new Patient(pId, name);
        patients.add(p);

        // Auto-create appointment for today
        scheduleAppointment(pId, LocalDateTime.now(), "Emergency Walk-in");
        return p;
    }

    // --- Feature 2: Record Edits ---
    public void updateRecord(String patientId, int age, String diagnosis, String notes, String contact)
            throws PatientNotFoundException, InvalidInputException {
        Patient p = locatePatient(patientId);

        if (contact != null && !contact.isEmpty() && !InputValidator.isValidContact(contact)) {
            throw new InvalidInputException("Invalid contact number format!");
        }

        if (age > 0) p.setAge(age);
        if (InputValidator.isNotBlank(contact)) p.setContactNumber(contact);
        if (InputValidator.isNotBlank(diagnosis)) p.setDiagnosis(diagnosis);
        if (InputValidator.isNotBlank(notes)) p.setNotes(notes);
    }

    // --- Feature 3: Cancellation & Removal ---
    public void scheduleAppointment(String patientId, LocalDateTime dateTime, String reason) throws PatientNotFoundException {
        locatePatient(patientId); // Ensure patient exists
        String aId = generateAppointmentId();
        Appointment app = new Appointment(aId, patientId, dateTime, reason);
        appointments.add(app);
    }

    public void cancelAppointment(String appointmentId) throws AppointmentNotFoundException {
        for (Appointment a : appointments) {
            if (a.getAppointmentId().equalsIgnoreCase(appointmentId)) {
                a.setStatus(AppointmentStatus.CANCELLED);
                return;
            }
        }
        throw new AppointmentNotFoundException("Appointment " + appointmentId + " not found!");
    }

    public void removePatient(String patientId) throws PatientNotFoundException {
        Patient p = locatePatient(patientId);
        patients.remove(p);
        appointments.removeIf(a -> a.getPatientId().equalsIgnoreCase(patientId)); // Purge appointments
    }

    // --- Feature 4: Patient Search ---
    public Patient searchById(String patientId) throws PatientNotFoundException {
        return locatePatient(patientId);
    }

    public List<Patient> searchByName(String partialName) {
        List<Patient> result = new ArrayList<>();
        for (Patient p : patients) {
            if (p.getName().toLowerCase().contains(partialName.toLowerCase())) {
                result.add(p);
            }
        }
        return result;
    }

    // --- Feature 5: Pending Appointments ---
    public List<Appointment> getPendingAppointments() {
        List<Appointment> pending = new ArrayList<>();
        for (Appointment a : appointments) {
            if (a.getStatus() == AppointmentStatus.PENDING) {
                pending.add(a);
            }
        }
        pending.sort((a1, a2) -> a1.getDateTime().compareTo(a2.getDateTime()));
        return pending;
    }

    // --- Feature 6: Report Generation (Returns String) ---
    public String generateMasterPatientList() {
        StringBuilder sb = new StringBuilder("=== MASTER PATIENT LIST ===\n");
        if (patients.isEmpty()) return sb.append("No patient records.\n").toString();

        for (Patient p : patients) {
            sb.append(p.displayInfo()).append("\n");
        }
        return sb.toString();
    }

    public String generateDailyAppointments(LocalDate date) {
        StringBuilder sb = new StringBuilder("=== DAILY APPOINTMENTS (" + date + ") ===\n");
        boolean found = false;
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");

        for (Appointment a : appointments) {
            if (a.getDateTime().toLocalDate().equals(date)) {
                sb.append("[").append(a.getDateTime().format(timeFmt)).append("] ")
                        .append("App ID: ").append(a.getAppointmentId())
                        .append(" | Patient ID: ").append(a.getPatientId())
                        .append(" | Status: ").append(a.getStatus())
                        .append(" | Reason: ").append(a.getReason()).append("\n");
                found = true;
            }
        }
        if (!found) sb.append("No appointments scheduled for this date.\n");
        return sb.toString();
    }

    public List<Patient> getPatients() { return patients; }
    public List<Doctor> getDoctors() { return doctors; }
}