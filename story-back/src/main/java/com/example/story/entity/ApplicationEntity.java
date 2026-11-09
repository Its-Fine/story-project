package com.example.story.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "applications")
public class ApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "city", columnDefinition = "TEXT", nullable = false)
    private String city;

    @Column(name = "organization", columnDefinition = "TEXT")
    private String organization = null;

    @Column(name = "mentor")
    private String mentor = null;

    @Column(name = "is_team", nullable = false)
    private Boolean isTeam;

    @Column(name = "is_curator", nullable = false)
    private Boolean isCurator;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    private ContestEntity contestEntity;

    @OneToMany(mappedBy = "applicationEntity", fetch = FetchType.LAZY)
    private List<WorkEntity> workEntities;

    @OneToMany(mappedBy = "applicationEntity", fetch = FetchType.LAZY)
    private List<MemberEntity> memberEntities;
}
