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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * "Add Walk-in" pop-up. The visit time is fixed to the moment the patient
 * arrives (now), so there's no date/time to pick - just who they are, which
 * doctor will see them, and their reason for the visit.
 */
public class AddWalkInDialog extends JDialog {

    private static final Color DARK = Theme.SIDEBAR_BG;
    private static final DateTimeFormatter NICE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.ENGLISH);

    private final ClinicManager clinic;
    private final Runnable onAdded;

    private final JTextField nameField    = FormField.textField("Enter full name");
    private final JTextField ageField     = FormField.textField("Enter age");
    private final JTextField contactField = FormField.textField("e.g. 0912 345 6789");
    private final JTextField reasonField  = FormField.textField("e.g. Check-up, lab test...");
    private final JComboBox<Doctor> doctorCombo = new JComboBox<>();
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

        AddWalkInDialog dialog = new AddWalkInDialog(owner, clinic, onAdded);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        dim.setVisible(false);
        owner.setGlassPane(oldGlass);
    }

    private AddWalkInDialog(JFrame owner, ClinicManager clinic, Runnable onAdded) {
        super(owner, "Add Walk-in", true);
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

        JLabel title = new JLabel("Add Walk-in");
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

        styleCombo(doctorCombo, "Select available doctor", d -> d.getName() + " - " + d.getSpecialization());
        place(form, cell("Assigned Doctor", comboRow(Icons.Type.USER, doctorCombo)), 1, 1);

        place(form, cell("Reason for Visit", new FormField(null, reasonField, null)), 0, 2, 2);

        JLabel note = new JLabel("Visit date and time are recorded automatically as right now.");
        note.setFont(Theme.font(Font.PLAIN, 13));
        note.setForeground(Theme.TEXT_MUTED);
        GridBagConstraints nc = new GridBagConstraints();
        nc.gridx = 0; nc.gridy = 3; nc.gridwidth = 2;
        nc.fill = GridBagConstraints.HORIZONTAL;
        nc.insets = new Insets(0, 4, 10, 0);
        form.add(note, nc);

        errorLabel.setFont(Theme.font(Font.PLAIN, 14));
        errorLabel.setForeground(Theme.ERROR);
        GridBagConstraints ec = new GridBagConstraints();
        ec.gridx = 0; ec.gridy = 4; ec.gridwidth = 2;
        ec.fill = GridBagConstraints.HORIZONTAL;
        ec.insets = new Insets(0, 4, 10, 0);
        form.add(errorLabel, ec);

        addButton = PillButton.primary("Add Walk-in", Icons.of(Icons.Type.PLUS_CIRCLE, 24, Color.WHITE));
        PillButton cancelButton = PillButton.secondary("Cancel");
        addButton.addActionListener(e -> submit());
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 40, 0));
        buttons.setOpaque(false);
        buttons.add(addButton);
        buttons.add(cancelButton);
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 0; bc.gridy = 5; bc.gridwidth = 2;
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
        place(form, comp, col, row, 1);
    }

    private static void place(JPanel form, JComponent comp, int col, int row, int width) {
        Dimension d = comp.getPreferredSize();
        comp.setPreferredSize(new Dimension(width == 2 ? 694 : (col == 0 ? 400 : 280), d.height));
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

    private void installFilters() {
        limit(nameField, 50, c -> Character.isLetter(c) || c == ' ' || c == '.' || c == '\'' || c == '-');
        limit(ageField, 3, c -> c >= '0' && c <= '9');
        limit(contactField, 13, c -> (c >= '0' && c <= '9') || c == ' ' || c == '-');
        limit(reasonField, InputValidator.MAX_REASON_LENGTH, c -> true);
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
                    : "Please select an available doctor.", doctorCombo);
            return;
        }

        String reason = InputValidator.normalizeSpaces(reasonField.getText());
        if (!InputValidator.isValidReason(reason)) {
            fail("Please enter the reason for this visit.", reasonField);
            return;
        }

        try {
            Patient p = clinic.registerWalkIn(name, age, contact, doctor.getDoctorId(), reason);
            LocalDateTime when = clinic.getNextAppointment(p.getPatientId()) != null
                    ? clinic.getNextAppointment(p.getPatientId()).getDateTime() : LocalDateTime.now();
            JOptionPane.showMessageDialog(this,
                    "Walk-in registered.\n\n"
                            + "Patient ID: " + p.getPatientId() + "\n"
                            + "Doctor: " + doctor.getName() + "\n"
                            + "Checked in: " + when.format(NICE_FMT),
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