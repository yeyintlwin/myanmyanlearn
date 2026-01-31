package com.barlarlar.myanmyanlearn.repository;

import com.barlarlar.myanmyanlearn.entity.LoginAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, String> {
}
