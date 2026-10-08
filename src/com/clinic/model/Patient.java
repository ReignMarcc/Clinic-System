package com.clinic.model;

public class Patient extends Persons {
    private final String patientId; // final, hindi pwedeng palitan
    private String diagnosis;
    private String notes;

    // emergency / walk-in: ID at name lang muna
    public Patient(String patientId, String name) {
        super(name);
        this.patientId = patientId;
        this.diagnosis = "";
        this.notes = "";
    }

    public Patient(String patientId, String name, int age, String contactNumber) {
        super(name, age, contactNumber);
        this.patientId = patientId;
        this.diagnosis = "";
        this.notes = "";
    }

    public String getPatientId() { return patientId; }
    public String getDiagnosis() { return diagnosis; }
    public String getNotes() { return notes; }

    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String displayInfo() {
        String ageText = (age == 0) ? "N/A" : String.valueOf(age); // emergency pa kasi
        String contactText = contactNumber.isEmpty() ? "N/A" : contactNumber;
        return "ID: " + patientId + " | Name: " + name + " | Age: " + ageText
                + " | Contact: " + contactText;
    }
}