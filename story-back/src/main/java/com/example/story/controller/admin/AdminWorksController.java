package com.example.story.controller.admin;

import com.example.story.dto.errors.ResponseErrorDto;
import com.example.story.dto.storage.LoadedFile;
import com.example.story.service.admin.AdminWorksService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/works")
@RequiredArgsConstructor
public class AdminWorksController {

    private final AdminWorksService adminWorksService;

    @GetMapping()
    public ResponseEntity<?> getAllByAppId(@RequestParam Long appId) {
        try {
            return ResponseEntity.ok().body(adminWorksService.obtainAllInfo(appId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @GetMapping("/{workId}")
    public ResponseEntity<?> getByWorkId(@PathVariable Long workId) {
        try {
            return ResponseEntity.ok().body(adminWorksService.obtainInfo(workId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @GetMapping("/{workId}/file")
    public ResponseEntity<?> getFileByWorkId(@PathVariable Long workId) {
        try {
            LoadedFile file = adminWorksService.obtainData(workId);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.getContentType())).body(file.getFileData());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }
}
