package com.clinic.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

public class InputValidator {

    // ---- Limits (adjust to your clinic's rules) ----
    public static final int MIN_AGE = 1;
    public static final int MAX_AGE = 120;
    public static final int MAX_NAME_LENGTH = 50;
    public static final int MAX_REASON_LENGTH = 200;
    public static final int MAX_NOTES_LENGTH = 500;
    public static final LocalTime CLINIC_OPEN = LocalTime.of(7, 0);
    public static final LocalTime CLINIC_CLOSE = LocalTime.of(17, 0);
    public static final int MAX_DAYS_AHEAD = 365;

    // ---- Formats ----
    public static final String BDAY_FORMAT_HINT = "MM/dd/yyyy";
    public static final String DATETIME_FORMAT_HINT = "MM/dd/yyyy HH:mm";

    private static final DateTimeFormatter BDAY_FORMATTER = DateTimeFormatter
            .ofPattern("MM/dd/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter
            .ofPattern("MM/dd/uuuu HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    // Letters (any language), spaces, dots, apostrophes, hyphens. Must start with a letter.
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\p{L}[\\p{L} .'-]*$");
    // [0-9] only, unlike Character.isDigit which accepts non-ASCII digits
    private static final Pattern CONTACT_PATTERN = Pattern.compile("^09[0-9]{9}$");
    private static final Pattern PATIENT_ID_PATTERN = Pattern.compile("^P[0-9]{5}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern APPOINTMENT_ID_PATTERN = Pattern.compile("^A[0-9]{4,}$", Pattern.CASE_INSENSITIVE);

    private InputValidator() {} // utility class, no instances

    // ---------- Basic checks ----------

    public static boolean isNotBlank(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public static boolean isWithinLength(String text, int maxLength) {
        return text != null && text.trim().length() <= maxLength;
    }

    // ---------- Person fields ----------

    public static boolean isValidName(String name) {
        if (!isNotBlank(name)) return false;
        String n = normalizeSpaces(name);
        return n.length() >= 2
                && n.length() <= MAX_NAME_LENGTH
                && NAME_PATTERN.matcher(n).matches();
    }

    public static boolean isValidAge(int age) {
        return age >= MIN_AGE && age <= MAX_AGE;
    }

    public static boolean isValidContact(String contact) {
        return contact != null && CONTACT_PATTERN.matcher(contact).matches();
    }

    public static boolean isValidBirthday(String birthday) {
        if (birthday == null || birthday.length() != 10) return false;
        try {
            LocalDate parsed = LocalDate.parse(birthday, BDAY_FORMATTER);
            return !parsed.isAfter(LocalDate.now())
                    && parsed.isAfter(LocalDate.now().minusYears(MAX_AGE + 1));
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    // ---------- IDs ----------

    public static boolean isValidPatientId(String id) {
        return id != null && PATIENT_ID_PATTERN.matcher(id.trim()).matches();
    }

    public static boolean isValidAppointmentId(String id) {
        return id != null && APPOINTMENT_ID_PATTERN.matcher(id.trim()).matches();
    }

    // ---------- Appointments ----------

    public static boolean isValidReason(String reason) {
        return isNotBlank(reason) && isWithinLength(reason, MAX_REASON_LENGTH);
    }

    /** Future date/time, within clinic hours, and not too far ahead. */
    public static boolean isValidAppointmentTime(LocalDateTime dt) {
        if (dt == null) return false;
        LocalDateTime now = LocalDateTime.now();
        if (dt.isBefore(now)) return false;
        if (dt.isAfter(now.plusDays(MAX_DAYS_AHEAD))) return false;
        LocalTime t = dt.toLocalTime();
        return !t.isBefore(CLINIC_OPEN) && t.isBefore(CLINIC_CLOSE);
    }

    /** Returns a human-readable reason why the time is invalid, or null if it is valid. */
    public static String appointmentTimeError(LocalDateTime dt) {
        if (dt == null) return "Date and time are required.";
        LocalDateTime now = LocalDateTime.now();
        if (dt.isBefore(now)) return "Appointment cannot be in the past.";
        if (dt.isAfter(now.plusDays(MAX_DAYS_AHEAD))) return "Appointment cannot be more than " + MAX_DAYS_AHEAD + " days ahead.";
        LocalTime t = dt.toLocalTime();
        if (t.isBefore(CLINIC_OPEN) || !t.isBefore(CLINIC_CLOSE)) {
            return "Clinic hours are " + CLINIC_OPEN + " to " + CLINIC_CLOSE + ".";
        }
        return null;
    }

    // ---------- Medical text ----------

    public static boolean isValidNotes(String text) {
        return text == null || isWithinLength(text, MAX_NOTES_LENGTH);
    }

    // ---------- Parsing helpers (for Swing text fields / console input) ----------

    /** Returns the age, or -1 if the text is not a valid age. */
    public static int parseAge(String text) {
        if (!isNotBlank(text)) return -1;
        try {
            int age = Integer.parseInt(text.trim());
            return isValidAge(age) ? age : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Parses "MM/dd/yyyy HH:mm". Returns null if the format is wrong. */
    public static LocalDateTime parseDateTime(String text) {
        if (!isNotBlank(text)) return null;
        try {
            return LocalDateTime.parse(text.trim(), DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // ---------- Cleanup ----------

    /** Trims and collapses repeated spaces: "  juan   cruz " -> "juan cruz". */
    public static String normalizeSpaces(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }
}