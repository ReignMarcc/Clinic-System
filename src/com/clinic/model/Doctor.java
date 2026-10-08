package com.clinic.model;

public class Doctor extends Persons {
    private final String doctorId;
    private String specialization;
    private String shift;
    private boolean isAvailable;

    public Doctor(String doctorId, String name, int age, String contactNumber, String specialization, String shift) {
        super(name, age, contactNumber);
        this.doctorId = doctorId;
        this.specialization = specialization;
        this.shift = shift;
        this.isAvailable = true;
    }

    public String getDoctorId() { return doctorId; }
    public String getSpecialization() { return specialization; }
    public String getShift() { return shift; }
    public boolean isAvailable() { return isAvailable; }

    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public void setShift(String shift) { this.shift = shift; }
    public void setAvailable(boolean available) { isAvailable = available; }

    @Override
    public String displayInfo() {
        String status = isAvailable ? "Available" : "On Leave / Busy";
        return "Dr. " + name + " (" + specialization + ") | ID: " + doctorId +
                " | Shift: " + shift + " | Status: " + status;
    }
}