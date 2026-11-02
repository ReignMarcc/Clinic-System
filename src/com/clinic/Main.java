package com.clinic;

import com.clinic.service.ClinicManager;
import com.clinic.ui.LoginFrame;
import com.clinic.ui.Mainframe;

import javax.swing.*;

public class    Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        ClinicManager clinic = new ClinicManager();

        SwingUtilities.invokeLater(() -> {
            new LoginFrame(() -> {
                new Mainframe(clinic).setVisible(true);
            }).setVisible(true);
        });
    }
}