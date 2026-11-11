package com.clinic.ui;

import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.service.ClinicManager;
import com.clinic.util.InputValidator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * "Add Appointment" pop-up. Same-day booking isn't allowed - the earliest
 * pickable date is tomorrow. A doctor must be chosen before the time picker
 * unlocks, since available times depend on that doctor's existing schedule.
 */
public class AddAppointmentDialog extends JDialog {

    private static final Color DARK = Theme.SIDEBAR_BG;

    private static final DateTimeFormatter DATE_PARSER = DateTimeFormatter
            .ofPattern("MM/dd/uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter NICE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.ENGLISH);

    private final ClinicManager clinic;
    private final Runnable onAdded;

    private final JTextField nameField    = FormField.textField("Enter full name");
    private final JTextField ageField     = FormField.textField("Enter age");
    private final JTextField contactField = FormField.textField("e.g. 0912 345 6789");
    private final JTextField reasonField  = FormField.textField("e.g. Check-up, lab test...");
    private final JTextField dateField    = FormField.textField("MM/DD/YYYY");
    private final JComboBox<Doctor> doctorCombo = new JComboBox<>();
    private final TimePicker timePicker = new TimePicker();
    private final IconButton calendarButton = new IconButton(
            Icons.of(Icons.Type.CALENDAR, 20, DARK), "Pick a date", new Color(0, 0, 0, 20));
    private final JLabel errorLabel = new JLabel(" ");
    private PillButton addButton;

    public static void open(JFrame owner, ClinicManager clinic, Runnable onAdded) {
        Component oldGlass = owner.getGlassPane();
        JComponent dim = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 120));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        dim.addMouseListener(new MouseAdapter() { });
        owner.setGlassPane(dim);
        dim.setVisible(true);

