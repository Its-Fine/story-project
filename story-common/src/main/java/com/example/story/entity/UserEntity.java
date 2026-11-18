package com.example.story.entity;

import com.example.story.configuration.StoryProperties;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "role", length = 40, nullable = false)
    private String role = StoryProperties.Roles.PARTICIPANT.toString();

    @Column(name = "closed_access", nullable = false)
    private Boolean closedAccess = false;

    @Column(name = "banned", nullable = false)
    private Boolean banned = false;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "consent", nullable = false)
    private Boolean consent;

    @Column(name = "parent_consent", nullable = false)
    private Boolean parentConsent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "userEntity", fetch = FetchType.LAZY)
    private List<ApplicationEntity> applicationEntities;
}
