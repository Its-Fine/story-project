package com.example.story.dto.auth;

import com.example.story.dto.Group;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RequestAuthDto {

    @NotBlank(message = "The 'email' field cannot be empty")
    @Email(message = "Incorrect email format")
    private String email;

    @NotBlank(message = "The 'password' field cannot be empty")
    @Size(groups = Group.Create.class, min = 8, max = 255, message = "The password must be between 8 and 255 characters")
    @Pattern(groups = Group.Create.class, regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,255}$",
            message = "The password must contain at least one number, one lowercase letter and one uppercase letter")
    private String password;

    @NotBlank(groups = Group.Create.class, message = "The 'firstName' field cannot be empty")
    private String firstName;

    @NotBlank(groups = Group.Create.class, message = "The 'lastName' field cannot be empty")
    private String lastName;

    @NotNull(groups = Group.Create.class, message = "The 'birthDate' field cannot be empty")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private Boolean consent;
    private Boolean parentConsent;
}
