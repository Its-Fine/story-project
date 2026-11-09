package com.example.story.controller;

import com.example.story.dto.errors.ResponseErrorDto;
import com.example.story.dto.users.RequestUserDataDto;
import com.example.story.dto.users.RequestUserPasswordDto;
import com.example.story.service.EventService;
import com.example.story.service.UsersService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final UsersService usersService;
    private final EventService eventService;

    @GetMapping("/data")
    public ResponseEntity<?> getUserData() {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(usersService.obtainUserData());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @PutMapping("/data")
    public ResponseEntity<?> putUserData(@RequestBody @Validated RequestUserDataDto dto) {
        try {
            if (eventService.getClosed()) return ResponseEntity.status(HttpStatus.GONE).build();
            else {
                usersService.updateUserData(dto);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> putUserPassword(@RequestBody @Validated RequestUserPasswordDto dto) {
        try {
            usersService.updateUserPassword(dto);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseErrorDto(e.getMessage()));
        }
    }
}
