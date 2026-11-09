package com.example.story.dto.contests;

import lombok.Data;

@Data
public class ResponseContestDto {
    private Long conId;
    private String name;
    private String description;
    private Integer startAge;
    private Integer endAge;
}
