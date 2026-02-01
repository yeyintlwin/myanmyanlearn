package com.barlarlar.myanmyanlearn.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.barlarlar.myanmyanlearn.model.Course;
import com.barlarlar.myanmyanlearn.service.AssessmentScoreRecordService;
import com.barlarlar.myanmyanlearn.service.CourseService;

@Controller
@Slf4j
@RequiredArgsConstructor
public class AssessmentController {
    private final CourseService courseService;
    private final ObjectMapper objectMapper;
    private final AssessmentScoreRecordService scoreRecordService;

    @Value("${app.assessment.seconds-per-slot:15}")
    private int secondsPerSlot;

    @GetMapping("/assessment")
    public String assessmentPage(
            Model model,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(name = "chapters", required = false) String chapters,
            @RequestParam(name = "courseId", required = false) String courseId) {
        setNoStoreHeaders(response);
        courseId = resolveCourseId(courseId, request);
        Authentication auth = SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()
                : null;
        if (courseId != null && !courseId.isBlank() && !courseService.canAccessCourse(courseId, auth)) {
            return "redirect:/home";
        }

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
            Course course = courseService.findAccessibleByIdFromDatabase(courseId, auth);
            if (course != null) {
                examTitle = course.getTitle();
            }
        }
        model.addAttribute("examTitle", examTitle != null ? examTitle : "Assessment");
        List<com.barlarlar.myanmyanlearn.model.Question> questions = (courseId != null && !courseId.isBlank())
                ? courseService.getAssessmentQuestionsFromDatabase(courseId, chapterList)
                : Collections.emptyList();

        Comparator<com.barlarlar.myanmyanlearn.model.Question> comparator = Comparator.<com.barlarlar.myanmyanlearn.model.Question>comparingInt(
                q -> parseChapterOrder(q.getChapterId()))
                .thenComparingInt(com.barlarlar.myanmyanlearn.model.Question::getQuestionNumber);
        questions.sort(comparator);

