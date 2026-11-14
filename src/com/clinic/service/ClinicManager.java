package com.clinic.service;

import com.clinic.exception.*;
import com.clinic.model.*;
import com.clinic.file.ActivityFileHandler;
import com.clinic.file.AppointmentFileHandler;
import com.clinic.file.DiagnosisFileHandler;
import com.clinic.file.DoctorFileHandler;
import com.clinic.file.PatientFileHandler;
import com.clinic.util.InputValidator;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ClinicManager {
    private final ArrayList<Patient> patients = new ArrayList<>();
    private final ArrayList<Doctor> doctors = new ArrayList<>();
    private final ArrayList<Appointment> appointments = new ArrayList<>();
    private final Random random = new Random();
    private int nextAppointmentNo = 1001; // counter, so IDs never repeat after removals

    private final ArrayList<ActivityEntry> activityLog = new ArrayList<>();
    private final ActivityFileHandler activityFile;
    private final DoctorFileHandler doctorFile;
    private final PatientFileHandler patientFile;
    private final DiagnosisFileHandler diagnosisFile;
    private final AppointmentFileHandler appointmentFile;

    public ClinicManager() {
        this(new DoctorFileHandler(), new PatientFileHandler(), new DiagnosisFileHandler(), new AppointmentFileHandler(),
                new ActivityFileHandler());
    }

    public ClinicManager(Path dataDir) {
        this(new DoctorFileHandler(dataDir.resolve(DoctorFileHandler.FILE_NAME)),
                new PatientFileHandler(dataDir.resolve(PatientFileHandler.FILE_NAME)),
                new DiagnosisFileHandler(dataDir.resolve(DiagnosisFileHandler.FILE_NAME)),
                new AppointmentFileHandler(dataDir.resolve(AppointmentFileHandler.FILE_NAME)),
                new ActivityFileHandler(dataDir.resolve(ActivityFileHandler.FILE_NAME)));
    }

    private ClinicManager(DoctorFileHandler doctorFile, PatientFileHandler patientFile,
                          DiagnosisFileHandler diagnosisFile, AppointmentFileHandler appointmentFile,
                          ActivityFileHandler activityFile) {
        this.activityFile = activityFile;
        this.doctorFile = doctorFile;
        this.patientFile = patientFile;
        this.diagnosisFile = diagnosisFile;
        this.appointmentFile = appointmentFile;

        loadFromFiles();
    }

    private void loadFromFiles() {
        System.out.println("Patient data is saved in: " + patientFile.getFile().getParent());
        try {
            doctors.addAll(doctorFile.load());   // doctors come only from doctors.csv; a new clinic starts with none
        } catch (Exception e) {
            System.err.println("Could not load " + doctorFile.getFile() + ": " + e.getMessage());
        }
        try {
            activityLog.addAll(activityFile.load());
        } catch (Exception e) {
            System.err.println("Could not load " + activityFile.getFile() + ": " + e.getMessage());
        }
        try {
            patients.addAll(patientFile.load());
        } catch (Exception e) {
            System.err.println("Could not load " + patientFile.getFile() + ": " + e.getMessage());
        }
        try {
            diagnosisFile.loadInto(patients);
        } catch (Exception e) {
            System.err.println("Could not load " + diagnosisFile.getFile() + ": " + e.getMessage());
        }
        try {
            for (Appointment a : appointmentFile.load()) {
                if (isPatientIdExists(a.getPatientId())) {
                    appointments.add(a);
                    nextAppointmentNo = Math.max(nextAppointmentNo, appointmentNumber(a.getAppointmentId()) + 1);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load " + appointmentFile.getFile() + ": " + e.getMessage());
        }
        System.out.println("Loaded " + doctors.size() + " doctors, " + patients.size() + " patients, " + appointments.size() + " appointments");
        refreshAppointmentStatuses();
    }

    private int appointmentNumber(String appointmentId) {
        try {
            return Integer.parseInt(appointmentId.substring(1));
        } catch (Exception e) {
            return 0;
        }
    }

    private void saveAppointments() {
        try {
            appointmentFile.save(appointments);
        } catch (Exception e) {
            System.err.println("Could not save " + appointmentFile.getFile() + ": " + e.getMessage());
        }
    }

    private void savePatients() {
        try {
            patientFile.save(patients);
        } catch (Exception e) {
            System.err.println("Could not save " + patientFile.getFile() + ": " + e.getMessage());
        }
    }

    private void saveDiagnoses() {
        try {
            diagnosisFile.save(patients);
        } catch (Exception e) {
            System.err.println("Could not save " + diagnosisFile.getFile() + ": " + e.getMessage());
        }
    }

    // --- Private Helpers ---
    private Patient locatePatient(String id) throws PatientNotFoundException {
        for (Patient p : patients) {
            if (p.getPatientId().equalsIgnoreCase(id)) return p;
        }
        throw new PatientNotFoundException("Patient with ID " + id + " not found!");
    }

    private Doctor locateDoctor(String id) throws InvalidInputException {
        for (Doctor d : doctors) {
            if (d.getDoctorId().equalsIgnoreCase(id)) return d;
        }
        throw new InvalidInputException("Doctor with ID " + id + " not found!");
    }

    private static final int DOCTOR_BUFFER_MINUTES = 30;       // gap required before an existing appointment
    private static final int CONSULTATION_MINUTES = 60;        // each appointment occupies a full hour

    /**
     * The doctor must exist, be available, and have no appointment whose reserved
     * window (30 minutes before it through the end of its 1-hour consultation)
     * overlaps the proposed time. Checked in both directions so the result doesn't
     * depend on which appointment happened to be booked first.
     */
    private void ensureDoctorFree(String doctorId, LocalDateTime when) throws InvalidInputException {
        Doctor d = locateDoctor(doctorId);
        if (!d.isAvailable()) {
            throw new InvalidInputException(d.getName() + " is not available!");
        }
        for (Appointment a : appointments) {
            if (a.getStatus() == AppointmentStatus.CANCELLED) continue;
            if (!doctorId.equalsIgnoreCase(a.getDoctorId())) continue;

            LocalDateTime existing = a.getDateTime();
            boolean newFallsInExistingWindow = !when.isBefore(existing.minusMinutes(DOCTOR_BUFFER_MINUTES))
                    && when.isBefore(existing.plusMinutes(CONSULTATION_MINUTES));
            boolean existingFallsInNewWindow = !existing.isBefore(when.minusMinutes(DOCTOR_BUFFER_MINUTES))
                    && existing.isBefore(when.plusMinutes(CONSULTATION_MINUTES));

            if (newFallsInExistingWindow || existingFallsInNewWindow) {
                throw new InvalidInputException(d.getName() + " is unavailable around that time (existing appointment at "
                        + existing.toLocalTime() + "). Next available slot: "
                        + existing.plusMinutes(CONSULTATION_MINUTES).toLocalTime() + " onwards.");
            }
        }
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
        return "A" + (nextAppointmentNo++);
    }

    private boolean isPatientIdExists(String id) {
        for (Patient p : patients) {
            if (p.getPatientId().equalsIgnoreCase(id)) return true;
        }
        return false;
    }

    private boolean isDuplicate(String name, String contact) {
        for (Patient p : patients) {
            if (InputValidator.normalizeSpaces(p.getName()).equalsIgnoreCase(InputValidator.normalizeSpaces(name))
                    && p.getContactNumber() != null && p.getContactNumber().equals(contact)) {
                return true;
            }
        }
        return false;
    }

    // --- Activity log (feeds the report) ---

    private static final String ACTOR_ADMIN = "Admin";
    private static final String ACTOR_SYSTEM = "System";
    private static final DateTimeFormatter LOG_WHEN = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    /** Newest last. Returns a copy, sorted by time. */
    public List<ActivityEntry> getActivityLog() {
        refreshAppointmentStatuses();
        List<ActivityEntry> copy = new ArrayList<>(activityLog);
        copy.sort(Comparator.comparing(ActivityEntry::getTimestamp));
        return Collections.unmodifiableList(copy);
    }

    private void log(ActivityType type, String patientId, String doctorId, String appointmentId, String details) {
        addLog(LocalDateTime.now(), ACTOR_ADMIN, type, patientId, doctorId, appointmentId, details);
        saveActivity();
    }

    private void addLog(LocalDateTime when, String actor, ActivityType type,
                        String patientId, String doctorId, String appointmentId, String details) {
        activityLog.add(new ActivityEntry(when, actor, type, patientId, doctorId, appointmentId, details));
    }

    /** A failed log write never blocks the real action; it is only reported on the console. */
    private void saveActivity() {
        try {
            activityFile.save(activityLog);
        } catch (IOException e) {
            System.err.println("Could not save " + activityFile.getFile() + ": " + e.getMessage());
        }
    }

    private String patientNameOrId(String patientId) {
        for (Patient p : patients) if (p.getPatientId().equalsIgnoreCase(patientId)) return p.getName();
        return patientId;
    }

    private String doctorNameOrNone(String doctorId) {
        if (doctorId == null || doctorId.isEmpty()) return "no doctor assigned";
        Doctor d = getDoctorById(doctorId);
        return d == null ? "doctor " + doctorId : d.getName();
    }

    // --- Feature 1: Patient Registration ---

    /**
     * Shared validation and creation logic for every "new patient (+ optional
     * appointment)" flow. enforceFutureDay and enforceClinicHours let each
     * public entry point (walk-in vs. scheduled appointment) apply only the
     * rules that make sense for it.
     */
    private Patient registerPatientCore(String name, int age, String contact, String doctorId,
                                        LocalDateTime schedule, String reason,
                                        boolean enforceFutureDay, boolean enforceClinicHours)
            throws InvalidInputException, DuplicatePatientException, PatientNotFoundException {
        name = InputValidator.normalizeSpaces(name);

        // Validate everything first, so nothing is stored when an input is bad
        if (!InputValidator.isValidName(name)) throw new InvalidInputException("Name must contain letters only (2-50 characters)!");
        if (!InputValidator.isValidAge(age)) throw new InvalidInputException("Age must be between " + InputValidator.MIN_AGE + " and " + InputValidator.MAX_AGE + "!");
        if (!InputValidator.isValidContact(contact)) throw new InvalidInputException("Contact must start with 09 and be 11 digits!");
        if (isDuplicate(name, contact)) throw new DuplicatePatientException("Patient with same name and contact already exists!");

        if (schedule != null) {
            if (reason != null && !InputValidator.isWithinLength(reason, InputValidator.MAX_REASON_LENGTH)) {
                throw new InvalidInputException("Reason is too long (max " + InputValidator.MAX_REASON_LENGTH + " characters)!");
            }
            if (enforceFutureDay && !schedule.toLocalDate().isAfter(LocalDate.now())) {
                throw new InvalidInputException("Appointments must be booked for a following day, not today.");
            }
            if (enforceClinicHours) {
                java.time.LocalTime t = schedule.toLocalTime();
                if (t.isBefore(InputValidator.CLINIC_OPEN) || !t.isBefore(InputValidator.CLINIC_CLOSE)) {
                    throw new InvalidInputException("Clinic hours are " + InputValidator.CLINIC_OPEN + " to " + InputValidator.CLINIC_CLOSE + ".");
                }
            }
            if (doctorId != null && !doctorId.isEmpty()) ensureDoctorFree(doctorId, schedule);
        }

        Patient p = new Patient(generatePatientId(), name, age, contact);
        patients.add(p);
        savePatients();
        log(ActivityType.PATIENT_REGISTERED, p.getPatientId(), "", "",
                "Registered " + name + " (age " + age + ")");

        if (schedule != null) {
            scheduleAppointment(p.getPatientId(), doctorId, schedule, reason);
        }
        return p;
    }

    /**
     * Registers a walk-in patient. The visit time is fixed to right now (when they
     * arrived and started waiting), so there's no date/time to enter - just who they
     * are, which doctor will see them, and their reason for the visit.
     */
    public Patient registerWalkIn(String name, int age, String contact, String doctorId, String reason)
            throws InvalidInputException, DuplicatePatientException, PatientNotFoundException {
        if (!InputValidator.isValidReason(reason)) throw new InvalidInputException("Please enter a reason for the visit.");
        return registerPatientCore(name, age, contact, doctorId, LocalDateTime.now(), reason, false, false);
    }

    /**
     * Registers a patient with a scheduled appointment for a following day (same-day
     * booking isn't allowed). Requires a doctor up front so the time picker can be
     * checked against that doctor's existing schedule.
     */
    public Patient registerAppointment(String name, int age, String contact, String doctorId,
                                       LocalDateTime schedule, String reason)
            throws InvalidInputException, DuplicatePatientException, PatientNotFoundException {
        if (!InputValidator.isValidReason(reason)) throw new InvalidInputException("Please enter a reason for the visit.");
        if (doctorId == null || doctorId.isEmpty()) throw new InvalidInputException("Please select a doctor first.");
        return registerPatientCore(name, age, contact, doctorId, schedule, reason, true, true);
    }

    // Old signatures kept so any other existing callers keep working.
    public Patient registerPatient(String name, int age, String contact, LocalDateTime schedule, String reason)
            throws InvalidInputException, DuplicatePatientException, PatientNotFoundException {
        return registerPatientCore(name, age, contact, null, schedule, reason, false, true);
    }

    public Patient registerPatient(String name, int age, String contact, String doctorId,
                                   LocalDateTime schedule, String reason)
            throws InvalidInputException, DuplicatePatientException, PatientNotFoundException {
        return registerPatientCore(name, age, contact, doctorId, schedule, reason, false, true);
    }

    // Extra: Emergency Registration (Name only)
    public Patient registerEmergencyPatient(String name) throws InvalidInputException, PatientNotFoundException {
        if (!InputValidator.isNotBlank(name)) throw new InvalidInputException("Name cannot be blank!");
        if (InputValidator.normalizeSpaces(name).length() > InputValidator.MAX_NAME_LENGTH) throw new InvalidInputException("Name is too long (max " + InputValidator.MAX_NAME_LENGTH + " characters)!");

        String pId = generatePatientId();
        Patient p = new Patient(pId, InputValidator.normalizeSpaces(name));
        patients.add(p);
        savePatients();
        log(ActivityType.PATIENT_REGISTERED, pId, "", "", "Registered " + p.getName() + " (emergency walk-in)");

        // Auto-create appointment for today
        scheduleAppointment(pId, LocalDateTime.now(), "Emergency Walk-in");
        return p;
    }

    // --- Feature 2: Record Edits ---
    public void updateRecord(String patientId, int age, String diagnosis, String notes, String contact)
            throws PatientNotFoundException, InvalidInputException {
        Patient p = locatePatient(patientId);
        contact = contact == null ? null : contact.trim();

        if (contact != null && !contact.isEmpty() && !InputValidator.isValidContact(contact)) {
            throw new InvalidInputException("Invalid contact number format!");
        }
        if (age != 0 && !InputValidator.isValidAge(age)) {
            throw new InvalidInputException("Age must be between " + InputValidator.MIN_AGE + " and " + InputValidator.MAX_AGE + "!");
        }
        if (!InputValidator.isValidNotes(diagnosis) || !InputValidator.isValidNotes(notes)) {
            throw new InvalidInputException("Diagnosis and notes must be at most " + InputValidator.MAX_NOTES_LENGTH + " characters!");
        }

        List<String> changed = new ArrayList<>();
        if (age != 0 && age != p.getAge()) changed.add("age");
        if (InputValidator.isNotBlank(contact) && !contact.equals(p.getContactNumber())) changed.add("contact number");
        if (InputValidator.isNotBlank(diagnosis) && !diagnosis.equals(p.getDiagnosis())) changed.add("diagnosis");
        if (InputValidator.isNotBlank(notes) && !notes.equals(p.getNotes())) changed.add("notes");

        if (age != 0) p.setAge(age);
        if (InputValidator.isNotBlank(contact)) p.setContactNumber(contact);
        if (InputValidator.isNotBlank(diagnosis)) p.setDiagnosis(diagnosis);
        if (InputValidator.isNotBlank(notes)) p.setNotes(notes);

        savePatients();
        saveDiagnoses();
        if (!changed.isEmpty()) {
            log(ActivityType.RECORD_UPDATED, p.getPatientId(), "", "",
                    "Updated " + p.getName() + "'s " + String.join(", ", changed));
        }
    }

    // --- Feature 3: Cancellation & Removal ---
    public void scheduleAppointment(String patientId, LocalDateTime dateTime, String reason) throws PatientNotFoundException {
        scheduleAppointment(patientId, "", dateTime, reason);
    }

    public void scheduleAppointment(String patientId, String doctorId, LocalDateTime dateTime, String reason)
            throws PatientNotFoundException {
        Patient patient = locatePatient(patientId); // Ensure patient exists
        String aId = generateAppointmentId();
        appointments.add(new Appointment(aId, patientId, doctorId, dateTime, reason));
        saveAppointments();
        log(ActivityType.APPOINTMENT_BOOKED, patient.getPatientId(), doctorId, aId,
                "Booked " + patient.getName() + " with " + doctorNameOrNone(doctorId)
                        + " for " + dateTime.format(LOG_WHEN) + (reason == null || reason.isEmpty() ? "" : " - " + reason));
    }

    public void cancelAppointment(String appointmentId) throws AppointmentNotFoundException, InvalidInputException {
        for (Appointment a : appointments) {
            if (a.getAppointmentId().equalsIgnoreCase(appointmentId)) {
                if (a.getStatus() == AppointmentStatus.COMPLETED) {
                    throw new InvalidInputException("This appointment is already completed and can't be cancelled.");
                }
                if (a.getStatus() == AppointmentStatus.CANCELLED) {
                    throw new InvalidInputException("This appointment is already cancelled.");
                }
                a.setStatus(AppointmentStatus.CANCELLED);
                saveAppointments();
                log(ActivityType.APPOINTMENT_CANCELLED, a.getPatientId(), a.getDoctorId(), a.getAppointmentId(),
                        "Cancelled " + patientNameOrId(a.getPatientId()) + "'s appointment with "
                                + doctorNameOrNone(a.getDoctorId()) + " (" + a.getDateTime().format(LOG_WHEN) + ")");
                return;
            }
        }
        throw new AppointmentNotFoundException("Appointment " + appointmentId + " not found!");
    }

    /** Marks an appointment completed. Done manually by staff once the consultation is finished. */
    public void completeAppointment(String appointmentId) throws AppointmentNotFoundException, InvalidInputException {
        for (Appointment a : appointments) {
            if (a.getAppointmentId().equalsIgnoreCase(appointmentId)) {
                if (a.getStatus() == AppointmentStatus.CANCELLED) {
                    throw new InvalidInputException("A cancelled appointment can't be marked completed.");
                }
                if (a.getStatus() == AppointmentStatus.COMPLETED) {
                    throw new InvalidInputException("This appointment is already completed.");
                }
                BillingService.getInstance().requirePaid(a); // a visit can't be completed until its bill is paid
                a.setStatus(AppointmentStatus.COMPLETED);
                saveAppointments();
                log(ActivityType.APPOINTMENT_COMPLETED, a.getPatientId(), a.getDoctorId(), a.getAppointmentId(),
                        "Completed " + patientNameOrId(a.getPatientId()) + "'s appointment with "
                                + doctorNameOrNone(a.getDoctorId()));
                return;
            }
        }
        throw new AppointmentNotFoundException("Appointment " + appointmentId + " not found!");
    }

    /**
     * Moves any PENDING appointment whose time has arrived to ON_GOING.
     * Call this before reading appointments, and periodically from the UI
     * (e.g. a Swing Timer) so statuses update even while a page sits open.
     * Returns true if anything changed.
     */
    public boolean refreshAppointmentStatuses() {
        boolean changed = false;
        LocalDateTime now = LocalDateTime.now();
        for (Appointment a : appointments) {
            if (a.getStatus() == AppointmentStatus.PENDING && !a.getDateTime().isAfter(now)) {
                a.setStatus(AppointmentStatus.ON_GOING);
                changed = true;
                // stamped with the scheduled time (when it really started), even if the app noticed later
                addLog(a.getDateTime(), ACTOR_SYSTEM, ActivityType.APPOINTMENT_STARTED,
                        a.getPatientId(), a.getDoctorId(), a.getAppointmentId(),
                        patientNameOrId(a.getPatientId()) + "'s appointment with "
                                + doctorNameOrNone(a.getDoctorId()) + " is now on-going");
            }
        }
        if (changed) {
            saveAppointments();
            saveActivity();
        }
        return changed;
    }

    public void removePatient(String patientId) throws PatientNotFoundException {
        Patient p = locatePatient(patientId);
        int removedAppointments = 0;
        for (Appointment a : appointments) if (a.getPatientId().equalsIgnoreCase(patientId)) removedAppointments++;
        patients.remove(p);
        appointments.removeIf(a -> a.getPatientId().equalsIgnoreCase(patientId)); // Purge appointments
        savePatients();
        saveDiagnoses();
        saveAppointments();
        log(ActivityType.PATIENT_REMOVED, p.getPatientId(), "", "",
                "Removed " + p.getName() + " and " + removedAppointments + " appointment"
                        + (removedAppointments == 1 ? "" : "s"));
    }

    // --- Feature 4: Patient Search ---
    public Patient searchById(String patientId) throws PatientNotFoundException {
        return locatePatient(patientId);
    }

    public List<Patient> searchByName(String partialName) {
        List<Patient> result = new ArrayList<>();
        if (partialName == null) return result;
        String needle = partialName.trim().toLowerCase();
        for (Patient p : patients) {
            if (p.getName().toLowerCase().contains(needle)) {
                result.add(p);
            }
        }
        return result;
    }

    // --- Feature 5: Pending Appointments ---
    public List<Appointment> getPendingAppointments() {
        refreshAppointmentStatuses();
        List<Appointment> pending = new ArrayList<>();
        for (Appointment a : appointments) {
            if (a.getStatus() == AppointmentStatus.PENDING) {
                pending.add(a);
            }
        }
        pending.sort(Comparator.comparing(Appointment::getDateTime));
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
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");

        List<Appointment> today = new ArrayList<>();
        for (Appointment a : appointments) {
            if (a.getDateTime().toLocalDate().equals(date)) today.add(a);
        }
        today.sort(Comparator.comparing(Appointment::getDateTime));

        for (Appointment a : today) {
            sb.append("[").append(a.getDateTime().format(timeFmt)).append("] ")
                    .append("App ID: ").append(a.getAppointmentId())
                    .append(" | Patient ID: ").append(a.getPatientId())
                    .append(" | Status: ").append(a.getStatus())
                    .append(" | Reason: ").append(a.getReason()).append("\n");
        }
        if (today.isEmpty()) sb.append("No appointments scheduled for this date.\n");
        return sb.toString();
    }

    /**
     * Patients who are "present" today: a non-cancelled appointment (scheduled visit
     * or walk-in) dated today. A patient drops out of this list the moment their
     * today's appointment is cancelled.
     */
    public List<Patient> getActiveTodayPatients() {
        refreshAppointmentStatuses();
        LocalDate today = LocalDate.now();
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        for (Appointment a : appointments) {
            if (a.getDateTime().toLocalDate().equals(today) && a.getStatus() != AppointmentStatus.CANCELLED) {
                ids.add(a.getPatientId().toUpperCase());
            }
        }
        List<Patient> result = new ArrayList<>();
        for (Patient p : patients) {
            if (ids.contains(p.getPatientId().toUpperCase())) result.add(p);
        }
        return result;
    }

    // --- Lookups used by the Search and Records pages ---

    /** Returns the doctor with this ID, or null when there is none. */
    public Doctor getDoctorById(String id) {
        if (id == null) return null;
        for (Doctor d : doctors) {
            if (d.getDoctorId().equalsIgnoreCase(id)) return d;
        }
        return null;
    }

    /** All appointments of one patient, oldest first. */
    public List<Appointment> getAppointmentsForPatient(String patientId) {
        refreshAppointmentStatuses();
        List<Appointment> list = new ArrayList<>();
        for (Appointment a : appointments) {
            if (a.getPatientId().equalsIgnoreCase(patientId)) list.add(a);
        }
        list.sort(Comparator.comparing(Appointment::getDateTime));
        return list;
    }

    /** All appointments of one doctor, oldest first. */
    public List<Appointment> getAppointmentsForDoctor(String doctorId) {
        refreshAppointmentStatuses();
        List<Appointment> list = new ArrayList<>();
        for (Appointment a : appointments) {
            if (doctorId != null && a.getDoctorId().equalsIgnoreCase(doctorId)) list.add(a);
        }
        list.sort(Comparator.comparing(Appointment::getDateTime));
        return list;
    }

    /** Soonest pending appointment from today onwards, or null. */
    public Appointment getNextAppointment(String patientId) {
        LocalDate today = LocalDate.now();
        for (Appointment a : getAppointmentsForPatient(patientId)) {
            if (a.getStatus() == AppointmentStatus.PENDING && !a.getDateTime().toLocalDate().isBefore(today)) {
                return a;
            }
        }
        return null;
    }

    /** Doctor of the next appointment, otherwise of the latest appointment that had one; null if none. */
    public Doctor getAssignedDoctor(String patientId) {
        Appointment next = getNextAppointment(patientId);
        if (next != null) {
            Doctor d = getDoctorById(next.getDoctorId());
            if (d != null) return d;
        }
        List<Appointment> all = getAppointmentsForPatient(patientId);
        for (int i = all.size() - 1; i >= 0; i--) {
            Doctor d = getDoctorById(all.get(i).getDoctorId());
            if (d != null) return d;
        }
        return null;
    }

    /** Today's appointments for a doctor, not counting cancelled ones. */
    public int countAppointmentsToday(String doctorId) {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (Appointment a : appointments) {
            if (doctorId != null && a.getDoctorId().equalsIgnoreCase(doctorId)
                    && a.getStatus() != AppointmentStatus.CANCELLED
                    && a.getDateTime().toLocalDate().equals(today)) {
                count++;
            }
        }
        return count;
    }

    /** Distinct patients who have (or had) an appointment with this doctor. */
    public List<Patient> getPatientsOfDoctor(String doctorId) {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        for (Appointment a : getAppointmentsForDoctor(doctorId)) ids.add(a.getPatientId().toUpperCase());
        List<Patient> result = new ArrayList<>();
        for (String id : ids) {
            for (Patient p : patients) {
                if (p.getPatientId().equalsIgnoreCase(id)) {
                    result.add(p);
                    break;
                }
            }
        }
        return result;
    }

    /** Saves diagnosis and notes. Unlike updateRecord, blank text clears the field. */
    public void updateMedicalNotes(String patientId, String diagnosis, String notes)
            throws PatientNotFoundException, InvalidInputException {
        Patient p = locatePatient(patientId);
        String d = diagnosis == null ? "" : diagnosis.trim();
        String n = notes == null ? "" : notes.trim();
        if (!InputValidator.isValidNotes(d) || !InputValidator.isValidNotes(n)) {
            throw new InvalidInputException("Diagnosis and notes must be at most "
                    + InputValidator.MAX_NOTES_LENGTH + " characters each!");
        }
        boolean diagChanged = !d.equals(p.getDiagnosis());
        boolean notesChanged = !n.equals(p.getNotes());
        p.setDiagnosis(d);
        p.setNotes(n);
        saveDiagnoses();
        if (diagChanged || notesChanged) {
            log(ActivityType.NOTES_UPDATED, p.getPatientId(), "", "",
                    "Updated " + p.getName() + "'s " + (diagChanged && notesChanged ? "diagnosis and notes"
                            : diagChanged ? "diagnosis" : "notes"));
        }
    }

    // --- Doctor management (Settings > Doctors) ---

    /** Doctors are stored with the "Dr." prefix, like the rest of the app expects: "Dr. Juan Cruz". */
    private static String cleanDoctorName(String raw) throws InvalidInputException {
        String bare = InputValidator.normalizeSpaces(raw);
        if (bare.regionMatches(true, 0, "Dr.", 0, 3)) bare = bare.substring(3).trim();
        if (!InputValidator.isValidName(bare)) {
            throw new InvalidInputException("Doctor name must contain letters only (2-" + InputValidator.MAX_NAME_LENGTH + " characters)!");
        }
        String full = "Dr. " + bare;
        if (full.length() > InputValidator.MAX_NAME_LENGTH) {
            throw new InvalidInputException("Doctor name is too long (max " + InputValidator.MAX_NAME_LENGTH + " characters including \"Dr.\")!");
        }
        return full;
    }

    private static String cleanLicense(String raw) throws InvalidInputException {
        String id = raw == null ? "" : raw.trim();
        if (!InputValidator.isValidLicenseId(id)) {
            throw new InvalidInputException("Doctor ID (license number) must be " + InputValidator.MIN_LICENSE_LENGTH + "-"
                    + InputValidator.MAX_LICENSE_LENGTH + " characters: letters, numbers and hyphens only.");
        }
        return id.toUpperCase();
    }

    private static void checkDoctorAge(int age) throws InvalidInputException {
        if (!InputValidator.isValidDoctorAge(age)) {
            throw new InvalidInputException("Doctor age must be between " + InputValidator.MIN_DOCTOR_AGE + " and " + InputValidator.MAX_AGE + "!");
        }
    }

    private static String cleanDoctorContact(String raw) throws InvalidInputException {
        String contact = raw == null ? "" : raw.trim();
        if (!InputValidator.isValidContact(contact)) throw new InvalidInputException("Contact must start with 09 and be 11 digits!");
        return contact;
    }

    private static String cleanSpecialization(String raw) throws InvalidInputException {
        if (!InputValidator.isValidSpecialization(raw)) {
            throw new InvalidInputException("Please enter a valid specialization (letters only, 2-" + InputValidator.MAX_SPECIALIZATION_LENGTH + " characters).");
        }
        return InputValidator.normalizeSpaces(raw);
    }

    private static String canonicalShift(String raw) throws InvalidInputException {
        if (raw != null) {
            for (String s : Doctor.SHIFTS) if (s.equalsIgnoreCase(raw.trim())) return s;
        }
        throw new InvalidInputException("Please choose a shift: " + String.join(" or ", Doctor.SHIFTS) + ".");
    }

    private boolean isDoctorDuplicate(String fullName, String contact, Doctor ignore) {
        for (Doctor d : doctors) {
            if (d == ignore) continue;
            if (InputValidator.normalizeSpaces(d.getName()).equalsIgnoreCase(fullName)
                    && contact.equals(d.getContactNumber())) return true;
        }
        return false;
    }

    /** Throws instead of printing when saving fails, so the screen never says "saved" when it wasn't. */
    private void saveDoctorsOrThrow() {
        try {
            doctorFile.save(doctors);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save " + doctorFile.getFile() + ": " + e.getMessage(), e);
        }
    }

    private Doctor findDoctor(String id) throws DoctorNotFoundException {
        Doctor d = getDoctorById(id == null ? null : id.trim());
        if (d == null) throw new DoctorNotFoundException("Doctor with ID " + id + " not found!");
        return d;
    }

    /** The doctorId is the doctor's license number. It is typed in, must be unique, and can never be changed afterwards. */
    public Doctor addDoctor(String doctorId, String name, int age, String contact,
                            String specialization, String shift)
            throws InvalidInputException, DuplicateDoctorException {
        String id = cleanLicense(doctorId);
        String fullName = cleanDoctorName(name);
        checkDoctorAge(age);
        String phone = cleanDoctorContact(contact);
        String spec = cleanSpecialization(specialization);
        String shiftName = canonicalShift(shift);

        if (getDoctorById(id) != null) {
            throw new DuplicateDoctorException("A doctor with ID " + id + " is already registered!");
        }
        if (isDoctorDuplicate(fullName, phone, null)) {
            throw new DuplicateDoctorException("A doctor with the same name and contact number already exists!");
        }

        Doctor d = new Doctor(id, fullName, age, phone, spec, shiftName);
        doctors.add(d);
        try {
            saveDoctorsOrThrow();
        } catch (RuntimeException e) {
            doctors.remove(d);
            throw e;
        }
        log(ActivityType.DOCTOR_ADDED, "", d.getDoctorId(), "",
                "Added " + d.getName() + " (" + d.getSpecialization() + ", " + d.getShift() + ")");
        return d;
    }

    /** Edits everything except the doctor ID. */
    public void updateDoctor(String doctorId, String name, int age, String contact,
                             String specialization, String shift)
            throws DoctorNotFoundException, InvalidInputException, DuplicateDoctorException {
        Doctor d = findDoctor(doctorId);
        String fullName = cleanDoctorName(name);
        checkDoctorAge(age);
        String phone = cleanDoctorContact(contact);
        String spec = cleanSpecialization(specialization);
        String shiftName = canonicalShift(shift);
        if (isDoctorDuplicate(fullName, phone, d)) {
            throw new DuplicateDoctorException("A doctor with the same name and contact number already exists!");
        }

        String oldName = d.getName(), oldContact = d.getContactNumber(), oldSpec = d.getSpecialization(), oldShift = d.getShift();
        int oldAge = d.getAge();
        d.setName(fullName);
        d.setAge(age);
        d.setContactNumber(phone);
        d.setSpecialization(spec);
        d.setShift(shiftName);
        try {
            saveDoctorsOrThrow();
        } catch (RuntimeException e) {
            d.setName(oldName); d.setAge(oldAge); d.setContactNumber(oldContact);
            d.setSpecialization(oldSpec); d.setShift(oldShift);
            throw e;
        }
        List<String> changed = new ArrayList<>();
        if (!oldName.equals(fullName)) changed.add("name");
        if (oldAge != age) changed.add("age");
        if (!oldContact.equals(phone)) changed.add("contact number");
        if (!oldSpec.equals(spec)) changed.add("specialization");
        if (!oldShift.equals(shiftName)) changed.add("shift");
        if (!changed.isEmpty()) {
            log(ActivityType.DOCTOR_UPDATED, "", d.getDoctorId(), "",
                    "Updated " + d.getName() + "'s " + String.join(", ", changed));
        }
    }

    /** On leave = not offered in Add Appointment / Add Walk-in. Existing appointments are left as they are. */
    public void setDoctorAvailable(String doctorId, boolean available) throws DoctorNotFoundException {
        Doctor d = findDoctor(doctorId);
        boolean old = d.isAvailable();
        d.setAvailable(available);
        try {
            saveDoctorsOrThrow();
        } catch (RuntimeException e) {
            d.setAvailable(old);
            throw e;
        }
        if (old != available) {
            log(available ? ActivityType.DOCTOR_AVAILABLE : ActivityType.DOCTOR_ON_LEAVE, "", d.getDoctorId(), "",
                    d.getName() + (available ? " is available again" : " was set on leave"));
        }
    }

    /** Appointments of this doctor that are still open (pending or on-going). */
    public int countActiveAppointments(String doctorId) {
        refreshAppointmentStatuses();
        int count = 0;
        for (Appointment a : appointments) {
            if (doctorId != null && a.getDoctorId().equalsIgnoreCase(doctorId)
                    && (a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.ON_GOING)) {
                count++;
            }
        }
        return count;
    }

    /** A doctor who still has open appointments cannot be removed. */
    public void removeDoctor(String doctorId) throws DoctorNotFoundException, DoctorInUseException {
        Doctor d = findDoctor(doctorId);
        int open = countActiveAppointments(d.getDoctorId());
        if (open > 0) {
            throw new DoctorInUseException(d.getName() + " still has " + open + " pending or on-going appointment"
                    + (open == 1 ? "" : "s") + ". Cancel or complete "
                    + (open == 1 ? "it" : "them") + " first, then try again.");
        }
        int index = doctors.indexOf(d);
        doctors.remove(d);
        try {
            saveDoctorsOrThrow();
        } catch (RuntimeException e) {
            doctors.add(index, d);
            throw e;
        }
        log(ActivityType.DOCTOR_REMOVED, "", d.getDoctorId(), "", "Removed " + d.getName());
    }

    // Read-only views, so the UI can't change the internal lists by accident
    public List<Patient> getPatients() { return Collections.unmodifiableList(patients); }
    public List<Doctor> getDoctors() { return Collections.unmodifiableList(doctors); }
    public List<Appointment> getAppointments() { refreshAppointmentStatuses(); return Collections.unmodifiableList(appointments); }
}