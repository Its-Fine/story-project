package com.example.story.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "contests")
public class ContestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "start_age", nullable = false)
    private Integer startAge;

    @Column(name = "end_age", nullable = false)
    private Integer endAge;

    @OneToMany(mappedBy = "contestEntity", fetch = FetchType.LAZY)
    private List<ApplicationEntity> applicationEntities;
}
