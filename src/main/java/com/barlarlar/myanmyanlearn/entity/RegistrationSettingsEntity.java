package com.barlarlar.myanmyanlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "registration_settings")
@Getter
@Setter
@NoArgsConstructor
public class RegistrationSettingsEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Byte id;

    @Column(name = "allowed_domain", length = 255)
    private String allowedDomain;

    @Column(name = "enforce_domain", nullable = false)
    private Boolean enforceDomain = false;
}
