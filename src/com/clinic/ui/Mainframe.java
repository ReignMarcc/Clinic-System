package com.clinic.ui;

import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.service.ClinicManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main window: top bar, collapsible sidebar and a stack of pages.
 *
 * To plug in your own panels later, call setPage(...) in the constructor, e.g.
 *     setPage(PAGE_PATIENTS, new RegistrationPanel(clinic));
 */
public class Mainframe extends JFrame {

    public static final String PAGE_DASHBOARD    = "Dashboard";
    public static final String PAGE_PATIENTS     = "Patients";
    public static final String PAGE_APPOINTMENTS = "Appointments";
    public static final String PAGE_RECORDS      = "Records";
    public static final String PAGE_REPORTS      = "Reports";
    public static final String PAGE_SETTINGS     = "Settings";

    private static final String[] PAGE_NAMES = {
            PAGE_DASHBOARD, PAGE_PATIENTS, PAGE_APPOINTMENTS, PAGE_RECORDS,
            PAGE_REPORTS, PAGE_SETTINGS
    };
    private static final Icons.Type[] PAGE_ICONS = {
            Icons.Type.HOME, Icons.Type.PATIENTS, Icons.Type.CALENDAR, Icons.Type.RECORDS,
            Icons.Type.REPORTS, Icons.Type.SETTINGS
    };

    private static final int SIDEBAR_EXPANDED = 270;
    private static final int SIDEBAR_COLLAPSED = 76;

    private final ClinicManager clinic;
    private final DashboardPanel dashboard;
    private final AppointmentPanel appointmentPanel;
    private final RegistrationPanel patientsPanel;
    private final RecordsPanel recordsPanel;
    private final ReportPanel reportPanel;
    private final Timer statusTimer;
    private final JPanel sidebar = new JPanel();
    private final JPanel pages = new JPanel(new CardLayout());
    private final JLabel headerTitle = new JLabel();
    private final Map<String, SidebarItem> navItems = new LinkedHashMap<>();
    private final Map<String, JComponent> pageMap = new HashMap<>();

    private boolean expanded = true;
    private int sidebarWidth = SIDEBAR_EXPANDED;
    private Timer sidebarAnimation;

    public Mainframe(ClinicManager clinic) {
        super("Clinic Management System");
        this.clinic = clinic;
        this.dashboard = new DashboardPanel(clinic, () -> showPage(PAGE_APPOINTMENTS));
        this.recordsPanel = new RecordsPanel(clinic);
        this.appointmentPanel = new AppointmentPanel(clinic,
                () -> showPage(PAGE_REPORTS),
                this::openPatientRecord);
        this.patientsPanel = new RegistrationPanel(clinic, this::openPatientRecord);
        this.reportPanel = new ReportPanel(clinic);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 760);
        setMinimumSize(new Dimension(820, 600));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(pages, BorderLayout.CENTER);
        setContentPane(root);

        pages.setBackground(Theme.CONTENT_BG);
        setPage(PAGE_DASHBOARD, dashboard);
        for (String name : PAGE_NAMES) {
            if (!PAGE_DASHBOARD.equals(name)) setPage(name, placeholder(name));
        }
        setPage(PAGE_SETTINGS, new SettingsPanel());
        setPage(PAGE_APPOINTMENTS, appointmentPanel);
        setPage(PAGE_RECORDS, recordsPanel);
        setPage(PAGE_PATIENTS, patientsPanel);
        setPage(PAGE_REPORTS, reportPanel);

        showPage(PAGE_DASHBOARD);

