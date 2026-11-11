package com.clinic.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Local admin credentials for this desktop demo. */
public final class AdminCredentials {
    private static final Path FILE = Paths.get(System.getProperty("user.home"), ".clinic-system", "admin.properties");
    private static final String DEFAULT_EMAIL = "admin@clinic.com";
    private static final String DEFAULT_PASSWORD = "admin123";
    private AdminCredentials() {}

    public static synchronized String getEmail() { return load().getProperty("email", DEFAULT_EMAIL); }

    public static synchronized boolean authenticate(String email, char[] password) {
        Properties p = load();
        if (email == null || password == null) return false;
        if (!p.getProperty("email", DEFAULT_EMAIL).equalsIgnoreCase(email.trim())) return false;
        String saltText = p.getProperty("salt");
        String hashText = p.getProperty("hash");
        if (saltText == null || hashText == null) {
            return DEFAULT_PASSWORD.equals(new String(password));
        }
        try {
            byte[] salt = Base64.getDecoder().decode(saltText);
            byte[] expected = Base64.getDecoder().decode(hashText);
            return MessageDigest.isEqual(expected, hash(password, salt));
        } catch (Exception ex) { return false; }
    }

    public static synchronized void change(String currentEmail, char[] currentPassword, String newEmail, char[] newPassword) throws Exception {
        if (!authenticate(currentEmail, currentPassword)) throw new IllegalArgumentException("Current email or password is incorrect.");
        String email = newEmail == null ? "" : newEmail.trim();
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) throw new IllegalArgumentException("Enter a valid email address.");
        if (newPassword == null || newPassword.length < 8) throw new IllegalArgumentException("New password must be at least 8 characters.");
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        Properties p = new Properties(); p.setProperty("email", email);
        p.setProperty("salt", Base64.getEncoder().encodeToString(salt));
        p.setProperty("hash", Base64.getEncoder().encodeToString(hash(newPassword, salt)));
        Files.createDirectories(FILE.getParent());
        try (OutputStream out = Files.newOutputStream(FILE)) { p.store(out, "Clinic System admin credentials"); }
    }

    private static Properties load() {
        Properties p = new Properties();
        if (Files.exists(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) { p.load(in); }
            catch (IOException ignored) { }
        }
        return p;
    }

    private static byte[] hash(char[] password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password, salt, 120000, 256);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }
}
