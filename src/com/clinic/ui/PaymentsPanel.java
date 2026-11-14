package com.clinic.ui;

import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Bill;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.service.BillingService;
import com.clinic.service.ClinicManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Payments & Billing page.
 *
 * - Two tiles: Total Revenue (paid bills) and Pending Bills, plus a Record Payment button.
 * - Recent Transactions: one row per bill, searchable by patient or invoice.
 * - Action column: Pay (cash or card) on unpaid bills, Receipt on paid bills, and
 *   Complete to finish the visit once it has been paid.
 */
public class PaymentsPanel extends JPanel {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);

    private final ClinicManager clinic;
    private final BillingService billing = BillingService.getInstance();

    private final JLabel revenueValue = tileValue();
    private final JLabel pendingValue = tileValue();
    private final JTextField searchField = FormField.textField("Search patient or invoice...");

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Invoice ID", "Patient Name", "Service", "Amount", "Status", "Action"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return c == 5; }
    };
    private final JTable table = new JTable(model);

    private final List<BillRow> allRows = new ArrayList<>();
    private List<BillRow> shown = new ArrayList<>();

    public PaymentsPanel(ClinicManager clinic) {
        this.clinic = clinic;

        setLayout(new BorderLayout(0, 18));
        setBackground(Theme.CONTENT_BG);
        setBorder(new EmptyBorder(18, 22, 22, 22));

        add(UiKit.pageTopBar("Payments"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(buildStatsRow(), BorderLayout.NORTH);
        body.add(buildTableCard(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        refresh();
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    /** Fills the search box (e.g. with an invoice number) so only that bill shows. */
    public void searchFor(String text) {
        searchField.setText(text == null ? "" : text);
    }

    public void refresh() {
        if (table.isEditing()) table.getCellEditor().cancelCellEditing();

        List<Appointment> appts = new ArrayList<>(clinic.getAppointments());
        billing.syncMissingBills(appts);   // old / other-route appointments get a bill too

        Map<String, Appointment> apptById = new HashMap<>();
        for (Appointment a : appts) apptById.put(a.getAppointmentId().toUpperCase(), a);
        Map<String, Patient> patients = new HashMap<>();
        for (Patient p : clinic.getPatients()) patients.put(p.getPatientId().toUpperCase(), p);

        List<Bill> bills = billing.getBills();
        bills.sort(Comparator.comparing(Bill::getCreatedAt).thenComparing(Bill::getInvoiceId).reversed());

        allRows.clear();
        int pending = 0;
        for (Bill b : bills) {
            Patient p = patients.get(b.getPatientId().toUpperCase());
            BillRow r = new BillRow(b,
                    apptById.get(b.getAppointmentId().toUpperCase()),
                    p == null ? b.getPatientId() : p.getName(),
                    p == null ? 0 : p.getAge());
            if (r.isPending()) pending++;
            allRows.add(r);
        }

        revenueValue.setText(Bill.moneyShort(billing.getTotalRevenue()));
        pendingValue.setText(String.valueOf(pending));
        applyFilter();
    }

    private void applyFilter() {
        if (table.isEditing()) table.getCellEditor().cancelCellEditing();
        String q = searchField.getText().trim().toLowerCase();

        shown = new ArrayList<>();
        model.setRowCount(0);
        for (BillRow r : allRows) {
            Bill b = r.getBill();
            if (!q.isEmpty() && !r.getPatientName().toLowerCase().contains(q)
                    && !b.getInvoiceId().toLowerCase().contains(q)) continue;
            shown.add(r);
            model.addRow(new Object[]{
                    b.getInvoiceId(),
                    r.getPatientName(),
                    b.getServiceLabel(),
                    Bill.money(b.getAmountDue()),
                    r.getStatus(),
                    ""});
        }
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private JComponent buildStatsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
        row.setOpaque(false);

        row.add(statTile(Icons.Type.WALLET, Theme.TILE_BLUE, revenueValue, "Total Revenue"));
        row.add(statTile(Icons.Type.RECORDS, Theme.TILE_YELLOW, pendingValue, "Pending Bills"));

        PillButton record = PillButton.primary("Record Payment", Icons.of(Icons.Type.PLUS_CIRCLE, 22, Color.WHITE));
        record.setFont(Theme.font(Font.BOLD, 16));
        record.setPreferredSize(new Dimension(220, 150));
        record.addActionListener(e -> recordPayment());
        row.add(wrapRight(record));
        return row;
    }

    private static JComponent wrapRight(JComponent c) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTH;
        p.add(c, gc);
        return p;
    }

    private static JLabel tileValue() {
        JLabel l = new JLabel("0");
        l.setFont(Theme.font(Font.BOLD, 32));
        l.setForeground(Theme.TEXT_DARK);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static UiKit.Card statTile(Icons.Type icon, Color color, JLabel value, String caption) {
        UiKit.Card card = new UiKit.Card(18, Color.WHITE, Theme.CARD_STROKE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 18, 14, 18));
        card.setPreferredSize(new Dimension(230, 150));

        JComponent iconTile = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                Icons.of(icon, 26, Theme.TEXT_DARK).paintIcon(this, g2, (getWidth() - 26) / 2, (getHeight() - 26) / 2);
                g2.dispose();
            }
        };
        Dimension d = new Dimension(46, 46);
        iconTile.setPreferredSize(d);
        iconTile.setMaximumSize(d);
        iconTile.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel cap = new JLabel(caption);
        cap.setFont(Theme.font(Font.PLAIN, 15));
        cap.setForeground(Theme.TEXT_DARK);
        cap.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(iconTile);
        card.add(Box.createVerticalStrut(10));
        card.add(value);
        card.add(cap);
        return card;
    }

    private JComponent buildTableCard() {
        UiKit.Card card = new UiKit.Card(18, Color.WHITE, null);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(18, 24, 16, 24));

        JLabel title = new JLabel("Recent Transactions");
        title.setFont(Theme.font(Font.BOLD, 22));
        title.setForeground(Theme.TEXT_DARK);

        JComponent searchBox = new FormField(Icons.of(Icons.Type.SEARCH, 16, Theme.TEXT_MUTED), searchField, null);
        searchBox.setPreferredSize(new Dimension(290, 40));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(title, BorderLayout.WEST);
        head.add(searchBox, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        UiKit.styleTable(table);
        table.setRowHeight(52);
        // preferred widths for wide windows; minimums keep the important columns readable on narrow ones
        int[] preferred = {110, 170, 160, 110, 118, 190};
        int[] minimum   = {100,  80, 150,  96, 108, 178};
        for (int i = 0; i < preferred.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(preferred[i]);
            table.getColumnModel().getColumn(i).setMinWidth(minimum[i]);
        }
        table.getColumnModel().getColumn(4).setCellRenderer(statusRenderer());

        BillActionCell actions = new BillActionCell(() -> shown, this::pay, this::showReceipt, this::completeVisit);
        table.getColumnModel().getColumn(5).setCellRenderer(actions);
        table.getColumnModel().getColumn(5).setCellEditor(actions);

        JScrollPane scroll = UiKit.roundScroll(table, 14, null, 0f);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------
    // Status pill (Paid / Pending / Cancelled)
    // ------------------------------------------------------------------

    private TableCellRenderer statusRenderer() {
        JPanel holder = new JPanel(new GridBagLayout());
        // fixed size so Paid / Pending / Cancelled pills all look the same
        UiKit.Pill pill = new UiKit.Pill() {
            @Override public Dimension getPreferredSize() { return new Dimension(94, 28); }
        };
        holder.setOpaque(true);
        holder.setBackground(Color.WHITE);
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.weightx = 1;
        gc.insets = new Insets(0, 14, 0, 0);
        holder.add(pill, gc);

        return (t, value, selected, focused, row, col) -> {
            String s = String.valueOf(value);
            if (BillRow.PAID.equals(s)) pill.style(BillRow.PAID, Theme.DONE_BG, Theme.DONE_FG);
            else if (BillRow.CANCELLED.equals(s)) pill.style(BillRow.CANCELLED, Theme.CANCELLED_BG, Theme.CANCELLED_FG);
            else pill.style(BillRow.PENDING, Theme.PENDING_BG, Theme.PENDING_FG);
            return holder;
        };
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void pay(BillRow r) {
        boolean ok = PayBillDialog.open(this, r.getBill(), r.getPatientName(), r.getPatientAge());
        if (!ok) return;
        refresh();

        // patient is here right now -> offer to finish the visit straight away
        Appointment appt = r.getAppointment();
        if (appt != null && appt.getStatus() == AppointmentStatus.ON_GOING) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Payment recorded for " + r.getBill().getInvoiceId() + ".\n\nMark this visit as completed now?",
                    "Payment recorded", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) completeVisit(r);
        }
    }

    private void completeVisit(BillRow r) {
        try {
            clinic.completeAppointment(r.getBill().getAppointmentId());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Can't complete visit", JOptionPane.WARNING_MESSAGE);
        }
        refresh();
    }

    /** "Record Payment" tile: pick any unpaid bill, then pay it. */
    private void recordPayment() {
        List<BillRow> pending = new ArrayList<>();
        for (BillRow r : allRows) if (r.isPending()) pending.add(r);
        if (pending.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There are no pending bills right now.",
                    "Record Payment", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] options = new String[pending.size()];
        for (int i = 0; i < options.length; i++) {
            BillRow r = pending.get(i);
            options[i] = r.getBill().getInvoiceId() + "  -  " + r.getPatientName() + "  -  "
                    + r.getBill().getServiceLabel() + "  -  " + Bill.money(r.getBill().getAmount());
        }
        Object picked = JOptionPane.showInputDialog(this, "Choose the bill to pay:", "Record Payment",
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (picked == null) return;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(picked)) { pay(pending.get(i)); return; }
        }
    }

    private void showReceipt(BillRow r) {
        Bill b = r.getBill();
        Appointment appt = r.getAppointment();
        Doctor doctor = appt == null || appt.getDoctorId().isEmpty() ? null
                : clinic.getDoctorById(appt.getDoctorId());

        StringBuilder sb = new StringBuilder();
        sb.append("CLINIC MANAGEMENT SYSTEM\n");
        sb.append("Official Receipt\n");
        sb.append("------------------------------------------\n");
        sb.append(line("Invoice", b.getInvoiceId()));
        sb.append(line("Date paid", b.getPaidAt() == null ? "-" : b.getPaidAt().format(WHEN)));
        sb.append(line("Patient", r.getPatientName()));
        if (doctor != null) sb.append(line("Doctor", doctor.getName()));
        sb.append("------------------------------------------\n");
        sb.append(line("Service", b.getServiceLabel()));
        sb.append(line("Fee", Bill.money(b.getAmount())));
        if (b.getDiscountAmount().signum() > 0) {
            sb.append(line("Discount", "-" + Bill.money(b.getDiscountAmount()) + " (20% "
                    + b.getDiscountLabel() + ")"));
            sb.append(line(b.getDiscountLabel() + " ID", b.getDiscountIdNumber()));
        }
        if (b.hasHmo()) {
            sb.append(line("HMO", b.getHmoProvider()));
            sb.append(line("Member no.", b.getHmoMemberId()));
            sb.append(line("Cardholder", b.getHmoHolder()));
            if (!b.getHmoApprovalCode().isEmpty()) sb.append(line("Approval", b.getHmoApprovalCode()));
        }
        sb.append("------------------------------------------\n");
        sb.append(line("TOTAL PAID", Bill.money(b.getAmountDue())));
        if (Bill.METHOD_CARD.equals(b.getMethod())) {
            sb.append(line("Paid by", "Card ending " + b.getCardLast4()));
            sb.append(line("Cardholder", b.getCardHolder()));
        } else {
            sb.append(line("Paid by", "Cash"));
            sb.append(line("Received", Bill.money(b.getAmountReceived())));
            BigDecimal change = b.getAmountReceived().subtract(b.getAmountDue());
            if (change.signum() > 0) sb.append(line("Change", Bill.money(change)));
        }
        sb.append("\nThank you!");

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setBorder(new EmptyBorder(8, 10, 8, 10));
        JOptionPane.showMessageDialog(this, area, "Receipt " + b.getInvoiceId(), JOptionPane.PLAIN_MESSAGE);
    }

    private static String line(String label, String value) {
        StringBuilder sb = new StringBuilder(label);
        while (sb.length() < 12) sb.append(' ');
        return sb.append(value).append('\n').toString();
    }
}