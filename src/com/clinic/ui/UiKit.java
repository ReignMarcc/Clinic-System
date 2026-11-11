package com.clinic.ui;

import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.service.ClinicManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/** Small reusable widgets shared by the Search and Records pages. */
public final class UiKit {

    public static final Color AVATAR_BLUE = new Color(0x2B4C8C);

    private UiKit() {}

    // ------------------------------------------------------------------
    // Text helpers
    // ------------------------------------------------------------------

    /** Escapes text so it can be placed inside a JLabel's HTML. */
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** "Oct. 23, 2026" (May has no period). */
    public static String formatDate(LocalDate d) {
        String shortName = d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        String fullName = d.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return shortName + (shortName.equals(fullName) ? "" : ".") + " " + d.getDayOfMonth() + ", " + d.getYear();
    }

    // ------------------------------------------------------------------
    // Card: rounded panel with fill and optional outline
    // ------------------------------------------------------------------

    public static class Card extends JPanel {
        private final int arc;
        private Color fill;
        private Color outline;
        private float outlineWidth = 1f;

        public Card(int arc, Color fill, Color outline) {
            this.arc = arc;
            this.fill = fill;
            this.outline = outline;
            setOpaque(false);
        }

        public void setFill(Color fill) {
            this.fill = fill;
            repaint();
        }

        public void setOutline(Color outline, float width) {
            this.outline = outline;
            this.outlineWidth = width;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            if (outline != null) {
                int inset = (int) Math.ceil(outlineWidth / 2);
                g2.setColor(outline);
                g2.setStroke(new BasicStroke(outlineWidth));
                g2.drawRoundRect(inset, inset, getWidth() - 1 - 2 * inset, getHeight() - 1 - 2 * inset, arc, arc);
            }
            g2.dispose();
        }
    }


    // ------------------------------------------------------------------
    // Rounded scroll pane: clips its content to rounded corners (so rows,
    // table headers and cards never poke out with sharp edges) and uses
    // slim rounded scrollbars.
    // ------------------------------------------------------------------

    public static class RoundScrollPane extends JScrollPane {
        private final int arc;
        private final Color outline;
        private final float outlineWidth;

