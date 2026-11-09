package com.example.story.dto.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RequestUserPasswordDto {

    @NotBlank(message = "The 'currentPassword' field cannot be empty")
    private String currentPassword;

    @NotBlank(message = "The 'newPassword' field cannot be empty")
    @Size(min = 8, max = 255, message = "The new password must be between 8 and 255 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,255}$",
            message = "The new password must contain at least one number, one lowercase letter and one uppercase letter")
    private String newPassword;
}
