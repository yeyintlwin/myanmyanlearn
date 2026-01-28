package com.barlarlar.myanmyanlearn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminCourseEditorController {

    @GetMapping("/admin/courses/{courseId}")
    public String courseEditor(@PathVariable("courseId") String courseId, Model model) {
        model.addAttribute("courseId", courseId == null ? "" : courseId);
        return "admin-course-editor";
    }
}
