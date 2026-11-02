package com.clinic.ui;

import com.clinic.util.InputValidator;
import com.clinic.util.AdminCredentials;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Admin login window.
 *
 * Usage (in Main):
 *   ClinicManager clinic = new ClinicManager();
 *   new LoginFrame(() -> new Mainframe(clinic).setVisible(true)).setVisible(true);
 *
 * Optional: put the hospital photo at  assets/login_bg.jpg  (project root).
 * If it is missing, a soft blue gradient is used instead.
 */
public class LoginFrame extends JFrame {

    private final Runnable onLoginSuccess;
    private final PlaceholderTextField usernameField = new PlaceholderTextField("Admin Email");
    private final PlaceholderPasswordField passwordField = new PlaceholderPasswordField("Password");
    private final char hiddenEchoChar = passwordField.getEchoChar();
    private final JLabel errorLabel = new JLabel(" ");

    public LoginFrame(Runnable onLoginSuccess) {
        super("Clinic System - Admin Login");
        this.onLoginSuccess = onLoginSuccess;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 800);
        setMinimumSize(new Dimension(520, 720));
        setLocationRelativeTo(null);
        setContentPane(buildContent());
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private JComponent buildContent() {
        BackgroundPanel root = new BackgroundPanel();
        root.setLayout(new BorderLayout());
        root.add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(buildCard());
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(24, 30, 24, 30));

