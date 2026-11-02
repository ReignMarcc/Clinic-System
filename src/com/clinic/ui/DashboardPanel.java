package com.clinic.ui;

import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.service.ClinicManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The "Main Dashboard" page. Call refresh() whenever it becomes visible
 * so the numbers and today's schedule reflect the latest records.
 */
public class DashboardPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final ClinicManager clinic;
    private final Runnable onViewAll;

    private final StatCard patientsCard = new StatCard(Icons.Type.PEOPLE, Theme.TILE_BLUE, "Active Patients");
    private final StatCard pendingCard  = new StatCard(Icons.Type.CALENDAR_CLOCK, Theme.TILE_YELLOW, "Pending Appointments");
    private final StatCard doctorsCard  = new StatCard(Icons.Type.GROUP, Theme.TILE_GREEN, "Doctors On Duty");

    private final JLabel dateLabel = new JLabel();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Patient Name", "Doctor", "Time", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JPanel tableHolder = new JPanel(new CardLayout());

    public DashboardPanel(ClinicManager clinic, Runnable onViewAll) {
        this.clinic = clinic;
        this.onViewAll = onViewAll;

        setLayout(new BorderLayout(0, 18));
        setBackground(Theme.CONTENT_BG);
        setBorder(new EmptyBorder(18, 22, 22, 22));

        add(buildTopBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 26));
        body.setOpaque(false);
        body.add(buildStats(), BorderLayout.NORTH);
        body.add(buildSchedule(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        refresh();
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    public void refresh() {
        dateLabel.setText(LocalDate.now().format(DATE_FMT));

        patientsCard.setValue(clinic.getActiveTodayPatients().size());
        pendingCard.setValue(clinic.getPendingAppointments().size());
        int onDuty = 0;
        for (Doctor d : clinic.getDoctors()) if (d.isAvailable()) onDuty++;
        doctorsCard.setValue(onDuty);

        Map<String, String> names = new HashMap<>();
        for (Patient p : clinic.getPatients()) names.put(p.getPatientId().toUpperCase(), p.getName());
        Map<String, String> doctorNames = new HashMap<>();
        for (Doctor d : clinic.getDoctors()) doctorNames.put(d.getDoctorId().toUpperCase(), d.getName());

        LocalDate today = LocalDate.now();
        List<Appointment> todays = new ArrayList<>();
        for (Appointment a : clinic.getAppointments()) {
            if (a.getDateTime().toLocalDate().equals(today)) todays.add(a);
        }
        todays.sort(Comparator.comparing(Appointment::getDateTime));

        tableModel.setRowCount(0);
        for (Appointment a : todays) {
            tableModel.addRow(new Object[]{
                    names.getOrDefault(a.getPatientId().toUpperCase(), a.getPatientId()),
                    a.getDoctorId().isEmpty() ? "Unassigned" : doctorNames.getOrDefault(a.getDoctorId().toUpperCase(), a.getDoctorId()),
                    a.getDateTime().format(TIME_FMT),
                    a.getStatus().name()
            });
        }
        ((CardLayout) tableHolder.getLayout()).show(tableHolder, todays.isEmpty() ? "empty" : "table");
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private JComponent buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JLabel title = new JLabel("Dashboard");
        title.setFont(Theme.font(Font.BOLD, 22));
        title.setForeground(Theme.TEXT_DARK);
        bar.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        dateLabel.setFont(Theme.font(Font.PLAIN, 13));
        dateLabel.setForeground(Theme.TEXT_DARK);
        right.add(dateLabel);
        right.add(new JLabel(Icons.of(Icons.Type.CALENDAR, 18, Theme.PRIMARY)));
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JComponent buildStats() {
        JPanel row = new JPanel(new GridLayout(1, 3, 26, 0));
        row.setOpaque(false);
        row.add(patientsCard);
        row.add(pendingCard);
        row.add(doctorsCard);
        return row;
    }

    private JComponent buildSchedule() {
        RoundedPanel card = new RoundedPanel(18);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(18, 24, 22, 24));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = new JLabel("Today's Schedule");
        title.setFont(Theme.font(Font.BOLD, 22));
        title.setForeground(Theme.TEXT_DARK);

        JButton viewAll = new JButton("View all");
        viewAll.setFont(Theme.font(Font.BOLD, 15));
        viewAll.setForeground(Theme.TEXT_DARK);
        viewAll.setBorderPainted(false);
        viewAll.setContentAreaFilled(false);
        viewAll.setFocusPainted(false);
        viewAll.setCursor(Theme.hand());
        viewAll.addActionListener(e -> onViewAll.run());

        head.add(title, BorderLayout.WEST);
        head.add(viewAll, BorderLayout.EAST);

        JLabel empty = new JLabel("No appointments scheduled for today.", SwingConstants.CENTER);
        empty.setFont(Theme.font(Font.PLAIN, 16));
        empty.setForeground(Theme.TEXT_MUTED);

        tableHolder.setOpaque(false);
        tableHolder.add(buildTable(), "table");
        tableHolder.add(empty, "empty");

        card.add(head, BorderLayout.NORTH);
        card.add(tableHolder, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildTable() {
        JTable table = new JTable(tableModel);
        table.setRowHeight(54);
        table.setFont(Theme.font(Font.PLAIN, 15));
        table.setForeground(Theme.TEXT_DARK);
        table.setBackground(Color.WHITE);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xE2E2E2));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setRowSelectionAllowed(false);
        table.setFocusable(false);
        table.setFillsViewportHeight(true);

        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(170);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                l.setBorder(new EmptyBorder(0, 18, 0, 8));
                l.setForeground(Theme.TEXT_DARK);
                return l;
            }
        });
        table.getColumnModel().getColumn(3).setCellRenderer(new StatusRenderer());

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setPreferredSize(new Dimension(100, 50));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                l.setOpaque(true);
                l.setBackground(Theme.CONTENT_BG);
                l.setForeground(Theme.TEXT_DARK);
                l.setFont(Theme.font(Font.BOLD, 17));
                l.setBorder(new EmptyBorder(0, 18, 0, 8));
                return l;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(new Color(0xD3D3D3), 2, true));
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    // ------------------------------------------------------------------
    // Components
    // ------------------------------------------------------------------

    /** White rounded card with a thin outline. */
    private static class RoundedPanel extends JPanel {
        private final int arc;

        RoundedPanel(int arc) {
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(Theme.CARD_STROKE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
        }
    }

    /** One of the three summary cards: coloured icon tile, big number, caption. */
    private static class StatCard extends RoundedPanel {
        private final JLabel value = new JLabel("0");

        StatCard(Icons.Type icon, Color tileColor, String caption) {
            super(18);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(16, 18, 14, 18));
            setPreferredSize(new Dimension(100, 150));

            Tile tile = new Tile(icon, tileColor);
            value.setFont(Theme.font(Font.BOLD, 36));
            value.setForeground(Theme.TEXT_DARK);
            value.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel cap = new JLabel(caption);
            cap.setFont(Theme.font(Font.PLAIN, 15));
            cap.setForeground(Theme.TEXT_DARK);
            cap.setAlignmentX(Component.LEFT_ALIGNMENT);

            add(tile);
            add(Box.createVerticalStrut(10));
            add(value);
            add(cap);
        }

        void setValue(int n) {
            value.setText(String.valueOf(n));
        }
    }

    private static class Tile extends JComponent {
        private final Icon icon;
        private final Color color;

        Tile(Icons.Type type, Color color) {
            this.icon = Icons.of(type, 28, Theme.TEXT_DARK);
            this.color = color;
            Dimension d = new Dimension(46, 46);
            setPreferredSize(d);
            setMaximumSize(d);
            setMinimumSize(d);
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            icon.paintIcon(this, g2, (getWidth() - 28) / 2, (getHeight() - 28) / 2);
            g2.dispose();
        }
    }

    /** Coloured rounded label used for appointment status. */
    private static class Pill extends JLabel {
        private Color fill = Color.LIGHT_GRAY;

        Pill() {
            setFont(Theme.font(Font.BOLD, 13));
            setHorizontalAlignment(CENTER);
            setOpaque(false);
        }

        void style(String text, Color fill, Color fg) {
            setText(text);
            this.fill = fill;
            setForeground(fg);
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

    private static class StatusRenderer implements TableCellRenderer {
        private final JPanel holder = new JPanel(new GridBagLayout());
        private final Pill pill = new Pill();

        StatusRenderer() {
            holder.setOpaque(true);
            holder.setBackground(Color.WHITE);
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.WEST;
            gc.weightx = 1;
            gc.insets = new Insets(0, 18, 0, 0);
            holder.add(pill, gc);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int r, int c) {
            String status = String.valueOf(value);
            switch (status) {
                case "COMPLETED":
                    pill.style("Completed", Theme.DONE_BG, Theme.DONE_FG);
                    break;
                case "CANCELLED":
                    pill.style("Cancelled", Theme.CANCELLED_BG, Theme.CANCELLED_FG);
                    break;
                case "ON_GOING":
                    pill.style("On-going", Theme.ONGOING_BG, Theme.ONGOING_FG);
                    break;
                default:
                    pill.style("Pending", Theme.PENDING_BG, Theme.PENDING_FG);
            }
            return holder;
        }
    }
}