package com.clinic.ui;

import com.clinic.model.Bill;
import com.clinic.service.BillingService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * The "Pay" pop-up in Payments & Billing.
 *
 *   1. Discount / Coverage : None, HMO / Health Card, PWD, or Senior Citizen
 *        HMO     -> provider, member number, cardholder name, approval (LOA) code
 *        PWD/Sr. -> ID number, and 20% comes off the price
 *   2. Payment method      : Card (name, number, expiry, CVV)  or  Cash (amount received)
 *
 * PayBillDialog.open(...) returns true only when the bill was saved as PAID.
 *
 * The full card number and CVV are checked but never saved. Only the last 4
 * digits and the expiry go into bills.csv.
 */
public class PayBillDialog extends JDialog {

    private static final Color DARK = PayKit.DARK;
    private static final int CONTENT_WIDTH = 600;

    private final Bill bill;
    private final String patientName;

    private boolean paid;
    private String discount = Bill.DISCOUNT_NONE;
    private String method;                 // null until Card or Cash is chosen
    private boolean settingCashProgrammatically;
    private boolean cashTouched;

    // discount chips
    private final UiKit.ToggleChip noneChip   = new UiKit.ToggleChip("None");
    private final UiKit.ToggleChip hmoChip    = new UiKit.ToggleChip("HMO / Health Card");
    private final UiKit.ToggleChip pwdChip    = new UiKit.ToggleChip("PWD");
    private final UiKit.ToggleChip seniorChip = new UiKit.ToggleChip("Senior Citizen");

    // method chips
    private final UiKit.ToggleChip cardChip = new UiKit.ToggleChip("Card");
    private final UiKit.ToggleChip cashChip = new UiKit.ToggleChip("Cash");

    // HMO fields
    private final JTextField hmoProviderField = FormField.textField("e.g. Maxicare, Intellicare");
    private final JTextField hmoMemberField   = FormField.textField("Member / card number");
    private final JTextField hmoHolderField   = FormField.textField("Name on the HMO card");
    private final JTextField hmoApprovalField = FormField.textField("Optional");

    // PWD / senior ID
    private final JTextField idField = FormField.textField("ID number");

    // card fields
    private final JTextField cardNameField   = FormField.textField("Name as shown on the card");
    private final JTextField cardNumberField = FormField.textField("1234 5678 9012 3456");
    private final JTextField expiryField     = FormField.textField("MM/YY");
    private final JPasswordField cvvField    = PayKit.passwordField("CVV");

    // cash fields
    private final JTextField cashField = FormField.textField("0.00");
    private final JLabel changeLabel = new JLabel(" ");

    // summary
    private final JLabel totalLabel = new JLabel();
    private final JLabel noteLabel = new JLabel(" ");

    // sections that show/hide
    private final JPanel hmoPanel = PayKit.vbox();
    private final JPanel idPanel = PayKit.vbox();
    private final JLabel idNote = new JLabel(" ");
    private final JPanel cardPanel = PayKit.vbox();
    private final JPanel cashPanel = PayKit.vbox();

    private final JLabel errorLabel = new JLabel(" ");
    private JScrollPane scroll;
    private JComponent header;
    private JComponent footer;
    private PillButton confirmButton;
    private JComponent badField;

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    /**
     * @param patientAge used to pre-select "Senior Citizen" for patients 60 and older
     * @return true if the bill was paid and saved, false if staff cancelled
     */
    public static boolean open(Component parent, Bill bill, String patientName, int patientAge) {
        Window win = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        JFrame frame = win instanceof JFrame ? (JFrame) win : null;

        Component oldGlass = null;
        JComponent dim = null;
        if (frame != null) {
            oldGlass = frame.getGlassPane();
            dim = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    g.setColor(new Color(0, 0, 0, 120));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            dim.addMouseListener(new MouseAdapter() { });
            frame.setGlassPane(dim);
            dim.setVisible(true);
        }

        PayBillDialog dialog = new PayBillDialog(win, bill, patientName, patientAge);
        dialog.setLocationRelativeTo(win);
        dialog.setVisible(true);

        if (frame != null) {
            dim.setVisible(false);
            frame.setGlassPane(oldGlass);
        }
        return dialog.paid;
    }

