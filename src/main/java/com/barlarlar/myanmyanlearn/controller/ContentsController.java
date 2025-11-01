package com.barlarlar.myanmyanlearn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;

import com.barlarlar.myanmyanlearn.model.Course;
import com.barlarlar.myanmyanlearn.service.CourseService;

@Controller
public class ContentsController {
    private final CourseService courseService;

    public ContentsController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/contents")
    public String contents(
        Model model,
        @RequestParam(name = "courseId", required = false) String courseId,
        @RequestParam(name = "courseTitle", required = false) String courseTitle,
        @RequestParam(name = "title", required = false) String title
    ) {
        String selected = (courseTitle != null && !courseTitle.isBlank()) ? courseTitle : (title != null ? title : null);

        Course course = null;
        if (courseId != null && !courseId.isBlank()) {
            course = courseService.findById(courseId);
        }
        if (course == null && selected != null && !selected.isBlank()) {
            course = courseService.findByTitle(selected);
        }

        // Prefer the title from the resolved course for display
        String displayTitle = course != null ? course.getTitle() : selected;
        model.addAttribute("courseTitle", displayTitle);
        if (course != null) {
            model.addAttribute("course", course);
        }
        model.addAttribute("course", course);
        return "contents";
    }
}
