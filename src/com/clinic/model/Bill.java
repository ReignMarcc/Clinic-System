package com.clinic.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * One bill (invoice) for one appointment. It starts out PENDING and becomes
 * PAID once the payment is recorded.
 *
 * The service, status, payment method and discount are kept as the plain text
 * codes defined below (they are also what gets saved to bills.csv).
 *
 * Card details: only the cardholder name, the LAST 4 digits and the expiry are
 * kept. The full card number and the CVV are never stored.
 */
public class Bill {

    // ---- services (fixed prices) ----
    public static final String SERVICE_CHECKUP = "GENERAL_CHECKUP";
    public static final String SERVICE_LAB = "LAB_TEST";
    public static final BigDecimal CHECKUP_PRICE = new BigDecimal("700.00");
    public static final BigDecimal LAB_PRICE = new BigDecimal("1200.00");

    // ---- status ----
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";

    // ---- how it was paid ----
    public static final String METHOD_CASH = "CASH";
    public static final String METHOD_CARD = "CARD";

    // ---- discount / coverage ----
    // NONE = full price. HMO = health card details are recorded.
    // PWD / SENIOR = 20% off (RA 10754 / RA 9994).
    public static final String DISCOUNT_NONE = "NONE";
    public static final String DISCOUNT_HMO = "HMO";
    public static final String DISCOUNT_PWD = "PWD";
    public static final String DISCOUNT_SENIOR = "SENIOR";
    public static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.20");

    private final String invoiceId;          // INV-001, INV-002, ...
    private final String appointmentId;
    private final String patientId;
    private final String service;            // SERVICE_CHECKUP or SERVICE_LAB
    private final BigDecimal amount;         // price before any discount
    private final LocalDateTime createdAt;

    private String discount = DISCOUNT_NONE;
    private String discountIdNumber = "";           // PWD / Senior ID number
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String status = STATUS_PENDING;
    private LocalDateTime paidAt;
    private String method = "";                     // METHOD_CASH / METHOD_CARD once paid
    private BigDecimal amountReceived = BigDecimal.ZERO;

    private String cardHolder = "";
    private String cardLast4 = "";
    private String cardExpiry = "";

    private String hmoProvider = "";
    private String hmoMemberId = "";
    private String hmoHolder = "";
    private String hmoApprovalCode = "";

