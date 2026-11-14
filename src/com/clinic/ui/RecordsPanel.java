package com.clinic.ui;

import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.service.ClinicManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "Records" page.
 *
 * Default view: two tabs, Patients and Doctors, listing every record so
 * staff land on real data immediately instead of an empty "search first"
 * prompt. A small link still opens the full Search page for name/ID/
 * specialization lookups.
 *
 * Clicking a row opens that patient's or doctor's full record, where
 * appointments can be marked Completed or Cancelled and diagnosis/notes
 * can be edited - this is the only place those actions happen.
 */
public class RecordsPanel extends JPanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private enum Tab { PATIENTS, DOCTORS }
    private enum View { LIST, DETAIL }

    private final ClinicManager clinic;


    private View view = View.LIST;
    private Tab tab = Tab.PATIENTS;
    private Patient currentPatient;
    private Doctor currentDoctor;
    private Doctor returnDoctor; // set when a patient was opened from a doctor's record

    private final JLabel titleLabel = new JLabel("Records");
    private final JButton backButton = new JButton("", Icons.of(Icons.Type.ARROW_LEFT, 18, Theme.PRIMARY));
    private final UiKit.ToggleChip patientsChip = new UiKit.ToggleChip("Patients");
    private final UiKit.ToggleChip doctorsChip = new UiKit.ToggleChip("Doctors");
    private final JPanel tabsRow = new JPanel();
    private final JTextField filterField = FormField.textField("Filter by name or ID...");
    private final JPanel holderInner = new JPanel(new BorderLayout());
    private final VScrollPanel holder = new VScrollPanel(new BorderLayout());
    private final JScrollPane scroll;

    public RecordsPanel(ClinicManager clinic) {
        this.clinic = clinic;

        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.CONTENT_BG);
        setBorder(new EmptyBorder(18, 22, 22, 22));

        titleLabel.setFont(Theme.font(Font.BOLD, 22));
        titleLabel.setForeground(Theme.TEXT_DARK);

        backButton.setFont(Theme.font(Font.BOLD, 15));
        backButton.setForeground(Theme.PRIMARY);
        backButton.setIconTextGap(8);
        backButton.setContentAreaFilled(false);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(Theme.hand());
        backButton.addActionListener(e -> goBack());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titleLabel, BorderLayout.WEST);
        header.add(backButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        holderInner.setOpaque(false);
        holder.add(holderInner, BorderLayout.NORTH);
        scroll = new JScrollPane(holder);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);

        patientsChip.addActionListener(e -> { tab = Tab.PATIENTS; render(); });
        doctorsChip.addActionListener(e -> { tab = Tab.DOCTORS; render(); });
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { renderList(); }
            @Override public void removeUpdate(DocumentEvent e)  { renderList(); }
            @Override public void changedUpdate(DocumentEvent e) { renderList(); }
        });

        render();
    }

    // ------------------------------------------------------------------
    // Public API (used by Search results, Patients page, Appointments page)
    // ------------------------------------------------------------------

    public void showPatient(Patient patient) {
        currentPatient = patient;
        currentDoctor = null;
        returnDoctor = null;
        tab = Tab.PATIENTS;
        view = View.DETAIL;
        render();
    }

    public void showDoctor(Doctor doctor) {
        currentDoctor = doctor;
        currentPatient = null;
        returnDoctor = null;
        tab = Tab.DOCTORS;
        view = View.DETAIL;
        render();
    }

    /** Re-reads the records and redraws whatever is currently shown (list or detail). */
    private java.util.function.Consumer<String> onCheckPayment;

    /** Called with an invoice number when staff press "Check Payment" on the unpaid-visit warning. */
    public void setOnCheckPayment(java.util.function.Consumer<String> handler) {
        this.onCheckPayment = handler;
    }

    public void refresh() {
        render();
    }

    /** Returns to the Patients/Doctors list. Used when the person opens Records from the sidebar. */
    public void resetToList() {
        view = View.LIST;
        currentPatient = null;
        currentDoctor = null;
        returnDoctor = null;
        render();
    }

    private void openPatientFromDoctor(Patient patient, Doctor from) {
        currentPatient = patient;
        currentDoctor = null;
        returnDoctor = from;
        view = View.DETAIL;
        render();
    }

    private void goBack() {
        if (returnDoctor != null) {
            currentDoctor = returnDoctor;
            currentPatient = null;
            returnDoctor = null;
            render();
        } else {
            view = View.LIST;
            render();
        }
    }

    // ------------------------------------------------------------------
    // Top-level render dispatch
    // ------------------------------------------------------------------

    private void render() {
        holderInner.removeAll();
        if (view == View.DETAIL && currentPatient != null) {
            titleLabel.setText("Patient Record");
            backButton.setText(returnDoctor != null ? "Back to doctor" : "Back to Patients");
            backButton.setVisible(true);
            holderInner.add(buildPatientView(currentPatient), BorderLayout.NORTH);
        } else if (view == View.DETAIL && currentDoctor != null) {
            titleLabel.setText("Doctor Record");
            backButton.setText("Back to Doctors");
            backButton.setVisible(true);
            holderInner.add(buildDoctorView(currentDoctor), BorderLayout.NORTH);
        } else {
            titleLabel.setText("Records");
            backButton.setVisible(false);
            holderInner.add(buildListView(), BorderLayout.NORTH);
        }
        holderInner.revalidate();
        holderInner.repaint();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    // ------------------------------------------------------------------
    // List view: Patients / Doctors tabs
    // ------------------------------------------------------------------

    private JComponent buildListView() {
        JPanel page = new JPanel();
        page.setOpaque(false);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));

        // tabs + filter + "full search" link, all on one row
        patientsChip.setActive(tab == Tab.PATIENTS);
        doctorsChip.setActive(tab == Tab.DOCTORS);

        tabsRow.removeAll();
        tabsRow.setOpaque(false);
        tabsRow.setLayout(new BoxLayout(tabsRow, BoxLayout.X_AXIS));
        tabsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabsRow.add(patientsChip);
        tabsRow.add(Box.createHorizontalStrut(10));
        tabsRow.add(doctorsChip);
        tabsRow.add(Box.createHorizontalStrut(18));

        JComponent filterBox = new FormField(Icons.of(Icons.Type.SEARCH, 16, Theme.TEXT_MUTED), filterField, null);
        filterBox.setPreferredSize(new Dimension(280, 40));
        filterBox.setMaximumSize(new Dimension(280, 40));
        tabsRow.add(filterBox);
        tabsRow.add(Box.createHorizontalGlue());

        page.add(tabsRow);
        page.add(Box.createVerticalStrut(14));

        UiKit.Card card = new UiKit.Card(20, Color.WHITE, null);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setPreferredSize(new Dimension(100, 520));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 520));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setName("recordsList"); // found later to refresh without rebuilding the whole page
        JPanel innerHolder = new JPanel(new BorderLayout());
        innerHolder.setOpaque(false);
        innerHolder.setBorder(new EmptyBorder(10, 10, 10, 10));
        innerHolder.add(inner, BorderLayout.NORTH);

        JScrollPane innerScroll = UiKit.roundScroll(innerHolder, 18, null, 0f);
        innerScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        card.add(innerScroll, BorderLayout.CENTER);

        page.add(card);
        fillList(inner);
        return page;
    }

    /** Rebuilds just the inner list (called on tab switch / filter typing, without losing the page). */
    private void renderList() {
        if (view != View.LIST) return;
        Component found = findByName(holderInner, "recordsList");
        if (found instanceof JPanel) {
            fillList((JPanel) found);
            found.revalidate();
            found.repaint();
        } else {
            render(); // list isn't built yet (first paint) - build the whole page
        }
    }

    private static Component findByName(Container root, String name) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName())) return c;
            if (c instanceof Container) {
                Component f = findByName((Container) c, name);
                if (f != null) return f;
            }
        }
        return null;
    }

    private void fillList(JPanel inner) {
        inner.removeAll();
        String q = filterField.getText().trim().toLowerCase();

        if (tab == Tab.PATIENTS) {
            List<Patient> list = new ArrayList<>(clinic.getPatients());
            list.sort(Comparator.comparing(p -> p.getName().toLowerCase()));
            if (!q.isEmpty()) {
                list.removeIf(p -> !p.getName().toLowerCase().contains(q) && !p.getPatientId().toLowerCase().contains(q));
            }
            if (list.isEmpty()) {
                inner.add(emptyNote(q.isEmpty() ? "No patients registered yet." : "No patients match \u201C" + q + "\u201D."));
            }
            for (Patient p : list) {
                inner.add(UiKit.patientResultCard(clinic, p, () -> showPatient(p)));
                inner.add(Box.createVerticalStrut(10));
            }
        } else {
            List<Doctor> list = new ArrayList<>(clinic.getDoctors());
            list.sort(Comparator.comparing(d -> d.getName().toLowerCase()));
            if (!q.isEmpty()) {
                list.removeIf(d -> !d.getName().toLowerCase().contains(q) && !d.getDoctorId().toLowerCase().contains(q));
            }
            if (list.isEmpty()) {
                inner.add(emptyNote(q.isEmpty() ? "No doctors on file yet." : "No doctors match \u201C" + q + "\u201D."));
            }
            for (Doctor d : list) {
                inner.add(UiKit.doctorResultCard(clinic, d, () -> showDoctor(d)));
                inner.add(Box.createVerticalStrut(10));
            }
        }
    }

    // ------------------------------------------------------------------
    // Patient record
    // ------------------------------------------------------------------

    private JComponent buildPatientView(Patient p) {
        List<Appointment> appts = clinic.getAppointmentsForPatient(p.getPatientId());
        Doctor assigned = clinic.getAssignedDoctor(p.getPatientId());
        Appointment next = clinic.getNextAppointment(p.getPatientId());

        int pending = 0, ongoing = 0, completed = 0, cancelled = 0;
        for (Appointment a : appts) {
            switch (a.getStatus()) {
                case PENDING: pending++; break;
                case ON_GOING: ongoing++; break;
                case COMPLETED: completed++; break;
                case CANCELLED: cancelled++; break;
            }
        }

        UiKit.Pill visitPill = new UiKit.Pill();
        if (next != null) visitPill.style("Upcoming visit", Theme.PENDING_BG, Theme.PENDING_FG);
        else visitPill.style("No upcoming visit", new Color(0xE3E6E8), Theme.TEXT_MUTED);

        JComponent profile = profileCard(
                new UiKit.Avatar(Icons.Type.USER, 84, UiKit.AVATAR_BLUE, Color.WHITE),
                p.getName(),
                "Patient ID: " + p.getPatientId(),
                visitPill,
                new String[][]{
                        {"Age", p.getAge() == 0 ? "N/A" : String.valueOf(p.getAge())},
                        {"Contact", p.getContactNumber().isEmpty() ? "N/A" : p.getContactNumber()},
                        {"Assigned Doctor", assigned == null ? "Not assigned"
                                : assigned.getName() + " (" + assigned.getSpecialization() + ")"},
                        {"Next Visit", next == null ? "None scheduled" : UiKit.formatDate(next.getDateTime().toLocalDate())}
                });

        JComponent stats = statsRow(
                new Object[]{pending, "Pending", Theme.TILE_YELLOW},
                new Object[]{ongoing, "On-going", Theme.ONGOING_BG},
                new Object[]{completed, "Completed", Theme.TILE_GREEN},
                new Object[]{cancelled, "Cancelled", Theme.CANCELLED_BG});

        JComponent historyCard = sectionCard("Appointment History", null,
                appts.isEmpty() ? emptyNote("No appointments yet.") : appointmentTable(appts, true));

        JComponent notesCard = buildNotesCard(p);

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(100, 420));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.gridx = 0;
        gc.weightx = 0.68;
        gc.insets = new Insets(0, 0, 0, 9);
        row.add(historyCard, gc);
        gc.gridx = 1;
        gc.weightx = 0.32;
        gc.insets = new Insets(0, 9, 0, 0);
        row.add(notesCard, gc);

        return stack(profile, stats, row);
    }

    private JComponent buildNotesCard(Patient p) {
        JTextArea diagnosis = textArea(p.getDiagnosis());
        JTextArea notes = textArea(p.getNotes());

        JLabel status = new JLabel(" ");
        status.setFont(Theme.font(Font.PLAIN, 13));

        PillButton save = PillButton.primary("Save", null);
        save.setFont(Theme.font(Font.BOLD, 16));
        save.setPreferredSize(new Dimension(120, 42));
        save.setMaximumSize(new Dimension(120, 42));
        save.addActionListener(e -> {
            try {
                clinic.updateMedicalNotes(p.getPatientId(), diagnosis.getText(), notes.getText());
                status.setForeground(Theme.DONE_FG);
                status.setText("Saved.");
            } catch (Exception ex) {
                status.setForeground(Theme.ERROR);
                status.setText(ex.getMessage());
            }
        });

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(fieldLabel("Diagnosis / Results"));
        body.add(Box.createVerticalStrut(6));
        body.add(wrapArea(diagnosis, 96));
        body.add(Box.createVerticalStrut(14));
        body.add(fieldLabel("Notes"));
        body.add(Box.createVerticalStrut(6));
        body.add(wrapArea(notes, 120));
        body.add(Box.createVerticalGlue());
        body.add(Box.createVerticalStrut(10));

        JPanel foot = new JPanel(new BorderLayout(12, 0));
        foot.setOpaque(false);
        foot.setAlignmentX(Component.LEFT_ALIGNMENT);
        foot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        foot.add(save, BorderLayout.WEST);
        foot.add(status, BorderLayout.CENTER);
        body.add(foot);

        return sectionCard("Diagnosis & Notes", null, body);
    }

    private static JTextArea textArea(String text) {
        JTextArea a = new JTextArea(text == null ? "" : text);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(Theme.font(Font.PLAIN, 14));
        a.setForeground(Theme.TEXT_DARK);
        a.setBorder(new EmptyBorder(8, 10, 8, 10));
        return a;
    }

    private static JComponent wrapArea(JTextArea area, int height) {
        JScrollPane sp = UiKit.roundScroll(area, 14, Theme.BORDER, 1f);
        sp.getViewport().setOpaque(true);
        sp.getViewport().setBackground(Color.WHITE);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setPreferredSize(new Dimension(100, height));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return sp;
    }

    private static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, 14));
        l.setForeground(Theme.PRIMARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // ------------------------------------------------------------------
    // Doctor record
    // ------------------------------------------------------------------

    private JComponent buildDoctorView(Doctor d) {
        List<Appointment> appts = clinic.getAppointmentsForDoctor(d.getDoctorId());
        List<Patient> patients = clinic.getPatientsOfDoctor(d.getDoctorId());
        LocalDate today = LocalDate.now();

        int pending = 0, ongoing = 0, completed = 0;
        Map<String, Integer> visits = new HashMap<>();
        Map<String, LocalDate> nextVisit = new HashMap<>();
        for (Appointment a : appts) {
            if (a.getStatus() == AppointmentStatus.PENDING) pending++;
            if (a.getStatus() == AppointmentStatus.ON_GOING) ongoing++;
            if (a.getStatus() == AppointmentStatus.COMPLETED) completed++;
            String key = a.getPatientId().toUpperCase();
            visits.merge(key, 1, Integer::sum);
            LocalDate date = a.getDateTime().toLocalDate();
            if ((a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.ON_GOING)
                    && !date.isBefore(today)) {
                nextVisit.merge(key, date, (x, y) -> x.isBefore(y) ? x : y);
            }
        }

        UiKit.Pill availability = new UiKit.Pill();
        availability.setStatus(d.isAvailable() ? "Available" : "On leave");

        JComponent profile = profileCard(
                new UiKit.Avatar(Icons.Type.DOCTOR, 84, UiKit.AVATAR_BLUE, Color.WHITE),
                d.getName(),
                d.getSpecialization() + "  |  Doctor ID: " + d.getDoctorId(),
                availability,
                new String[][]{
                        {"Specialization", d.getSpecialization()},
                        {"Duty Shift", d.getShift()},
                        {"Age", d.getAge() == 0 ? "N/A" : String.valueOf(d.getAge())},
                        {"Contact", d.getContactNumber().isEmpty() ? "N/A" : d.getContactNumber()}
                });

        JComponent stats = statsRow(
                new Object[]{clinic.countAppointmentsToday(d.getDoctorId()), "Appointments Today", Theme.TILE_BLUE},
                new Object[]{pending + ongoing, "Pending / On-going", Theme.TILE_YELLOW},
                new Object[]{completed, "Completed", Theme.TILE_GREEN},
                new Object[]{patients.size(), "Patients Assigned", new Color(0xD9CFF5)});

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Patient Name", "Patient ID", "Contact", "Visits", "Next Visit"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        List<Patient> rowPatients = new ArrayList<>();
        for (Patient p : patients) {
            String key = p.getPatientId().toUpperCase();
            LocalDate next = nextVisit.get(key);
            model.addRow(new Object[]{
                    p.getName(),
                    p.getPatientId(),
                    p.getContactNumber().isEmpty() ? "N/A" : p.getContactNumber(),
                    visits.getOrDefault(key, 0),
                    next == null ? "-" : UiKit.formatDate(next)});
            rowPatients.add(p);
        }
        JTable table = new JTable(model);
        UiKit.styleTable(table);
        table.setCursor(Theme.hand());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && row < rowPatients.size()) openPatientFromDoctor(rowPatients.get(row), d);
            }
        });

        JComponent listCard = sectionCard("Assigned Patients", "Click a patient to open their record",
                patients.isEmpty() ? emptyNote("No patients assigned to this doctor yet.") : tableScroll(table));
        listCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        listCard.setPreferredSize(new Dimension(100, 380));
        listCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));

        return stack(profile, stats, listCard);
    }

    // ------------------------------------------------------------------
    // Appointment history table with per-row Complete / Cancel actions
    // ------------------------------------------------------------------

    private JComponent appointmentTable(List<Appointment> appts, boolean actionable) {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Date & Time", "Doctor", "Reason", "Status", "Action"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return actionable && c == 4; }
        };
        for (Appointment a : appts) {
            Doctor d = clinic.getDoctorById(a.getDoctorId());
            model.addRow(new Object[]{
                    UiKit.formatDate(a.getDateTime().toLocalDate()) + ", " + a.getDateTime().format(TIME_FMT),
                    d == null ? "-" : d.getName(),
                    a.getReason(),
                    a.getStatus().name(),
                    ""});
        }
        JTable table = new JTable(model);
        UiKit.styleTable(table);
        table.setRowHeight(46);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(170);
        table.getColumnModel().getColumn(3).setCellRenderer(new UiKit.StatusRenderer());

        if (actionable) {
            AppointmentActionCell cell = new AppointmentActionCell(appts, table);
            table.getColumnModel().getColumn(4).setCellRenderer(cell);
            table.getColumnModel().getColumn(4).setCellEditor(cell);
        }
        return tableScroll(table);
    }

    /** Complete / Cancel buttons shown only for appointments that are still open. */
    private class AppointmentActionCell extends AbstractCellEditor
            implements javax.swing.table.TableCellRenderer, javax.swing.table.TableCellEditor {
        private final List<Appointment> appts;
        private final JTable owner;

        AppointmentActionCell(List<Appointment> appts, JTable owner) {
            this.appts = appts;
            this.owner = owner;
        }

        private JComponent build(int row, boolean live) {
            JPanel holder = UiKit.actionRow(14);
            if (row < 0 || row >= appts.size()) return holder;

            Appointment a = appts.get(row);
            boolean open = a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.ON_GOING;
            if (!open) {
                JLabel dash = new JLabel("-");
                dash.setForeground(Theme.TEXT_MUTED);
                holder.add(dash);
                return holder;
            }

            JButton complete = UiKit.tableActionButton("Complete", Color.WHITE, Theme.DONE_FG, null);
            JButton cancel = UiKit.tableActionButton("Cancel", Color.WHITE, Theme.ERROR, null);
            if (live) {
                complete.addActionListener(e -> {
                    fireEditingStopped();
                    act(a.getAppointmentId(), true);
                });
                cancel.addActionListener(e -> {
                    fireEditingStopped();
                    act(a.getAppointmentId(), false);
                });
            }
            holder.add(complete);
            holder.add(Box.createHorizontalStrut(6));
            holder.add(cancel);
            return holder;
        }

        private void act(String appointmentId, boolean complete) {
            if (!complete) {
                int choice = JOptionPane.showConfirmDialog(owner,
                        "Cancel this appointment? This can't be undone.",
                        "Confirm cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) return;
            }
            try {
                if (complete) clinic.completeAppointment(appointmentId);
                else clinic.cancelAppointment(appointmentId);
                render(); // reflect the new status and totals immediately
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? "Could not update the appointment." : ex.getMessage();
                if (complete && msg.contains("been paid") && onCheckPayment != null) {
                    // Unpaid visit: offer to jump straight to that bill in Payments & Billing
                    com.clinic.model.Bill bill = com.clinic.service.BillingService.getInstance().getByAppointment(appointmentId);
                    Object[] options = {"Check Payment", "Close"};
                    int picked = JOptionPane.showOptionDialog(owner, msg, "Can't update appointment",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                    if (picked == 0) onCheckPayment.accept(bill != null ? bill.getInvoiceId() : "");
                } else {
                    JOptionPane.showMessageDialog(owner, msg, "Can't update appointment", JOptionPane.WARNING_MESSAGE);
                }
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            return build(r, false);
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) {
            return build(r, true);
        }

        @Override public Object getCellEditorValue() { return ""; }
    }

    // ------------------------------------------------------------------
    // Shared building blocks
    // ------------------------------------------------------------------

    private static JComponent stack(JComponent... parts) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (int i = 0; i < parts.length; i++) {
            parts[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(parts[i]);
            if (i < parts.length - 1) p.add(Box.createVerticalStrut(18));
        }
        return p;
    }

    private JComponent profileCard(JComponent avatar, String name, String subtitle, JComponent badge, String[][] info) {
        // Larger radius than the smaller cards below it: this card is much wider/taller,
        // so the same pixel radius that looks rounded on a small tile reads as almost
        // sharp here. A bigger arc keeps it visually consistent with the rest of the page.
        UiKit.Card card = new UiKit.Card(28, Color.WHITE, Theme.CARD_STROKE);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(Theme.font(Font.BOLD, 26));
        nameLabel.setForeground(Theme.TEXT_DARK);
        JLabel sub = new JLabel(subtitle);
        sub.setFont(Theme.font(Font.PLAIN, 15));
        sub.setForeground(Theme.PRIMARY);

        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(nameLabel);
        names.add(Box.createVerticalStrut(4));
        names.add(sub);

        JPanel namesWrap = new JPanel(new GridBagLayout());
        namesWrap.setOpaque(false);
        GridBagConstraints left = new GridBagConstraints();
        left.anchor = GridBagConstraints.WEST;
        left.weightx = 1;
        namesWrap.add(names, left);

        JPanel badgeWrap = new JPanel(new GridBagLayout());
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);

        JPanel avatarWrap = new JPanel(new GridBagLayout());
        avatarWrap.setOpaque(false);
        avatarWrap.add(avatar);

        JPanel top = new JPanel(new BorderLayout(18, 0));
        top.setOpaque(false);
        top.add(avatarWrap, BorderLayout.WEST);
        top.add(namesWrap, BorderLayout.CENTER);
        top.add(badgeWrap, BorderLayout.EAST);

        JPanel grid = new JPanel(new GridLayout(1, info.length, 18, 0));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, Theme.BORDER), new EmptyBorder(14, 0, 0, 0)));
        for (String[] item : info) {
            JPanel cell = new JPanel();
            cell.setOpaque(false);
            cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
            JLabel k = new JLabel(item[0]);
            k.setFont(Theme.font(Font.PLAIN, 13));
            k.setForeground(Theme.TEXT_MUTED);
            JLabel v = new JLabel("<html>" + UiKit.esc(item[1]) + "</html>");
            v.setFont(Theme.font(Font.BOLD, 15));
            v.setForeground(Theme.TEXT_DARK);
            cell.add(k);
            cell.add(Box.createVerticalStrut(3));
            cell.add(v);
            grid.add(cell);
        }

        card.add(top, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    /** Small number tiles. Each item is {Integer count, String caption, Color accent}. */
    private JComponent statsRow(Object[]... items) {
        JPanel row = new JPanel(new GridLayout(1, items.length, 18, 0));
        row.setOpaque(false);
        for (Object[] it : items) {
            final Color accent = (Color) it[2];
            UiKit.Card tile = new UiKit.Card(16, Color.WHITE, Theme.CARD_STROKE);
            tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
            tile.setBorder(new EmptyBorder(14, 18, 14, 18));

            JComponent bar = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.antialias(g2);
                    g2.setColor(accent);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.dispose();
                }
            };
            bar.setPreferredSize(new Dimension(44, 7));
            bar.setMaximumSize(new Dimension(44, 7));
            bar.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel number = new JLabel(String.valueOf(it[0]));
            number.setFont(Theme.font(Font.BOLD, 28));
            number.setForeground(Theme.TEXT_DARK);
            number.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel caption = new JLabel((String) it[1]);
            caption.setFont(Theme.font(Font.PLAIN, 13));
            caption.setForeground(Theme.TEXT_MUTED);
            caption.setAlignmentX(Component.LEFT_ALIGNMENT);

            tile.add(bar);
            tile.add(Box.createVerticalStrut(8));
            tile.add(number);
            tile.add(caption);
            row.add(tile);
        }
        row.setPreferredSize(new Dimension(100, 104));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        return row;
    }

    private JComponent sectionCard(String title, String hint, JComponent content) {
        UiKit.Card card = new UiKit.Card(24, Color.WHITE, Theme.CARD_STROKE);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 20, 18, 20));

        JLabel t = new JLabel(title);
        t.setFont(Theme.font(Font.BOLD, 19));
        t.setForeground(Theme.TEXT_DARK);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(t, BorderLayout.WEST);
        if (hint != null) {
            JLabel h = new JLabel(hint);
            h.setFont(Theme.font(Font.PLAIN, 13));
            h.setForeground(Theme.TEXT_MUTED);
            head.add(h, BorderLayout.EAST);
        }

        card.add(head, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private static JComponent tableScroll(JTable table) {
        JScrollPane sp = UiKit.roundScroll(table, 14, new Color(0xD3D3D3), 1f);
        sp.getViewport().setOpaque(true);
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    private static JComponent emptyNote(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(Theme.font(Font.PLAIN, 15));
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }
}