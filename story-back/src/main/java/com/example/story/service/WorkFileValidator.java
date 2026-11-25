package com.example.story.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Component
public class WorkFileValidator {

    public ValidatedFile validate(MultipartFile file, List<String> configuredTypes) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new IllegalArgumentException("Файл пуст");
        }

        String safeName = safeFileName(file.getOriginalFilename());
        String declaredType = normalizeType(file.getContentType());
        String detectedType = detectType(file);
        List<String> allowedTypes = configuredTypes.stream().map(this::normalizeType).toList();

        if (!allowedTypes.contains(detectedType)) {
            throw new IllegalArgumentException("Фактический формат файла не разрешён");
        }
        if (!detectedType.equals(declaredType)) {
            throw new IllegalArgumentException("Содержимое файла не соответствует заявленному формату");
        }
        if (!extensionMatches(safeName, detectedType)) {
            throw new IllegalArgumentException("Расширение файла не соответствует его содержимому");
        }
        return new ValidatedFile(safeName, detectedType);
    }

    private String safeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("Имя файла отсутствует");
        }
        String normalized = originalName.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_")
                .trim();
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            throw new IllegalArgumentException("Некорректное имя файла");
        }
        return name;
    }

    private String detectType(MultipartFile file) throws IOException {
        byte[] header;
        try (InputStream stream = file.getInputStream()) {
            header = stream.readNBytes(8);
        }
        if (header.length >= 8
                && unsigned(header[0]) == 0x89 && header[1] == 0x50 && header[2] == 0x4E
                && header[3] == 0x47 && header[4] == 0x0D && header[5] == 0x0A
                && header[6] == 0x1A && header[7] == 0x0A) {
            return "image/png";
        }
        if (header.length >= 3
                && unsigned(header[0]) == 0xFF && unsigned(header[1]) == 0xD8 && unsigned(header[2]) == 0xFF) {
            return "image/jpeg";
        }
        if (header.length >= 4) {
            boolean littleEndian = header[0] == 0x49 && header[1] == 0x49 && header[2] == 0x2A && header[3] == 0x00;
            boolean bigEndian = header[0] == 0x4D && header[1] == 0x4D && header[2] == 0x00 && header[3] == 0x2A;
            if (littleEndian || bigEndian) return "image/tiff";
        }
        throw new IllegalArgumentException("Не удалось определить формат файла");
    }

    private boolean extensionMatches(String name, String contentType) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        return switch (contentType) {
            case "image/png" -> lowerName.endsWith(".png");
            case "image/jpeg" -> lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg");
            case "image/tiff" -> lowerName.endsWith(".tif") || lowerName.endsWith(".tiff");
            default -> false;
        };
    }

    private String normalizeType(String contentType) {
        if (contentType == null) return "";
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("image/jpg") ? "image/jpeg" : normalized;
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    public record ValidatedFile(String safeName, String contentType) {
    }
}
