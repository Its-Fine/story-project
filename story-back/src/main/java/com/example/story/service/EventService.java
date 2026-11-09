package com.example.story.service;

import com.example.story.configuration.StoryProperties;
import com.example.story.dto.event.RequestEventDto;
import com.example.story.dto.event.ResponseEventDto;

import com.example.story.entity.UserEntity;
import com.example.story.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.example.story.utilities.Helpers.getCurrentUser;

@Service
@RequiredArgsConstructor
public class EventService {

    private final StoryProperties storyProperties;
    private final UserRepository userRepository;

    // Administrator

    public ResponseEventDto setStatus(RequestEventDto dto) {
        storyProperties.setClosed(dto.getClose());
        ResponseEventDto result = new ResponseEventDto();
        result.setClose(storyProperties.getClosed());
        return result;
    }

    // Public

    public ResponseEventDto getStatus() {
        ResponseEventDto result = new ResponseEventDto();
        result.setClose(this.getClosed());
        return result;
    }

    // --

    public Boolean getClosed() {
        UserEntity user = getCurrentUser(userRepository);
        return !user.getClosedAccess() && storyProperties.getClosed();
    }
}
