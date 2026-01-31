package com.barlarlar.myanmyanlearn.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;

import com.barlarlar.myanmyanlearn.model.Course;
import com.barlarlar.myanmyanlearn.model.Content;
import com.barlarlar.myanmyanlearn.model.Subcontent;
import com.barlarlar.myanmyanlearn.service.CourseService;
import com.barlarlar.myanmyanlearn.service.AssessmentScoreRecordService;
import com.fasterxml.jackson.databind.JsonNode;

@Controller
public class ContentsController {
    private final CourseService courseService;
    private final AssessmentScoreRecordService scoreRecordService;

    public ContentsController(CourseService courseService, AssessmentScoreRecordService scoreRecordService) {
        this.courseService = courseService;
        this.scoreRecordService = scoreRecordService;
    }

    @GetMapping("/contents")
    public String contents(
            Model model,
            @RequestParam(name = "courseId", required = false) String courseId,
            @RequestParam(name = "courseTitle", required = false) String courseTitle,
            @RequestParam(name = "title", required = false) String title) {
        String selected = (courseTitle != null && !courseTitle.isBlank()) ? courseTitle
                : (title != null ? title : null);

        Course course = null;
        if (courseId != null && !courseId.isBlank()) {
            course = courseService.findByIdFromDatabase(courseId);
        }
        if (course == null && selected != null && !selected.isBlank()) {
            course = courseService.findByTitleFromDatabase(selected);
        }

        // Prefer the title from the resolved course for display
        String displayTitle = course != null ? course.getTitle() : selected;
        model.addAttribute("courseTitle", displayTitle);
        if (course != null) {
            model.addAttribute("course", course);
        }
        model.addAttribute("course", course);

        Map<String, Integer> subChapterProgress = new HashMap<>();
        Map<Integer, Integer> chapterProgress = new HashMap<>();
        if (course != null) {
            Optional<JsonNode> scoreJsonOpt = scoreRecordService
                    .latestScoreJsonForCurrentUser(Optional.ofNullable(course.getId()));
            if (scoreJsonOpt.isPresent()) {
                JsonNode scoreJson = scoreJsonOpt.get();
                chapterProgress = computeChapterProgress(scoreJson);
                subChapterProgress = computeSubChapterProgress(course, scoreJson, chapterProgress);
            }
        }
        model.addAttribute("subChapterProgress", subChapterProgress);
        model.addAttribute("chapterProgress", chapterProgress);
        return "contents";
    }

    private Map<Integer, Integer> computeChapterProgress(JsonNode scoreJsonRoot) {
        Map<Integer, Integer> out = new HashMap<>();
        if (scoreJsonRoot == null || !scoreJsonRoot.isObject()) {
            return out;
        }
        JsonNode chapters = scoreJsonRoot.get("chapters");
        if (chapters == null || !chapters.isArray()) {
            return out;
        }

        for (JsonNode chapterNode : chapters) {
            if (chapterNode == null || !chapterNode.isObject()) {
                continue;
            }
            JsonNode chapterNoNode = chapterNode.get("chapter_no");
            if (chapterNoNode == null || !chapterNoNode.canConvertToInt()) {
                continue;
            }
            int chapterNo = chapterNoNode.asInt();

            JsonNode questions = chapterNode.get("questions");
            if (questions == null || !questions.isArray()) {
                continue;
            }

            int total = 0;
            int correct = 0;
            for (JsonNode qNode : questions) {
                if (qNode == null || !qNode.isObject()) {
                    continue;
                }
                JsonNode slopes = qNode.get("slopes");
                if (slopes == null || !slopes.isArray()) {
                    continue;
                }
                for (JsonNode slopeNode : slopes) {
                    if (slopeNode == null || !slopeNode.isObject()) {
                        continue;
                    }
                    JsonNode isCorrectNode = slopeNode.get("is_correct");
                    if (isCorrectNode == null || !isCorrectNode.isBoolean()) {
                        continue;
                    }
                    total++;
                    if (isCorrectNode.asBoolean()) {
                        correct++;
                    }
                }
            }

            int percent = total == 0 ? 0 : (int) Math.round((correct * 100.0) / total);
            if (percent < 0) {
                percent = 0;
            } else if (percent > 100) {
                percent = 100;
            }
            out.put(chapterNo, percent);
        }
        return out;
    }

    private Map<String, Integer> computeSubChapterProgress(Course course, JsonNode scoreJsonRoot,
            Map<Integer, Integer> chapterProgress) {
        Map<String, Integer> out = new HashMap<>();
        if (course == null || course.getContents() == null) {
            return out;
        }
        if (chapterProgress == null) {
            chapterProgress = Map.of();
        }

        Map<String, Integer> questionProgress = computeQuestionProgress(scoreJsonRoot);

        for (Content content : course.getContents()) {
            if (content == null || content.getSubcontents() == null) {
                continue;
            }
            int chapterNo = content.getOrder();
            int chapterPct = chapterProgress.getOrDefault(chapterNo, 0);

            boolean singleSubcontent = content.getSubcontents().size() == 1;
            for (Subcontent sub : content.getSubcontents()) {
                if (sub == null) {
                    continue;
                }
                String key = chapterNo + "-" + sub.getOrder();
                Integer pct = questionProgress.get(key);
                if (singleSubcontent) {
                    pct = chapterPct;
                } else if (pct == null) {
                    pct = chapterPct;
                }
                out.put(key, pct);
            }
        }

        return out;
    }

    private Map<String, Integer> computeQuestionProgress(JsonNode scoreJsonRoot) {
        Map<String, Integer> out = new HashMap<>();
        if (scoreJsonRoot == null || !scoreJsonRoot.isObject()) {
            return out;
        }
        JsonNode chapters = scoreJsonRoot.get("chapters");
        if (chapters == null || !chapters.isArray()) {
            return out;
        }

        for (JsonNode chapterNode : chapters) {
            if (chapterNode == null || !chapterNode.isObject()) {
                continue;
            }
            JsonNode chapterNoNode = chapterNode.get("chapter_no");
            if (chapterNoNode == null || !chapterNoNode.canConvertToInt()) {
                continue;
            }
            int chapterNo = chapterNoNode.asInt();
            JsonNode questions = chapterNode.get("questions");
            if (questions == null || !questions.isArray()) {
                continue;
            }

            for (JsonNode qNode : questions) {
                if (qNode == null || !qNode.isObject()) {
                    continue;
                }
                JsonNode qNoNode = qNode.get("question_no");
                if (qNoNode == null || !qNoNode.canConvertToInt()) {
                    continue;
                }
                int questionNo = qNoNode.asInt();

                JsonNode slopes = qNode.get("slopes");
                if (slopes == null || !slopes.isArray()) {
                    continue;
                }

                int total = 0;
                int correct = 0;
                for (JsonNode slopeNode : slopes) {
                    if (slopeNode == null || !slopeNode.isObject()) {
                        continue;
                    }
                    JsonNode isCorrectNode = slopeNode.get("is_correct");
                    if (isCorrectNode == null || !isCorrectNode.isBoolean()) {
                        continue;
                    }
                    total++;
                    if (isCorrectNode.asBoolean()) {
                        correct++;
                    }
                }
                int percent = total == 0 ? 0 : (int) Math.round((correct * 100.0) / total);
                if (percent < 0) {
                    percent = 0;
                } else if (percent > 100) {
                    percent = 100;
                }
                out.put(chapterNo + "-" + questionNo, percent);
            }
        }

        return out;
    }
}
