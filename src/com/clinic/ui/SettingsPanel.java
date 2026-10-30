package com.clinic.ui;

import com.clinic.util.AdminCredentials;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Allows the signed-in clinic admin to update their login credentials. */
public class SettingsPanel extends JPanel {
    private final JTextField currentEmail = new JTextField(AdminCredentials.getEmail(), 24);
    private final JPasswordField currentPassword = new JPasswordField(24);
    private final JTextField newEmail = new JTextField(AdminCredentials.getEmail(), 24);
    private final JPasswordField newPassword = new JPasswordField(24);
    private final JPasswordField confirmPassword = new JPasswordField(24);

    public SettingsPanel() {
        setLayout(new GridBagLayout());
        setBackground(Theme.CONTENT_BG);
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

        // Mas maliit na font at icon para kumasya yung buong "Save Changes"
        JButton save = PillButton.primary("Save Changes", Icons.of(Icons.Type.SETTINGS, 16, Color.WHITE));
        save.setFont(Theme.font(Font.BOLD, 14));
        Dimension btnSize = new Dimension(165, 38);
        save.setPreferredSize(btnSize);
        save.setMinimumSize(btnSize);
        save.addActionListener(e -> saveChanges());

        c.gridx = 1; c.gridy = 7; c.anchor = GridBagConstraints.EAST; card.add(save, c);
        add(card);
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