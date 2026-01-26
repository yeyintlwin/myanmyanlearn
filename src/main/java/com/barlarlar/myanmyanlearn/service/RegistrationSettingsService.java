package com.barlarlar.myanmyanlearn.service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RegistrationSettingsService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private volatile boolean initialized;

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS registration_settings (" +
                            "id TINYINT NOT NULL PRIMARY KEY," +
                            "allowed_domain VARCHAR(255)," +
                            "enforce_domain TINYINT(1) NOT NULL DEFAULT 0" +
                            ")");
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM registration_settings WHERE id = 1",
                    Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.update(
                        "INSERT INTO registration_settings (id, allowed_domain, enforce_domain) VALUES (1, NULL, 0)");
            }
            initialized = true;
        }
    }

    public SettingsView getSettings() {
        ensureInitialized();
        return jdbcTemplate.query(
                "SELECT allowed_domain, enforce_domain FROM registration_settings WHERE id = 1",
                rs -> {
                    if (!rs.next()) {
                        SettingsView v = new SettingsView();
                        v.setAllowedDomain(null);
                        v.setEnforceDomain(false);
                        return v;
                    }
                    SettingsView v = new SettingsView();
                    v.setAllowedDomain(rs.getString("allowed_domain"));
                    v.setEnforceDomain(rs.getInt("enforce_domain") != 0);
                    return v;
                });
    }

    public void updateSettings(String allowedDomain, boolean enforceDomain) {
        ensureInitialized();
        String normalizedDomain = normalizeDomain(allowedDomain);
        jdbcTemplate.update(
                "UPDATE registration_settings SET allowed_domain = ?, enforce_domain = ? WHERE id = 1",
                normalizedDomain,
                enforceDomain ? 1 : 0);
    }

    public boolean isRegistrationEmailAllowed(String email) {
        ensureInitialized();
        SettingsView settings = getSettings();
        if (!settings.isEnforceDomain()) {
            return true;
        }
        String domain = normalizeDomain(settings.getAllowedDomain());
        if (domain == null || domain.isBlank()) {
            return true;
        }
        if (email == null || email.isBlank()) {
            return false;
        }
        String loweredEmail = email.trim().toLowerCase(Locale.ROOT);
        return loweredEmail.endsWith("@" + domain);
    }

    public String getDisplayDomain() {
        ensureInitialized();
        SettingsView settings = getSettings();
        String domain = normalizeDomain(settings.getAllowedDomain());
        return domain == null ? "" : domain;
    }

    private String normalizeDomain(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.startsWith("@")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    public static class SettingsView {
        private String allowedDomain;
        private boolean enforceDomain;

        public String getAllowedDomain() {
            return allowedDomain;
        }

        public void setAllowedDomain(String allowedDomain) {
            this.allowedDomain = allowedDomain;
        }

        public boolean isEnforceDomain() {
            return enforceDomain;
        }

        public void setEnforceDomain(boolean enforceDomain) {
            this.enforceDomain = enforceDomain;
        }
    }
}
