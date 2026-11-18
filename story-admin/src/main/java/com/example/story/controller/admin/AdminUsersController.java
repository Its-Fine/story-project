package com.example.story.controller.admin;

import com.example.story.dto.errors.ResponseErrorDto;
import com.example.story.dto.users.RequestUserAccessDto;
import com.example.story.dto.users.RequestUserPasswordDto;
import com.example.story.service.admin.AdminUsersService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUsersController {

    private final AdminUsersService adminUsersService;

    @GetMapping()
    public ResponseEntity<?> getAllByEmail(@RequestParam(required = false) String email,
                                         @RequestParam(required = false) Long count, @RequestParam(required = false) Long page)
    {
        try {
            return ResponseEntity.ok(adminUsersService.obtainAllUserData(email, count, page));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @GetMapping("/count")
    public ResponseEntity<?> getCountByEmail(@RequestParam(required = false) String email)
    {
        try {
            return ResponseEntity.ok(adminUsersService.obtainCountUserData(email));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getByUserId(@PathVariable Long userId)
    {
        try {
            return ResponseEntity.ok(adminUsersService.obtainUserData(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<?> putPasswordByUserId(@PathVariable Long userId, @RequestBody @Validated RequestUserPasswordDto dto)
    {
        try {
            adminUsersService.updateUserPassword(userId, dto);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @PutMapping("/{userId}/access")
    public ResponseEntity<?> putAccessByUserId(@PathVariable Long userId, @RequestBody @Validated RequestUserAccessDto dto)
    {
        try {
            adminUsersService.updateUserAccess(userId, dto);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }
}
