package com.example.story.service.storage;

import com.example.story.dto.storage.LoadedFile;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {
    void saveFile(String fileName, MultipartFile file) throws IOException;

    LoadedFile loadFile(String fileName) throws IOException;

    void deleteFile(String fileName) throws IOException;
}
