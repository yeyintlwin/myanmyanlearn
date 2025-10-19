package com.barlarlar.myanmyanlearn.repository;

import com.barlarlar.myanmyanlearn.entity.Role;
import com.barlarlar.myanmyanlearn.entity.RoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, RoleId> {

    // Find all roles for a user
    List<Role> findByUserId(String userId);

    // Find all roles for a user with specific role
    List<Role> findByUserIdAndRole(String userId, String role);

    // Check if user has specific role
    boolean existsByUserIdAndRole(String userId, String role);

    // Delete all roles for a user
    void deleteByUserId(String userId);

    // Find users with specific role
    @Query("SELECT r.userId FROM Role r WHERE r.role = :role")
    List<String> findUserIdsByRole(@Param("role") String role);

    // Count users with specific role
    @Query("SELECT COUNT(r) FROM Role r WHERE r.role = :role")
    long countByRole(@Param("role") String role);
}
