package com.barlarlar.myanmyanlearn.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class AdminMarkdownEditorController {

    @GetMapping("/admin/markdown-editor")
    public String editor(
            @RequestParam(name = "courseId", required = false) String courseId,
            @RequestParam(name = "chapterId", required = false) String chapterId,
            @RequestParam(name = "subchapterId", required = false) String subchapterId,
            Model model) {
        model.addAttribute("courseId", courseId == null ? "" : courseId);
        model.addAttribute("chapterId", chapterId == null ? "" : chapterId);
        model.addAttribute("subchapterId", subchapterId == null ? "" : subchapterId);
        return "admin-markdown-editor";
    }

    @PostMapping(value = "/admin/markdown-editor/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        Map<String, Object> out = new HashMap<>();
        if (file == null || file.isEmpty()) {
            out.put("ok", false);
            out.put("message", "No file uploaded.");
            return ResponseEntity.badRequest().body(out);
        }
        if (file.getSize() > 5L * 1024L * 1024L) {
            out.put("ok", false);
            out.put("message", "Image must be 5MB or smaller.");
            return ResponseEntity.badRequest().body(out);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            out.put("ok", false);
            out.put("message", "Only image files are allowed.");
            return ResponseEntity.badRequest().body(out);
        }

        String originalName = file.getOriginalFilename();
        String safeName = sanitizeFilename(originalName == null ? "image" : originalName);
        String ext = extensionFromNameOrType(safeName, contentType);
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        String home = System.getProperty("user.home");
        Path dir = Paths.get((home != null ? home : ""), ".myanmyanlearn", "uploads", "markdown-editor");
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(dir)) {
                out.put("ok", false);
                out.put("message", "Invalid upload path.");
                return ResponseEntity.badRequest().body(out);
            }
            file.transferTo(target.toFile());
        } catch (IOException e) {
            out.put("ok", false);
            out.put("message", "Failed to save image.");
            return ResponseEntity.internalServerError().body(out);
        }

        out.put("ok", true);
        out.put("url", "/uploads/markdown-editor/" + filename);
        return ResponseEntity.ok(out);
    }

    private static String sanitizeFilename(String name) {
        String v = name.trim();
        v = v.replace("\\", "_").replace("/", "_");
        v = v.replaceAll("[^A-Za-z0-9._-]", "_");
        if (v.isBlank()) {
            return "image";
        }
        if (v.length() > 80) {
            v = v.substring(v.length() - 80);
        }
        return v;
    }

    private static String extensionFromNameOrType(String filename, String contentType) {
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) {
            ext = filename.substring(dot).toLowerCase();
            if (ext.matches("\\.[a-z0-9]{1,8}")) {
                return ext;
            }
        }
        String ct = contentType == null ? "" : contentType.toLowerCase();
        if (ct.contains("png")) return ".png";
        if (ct.contains("jpeg") || ct.contains("jpg")) return ".jpg";
        if (ct.contains("gif")) return ".gif";
        if (ct.contains("webp")) return ".webp";
        if (ct.contains("svg")) return ".svg";
        return ".png";
    }
}

