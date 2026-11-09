package com.example.story.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SignupRequestDto {

    @NotBlank(message = "The 'email' field cannot be empty")
    @Email(message = "Incorrect email format")
    private String email;

    @NotBlank(message = "The 'password' field cannot be empty")
    @Size(min = 8, max = 255, message = "The password must be between 8 and 255 characters")
    private String password;

    @NotBlank(message = "The 'firstName' field cannot be empty")
    private String firstName;

    @NotBlank(message = "The 'lastName' field cannot be empty")
    private String lastName;

    @NotNull(message = "The 'birthDate' field cannot be empty")
    private LocalDate birthDate;

    private Boolean consent;
    private Boolean parentConsent;
}
package com.example.story.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SignupRequestDto {

    @NotBlank(message = "The 'email' field cannot be empty")
    @Email(message = "Incorrect email format")
    private String email;

    @NotBlank(message = "The 'password' field cannot be empty")
    @Size(min = 8, max = 255, message = "The password must be between 8 and 255 characters")
    private String password;

    @NotBlank(message = "The 'firstName' field cannot be empty")
    private String firstName;

    @NotBlank(message = "The 'lastName' field cannot be empty")
    private String lastName;

    @NotNull(message = "The 'birthDate' field cannot be empty")
    private LocalDate birthDate;

    private Boolean consent;
    private Boolean parentConsent;
}
