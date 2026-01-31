package com.barlarlar.myanmyanlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "registration_settings")
public class RegistrationSettingsEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Byte id;

    @Column(name = "allowed_domain", length = 255)
    private String allowedDomain;

    @Column(name = "enforce_domain", nullable = false)
    private Boolean enforceDomain = false;

    public RegistrationSettingsEntity() {
    }

    public Byte getId() {
        return id;
    }

    public void setId(Byte id) {
        this.id = id;
    }

    public String getAllowedDomain() {
        return allowedDomain;
    }

    public void setAllowedDomain(String allowedDomain) {
        this.allowedDomain = allowedDomain;
    }

    public Boolean getEnforceDomain() {
        return enforceDomain;
    }

    public void setEnforceDomain(Boolean enforceDomain) {
        this.enforceDomain = enforceDomain;
    }
}

