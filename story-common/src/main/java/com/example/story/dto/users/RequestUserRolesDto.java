package com.example.story.dto.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class RequestUserRolesDto {

    @NotBlank(message = "The 'currentPassword' field cannot be empty")
    private String currentPassword;

    @NotEmpty(message = "At least one role is required")
    private Set<@NotBlank String> roles;
}
