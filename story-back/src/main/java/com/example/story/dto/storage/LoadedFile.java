package com.example.story.dto.storage;

import lombok.Builder;
import lombok.Data;

import org.springframework.core.io.Resource;

@Data
@Builder
public class LoadedFile {
    private String contentType;
    private Resource fileData;
}