        // Every 30s, promote any PENDING appointment whose time has arrived to ON_GOING,
        // and refresh the visible pages so the change shows up without navigating away.
        statusTimer = new Timer(30_000, e -> {
            if (clinic.refreshAppointmentStatuses()) refreshData();
        });
        statusTimer.start();
    }

    // ------------------------------------------------------------------
    // Pages
    // ------------------------------------------------------------------

    /** Adds or replaces the page shown under a sidebar item. */
    public void setPage(String name, JComponent page) {
        JComponent old = pageMap.put(name, page);
        if (old != null) pages.remove(old);
        pages.add(page, name);
        pages.revalidate();
    }

    public void showPage(String name) {
        ((CardLayout) pages.getLayout()).show(pages, name);
        headerTitle.setText(headerTitleFor(name));
        navItems.forEach((key, item) -> item.setActive(key.equals(name)));
        if (PAGE_DASHBOARD.equals(name)) dashboard.refresh();
        if (PAGE_APPOINTMENTS.equals(name)) appointmentPanel.refresh();
        if (PAGE_PATIENTS.equals(name)) patientsPanel.refreshData();
        if (PAGE_RECORDS.equals(name)) recordsPanel.refresh();
    }

    private static String headerTitleFor(String name) {
        if (PAGE_DASHBOARD.equals(name)) return "Main Dashboard";
        if (PAGE_APPOINTMENTS.equals(name)) return "Pending Appointments & Reports";
        return name;
    }

    /** Opens a patient's record in the Records page (used by Search results). */
    public void openPatientRecord(Patient patient) {
        recordsPanel.showPatient(patient);
        showPage(PAGE_RECORDS);
    }

    /** Opens a doctor's record in the Records page (used by Search results). */
    public void openDoctorRecord(Doctor doctor) {
        recordsPanel.showDoctor(doctor);
        showPage(PAGE_RECORDS);
    }

    private void logout() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?", "Logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        statusTimer.stop();   // para hindi tumakbo sa background yung lumang window
        dispose();
        SwingUtilities.invokeLater(() ->
                new LoginFrame(() -> new Mainframe(clinic).setVisible(true)).setVisible(true));
    }

    private void refreshData() {
        dashboard.refresh();
        appointmentPanel.refresh();
        patientsPanel.refreshData();
        recordsPanel.refresh();
    }

    private JComponent placeholder(String name) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.CONTENT_BG);
        JLabel l = new JLabel(name + " - coming soon");
        l.setFont(Theme.font(Font.PLAIN, 20));
        l.setForeground(Theme.TEXT_MUTED);
        p.add(l);
        return p;
    }

    // ------------------------------------------------------------------
    // Header + sidebar
    // ------------------------------------------------------------------

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(0, 16, 0, 18));
        header.setPreferredSize(new Dimension(100, 62));

        IconButton toggle = new IconButton(
                Icons.of(Icons.Type.MENU, 26, Color.WHITE), "Open / close sidebar", new Color(255, 255, 255, 45));
        toggle.addActionListener(e -> toggleSidebar());

        headerTitle.setFont(Theme.font(Font.BOLD, 28));
        headerTitle.setForeground(Color.WHITE);

        header.add(toggle, BorderLayout.WEST);
        header.add(headerTitle, BorderLayout.CENTER);
        return header;
    }

    private JComponent buildSidebar() {
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.SIDEBAR_BG);
        sidebar.setBorder(new EmptyBorder(14, 12, 16, 12));
        sidebar.setPreferredSize(new Dimension(SIDEBAR_EXPANDED, 0));

        sidebar.add(new SidebarItem(Icons.Type.HEART_PULSE, 38, "Clinic Management", null, 60, false));
        sidebar.add(Box.createVerticalStrut(26));

        for (int i = 0; i < PAGE_NAMES.length; i++) {
            String name = PAGE_NAMES[i];
            SidebarItem item = new SidebarItem(PAGE_ICONS[i], 26, name, null, 50, true);
            item.setToolTipText(name); // helpful when the sidebar is collapsed
            item.addActionListener(e -> {
                if (PAGE_RECORDS.equals(name)) recordsPanel.resetToList();
                showPage(name);
            });
            navItems.put(name, item);
            sidebar.add(item);
            sidebar.add(Box.createVerticalStrut(6));
        }

        sidebar.add(Box.createVerticalGlue());
        SidebarItem admin = new SidebarItem(Icons.Type.AVATAR, 44, "Admin", "System Administrator", 66, true);
        admin.setToolTipText("Account");
        admin.addActionListener(e ->
                AccountMenu.show(admin, () -> showPage(PAGE_SETTINGS), this::logout, false, true));
        sidebar.add(admin);
        return sidebar;
    }

    private void toggleSidebar() {
        expanded = !expanded;
        final int target = expanded ? SIDEBAR_EXPANDED : SIDEBAR_COLLAPSED;

        if (sidebarAnimation != null) sidebarAnimation.stop();
        sidebarAnimation = new Timer(12, null);
        sidebarAnimation.addActionListener(e -> {
            int diff = target - sidebarWidth;
            sidebarWidth += Integer.signum(diff) * Math.min(Math.abs(diff), 20);
            sidebar.setPreferredSize(new Dimension(sidebarWidth, 0));
            sidebar.revalidate();
            sidebar.repaint();
            if (sidebarWidth == target) sidebarAnimation.stop();
        });
        sidebarAnimation.start();
    }

    // ------------------------------------------------------------------
    // Sidebar row (icon + text). The icon stays at a fixed x so it remains
    // centred when the sidebar collapses; the text simply gets clipped.
    // ------------------------------------------------------------------

    private static class SidebarItem extends JButton {
        private static final int ICON_CENTER_X = 26; // (collapsed width 76 - 2*12 padding) / 2
        private static final int TEXT_X = 62;

        private final Icons.Type iconType;
        private final int iconSize;
        private final String title;
        private final String subtitle;
        private final boolean interactive;
        private boolean active;

        SidebarItem(Icons.Type iconType, int iconSize, String title, String subtitle, int height, boolean interactive) {
            this.iconType = iconType;
            this.iconSize = iconSize;
            this.title = title;
            this.subtitle = subtitle;
            this.interactive = interactive;

            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(interactive);
            setRolloverEnabled(interactive);
            setCursor(interactive ? Theme.hand() : Cursor.getDefaultCursor());
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(100, height));
            setMinimumSize(new Dimension(0, height));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        }

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();

            if (active) {
                g2.setColor(Theme.SIDEBAR_ACTIVE);
                g2.fillRoundRect(0, 0, w, h, 18, 18);
            } else if (interactive && getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, 28));
                g2.fillRoundRect(0, 0, w, h, 18, 18);
            }

            Icons.of(iconType, iconSize, Color.WHITE)
                    .paintIcon(this, g2, ICON_CENTER_X - iconSize / 2, (h - iconSize) / 2);

            g2.setColor(Color.WHITE);
            if (subtitle == null) {
                g2.setFont(Theme.font(Font.BOLD, 17));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(title, TEXT_X, (h - fm.getHeight()) / 2 + fm.getAscent());
            } else {
                Font titleFont = Theme.font(Font.BOLD, 22);
                Font subFont = Theme.font(Font.PLAIN, 13);
                FontMetrics tf = g2.getFontMetrics(titleFont);
                FontMetrics sf = g2.getFontMetrics(subFont);
                int top = (h - (tf.getHeight() + sf.getHeight())) / 2;
                g2.setFont(titleFont);
                g2.drawString(title, TEXT_X, top + tf.getAscent());
                g2.setFont(subFont);
                g2.drawString(subtitle, TEXT_X, top + tf.getHeight() + sf.getAscent());
            }
            g2.dispose();
        }
    }
}