package com.clinic.model;

/** Everything the system records in the activity log. */
public enum ActivityType {
    PATIENT_REGISTERED("Patient registered"),
    APPOINTMENT_BOOKED("Appointment booked"),
    APPOINTMENT_STARTED("Appointment started"),
    APPOINTMENT_COMPLETED("Appointment completed"),
    APPOINTMENT_CANCELLED("Appointment cancelled"),
    RECORD_UPDATED("Record updated"),
    NOTES_UPDATED("Diagnosis / notes updated"),
    PATIENT_REMOVED("Patient removed"),
    DOCTOR_ADDED("Doctor added"),
    DOCTOR_UPDATED("Doctor details updated"),
    DOCTOR_ON_LEAVE("Doctor set on leave"),
    DOCTOR_AVAILABLE("Doctor set available"),
    DOCTOR_REMOVED("Doctor removed");

    private final String label;

    ActivityType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
