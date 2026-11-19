package com.example.story.service;

import com.example.story.dto.event.RequestEventDto;
import com.example.story.dto.event.ResponseEventDto;
import com.example.story.entity.EventSettingsEntity;
import com.example.story.entity.UserEntity;
import com.example.story.repository.EventSettingsRepository;
import com.example.story.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.story.utilities.Helpers.getCurrentUser;
import static com.example.story.security.AuthorizationRules.requireAuthority;
import static com.example.story.security.Permissions.EVENT_MANAGE;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final short EVENT_SETTINGS_ID = 1;

    private final EventSettingsRepository eventSettingsRepository;
    private final UserRepository userRepository;

    // Administrator

    @Transactional
    public ResponseEventDto setStatus(RequestEventDto dto) {
        requireAuthority(EVENT_MANAGE);
        EventSettingsEntity settings = getOrCreateSettings();
        settings.setApplicationsClosed(dto.getClose());
        EventSettingsEntity savedSettings = eventSettingsRepository.save(settings);

        ResponseEventDto result = new ResponseEventDto();
        result.setClose(savedSettings.getApplicationsClosed());
        return result;
    }

    // Public

    @Transactional(readOnly = true)
    public ResponseEventDto getStatus() {
        ResponseEventDto result = new ResponseEventDto();
        result.setClose(isApplicationsClosed());
        return result;
    }

    // --

    @Transactional(readOnly = true)
    public boolean isApplicationsClosed() {
        return eventSettingsRepository.findById(EVENT_SETTINGS_ID)
                .map(EventSettingsEntity::getApplicationsClosed)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Boolean getClosed() {
        UserEntity user = getCurrentUser(userRepository);
        return !user.getClosedAccess() && isApplicationsClosed();
    }

    private EventSettingsEntity getOrCreateSettings() {
        return eventSettingsRepository.findById(EVENT_SETTINGS_ID).orElseGet(() -> {
            EventSettingsEntity settings = new EventSettingsEntity();
            settings.setId(EVENT_SETTINGS_ID);
            settings.setApplicationsClosed(false);
            return settings;
        });
    }
}
