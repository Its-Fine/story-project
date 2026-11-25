package com.example.story.dto.access;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResponseRoleDto {
    private String name;
    private String description;
    private List<String> permissions;
    private Long assignedUsers;
    private Boolean system;
}
