package com.barlarlar.myanmyanlearn.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.barlarlar.myanmyanlearn.model.Course;
import com.barlarlar.myanmyanlearn.service.AssessmentScoreRecordService;
import com.barlarlar.myanmyanlearn.service.CourseService;

@Controller
public class AssessmentController {
    private static final Logger log = LoggerFactory.getLogger(AssessmentController.class);
    private final CourseService courseService;
    private final AssessmentScoreRecordService scoreRecordService;
    private final ObjectMapper objectMapper;

    @Value("${app.assessment.seconds-per-slot:120}")
    private int secondsPerSlot;

    public AssessmentController(
            CourseService courseService,
            AssessmentScoreRecordService scoreRecordService,
            ObjectMapper objectMapper) {
        this.courseService = courseService;
        this.scoreRecordService = scoreRecordService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/assessment")
    public String assessmentPage(
            Model model,
            HttpServletResponse response,
            @RequestParam(name = "chapters", required = false) String chapters,
            @RequestParam(name = "courseId", required = false) String courseId) {
        setNoStoreHeaders(response);

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
        List<com.barlarlar.myanmyanlearn.model.Question> questions = com.barlarlar.myanmyanlearn.datasource.QuestionDataSource
                .getSampleQuestions();

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

        Comparator<com.barlarlar.myanmyanlearn.model.Question> comparator = Comparator.<com.barlarlar.myanmyanlearn.model.Question>comparingInt(
                q -> parseChapterOrder(q.getChapterId()))
                .thenComparingInt(com.barlarlar.myanmyanlearn.model.Question::getQuestionNumber);
        questions.sort(comparator);

        model.addAttribute("questions", questions);

        // Calculate total time for the exam based on total slots
        int totalSlots = questions.stream()
                .mapToInt(q -> q.getSlotCount() > 0 ? q.getSlotCount() : 1)
                .sum();
        long totalTimeSeconds = (long) totalSlots * secondsPerSlot;
        model.addAttribute("totalTimeSeconds", totalTimeSeconds);

        return "assessment";
    }

    @PostMapping("/assessment/score")
    public String assessmentScorePage(
            Model model,
            HttpServletResponse response,
            @RequestParam(name = "chapters", required = false) String chapters,
            @RequestParam(name = "courseId", required = false) String courseId,
            @RequestParam Map<String, String> allParams) {
        setNoStoreHeaders(response);
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

        List<com.barlarlar.myanmyanlearn.model.Question> questions = com.barlarlar.myanmyanlearn.datasource.QuestionDataSource
                .getSampleQuestions();

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

        Comparator<com.barlarlar.myanmyanlearn.model.Question> comparator = Comparator.<com.barlarlar.myanmyanlearn.model.Question>comparingInt(
                q -> parseChapterOrder(q.getChapterId()))
                .thenComparingInt(com.barlarlar.myanmyanlearn.model.Question::getQuestionNumber);
        questions.sort(comparator);

        model.addAttribute("questions", questions);

        // Sum total possible marks
        double totalPossible = questions.stream()
                .mapToDouble(q -> {
                    try {
                        return q.getMarks();
                    } catch (Exception e) {
                        return 0.0;
                    }
                })
                .sum();
        model.addAttribute("totalPossible", totalPossible);

        // Build selection map for keys like q{index}-s{slot}
        java.util.Map<String, Integer> selectionMap = allParams.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().matches("q\\d+-s\\d+"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            try {
                                return Integer.parseInt(e.getValue());
                            } catch (NumberFormatException ex) {
                                return -1;
                            }
                        }));

