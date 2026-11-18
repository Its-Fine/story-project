package com.example.story.dto.applications;

import com.example.story.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RequestApplicationStatusDto {

    @NotNull(message = "The 'status' field cannot be empty")
    private ApplicationStatus status;

    @Size(max = 2000, message = "Rejection reason must not exceed 2000 characters")
    private String rejectionReason;
}
