package com.clinic.ui;

import com.clinic.model.Doctor;
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
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * Pop-up used both to add a doctor and to edit one (same look as Add Walk-in).
 * The Doctor ID is the doctor's license number: typed in when adding, and locked afterwards.
 */
public class AddDoctorDialog extends JDialog {

    private static final Color DARK = Theme.SIDEBAR_BG;

    private final ClinicManager clinic;
    private final Runnable onSaved;
    private final Doctor editing;   // null = adding a new doctor

    private final JTextField nameField = FormField.textField("Full name (without \"Dr.\")");
    private final JTextField ageField = FormField.textField("Enter age");
    private final JTextField licenseField = FormField.textField("License number, e.g. 0123456");
    private final JTextField contactField = FormField.textField("e.g. 0912 345 6789");
    private final JTextField specField = FormField.textField("e.g. General Physician");
    private final JComboBox<String> shiftCombo = new JComboBox<>(Doctor.SHIFTS);
    private final JLabel errorLabel = new JLabel(" ");
    private PillButton saveButton;

    public static void openAdd(JFrame owner, ClinicManager clinic, Runnable onSaved) {
        show(owner, new AddDoctorDialog(owner, clinic, null, onSaved));
    }

    public static void openEdit(JFrame owner, ClinicManager clinic, Doctor doctor, Runnable onSaved) {
        show(owner, new AddDoctorDialog(owner, clinic, doctor, onSaved));
    }

    private static void show(JFrame owner, AddDoctorDialog dialog) {
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

        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        dim.setVisible(false);
        owner.setGlassPane(oldGlass);
    }

