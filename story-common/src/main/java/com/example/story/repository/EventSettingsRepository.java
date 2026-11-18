package com.example.story.repository;

import com.example.story.entity.EventSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSettingsRepository extends JpaRepository<EventSettingsEntity, Short> {
}
