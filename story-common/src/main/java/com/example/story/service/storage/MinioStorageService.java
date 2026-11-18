package com.example.story.service.storage;

import com.example.story.dto.storage.LoadedFile;
import com.example.story.configuration.StoryProperties;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final StoryProperties storyProperties;

    @Override
    public void saveFile(String fileName, MultipartFile file) throws IOException {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(storyProperties.getMinio().getBucket())
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public LoadedFile loadFile(String fileName) throws IOException {
        try {
            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(storyProperties.getMinio().getBucket())
                            .object(fileName)
                            .build()
            );

            String contentType = response.headers().get("Content-Type");
            if (contentType == null || contentType.isEmpty()) contentType = "application/octet-stream";

            return LoadedFile.builder().contentType(contentType).fileData(new InputStreamResource(response)).build();
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void deleteFile(String fileName) throws IOException {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(storyProperties.getMinio().getBucket())
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
