package com.example.story.service.admin;

import com.example.story.configuration.StoryProperties;
import com.example.story.dto.storage.LoadedFile;
import com.example.story.dto.works.ResponseWorkAdminDto;
import com.example.story.entity.ApplicationEntity;
import com.example.story.entity.UserEntity;
import com.example.story.entity.WorkEntity;
import com.example.story.mapper.WorkMapper;
import com.example.story.repository.ApplicationRepository;
import com.example.story.repository.UserRepository;
import com.example.story.repository.WorkRepository;
import com.example.story.service.storage.StorageService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.example.story.utilities.Helpers.getCurrentUser;

@Service
@RequiredArgsConstructor
public class AdminWorksService {

    private final WorkRepository workRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final WorkMapper workMapper;
    private final StorageService storageService;

    public List<ResponseWorkAdminDto> obtainAllInfo(Long appId) {
        UserEntity admin = getCurrentUser(userRepository);
        if (!admin.getRole().equals(StoryProperties.Roles.ADMIN.toString())) throw new RuntimeException("You are not an administrator");

        ApplicationEntity application = applicationRepository.findById(appId).orElseThrow(() -> new RuntimeException("Application not found"));
        List<ResponseWorkAdminDto> result = new ArrayList<>();
        for (WorkEntity work : application.getWorkEntities()) {
            result.add(workMapper.entityToWorkAdminDto(work));
        }
        return result;
    }

    public ResponseWorkAdminDto obtainInfo(Long workId) {
        UserEntity admin = getCurrentUser(userRepository);
        if (!admin.getRole().equals(StoryProperties.Roles.ADMIN.toString())) throw new RuntimeException("You are not an administrator");

        WorkEntity work = workRepository.findById(workId).orElseThrow(() -> new RuntimeException("Work not found"));
        return workMapper.entityToWorkAdminDto(work);
    }

    public LoadedFile obtainData(Long workId) throws IOException {
        UserEntity admin = getCurrentUser(userRepository);
        if (!admin.getRole().equals(StoryProperties.Roles.ADMIN.toString())) throw new RuntimeException("You are not an administrator");

        WorkEntity work = workRepository.findById(workId).orElseThrow(() -> new RuntimeException("Work not found"));
        if (work.getBroken()) throw new IOException("Work file is broken");

        try {
            return storageService.loadFile(work.getFileName());
        } catch (IOException e) {
            work.setBroken(true);
            workRepository.save(work);
            throw new IOException("Work file is broken");
        }
    }
}
