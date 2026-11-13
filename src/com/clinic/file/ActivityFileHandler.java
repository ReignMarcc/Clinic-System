package com.clinic.file;

import com.clinic.model.ActivityEntry;
import com.clinic.model.ActivityType;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Reads and writes activity.csv: one row per thing that happened in the system. */
public class ActivityFileHandler {
    public static final String FILE_NAME = "activity.csv";
    private static final String HEADER = "timestamp,actor,action,patientId,doctorId,appointmentId,details";

    private final Path file;

    public ActivityFileHandler() {
        this(CsvUtil.defaultDataDir().resolve(FILE_NAME));
    }

    public ActivityFileHandler(Path file) {
        this.file = file;
    }

    public Path getFile() { return file; }

    public void ensureFileExists() throws IOException {
        CsvUtil.createIfMissing(file, HEADER);
    }

    public void save(Collection<ActivityEntry> entries) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (ActivityEntry e : entries) {
            lines.add(CsvUtil.toLine(
                    e.getTimestamp().withNano(0).toString(),
                    e.getActor(),
                    e.getType().name(),
                    e.getPatientId(),
                    e.getDoctorId(),
                    e.getAppointmentId(),
                    e.getDetails()));
        }
        CsvUtil.writeAtomically(file, lines);
    }

    public List<ActivityEntry> load() throws IOException {
        List<ActivityEntry> result = new ArrayList<>();
        ensureFileExists();
        String text = CsvUtil.readOrNull(file);
        if (text == null) return result;

        boolean first = true;
        for (List<String> row : CsvUtil.parse(text)) {
            if (first) { first = false; if (row.get(0).equals("timestamp")) continue; }
            if (row.size() < 7) continue;

            LocalDateTime when;
            try { when = LocalDateTime.parse(row.get(0).trim()); }
            catch (DateTimeParseException e) { continue; }

            ActivityType type;
            try { type = ActivityType.valueOf(row.get(2).trim()); }
            catch (IllegalArgumentException e) { continue; }   // unknown action from a newer/older version

            result.add(new ActivityEntry(when, row.get(1).trim(), type,
                    row.get(3).trim(), row.get(4).trim(), row.get(5).trim(), row.get(6)));
        }
        return result;
    }
}
