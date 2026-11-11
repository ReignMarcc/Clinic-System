package com.clinic.ui;

import com.clinic.model.Patient;
import com.clinic.service.ClinicManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * "Patients" page: every registered patient, with a search box and a View
 * button per row. View opens a small menu (Edit Records / Cancel Appointment)
 * that both land on that patient's record in the Records page.
 */
public class RegistrationPanel extends JPanel {

    private final ClinicManager clinic;
    private final Consumer<Patient> onViewPatient;

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Patient Name", "Age", "Contact No.", "Assigned Doctor", "Action"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return c == 4; }
    };
    private final JTable table = new JTable(model);
    private final JLabel countLabel = new JLabel(" ");
    private final JTextField searchField = FormField.textField("Search patient name or ID");
    private List<Patient> rows = new ArrayList<>();

    public RegistrationPanel(ClinicManager clinic, Consumer<Patient> onViewPatient) {
        this.clinic = clinic;
        this.onViewPatient = onViewPatient;

        setLayout(new BorderLayout(0, 18));
        setBackground(Theme.CONTENT_BG);
        setBorder(new EmptyBorder(18, 22, 22, 22));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UiKit.pageTopBar("Patients"), BorderLayout.CENTER);

        // Search box (kaliwa) + Add Patient button (kanan)
        FormField searchRow = new FormField(Icons.of(Icons.Type.SEARCH, 20, Theme.PRIMARY), searchField, null);
        searchRow.setPreferredSize(new Dimension(320, 46));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshData(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshData(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshData(); }
        });
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(searchRow);

        PillButton addWalkIn = PillButton.primary("Add Walk-in", Icons.of(Icons.Type.PLUS_CIRCLE, 20, Color.WHITE));
        addWalkIn.setFont(Theme.font(Font.BOLD, 15));
        addWalkIn.setPreferredSize(new Dimension(190, 46));
        addWalkIn.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            if (owner instanceof JFrame) {
                AddWalkInDialog.open((JFrame) owner, clinic, this::refreshData);
            }
        });

        PillButton addAppt = PillButton.primary("Add Appointment", Icons.of(Icons.Type.CALENDAR, 20, Color.WHITE));
        addAppt.setFont(Theme.font(Font.BOLD, 15));
        addAppt.setPreferredSize(new Dimension(220, 46));
        addAppt.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            if (owner instanceof JFrame) {
                AddAppointmentDialog.open((JFrame) owner, clinic, this::refreshData);
            }
        });

        JPanel addWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        addWrap.setOpaque(false);
        addWrap.add(addWalkIn);
        addWrap.add(addAppt);

        JPanel actionRow = new JPanel(new BorderLayout(16, 0));
        actionRow.setOpaque(false);
        actionRow.add(searchWrap, BorderLayout.WEST);
        actionRow.add(addWrap, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        header.add(top, BorderLayout.NORTH);
        header.add(actionRow, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);

        refreshData();
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    public void refreshData() {
        List<Patient> all = clinic.getPatients();
        String query = searchField.getText().trim().toLowerCase();

        rows = new ArrayList<>();
        for (Patient p : all) {
            if (query.isEmpty()
                    || p.getName().toLowerCase().contains(query)
                    || p.getPatientId().toLowerCase().contains(query)) {
                rows.add(p);
            }
        }

        model.setRowCount(0);
        for (Patient p : rows) {
            com.clinic.model.Doctor assigned = clinic.getAssignedDoctor(p.getPatientId());
            model.addRow(new Object[]{
                    p.getName(),
                    p.getAge() == 0 ? "N/A" : String.valueOf(p.getAge()),
                    p.getContactNumber().isEmpty() ? "N/A" : p.getContactNumber(),
                    assigned == null ? "Not assigned" : assigned.getName(),
                    "View"});
        }

        if (query.isEmpty()) {
            countLabel.setText(all.size() + (all.size() == 1 ? " patient" : " patients"));
        } else {
            countLabel.setText(rows.size() + " of " + all.size() + " patients");
        }
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
        JLabel title = new JLabel("All Patients");
        title.setFont(Theme.font(Font.BOLD, 22));
        title.setForeground(Theme.TEXT_DARK);
        countLabel.setFont(Theme.font(Font.PLAIN, 14));
        countLabel.setForeground(Theme.PRIMARY);
        head.add(title, BorderLayout.WEST);
        head.add(countLabel, BorderLayout.EAST);

        UiKit.styleTable(table);
        table.setRowHeight(50);
        table.getColumnModel().getColumn(0).setPreferredWidth(220);
        table.getColumnModel().getColumn(1).setPreferredWidth(60);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(180);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);

        ViewButtonCell viewCell = new ViewButtonCell();
        table.getColumnModel().getColumn(4).setCellRenderer(viewCell);
        table.getColumnModel().getColumn(4).setCellEditor(viewCell);

        JScrollPane scroll = UiKit.roundScroll(table, 14, null, 0f);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(Color.WHITE);

        card.add(head, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------
    // View button -> straight to that patient's record (edit/cancel happen there)
    // ------------------------------------------------------------------

    private void openRecordForRow(int row) {
        if (row < 0 || row >= rows.size()) return;
        onViewPatient.accept(rows.get(row));
    }

    /** Renders and edits the Action column as a small "View" button, left-aligned to match the header. */
    private class ViewButtonCell extends AbstractCellEditor implements javax.swing.table.TableCellRenderer, javax.swing.table.TableCellEditor {
        private int editingRow;

        private JButton makeButton() {
            JButton b = UiKit.tableActionButton("View", Theme.PRIMARY, Color.WHITE, Theme.PRIMARY);
            b.setPreferredSize(new Dimension(70, 32));
            return b;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JPanel holder = UiKit.actionRow(14);
            holder.setOpaque(true);
            holder.setBackground(Color.WHITE);
            holder.add(makeButton());
            return holder;
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) {
            editingRow = r;
            JPanel holder = UiKit.actionRow(14);
            holder.setOpaque(true);
            holder.setBackground(Color.WHITE);
            JButton b = makeButton();
            b.addActionListener(e -> {
                fireEditingStopped();
                openRecordForRow(editingRow);
            });
            holder.add(b);
            return holder;
        }

        @Override public Object getCellEditorValue() { return "View"; }
    }
}