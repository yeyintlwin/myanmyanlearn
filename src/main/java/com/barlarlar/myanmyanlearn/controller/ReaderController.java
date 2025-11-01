package com.barlarlar.myanmyanlearn.controller;

import com.barlarlar.myanmyanlearn.model.Content;
import com.barlarlar.myanmyanlearn.model.Course;
import com.barlarlar.myanmyanlearn.model.Subcontent;
import com.barlarlar.myanmyanlearn.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

@Controller
public class ReaderController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/reader")
    public String reader(
            Model model,
            @RequestParam(name = "courseId") String courseId,
            @RequestParam(name = "ch") Integer chapterOrder,
            @RequestParam(name = "sc") Integer subOrder
    ) {
        Course course = courseService.findById(courseId);
        if (course == null) {
            model.addAttribute("error", "Course not found");
            return "reader";
        }
        Optional<Content> contentOpt = course.getContents().stream()
                .filter(c -> c.getOrder() == chapterOrder)
                .findFirst();
        if (contentOpt.isEmpty()) {
            model.addAttribute("course", course);
            model.addAttribute("error", "Chapter not found");
            return "reader";
        }
        Content content = contentOpt.get();
        Optional<Subcontent> subOpt = content.getSubcontents().stream()
                .filter(s -> s.getOrder() == subOrder)
                .findFirst();
        if (subOpt.isEmpty()) {
            model.addAttribute("course", course);
            model.addAttribute("content", content);
            model.addAttribute("error", "Section not found");
            return "reader";
        }
        Subcontent sub = subOpt.get();

        model.addAttribute("course", course);
        model.addAttribute("content", content);
        model.addAttribute("sub", sub);
        model.addAttribute("markdownPath", sub.getMarkdownPath());
        return "reader";
    }

    @GetMapping(value = "/reader/md", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> markdown(
            @RequestParam(name = "courseId") String courseId,
            @RequestParam(name = "ch") Integer chapterOrder,
            @RequestParam(name = "sc") Integer subOrder
    ) {
        Course course = courseService.findById(courseId);
        if (course == null) {
            return ResponseEntity.notFound().build();
        }
        Optional<Content> contentOpt = course.getContents().stream()
                .filter(c -> c.getOrder() == chapterOrder)
                .findFirst();
        if (contentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Content content = contentOpt.get();
        Optional<Subcontent> subOpt = content.getSubcontents().stream()
                .filter(s -> s.getOrder() == subOrder)
                .findFirst();
        if (subOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String path = subOpt.get().getMarkdownPath();
        try {
            ClassPathResource res = new ClassPathResource(path.startsWith("/") ? path.substring(1) : path);
            byte[] bytes = res.getInputStream().readAllBytes();
            return ResponseEntity.ok(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
