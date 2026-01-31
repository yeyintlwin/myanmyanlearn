package com.barlarlar.myanmyanlearn.service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.barlarlar.myanmyanlearn.entity.RegistrationSettingsEntity;
import com.barlarlar.myanmyanlearn.repository.RegistrationSettingsRepository;

@Service
public class RegistrationSettingsService {

    @Autowired
    private RegistrationSettingsRepository registrationSettingsRepository;

    private volatile boolean initialized;

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            Byte id = 1;
            if (!registrationSettingsRepository.existsById(id)) {
                RegistrationSettingsEntity e = new RegistrationSettingsEntity();
                e.setId(id);
                e.setAllowedDomain(null);
                e.setEnforceDomain(false);
                registrationSettingsRepository.save(e);
            }
            initialized = true;
        }
    }

    public SettingsView getSettings() {
        ensureInitialized();
        RegistrationSettingsEntity e = registrationSettingsRepository.findById((byte) 1).orElse(null);
        SettingsView v = new SettingsView();
        v.setAllowedDomain(e != null ? e.getAllowedDomain() : null);
        v.setEnforceDomain(e != null && e.getEnforceDomain() != null && e.getEnforceDomain());
        return v;
    }

    public void updateSettings(String allowedDomain, boolean enforceDomain) {
        ensureInitialized();
        String normalizedDomain = normalizeDomain(allowedDomain);
        RegistrationSettingsEntity e = registrationSettingsRepository.findById((byte) 1).orElseGet(() -> {
            RegistrationSettingsEntity created = new RegistrationSettingsEntity();
            created.setId((byte) 1);
            return created;
        });
        e.setAllowedDomain(normalizedDomain);
        e.setEnforceDomain(enforceDomain);
        registrationSettingsRepository.save(e);
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
