package com.clinic.file;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads and writes services.csv (serviceId,name,price,active) next to the other data files. */
public class ServiceFileHandler {
    private static final String HEADER = "serviceId,name,price,active";
    private final Path file;

    public ServiceFileHandler() {
        this.file = CsvUtil.defaultDataDir().resolve("services.csv");
    }

    /** Each row is {id, name, price, active}. */
    public List<String[]> load() throws IOException {
        List<String[]> rows = new ArrayList<>();
        CsvUtil.createIfMissing(file, HEADER);
        String text = CsvUtil.readOrNull(file);
        if (text == null) return rows;
        boolean first = true;
        for (List<String> r : CsvUtil.parse(text)) {
            if (first) { first = false; if (r.get(0).equals("serviceId")) continue; }
            if (r.size() < 4 || r.get(0).trim().isEmpty() || r.get(1).trim().isEmpty()) continue;
            rows.add(new String[]{r.get(0).trim(), r.get(1).trim(), r.get(2).trim(), r.get(3).trim()});
        }
        return rows;
    }

    public void save(List<String[]> rows) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (String[] r : rows) lines.add(CsvUtil.toLine(r[0], r[1], r[2], r[3]));
        CsvUtil.writeAtomically(file, lines);
    }
}
