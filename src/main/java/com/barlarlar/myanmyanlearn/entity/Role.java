package com.barlarlar.myanmyanlearn.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@IdClass(RoleId.class)
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @Column(name = "user_id", length = 50)
    private String userId;

    @Id
    @Column(name = "role", length = 50)
    private String role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private Member member;

    public Role(String userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    @Override
    public String toString() {
        return "Role{" +
                "userId='" + userId + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
