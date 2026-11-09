package com.example.story.service;

import com.example.story.dto.contests.ResponseContestDto;
import com.example.story.entity.ContestEntity;
import com.example.story.entity.UserEntity;
import com.example.story.mapper.ContestMapper;
import com.example.story.repository.ContestRepository;
import com.example.story.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.example.story.utilities.Helpers.getCurrentUser;
import static com.example.story.utilities.Helpers.validateAge;

@Service
@Transactional
@RequiredArgsConstructor
public class ContestsService {

    private final ContestRepository contestRepository;
    private final UserRepository userRepository;
    private final ContestMapper contestMapper;

    public List<ResponseContestDto> obtainAll(String name) {
        List<ContestEntity> contests = (name == null) ? contestRepository.findAll() : contestRepository.findAlikeByName(name);

        List<ResponseContestDto> result = new ArrayList<>();
        for (ContestEntity entity : contests) result.add(contestMapper.entityToContestDto(entity));
        return result;
    }

    public List<ResponseContestDto> obtainAvailable(String name) {
        UserEntity user = getCurrentUser(userRepository);
        List<ContestEntity> contests = (name == null) ? contestRepository.findAll() : contestRepository.findAlikeByName(name);

        List<ResponseContestDto> result = new ArrayList<>();
        for (ContestEntity entity : contests) {
            if (validateAge(user.getBirthDate(), entity.getStartAge(), entity.getEndAge())) {
                result.add(contestMapper.entityToContestDto(entity));
            }
        }
        return result;
    }
}
