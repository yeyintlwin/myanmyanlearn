package com.barlarlar.myanmyanlearn.controller;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.barlarlar.myanmyanlearn.service.AdminCourseDbService;
import java.io.IOException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/admin/courses")
public class AdminCoursesApiController {
    private final AdminCourseDbService db;

    public AdminCoursesApiController(AdminCourseDbService db) {
        this.db = db;
    }

    @GetMapping("/{courseId}/editor")
    public ResponseEntity<AdminCourseDbService.CourseEditor> loadCourseEditor(@PathVariable String courseId) {
        AdminCourseDbService.CourseEditor editor = db.loadCourseEditor(courseId);
        if (editor == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(editor);
    }

    @PutMapping("/{courseId}/meta")
    public ResponseEntity<Void> saveCourseMeta(
            @PathVariable String courseId,
            @RequestBody AdminCourseDbService.CourseSummary meta) {
        if (meta == null || meta.id() == null || meta.id().isBlank() || !courseId.equals(meta.id())) {
            return ResponseEntity.badRequest().build();
        }
        db.upsertCourseMeta(meta);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{courseId}/cover-image", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> uploadCoverImage(
            @PathVariable String courseId,
            @RequestParam(name = "file") MultipartFile file) {
        try {
            String url = db.uploadCourseCoverImage(courseId, file);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "url", url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Invalid request."));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "ok", false,
                    "message", "Failed to save image."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "ok", false,
                    "message", "Server error."));
        }
    }

    @PutMapping("/{courseId}/editor")
    public ResponseEntity<Map<String, Object>> saveCourseEditor(
            @PathVariable String courseId,
            @RequestBody AdminCourseDbService.CourseEditor editor) {
        if (editor == null || editor.id() == null || editor.id().isBlank() || !courseId.equals(editor.id())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", "Invalid request."));
        }
        try {
            db.saveCourseEditor(editor);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(Map.of(
                    "ok", false,
                    "message", "Save conflict. Try again."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Invalid request."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "ok", false,
                    "message", "Server error."));
        }
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String courseId) {
        db.deleteCourse(courseId);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{courseId}/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> exportCourse(@PathVariable String courseId) {
        String id = courseId != null ? courseId.trim() : "";
        if (id.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        StreamingResponseBody body = outputStream -> db.writeCourseBllArchive(id, outputStream);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename(id + ".bll").build());
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> importCourse(@RequestParam(name = "file") MultipartFile file) {
        try {
            String courseId = db.importCourseBllArchive(file);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "courseId", courseId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Invalid request."));
        } catch (IOException e) {
            String msg = e.getMessage() != null ? e.getMessage().trim() : "";
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", msg.isBlank() ? "Failed to import archive." : msg));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "ok", false,
                    "message", "Server error."));
        }
    }
}
