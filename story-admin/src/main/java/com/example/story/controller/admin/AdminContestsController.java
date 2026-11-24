package com.example.story.controller.admin;

import com.example.story.dto.Group;
import com.example.story.dto.contests.RequestContestDto;
import com.example.story.dto.errors.ResponseErrorDto;
import com.example.story.exception.ResourceNotFoundException;
import com.example.story.service.admin.AdminContestsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/contests")
@PreAuthorize("hasAuthority(T(com.example.story.security.Permissions).CONTEST_MANAGE)")
@RequiredArgsConstructor
public class AdminContestsController {

    private final AdminContestsService adminContestsService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(adminContestsService.obtainAll());
    }

    @GetMapping("/{contestId}")
    public ResponseEntity<?> get(@PathVariable Long contestId) {
        try {
            return ResponseEntity.ok(adminContestsService.obtain(contestId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Validated(Group.Create.class) RequestContestDto dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(adminContestsService.create(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @PutMapping("/{contestId}")
    public ResponseEntity<?> update(@PathVariable Long contestId,
                                    @RequestBody @Validated(Group.Create.class) RequestContestDto dto) {
        try {
            return ResponseEntity.ok(adminContestsService.update(contestId, dto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseErrorDto(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @DeleteMapping("/{contestId}")
    public ResponseEntity<?> delete(@PathVariable Long contestId) {
        try {
            adminContestsService.delete(contestId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseErrorDto(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ResponseErrorDto(e.getMessage()));
        }
    }
}
