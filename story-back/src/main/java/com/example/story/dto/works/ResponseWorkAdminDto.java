package com.example.story.dto.works;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResponseWorkAdminDto {
    private Long workId;
    private String name;
    private String fileName;
    private Integer fileSize;
    private Boolean broken;
    private Integer rate;
    private Boolean isRated;
    private String ratedBy;
    private LocalDateTime ratedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
