package com.clinic.ui;

import com.clinic.model.Doctor;
import com.clinic.service.ClinicManager;
import com.clinic.util.AdminCredentials;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Settings page, phone-style: a tappable list of menu items. Tapping one
 * reveals its content in place of the list; a back link returns to the menu.
 * Items: "Account Settings" and "Doctor Settings" (add / edit / remove doctors).
 * More items can be added later without changing this structure.
 */
public class SettingsPanel extends JPanel {

    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);

    private final JTextField currentEmail = new JTextField(AdminCredentials.getEmail(), 24);
    private final JPasswordField currentPassword = new JPasswordField(24);
    private final JTextField newEmail = new JTextField(AdminCredentials.getEmail(), 24);
    private final JPasswordField newPassword = new JPasswordField(24);
    private final JPasswordField confirmPassword = new JPasswordField(24);

    private static final String MENU = "menu";
    private static final String ACCOUNT = "account";
    private static final String DOCTORS = "doctors";

    private final DoctorSettingsPanel doctorSettings;

    /** @param onViewDoctor called when "View records" is chosen on a doctor (opens that doctor's record) */
    public SettingsPanel(ClinicManager clinic, Consumer<Doctor> onViewDoctor) {
        setLayout(new BorderLayout(0, 18));
        setBackground(Theme.CONTENT_BG);
        setBorder(new EmptyBorder(18, 22, 22, 22));

        add(UiKit.pageTopBar("Settings"), BorderLayout.NORTH);

        body.setOpaque(false);
        doctorSettings = new DoctorSettingsPanel(clinic, () -> cards.show(body, MENU), onViewDoctor);

        body.add(buildMenu(), MENU);
        body.add(buildAccountSettings(), ACCOUNT);
        body.add(doctorSettings, DOCTORS);
        add(body, BorderLayout.CENTER);

        cards.show(body, MENU);
    }

    /** Back to the list of settings (used when Settings is opened from the sidebar). */
    public void showMenu() {
        cards.show(body, MENU);
    }

    /** Reloads anything that can change elsewhere in the app (the doctor list). */
    public void refresh() {
        doctorSettings.refresh();
    }

    // ------------------------------------------------------------------
    // Menu list
    // ------------------------------------------------------------------

    private JComponent buildMenu() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);

        UiKit.Card card = new UiKit.Card(20, Color.WHITE, Theme.CARD_STROKE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(8, 8, 8, 8));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(menuRow("Account Settings", "Change your login email and password",
                Icons.Type.USER, () -> cards.show(body, ACCOUNT)));
        card.add(divider());
        card.add(menuRow("Doctor Settings", "Add, edit, or remove the clinic's doctors",
                Icons.Type.DOCTOR, () -> { doctorSettings.refresh(); cards.show(body, DOCTORS); }));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(card, BorderLayout.NORTH);
        wrap.add(top, BorderLayout.NORTH);
        return wrap;
    }

    private static JComponent divider() {
        JPanel line = new JPanel();
        line.setBackground(new Color(0xE9ECEE));
        line.setPreferredSize(new Dimension(1, 1));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return line;
    }

    private JComponent menuRow(String title, String subtitle, Icons.Type icon, Runnable onClick) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(true);
        row.setBackground(Color.WHITE);
        row.setBorder(new EmptyBorder(16, 14, 16, 14));
        row.setCursor(Theme.hand());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        JLabel iconLabel = new JLabel(Icons.of(icon, 24, Theme.PRIMARY));
        row.add(iconLabel, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(Theme.font(Font.BOLD, 17));
        t.setForeground(Theme.TEXT_DARK);
        JLabel s = new JLabel(subtitle);
        s.setFont(Theme.font(Font.PLAIN, 13));
        s.setForeground(Theme.TEXT_MUTED);
        text.add(t);
        text.add(Box.createVerticalStrut(2));
        text.add(s);
        row.add(text, BorderLayout.CENTER);

        JLabel chevron = new JLabel(Icons.of(Icons.Type.ARROW_LEFT, 16, Theme.TEXT_MUTED)) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.rotate(Math.PI, getWidth() / 2.0, getHeight() / 2.0); // arrow-left, flipped, points right
                super.paint(g2);
                g2.dispose();
            }
        };
        chevron.setBorder(new EmptyBorder(0, 0, 0, 6));
        row.add(chevron, BorderLayout.EAST);

        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { row.setBackground(new Color(0xF2F5F7)); }
            @Override public void mouseExited(MouseEvent e)  { row.setBackground(Color.WHITE); }
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
        });
        return row;
    }

    // ------------------------------------------------------------------
    // Account Settings content (only shown after tapping the menu row)
    // ------------------------------------------------------------------

    private JComponent buildAccountSettings() {
        JPanel wrap = new JPanel(new BorderLayout(0, 14));
        wrap.setOpaque(false);

        JButton back = new JButton("Back to Settings", Icons.of(Icons.Type.ARROW_LEFT, 16, Theme.PRIMARY));
        back.setFont(Theme.font(Font.BOLD, 14));
        back.setForeground(Theme.PRIMARY);
        back.setIconTextGap(8);
        back.setContentAreaFilled(false);
        back.setBorderPainted(false);
        back.setFocusPainted(false);
        back.setCursor(Theme.hand());
        back.addActionListener(e -> cards.show(body, MENU));
        wrap.add(back, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(26, 30, 26, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 6, 8, 6); c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        JLabel title = new JLabel("Account Settings"); title.setFont(Theme.font(Font.BOLD, 25)); title.setForeground(Theme.PRIMARY); card.add(title, c);
        c.gridy++; JLabel note = new JLabel("Change the email and password used to log in."); note.setForeground(Theme.TEXT_MUTED); card.add(note, c);
        c.gridwidth = 1;
        addRow(card, c, 2, "Current email", currentEmail);
        addRow(card, c, 3, "Current password", currentPassword);
        addRow(card, c, 4, "New email", newEmail);
        addRow(card, c, 5, "New password (8+ characters)", newPassword);
        addRow(card, c, 6, "Confirm new password", confirmPassword);

        JButton save = PillButton.primary("Save Changes", Icons.of(Icons.Type.SETTINGS, 16, Color.WHITE));
        save.setFont(Theme.font(Font.BOLD, 14));
        Dimension btnSize = new Dimension(190, 40);
        save.setPreferredSize(btnSize);
        save.setMinimumSize(btnSize);
        save.addActionListener(e -> saveChanges());

        c.gridx = 1; c.gridy = 7; c.anchor = GridBagConstraints.EAST; card.add(save, c);
        center.add(card);
        wrap.add(center, BorderLayout.CENTER);
        return wrap;
    }

    private void addRow(JPanel panel, GridBagConstraints base, int row, String label, JComponent field) {
        GridBagConstraints c = (GridBagConstraints) base.clone();
        c.gridy = row; c.gridx = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        field.setPreferredSize(new Dimension(270, 34)); panel.add(field, c);
    }

    private void saveChanges() {
        char[] oldPass = currentPassword.getPassword();
        char[] newPass = newPassword.getPassword();
        char[] confirm = confirmPassword.getPassword();
        try {
            if (!java.util.Arrays.equals(newPass, confirm)) throw new IllegalArgumentException("New passwords do not match.");
            AdminCredentials.change(currentEmail.getText(), oldPass, newEmail.getText(), newPass);
            currentEmail.setText(newEmail.getText().trim());
            currentPassword.setText(""); newPassword.setText(""); confirmPassword.setText("");
            JOptionPane.showMessageDialog(this, "Login credentials updated successfully.", "Settings", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() == null ? "Could not save credentials." : ex.getMessage(), "Unable to update", JOptionPane.ERROR_MESSAGE);
        } finally {
            java.util.Arrays.fill(oldPass, '\0'); java.util.Arrays.fill(newPass, '\0'); java.util.Arrays.fill(confirm, '\0');
        }
    }
}