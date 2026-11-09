package com.example.story.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "auth_users")
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private java.time.LocalDate birthDate;

    @Column(name = "role", length = 40, nullable = false)
    private String role = "PARTICIPANT";

    @Column(name = "banned", nullable = false)
    private Boolean banned = false;

    @Column(name = "consent", nullable = false)
    private Boolean consent = false;

    @Column(name = "parent_consent", nullable = false)
    private Boolean parentConsent = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
