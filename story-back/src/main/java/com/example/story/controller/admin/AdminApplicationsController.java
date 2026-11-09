package com.example.story.controller.admin;

import com.example.story.dto.errors.ResponseErrorDto;
import com.example.story.service.admin.AdminApplicationsService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/applications")
@RequiredArgsConstructor
public class AdminApplicationsController {

    private final AdminApplicationsService adminApplicationService;

    @GetMapping()
    public ResponseEntity<?> getAllBy(@RequestParam(required = false) Long userId, @RequestParam(required = false) Long conId,
                                      @RequestParam(required = false) Long count, @RequestParam(required = false) Long page)
    {
        try {
            return ResponseEntity.ok().body(adminApplicationService.obtainAll(userId, conId, count, page));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @GetMapping("/count")
    public ResponseEntity<?> getCountBy(@RequestParam(required = false) Long userId, @RequestParam(required = false) Long conId)
    {
        try {
            return ResponseEntity.ok().body(adminApplicationService.obtainCount(userId, conId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @GetMapping("/{appId}")
    public ResponseEntity<?> getByAppId(@PathVariable Long appId)
    {
        try {
            return ResponseEntity.ok().body(adminApplicationService.obtainByAppId(appId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }
}
