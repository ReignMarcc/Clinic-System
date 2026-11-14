package com.clinic.service;

import com.clinic.exception.InvalidInputException;
import com.clinic.file.ServiceFileHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The list of services (name + price) offered in the Add Walk-in / Add Appointment drop-down.
 * Saved in services.csv. A removed service is only hidden from the drop-down, so old bills
 * that used it still show the right name.
 */
public final class ServiceCatalog {

    public static final class Item {
        private final String id;
        private final String name;
        private final BigDecimal price;
        private boolean active;

        Item(String id, String name, BigDecimal price, boolean active) {
            this.id = id; this.name = name; this.price = price; this.active = active;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public BigDecimal getPrice() { return price; }
        public boolean isActive() { return active; }
    }

    private static ServiceCatalog instance;

    /** Special drop-down entry for services that are not in the list. Never saved to services.csv. */
    public static final Item OTHERS = new Item("OTHERS", "Others", BigDecimal.ZERO, true);

    public static synchronized ServiceCatalog getInstance() {
        if (instance == null) instance = new ServiceCatalog();
        return instance;
    }

    private final ServiceFileHandler files = new ServiceFileHandler();
    private final List<Item> items = new ArrayList<>();

    private ServiceCatalog() {
        try {
            for (String[] r : files.load()) {
                try {
                    items.add(new Item(r[0], r[1], new BigDecimal(r[2]).setScale(2, RoundingMode.HALF_UP),
                            !"false".equalsIgnoreCase(r[3])));
                } catch (NumberFormatException ignored) { /* skip a bad hand-edited row */ }
            }
        } catch (IOException e) {
            System.err.println("Could not read services: " + e.getMessage());
        }
        boolean added = false;
        if (findAny("GENERAL_CHECKUP") == null) { items.add(0, new Item("GENERAL_CHECKUP", "General Check-up", new BigDecimal("700.00"), true)); added = true; }
        if (findAny("LAB_TEST") == null)        { items.add(1, new Item("LAB_TEST", "Lab Test", new BigDecimal("1200.00"), true)); added = true; }
        if (added) persist();
    }

    private Item findAny(String id) {
        for (Item i : items) if (i.id.equalsIgnoreCase(id)) return i;
        return null;
    }

    /** The service with this id (even if it was removed from the drop-down), or null. */
    public synchronized Item find(String id) {
        return id == null ? null : findAny(id.trim());
    }

    public synchronized Item findByName(String name) {
        if (name == null) return null;
        for (Item i : items) if (i.name.equalsIgnoreCase(name.trim())) return i;
        return null;
    }

    /** Services shown in the drop-down. */
    public synchronized List<Item> getActive() {
        List<Item> out = new ArrayList<>();
        for (Item i : items) if (i.active) out.add(i);
        return out;
    }

    public synchronized Item add(String name, String priceText) throws InvalidInputException {
        String n = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (n.isEmpty()) throw new InvalidInputException("Please type the service name.");
        if (n.length() > 40) throw new InvalidInputException("Service name can be at most 40 characters.");
        if (n.contains(",") || n.contains("\"")) throw new InvalidInputException("Service name can't contain commas or quotation marks.");
        for (Item i : items) {
            if (i.active && i.name.equalsIgnoreCase(n)) throw new InvalidInputException("\"" + n + "\" is already in the list.");
        }
        BigDecimal price;
        try {
            price = new BigDecimal(priceText == null ? "" : priceText.replace(",", "").replace("\u20B1", "").replace("PHP", "").trim());
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Please enter the price as a number, e.g. 850 or 850.50.");
        }
        price = price.setScale(2, RoundingMode.HALF_UP);
        if (price.signum() <= 0) throw new InvalidInputException("The price must be more than zero.");
        if (price.compareTo(new BigDecimal("1000000")) > 0) throw new InvalidInputException("The price is too large.");

        int max = 0;
        for (Item i : items) {
            if (i.id.startsWith("SVC")) {
                try { max = Math.max(max, Integer.parseInt(i.id.substring(3))); } catch (NumberFormatException ignored) { }
            }
        }
        Item item = new Item("SVC" + (max + 1), n, price, true);
        items.add(item);
        persist();
        return item;
    }

    /** Hides a service from the drop-down. Old bills keep its name. */
    public synchronized void remove(String id) {
        Item i = findAny(id);
        if (i != null) { i.active = false; persist(); }
    }

    private void persist() {
        List<String[]> rows = new ArrayList<>();
        for (Item i : items) rows.add(new String[]{i.id, i.name, i.price.toPlainString(), String.valueOf(i.active)});
        try {
            files.save(rows);
        } catch (IOException e) {
            System.err.println("Could not save services: " + e.getMessage());
        }
    }
}