        model.addAttribute("questions", questions);

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
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(name = "chapters", required = false) String chapters,
            @RequestParam(name = "courseId", required = false) String courseId,
            @RequestParam Map<String, String> allParams) {
        setNoStoreHeaders(response);
        courseId = resolveCourseId(courseId, request);
        Authentication auth = SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()
                : null;
        if (courseId != null && !courseId.isBlank() && !courseService.canAccessCourse(courseId, auth)) {
            return "redirect:/home";
        }
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
            Course course = courseService.findAccessibleByIdFromDatabase(courseId, auth);
            if (course != null) {
                examTitle = course.getTitle();
            }
        }
        model.addAttribute("examTitle", examTitle != null ? examTitle : "Assessment");

        List<com.barlarlar.myanmyanlearn.model.Question> questions = (courseId != null && !courseId.isBlank())
                ? courseService.getAssessmentQuestionsFromDatabase(courseId, chapterList)
                : Collections.emptyList();

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
        java.math.BigDecimal earnedTotal = java.math.BigDecimal.ZERO;
        for (int i = 0; i < questions.size(); i++) {
            com.barlarlar.myanmyanlearn.model.Question q = questions.get(i);
            int idx = i + 1;
            java.util.List<java.util.Map<String, Object>> slotViews = new java.util.ArrayList<>();
            int slotCount = q.getSlotCount();
            if (slotCount <= 0)
                slotCount = 1;
            java.math.BigDecimal qMarks = java.math.BigDecimal.valueOf((double) q.getMarks());
            java.math.BigDecimal perSlotMarks = qMarks.divide(
                    java.math.BigDecimal.valueOf((long) slotCount),
                    6,
                    java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal earnedForQuestion = java.math.BigDecimal.ZERO;
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
                java.util.List<Integer> correctIndices = new java.util.ArrayList<>();
                java.util.List<String> correctTexts = new java.util.ArrayList<>();
                if (opts != null) {
                    for (com.barlarlar.myanmyanlearn.model.QuestionOption opt : opts) {
                        if (opt.getOptionIndex() == selectedIdx) {
                            selectedText = opt.getOptionContent();
                        }
                        if (opt.getIsCorrect()) {
                            correctIndices.add(opt.getOptionIndex());
                            correctTexts.add(opt.getOptionContent());
                        }
                    }
                }
                boolean isCorrect = (selectedIdx != -1) && correctIndices.contains(selectedIdx);
                if (isCorrect) {
                    earnedForQuestion = earnedForQuestion.add(perSlotMarks);
                }
                Integer correctIdx = correctIndices.isEmpty() ? null : correctIndices.get(0);
                String correctText = correctTexts.isEmpty() ? null : String.join(", ", correctTexts);
                java.util.Map<String, Object> slotView = new java.util.HashMap<>();
                slotView.put("slotNum", slot);
                slotView.put("selectedIndex", selectedIdx);
                slotView.put("selectedText", selectedText);
                slotView.put("correctIndex", correctIdx);
                slotView.put("correctText", correctText);
                slotView.put("isCorrect", isCorrect);
                slotViews.add(slotView);
            }
            earnedTotal = earnedTotal.add(earnedForQuestion);
            java.util.Map<String, Object> qView = new java.util.HashMap<>();
            qView.put("q", q);
            qView.put("index", idx);
            qView.put("slots", slotViews);
            scoredQuestions.add(qView);
        }
        model.addAttribute("scoredQuestions", scoredQuestions);
        model.addAttribute("yourScore", earnedTotal.stripTrailingZeros().toPlainString());

        try {
            var root = objectMapper.createObjectNode();
            if (courseId != null && !courseId.isBlank()) {
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
            try {
                scoreRecordService.upsertMergedForCurrentUser(root);
            } catch (IllegalStateException e) {
                log.warn("Failed to save assessment score json", e);
            } catch (IllegalArgumentException e) {
                log.warn("Failed to save assessment score json", e);
            } catch (Exception e) {
                log.warn("Failed to save assessment score json", e);
            }
        } catch (Exception e) {
            log.warn("Failed to build assessment score json", e);
            model.addAttribute("scoreJson", "{}");
        }

        return "assessment-score";
    }

    @GetMapping(value = "/assessment/md", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> assessmentQuestionMarkdown(
            @RequestParam(name = "courseId") String courseId,
            @RequestParam(name = "ch") Integer chapterOrder,
            @RequestParam(name = "q") Integer questionNo) {
        Authentication auth = SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()
                : null;
        if (!courseService.canAccessCourse(courseId, auth)) {
            return ResponseEntity.notFound().build();
        }
        String markdown = courseService.findAssessmentQuestionMarkdownFromDatabase(courseId, chapterOrder, questionNo);
        if (markdown == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(markdown);
    }

    @GetMapping(value = "/assessment/explain", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> assessmentExplanationMarkdown(
            @RequestParam(name = "courseId") String courseId,
            @RequestParam(name = "ch") Integer chapterOrder,
            @RequestParam(name = "q") Integer questionNo) {
        Authentication auth = SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()
                : null;
        if (!courseService.canAccessCourse(courseId, auth)) {
            return ResponseEntity.notFound().build();
        }
        String markdown = courseService.findAssessmentExplanationMarkdownFromDatabase(courseId, chapterOrder,
                questionNo);
        if (markdown == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(markdown);
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

    private String resolveCourseId(String courseId, HttpServletRequest request) {
        if (courseId != null && !courseId.isBlank()) {
            return courseId;
        }
        if (request == null) {
            return null;
        }
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(referer);
            String query = uri.getQuery();
            if (query == null || query.isBlank()) {
                return null;
            }
            for (String part : query.split("&")) {
                if (part == null || part.isBlank()) {
                    continue;
                }
                int idx = part.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = part.substring(0, idx);
                if (!"courseId".equals(key)) {
                    continue;
                }
                String raw = part.substring(idx + 1);
                String decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);
                if (decoded != null && !decoded.isBlank()) {
                    return decoded;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
