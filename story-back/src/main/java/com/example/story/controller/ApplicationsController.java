package com.example.story.controller;

import com.example.story.dto.errors.ResponseErrorDto;
import com.example.story.dto.applications.RequestApplicationDto;
import com.example.story.service.ApplicationsService;
import com.example.story.dto.Group;
import com.example.story.service.EventService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('PARTICIPANT')")
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationsController {

    private final ApplicationsService applicationService;
    private final EventService eventService;

    @PostMapping()
    public ResponseEntity<?> post(@RequestParam Long conId, @RequestBody @Validated(Group.Create.class) RequestApplicationDto dto) {
        try {
            if (eventService.getClosed()) return ResponseEntity.status(HttpStatus.GONE).build();
            else return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.create(conId, dto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @GetMapping()
    public ResponseEntity<?> getAll() {
        try {
            return ResponseEntity.ok().body(applicationService.obtainAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @GetMapping("/{appId}")
    public ResponseEntity<?> getByAppId(@PathVariable Long appId) {
        try {
            return ResponseEntity.ok().body(applicationService.obtain(appId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @PutMapping("/{appId}")
    public ResponseEntity<?> putByAppId(@PathVariable Long appId, @RequestBody @Validated(Group.Update.class) RequestApplicationDto dto) {
        try {
            if (eventService.getClosed()) return ResponseEntity.status(HttpStatus.GONE).build();
            else {
                applicationService.update(appId, dto);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @DeleteMapping("/{appId}")
    public ResponseEntity<?> deleteByAppId(@PathVariable Long appId) {
        try {
            if (eventService.getClosed()) return ResponseEntity.status(HttpStatus.GONE).build();
            else {
                applicationService.delete(appId);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }
}