    private AddDoctorDialog(JFrame owner, ClinicManager clinic, Doctor editing, Runnable onSaved) {
        super(owner, editing == null ? "Add Doctor" : "Edit Doctor", true);
        this.clinic = clinic;
        this.editing = editing;
        this.onSaved = onSaved;

        setUndecorated(true);

        Surface surface = new Surface();
        surface.setLayout(new BorderLayout());
        surface.add(buildHeader(), BorderLayout.NORTH);
        surface.add(buildForm(), BorderLayout.CENTER);
        setContentPane(surface);

        installFilters();
        if (editing != null) fillFrom(editing);

        getRootPane().setDefaultButton(saveButton);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) {
                (editing == null ? licenseField : nameField).requestFocusInWindow();
            }
        });
        pack();
        setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 34, 34));
    }

    // ------------------------------------------------------------------
    // Layout (mirrors AddWalkInDialog)
    // ------------------------------------------------------------------

    private JComponent buildHeader() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(22, 24, 16, 24));

        JLabel title = new JLabel(editing == null ? "Add Doctor" : "Edit Doctor");
        title.setFont(Theme.font(Font.BOLD, 30));
        title.setForeground(DARK);
        row.add(new JLabel(Icons.of(Icons.Type.DOCTOR, 46, DARK)));
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

        FormField licenseRow = new FormField(null, licenseField, null);
        place(form, cell("Doctor ID (License No.)", licenseRow), 0, 1);
        place(form, cell("Contact Number", new FormField(null, contactField, null)), 1, 1);

        place(form, cell("Specialization", new FormField(null, specField, null)), 0, 2);
        styleCombo(shiftCombo, "Select shift", s -> s);
        place(form, cell("Shift", comboRow(Icons.Type.CALENDAR, shiftCombo)), 1, 2);

        if (editing != null) {
            licenseField.setEditable(false);
            licenseField.setFocusable(false);
            licenseField.setForeground(Theme.TEXT_MUTED);
        }

        JLabel note = new JLabel(editing == null
                ? "\"Dr.\" is added automatically. The Doctor ID can't be changed after saving."
                : "The Doctor ID is locked. Everything else can be updated.");
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

        saveButton = editing == null
                ? PillButton.primary("Add Doctor", Icons.of(Icons.Type.PLUS_CIRCLE, 24, Color.WHITE))
                : PillButton.primary("Save Changes", null);
        PillButton cancelButton = PillButton.secondary("Cancel");
        saveButton.addActionListener(e -> submit());
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 40, 0));
        buttons.setOpaque(false);
        buttons.add(saveButton);
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

    /** Same drop-down look as the walk-in dialog. */
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

            @Override
            @SuppressWarnings("unchecked")
            public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
                Object v = comboBox.getSelectedItem();
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setFont(comboBox.getFont());
                g2.setColor(v == null ? Theme.TEXT_MUTED : Theme.TEXT_DARK);
                FontMetrics fm = g2.getFontMetrics();
                String text = v == null ? placeholder : label.apply((T) v);
                g2.drawString(text, bounds.x, bounds.y + (bounds.height - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }

            @Override
            protected javax.swing.plaf.basic.ComboPopup createPopup() {
                javax.swing.plaf.basic.BasicComboPopup popup = new javax.swing.plaf.basic.BasicComboPopup(comboBox) {
                    @Override
                    protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
                        int wanted = list.getPreferredSize().width + 24;
                        return super.computePopupBounds(px, py, Math.max(pw, wanted), ph);
                    }

                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        Theme.antialias(g2);
                        g2.setColor(Color.WHITE);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                        g2.dispose();
                    }
                };
                popup.setOpaque(false);
                popup.setLightWeightPopupEnabled(true);
                popup.setBorder(new UiKit.RoundBorder(Theme.BORDER, 16, 5, 5));
                return popup;
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
        Runnable force = () -> {
            c.invalidate();
            c.revalidate();
            Window w = SwingUtilities.getWindowAncestor(c);
            if (w != null) w.repaint(); else c.repaint();
        };
        force.run();
        SwingUtilities.invokeLater(force);
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    private void fillFrom(Doctor d) {
        String name = d.getName();
        if (name.regionMatches(true, 0, "Dr.", 0, 3)) name = name.substring(3).trim();
        nameField.setText(name);
        ageField.setText(d.getAge() == 0 ? "" : String.valueOf(d.getAge()));
        licenseField.setText(d.getDoctorId());
        contactField.setText(d.getContactNumber());
        specField.setText(d.getSpecialization());
        shiftCombo.setSelectedItem(d.getShift());
    }

    private void installFilters() {
        limit(nameField, 46, c -> Character.isLetter(c) || c == ' ' || c == '.' || c == '\'' || c == '-', false);
        limit(ageField, 3, c -> c >= '0' && c <= '9', false);
        limit(contactField, 13, c -> (c >= '0' && c <= '9') || c == ' ' || c == '-', false);
        limit(licenseField, InputValidator.MAX_LICENSE_LENGTH,
                c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '-', true);
        limit(specField, InputValidator.MAX_SPECIALIZATION_LENGTH,
                c -> Character.isLetter(c) || " .,'&/()-".indexOf(c) >= 0, false);
    }

    private static void limit(JTextField field, int max, IntPredicate allowed, boolean upper) {
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
                for (char c : text.toCharArray()) if (allowed.test(c)) ok.append(upper ? Character.toUpperCase(c) : c);
                int room = max - (fb.getDocument().getLength() - length);
                if (room <= 0 && ok.length() > 0) return;
                if (ok.length() > room) ok.setLength(room);
                super.replace(fb, offset, length, ok.toString(), attrs);
            }
        });
    }

    private void submit() {
        errorLabel.setText(" ");

        String bare = InputValidator.normalizeSpaces(nameField.getText());
        if (bare.regionMatches(true, 0, "Dr.", 0, 3)) bare = bare.substring(3).trim();
        if (!InputValidator.isValidName(bare)) {
            fail("Please enter the doctor's full name (letters only, 2-" + InputValidator.MAX_NAME_LENGTH + " characters).", nameField);
            return;
        }

        int age = InputValidator.parseAge(ageField.getText());
        if (age < 0 || !InputValidator.isValidDoctorAge(age)) {
            fail("Age must be a whole number from " + InputValidator.MIN_DOCTOR_AGE + " to " + InputValidator.MAX_AGE + ".", ageField);
            return;
        }

        String license = licenseField.getText().trim();
        if (editing == null && !InputValidator.isValidLicenseId(license)) {
            fail("Doctor ID (license number) must be " + InputValidator.MIN_LICENSE_LENGTH + "-"
                    + InputValidator.MAX_LICENSE_LENGTH + " characters: letters, numbers and hyphens only.", licenseField);
            return;
        }

        String contact = contactField.getText().replaceAll("[\\s-]", "");
        if (!InputValidator.isValidContact(contact)) {
            fail("Contact number must start with 09 and have 11 digits.", contactField);
            return;
        }

        String spec = specField.getText();
        if (!InputValidator.isValidSpecialization(spec)) {
            fail("Please enter the doctor's specialization (letters only).", specField);
            return;
        }

        String shift = (String) shiftCombo.getSelectedItem();
        if (shift == null) {
            fail("Please choose a shift.", shiftCombo);
            return;
        }

        try {
            Doctor saved;
            if (editing == null) {
                saved = clinic.addDoctor(license, bare, age, contact, spec, shift);
            } else {
                clinic.updateDoctor(editing.getDoctorId(), bare, age, contact, spec, shift);
                saved = editing;
            }
            JOptionPane.showMessageDialog(this,
                    (editing == null ? "Doctor added.\n\n" : "Doctor updated.\n\n")
                            + "Name: " + saved.getName() + "\n"
                            + "Doctor ID: " + saved.getDoctorId() + "\n"
                            + "Specialization: " + saved.getSpecialization() + "\n"
                            + "Shift: " + saved.getShift(),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            if (onSaved != null) onSaved.run();
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
