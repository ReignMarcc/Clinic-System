package com.clinic.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;

/** Small month calendar. Dates before `min` are greyed out and cannot be picked. */
public class CalendarPopup extends JPopupMenu {

    private static final String[] DAY_NAMES = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};

    private final LocalDate min;
    private final LocalDate selected;
    private final Consumer<LocalDate> onPick;
    private YearMonth shown;

    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel grid = new JPanel(new GridLayout(7, 7, 3, 3));
    private final JButton prev = navButton("<");
    private final JButton next = navButton(">");

    public CalendarPopup(LocalDate initial, LocalDate min, Consumer<LocalDate> onPick) {
        this.min = min;
        this.selected = initial;
        this.onPick = onPick;
        LocalDate start = (initial == null || initial.isBefore(min)) ? min : initial;
        this.shown = YearMonth.from(start);

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        root.setPreferredSize(new Dimension(320, 330));
        root.setMinimumSize(new Dimension(320, 330));

        monthLabel.setFont(Theme.font(Font.BOLD, 16));
        monthLabel.setForeground(Theme.SIDEBAR_BG);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(prev, BorderLayout.WEST);
        head.add(monthLabel, BorderLayout.CENTER);
        head.add(next, BorderLayout.EAST);

        grid.setOpaque(false);
        root.add(head, BorderLayout.NORTH);
        root.add(grid, BorderLayout.CENTER);

        prev.addActionListener(e -> { shown = shown.minusMonths(1); rebuild(); });
        next.addActionListener(e -> { shown = shown.plusMonths(1); rebuild(); });

        setOpaque(false);
        setLightWeightPopupEnabled(true);
        setBorder(new UiKit.RoundBorder(Theme.BORDER, 16, 0, 0));
        root.setOpaque(false);
        add(root);
        rebuild();
    }


    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        g2.dispose();
    }

    private static JButton navButton(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.font(Font.BOLD, 20));
        b.setForeground(Theme.SIDEBAR_BG);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Theme.hand());
        return b;
    }

    private void rebuild() {
        grid.removeAll();

        for (String name : DAY_NAMES) {
            JLabel l = new JLabel(name, SwingConstants.CENTER);
            l.setFont(Theme.font(Font.BOLD, 12));
            l.setForeground(Theme.TEXT_MUTED);
            grid.add(l);
        }

        int offset = shown.atDay(1).getDayOfWeek().getValue() % 7; // Sunday = 0
        int cells = 0;
        for (int i = 0; i < offset; i++, cells++) grid.add(new JLabel());
        for (int d = 1; d <= shown.lengthOfMonth(); d++, cells++) grid.add(dayButton(shown.atDay(d)));
        while (cells < 42) { grid.add(new JLabel()); cells++; } // keep a constant size

        monthLabel.setText(shown.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + shown.getYear());
        prev.setEnabled(shown.isAfter(YearMonth.from(min)));
        grid.revalidate();
        grid.repaint();
    }

    private JButton dayButton(LocalDate date) {
        final boolean blocked = date.isBefore(min);
        final boolean isSelected = date.equals(selected);
        final boolean isToday = date.equals(LocalDate.now());

        JButton b = new JButton(String.valueOf(date.getDayOfMonth())) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                int d = Math.min(getWidth(), getHeight()) - 2;
                int x = (getWidth() - d) / 2, y = (getHeight() - d) / 2;
                if (isSelected) {
                    g2.setColor(Theme.PRIMARY);
                    g2.fillOval(x, y, d, d);
                } else if (!blocked && getModel().isRollover()) {
                    g2.setColor(new Color(34, 96, 122, 40));
                    g2.fillOval(x, y, d, d);
                } else if (isToday) {
                    g2.setColor(Theme.PRIMARY);
                    g2.drawOval(x, y, d, d);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(Theme.font(Font.PLAIN, 14));
        b.setMargin(new Insets(2, 2, 2, 2));
        b.setForeground(isSelected ? Color.WHITE : blocked ? new Color(0xB5BDC3) : Theme.TEXT_DARK);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        if (!blocked) {
            b.setCursor(Theme.hand());
            b.addActionListener(e -> {
                setVisible(false);
                onPick.accept(date);
            });
        }
        return b;
    }
}