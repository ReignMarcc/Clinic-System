package com.clinic.ui;

import javax.swing.*;
import java.awt.*;

/** Rounded button with hover/press colours. Use PillButton.primary(...) or PillButton.secondary(...). */
public class PillButton extends JButton {
    private final Color bg, hover, pressed, outline;

    public PillButton(String text, Icon icon, Color fg, Color bg, Color hover, Color pressed, Color outline) {
        super(text, icon);
        this.bg = bg;
        this.hover = hover;
        this.pressed = pressed;
        this.outline = outline;

        setFont(Theme.font(Font.BOLD, 19));
        setForeground(fg);
        setIconTextGap(10);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setCursor(Theme.hand());
        setPreferredSize(new Dimension(160, 54));
    }

    public static PillButton primary(String text, Icon icon) {
        return new PillButton(text, icon, Color.WHITE,
                Theme.SIDEBAR_BG, new Color(0x16506F), new Color(0x0A2D41), null);
    }

    public static PillButton secondary(String text) {
        return new PillButton(text, null, Theme.SIDEBAR_BG,
                new Color(0xC7C7C7), new Color(0xBABABA), new Color(0xA8A8A8), new Color(0x7A8F9B));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        ButtonModel m = getModel();
        g2.setColor(m.isPressed() ? pressed : m.isRollover() ? hover : bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
        if (outline != null) {
            g2.setColor(outline);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
        }
        g2.dispose();
        super.paintComponent(g); // icon + text
    }
}