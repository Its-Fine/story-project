package com.example.story.dto.applications;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ResponseApplicationAdminDto {
    private Long appId;
    private String name;
    private String description;
    private String city;
    private String organization;
    private String mentorName;
    private String contestName;
    private Integer worksCount;
    private Boolean isTeam;
    private Boolean isCurator;
    private List<ResponseApplicationMemberDto> members;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
