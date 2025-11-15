package com.barlarlar.myanmyanlearn.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AssessmentController {

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

        return "assessment";
    }
}
