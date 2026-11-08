package com.example.user_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @Column(length = 20)
    private String username;

    @Column(length = 128, nullable = false)
    private String fullname;

    @Column(length = 128, nullable = false, unique = true)
    private String email;

    @Column(length = 15)
    private String phone;

    private boolean enabled = true;

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean deleted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id")
    private University university;
}