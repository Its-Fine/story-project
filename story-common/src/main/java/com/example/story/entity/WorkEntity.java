package com.example.story.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "works", uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "application_id"})})
public class WorkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "file_name", nullable = false, length = 1024)
    private String fileName;

    @Column(name = "rate")
    private Integer rate = null;

    @Column(name = "is_rated", nullable = false)
    private Boolean isRated = false;

    @Column(name = "rated_by")
    private String ratedBy;

    @Column(name = "rated_at")
    private LocalDateTime ratedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @Column(name = "broken", nullable = false)
    private Boolean broken = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private ApplicationEntity applicationEntity;
}
