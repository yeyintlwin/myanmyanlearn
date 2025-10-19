package com.barlarlar.myanmyanlearn.repository;

import com.barlarlar.myanmyanlearn.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, String> {

    // Find member by email
    Optional<Member> findByEmail(String email);

    // Check if email exists
    boolean existsByEmail(String email);

    // Check if user ID exists
    boolean existsByUserId(String userId);

    // Find member by email and active status
    Optional<Member> findByEmailAndActive(String email, Boolean active);

    // Find member by user ID and active status
    Optional<Member> findByUserIdAndActive(String userId, Boolean active);

    // Find members with unverified email
    @Query("SELECT m FROM Member m WHERE m.email = :email AND m.emailVerified = false")
    Optional<Member> findUnverifiedMemberByEmail(@Param("email") String email);

    // Find members with pending OTP
    @Query("SELECT m FROM Member m WHERE m.email = :email AND m.otpCode IS NOT NULL AND m.otpExpiresAt > CURRENT_TIMESTAMP")
    Optional<Member> findMemberWithValidOtp(@Param("email") String email);
}
