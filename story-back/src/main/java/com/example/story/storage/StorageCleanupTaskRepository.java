package com.example.story.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StorageCleanupTaskRepository extends JpaRepository<StorageCleanupTaskEntity, Long> {

    boolean existsByObjectName(String objectName);

    List<StorageCleanupTaskEntity> findTop50ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            LocalDateTime nextAttemptAt);
}
