package com.clinic.ui;

import com.clinic.model.Bill;
import com.clinic.service.ServiceCatalog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Settings > Service Settings. Staff type a service name and its price to add it to the drop-down
 * used in Add Walk-in / Add Appointment. Services are saved in services.csv.
 */
public class ServiceSettingsPanel extends JPanel {

    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Service", "Price"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField nameField = FormField.textField("Service name, e.g. X-Ray");
    private final JTextField priceField = FormField.textField("Price in PHP, e.g. 850");
    private final JLabel message = new JLabel(" ");
    private List<ServiceCatalog.Item> shown;

    public ServiceSettingsPanel(Runnable onBack) {
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        JButton back = new JButton("Back to Settings", Icons.of(Icons.Type.ARROW_LEFT, 16, Theme.PRIMARY));
        back.setFont(Theme.font(Font.BOLD, 14));
        back.setForeground(Theme.PRIMARY);
        back.setIconTextGap(8);
        back.setContentAreaFilled(false);
        back.setBorderPainted(false);
        back.setFocusPainted(false);
        back.setCursor(Theme.hand());
        back.setHorizontalAlignment(SwingConstants.LEFT);
        back.addActionListener(e -> onBack.run());
        add(back, BorderLayout.NORTH);

        UiKit.Card card = new UiKit.Card(20, Color.WHITE, Theme.CARD_STROKE);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(22, 26, 20, 26));

        JLabel title = new JLabel("Service Settings");
        title.setFont(Theme.font(Font.BOLD, 25));
        title.setForeground(Theme.PRIMARY);
        JLabel note = new JLabel("Services added here appear in the Service drop-down when adding a patient.");
        note.setFont(Theme.font(Font.PLAIN, 13));
        note.setForeground(Theme.TEXT_MUTED);

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(title);
        head.add(Box.createVerticalStrut(2));
        head.add(note);
        head.add(Box.createVerticalStrut(14));
        head.add(addRow());
        head.add(Box.createVerticalStrut(6));
        message.setFont(Theme.font(Font.PLAIN, 14));
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(message);

        UiKit.styleTable(table);
        table.setRowHeight(44);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                l.setBorder(new EmptyBorder(0, 14, 0, 8));
                l.setOpaque(true);
                if (sel) {
                    l.setBackground(Theme.SIDEBAR_ACTIVE);   // same blue as the dropdown highlight
                    l.setForeground(Color.WHITE);
                    l.setFont(Theme.font(Font.BOLD, 14));
                } else {
                    l.setBackground(Color.WHITE);
                    l.setForeground(Theme.TEXT_DARK);
                    l.setFont(Theme.font(Font.PLAIN, 14));
                }
                return l;
            }
        });
        table.getColumnModel().getColumn(0).setPreferredWidth(360);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Color.WHITE);

        PillButton remove = PillButton.secondary("Remove Selected");
        remove.setFont(Theme.font(Font.BOLD, 14));
        remove.setPreferredSize(new Dimension(190, 42));
        remove.addActionListener(e -> removeSelected());
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        foot.setOpaque(false);
        foot.add(remove);

        card.add(head, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(foot, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);

        refresh();
    }

    private JComponent addRow() {
        FormField nameBox = new FormField(null, nameField, null);
        FormField priceBox = new FormField(null, priceField, null);
        priceBox.setPreferredSize(new Dimension(220, 46));

        PillButton add = PillButton.primary("Add Service", Icons.of(Icons.Type.PLUS_CIRCLE, 20, Color.WHITE));
        add.setFont(Theme.font(Font.BOLD, 15));
        add.setPreferredSize(new Dimension(190, 46));
        add.addActionListener(e -> addService());
        priceField.addActionListener(e -> addService());

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(priceBox);
        right.add(add);
        row.add(nameBox, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    public void refresh() {
        shown = ServiceCatalog.getInstance().getActive();
        model.setRowCount(0);
        for (ServiceCatalog.Item i : shown) model.addRow(new Object[]{i.getName(), Bill.money(i.getPrice())});
    }

    private void addService() {
        try {
            ServiceCatalog.Item item = ServiceCatalog.getInstance().add(nameField.getText(), priceField.getText());
            nameField.setText("");
            priceField.setText("");
            refresh();
            message.setForeground(Theme.DONE_FG);
            message.setText("Added \"" + item.getName() + "\" (" + Bill.money(item.getPrice()) + ").");
            nameField.requestFocusInWindow();
        } catch (Exception ex) {
            message.setForeground(Theme.ERROR);
            message.setText(ex.getMessage());
        }
    }

    private void removeSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= shown.size()) {
            message.setForeground(Theme.ERROR);
            message.setText("Select a service in the table first.");
            return;
        }
        ServiceCatalog.Item item = shown.get(row);
        int choice = JOptionPane.showConfirmDialog(this,
                "Remove \"" + item.getName() + "\" from the drop-down?\nBills already made with it are not affected.",
                "Remove service", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        ServiceCatalog.getInstance().remove(item.getId());
        refresh();
        message.setForeground(Theme.DONE_FG);
        message.setText("Removed \"" + item.getName() + "\".");
    }
}
