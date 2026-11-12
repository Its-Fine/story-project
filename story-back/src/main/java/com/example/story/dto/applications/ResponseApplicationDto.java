package com.example.story.dto.applications;

import lombok.Data;

import java.util.List;

@Data
public class ResponseApplicationDto {
    private Long appId;
    private String name;
    private String description;
    private String city;
    private String organization;
    private String mentorName;
    private String contestName;
    private Long contestId;
    private Integer worksCount;
    private Boolean isTeam;
    private Boolean isCurator;
    private String status;
    private String rejectionReason;
    private List<String> linksToWorks;
    private List<ResponseApplicationMemberDto> members;
}
