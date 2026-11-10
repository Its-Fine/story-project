package com.example.story.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {

    @NotBlank(message = "The 'email' field cannot be empty")
    @Email(message = "Incorrect email format")
    private String email;

    @NotBlank(message = "The 'password' field cannot be empty")
    private String password;
}