package com.barlarlar.myanmyanlearn.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.barlarlar.myanmyanlearn.service.AdminCourseDbService;

@RestController
@RequestMapping("/api/admin/courses")
public class AdminCoursesApiController {
    private final AdminCourseDbService db;

    public AdminCoursesApiController(AdminCourseDbService db) {
        this.db = db;
    }

    @GetMapping
    public List<AdminCourseDbService.CourseSummary> listCourses() {
        return db.listCourses();
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

    @PutMapping("/{courseId}/editor")
    public ResponseEntity<Void> saveCourseEditor(
            @PathVariable String courseId,
            @RequestBody AdminCourseDbService.CourseEditor editor) {
        if (editor == null || editor.id() == null || editor.id().isBlank() || !courseId.equals(editor.id())) {
            return ResponseEntity.badRequest().build();
        }
        db.saveCourseEditor(editor);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String courseId) {
        db.deleteCourse(courseId);
        return ResponseEntity.ok().build();
    }
}

