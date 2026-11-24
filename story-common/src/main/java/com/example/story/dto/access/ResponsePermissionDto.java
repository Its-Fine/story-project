package com.example.story.dto.access;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponsePermissionDto {
    private String name;
    private String description;
}