        JLabel title = new JLabel("Admin Login");
        title.setFont(Theme.font(Font.BOLD, 30));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        return header;
    }

    private JComponent buildCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        IconCircle lockCircle = new IconCircle();

        JLabel title = new JLabel("Admin Login");
        title.setFont(Theme.font(Font.BOLD, 34));
        title.setForeground(Theme.PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Restricted Access");
        subtitle.setFont(Theme.font(Font.PLAIN, 18));
        subtitle.setForeground(Theme.PRIMARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Email field
        FieldPanel userRow = new FieldPanel(
                Icons.of(Icons.Type.USER, 20, Theme.PRIMARY), usernameField, null, Theme.FIELD_BG);

        // Password field with show/hide toggle
        JButton eyeButton = buildEyeButton();
        FieldPanel passRow = new FieldPanel(
                Icons.of(Icons.Type.LOCK, 20, Theme.PRIMARY), passwordField, eyeButton, Color.WHITE);

        JButton loginButton = buildLoginButton();

        errorLabel.setFont(Theme.font(Font.PLAIN, 14));
        errorLabel.setForeground(Theme.ERROR);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel footer = new JLabel(
                "<html><div style='text-align:center;width:250px'>"
                        + "Only authorized personnel can access this system</div></html>");
        footer.setFont(Theme.font(Font.PLAIN, 14));
        footer.setForeground(Theme.PRIMARY);
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(lockCircle);
        card.add(Box.createVerticalStrut(10));
        card.add(title);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(30));
        card.add(userRow);
        card.add(Box.createVerticalStrut(12));
        card.add(passRow);
        card.add(Box.createVerticalStrut(26));
        card.add(loginButton);
        JPanel footerWrap = new JPanel(new GridBagLayout());
        footerWrap.setOpaque(false);
        footerWrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        footerWrap.add(footer); // add first, then measure, so the height isn't locked to an empty panel
        footerWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, footerWrap.getPreferredSize().height));

        card.add(Box.createVerticalStrut(8));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(14));
        card.add(footerWrap);

        // Pressing Enter anywhere triggers login
        getRootPane().setDefaultButton(loginButton);
        return card;
    }

    private JButton buildEyeButton() {
        JButton eye = new JButton(Icons.of(Icons.Type.EYE_OFF, 20, Theme.PRIMARY));
        eye.setBorderPainted(false);
        eye.setContentAreaFilled(false);
        eye.setFocusPainted(false);
        eye.setFocusable(false);
        eye.setCursor(Theme.hand());
        eye.setToolTipText("Show / hide password");
        eye.addActionListener(e -> {
            boolean currentlyHidden = passwordField.getEchoChar() != 0;
            passwordField.setEchoChar(currentlyHidden ? (char) 0 : hiddenEchoChar);
            eye.setIcon(Icons.of(currentlyHidden ? Icons.Type.EYE : Icons.Type.EYE_OFF, 20, Theme.PRIMARY));
        });
        return eye;
    }

    private JButton buildLoginButton() {
        JButton button = new JButton("Login") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                ButtonModel m = getModel();
                g2.setColor(m.isPressed() ? Theme.PRIMARY_PRESSED
                        : m.isRollover() ? Theme.PRIMARY_HOVER : Theme.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g); // draws the text
            }
        };
        button.setFont(Theme.font(Font.BOLD, 20));
        button.setForeground(Color.WHITE);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(Theme.hand());
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(100, 58));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        button.addActionListener(e -> attemptLogin());
        return button;
    }

    // ------------------------------------------------------------------
    // Login logic
    // ------------------------------------------------------------------

    private void attemptLogin() {
        errorLabel.setText(" ");
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());

        if (!InputValidator.isNotBlank(user) || pass.isEmpty()) {
            errorLabel.setText("Please enter your email and password.");
            return;
        }

        if (AdminCredentials.authenticate(user, passwordField.getPassword())) {
            dispose();
            onLoginSuccess.run();
        } else {
            errorLabel.setText("Invalid email or password.");
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }

    // ------------------------------------------------------------------
    // Custom components
    // ------------------------------------------------------------------

    /** Full-window background: hospital photo (cover-fit) or a gradient fallback. */
    private static class BackgroundPanel extends JPanel {
        private final BufferedImage image = loadImage();

        private static BufferedImage loadImage() {
            for (String path : new String[]{"assets/login_bg.jpg", "assets/login_bg.png"}) {
                File f = new File(path);
                if (f.exists()) {
                    try {
                        return ImageIO.read(f);
                    } catch (Exception ignored) { /* fall back to gradient */ }
                }
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();
            if (image != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                double scale = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
                int dw = (int) Math.ceil(image.getWidth() * scale);
                int dh = (int) Math.ceil(image.getHeight() * scale);
                g2.drawImage(image, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
            } else {
                g2.setPaint(new GradientPaint(0, 0, new Color(0xCFE0EA), 0, h, new Color(0xF4F8FB)));
                g2.fillRect(0, 0, w, h);
            }
            g2.dispose();
        }
    }

    /** White rounded card with a soft drop shadow. */
    private static class CardPanel extends JPanel {
        private static final int SHADOW = 10;
        private static final int ARC = 24;

        CardPanel() {
            setOpaque(false);
            setBorder(new EmptyBorder(SHADOW + 32, SHADOW + 40, SHADOW + 28, SHADOW + 40));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.width = 456 + 2 * SHADOW;
            return d;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            for (int i = SHADOW; i > 0; i--) {
                int inset = SHADOW - i;
                g2.setColor(new Color(0, 0, 0, 5));
                g2.fillRoundRect(inset, inset + 3, getWidth() - 2 * inset, getHeight() - 2 * inset,
                        ARC + i, ARC + i);
            }
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(SHADOW, SHADOW, getWidth() - 2 * SHADOW, getHeight() - 2 * SHADOW, ARC, ARC);
            g2.setColor(Theme.CARD_BORDER);
            g2.drawRoundRect(SHADOW, SHADOW, getWidth() - 2 * SHADOW - 1, getHeight() - 2 * SHADOW - 1, ARC, ARC);
            g2.dispose();
        }
    }

    /** Teal circle with an open padlock. */
    private static class IconCircle extends JComponent {
        IconCircle() {
            Dimension d = new Dimension(94, 94);
            setPreferredSize(d);
            setMaximumSize(d);
            setMinimumSize(d);
            setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(Theme.PRIMARY);
            g2.fillOval(0, 0, getWidth(), getHeight());

            double cx = getWidth() / 2.0, cy = getHeight() / 2.0 + 2;
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Double(cx - 14, cy - 1, 28, 24, 6, 6));
            g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Arc2D.Double(cx - 9, cy - 21, 18, 20, 0, 180, Arc2D.OPEN));
            g2.draw(new Line2D.Double(cx - 9, cy - 11, cx - 9, cy - 1)); // right side lifted = unlocked
            g2.dispose();
        }
    }

    /** Rounded input row: [icon] [text field] [optional trailing button]. */
    private static class FieldPanel extends JPanel {
        private final Color background;
        private boolean focused;

        FieldPanel(Icon icon, JTextComponent field, JComponent trailing, Color background) {
            this.background = background;
            setOpaque(false);
            setLayout(new BorderLayout(14, 0));
            setBorder(new EmptyBorder(0, 18, 0, 14));
            setPreferredSize(new Dimension(100, 54));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
            setAlignmentX(Component.CENTER_ALIGNMENT);

            field.setBorder(null);
            field.setOpaque(false);
            field.setFont(Theme.font(Font.PLAIN, 15));
            field.setForeground(new Color(0x1F3A47));
            field.setCaretColor(Theme.PRIMARY);
            field.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e)   { focused = false; repaint(); }
            });

            add(new JLabel(icon), BorderLayout.WEST);
            add(field, BorderLayout.CENTER);
            if (trailing != null) add(trailing, BorderLayout.EAST);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(background);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(focused ? Theme.PRIMARY : Theme.BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.8f : 1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
        }
    }

    // ---- Text fields that show grey hint text while empty ----

    private static void paintPlaceholder(JTextComponent c, Graphics g, String hint) {
        if (c.getDocument().getLength() > 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        g2.setColor(Theme.TEXT_MUTED);
        g2.setFont(c.getFont());
        FontMetrics fm = g2.getFontMetrics();
        int y = (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(hint, c.getInsets().left, y);
        g2.dispose();
    }

    private static class PlaceholderTextField extends JTextField {
        private final String hint;
        PlaceholderTextField(String hint) { this.hint = hint; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintPlaceholder(this, g, hint);
        }
    }

    private static class PlaceholderPasswordField extends JPasswordField {
        private final String hint;
        PlaceholderPasswordField(String hint) { this.hint = hint; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintPlaceholder(this, g, hint);
        }
    }
}