    private PayBillDialog(Window owner, Bill bill, String patientName, int patientAge) {
        super(owner, "Payment", ModalityType.APPLICATION_MODAL);
        this.bill = bill;
        this.patientName = patientName == null ? "" : patientName;

        setUndecorated(true);

        JPanel surface = new JPanel(new BorderLayout()) {   // white rounded background
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 34, 34);
                g2.dispose();
            }
        };
        surface.setOpaque(false);
        header = buildHeader();
        footer = buildFooter();
        surface.add(header, BorderLayout.NORTH);
        surface.add(buildBody(), BorderLayout.CENTER);
        surface.add(footer, BorderLayout.SOUTH);
        setContentPane(surface);

        installFilters();
        if (patientAge >= 60) discount = Bill.DISCOUNT_SENIOR;   // suggested, staff can change it
        refreshState();

        getRootPane().setDefaultButton(confirmButton);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private JComponent buildHeader() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(20, 24, 6, 24));

        JLabel title = new JLabel("Payment");
        title.setFont(Theme.font(Font.BOLD, 30));
        title.setForeground(DARK);
        row.add(new JLabel(Icons.of(Icons.Type.WALLET, 40, DARK)));
        row.add(title);

        JLabel subtitle = new JLabel("Invoice " + bill.getInvoiceId()
                + (patientName.isEmpty() ? "" : "   |   Patient: " + patientName));
        subtitle.setFont(Theme.font(Font.PLAIN, 14));
        subtitle.setForeground(Theme.TEXT_MUTED);
        subtitle.setBorder(new EmptyBorder(0, 30, 14, 24));

        JPanel divider = new JPanel();
        divider.setBackground(new Color(0xD9D9D9));
        divider.setPreferredSize(new Dimension(1, 2));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(row, BorderLayout.NORTH);
        top.add(subtitle, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(top, BorderLayout.CENTER);
        wrap.add(divider, BorderLayout.SOUTH);
        return wrap;
    }

    private JComponent buildBody() {
        JPanel content = new JPanel() {                      // fixed width, grows downward
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(CONTENT_WIDTH, super.getPreferredSize().height);
            }
        };
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(16, 30, 6, 30));

        // summary card: service + total
        content.add(buildSummary());
        content.add(Box.createVerticalStrut(18));

        // 1. Discount / coverage
        content.add(PayKit.sectionLabel("Discount / Coverage", false));
        content.add(Box.createVerticalStrut(8));
        PayKit.sizeChip(noneChip, 84);
        PayKit.sizeChip(hmoChip, 176);
        PayKit.sizeChip(pwdChip, 84);
        PayKit.sizeChip(seniorChip, 148);
        content.add(PayKit.chipRow(noneChip, hmoChip, pwdChip, seniorChip));
        content.add(Box.createVerticalStrut(14));

        noneChip.addActionListener(e -> { discount = Bill.DISCOUNT_NONE; refreshState(); });
        hmoChip.addActionListener(e -> { discount = Bill.DISCOUNT_HMO; refreshState(); });
        pwdChip.addActionListener(e -> { discount = Bill.DISCOUNT_PWD; refreshState(); });
        seniorChip.addActionListener(e -> { discount = Bill.DISCOUNT_SENIOR; refreshState(); });

        hmoPanel.add(twoCols(PayKit.cell("HMO Provider", hmoProviderField, true),
                PayKit.cell("Member / Card Number", hmoMemberField, true)));
        hmoPanel.add(Box.createVerticalStrut(12));
        hmoPanel.add(twoCols(PayKit.cell("Cardholder Name", hmoHolderField, true),
                PayKit.cell("Approval / LOA Code (optional)", hmoApprovalField, false)));
        hmoPanel.add(Box.createVerticalStrut(14));
        content.add(hmoPanel);

        idNote.setFont(Theme.font(Font.PLAIN, 13));
        idNote.setForeground(Theme.TEXT_MUTED);
        idNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        idPanel.add(PayKit.cell("ID Number", idField, true));
        idPanel.add(Box.createVerticalStrut(4));
        idPanel.add(idNote);
        idPanel.add(Box.createVerticalStrut(14));
        content.add(idPanel);

        // 2. Payment method
        content.add(PayKit.sectionLabel("Payment Method", true));
        content.add(Box.createVerticalStrut(8));
        PayKit.sizeChip(cardChip, 120);
        PayKit.sizeChip(cashChip, 120);
        content.add(PayKit.chipRow(cardChip, cashChip));
        content.add(Box.createVerticalStrut(14));

        cardChip.addActionListener(e -> { method = Bill.METHOD_CARD; refreshState(); });
        cashChip.addActionListener(e -> { method = Bill.METHOD_CASH; refreshState(); });

        JPanel expiryCvv = new JPanel(new GridLayout(1, 2, 16, 0));
        expiryCvv.setOpaque(false);
        expiryCvv.add(PayKit.cell("Expiry Date", expiryField, true));
        expiryCvv.add(PayKit.cell("CVV", cvvField, true));
        cardPanel.add(PayKit.cell("Cardholder Name", cardNameField, true));
        cardPanel.add(Box.createVerticalStrut(12));
        cardPanel.add(PayKit.cell("Card Number", cardNumberField, true));
        cardPanel.add(Box.createVerticalStrut(12));
        cardPanel.add(PayKit.fixHeight(expiryCvv));
        cardPanel.add(Box.createVerticalStrut(14));
        content.add(cardPanel);

        changeLabel.setFont(Theme.font(Font.BOLD, 15));
        changeLabel.setForeground(Theme.DONE_FG);
        changeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cashPanel.add(PayKit.cell("Amount Received (PHP)", cashField, true));
        cashPanel.add(Box.createVerticalStrut(4));
        cashPanel.add(changeLabel);
        cashPanel.add(Box.createVerticalStrut(14));
        content.add(cashPanel);

        scroll = new JScrollPane(content,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        UiKit.styleScrollBar(scroll.getVerticalScrollBar());
        return scroll;
    }

    private JComponent buildSummary() {
        UiKit.Card card = new UiKit.Card(18, new Color(0xE9F0F4), null);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel service = new JLabel(bill.getServiceLabel());
        service.setFont(Theme.font(Font.BOLD, 20));
        service.setForeground(DARK);
        JLabel fee = new JLabel("Fee " + Bill.money(bill.getAmount()));
        fee.setFont(Theme.font(Font.PLAIN, 14));
        fee.setForeground(Theme.TEXT_MUTED);
        noteLabel.setFont(Theme.font(Font.PLAIN, 13));
        noteLabel.setForeground(Theme.TEXT_MUTED);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(service);
        left.add(Box.createVerticalStrut(2));
        left.add(fee);

        JLabel dueCaption = new JLabel("Amount Due", SwingConstants.RIGHT);
        dueCaption.setFont(Theme.font(Font.PLAIN, 13));
        dueCaption.setForeground(Theme.TEXT_MUTED);
        totalLabel.setFont(Theme.font(Font.BOLD, 30));
        totalLabel.setForeground(DARK);
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(dueCaption, BorderLayout.NORTH);
        right.add(totalLabel, BorderLayout.CENTER);

        card.add(left, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        noteLabel.setBorder(new EmptyBorder(6, 0, 0, 0));
        card.add(noteLabel, BorderLayout.SOUTH);   // discount / HMO note gets the full width
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return PayKit.fixHeight(card);
    }

    private static JComponent twoCols(JComponent a, JComponent b) {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.add(a);
        row.add(b);
        return PayKit.fixHeight(row);
    }

    private JComponent buildFooter() {
        JPanel f = new JPanel(new BorderLayout(0, 8));
        f.setOpaque(false);
        f.setBorder(new EmptyBorder(6, 30, 24, 30));

        errorLabel.setFont(Theme.font(Font.PLAIN, 14));
        errorLabel.setForeground(Theme.ERROR);
        f.add(errorLabel, BorderLayout.NORTH);

        confirmButton = PillButton.primary("Confirm Payment", null);
        PillButton cancelButton = PillButton.secondary("Cancel");
        confirmButton.addActionListener(e -> submit());
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 24, 0));
        buttons.setOpaque(false);
        buttons.add(confirmButton);
        buttons.add(cancelButton);
        f.add(buttons, BorderLayout.CENTER);
        return f;
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    private boolean percentOff() {
        return Bill.DISCOUNT_PWD.equals(discount) || Bill.DISCOUNT_SENIOR.equals(discount);
    }

    private BigDecimal totalDue() {
        return percentOff() ? bill.getAmount().subtract(Bill.discountFor(bill.getAmount())) : bill.getAmount();
    }

    /** Shows/hides sections for the current choices, updates the total, then resizes to fit. */
    private void refreshState() {
        noneChip.setActive(Bill.DISCOUNT_NONE.equals(discount));
        hmoChip.setActive(Bill.DISCOUNT_HMO.equals(discount));
        pwdChip.setActive(Bill.DISCOUNT_PWD.equals(discount));
        seniorChip.setActive(Bill.DISCOUNT_SENIOR.equals(discount));
        cardChip.setActive(Bill.METHOD_CARD.equals(method));
        cashChip.setActive(Bill.METHOD_CASH.equals(method));

        hmoPanel.setVisible(Bill.DISCOUNT_HMO.equals(discount));
        idPanel.setVisible(percentOff());
        cardPanel.setVisible(Bill.METHOD_CARD.equals(method));
        cashPanel.setVisible(Bill.METHOD_CASH.equals(method));

        BigDecimal total = totalDue();
        totalLabel.setText(Bill.money(total));
        switch (discount) {
            case Bill.DISCOUNT_PWD:
            case Bill.DISCOUNT_SENIOR:
                noteLabel.setText("20% " + Bill.discountLabel(discount) + " discount: -"
                        + Bill.money(Bill.discountFor(bill.getAmount())));
                idNote.setText("The " + Bill.discountLabel(discount) + " ID number is required for the 20% discount.");
                break;
            case Bill.DISCOUNT_HMO:
                noteLabel.setText("HMO / health card details will be saved with this bill.");
                break;
            default:
                noteLabel.setText(" ");
        }

        // keep "Amount Received" in step with the total until staff type their own amount
        if (!cashTouched) setCash(total.toPlainString());
        updateChange();

        errorLabel.setText(" ");
        resizeToFit();
    }

    private void setCash(String text) {
        settingCashProgrammatically = true;
        cashField.setText(text);
        settingCashProgrammatically = false;
    }

    private void updateChange() {
        try {
            BigDecimal received = new BigDecimal(cashField.getText().trim());
            BigDecimal change = received.subtract(totalDue());
            changeLabel.setText(change.signum() > 0 ? "Change: " + Bill.money(change) : " ");
        } catch (NumberFormatException e) {
            changeLabel.setText(" ");
        }
    }

    private void resizeToFit() {
        Component view = scroll.getViewport().getView();
        view.invalidate();
        Dimension want = view.getPreferredSize();

        int usable;
        try {
            usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height - 30;
        } catch (HeadlessException e) {
            usable = 700;
        }
        int maxBody = usable - header.getPreferredSize().height - footer.getPreferredSize().height;
        int bodyH = Math.max(160, Math.min(want.height, maxBody));
        int bar = scroll.getVerticalScrollBar().getPreferredSize().width;
        scroll.setPreferredSize(new Dimension(want.width + bar, bodyH));

        pack();
        setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 34, 34));
        if (isVisible()) setLocationRelativeTo(getOwner());
    }

    // ------------------------------------------------------------------
    // Submit
    // ------------------------------------------------------------------

    private void submit() {
        badField = null;
        String error = checkInputs();
        if (error != null) {
            errorLabel.setText(error);
            if (badField != null) {
                badField.requestFocusInWindow();
                badField.scrollRectToVisible(new Rectangle(0, -40, 1, badField.getHeight() + 80));
            }
            return;
        }
        try {
            // discount / coverage
            bill.setDiscount(discount, percentOff() ? idField.getText().trim() : "");
            if (Bill.DISCOUNT_HMO.equals(discount)) {
                bill.setHmo(hmoProviderField.getText().trim(), hmoMemberField.getText().trim(),
                        hmoHolderField.getText().trim(), hmoApprovalField.getText().trim());
            } else {
                bill.setHmo("", "", "", "");
            }

            // payment
            BillingService billing = BillingService.getInstance();
            if (Bill.METHOD_CARD.equals(method)) {
                String digits = cardNumberField.getText().replaceAll("\\D", "");
                billing.recordCardPayment(bill, cardNameField.getText().trim(),
                        digits.substring(digits.length() - 4), expiryField.getText().trim());
            } else {
                billing.recordCashPayment(bill, new BigDecimal(cashField.getText().trim()));
            }
            paid = true;
            dispose();
        } catch (Exception ex) {
            errorLabel.setText("Couldn't save the payment: " + ex.getMessage());
        }
    }

    private String fail(JComponent field, String message) {
        badField = field;
        return message;
    }

    /** @return an error message, or null when everything is filled in correctly */
    private String checkInputs() {
        String err = checkDiscount();
        if (err != null) return err;

        if (method == null) return fail(cardChip, "Please choose Card or Cash.");

        return Bill.METHOD_CARD.equals(method) ? checkCard() : checkCash();
    }

    private String checkDiscount() {
        if (Bill.DISCOUNT_HMO.equals(discount)) {
            String provider = hmoProviderField.getText().trim();
            if (provider.isEmpty()) return fail(hmoProviderField, "Enter the HMO provider.");
            if (PayKit.letterCount(provider) < 2)
                return fail(hmoProviderField, "HMO provider must be a name, like Maxicare or Intellicare.");

            String member = hmoMemberField.getText().trim();
            if (member.isEmpty()) return fail(hmoMemberField, "Enter the HMO member / card number.");
            if (member.length() < 6) return fail(hmoMemberField, "HMO member number is too short (at least 6 characters).");
            if (!PayKit.hasDigit(member)) return fail(hmoMemberField, "HMO member number must contain digits.");
            if (PayKit.allSame(member.replace("-", "")))
                return fail(hmoMemberField, "That HMO member number isn't valid. Please check it.");

            if (!PayKit.isFullName(hmoHolderField.getText().trim()))
                return fail(hmoHolderField, "Enter the HMO cardholder's first and last name.");

            String loa = hmoApprovalField.getText().trim();
            if (!loa.isEmpty()) {
                if (loa.length() < 4) return fail(hmoApprovalField, "Approval / LOA code is too short (at least 4 characters).");
                if (!PayKit.hasDigit(loa)) return fail(hmoApprovalField, "Approval / LOA code must contain digits.");
            }
        }
        if (percentOff()) {
            String id = idField.getText().trim();
            String which = Bill.discountLabel(discount);
            if (id.isEmpty()) return fail(idField, "Enter the " + which + " ID number to give the discount.");
            if (id.length() < 4) return fail(idField, "The " + which + " ID number is too short (at least 4 characters).");
            if (!PayKit.hasDigit(id)) return fail(idField, "The " + which + " ID number must contain digits.");
            if (PayKit.allSame(id.replace("-", "")))
                return fail(idField, "That " + which + " ID number isn't valid. Please check it.");
        }
        return null;
    }

    private String checkCash() {
        String text = cashField.getText().trim();
        if (text.isEmpty()) return fail(cashField, "Enter the amount received.");
        BigDecimal received;
        try {
            received = new BigDecimal(text);
        } catch (NumberFormatException e) {
            return fail(cashField, "Amount received isn't a valid number.");
        }
        if (received.compareTo(totalDue()) < 0)
            return fail(cashField, "Amount received is less than the amount due (" + Bill.money(totalDue()) + ").");
        return null;
    }

    private String checkCard() {
        String name = cardNameField.getText().trim();
        if (!PayKit.isFullName(name)) return fail(cardNameField, "Enter the cardholder's first and last name.");

        String digits = cardNumberField.getText().replaceAll("\\D", "");
        if (digits.isEmpty()) return fail(cardNumberField, "Enter the card number.");
        boolean amex = digits.startsWith("34") || digits.startsWith("37");
        if (amex && digits.length() != 15)
            return fail(cardNumberField, "American Express card numbers have 15 digits.");
        if (digits.startsWith("4") && digits.length() != 13 && digits.length() != 16 && digits.length() != 19)
            return fail(cardNumberField, "Visa card numbers have 16 digits.");
        if (PayKit.isMastercardPrefix(digits) && digits.length() != 16)
            return fail(cardNumberField, "Mastercard numbers have 16 digits.");
        if (digits.length() < 13 || digits.length() > 19)
            return fail(cardNumberField, "Card number must be 13 to 19 digits.");
        if (PayKit.allSame(digits) || !PayKit.passesLuhn(digits))
            return fail(cardNumberField, "That card number isn't valid. Please check for typos.");

        String exp = expiryField.getText().trim();
        if (!exp.matches("(0[1-9]|1[0-2])/\\d{2}"))
            return fail(expiryField, "Enter the expiry date as MM/YY (for example 08/28).");
        YearMonth ym = YearMonth.of(2000 + Integer.parseInt(exp.substring(3)), Integer.parseInt(exp.substring(0, 2)));
        if (ym.isBefore(YearMonth.now())) return fail(expiryField, "This card has expired.");
        if (ym.isAfter(YearMonth.now().plusYears(10)))
            return fail(expiryField, "That expiry date is too far ahead. Please check it.");

        int cvvLen = cvvField.getPassword().length;
        int need = amex ? 4 : 3;
        if (cvvLen != need)
            return fail(cvvField, "CVV must be " + need + " digits" + (amex ? " for American Express." : "."));
        return null;
    }

    // ------------------------------------------------------------------
    // Typing limits
    // ------------------------------------------------------------------

    private void installFilters() {
        PayKit.filter(hmoProviderField, s -> PayKit.allowed(s, 40, c -> Character.isLetterOrDigit(c) || " &.-".indexOf(c) >= 0));
        PayKit.filter(hmoMemberField, s -> PayKit.allowed(s, 25, c -> Character.isLetterOrDigit(c) || c == '-'));
        PayKit.filter(hmoHolderField, s -> PayKit.allowed(s, 50, PayKit::nameChar));
        PayKit.filter(hmoApprovalField, s -> PayKit.allowed(s, 20, c -> Character.isLetterOrDigit(c) || c == '-'));
        PayKit.filter(idField, s -> PayKit.allowed(s, 25, c -> Character.isLetterOrDigit(c) || c == '-'));

        PayKit.filter(cardNameField, s -> PayKit.allowed(s, 50, PayKit::nameChar));
        PayKit.filter(cardNumberField, s -> {                // 1234 5678 9012 3456
            String d = s.replaceAll("\\D", "");
            if (!s.replace(" ", "").equals(d) || d.length() > 19) return null;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < d.length(); i++) {
                if (i > 0 && i % 4 == 0) sb.append(' ');
                sb.append(d.charAt(i));
            }
            return sb.toString();
        });
        PayKit.filter(expiryField, s -> {                    // MM/YY
            String d = s.replace("/", "");
            if (!d.matches("\\d{0,4}")) return null;
            return d.length() >= 3 ? d.substring(0, 2) + "/" + d.substring(2) : d;
        });
        PayKit.filter(cvvField, s -> s.matches("\\d{0,4}") ? s : null);
        PayKit.filter(cashField, s -> s.matches("\\d{0,7}(\\.\\d{0,2})?") ? s : null);

        // clear the red message as soon as staff start fixing something
        javax.swing.event.DocumentListener clearError = new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { errorLabel.setText(" "); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { errorLabel.setText(" "); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { errorLabel.setText(" "); }
        };
        for (javax.swing.text.JTextComponent f : new javax.swing.text.JTextComponent[]{
                hmoProviderField, hmoMemberField, hmoHolderField, hmoApprovalField, idField,
                cardNameField, cardNumberField, expiryField, cvvField, cashField}) {
            f.getDocument().addDocumentListener(clearError);
        }

        // typing in "Amount Received" stops it from following the total, and shows the change
        cashField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void changed() {
                if (!settingCashProgrammatically) cashTouched = true;
                updateChange();
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { changed(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { changed(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { changed(); }
        });
    }
}