package com.example.story.controller;

import com.example.story.dto.errors.ResponseErrorDto;
import com.example.story.service.ContestsService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('PARTICIPANT')")
@RequestMapping("/api/v1/contests")
@RequiredArgsConstructor
public class ContestsController {

    private final ContestsService contestsService;

    @GetMapping("/all")
    public ResponseEntity<?> getAll(@RequestParam(required = false) String name) {
        try {
            return ResponseEntity.ok().body(contestsService.obtainAll(name));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @GetMapping()
    public ResponseEntity<?> getAvailable(@RequestParam(required = false) String name) {
        try {
            return ResponseEntity.ok().body(contestsService.obtainAvailable(name));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }
}
