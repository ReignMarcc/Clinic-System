package com.clinic.exception;

public class DoctorInUseException extends Exception {
    public DoctorInUseException(String message) {
        super(message);
    }
}
