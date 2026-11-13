package com.clinic.ui;

import com.clinic.exception.DoctorInUseException;
import com.clinic.model.Doctor;
import com.clinic.service.ClinicManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Settings > Doctor Settings. Same look as the Patients page: a header row with the
 * Add Doctor button, and a card with a table of every doctor. The three-dot button
 * on each row opens a small menu: View, Edit, Set On Leave / Set Available, Remove.
 */
public class DoctorSettingsPanel extends JPanel {

    private static final int ACTION_COL = 5;
    private static final String CARD_TABLE = "table";
    private static final String CARD_EMPTY = "empty";

    private final ClinicManager clinic;
    private final Consumer<Doctor> onViewDoctor;

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Doctor Name", "Doctor ID", "Specialization", "Shift", "Status", "Action"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel countLabel = new JLabel(" ");
    private final CardLayout bodyCards = new CardLayout();
    private final JPanel bodyHolder = new JPanel(bodyCards);
    private List<Doctor> rows = new ArrayList<>();
    private int hoverRow = -1;

    public DoctorSettingsPanel(ClinicManager clinic, Runnable onBack, Consumer<Doctor> onViewDoctor) {
        this.clinic = clinic;
        this.onViewDoctor = onViewDoctor;

        setLayout(new BorderLayout(0, 14));
        setOpaque(false);

        // Back link (same look as the Account Settings page)
        JButton back = new JButton("Back to Settings", Icons.of(Icons.Type.ARROW_LEFT, 16, Theme.PRIMARY));
        back.setFont(Theme.font(Font.BOLD, 14));
        back.setForeground(Theme.PRIMARY);
        back.setIconTextGap(8);
        back.setContentAreaFilled(false);
        back.setBorderPainted(false);
        back.setFocusPainted(false);
        back.setCursor(Theme.hand());
        back.addActionListener(e -> onBack.run());
        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backRow.setOpaque(false);
        backRow.add(back);

        // Action row: short description on the left, Add Doctor on the right
        JLabel hint = new JLabel("Manage the doctors of the clinic.");
        hint.setFont(Theme.font(Font.PLAIN, 14));
        hint.setForeground(Theme.TEXT_MUTED);

        PillButton addDoctor = PillButton.primary("Add Doctor", Icons.of(Icons.Type.PLUS_CIRCLE, 20, Color.WHITE));
        addDoctor.setFont(Theme.font(Font.BOLD, 15));
        addDoctor.setPreferredSize(new Dimension(190, 46));
        addDoctor.addActionListener(e -> {
            JFrame owner = ownerFrame();
            if (owner != null) AddDoctorDialog.openAdd(owner, clinic, this::refresh);
        });

        JPanel actionRow = new JPanel(new BorderLayout(16, 0));
        actionRow.setOpaque(false);
        actionRow.add(hint, BorderLayout.WEST);
        actionRow.add(addDoctor, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        header.add(backRow, BorderLayout.NORTH);
        header.add(actionRow, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);

        refresh();
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    public void refresh() {
        rows = new ArrayList<>(clinic.getDoctors());
        rows.sort(Comparator.comparing(Doctor::getName, String.CASE_INSENSITIVE_ORDER));

        model.setRowCount(0);
        for (Doctor d : rows) {
            model.addRow(new Object[]{
                    d.getName(),
                    d.getDoctorId(),
                    d.getSpecialization(),
                    d.getShift(),
                    d.isAvailable() ? "Available" : "On leave",
                    ""});
        }
        hoverRow = -1;
        countLabel.setText(rows.size() + (rows.size() == 1 ? " doctor" : " doctors"));
        bodyCards.show(bodyHolder, rows.isEmpty() ? CARD_EMPTY : CARD_TABLE);
        table.repaint();
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private JComponent buildTableCard() {
        UiKit.Card card = new UiKit.Card(18, Color.WHITE, null);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(18, 24, 18, 24));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = new JLabel("All Doctors");
        title.setFont(Theme.font(Font.BOLD, 22));
        title.setForeground(Theme.TEXT_DARK);
        countLabel.setFont(Theme.font(Font.PLAIN, 14));
        countLabel.setForeground(Theme.PRIMARY);
        head.add(title, BorderLayout.WEST);
        head.add(countLabel, BorderLayout.EAST);

        UiKit.styleTable(table);
        table.setRowHeight(50);
        table.getColumnModel().getColumn(0).setPreferredWidth(210);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(ACTION_COL).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setCellRenderer(new UiKit.StatusRenderer());
        table.getColumnModel().getColumn(ACTION_COL).setCellRenderer(new DotsRenderer());

        // The Action column is not an editor: a click on it just opens the popup menu.
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == ACTION_COL && row < rows.size()) showMenu(row);
            }
            @Override public void mouseExited(MouseEvent e) { setHover(-1); }
        });
        table.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                boolean onDots = row >= 0 && table.columnAtPoint(e.getPoint()) == ACTION_COL;
                table.setCursor(onDots ? Theme.hand() : Cursor.getDefaultCursor());
                setHover(onDots ? row : -1);
            }
        });

        JScrollPane scroll = UiKit.roundScroll(table, 14, null, 0f);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(Color.WHITE);

        JLabel empty = new JLabel("<html><div style='text-align:center'>No doctors yet.<br>Click <b>Add Doctor</b> to register one.</div></html>",
                SwingConstants.CENTER);
        empty.setFont(Theme.font(Font.PLAIN, 16));
        empty.setForeground(Theme.TEXT_MUTED);

        bodyHolder.setOpaque(false);
        bodyHolder.add(scroll, CARD_TABLE);
        bodyHolder.add(empty, CARD_EMPTY);

        card.add(head, BorderLayout.NORTH);
        card.add(bodyHolder, BorderLayout.CENTER);
        return card;
    }

    private void setHover(int row) {
        if (row != hoverRow) {
            hoverRow = row;
            table.repaint();
        }
    }

    private JFrame ownerFrame() {
        Window w = SwingUtilities.getWindowAncestor(this);
        return w instanceof JFrame ? (JFrame) w : null;
    }

    // ------------------------------------------------------------------
    // Three-dot menu
    // ------------------------------------------------------------------

    private void showMenu(int row) {
        Doctor d = rows.get(row);

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

        AccountMenu.MenuButton view = new AccountMenu.MenuButton("View records", Theme.TEXT_DARK);
        view.addActionListener(e -> { menu.setVisible(false); onViewDoctor.accept(d); });

        AccountMenu.MenuButton edit = new AccountMenu.MenuButton("Edit details", Theme.TEXT_DARK);
        edit.addActionListener(e -> {
            menu.setVisible(false);
            JFrame owner = ownerFrame();
            if (owner != null) AddDoctorDialog.openEdit(owner, clinic, d, this::refresh);
        });

        AccountMenu.MenuButton leave = new AccountMenu.MenuButton(d.isAvailable() ? "Set on leave" : "Set available", Theme.TEXT_DARK);
        leave.addActionListener(e -> { menu.setVisible(false); toggleLeave(d); });

        AccountMenu.MenuButton remove = new AccountMenu.MenuButton("Remove doctor", Theme.ERROR);
        remove.addActionListener(e -> { menu.setVisible(false); removeDoctor(d); });

        menu.add(view);
        menu.add(edit);
        menu.add(leave);
        menu.addSeparator();
        menu.add(remove);

        Rectangle cell = table.getCellRect(row, ACTION_COL, false);
        Dimension size = menu.getPreferredSize();
        int x = Math.max(0, cell.x + cell.width - size.width - 8);
        menu.show(table, x, cell.y + cell.height - 6);
    }

    private void toggleLeave(Doctor d) {
        try {
            if (d.isAvailable()) {
                int open = clinic.countActiveAppointments(d.getDoctorId());
                String msg = d.getName() + " will no longer be offered when booking appointments or walk-ins.";
                if (open > 0) {
                    msg += "\n\nThey still have " + open + " pending or on-going appointment" + (open == 1 ? "" : "s")
                            + ". Those stay booked, so please cancel or reschedule them if needed.";
                }
                int choice = JOptionPane.showConfirmDialog(this, msg + "\n\nSet on leave?", "Set On Leave",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) return;
                clinic.setDoctorAvailable(d.getDoctorId(), false);
            } else {
                clinic.setDoctorAvailable(d.getDoctorId(), true);
            }
            refresh();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void removeDoctor(Doctor d) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Remove " + d.getName() + " (ID " + d.getDoctorId() + ")?\nThis cannot be undone.",
                "Remove Doctor", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            clinic.removeDoctor(d.getDoctorId());
            refresh();
        } catch (DoctorInUseException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Remove Doctor", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage() == null ? "Something went wrong." : ex.getMessage(),
                "Unable to update", JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------------------
    // Three-dot cell
    // ------------------------------------------------------------------

    private class DotsRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private boolean hot;
        private final Icon icon = Icons.of(Icons.Type.MORE_VERTICAL, 20, Theme.TEXT_DARK);

        DotsRenderer() {
            setOpaque(true);
            setBackground(Color.WHITE);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            hot = (r == hoverRow);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int size = 34;
            int x = 14, y = (getHeight() - size) / 2;
            if (hot) {
                g2.setColor(new Color(0xE3EEF3));
                g2.fillRoundRect(x, y, size, size, 12, 12);
            }
            icon.paintIcon(this, g2, x + (size - icon.getIconWidth()) / 2, y + (size - icon.getIconHeight()) / 2);
            g2.dispose();
        }
    }
}
