package com.example.story.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "permissions")
public class PermissionEntity {

    @Id
    @Column(name = "name", length = 120)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;
}
