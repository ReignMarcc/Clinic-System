package com.clinic.exception;

public class DuplicatePatientException extends Exception {
    public DuplicatePatientException(String message) {
        super(message);
    }
}