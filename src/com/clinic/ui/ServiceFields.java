package com.clinic.ui;

import com.clinic.model.Appointment;
import com.clinic.model.Bill;
import com.clinic.model.Patient;
import com.clinic.service.BillingService;
import com.clinic.service.ClinicManager;

import javax.swing.*;
import java.awt.*;

/**
 * The "Service" choice in the Add Walk-in and Add Appointment pop-ups. It replaces the old
 * free-text "Reason for Visit" box with two options, each with a fixed price:
 *
 *   General Check-up (PHP 700)      Lab Test (PHP 1,200)
 *
 * HMO / health cards and PWD / Senior Citizen discounts are NOT asked here.
 * They are handled later, in Payments & Billing, when the bill is paid.
 */
public class ServiceFields extends JPanel {

    private String service;   // Bill.SERVICE_CHECKUP or Bill.SERVICE_LAB, null until chosen

    private final UiKit.ToggleChip checkupChip = new UiKit.ToggleChip(chipText(Bill.SERVICE_CHECKUP));
    private final UiKit.ToggleChip labChip = new UiKit.ToggleChip(chipText(Bill.SERVICE_LAB));

    public ServiceFields() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 8));

        PayKit.sizeChip(checkupChip, 210);
        PayKit.sizeChip(labChip, 190);
        checkupChip.addActionListener(e -> choose(Bill.SERVICE_CHECKUP));
        labChip.addActionListener(e -> choose(Bill.SERVICE_LAB));

        JLabel title = new JLabel("<html>Service<font color='#E53935'>*</font></html>");
        title.setFont(Theme.font(Font.BOLD, 20));
        title.setForeground(PayKit.DARK);
        add(title, BorderLayout.NORTH);
        add(PayKit.chipRow(checkupChip, labChip), BorderLayout.CENTER);
    }

    private static String chipText(String service) {
        return Bill.serviceLabel(service) + "  " + Bill.moneyShort(Bill.servicePrice(service));
    }

    private void choose(String s) {
        service = s;
        checkupChip.setActive(Bill.SERVICE_CHECKUP.equals(s));
        labChip.setActive(Bill.SERVICE_LAB.equals(s));
    }

    /** Bill.SERVICE_CHECKUP or Bill.SERVICE_LAB, or null if nothing was chosen. */
    public String getService() { return service; }

    /** The chip to focus when nothing was chosen. */
    public JComponent getFocusTarget() { return checkupChip; }

    /** @return an error message, or null when a service was chosen */
    public String check() {
        return service == null ? "Please choose General Check-up or Lab Test." : null;
    }

    /** Creates the pending bill for the appointment that was just booked for this patient. */
    public Bill createBill(ClinicManager clinic, Patient patient) {
        Appointment latest = null;
        for (Appointment a : clinic.getAppointmentsForPatient(patient.getPatientId())) latest = a;
        if (latest == null || service == null) return null;
        return BillingService.getInstance().createBill(latest.getAppointmentId(), patient.getPatientId(), service);
    }
}