        // Build scored view model aligned with sorted questions
        java.util.List<java.util.Map<String, Object>> scoredQuestions = new java.util.ArrayList<>();
        int fullyCorrectCount = 0;
        for (int i = 0; i < questions.size(); i++) {
            com.barlarlar.myanmyanlearn.model.Question q = questions.get(i);
            int idx = i + 1;
            java.util.List<java.util.Map<String, Object>> slotViews = new java.util.ArrayList<>();
            boolean allSlotsCorrect = true;
            int slotCount = q.getSlotCount();
            if (slotCount <= 0)
                slotCount = 1;
            for (int slot = 1; slot <= slotCount; slot++) {
                java.util.List<com.barlarlar.myanmyanlearn.model.QuestionOption> opts = (q.getSlotOptions() != null
                        && q.getSlotOptions().size() >= slot)
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

        try {
            var root = objectMapper.createObjectNode();
            if (courseId != null) {
                root.put("courseId", courseId);
            } else {
                root.putNull("courseId");
            }

            var chaptersArr = objectMapper.createArrayNode();
            java.util.Map<Integer, com.fasterxml.jackson.databind.node.ObjectNode> chapterNodes = new java.util.LinkedHashMap<>();

            for (java.util.Map<String, Object> sq : scoredQuestions) {
                Object qObj = sq.get("q");
                if (!(qObj instanceof com.barlarlar.myanmyanlearn.model.Question)) {
                    continue;
                }
                com.barlarlar.myanmyanlearn.model.Question q = (com.barlarlar.myanmyanlearn.model.Question) qObj;
                int chapterNo = parseChapterOrder(q.getChapterId());
                if (chapterNo == Integer.MAX_VALUE) {
                    continue;
                }

                com.fasterxml.jackson.databind.node.ObjectNode chapterNode = chapterNodes.get(chapterNo);
                if (chapterNode == null) {
                    chapterNode = objectMapper.createObjectNode();
                    chapterNode.put("chapter_no", chapterNo);
                    chapterNode.set("questions", objectMapper.createArrayNode());
                    chapterNodes.put(chapterNo, chapterNode);
                }

                com.fasterxml.jackson.databind.node.ObjectNode qNode = objectMapper.createObjectNode();
                qNode.put("question_no", q.getQuestionNumber());
                var slopesArr = objectMapper.createArrayNode();

                Object slotsObj = sq.get("slots");
                if (slotsObj instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<java.util.Map<String, Object>> slots = (java.util.List<java.util.Map<String, Object>>) slotsObj;
                    for (java.util.Map<String, Object> slotView : slots) {
                        Object slotNumObj = slotView.get("slotNum");
                        Object isCorrectObj = slotView.get("isCorrect");
                        if (!(slotNumObj instanceof Integer) || !(isCorrectObj instanceof Boolean)) {
                            continue;
                        }
                        int slopeNo = (Integer) slotNumObj;
                        boolean isCorrect = (Boolean) isCorrectObj;

                        com.fasterxml.jackson.databind.node.ObjectNode slopeNode = objectMapper.createObjectNode();
                        slopeNode.put("slope_no", slopeNo);
                        slopeNode.put("is_correct", isCorrect);
                        slopesArr.add(slopeNode);
                    }
                }
                qNode.set("slopes", slopesArr);

                ((com.fasterxml.jackson.databind.node.ArrayNode) chapterNode.get("questions")).add(qNode);
            }

            for (com.fasterxml.jackson.databind.node.ObjectNode chapterNode : chapterNodes.values()) {
                chaptersArr.add(chapterNode);
            }
            root.set("chapters", chaptersArr);

            String scoreJson = objectMapper.writeValueAsString(root);
            model.addAttribute("scoreJson", scoreJson);
        } catch (Exception e) {
            log.warn("Failed to build assessment score json", e);
            model.addAttribute("scoreJson", "{}");
        }

        return "assessment-score";
    }

    private int parseChapterOrder(String chapterId) {
        if (chapterId == null)
            return Integer.MAX_VALUE;
        try {
            return Integer.parseInt(chapterId.trim());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private void setNoStoreHeaders(HttpServletResponse response) {
        if (response == null)
            return;
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
