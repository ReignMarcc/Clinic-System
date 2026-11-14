package com.clinic.file;

import com.clinic.model.Bill;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reads/writes bills.csv, which sits next to the other CSV files.
 * Same style as AppointmentFileHandler. It lives in this package because
 * CsvUtil is package-private.
 */
public class BillFileHandler {
    public static final String FILE_NAME = "bills.csv";
    private static final String HEADER =
            "invoiceId,appointmentId,patientId,service,amount,discount,discountId,discountAmount,status,"
                    + "createdAt,paidAt,method,amountReceived,cardHolder,cardLast4,cardExpiry,"
                    + "hmoProvider,hmoMemberId,hmoHolder,hmoApprovalCode";

    private final Path file;

    public BillFileHandler() {
        this(CsvUtil.defaultDataDir().resolve(FILE_NAME));
    }

    public BillFileHandler(Path file) {
        this.file = file;
    }

    public Path getFile() { return file; }

    public void ensureFileExists() throws IOException {
        CsvUtil.createIfMissing(file, HEADER);
    }

    public void save(Collection<Bill> bills) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Bill b : bills) {
            lines.add(CsvUtil.toLine(
                    b.getInvoiceId(),
                    b.getAppointmentId(),
                    b.getPatientId(),
                    b.getService(),
                    b.getAmount().toPlainString(),
                    b.getDiscount(),
                    b.getDiscountIdNumber(),
                    b.getDiscountAmount().toPlainString(),
                    b.getStatus(),
                    b.getCreatedAt().toString(),
                    b.getPaidAt() == null ? "" : b.getPaidAt().toString(),
                    b.getMethod(),
                    b.getAmountReceived().toPlainString(),
                    b.getCardHolder(),
                    b.getCardLast4(),
                    b.getCardExpiry(),
                    b.getHmoProvider(),
                    b.getHmoMemberId(),
                    b.getHmoHolder(),
                    b.getHmoApprovalCode()));
        }
        CsvUtil.writeAtomically(file, lines);
    }

    public List<Bill> load() throws IOException {
        List<Bill> result = new ArrayList<>();
        ensureFileExists();
        String text = CsvUtil.readOrNull(file);
        if (text == null) return result;

        boolean first = true;
        for (List<String> row : CsvUtil.parse(text)) {
            if (first) { first = false; if (row.get(0).equals("invoiceId")) continue; }
            if (row.size() < 20 || row.get(0).trim().isEmpty()) continue;

            String rawService = row.get(3).trim();
            String service = Bill.isCustom(rawService) ? rawService : rawService.toUpperCase();
            if (!Bill.isService(service)) continue;

            BigDecimal amount;
            LocalDateTime created;
            try {
                amount = new BigDecimal(row.get(4).trim());
                created = LocalDateTime.parse(row.get(9).trim());
            } catch (NumberFormatException | DateTimeParseException e) {
                continue;
            }

            String status = row.get(8).trim().toUpperCase();
            String method = row.get(11).trim().toUpperCase();

            LocalDateTime paidAt = null;
            if (!row.get(10).trim().isEmpty()) {
                try { paidAt = LocalDateTime.parse(row.get(10).trim()); }
                catch (DateTimeParseException ignored) { }
            }
            BigDecimal received = BigDecimal.ZERO;
            try { received = new BigDecimal(row.get(12).trim()); }
            catch (NumberFormatException ignored) { }

            Bill b = new Bill(row.get(0).trim(), row.get(1).trim(), row.get(2).trim(), service, amount, created);
            b.restore(status, paidAt, method, received);

            String discount = row.get(5).trim().toUpperCase();
            BigDecimal discountAmount = BigDecimal.ZERO;
            try { discountAmount = new BigDecimal(row.get(7).trim()); }
            catch (NumberFormatException ignored) { }
            b.restoreDiscount(discount, row.get(6), discountAmount);

            b.setCard(row.get(13), row.get(14), row.get(15));
            b.setHmo(row.get(16), row.get(17), row.get(18), row.get(19));
            result.add(b);
        }
        return result;
    }
}