package com.barlarlar.myanmyanlearn.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
        List<com.barlarlar.myanmyanlearn.model.Question> questions =
                com.barlarlar.myanmyanlearn.datasource.QuestionDataSource.getSampleQuestions();

        if (courseId != null && !courseId.isBlank()) {
            questions = questions.stream()
                    .filter(q -> courseId.equals(q.getCourseId()))
                    .collect(Collectors.toList());
        }

        if (!chapterList.isEmpty()) {
            questions = questions.stream()
                    .filter(q -> chapterList.contains(q.getChapterId()))
                    .collect(Collectors.toList());
        }

        Comparator<com.barlarlar.myanmyanlearn.model.Question> comparator =
                Comparator.<com.barlarlar.myanmyanlearn.model.Question>comparingInt(q -> parseChapterOrder(q.getChapterId()))
                        .thenComparingInt(com.barlarlar.myanmyanlearn.model.Question::getQuestionNumber);
        questions.sort(comparator);

        model.addAttribute("questions", questions);

        return "assessment";
    }

    @PostMapping("/assessment/score")
    public String assessmentScorePage(
            Model model,
            @RequestParam(name = "chapters", required = false) String chapters,
            @RequestParam(name = "courseId", required = false) String courseId,
            @RequestParam Map<String, String> allParams) {
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

        String examTitle = null;
        if (courseId != null && !courseId.isBlank()) {
            Course course = courseService.findById(courseId);
            if (course != null) {
                examTitle = course.getTitle();
            }
        }
        model.addAttribute("examTitle", examTitle != null ? examTitle : "Assessment");

        List<com.barlarlar.myanmyanlearn.model.Question> questions =
                com.barlarlar.myanmyanlearn.datasource.QuestionDataSource.getSampleQuestions();

        if (courseId != null && !courseId.isBlank()) {
            questions = questions.stream()
                    .filter(q -> courseId.equals(q.getCourseId()))
                    .collect(Collectors.toList());
        }

        if (!chapterList.isEmpty()) {
            questions = questions.stream()
                    .filter(q -> chapterList.contains(q.getChapterId()))
                    .collect(Collectors.toList());
        }

        Comparator<com.barlarlar.myanmyanlearn.model.Question> comparator =
                Comparator.<com.barlarlar.myanmyanlearn.model.Question>comparingInt(q -> parseChapterOrder(q.getChapterId()))
                        .thenComparingInt(com.barlarlar.myanmyanlearn.model.Question::getQuestionNumber);
        questions.sort(comparator);

        model.addAttribute("questions", questions);

        // Sum total possible marks
        double totalPossible = questions.stream()
                .mapToDouble(q -> {
                    try { return q.getMarks(); } catch (Exception e) { return 0.0; }
                })
                .sum();
        model.addAttribute("totalPossible", totalPossible);

        // Build selection map for keys like q{index}-s{slot}
        java.util.Map<String, Integer> selectionMap = allParams.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().matches("q\\d+-s\\d+"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            try { return Integer.parseInt(e.getValue()); } catch (NumberFormatException ex) { return -1; }
                        }
                ));

        // Build scored view model aligned with sorted questions
        java.util.List<java.util.Map<String, Object>> scoredQuestions = new java.util.ArrayList<>();
        int fullyCorrectCount = 0;
        for (int i = 0; i < questions.size(); i++) {
            com.barlarlar.myanmyanlearn.model.Question q = questions.get(i);
            int idx = i + 1;
            java.util.List<java.util.Map<String, Object>> slotViews = new java.util.ArrayList<>();
            boolean allSlotsCorrect = true;
            int slotCount = q.getSlotCount();
            if (slotCount <= 0) slotCount = 1;
            for (int slot = 1; slot <= slotCount; slot++) {
                java.util.List<com.barlarlar.myanmyanlearn.model.QuestionOption> opts =
                        (q.getSlotOptions() != null && q.getSlotOptions().size() >= slot)
                                ? q.getSlotOptions().get(slot - 1)
                                : q.getOptions();
                int selectedIdx = -1;
                String key = "q" + idx + "-s" + slot;
                if (selectionMap.containsKey(key)) {
                    selectedIdx = selectionMap.get(key);
                }
                String selectedText = null;
                Integer correctIdx = null;
                String correctText = null;
                if (opts != null) {
                    for (com.barlarlar.myanmyanlearn.model.QuestionOption opt : opts) {
                        if (opt.getOptionIndex() == selectedIdx) {
                            selectedText = opt.getOptionContent();
                        }
                        if (opt.getIsCorrect()) {
                            correctIdx = opt.getOptionIndex();
                            correctText = opt.getOptionContent();
                        }
                    }
                }
                boolean isCorrect = (correctIdx != null) && (selectedIdx == correctIdx);
                if (!isCorrect) {
                    allSlotsCorrect = false;
                }
                java.util.Map<String, Object> slotView = new java.util.HashMap<>();
                slotView.put("slotNum", slot);
                slotView.put("selectedIndex", selectedIdx);
                slotView.put("selectedText", selectedText);
                slotView.put("correctIndex", correctIdx);
                slotView.put("correctText", correctText);
                slotView.put("isCorrect", isCorrect);
                slotViews.add(slotView);
            }
            if (allSlotsCorrect) {
                fullyCorrectCount++;
            }
            java.util.Map<String, Object> qView = new java.util.HashMap<>();
            qView.put("q", q);
            qView.put("index", idx);
            qView.put("slots", slotViews);
            scoredQuestions.add(qView);
        }
        model.addAttribute("scoredQuestions", scoredQuestions);
        model.addAttribute("yourScore", fullyCorrectCount);
        return "assessment-score";
    }

    private int parseChapterOrder(String chapterId) {
        if (chapterId == null) return Integer.MAX_VALUE;
        try {
            return Integer.parseInt(chapterId.trim());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
