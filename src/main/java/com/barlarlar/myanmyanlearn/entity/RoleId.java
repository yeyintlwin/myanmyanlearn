package com.barlarlar.myanmyanlearn.entity;

import java.io.Serializable;
import java.util.Objects;

public class RoleId implements Serializable {

    private String userId;
    private String role;

    // Constructors
    public RoleId() {
    }

    public RoleId(String userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RoleId roleId = (RoleId) o;
        return Objects.equals(userId, roleId.userId) && Objects.equals(role, roleId.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, role);
    }

    @Override
    public String toString() {
        return "RoleId{" +
                "userId='" + userId + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
