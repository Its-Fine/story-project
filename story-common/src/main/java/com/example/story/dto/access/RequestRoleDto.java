package com.example.story.dto.access;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class RequestRoleDto {

    @NotBlank(message = "Role name is required")
    @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{1,79}", message = "Role name must contain letters, numbers or underscore")
    private String name;

    @NotBlank(message = "Role description is required")
    @Size(max = 255, message = "Role description is too long")
    private String description;

    @NotEmpty(message = "At least one permission is required")
    private Set<@NotBlank String> permissions;
}
