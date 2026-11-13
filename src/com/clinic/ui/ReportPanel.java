package com.clinic.ui;

import com.clinic.service.ClinicManager;
import com.clinic.service.ReportGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * "Reports" page. Pick a day (or a range), press Generate, read the report on screen,
 * and save it as a .txt file. The report itself is built by ReportGenerator.
 */
public class ReportPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_PARSER = DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
    private static final LocalDate EARLIEST = LocalDate.of(2020, 1, 1);

    private final ClinicManager clinic;
    private final JTextField fromField = FormField.textField("MM/DD/YYYY");
    private final JTextField toField = FormField.textField("MM/DD/YYYY");
    private final JTextArea output = new JTextArea();
    private final JLabel errorLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel(" ");
    private PillButton saveButton;

    private String lastReport = "";
    private LocalDate lastFrom, lastTo;

    public ReportPanel(ClinicManager clinic) {
        this.clinic = clinic;

        setLayout(new BorderLayout(0, 18));
        setBackground(Theme.CONTENT_BG);
        setBorder(new EmptyBorder(18, 22, 22, 22));
        add(UiKit.pageTopBar("Reports"), BorderLayout.NORTH);

        UiKit.Card card = new UiKit.Card(18, Color.WHITE, null);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(18, 24, 16, 24));
        card.add(buildControls(), BorderLayout.NORTH);
        card.add(buildOutput(), BorderLayout.CENTER);

        statusLabel.setFont(Theme.font(Font.PLAIN, 13));
        statusLabel.setForeground(Theme.TEXT_MUTED);
        card.add(statusLabel, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);

        limitDate(fromField);
        limitDate(toField);
        setRange(LocalDate.now(), LocalDate.now());
        generate();
    }

    /** Called when the page is shown: rebuilds the report for the dates currently chosen. */
    public void refresh() {
        generate();
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private JComponent buildControls() {
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);

        JLabel title = new JLabel("Generate Report");
        title.setFont(Theme.font(Font.BOLD, 22));
        title.setForeground(Theme.TEXT_DARK);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);
        row.add(label("From"));
        row.add(dateRow(fromField));
        row.add(label("To"));
        row.add(dateRow(toField));

        PillButton generate = PillButton.primary("Generate", Icons.of(Icons.Type.REPORTS, 20, Color.WHITE));
        generate.setFont(Theme.font(Font.BOLD, 15));
        generate.setPreferredSize(new Dimension(160, 46));
        generate.addActionListener(e -> generate());
        row.add(generate);

        saveButton = PillButton.secondary("Save as .txt");
        saveButton.setFont(Theme.font(Font.BOLD, 15));
        saveButton.setPreferredSize(new Dimension(150, 46));
        saveButton.addActionListener(e -> saveToFile());
        row.add(saveButton);

        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        quick.setOpaque(false);
        quick.add(linkButton("Today", () -> { setRange(LocalDate.now(), LocalDate.now()); generate(); }));
        quick.add(linkButton("Yesterday", () -> { LocalDate y = LocalDate.now().minusDays(1); setRange(y, y); generate(); }));
        quick.add(linkButton("Last 7 days", () -> { setRange(LocalDate.now().minusDays(6), LocalDate.now()); generate(); }));
        quick.add(linkButton("This month", () -> { LocalDate n = LocalDate.now(); setRange(n.withDayOfMonth(1), n); generate(); }));

        errorLabel.setFont(Theme.font(Font.PLAIN, 14));
        errorLabel.setForeground(Theme.ERROR);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{row, quick, errorLabel}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        stack.add(row);
        stack.add(Box.createVerticalStrut(6));
        stack.add(quick);
        stack.add(Box.createVerticalStrut(2));
        stack.add(errorLabel);

        top.add(title, BorderLayout.NORTH);
        top.add(stack, BorderLayout.CENTER);
        return top;
    }

    private JComponent buildOutput() {
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        output.setForeground(Theme.TEXT_DARK);
        output.setBackground(Color.WHITE);
        output.setMargin(new Insets(10, 14, 10, 14));
        output.setLineWrap(false);

        JScrollPane scroll = UiKit.roundScroll(output, 14, Theme.BORDER, 1.2f);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, 15));
        l.setForeground(Theme.TEXT_DARK);
        return l;
    }

    private JComponent dateRow(JTextField field) {
        IconButton calendarButton = new IconButton(
                Icons.of(Icons.Type.CALENDAR, 20, Theme.SIDEBAR_BG), "Pick a date", new Color(0, 0, 0, 20));
        FormField row = new FormField(null, field, calendarButton);
        row.setPreferredSize(new Dimension(190, 46));
        calendarButton.addActionListener(e -> {
            LocalDate current = parse(field.getText());
            new CalendarPopup(current == null ? LocalDate.now() : current, EARLIEST,
                    d -> field.setText(d.format(DATE_FMT)))
                    .show(row, 0, row.getHeight() + 4);
        });
        return row;
    }

    private JButton linkButton(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(Theme.font(Font.BOLD, 13));
        b.setForeground(Theme.PRIMARY);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Theme.hand());
        b.setMargin(new Insets(0, 0, 0, 0));
        b.addActionListener(e -> action.run());
        return b;
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void setRange(LocalDate from, LocalDate to) {
        fromField.setText(from.format(DATE_FMT));
        toField.setText(to.format(DATE_FMT));
    }

    private static LocalDate parse(String text) {
        try {
            return LocalDate.parse(text.trim(), DATE_PARSER);
        } catch (Exception e) {
            return null;
        }
    }

    private void generate() {
        errorLabel.setText(" ");
        LocalDate from = parse(fromField.getText());
        LocalDate to = parse(toField.getText());
        if (from == null || to == null) {
            errorLabel.setText("Please enter both dates as MM/DD/YYYY (for example 11/12/2026).");
            return;
        }
        if (to.isBefore(from)) {
            errorLabel.setText("The \"To\" date can't be earlier than the \"From\" date.");
            return;
        }
        try {
            lastReport = ReportGenerator.generate(clinic, from, to);
            lastFrom = from;
            lastTo = to;
            output.setText(lastReport);
            output.setCaretPosition(0);
            statusLabel.setText(from.equals(to)
                    ? "Report for " + from.format(DATE_FMT) + " - press Save as .txt to keep a copy."
                    : "Report for " + from.format(DATE_FMT) + " to " + to.format(DATE_FMT) + " - press Save as .txt to keep a copy.");
        } catch (Exception ex) {
            errorLabel.setText("Could not build the report: " + ex.getMessage());
        }
    }

    private void saveToFile() {
        if (lastReport.isEmpty() || lastFrom == null) {
            errorLabel.setText("Generate a report first.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save report");
        chooser.setFileFilter(new FileNameExtensionFilter("Text file (*.txt)", "txt"));
        String name = "clinic-report_" + lastFrom.format(FILE_FMT)
                + (lastFrom.equals(lastTo) ? "" : "_to_" + lastTo.format(FILE_FMT)) + ".txt";
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), name));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".txt")) file = new File(file.getParentFile(), file.getName() + ".txt");
        if (file.exists() && JOptionPane.showConfirmDialog(this, file.getName() + " already exists. Replace it?",
                "Save report", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            // Windows-friendly line endings so it also looks right in Notepad
            String text = lastReport.replace("\r\n", "\n").replace("\n", "\r\n");
            Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
            JOptionPane.showMessageDialog(this, "Report saved to:\n" + file.getAbsolutePath(),
                    "Report saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not save the report: " + ex.getMessage(),
                    "Unable to save", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Only digits and "/" in the date boxes, max 10 characters. */
    private static void limitDate(JTextField field) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                    throws BadLocationException {
                replace(fb, offset, 0, text, attrs);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text == null) {
                    super.replace(fb, offset, length, null, attrs);
                    return;
                }
                StringBuilder ok = new StringBuilder();
                for (char c : text.toCharArray()) if ((c >= '0' && c <= '9') || c == '/') ok.append(c);
                int room = 10 - (fb.getDocument().getLength() - length);
                if (room <= 0 && ok.length() > 0) return;
                if (ok.length() > room) ok.setLength(room);
                super.replace(fb, offset, length, ok.toString(), attrs);
            }
        });
    }
}
