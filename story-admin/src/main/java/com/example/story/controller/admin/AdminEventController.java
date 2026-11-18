package com.example.story.controller.admin;

import com.example.story.dto.errors.ResponseErrorDto;
import com.example.story.dto.event.RequestEventDto;
import com.example.story.service.EventService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/event")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;

    @PutMapping("/status")
    public ResponseEntity<?> putStatus(@RequestBody @Validated RequestEventDto dto) {
        try {
            return ResponseEntity.ok().body(eventService.setStatus(dto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }
}
