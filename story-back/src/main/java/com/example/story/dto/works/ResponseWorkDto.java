package com.example.story.dto.works;

import lombok.Data;

@Data
public class ResponseWorkDto {
    private Long workId;
    private String name;
    private Integer fileSize;
    private Boolean broken;
}
