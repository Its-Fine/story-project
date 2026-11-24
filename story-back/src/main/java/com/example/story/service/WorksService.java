package com.example.story.service;

import com.example.story.dto.works.ResponseWorkDto;
import com.example.story.dto.works.ResponseWorkIdDto;
import com.example.story.dto.storage.LoadedFile;
import com.example.story.entity.UserEntity;
import com.example.story.mapper.WorkMapper;
import com.example.story.configuration.StoryProperties;
import com.example.story.entity.ApplicationEntity;
import com.example.story.entity.WorkEntity;
import com.example.story.repository.ApplicationRepository;
import com.example.story.repository.WorkRepository;
import com.example.story.repository.UserRepository;

import com.example.story.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.story.utilities.Helpers.getCurrentUser;

@Service
@Transactional
@RequiredArgsConstructor
public class WorksService {

    private final WorkRepository workRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final WorkMapper workMapper;
    private final StorageService storageService;
    private final StoryProperties storyProperties;
    private final WorkFileValidator workFileValidator;

    public List<ResponseWorkDto> obtainAllInfo(Long appId) {
        ApplicationEntity application = getApplicationAndCheckAccess(appId);

        List<ResponseWorkDto> result = new ArrayList<>();
        for (WorkEntity work : application.getWorkEntities()) {
            result.add(workMapper.entityToWorkDto(work));
        }
        return result;
    }

    public ResponseWorkDto obtainInfo(Long workId) {
        WorkEntity work = getWorkAndCheckAccess(workId);
        return workMapper.entityToWorkDto(work);
    }

    public LoadedFile obtainData(Long workId) throws IOException {
        WorkEntity work = getWorkAndCheckAccess(workId);
        if (work.getBroken()) throw new IOException("Work file is broken");

        try {
            return storageService.loadFile(work.getFileName());
        } catch (IOException e) {
            work.setBroken(true);
            workRepository.save(work);
            throw new IOException("Work file is broken");
        }
    }

    @Transactional(rollbackFor = IOException.class)
    public ResponseWorkIdDto create(Long appId, MultipartFile file) throws IOException {
        ApplicationEntity application = getApplicationAndCheckAccessAndConditions(appId, file);
        WorkFileValidator.ValidatedFile validatedFile = workFileValidator.validate(
                file, storyProperties.getWork().getAllowedContentTypes());
        if (validatedFile.safeName().length() > storyProperties.getWork().getNameSize()) {
            throw new IllegalArgumentException(
                    "Имя файла длиннее " + storyProperties.getWork().getNameSize() + " символов");
        }
        if (workRepository.findByNameAndApplicationId(validatedFile.safeName(), appId).isPresent()) {
            throw new IllegalArgumentException("Файл с таким именем уже добавлен в заявку");
        }

        String fileName = "user-" + application.getUserEntity().getId()
                + "/app-" + appId + "/" + UUID.randomUUID() + "-" + validatedFile.safeName();

        WorkEntity work = new WorkEntity();
        work.setName(validatedFile.safeName());
        work.setFileName(fileName);
        work.setFileSize((int) file.getSize());
        work.setApplicationEntity(application);
        WorkEntity newWork = workRepository.save(work);

        storageService.saveFile(fileName, file, validatedFile.contentType());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        storageService.deleteFile(fileName);
                    } catch (IOException ignored) {
                        // The database rollback remains authoritative; orphan cleanup can be retried operationally.
                    }
                }
            }
        });

        return workMapper.entityToWorkIdDto(newWork);
    }

    public void delete(Long workId) {
        WorkEntity work = getWorkAndCheckAccess(workId);
        try {
            storageService.deleteFile(work.getFileName());
        } catch (IOException e) {
            System.out.println("Error while deleting: " + work.getFileName());
        }
        workRepository.delete(work);
    }

    // --- вспомогательные методы ---

    private ApplicationEntity getApplicationAndCheckAccess(Long appId) {
        ApplicationEntity app = applicationRepository.findById(appId).orElseThrow(() -> new RuntimeException("Application not found"));
        UserEntity user = getCurrentUser(userRepository);
        if (!app.getUserEntity().getId().equals(user.getId())) {
            throw new RuntimeException("Application not found");
        }
        return app;
    }

    private WorkEntity getWorkAndCheckAccess(Long workId) {
        WorkEntity work = workRepository.findById(workId).orElseThrow(() -> new RuntimeException("Work not found"));
        UserEntity user = getCurrentUser(userRepository);
        if (!work.getApplicationEntity().getUserEntity().getId().equals(user.getId())) {
            throw new RuntimeException("Work not found");
        }
        return work;
    }

    private ApplicationEntity getApplicationAndCheckAccessAndConditions(Long appId, MultipartFile file) {
        ApplicationEntity app = applicationRepository.findById(appId).orElseThrow(() -> new RuntimeException("Application not found"));
        UserEntity user = getCurrentUser(userRepository);

        if (!app.getUserEntity().getId().equals(user.getId())) {
            throw new RuntimeException("Application not found");
        }
        if (storyProperties.getApplication().getWorkCount() > -1 &&
                workRepository.countByApplicationId(app.getId()) >= storyProperties.getApplication().getWorkCount()) {
            throw new RuntimeException("User already has max works for this application");
        }
        if (file.getSize() > storyProperties.getWork().getSize().toBytes()) {
            throw new RuntimeException("File size bigger than " + storyProperties.getWork().getSize() + " bytes");
        }
        if (workRepository.sumSizeByApplicationId(appId) + file.getSize() > storyProperties.getApplication().getSize().toBytes()) {
            throw new RuntimeException("Total works size for this applications exceeded");
        }
        return app;
    }
}
