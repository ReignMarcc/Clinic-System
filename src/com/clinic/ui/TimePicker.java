package com.clinic.ui;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * "Alarm clock" style time picker: an hour wheel and a minute wheel, each
 * scrollable up/down with the spinner arrows or the mouse wheel. Restricted
 * to appointment start times between 7 AM and 4 PM (so the 1-hour
 * consultation always finishes by 5 PM).
 */
public class TimePicker extends JPanel {

    private static final List<String> HOUR_LABELS = Arrays.asList(
            "7 AM", "8 AM", "9 AM", "10 AM", "11 AM", "12 PM", "1 PM", "2 PM", "3 PM", "4 PM");
    private static final int[] HOUR_24 = {7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    private final JSpinner hourSpinner = new JSpinner(new SpinnerListModel(HOUR_LABELS));
    private final JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

    public TimePicker() {
        setOpaque(false);
        // Centered, single row: the field's own label already says "Appointment Time",
        // so no extra "Hour"/"Minute" captions are needed, keeping this short enough
        // to fit inside the same 46px-tall row every other field uses.
        setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));

        style(hourSpinner, 78);
        JSpinner.NumberEditor minuteEditor = new JSpinner.NumberEditor(minuteSpinner, "00");
        minuteSpinner.setEditor(minuteEditor);
        style(minuteSpinner, 56);

        JLabel colon = new JLabel(":");
        colon.setFont(Theme.font(Font.BOLD, 16));
        colon.setForeground(Theme.SIDEBAR_BG);

        add(hourSpinner);
        add(colon);
        add(minuteSpinner);
    }

    private static void style(JSpinner spinner, int width) {
        spinner.setPreferredSize(new Dimension(width, 32));
        spinner.setFont(Theme.font(Font.BOLD, 14));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField field = ((JSpinner.DefaultEditor) editor).getTextField();
            field.setHorizontalAlignment(JTextField.CENTER);
            field.setEditable(false); // scroll/arrows only, so the text always stays a valid value
            field.setBackground(Color.WHITE);
            field.setForeground(Theme.TEXT_DARK);
        }
    }

    /** The currently selected time (always a valid 7 AM - 4:59 PM start time). */
    public LocalTime getTime() {
        int idx = HOUR_LABELS.indexOf(hourSpinner.getValue());
        int hour24 = HOUR_24[Math.max(idx, 0)];
        int minute = (Integer) minuteSpinner.getValue();
        return LocalTime.of(hour24, minute);
    }

    public void setTime(LocalTime time) {
        int hour24 = time.getHour();
        for (int i = 0; i < HOUR_24.length; i++) {
            if (HOUR_24[i] == hour24) {
                hourSpinner.setValue(HOUR_LABELS.get(i));
                break;
            }
        }
        minuteSpinner.setValue(time.getMinute());
    }
}