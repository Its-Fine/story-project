package com.example.story.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("first_name")
    private String firstName;

    @NotBlank(message = "The 'lastName' field cannot be empty")
    @JsonProperty("last_name")
    private String lastName;

    @NotNull(message = "The 'birthDate' field cannot be empty")
    @JsonProperty("birth_date")
    private LocalDate birthDate;

    @JsonProperty("password_again")
    private String passwordAgain;

    private Boolean consent;

    @JsonProperty("parent_consent")
    private Boolean parentConsent;
}