package com.clinic.service;

import com.clinic.exception.InvalidInputException;
import com.clinic.file.BillFileHandler;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Bill;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Keeps every bill and saves them to bills.csv. One shared instance is used by
 * the Add Walk-in / Add Appointment pop-ups, the Payments & Billing page and
 * ClinicManager (which refuses to complete a visit that hasn't been paid).
 */
public final class BillingService {

    private static BillingService instance;

    public static synchronized BillingService getInstance() {
        if (instance == null) instance = new BillingService(new BillFileHandler());
        return instance;
    }

    private final BillFileHandler files;
    private final List<Bill> bills = new ArrayList<>();

    public BillingService(BillFileHandler files) {
        this.files = files;
        try {
            bills.addAll(files.load());
        } catch (IOException e) {
            System.err.println("Could not read " + files.getFile() + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Lookups
    // ------------------------------------------------------------------

    public synchronized List<Bill> getBills() {
        return new ArrayList<>(bills);
    }

    public synchronized Bill getByAppointment(String appointmentId) {
        for (Bill b : bills) {
            if (b.getAppointmentId().equalsIgnoreCase(appointmentId)) return b;
        }
        return null;
    }

    /** Total of every PAID bill. */
    public synchronized BigDecimal getTotalRevenue() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Bill b : bills) if (b.isPaid()) sum = sum.add(b.getAmountDue());
        return sum;
    }

    // ------------------------------------------------------------------
    // Creating bills
    // ------------------------------------------------------------------

    /**
     * Creates the (pending) bill for an appointment, priced by the chosen service.
     * If that appointment already has a bill, the existing one is returned.
     * Discounts and HMO details are added later, when the bill is paid.
     */
    public synchronized Bill createBill(String appointmentId, String patientId, String service) {
        Bill existing = getByAppointment(appointmentId);
        if (existing != null) return existing;

        Bill bill = new Bill(nextInvoiceId(), appointmentId, patientId, service, Bill.servicePrice(service),
                LocalDateTime.now());
        bills.add(bill);
        persist();
        return bill;
    }

    /**
     * Appointments made before billing existed (or by any other route) don't have a
     * bill yet. Give every still-open appointment one, so nothing can slip through.
     */
    public synchronized void syncMissingBills(Collection<Appointment> appointments) {
        boolean changed = false;
        for (Appointment a : appointments) {
            boolean open = a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.ON_GOING;
            if (!open || getByAppointment(a.getAppointmentId()) != null) continue;
            String service = Bill.serviceFromReason(a.getReason());
            bills.add(new Bill(nextInvoiceId(), a.getAppointmentId(), a.getPatientId(), service,
                    Bill.servicePrice(service), LocalDateTime.now()));
            changed = true;
        }
        if (changed) persist();
    }

    private String nextInvoiceId() {
        int max = 0;
        for (Bill b : bills) {
            String id = b.getInvoiceId();
            if (id.toUpperCase().startsWith("INV-")) {
                try { max = Math.max(max, Integer.parseInt(id.substring(4))); }
                catch (NumberFormatException ignored) { }
            }
        }
        return String.format("INV-%03d", max + 1);
    }

    // ------------------------------------------------------------------
    // Paying
    // ------------------------------------------------------------------

    /** Marks a bill paid by cash. "received" is what the patient handed over. Apply any discount to the bill first. */
    public synchronized void recordCashPayment(Bill bill, BigDecimal received) throws IOException {
        bill.markPaid(Bill.METHOD_CASH, received, LocalDateTime.now());
        files.save(bills);
    }

    /** Marks a bill paid by card. Only the last 4 digits and the expiry are kept. */
    public synchronized void recordCardPayment(Bill bill, String holder, String last4, String expiry)
            throws IOException {
        bill.setCard(holder, last4, expiry);
        bill.markPaid(Bill.METHOD_CARD, bill.getAmountDue(), LocalDateTime.now());
        files.save(bills);
    }

    /**
     * Throws unless the appointment's bill has been paid. ClinicManager calls this
     * before completing a visit, so no screen can complete an unpaid one.
     */
    public synchronized void requirePaid(Appointment a) throws InvalidInputException {
        Bill bill = getByAppointment(a.getAppointmentId());
        if (bill == null) {
            bill = createBill(a.getAppointmentId(), a.getPatientId(), Bill.serviceFromReason(a.getReason()));
        }
        if (!bill.isPaid()) {
            throw new InvalidInputException("This visit hasn't been paid yet.\n"
                    + "Please settle " + bill.getInvoiceId() + " (" + Bill.money(bill.getAmountDue())
                    + ") in Payments & Billing first.");
        }
    }

    private void persist() {
        try {
            files.save(bills);
        } catch (IOException e) {
            System.err.println("Could not save " + files.getFile() + ": " + e.getMessage());
        }
    }
}