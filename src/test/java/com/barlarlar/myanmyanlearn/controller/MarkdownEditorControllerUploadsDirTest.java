package com.barlarlar.myanmyanlearn.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.barlarlar.myanmyanlearn.service.storage.LocalStorageService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

public class MarkdownEditorControllerUploadsDirTest {
    @Test
    void localStorageSavesUnderConfiguredBaseDir(@TempDir Path tempDir) throws Exception {
        LocalStorageService storage = new LocalStorageService(tempDir.toString(), "/uploads");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.png",
                "image/png",
                new byte[] { 1, 2, 3, 4 });

        String key = "courses/course_1/images/demo.png";
        var stored = storage.put(key, file);

        assertEquals("/uploads/" + key, stored.url());
        assertTrue(Files.isRegularFile(tempDir.resolve(key)));
    }
}
