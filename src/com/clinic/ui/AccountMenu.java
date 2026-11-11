package com.clinic.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public final class AccountMenu {

    public static void show(Component anchor, Runnable onSettings, Runnable onLogout,
                            boolean alignRight, boolean openAbove) {
        JPopupMenu menu = new JPopupMenu() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
            }
        };
        menu.setOpaque(false);
        menu.setLightWeightPopupEnabled(true);
        menu.setBackground(Color.WHITE);
        menu.setBorder(new CompoundBorder(new UiKit.RoundBorder(Theme.CARD_STROKE, 18, 0, 0), new EmptyBorder(8, 8, 8, 8)));

        JLabel name = new JLabel("Admin");
        name.setFont(Theme.font(Font.BOLD, 16));
        name.setForeground(Theme.TEXT_DARK);
        JLabel role = new JLabel("System Administrator");
        role.setFont(Theme.font(Font.PLAIN, 12));
        role.setForeground(Theme.TEXT_MUTED);
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBorder(new EmptyBorder(4, 16, 8, 16));
        head.add(name);
        head.add(role);

        MenuButton settings = new MenuButton("Settings", Theme.TEXT_DARK);
        settings.addActionListener(e -> { menu.setVisible(false); onSettings.run(); });

        MenuButton logout = new MenuButton("Logout", Theme.ERROR);
        logout.addActionListener(e -> { menu.setVisible(false); onLogout.run(); });

        menu.add(head);
        menu.addSeparator();
        menu.add(settings);
        menu.add(logout);

        Dimension size = menu.getPreferredSize();
        int x = alignRight ? anchor.getWidth() - size.width : 0;
        int y = openAbove ? -size.height - 4 : anchor.getHeight() + 4;
        menu.show(anchor, x, y);
    }

    /** Row inside the popup with a rounded hover highlight. */
    static class MenuButton extends JButton {
        MenuButton(String text, Color fg) {
            super(text);
            setFont(Theme.font(Font.PLAIN, 15));
            setForeground(fg);
            setHorizontalAlignment(LEFT);
            setBorder(new EmptyBorder(10, 16, 10, 40));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Theme.hand());
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, super.getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (getModel().isRollover()) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setColor(new Color(0xE3EEF3));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private AccountMenu() {}
}