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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    public ResponseWorkIdDto create(Long appId, MultipartFile file) throws IOException {
        ApplicationEntity application = getApplicationAndCheckAccessAndConditions(appId, file);

        String fileName = "user-" + application.getUserEntity().getId() + "/app-" + appId + "/" + file.getOriginalFilename();

        WorkEntity work = new WorkEntity();
        work.setName(file.getOriginalFilename());
        work.setFileName(fileName);
        work.setFileSize((int) file.getSize());
        work.setApplicationEntity(application);
        WorkEntity newWork = workRepository.save(work);

        storageService.saveFile(fileName, file);

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
        if (file.getOriginalFilename().length() > storyProperties.getWork().getNameSize()) {
            throw new RuntimeException("File name length bigger than " + storyProperties.getWork().getNameSize() + " characters");
        }
        if (workRepository.findByNameAndApplicationId(file.getOriginalFilename(), appId).isPresent()) {
            throw new RuntimeException("Work with this name is already exist");
        }

        String contentType = file.getContentType();
        if (contentType == null || !Set.of("image/png", "image/jpeg", "image/jpg", "image/tiff").contains(contentType)) {
            throw new RuntimeException("Invalid file format. Only PNG, JPEG, JPG, TIFF are allowed");
        }
        return app;
    }
}