    public Bill(String invoiceId, String appointmentId, String patientId,
                String service, BigDecimal amount, LocalDateTime createdAt) {
        this.invoiceId = invoiceId;
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.service = isService(service) ? service : SERVICE_CHECKUP;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    // ------------------------------------------------------------------
    // Services, discounts and money (static helpers)
    // ------------------------------------------------------------------

    public static boolean isService(String s) {
        return SERVICE_CHECKUP.equals(s) || SERVICE_LAB.equals(s);
    }

    public static String serviceLabel(String service) {
        return SERVICE_LAB.equals(service) ? "Lab Test" : "General Check-up";
    }

    public static BigDecimal servicePrice(String service) {
        return SERVICE_LAB.equals(service) ? LAB_PRICE : CHECKUP_PRICE;
    }

    /**
     * Maps a saved appointment "reason" back to a service. Older appointments
     * were typed in freely, so anything mentioning "lab" is a Lab Test and
     * everything else is treated as a General Check-up.
     */
    public static String serviceFromReason(String reason) {
        return reason != null && reason.trim().toLowerCase().contains("lab") ? SERVICE_LAB : SERVICE_CHECKUP;
    }

    public static String discountLabel(String discount) {
        if (DISCOUNT_HMO.equals(discount)) return "HMO / Health Card";
        if (DISCOUNT_PWD.equals(discount)) return "PWD";
        if (DISCOUNT_SENIOR.equals(discount)) return "Senior Citizen";
        return "None";
    }

    /** PWD and Senior Citizen take 20% off; None and HMO take nothing off. */
    public static boolean isPercentDiscount(String discount) {
        return DISCOUNT_PWD.equals(discount) || DISCOUNT_SENIOR.equals(discount);
    }

    /** 20% of a price, rounded to centavos. */
    public static BigDecimal discountFor(BigDecimal price) {
        return price.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /** "₱1,200.00" */
    public static String money(BigDecimal v) {
        return String.format(Locale.ENGLISH, "\u20B1%,.2f", v);
    }

    /** "₱700" when the amount is whole, otherwise "₱700.50". */
    public static String moneyShort(BigDecimal v) {
        return v.stripTrailingZeros().scale() <= 0
                ? String.format(Locale.ENGLISH, "\u20B1%,d", v.toBigInteger())
                : money(v);
    }

    // ------------------------------------------------------------------
    // State changes
    // ------------------------------------------------------------------

    public void markPaid(String method, BigDecimal received, LocalDateTime when) {
        this.status = STATUS_PAID;
        this.method = method == null ? "" : method;
        this.amountReceived = received == null ? getAmountDue() : received;
        this.paidAt = when;
    }

    /** Used when loading from disk. */
    public void restore(String status, LocalDateTime paidAt, String method, BigDecimal received) {
        this.status = STATUS_PAID.equals(status) ? STATUS_PAID : STATUS_PENDING;
        this.paidAt = paidAt;
        this.method = METHOD_CASH.equals(method) || METHOD_CARD.equals(method) ? method : "";
        this.amountReceived = received == null ? BigDecimal.ZERO : received;
    }

    /** Sets the discount type. PWD and Senior take 20% off the price; NONE and HMO take nothing off. */
    public void setDiscount(String type, String idNumber) {
        this.discount = normalizeDiscount(type);
        this.discountIdNumber = nz(idNumber);
        this.discountAmount = isPercentDiscount(this.discount) ? discountFor(amount) : BigDecimal.ZERO;
    }

    /** Used when loading from disk. */
    public void restoreDiscount(String type, String idNumber, BigDecimal discountAmount) {
        this.discount = normalizeDiscount(type);
        this.discountIdNumber = nz(idNumber);
        this.discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
    }

    public void setCard(String holder, String last4, String expiry) {
        this.cardHolder = nz(holder);
        this.cardLast4 = nz(last4);
        this.cardExpiry = nz(expiry);
    }

    public void setHmo(String provider, String memberId, String holder, String approvalCode) {
        this.hmoProvider = nz(provider);
        this.hmoMemberId = nz(memberId);
        this.hmoHolder = nz(holder);
        this.hmoApprovalCode = nz(approvalCode);
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public boolean isPaid() { return STATUS_PAID.equals(status); }
    public boolean hasHmo() { return !hmoProvider.isEmpty(); }

    public String getInvoiceId() { return invoiceId; }
    public String getAppointmentId() { return appointmentId; }
    public String getPatientId() { return patientId; }
    public String getService() { return service; }
    public String getServiceLabel() { return serviceLabel(service); }
    /** The price of the service, before any discount. */
    public BigDecimal getAmount() { return amount; }
    public String getDiscount() { return discount; }
    public String getDiscountLabel() { return discountLabel(discount); }
    public String getDiscountIdNumber() { return discountIdNumber; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    /** What the patient actually pays: price minus discount. */
    public BigDecimal getAmountDue() { return amount.subtract(discountAmount); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public String getMethod() { return method; }
    public BigDecimal getAmountReceived() { return amountReceived; }
    public String getCardHolder() { return cardHolder; }
    public String getCardLast4() { return cardLast4; }
    public String getCardExpiry() { return cardExpiry; }
    public String getHmoProvider() { return hmoProvider; }
    public String getHmoMemberId() { return hmoMemberId; }
    public String getHmoHolder() { return hmoHolder; }
    public String getHmoApprovalCode() { return hmoApprovalCode; }

    private static String normalizeDiscount(String type) {
        return DISCOUNT_HMO.equals(type) || DISCOUNT_PWD.equals(type) || DISCOUNT_SENIOR.equals(type)
                ? type : DISCOUNT_NONE;
    }

    private static String nz(String s) { return s == null ? "" : s; }
}