package com.clinic.file;

import com.clinic.model.Patient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PatientFileHandler {
    public static final String FILE_NAME = "patients.csv";
    private static final String HEADER = "patientId,name,age,contactNumber";

    private final Path file;

    public PatientFileHandler() {
        this(CsvUtil.defaultDataDir().resolve(FILE_NAME));
    }

    public PatientFileHandler(Path file) {
        this.file = file;
    }

    public Path getFile() { return file; }

    public void ensureFileExists() throws IOException {
        CsvUtil.createIfMissing(file, HEADER);
    }

    public void save(Collection<Patient> patients) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Patient p : patients) {
            lines.add(CsvUtil.toLine(
                    p.getPatientId(),
                    p.getName(),
                    String.valueOf(p.getAge()),
                    p.getContactNumber()));
        }
        CsvUtil.writeAtomically(file, lines);
    }

    public List<Patient> load() throws IOException {
        List<Patient> result = new ArrayList<>();
        ensureFileExists();
        String text = CsvUtil.readOrNull(file);
        if (text == null) return result;

        boolean first = true;
        for (List<String> row : CsvUtil.parse(text)) {
            if (first) { first = false; if (row.get(0).equals("patientId")) continue; }
            if (row.size() < 4 || row.get(0).trim().isEmpty() || row.get(1).trim().isEmpty()) continue;

            int age;
            try { age = Integer.parseInt(row.get(2).trim()); }
            catch (NumberFormatException e) { age = 0; }

            final String id = row.get(0).trim();
            boolean dup = false;
            for (Patient existing : result) if (existing.getPatientId().equalsIgnoreCase(id)) { dup = true; break; }
            if (dup) continue; // skip duplicate IDs from a hand-edited file
            result.add(new Patient(id, row.get(1).trim(), age, row.get(3).trim()));
        }
        return result;
    }
}