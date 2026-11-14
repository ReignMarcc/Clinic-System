package com.clinic.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.function.IntPredicate;
import java.util.function.UnaryOperator;

/** Small form helpers shared by the billing pop-ups (typing limits, checks, layout bits). */
final class PayKit {

    static final Color DARK = Theme.SIDEBAR_BG;

    private PayKit() {}

    // ---- layout bits ----

    static JPanel vbox() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    static JLabel sectionLabel(String text, boolean required) {
        JLabel l = new JLabel(required ? "<html>" + text + "<font color='#E53935'>*</font></html>" : text);
        l.setFont(Theme.font(Font.BOLD, 20));
        l.setForeground(DARK);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Label on top, styled field underneath. */
    static JPanel cell(String label, JComponent field, boolean required) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        JLabel l = new JLabel(required ? "<html>" + label + "<font color='#E53935'>*</font></html>" : label);
        l.setFont(Theme.font(Font.BOLD, 16));
        l.setForeground(DARK);
        p.add(l, BorderLayout.NORTH);
        p.add(new FormField(null, field, null), BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return fixHeight(p);
    }

    /** Stops BoxLayout from stretching a row taller than it needs to be. */
    static <T extends JComponent> T fixHeight(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    static JComponent chipRow(JComponent... chips) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        for (int i = 0; i < chips.length; i++) {
            if (i > 0) row.add(Box.createHorizontalStrut(8));
            row.add(chips[i]);
        }
        return fixHeight(row);
    }

    static void sizeChip(UiKit.ToggleChip chip, int width) {
        chip.setFont(Theme.font(Font.BOLD, 14));
        chip.setPreferredSize(new Dimension(width, 40));
        chip.setMinimumSize(new Dimension(width, 40));
        chip.setMaximumSize(new Dimension(width, 40));
    }

    static JPasswordField passwordField(String hint) {
        JPasswordField f = new JPasswordField();
        f.setBorder(null);
        f.setOpaque(false);
        f.setFont(Theme.font(Font.PLAIN, 15));
        f.setForeground(Theme.TEXT_DARK);
        f.setCaretColor(Theme.SIDEBAR_BG);
        f.setToolTipText(hint);
        return f;
    }

    // ---- what staff are allowed to type ----

    /** Applies "normalize" to the text that WOULD result from an edit; null rejects the edit. */
    static void filter(JTextComponent field, UnaryOperator<String> normalize) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                    throws BadLocationException {
                replace(fb, offset, 0, text, attrs);
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                replace(fb, offset, length, "", null);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = current.substring(0, offset) + (text == null ? "" : text)
                        + current.substring(offset + length);
                String out = normalize.apply(next);
                if (out == null || out.equals(current)) return;
                fb.replace(0, current.length(), out, attrs);
            }
        });
    }

    static String allowed(String s, int max, IntPredicate ok) {
        if (s.length() > max) return null;
        for (int i = 0; i < s.length(); i++) if (!ok.test(s.charAt(i))) return null;
        return s;
    }

    static boolean nameChar(int c) {
        return Character.isLetter(c) || " .'-".indexOf(c) >= 0;
    }

    // ---- checking helpers ----

    /** At least two words, each with letters (e.g. "Juan Dela Cruz"). */
    static boolean isFullName(String name) {
        int words = 0;
        for (String part : name.trim().split("\\s+")) if (letterCount(part) >= 1) words++;
        return words >= 2 && letterCount(name) >= 3;
    }

    static int letterCount(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (Character.isLetter(s.charAt(i))) n++;
        return n;
    }

    static boolean hasDigit(String s) {
        for (int i = 0; i < s.length(); i++) if (Character.isDigit(s.charAt(i))) return true;
        return false;
    }

    static boolean allSame(String s) {
        for (int i = 1; i < s.length(); i++) if (s.charAt(i) != s.charAt(0)) return false;
        return !s.isEmpty();
    }

    static boolean isMastercardPrefix(String d) {
        if (d.length() < 4) return false;
        int two = Integer.parseInt(d.substring(0, 2));
        int four = Integer.parseInt(d.substring(0, 4));
        return (two >= 51 && two <= 55) || (four >= 2221 && four <= 2720);
    }

    /** Luhn checksum: catches mistyped or made-up card numbers. */
    static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (dbl) { n *= 2; if (n > 9) n -= 9; }
            sum += n;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }

    static EmptyBorder pad(int t, int l, int b, int r) {
        return new EmptyBorder(t, l, b, r);
    }
}