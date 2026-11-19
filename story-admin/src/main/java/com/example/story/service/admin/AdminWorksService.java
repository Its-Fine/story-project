package com.example.story.service.admin;

import com.example.story.dto.storage.LoadedFile;
import com.example.story.dto.works.ResponseWorkAdminDto;
import com.example.story.dto.works.RequestWorkRatingDto;
import com.example.story.entity.ApplicationEntity;
import com.example.story.entity.UserEntity;
import com.example.story.entity.WorkEntity;
import com.example.story.exception.ResourceNotFoundException;
import com.example.story.mapper.WorkMapper;
import com.example.story.repository.ApplicationRepository;
import com.example.story.repository.UserRepository;
import com.example.story.repository.WorkRepository;
import com.example.story.service.storage.StorageService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

import static com.example.story.utilities.Helpers.getCurrentUser;
import static com.example.story.security.AuthorizationRules.requireAuthority;
import static com.example.story.security.Permissions.WORK_RATE;
import static com.example.story.security.Permissions.WORK_READ_ANY;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminWorksService {

    private final WorkRepository workRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final WorkMapper workMapper;
    private final StorageService storageService;

    public List<ResponseWorkAdminDto> obtainAllInfo(Long appId) {
        requireAuthority(WORK_READ_ANY);

        ApplicationEntity application = applicationRepository.findById(appId).orElseThrow(() -> new RuntimeException("Application not found"));
        List<ResponseWorkAdminDto> result = new ArrayList<>();
        for (WorkEntity work : application.getWorkEntities()) {
            result.add(workMapper.entityToWorkAdminDto(work));
        }
        return result;
    }

    public ResponseWorkAdminDto obtainInfo(Long workId) {
        requireAuthority(WORK_READ_ANY);

        WorkEntity work = workRepository.findById(workId).orElseThrow(() -> new RuntimeException("Work not found"));
        return workMapper.entityToWorkAdminDto(work);
    }

    public LoadedFile obtainData(Long workId) throws IOException {
        requireAuthority(WORK_READ_ANY);

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

    public ResponseWorkAdminDto updateRating(Long workId, RequestWorkRatingDto dto) {
        UserEntity admin = requireAdmin();
        WorkEntity work = workRepository.findById(workId)
                .orElseThrow(() -> new ResourceNotFoundException("Work not found"));
        if (work.getBroken()) {
            throw new IllegalStateException("Broken work cannot be rated");
        }

        work.setRate(dto.getRate());
        work.setIsRated(true);
        work.setRatedBy(admin.getEmail());
        work.setRatedAt(LocalDateTime.now());
        return workMapper.entityToWorkAdminDto(workRepository.saveAndFlush(work));
    }

    public ResponseWorkAdminDto clearRating(Long workId) {
        UserEntity admin = requireAdmin();
        WorkEntity work = workRepository.findById(workId)
                .orElseThrow(() -> new ResourceNotFoundException("Work not found"));
        work.setRate(null);
        work.setIsRated(false);
        work.setRatedBy(admin.getEmail());
        work.setRatedAt(LocalDateTime.now());
        return workMapper.entityToWorkAdminDto(workRepository.saveAndFlush(work));
    }

    private UserEntity requireAdmin() {
        UserEntity admin = getCurrentUser(userRepository);
        requireAuthority(WORK_RATE);
        return admin;
    }
}
