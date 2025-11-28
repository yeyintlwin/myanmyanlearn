package com.barlarlar.myanmyanlearn.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.barlarlar.myanmyanlearn.model.Course;
import com.barlarlar.myanmyanlearn.service.CourseService;

@Controller
public class AssessmentController {
    private final CourseService courseService;

    public AssessmentController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/assessment")
    public String assessmentPage(
            Model model,
            @RequestParam(name = "chapters", required = false) String chapters,
            @RequestParam(name = "courseId", required = false) String courseId) {
        List<String> chapterList;
        if (chapters == null || chapters.isBlank()) {
            chapterList = Collections.emptyList();
        } else {
            chapterList = Arrays.stream(chapters.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
        }

        model.addAttribute("selectedChapters", chapterList);
        model.addAttribute("chapterCount", chapterList.size());
        model.addAttribute("rawChaptersParam", chapters == null ? "" : chapters);
        model.addAttribute("courseId", courseId);

        // Resolve exam title from courseId if available
        String examTitle = null;
        if (courseId != null && !courseId.isBlank()) {
            Course course = courseService.findById(courseId);
            if (course != null) {
                examTitle = course.getTitle();
            }
        }
        model.addAttribute("examTitle", examTitle != null ? examTitle : "Assessment");
        // Provide sample questions for the assessment UI (for testing/demo)

        return "assessment";
    }
}
