package com.example.story.service;

import com.example.story.storage.StorageCleanupTaskEntity;
import com.example.story.storage.StorageCleanupTaskRepository;
import com.example.story.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageCleanupService {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final StorageCleanupTaskRepository cleanupTasks;
    private final StorageService storageService;

    /**
     * Must be called inside the same transaction that removes the database record
     * referencing the object. A rollback therefore keeps both the reference and the
     * absence of a cleanup task; a commit makes the cleanup request durable.
     */
    @Transactional
    public void schedule(String objectName) {
        if (objectName == null || objectName.isBlank() || cleanupTasks.existsByObjectName(objectName)) {
            return;
        }
        StorageCleanupTaskEntity task = new StorageCleanupTaskEntity();
        task.setObjectName(objectName);
        task.setNextAttemptAt(LocalDateTime.now());
        cleanupTasks.save(task);
    }

    @Scheduled(fixedDelayString = "${story.storage-cleanup.interval-ms:30000}")
    @Transactional
    public void processPendingTasks() {
        for (StorageCleanupTaskEntity task : cleanupTasks
                .findTop50ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(LocalDateTime.now())) {
            try {
                storageService.deleteFile(task.getObjectName());
                cleanupTasks.delete(task);
            } catch (IOException exception) {
                task.setAttempts(task.getAttempts() + 1);
                task.setLastError(limit(exception.getMessage()));
                task.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSeconds(task.getAttempts())));
                cleanupTasks.save(task);
                log.warn("Storage cleanup failed for object {} (attempt {})",
                        task.getObjectName(), task.getAttempts(), exception);
            }
        }
    }

    private long backoffSeconds(int attempts) {
        int exponent = Math.min(Math.max(attempts - 1, 0), 8);
        return Math.min(30L * (1L << exponent), 3600L);
    }

    private String limit(String message) {
        if (message == null) return "Storage operation failed";
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
