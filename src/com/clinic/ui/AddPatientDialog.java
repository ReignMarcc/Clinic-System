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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * "Add Patient / Appointment" pop-up.
 */
public class AddPatientDialog extends JDialog {

    private static final String DEFAULT_REASON = "Consultation";
    private static final Color DARK = Theme.SIDEBAR_BG;

    private static final DateTimeFormatter DATE_PARSER = DateTimeFormatter
            .ofPattern("MM/dd/uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter NICE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.ENGLISH);

    private final ClinicManager clinic;
    private final Runnable onAdded;

    private final JTextField nameField    = FormField.textField("Enter full name");
    private final JTextField ageField     = FormField.textField("Enter age");
    private final JTextField contactField = FormField.textField("e.g. 0912 345 6789");
    private final JTextField dateField    = FormField.textField("MM/DD/YYYY");
    private final JComboBox<Doctor> doctorCombo = new JComboBox<>();
    private final JComboBox<LocalTime> timeCombo = new JComboBox<>();
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

        AddPatientDialog dialog = new AddPatientDialog(owner, clinic, onAdded);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        dim.setVisible(false);
        owner.setGlassPane(oldGlass);
    }

    private AddPatientDialog(JFrame owner, ClinicManager clinic, Runnable onAdded) {
        super(owner, "Add Patient / Appointment", true);
        this.clinic = clinic;
        this.onAdded = onAdded;

        setUndecorated(true);

        Surface surface = new Surface();
        surface.setLayout(new BorderLayout());
        surface.add(buildHeader(), BorderLayout.NORTH);
        surface.add(buildForm(), BorderLayout.CENTER);
        setContentPane(surface);

        loadDoctors();
        loadTimes();
        installFilters();

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

        JLabel title = new JLabel("Add Patient / Appointment");
        title.setFont(Theme.font(Font.BOLD, 30));
        title.setForeground(DARK);
        row.add(new JLabel(Icons.of(Icons.Type.USER, 46, DARK)));
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

        IconButton calendarButton = new IconButton(
                Icons.of(Icons.Type.CALENDAR, 20, DARK), "Pick a date", new Color(0, 0, 0, 20));
        FormField dateRow = new FormField(Icons.of(Icons.Type.PEOPLE, 20, DARK), dateField, calendarButton);
        calendarButton.addActionListener(e -> new CalendarPopup(
                parseDate(dateField.getText()), LocalDate.now(), d -> dateField.setText(d.format(DATE_FMT)))
                .show(dateRow, 0, dateRow.getHeight() + 4));
        place(form, cell("Appointment Date", dateRow), 1, 1);

        styleCombo(doctorCombo, "Select available doctor", d -> d.getName() + " - " + d.getSpecialization());
        place(form, cell("Assigned Doctor", comboRow(Icons.Type.USER, doctorCombo)), 0, 2);

        styleCombo(timeCombo, "Select time", t -> t.format(TIME_FMT));
        place(form, cell("Appointment Time", comboRow(Icons.Type.CALENDAR_CLOCK, timeCombo)), 1, 2);

        errorLabel.setFont(Theme.font(Font.PLAIN, 14));
        errorLabel.setForeground(Theme.ERROR);
        GridBagConstraints ec = new GridBagConstraints();
        ec.gridx = 0; ec.gridy = 3; ec.gridwidth = 2;
        ec.fill = GridBagConstraints.HORIZONTAL;
        ec.insets = new Insets(0, 4, 10, 0);
        form.add(errorLabel, ec);

        addButton = PillButton.primary("Add Patient", Icons.of(Icons.Type.PLUS_CIRCLE, 24, Color.WHITE));
        PillButton cancelButton = PillButton.secondary("Cancel");
        addButton.addActionListener(e -> submit());
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 40, 0));
        buttons.setOpaque(false);
        buttons.add(addButton);
        buttons.add(cancelButton);
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 0; bc.gridy = 4; bc.gridwidth = 2;
        bc.fill = GridBagConstraints.HORIZONTAL;
        form.add(buttons, bc);

        return form;
    }

    private static JPanel cell(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        JLabel l = new JLabel("<html>" + label + "<font color='#E53935'>*</font></html>");
        l.setFont(Theme.font(Font.BOLD, 20));
        l.setForeground(DARK);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static void place(JPanel form, JComponent comp, int col, int row) {
        Dimension d = comp.getPreferredSize();
        comp.setPreferredSize(new Dimension(col == 0 ? 400 : 280, d.height));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = col;
        gc.gridy = row;
        gc.weightx = col == 0 ? 0.58 : 0.42;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTH;
        gc.insets = new Insets(0, col == 0 ? 0 : 14, 18, col == 0 ? 14 : 0);
        form.add(comp, gc);
    }

    private static FormField comboRow(Icons.Type icon, JComboBox<?> combo) {
        IconButton chevron = new IconButton(
                Icons.of(Icons.Type.CHEVRON_DOWN, 18, DARK), "Show options", new Color(0, 0, 0, 20));
        chevron.addActionListener(e -> combo.setPopupVisible(!combo.isPopupVisible()));
        return new FormField(Icons.of(icon, 20, DARK), combo, chevron);
    }

    /** Makes a combo box look flat inside a FormField and shows placeholder text when nothing is selected. */
    private static <T> void styleCombo(JComboBox<T> combo, String placeholder, Function<T, String> label) {
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                squareButton = false;   // ito yung nagpapalabas ng gray na parihaba
            }

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
    /** Forces the whole dialog to repaint, since the popup can leave stale pixels on the transparent window. */
    private static void repaintLater(JComponent c) {
        SwingUtilities.invokeLater(() -> {
            JRootPane root = SwingUtilities.getRootPane(c);
            if (root != null) root.repaint();
            else c.repaint();
        });
    }
    private void loadDoctors() {
        List<Doctor> available = new ArrayList<>();
        for (Doctor d : clinic.getDoctors()) if (d.isAvailable()) available.add(d);
        available.sort(Comparator.comparing(Doctor::getName));
        for (Doctor d : available) doctorCombo.addItem(d);
        doctorCombo.setSelectedIndex(-1);
        doctorCombo.revalidate(); doctorCombo.repaint();
    }

    private void loadTimes() {
        for (LocalTime t = InputValidator.CLINIC_OPEN; t.isBefore(InputValidator.CLINIC_CLOSE); t = t.plusMinutes(30)) {
            timeCombo.addItem(t);
        }
        timeCombo.setSelectedIndex(-1);
        timeCombo.revalidate(); timeCombo.repaint();
    }

    private void installFilters() {
        limit(nameField, 50, c -> Character.isLetter(c) || c == ' ' || c == '.' || c == '\'' || c == '-');
        limit(ageField, 3, c -> c >= '0' && c <= '9');
        limit(contactField, 13, c -> (c >= '0' && c <= '9') || c == ' ' || c == '-');
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

        LocalDate date = parseDate(dateField.getText());
        if (date == null) {
            fail("Please pick an appointment date (MM/DD/YYYY).", dateField);
            return;
        }

        Doctor doctor = (Doctor) doctorCombo.getSelectedItem();
        if (doctor == null) {
            fail(doctorCombo.getItemCount() == 0 ? "No doctors are available right now."
                    : "Please select an available doctor.", doctorCombo);
            return;
        }

        LocalTime time = (LocalTime) timeCombo.getSelectedItem();
        if (time == null) {
            fail("Please select an appointment time.", timeCombo);
            return;
        }

        LocalDateTime when = LocalDateTime.of(date, time);
        String timeError = InputValidator.appointmentTimeError(when);
        if (timeError != null) {
            fail(timeError, dateField);
            return;
        }

        try {
            Patient p = clinic.registerPatient(name, age, contact, doctor.getDoctorId(), when, DEFAULT_REASON);
            JOptionPane.showMessageDialog(this,
                    "Patient registered successfully.\n\n"
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