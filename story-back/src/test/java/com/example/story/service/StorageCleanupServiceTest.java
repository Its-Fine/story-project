package com.example.story.service;

import com.example.story.service.storage.StorageService;
import com.example.story.storage.StorageCleanupTaskEntity;
import com.example.story.storage.StorageCleanupTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageCleanupServiceTest {

    @Mock StorageCleanupTaskRepository cleanupTasks;
    @Mock StorageService storageService;
    @InjectMocks StorageCleanupService service;

    @Test
    void schedulePersistsAUniqueCleanupRequest() {
        when(cleanupTasks.existsByObjectName("user-1/app-2/work.png")).thenReturn(false);

        service.schedule("user-1/app-2/work.png");

        ArgumentCaptor<StorageCleanupTaskEntity> task = ArgumentCaptor.forClass(StorageCleanupTaskEntity.class);
        verify(cleanupTasks).save(task.capture());
        assertThat(task.getValue().getObjectName()).isEqualTo("user-1/app-2/work.png");
        assertThat(task.getValue().getAttempts()).isZero();
        assertThat(task.getValue().getNextAttemptAt()).isNotNull();
    }

    @Test
    void scheduleDoesNotDuplicateAnExistingRequest() {
        when(cleanupTasks.existsByObjectName("work.png")).thenReturn(true);

        service.schedule("work.png");

        verify(cleanupTasks, never()).save(any());
    }

    @Test
    void successfulCleanupRemovesTheDurableRequest() throws Exception {
        StorageCleanupTaskEntity task = task("work.png");
        when(cleanupTasks.findTop50ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(task));

        service.processPendingTasks();

        verify(storageService).deleteFile("work.png");
        verify(cleanupTasks).delete(task);
    }

    @Test
    void failedCleanupIsRetriedLater() throws Exception {
        StorageCleanupTaskEntity task = task("work.png");
        when(cleanupTasks.findTop50ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(task));
        doThrow(new IOException("MinIO unavailable")).when(storageService).deleteFile("work.png");

        LocalDateTime before = LocalDateTime.now();
        service.processPendingTasks();

        assertThat(task.getAttempts()).isEqualTo(1);
        assertThat(task.getLastError()).isEqualTo("MinIO unavailable");
        assertThat(task.getNextAttemptAt()).isAfter(before);
        verify(cleanupTasks).save(task);
        verify(cleanupTasks, never()).delete(task);
    }

    private StorageCleanupTaskEntity task(String objectName) {
        StorageCleanupTaskEntity task = new StorageCleanupTaskEntity();
        task.setId(1L);
        task.setObjectName(objectName);
        task.setNextAttemptAt(LocalDateTime.now());
        return task;
    }
}