        AddAppointmentDialog dialog = new AddAppointmentDialog(owner, clinic, onAdded);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        dim.setVisible(false);
        owner.setGlassPane(oldGlass);
    }

    private AddAppointmentDialog(JFrame owner, ClinicManager clinic, Runnable onAdded) {
        super(owner, "Add Appointment", true);
        this.clinic = clinic;
        this.onAdded = onAdded;

        setUndecorated(true);

        Surface surface = new Surface();
        surface.setLayout(new BorderLayout());
        surface.add(buildHeader(), BorderLayout.NORTH);
        surface.add(buildForm(), BorderLayout.CENTER);
        setContentPane(surface);

        loadDoctors();
        installFilters();
        setSchedulingEnabled(false); // locked until a doctor is chosen

        getRootPane().setDefaultButton(addButton);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { nameField.requestFocusInWindow(); }
        });
        pack();
        setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 34, 34));
    }

    private JComponent buildHeader() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(22, 24, 16, 24));

        JLabel title = new JLabel("Add Appointment");
        title.setFont(Theme.font(Font.BOLD, 30));
        title.setForeground(DARK);
        row.add(new JLabel(Icons.of(Icons.Type.CALENDAR, 42, DARK)));
        row.add(title);

        JPanel divider = new JPanel();
        divider.setBackground(new Color(0xD9D9D9));
        divider.setPreferredSize(new Dimension(1, 2));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(row, BorderLayout.CENTER);
        wrap.add(divider, BorderLayout.SOUTH);
        return wrap;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(22, 30, 28, 30));

        place(form, cell("Full Name", new FormField(null, nameField, null)), 0, 0);
        place(form, cell("Age", new FormField(null, ageField, null)), 1, 0);

        place(form, cell("Contact Number", new FormField(null, contactField, null)), 0, 1);

        styleCombo(doctorCombo, "Select a doctor first", d -> d.getName() + " - " + d.getSpecialization());
        place(form, cell("Assigned Doctor", comboRow(Icons.Type.USER, doctorCombo)), 1, 1);

        place(form, cell("Reason for Visit", new FormField(null, reasonField, null)), 0, 2, 2);

        FormField dateRow = new FormField(Icons.of(Icons.Type.CALENDAR, 20, DARK), dateField, calendarButton);
        calendarButton.addActionListener(e -> new CalendarPopup(
                parseDate(dateField.getText()), LocalDate.now().plusDays(1),
                d -> dateField.setText(d.format(DATE_FMT)))
                .show(dateRow, 0, dateRow.getHeight() + 4));
        place(form, cell("Appointment Date", dateRow), 0, 3);

        JPanel timeCell = cell("Appointment Time (7 AM - 5 PM)", wrapField(timePicker));
        place(form, timeCell, 1, 3);

        JLabel doctorFirstNote = new JLabel("Pick a doctor above to unlock the date and time.");
        doctorFirstNote.setFont(Theme.font(Font.PLAIN, 13));
        doctorFirstNote.setForeground(Theme.TEXT_MUTED);
        GridBagConstraints nc = new GridBagConstraints();
        nc.gridx = 0; nc.gridy = 4; nc.gridwidth = 2;
        nc.fill = GridBagConstraints.HORIZONTAL;
        nc.insets = new Insets(0, 4, 10, 0);
        form.add(doctorFirstNote, nc);

        doctorCombo.addItemListener(e -> {
            boolean chosen = doctorCombo.getSelectedItem() != null;
            setSchedulingEnabled(chosen);
            doctorFirstNote.setVisible(!chosen);
        });

        errorLabel.setFont(Theme.font(Font.PLAIN, 14));
        errorLabel.setForeground(Theme.ERROR);
        GridBagConstraints ec = new GridBagConstraints();
        ec.gridx = 0; ec.gridy = 5; ec.gridwidth = 2;
        ec.fill = GridBagConstraints.HORIZONTAL;
        ec.insets = new Insets(0, 4, 10, 0);
        form.add(errorLabel, ec);

        addButton = PillButton.primary("Add Appointment", Icons.of(Icons.Type.PLUS_CIRCLE, 24, Color.WHITE));
        PillButton cancelButton = PillButton.secondary("Cancel");
        addButton.addActionListener(e -> submit());
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 40, 0));
        buttons.setOpaque(false);
        buttons.add(addButton);
        buttons.add(cancelButton);
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 0; bc.gridy = 6; bc.gridwidth = 2;
        bc.fill = GridBagConstraints.HORIZONTAL;
        form.add(buttons, bc);

        return form;
    }

    private void setSchedulingEnabled(boolean enabled) {
        dateField.setEnabled(enabled);
        calendarButton.setEnabled(enabled);
        timePicker.setEnabled(enabled);
        for (Component c : timePicker.getComponents()) c.setEnabled(enabled);
    }

    private static JComponent wrapField(JComponent inner) {
        FormField row = new FormField(null, wrapAsTextComponent(inner), null);
        return row;
    }

    /** FormField expects a JTextComponent-shaped center piece; wrap the TimePicker in a plain holder instead. */
    private static JComponent wrapAsTextComponent(JComponent inner) {
        // FormField just needs any JComponent in its CENTER slot visually; TimePicker works directly.
        return inner;
    }

    private static JPanel cell(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        JLabel l = new JLabel("<html>" + label + "<font color='#E53935'>*</font></html>");
        l.setFont(Theme.font(Font.BOLD, 18));
        l.setForeground(DARK);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static void place(JPanel form, JComponent comp, int col, int row) {
        place(form, comp, col, row, 1);
    }

    private static void place(JPanel form, JComponent comp, int col, int row, int width) {
        Dimension d = comp.getPreferredSize();
        comp.setPreferredSize(new Dimension(width == 2 ? 694 : (col == 0 ? 400 : 280), Math.max(d.height, 46)));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = col;
        gc.gridy = row;
        gc.gridwidth = width;
        gc.weightx = width == 2 ? 1.0 : (col == 0 ? 0.58 : 0.42);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTH;
        gc.insets = new Insets(0, col == 0 || width == 2 ? 0 : 14, 18, col == 0 || width == 2 ? 14 : 0);
        form.add(comp, gc);
    }

    private static FormField comboRow(Icons.Type icon, JComboBox<?> combo) {
        IconButton chevron = new IconButton(
                Icons.of(Icons.Type.CHEVRON_DOWN, 18, DARK), "Show options", new Color(0, 0, 0, 20));
        chevron.addActionListener(e -> combo.setPopupVisible(!combo.isPopupVisible()));
        return new FormField(Icons.of(icon, 20, DARK), combo, chevron);
    }

    private static <T> void styleCombo(JComboBox<T> combo, String placeholder, Function<T, String> label) {
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton none = new JButton();
                none.setPreferredSize(new Dimension(0, 0));
                none.setBorder(null);
                none.setOpaque(false);
                none.setContentAreaFilled(false);
                none.setFocusable(false);
                return none;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                // transparent: the FormField draws the background
            }
        });
        combo.setOpaque(false);
        combo.setBorder(null);
        combo.setFont(Theme.font(Font.PLAIN, 15));
        combo.setSelectedIndex(-1);

        combo.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) { }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { repaintLater(combo); }
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { repaintLater(combo); }
        });
        combo.addItemListener(e -> repaintLater(combo));
        combo.addActionListener(e -> repaintLater(combo));

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            @SuppressWarnings("unchecked")
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                boolean inPopup = index >= 0;
                if (value == null) {
                    l.setText(placeholder);
                    l.setForeground(Theme.TEXT_MUTED);
                } else {
                    l.setText(label.apply((T) value));
                    l.setForeground(inPopup && isSelected ? Color.WHITE : Theme.TEXT_DARK);
                }
                l.setOpaque(inPopup);
                if (inPopup) l.setBackground(isSelected ? Theme.SIDEBAR_ACTIVE : Color.WHITE);
                l.setBorder(new EmptyBorder(6, inPopup ? 10 : 0, 6, 8));
                return l;
            }
        });
    }

    private static void repaintLater(JComponent c) {
        // JComboBox with a custom UI sometimes doesn't repaint its displayed
        // value right away; force it immediately and again next pulse so the
        // picked doctor shows up without needing an unrelated click first.
        Runnable force = () -> {
            c.invalidate();
            c.revalidate();
            Window w = SwingUtilities.getWindowAncestor(c);
            if (w != null) w.repaint(); else c.repaint();
        };
        force.run();
        SwingUtilities.invokeLater(force);
    }

    private void loadDoctors() {
        List<Doctor> available = new ArrayList<>();
        for (Doctor d : clinic.getDoctors()) if (d.isAvailable()) available.add(d);
        available.sort(Comparator.comparing(Doctor::getName));
        for (Doctor d : available) doctorCombo.addItem(d);
        doctorCombo.setSelectedIndex(-1);
        doctorCombo.revalidate(); doctorCombo.repaint();
    }

    private void installFilters() {
        limit(nameField, 50, c -> Character.isLetter(c) || c == ' ' || c == '.' || c == '\'' || c == '-');
        limit(ageField, 3, c -> c >= '0' && c <= '9');
        limit(contactField, 13, c -> (c >= '0' && c <= '9') || c == ' ' || c == '-');
        limit(reasonField, InputValidator.MAX_REASON_LENGTH, c -> true);
        limit(dateField, 10, c -> (c >= '0' && c <= '9') || c == '/');
    }

    private static void limit(JTextField field, int max, IntPredicate allowed) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                    throws BadLocationException {
                replace(fb, offset, 0, text, attrs);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text == null) {
                    super.replace(fb, offset, length, null, attrs);
                    return;
                }
                StringBuilder ok = new StringBuilder();
                for (char c : text.toCharArray()) if (allowed.test(c)) ok.append(c);
                int room = max - (fb.getDocument().getLength() - length);
                if (room <= 0 && ok.length() > 0) return;
                if (ok.length() > room) ok.setLength(room);
                super.replace(fb, offset, length, ok.toString(), attrs);
            }
        });
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim(), DATE_PARSER);
        } catch (Exception e) {
            return null;
        }
    }

    private void submit() {
        errorLabel.setText(" ");

        String name = InputValidator.normalizeSpaces(nameField.getText());
        if (!InputValidator.isValidName(name)) {
            fail("Please enter a valid full name (letters only, 2-50 characters).", nameField);
            return;
        }

        int age = InputValidator.parseAge(ageField.getText());
        if (age < 0) {
            fail("Age must be a whole number from " + InputValidator.MIN_AGE + " to " + InputValidator.MAX_AGE + ".", ageField);
            return;
        }

        String contact = contactField.getText().replaceAll("[\\s-]", "");
        if (!InputValidator.isValidContact(contact)) {
            fail("Contact number must start with 09 and have 11 digits.", contactField);
            return;
        }

        Doctor doctor = (Doctor) doctorCombo.getSelectedItem();
        if (doctor == null) {
            fail(doctorCombo.getItemCount() == 0 ? "No doctors are available right now."
                    : "Please select a doctor first.", doctorCombo);
            return;
        }

        String reason = InputValidator.normalizeSpaces(reasonField.getText());
        if (!InputValidator.isValidReason(reason)) {
            fail("Please enter the reason for this visit.", reasonField);
            return;
        }

        LocalDate date = parseDate(dateField.getText());
        if (date == null) {
            fail("Please pick an appointment date (MM/DD/YYYY).", dateField);
            return;
        }
        if (!date.isAfter(LocalDate.now())) {
            fail("Appointments must be booked for a following day, not today.", dateField);
            return;
        }

        LocalDateTime when = LocalDateTime.of(date, timePicker.getTime());

        try {
            Patient p = clinic.registerAppointment(name, age, contact, doctor.getDoctorId(), when, reason);
            JOptionPane.showMessageDialog(this,
                    "Appointment booked.\n\n"
                            + "Patient ID: " + p.getPatientId() + "\n"
                            + "Doctor: " + doctor.getName() + "\n"
                            + "Appointment: " + when.format(NICE_FMT),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            if (onAdded != null) onAdded.run();
        } catch (Exception ex) {
            errorLabel.setText(ex.getMessage());
        }
    }

    private void fail(String message, JComponent focusTarget) {
        errorLabel.setText(message);
        focusTarget.requestFocusInWindow();
    }

    private static class Surface extends JPanel {
        Surface() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 34, 34);
            g2.dispose();
        }
    }
}