        public RoundScrollPane(Component view, int arc, Color outline, float outlineWidth) {
            super(view);
            this.arc = arc;
            this.outline = outline;
            this.outlineWidth = outlineWidth;
            setBorder(null);
            setOpaque(false);
            getViewport().setOpaque(false);
            setViewportBorder(null);
            styleScrollBar(getVerticalScrollBar());
            styleScrollBar(getHorizontalScrollBar());
            setCorner(JScrollPane.LOWER_RIGHT_CORNER, new JPanel() {{ setOpaque(false); }});
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.clip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));
            super.paint(g2);
            g2.dispose();
            if (outline != null) {
                Graphics2D o = (Graphics2D) g.create();
                Theme.antialias(o);
                o.setColor(outline);
                o.setStroke(new BasicStroke(outlineWidth));
                float half = outlineWidth / 2f;
                o.draw(new java.awt.geom.RoundRectangle2D.Double(half, half,
                        getWidth() - outlineWidth, getHeight() - outlineWidth, arc, arc));
                o.dispose();
            }
        }
    }

    /** Wraps a view in a rounded, clipped scroll pane. */
    public static JScrollPane roundScroll(Component view, int arc, Color outline, float outlineWidth) {
        return new RoundScrollPane(view, arc, outline, outlineWidth);
    }

    /** Rounded outline border (with optional inner padding) so bordered widgets have no sharp corners. */
    public static class RoundBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int arc;
        private final Insets pad;

        public RoundBorder(Color color, int arc, int padV, int padH) {
            this.color = color;
            this.arc = arc;
            this.pad = new Insets(padV, padH, padV, padH);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) { return (Insets) pad.clone(); }
        @Override public Insets getBorderInsets(Component c, Insets i) {
            i.set(pad.top, pad.left, pad.bottom, pad.right);
            return i;
        }
    }

    public static void styleScrollBar(JScrollBar bar) {
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(0xB5BEC5);
                trackColor = new Color(0, 0, 0, 0);
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (r.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setColor(isThumbRollover() || isDragging ? new Color(0x8E9AA3) : thumbColor);
                boolean vert = scrollbar.getOrientation() == JScrollBar.VERTICAL;
                int d = 8;
                if (vert) g2.fillRoundRect(r.x + (r.width - d) / 2, r.y + 2, d, r.height - 4, d, d);
                else g2.fillRoundRect(r.x + 2, r.y + (r.height - d) / 2, r.width - 4, d, d, d);
                g2.dispose();
            }
        });
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(14, 14));
        bar.setUnitIncrement(18);
    }

    // ------------------------------------------------------------------
    // Avatar: filled circle with an icon
    // ------------------------------------------------------------------

    public static class Avatar extends JComponent {
        private final int diameter;
        private final Color background;
        private final Icon icon;

        public Avatar(Icons.Type type, int diameter, Color background, Color iconColor) {
            this.diameter = diameter;
            this.background = background;
            this.icon = Icons.of(type, (int) Math.round(diameter * 0.58), iconColor);
            Dimension d = new Dimension(diameter, diameter);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(background);
            g2.fillOval(0, 0, diameter, diameter);
            icon.paintIcon(this, g2, (diameter - icon.getIconWidth()) / 2, (diameter - icon.getIconHeight()) / 2);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Tag: outlined rounded label with an optional icon (e.g. "Pediatrics")
    // ------------------------------------------------------------------

    public static class Tag extends JComponent {
        private final String text;
        private final Icon icon;
        private final Color color;

        public Tag(String text, Icon icon, Color color) {
            this.text = text;
            this.icon = icon;
            this.color = color;
            setFont(Theme.font(Font.BOLD, 16));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int w = 16 + (icon != null ? icon.getIconWidth() + 8 : 0) + fm.stringWidth(text) + 16;
            return new Dimension(w, 38);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, h, h);

            int x = 16;
            if (icon != null) {
                icon.paintIcon(this, g2, x, (h - icon.getIconHeight()) / 2);
                x += icon.getIconWidth() + 8;
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, x, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Pill: coloured rounded status label
    // ------------------------------------------------------------------

    public static class Pill extends JLabel {
        private Color fill = Color.LIGHT_GRAY;

        public Pill() {
            setFont(Theme.font(Font.BOLD, 13));
            setHorizontalAlignment(CENTER);
            setOpaque(false);
        }

        public void style(String text, Color fill, Color fg) {
            setText(text);
            this.fill = fill;
            setForeground(fg);
        }

        /** Colours by meaning: pending / completed / cancelled / available / on leave. */
        public void setStatus(String status) {
            String key = status == null ? "" : status.trim().toLowerCase();
            String label = key.isEmpty() ? "" : Character.toUpperCase(key.charAt(0)) + key.substring(1);
            switch (key) {
                case "completed":
                case "available":
                    style(label, Theme.DONE_BG, Theme.DONE_FG);
                    break;
                case "cancelled":
                case "on leave":
                    style(label, Theme.CANCELLED_BG, Theme.CANCELLED_FG);
                    break;
                case "pending":
                    style(label, Theme.PENDING_BG, Theme.PENDING_FG);
                    break;
                case "on_going":
                case "on-going":
                case "on going":
                    style("On-going", Theme.ONGOING_BG, Theme.ONGOING_FG);
                    break;
                default:
                    style(label, new Color(0xE3E6E8), Theme.TEXT_MUTED);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 30, d.height + 10);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Table cell renderer that shows an appointment status as a Pill. */
    public static class StatusRenderer implements TableCellRenderer {
        private final JPanel holder = new JPanel(new GridBagLayout());
        private final Pill pill = new Pill();

        public StatusRenderer() {
            holder.setOpaque(true);
            holder.setBackground(Color.WHITE);
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.WEST;
            gc.weightx = 1;
            gc.insets = new Insets(0, 14, 0, 0);
            holder.add(pill, gc);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int r, int c) {
            pill.setStatus(String.valueOf(value));
            return holder;
        }
    }

    // ------------------------------------------------------------------
    // Table styling
    // ------------------------------------------------------------------

    public static void styleTable(JTable table) {
        table.setRowHeight(46);
        table.setFont(Theme.font(Font.PLAIN, 14));
        table.setForeground(Theme.TEXT_DARK);
        table.setBackground(Color.WHITE);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xE2E2E2));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setRowSelectionAllowed(false);
        table.setFocusable(false);
        table.setFillsViewportHeight(true);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                l.setBorder(new EmptyBorder(0, 14, 0, 8));
                l.setForeground(Theme.TEXT_DARK);
                return l;
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setPreferredSize(new Dimension(100, 42));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                l.setOpaque(true);
                l.setBackground(Theme.CONTENT_BG);
                l.setForeground(Theme.TEXT_DARK);
                l.setFont(Theme.font(Font.BOLD, 14));
                l.setBorder(new EmptyBorder(0, 14, 0, 8));
                return l;
            }
        });
    }

    // ------------------------------------------------------------------
    // Page top bar: title on the left, today's date on the right.
    // Logging out happens from the admin profile in the sidebar, so this
    // bar carries nothing else. Builds the date fresh every call, so it's
    // never stale like a cached label.
    // ------------------------------------------------------------------

    public static JComponent pageTopBar(String title) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.font(Font.BOLD, 22));
        titleLabel.setForeground(Theme.TEXT_DARK);
        bar.add(titleLabel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        JLabel dateLabel = new JLabel(formatDate(LocalDate.now())); // computed now, never cached
        dateLabel.setFont(Theme.font(Font.PLAIN, 13));
        dateLabel.setForeground(Theme.TEXT_DARK);
        right.add(dateLabel);
        right.add(new JLabel(Icons.of(Icons.Type.CALENDAR, 18, Theme.PRIMARY)));

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ------------------------------------------------------------------
    // Toggle chip, e.g. the Patients / Doctors switch on Search and Records.
    // ------------------------------------------------------------------

    public static class ToggleChip extends JButton {
        private boolean active;

        public ToggleChip(String text) {
            super(text);
            setFont(Theme.font(Font.BOLD, 16));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Theme.hand());
            Dimension d = new Dimension(140, 40);
            setPreferredSize(d);
            setMaximumSize(d);
        }

        public void setActive(boolean active) {
            this.active = active;
            setForeground(active ? Color.WHITE : Theme.PRIMARY);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();
            if (active) {
                g2.setColor(Theme.PRIMARY);
                g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            } else {
                g2.setColor(getModel().isRollover() ? new Color(34, 96, 122, 28) : new Color(0, 0, 0, 0));
                g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.setColor(Theme.PRIMARY);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, h, h);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ------------------------------------------------------------------
    // Clickable card with a hover highlight, used for search/record result rows.
    // ------------------------------------------------------------------

    public static class ClickableCard extends Card {
        private final Color fillNormal;
        private final Color fillHover;
        private final Color outlineNormal;
        private final float outlineWidthNormal;

        public ClickableCard(Color fillNormal, Color fillHover, Color outline, float outlineWidth, Runnable onClick) {
            super(16, fillNormal, outline);
            this.fillNormal = fillNormal;
            this.fillHover = fillHover;
            this.outlineNormal = outline;
            this.outlineWidthNormal = outlineWidth;
            setOutline(outline, outlineWidth);
            setCursor(Theme.hand());
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    setFill(fillHover);
                    setOutline(Theme.PRIMARY, 2f);
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    setFill(fillNormal);
                    setOutline(outlineNormal, outlineWidthNormal);
                }

                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    onClick.run();
                }
            });
        }
    }

    private static final Color RESULT_CARD_FILL = new Color(0xE4E4E4);
    private static final Color RESULT_CARD_HOVER = new Color(0xF0F0F0);

    /** A patient result row: avatar, name, ID, next visit, and their doctor's specialization tag. */
    public static JComponent patientResultCard(ClinicManager clinic, Patient p, Runnable onClick) {
        Doctor assigned = clinic.getAssignedDoctor(p.getPatientId());
        Appointment next = clinic.getNextAppointment(p.getPatientId());
        String visit = next == null ? "None scheduled" : formatDate(next.getDateTime().toLocalDate());

        ClickableCard card = new ClickableCard(RESULT_CARD_FILL, RESULT_CARD_HOVER, Color.WHITE, 2f, onClick);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(12, 16, 12, 18));
        sizeResultCard(card, 98);

        card.add(gridCenter(new Avatar(Icons.Type.USER, 56, AVATAR_BLUE, Color.WHITE)), BorderLayout.WEST);

        JPanel info = vbox();
        info.add(boldLabel(p.getName(), 18));
        info.add(infoLine("Patient ID:", p.getPatientId()));
        info.add(infoLine("Next visit:", visit));
        card.add(gridCenterLeft(info), BorderLayout.CENTER);

        Icon docIcon = Icons.of(Icons.Type.DOCTOR, 20, AVATAR_BLUE);
        Tag tag = assigned != null
                ? new Tag(assigned.getSpecialization(), docIcon, Theme.PRIMARY)
                : new Tag("No doctor yet", null, Theme.TEXT_MUTED);
        card.add(gridCenter(tag), BorderLayout.EAST);
        return card;
    }

    /** A doctor result row: icon, name, specialization, and today's appointment count. */
    public static JComponent doctorResultCard(ClinicManager clinic, Doctor d, Runnable onClick) {
        int today = clinic.countAppointmentsToday(d.getDoctorId());

        ClickableCard card = new ClickableCard(RESULT_CARD_FILL, RESULT_CARD_HOVER, new Color(0x4A5560), 1f, onClick);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(12, 16, 12, 18));
        sizeResultCard(card, 92);

        card.add(gridCenter(new JLabel(Icons.of(Icons.Type.DOCTOR, 52, AVATAR_BLUE))), BorderLayout.WEST);

        JPanel info = vbox();
        info.add(boldLabel(d.getName(), 18));
        info.add(coloredLabel(d.getSpecialization(), 15));
        info.add(coloredLabel(today + (today == 1 ? " Appointment Today" : " Appointments Today"), 14));
        card.add(gridCenterLeft(info), BorderLayout.CENTER);
        return card;
    }

    private static void sizeResultCard(JComponent card, int height) {
        card.setPreferredSize(new Dimension(100, height));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static JPanel vbox() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static JPanel gridCenter(JComponent c) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    private static JPanel gridCenterLeft(JComponent c) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.weightx = 1;
        p.add(c, gc);
        return p;
    }

    private static JLabel boldLabel(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, size));
        l.setForeground(Theme.TEXT_DARK);
        return l;
    }

    private static JLabel coloredLabel(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.PLAIN, size));
        l.setForeground(Theme.PRIMARY);
        return l;
    }

    /** "Patient ID: P12345" with a teal label and a bold value. */
    private static JLabel infoLine(String label, String value) {
        JLabel l = new JLabel("<html><span style='color:#22607A'>" + esc(label) + "</span> <b>"
                + esc(value) + "</b></html>");
        l.setFont(Theme.font(Font.PLAIN, 14));
        l.setForeground(Theme.TEXT_DARK);
        return l;
    }
    
    /** White row for table action cells: left padding, children vertically centred in the row. */
    public static JPanel actionRow(int leftPad) {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
    p.setOpaque(true);
    p.setBackground(Color.WHITE);
    p.setBorder(new EmptyBorder(0, leftPad, 0, 0));
    return p;
}
    /** Small rounded action button used inside table cells (e.g. "View", "Complete", "Cancel"). */
    public static JButton tableActionButton(String text, Color fg, Color bg, Color outline) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setColor(getModel().isRollover() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (outline != null) {
                    g2.setColor(outline);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(Theme.font(Font.BOLD, 12));
        b.setForeground(fg);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setCursor(Theme.hand());
        b.setMargin(new Insets(2, 10, 2, 10));
        return b;
    }
}