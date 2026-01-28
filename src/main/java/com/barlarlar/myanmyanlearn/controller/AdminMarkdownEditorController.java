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
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.UUID;

@Controller
public class AdminMarkdownEditorController {

    @GetMapping("/admin/markdown-editor")
    public String editor(
            @RequestParam(name = "courseId", required = false) String courseId,
            @RequestParam(name = "chapterId", required = false) String chapterId,
            @RequestParam(name = "kind", required = false) String kind,
            @RequestParam(name = "questionId", required = false) String questionId,
            @RequestParam(name = "subchapterId", required = false) String subchapterId,
            Model model) {
        model.addAttribute("courseId", courseId == null ? "" : courseId);
        model.addAttribute("chapterId", chapterId == null ? "" : chapterId);
        model.addAttribute("kind", kind == null ? "" : kind);
        model.addAttribute("questionId", questionId == null ? "" : questionId);
        model.addAttribute("subchapterId", subchapterId == null ? "" : subchapterId);
        return "admin-markdown-editor";
    }

    @PostMapping(value = "/admin/markdown-editor/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("courseId") String courseId,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> out = new HashMap<>();
        String courseFolder = sanitizePathSegment(courseId == null ? "" : courseId);
        if (courseFolder.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing courseId.");
            return ResponseEntity.badRequest().body(out);
        }
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

        Path dir = uploadsDir(courseFolder);
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(dir)) {
                out.put("ok", false);
                out.put("message", "Invalid upload path.");
                return ResponseEntity.badRequest().body(out);
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            out.put("ok", false);
            out.put("message", "Failed to save image.");
            return ResponseEntity.internalServerError().body(out);
        }

        out.put("ok", true);
        out.put("url", "/uploads/courses/" + courseFolder + "/markdown-editor/" + filename);
        out.put("name", filename);
        return ResponseEntity.ok(out);
    }

    @GetMapping(value = "/admin/markdown-editor/images", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> listImages(@RequestParam("courseId") String courseId) {
        Map<String, Object> out = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        String courseFolder = sanitizePathSegment(courseId == null ? "" : courseId);
        if (courseFolder.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing courseId.");
            return ResponseEntity.badRequest().body(out);
        }
        Path dir = uploadsDir(courseFolder);
        try {
            if (Files.isDirectory(dir)) {
                try (Stream<Path> stream = Files.list(dir)) {
                    stream.filter(Files::isRegularFile)
                            .filter(p -> isImageFile(p.getFileName() == null ? "" : p.getFileName().toString()))
                            .sorted(Comparator.comparingLong(this::safeLastModified).reversed())
                            .forEach(p -> {
                                String name = p.getFileName().toString();
                                Map<String, Object> item = new HashMap<>();
                                item.put("name", name);
                                item.put("url", "/uploads/courses/" + courseFolder + "/markdown-editor/" + name);
                                item.put("sizeBytes", safeSize(p));
                                item.put("lastModifiedEpochMs", safeLastModified(p));
                                items.add(item);
                            });
                }
            }
        } catch (IOException e) {
            out.put("ok", false);
            out.put("message", "Failed to list images.");
            return ResponseEntity.internalServerError().body(out);
        }
        out.put("ok", true);
        out.put("items", items);
        out.put("count", items.size());
        out.put("generatedAt", Instant.now().toString());
        return ResponseEntity.ok(out);
    }

    @PostMapping(value = "/admin/markdown-editor/images/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> deleteImage(
            @RequestParam("courseId") String courseId,
            @RequestParam("name") String name) {
        Map<String, Object> out = new HashMap<>();
        String courseFolder = sanitizePathSegment(courseId == null ? "" : courseId);
        if (courseFolder.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing courseId.");
            return ResponseEntity.badRequest().body(out);
        }
        String safeName = sanitizeFilename(name == null ? "" : name);
        if (safeName.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing image name.");
            return ResponseEntity.badRequest().body(out);
        }
        if (!isImageFile(safeName)) {
            out.put("ok", false);
            out.put("message", "Only image files can be deleted here.");
            return ResponseEntity.badRequest().body(out);
        }
        Path dir = uploadsDir(courseFolder);
        Path target = dir.resolve(safeName).normalize();
        if (!target.startsWith(dir)) {
            out.put("ok", false);
            out.put("message", "Invalid path.");
            return ResponseEntity.badRequest().body(out);
        }
        try {
            boolean existed = Files.deleteIfExists(target);
            out.put("ok", true);
            out.put("deleted", existed);
            return ResponseEntity.ok(out);
        } catch (IOException e) {
            out.put("ok", false);
            out.put("message", "Failed to delete image.");
            return ResponseEntity.internalServerError().body(out);
        }
    }

    private static Path uploadsDir(String courseFolder) {
        String home = System.getProperty("user.home");
        return Paths.get((home != null ? home : ""), ".myanmyanlearn", "uploads", "courses", courseFolder, "markdown-editor");
    }

    private long safeLastModified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private long safeSize(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return 0L;
        }
    }

    private static boolean isImageFile(String name) {
        String v = name == null ? "" : name.toLowerCase();
        return v.endsWith(".png") || v.endsWith(".jpg") || v.endsWith(".jpeg") || v.endsWith(".gif") || v.endsWith(".webp")
                || v.endsWith(".svg");
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

    private static String sanitizePathSegment(String value) {
        String v = (value == null ? "" : value).trim();
        if (v.isBlank()) return "";
        v = v.replaceAll("[^A-Za-z0-9_-]", "_");
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
