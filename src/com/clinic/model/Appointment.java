package com.clinic.model;

import java.time.LocalDateTime;

public class Appointment {
    private final String appointmentId;
    private final String patientId;
    private LocalDateTime dateTime;
    private String reason;
    private AppointmentStatus status;

    public Appointment(String appointmentId, String patientId, LocalDateTime dateTime, String reason) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.dateTime = dateTime;
        this.reason = reason;
        this.status = AppointmentStatus.PENDING; // Default status pagkagagawa
    }

    public String getAppointmentId() { return appointmentId; }
    public String getPatientId() { return patientId; }
    public LocalDateTime getDateTime() { return dateTime; }
    public String getReason() { return reason; }
    public AppointmentStatus getStatus() { return status; }

    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public void setReason(String reason) { this.reason = reason; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
}