package com.clinic.service;

import com.clinic.model.ActivityEntry;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the plain-text report for a day (or a range of days): summary, what each doctor handled,
 * what happened to each patient, and the activity log. Pure text, no Swing, so it can also be saved as .txt.
 */
public final class ReportGenerator {
    private static final int WIDTH = 78;
    private static final String RULE_HEAVY = repeat('=', WIDTH);
    private static final String RULE_LIGHT = repeat('-', WIDTH);

    private static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a", Locale.ENGLISH);

    private ReportGenerator() {}

    public static String generate(ClinicManager clinic, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) { LocalDate t = from; from = to; to = t; }
        final LocalDate start = from, end = to;
        final boolean multiDay = !start.equals(end);

        // ---- data for the period
        List<Appointment> appts = new ArrayList<>();
        for (Appointment a : clinic.getAppointments()) {
            LocalDate d = a.getDateTime().toLocalDate();
            if (!d.isBefore(start) && !d.isAfter(end)) appts.add(a);
        }
        appts.sort(Comparator.comparing(Appointment::getDateTime));

        List<ActivityEntry> acts = new ArrayList<>();
        for (ActivityEntry e : clinic.getActivityLog()) {
            LocalDate d = e.getTimestamp().toLocalDate();
            if (!d.isBefore(start) && !d.isAfter(end)) acts.add(e);
        }

        Map<String, Patient> patientById = new LinkedHashMap<>();
        for (Patient p : clinic.getPatients()) patientById.put(p.getPatientId().toUpperCase(), p);
        Map<String, Doctor> doctorById = new LinkedHashMap<>();
        for (Doctor d : clinic.getDoctors()) doctorById.put(d.getDoctorId().toUpperCase(), d);

        StringBuilder sb = new StringBuilder();

        // ---- header
        sb.append(RULE_HEAVY).append('\n');
        sb.append(center(multiDay ? "CLINIC MANAGEMENT SYSTEM - ACTIVITY REPORT" : "CLINIC MANAGEMENT SYSTEM - DAILY REPORT")).append('\n');
        sb.append(RULE_HEAVY).append('\n');
        sb.append(multiDay
                ? "Period    : " + start.format(SHORT_DAY) + "  to  " + end.format(SHORT_DAY)
                : "Date      : " + start.format(LONG_DAY)).append('\n');
        sb.append("Generated : ").append(LocalDateTime.now().format(STAMP)).append("\n\n");

        // ---- summary
        int pending = 0, ongoing = 0, completed = 0, cancelled = 0;
        Map<String, Boolean> patientsSeen = new LinkedHashMap<>();
        for (Appointment a : appts) {
            patientsSeen.put(a.getPatientId().toUpperCase(), Boolean.TRUE);
            switch (a.getStatus()) {
                case PENDING: pending++; break;
                case ON_GOING: ongoing++; break;
                case COMPLETED: completed++; break;
                case CANCELLED: cancelled++; break;
                default: break;
            }
        }
        int registered = 0;
        for (ActivityEntry e : acts) if (e.getType() == com.clinic.model.ActivityType.PATIENT_REGISTERED) registered++;

        section(sb, "SUMMARY");
        sb.append(String.format("  Appointments in this period : %d%n", appts.size()));
        sb.append(String.format("    Pending %d  |  On-going %d  |  Completed %d  |  Cancelled %d%n", pending, ongoing, completed, cancelled));
        sb.append(String.format("  Patients with appointments  : %d%n", patientsSeen.size()));
        sb.append(String.format("  New patients registered     : %d%n", registered));
        sb.append(String.format("  Activity entries recorded   : %d%n", acts.size()));
        sb.append(String.format("  Doctors on file             : %d%n%n", doctorById.size()));

        // ---- doctors
        section(sb, "DOCTORS - WHAT EACH DOCTOR HANDLED");
        List<Doctor> doctors = new ArrayList<>(clinic.getDoctors());
        doctors.sort(Comparator.comparing(Doctor::getName, String.CASE_INSENSITIVE_ORDER));
        if (doctors.isEmpty()) sb.append("  No doctors are registered.\n\n");
        for (Doctor d : doctors) {
            List<Appointment> mine = new ArrayList<>();
            for (Appointment a : appts) if (a.getDoctorId().equalsIgnoreCase(d.getDoctorId())) mine.add(a);
            sb.append("  ").append(d.getName()).append("  (ID ").append(d.getDoctorId()).append(")\n");
            sb.append("    ").append(d.getSpecialization()).append(" | ").append(d.getShift())
                    .append(" | ").append(d.isAvailable() ? "Available" : "ON LEAVE").append('\n');
            appointmentBlock(sb, mine, patientById, multiDay, true);
        }
        List<Appointment> noDoctor = new ArrayList<>();
        for (Appointment a : appts) if (!doctorById.containsKey(a.getDoctorId().toUpperCase())) noDoctor.add(a);
        if (!noDoctor.isEmpty()) {
            sb.append("  Unassigned / doctor no longer on file\n");
            appointmentBlock(sb, noDoctor, patientById, multiDay, true);
        }

