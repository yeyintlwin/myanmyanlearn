package com.barlarlar.myanmyanlearn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barlarlar.myanmyanlearn.entity.RegistrationSettingsEntity;

@Repository
public interface RegistrationSettingsRepository extends JpaRepository<RegistrationSettingsEntity, Byte> {
}

