package com.clinic.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public class InputValidator {

    private static final DateTimeFormatter BDAY_FORMATTER = DateTimeFormatter
            .ofPattern("MM/dd/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    public static boolean isValidBirthday(String birthday) {
        if (birthday == null || birthday.length() != 10) return false;

        try {
            LocalDate parsedDate = LocalDate.parse(birthday, BDAY_FORMATTER);
            if (parsedDate.isAfter(LocalDate.now())) return false;
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isValidContact(String contact) {
        if (contact == null || contact.length() != 11 || !contact.startsWith("09")) {
            return false;
        }

        for (int i = 0; i < contact.length(); i++) {
            if (!Character.isDigit(contact.charAt(i))) return false;
        }
        return true;
    }

    // Checks if text is not blank
    public static boolean isNotBlank(String text) {
        return text != null && !text.trim().isEmpty();
    }
}