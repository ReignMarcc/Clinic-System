package com.clinic.ui;

import com.clinic.model.Appointment;
import com.clinic.model.Bill;
import com.clinic.model.Patient;
import com.clinic.service.BillingService;
import com.clinic.service.ClinicManager;
import com.clinic.service.ServiceCatalog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The "Service" drop-down in the Add Walk-in and Add Appointment pop-ups. It lists every service
 * (name and price) saved in Settings > Service Settings, plus "Others". Picking Others shows a
 * name box and a price box; that service and price are kept on the bill only.
 */
public class ServiceFields extends JPanel {

    private final JComboBox<ServiceCatalog.Item> combo = new JComboBox<>();
    private final JTextField otherName = FormField.textField("Type the service");
    private final JTextField otherPrice = FormField.textField("Price, e.g. 850");
    private final FormField otherNameBox = new FormField(null, otherName, null);
    private final FormField otherPriceBox = new FormField(null, otherPrice, null);

    public ServiceFields() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 8));

        JLabel title = new JLabel("<html>Service<font color='#E53935'>*</font></html>");
        title.setFont(Theme.font(Font.BOLD, 20));
        title.setForeground(PayKit.DARK);

        for (ServiceCatalog.Item item : ServiceCatalog.getInstance().getActive()) combo.addItem(item);
        combo.addItem(ServiceCatalog.OTHERS);
        style(combo);
        combo.setSelectedIndex(-1);

        IconButton chevron = new IconButton(Icons.of(Icons.Type.CHEVRON_DOWN, 18, PayKit.DARK),
                "Show options", new Color(0, 0, 0, 20));
        chevron.addActionListener(e -> combo.setPopupVisible(!combo.isPopupVisible()));

        FormField comboBox = new FormField(null, combo, chevron);
        otherNameBox.setPreferredSize(new Dimension(230, 46));
        otherPriceBox.setPreferredSize(new Dimension(140, 46));
        otherNameBox.setVisible(false);
        otherPriceBox.setVisible(false);

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.weightx = 1;
        row.add(comboBox, gc);
        gc.gridx = 1; gc.weightx = 0; gc.insets = new Insets(0, 10, 0, 0);
        row.add(otherNameBox, gc);
        gc.gridx = 2;
        row.add(otherPriceBox, gc);

        combo.addActionListener(e -> {
            boolean others = combo.getSelectedItem() == ServiceCatalog.OTHERS;
            otherNameBox.setVisible(others);
            otherPriceBox.setVisible(others);
            row.revalidate();
            row.repaint();
            if (others) SwingUtilities.invokeLater(otherName::requestFocusInWindow);
        });

        add(title, BorderLayout.NORTH);
        add(row, BorderLayout.CENTER);
    }

    private static String shown(ServiceCatalog.Item item) {
        return item == ServiceCatalog.OTHERS
                ? "Others"
                : item.getName() + "  -  " + Bill.moneyShort(item.getPrice());
    }

    private static void style(JComboBox<ServiceCatalog.Item> c) {
        c.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton none = new JButton();
                none.setPreferredSize(new Dimension(0, 0));
                none.setBorder(null);
                none.setOpaque(false);
                none.setContentAreaFilled(false);
                none.setFocusable(false);
                return none;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                // transparent: the FormField draws the background
            }

            @Override
            public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
                // Draw the shown text ourselves so the chosen service never renders blank.
                ServiceCatalog.Item item = (ServiceCatalog.Item) comboBox.getSelectedItem();
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setFont(comboBox.getFont());
                g2.setColor(item == null ? Theme.TEXT_MUTED : Theme.TEXT_DARK);
                FontMetrics fm = g2.getFontMetrics();
                String text = item == null ? "Select a service" : shown(item);
                g2.drawString(text, bounds.x, bounds.y + (bounds.height - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }

            @Override
            protected javax.swing.plaf.basic.ComboPopup createPopup() {
                return new javax.swing.plaf.basic.BasicComboPopup(comboBox) {
                    @Override
                    protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
                        int wanted = list.getPreferredSize().width + 24;
                        return super.computePopupBounds(px, py, Math.max(pw, wanted), ph);
                    }
                };
            }
        });
        c.setOpaque(false);
        c.setBorder(null);
        c.setFont(Theme.font(Font.PLAIN, 15));

        Runnable repaint = () -> {
            Runnable force = () -> {
                c.invalidate();
                c.revalidate();
                Window w = SwingUtilities.getWindowAncestor(c);
                if (w != null) w.repaint(); else c.repaint();
            };
            force.run();
            SwingUtilities.invokeLater(force);
        };
        c.addItemListener(e -> repaint.run());
        c.addActionListener(e -> repaint.run());
        c.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) { }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { repaint.run(); }
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { repaint.run(); }
        });

        c.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                boolean inPopup = index >= 0;
                if (value == null) {
                    l.setText("Select a service");
                    l.setForeground(Theme.TEXT_MUTED);
                } else {
                    ServiceCatalog.Item item = (ServiceCatalog.Item) value;
                    l.setText(shown(item));
                    l.setForeground(inPopup && isSelected ? Color.WHITE : Theme.TEXT_DARK);
                }
                l.setOpaque(inPopup);
                if (inPopup) l.setBackground(isSelected ? Theme.SIDEBAR_ACTIVE : Color.WHITE);
                l.setBorder(new EmptyBorder(6, inPopup ? 10 : 0, 6, 8));
                return l;
            }
        });
    }

    private boolean isOthers() {
        return combo.getSelectedItem() == ServiceCatalog.OTHERS;
    }

    private String cleanName() {
        return otherName.getText().trim().replaceAll("\\s+", " ");
    }

    /** The typed price, or null if it isn't a number. */
    private BigDecimal parsePrice() {
        try {
            return new BigDecimal(otherPrice.getText().replace(",", "").replace("\u20B1", "")
                    .replace("PHP", "").trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The chosen service's id (or OTHER:name for a custom one), or null if nothing was chosen. */
    public String getService() {
        ServiceCatalog.Item item = (ServiceCatalog.Item) combo.getSelectedItem();
        if (item == null) return null;
        if (item == ServiceCatalog.OTHERS) {
            String n = cleanName();
            return n.isEmpty() ? null : Bill.CUSTOM_PREFIX + n;
        }
        return item.getId();
    }

    /** The component to focus when something needs fixing. */
    public JComponent getFocusTarget() {
        if (isOthers()) return cleanName().isEmpty() ? otherName : otherPrice;
        return combo;
    }

    /** @return an error message, or null when the service is ready */
    public String check() {
        if (combo.getSelectedItem() == null) return "Please select a service.";
        if (!isOthers()) return null;

        String n = cleanName();
        if (n.isEmpty()) return "Please type the service name.";
        if (n.length() > 40) return "Service name can be at most 40 characters.";
        if (n.contains(",") || n.contains("\"")) return "Service name can't contain commas or quotation marks.";

        BigDecimal price = parsePrice();
        if (price == null) return "Please enter the price as a number, e.g. 850 or 850.50.";
        if (price.signum() <= 0) return "The price must be more than zero.";
        if (price.compareTo(new BigDecimal("1000000")) > 0) return "The price is too large.";
        return null;
    }

    /** Creates the pending bill for the appointment that was just booked for this patient. */
    public Bill createBill(ClinicManager clinic, Patient patient) {
        Appointment latest = null;
        for (Appointment a : clinic.getAppointmentsForPatient(patient.getPatientId())) latest = a;
        String service = getService();
        if (latest == null || service == null) return null;
        if (Bill.isCustom(service)) {
            return BillingService.getInstance().createBill(
                    latest.getAppointmentId(), patient.getPatientId(), service, parsePrice());
        }
        return BillingService.getInstance().createBill(latest.getAppointmentId(), patient.getPatientId(), service);
    }
}