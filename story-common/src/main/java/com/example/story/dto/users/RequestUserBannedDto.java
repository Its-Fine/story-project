package com.example.story.dto.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RequestUserBannedDto {

    @NotBlank(message = "The 'currentPassword' field cannot be empty")
    private String currentPassword;

    @NotNull(message = "The 'banned' field cannot be null")
    private Boolean banned;
}
