package com.clinic.model;

import java.time.LocalDateTime;

/** One line of the activity log: what happened, when, and to which patient / doctor / appointment. */
public class ActivityEntry {
    private final LocalDateTime timestamp;
    private final String actor;          // "Admin" for things staff did, "System" for automatic changes
    private final ActivityType type;
    private final String patientId;      // "" when not about a patient
    private final String doctorId;       // "" when not about a doctor
    private final String appointmentId;  // "" when not about an appointment
    private final String details;

    public ActivityEntry(LocalDateTime timestamp, String actor, ActivityType type,
                         String patientId, String doctorId, String appointmentId, String details) {
        this.timestamp = timestamp;
        this.actor = actor == null ? "" : actor;
        this.type = type;
        this.patientId = patientId == null ? "" : patientId;
        this.doctorId = doctorId == null ? "" : doctorId;
        this.appointmentId = appointmentId == null ? "" : appointmentId;
        this.details = details == null ? "" : details;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public String getActor() { return actor; }
    public ActivityType getType() { return type; }
    public String getPatientId() { return patientId; }
    public String getDoctorId() { return doctorId; }
    public String getAppointmentId() { return appointmentId; }
    public String getDetails() { return details; }
}
