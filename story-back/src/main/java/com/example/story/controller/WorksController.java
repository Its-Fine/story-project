package com.example.story.controller;

import com.example.story.dto.errors.ResponseErrorDto;
import com.example.story.dto.storage.LoadedFile;
import com.example.story.service.EventService;
import com.example.story.service.WorksService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@PreAuthorize("hasRole('PARTICIPANT')")
@RequestMapping("/api/v1/works")
@RequiredArgsConstructor
public class WorksController {

    private final WorksService worksService;
    private final EventService eventService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> post(@RequestParam Long appId, @RequestParam("file") MultipartFile file) {
        try {
            if (eventService.getClosed()) return ResponseEntity.status(HttpStatus.GONE).build();
            else return ResponseEntity.status(HttpStatus.CREATED).body(worksService.create(appId, file));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @GetMapping()
    public ResponseEntity<?> getAllByAppId(@RequestParam Long appId) {
        try {
            return ResponseEntity.ok().body(worksService.obtainAllInfo(appId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @GetMapping("/{workId}")
    public ResponseEntity<?> getByWorkId(@PathVariable Long workId) {
        try {
            return ResponseEntity.ok().body(worksService.obtainInfo(workId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @GetMapping("/{workId}/file")
    public ResponseEntity<?> getFileByWorkId(@PathVariable Long workId) {
        try {
            LoadedFile file = worksService.obtainData(workId);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.getContentType())).body(file.getFileData());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @DeleteMapping("/{workId}")
    public ResponseEntity<?> deleteByWorkId(@PathVariable Long workId) {
        try {
            if (eventService.getClosed()) return ResponseEntity.status(HttpStatus.GONE).build();
            else {
                worksService.delete(workId);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }
}