        // ---- patients
        section(sb, "PATIENTS - WHAT HAPPENED TO EACH PATIENT");
        if (patientsSeen.isEmpty()) sb.append("  No patient had an appointment in this period.\n\n");
        List<Patient> seen = new ArrayList<>();
        for (String id : patientsSeen.keySet()) { Patient p = patientById.get(id); if (p != null) seen.add(p); }
        seen.sort(Comparator.comparing(Patient::getName, String.CASE_INSENSITIVE_ORDER));
        for (Patient p : seen) {
            sb.append("  ").append(p.getName()).append("  (").append(p.getPatientId()).append(")")
                    .append(p.getAge() > 0 ? ", age " + p.getAge() : "")
                    .append(p.getContactNumber().isEmpty() ? "" : ", " + p.getContactNumber()).append('\n');
            for (Appointment a : appts) {
                if (!a.getPatientId().equalsIgnoreCase(p.getPatientId())) continue;
                Doctor d = doctorById.get(a.getDoctorId().toUpperCase());
                sb.append("    ").append(when(a, multiDay)).append("  with ")
                        .append(d != null ? d.getName() : "no doctor assigned").append('\n');
                wrap(sb, "      ", "Reason: " + nz(a.getReason(), "-") + "  [" + a.getStatus() + "]");
            }
            wrap(sb, "    ", "Diagnosis (current record): " + nz(p.getDiagnosis(), "None / pending"));
            wrap(sb, "    ", "Notes (current record): " + nz(p.getNotes(), "None"));
            sb.append('\n');
        }
        int missing = 0;
        for (String id : patientsSeen.keySet()) if (!patientById.containsKey(id)) missing++;
        if (missing > 0) sb.append("  (").append(missing).append(" patient record(s) from this period have since been removed.)\n\n");

        // ---- activity log
        section(sb, "ACTIVITY LOG - EVERYTHING DONE IN THE SYSTEM");
        if (acts.isEmpty()) {
            sb.append("  No activity was recorded in this period.\n");
            sb.append("  (The log only contains actions made after it was added to the system.)\n\n");
        }
        for (ActivityEntry e : acts) {
            String stamp = multiDay ? e.getTimestamp().format(STAMP) : e.getTimestamp().format(TIME);
            sb.append("  [").append(stamp).append("] ").append(e.getActor()).append(" - ").append(e.getType().getLabel()).append('\n');
            wrap(sb, "      ", e.getDetails());
            StringBuilder refs = new StringBuilder();
            if (!e.getPatientId().isEmpty()) refs.append("Patient ").append(e.getPatientId()).append("   ");
            if (!e.getDoctorId().isEmpty()) refs.append("Doctor ").append(e.getDoctorId()).append("   ");
            if (!e.getAppointmentId().isEmpty()) refs.append("Appointment ").append(e.getAppointmentId());
            if (refs.length() > 0) sb.append("      ").append(refs.toString().trim()).append('\n');
        }
        if (!acts.isEmpty()) sb.append('\n');

        sb.append(RULE_HEAVY).append('\n');
        sb.append(center("END OF REPORT")).append('\n');
        return sb.toString();
    }

    // ------------------------------------------------------------------ helpers

    private static void appointmentBlock(StringBuilder sb, List<Appointment> list, Map<String, Patient> patientById,
                                         boolean multiDay, boolean indentMore) {
        if (list.isEmpty()) {
            sb.append("    No appointments in this period.\n\n");
            return;
        }
        int c = 0, o = 0, p = 0, x = 0;
        for (Appointment a : list) {
            if (a.getStatus() == AppointmentStatus.COMPLETED) c++;
            else if (a.getStatus() == AppointmentStatus.ON_GOING) o++;
            else if (a.getStatus() == AppointmentStatus.PENDING) p++;
            else if (a.getStatus() == AppointmentStatus.CANCELLED) x++;
        }
        sb.append(String.format("    Total %d  |  Completed %d  |  On-going %d  |  Pending %d  |  Cancelled %d%n", list.size(), c, o, p, x));
        for (Appointment a : list) {
            Patient pt = patientById.get(a.getPatientId().toUpperCase());
            String who = pt != null ? pt.getName() + " (" + pt.getPatientId() + ")" : a.getPatientId() + " (removed)";
            wrap(sb, "    - ", when(a, multiDay) + " | " + who + " | " + nz(a.getReason(), "-") + " | " + a.getStatus(), "      ");
        }
        sb.append('\n');
    }

    private static String when(Appointment a, boolean multiDay) {
        return multiDay ? a.getDateTime().format(STAMP) : a.getDateTime().format(TIME);
    }

    private static void section(StringBuilder sb, String title) {
        sb.append(RULE_LIGHT).append('\n').append(title).append('\n').append(RULE_LIGHT).append('\n');
    }

    private static String nz(String s, String fallback) {
        return s == null || s.trim().isEmpty() ? fallback : s.trim();
    }

    private static String center(String s) {
        int pad = Math.max(0, (WIDTH - s.length()) / 2);
        return repeat(' ', pad) + s;
    }

    private static String repeat(char c, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(c);
        return b.toString();
    }

    private static void wrap(StringBuilder sb, String indent, String text) {
        wrap(sb, indent, text, indent);
    }

    /** Word-wraps text to the report width; first line uses firstIndent, the rest use nextIndent. */
    private static void wrap(StringBuilder sb, String firstIndent, String text, String nextIndent) {
        String indent = firstIndent;
        StringBuilder line = new StringBuilder(indent);
        boolean empty = true;
        for (String word : text.split("\\s+")) {
            if (word.isEmpty()) continue;
            if (!empty && line.length() + 1 + word.length() > WIDTH) {
                sb.append(line).append('\n');
                indent = nextIndent;
                line = new StringBuilder(indent);
                empty = true;
            }
            if (!empty) line.append(' ');
            line.append(word);
            empty = false;
        }
        sb.append(line).append('\n');
    }
}
