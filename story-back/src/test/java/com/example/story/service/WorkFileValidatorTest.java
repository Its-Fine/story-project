package com.example.story.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkFileValidatorTest {

    private final WorkFileValidator validator = new WorkFileValidator();

    @Test
    void validPngIsDetectedFromContentAndPathIsRemoved() throws Exception {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "../folder/design.png", "image/png", png);

        WorkFileValidator.ValidatedFile result = validator.validate(file, List.of("image/png"));

        assertThat(result.safeName()).isEqualTo("design.png");
        assertThat(result.contentType()).isEqualTo("image/png");
    }

    @Test
    void declaredImageWithUnknownContentIsRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", "not-an-image".getBytes());

        assertThatThrownBy(() -> validator.validate(file, List.of("image/png")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("определить формат");
    }

    @Test
    void mismatchedExtensionIsRejected() {
        byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/jpeg", jpeg);

        assertThatThrownBy(() -> validator.validate(file, List.of("image/jpeg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Расширение");
    }
}
