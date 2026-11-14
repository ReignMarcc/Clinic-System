package com.clinic.ui;

import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Bill;

/** One row of the Payments table: a bill plus the patient and appointment it belongs to. */
final class BillRow {

    static final String PAID = "Paid";
    static final String PENDING = "Pending";
    static final String CANCELLED = "Cancelled";

    private final Bill bill;
    private final Appointment appointment;   // may be null if the appointment was removed
    private final String patientName;
    private final int patientAge;
    private final String status;

    BillRow(Bill bill, Appointment appointment, String patientName, int patientAge) {
        this.bill = bill;
        this.appointment = appointment;
        this.patientName = patientName;
        this.patientAge = patientAge;

        if (bill.isPaid()) status = PAID;
        else if (appointment != null && appointment.getStatus() == AppointmentStatus.CANCELLED) status = CANCELLED;
        else status = PENDING;
    }

    Bill getBill() { return bill; }
    Appointment getAppointment() { return appointment; }
    String getPatientName() { return patientName; }
    int getPatientAge() { return patientAge; }
    String getStatus() { return status; }

    boolean isPaid() { return PAID.equals(status); }
    boolean isPending() { return PENDING.equals(status); }
    boolean isCancelled() { return CANCELLED.equals(status); }

    /** True while the visit is still PENDING or ON_GOING (so it can still be completed). */
    boolean isVisitOpen() {
        return appointment != null && (appointment.getStatus() == AppointmentStatus.PENDING
                || appointment.getStatus() == AppointmentStatus.ON_GOING);
    }
}   