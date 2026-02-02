package com.barlarlar.myanmyanlearn.controller;

import com.barlarlar.myanmyanlearn.entity.CourseChapterEntity;
import com.barlarlar.myanmyanlearn.entity.CourseQuestionEntity;
import com.barlarlar.myanmyanlearn.entity.CourseSubchapterEntity;
import com.barlarlar.myanmyanlearn.repository.CourseChapterRepository;
import com.barlarlar.myanmyanlearn.repository.CourseQuestionOptionRepository;
import com.barlarlar.myanmyanlearn.repository.CourseRepository;
import com.barlarlar.myanmyanlearn.repository.CourseQuestionRepository;
import com.barlarlar.myanmyanlearn.repository.CourseQuestionSlotOptionRepository;
import com.barlarlar.myanmyanlearn.repository.CourseQuestionSlotRepository;
import com.barlarlar.myanmyanlearn.repository.CourseSubchapterRepository;
import com.barlarlar.myanmyanlearn.service.storage.StorageService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@Slf4j
@RequiredArgsConstructor
public class MarkdownEditorController {
    private final CourseRepository courseRepository;
    private final CourseChapterRepository courseChapterRepository;
    private final CourseSubchapterRepository courseSubchapterRepository;
    private final CourseQuestionRepository courseQuestionRepository;
    private final CourseQuestionSlotRepository courseQuestionSlotRepository;
    private final CourseQuestionSlotOptionRepository courseQuestionSlotOptionRepository;
    private final CourseQuestionOptionRepository courseQuestionOptionRepository;
    private final StorageService storageService;

    @GetMapping("/markdown-editor")
    public String editor(
            @RequestParam(name = "courseId") String courseId,
            @RequestParam(name = "chapterId") String chapterId,
            @RequestParam(name = "kind") String kind,
            @RequestParam(name = "subchapterId", required = false) String subchapterId,
            @RequestParam(name = "questionId", required = false) String questionId,
            Model model) {
        EditorTarget target = resolveTarget(courseId, chapterId, kind, subchapterId, questionId);
        model.addAttribute("courseId", target.courseId());
        model.addAttribute("chapterId", target.chapterUidOrNumber());
        model.addAttribute("kind", target.kind());
        model.addAttribute("subchapterId", target.subchapterUidOrNumber());
        model.addAttribute("questionId", target.questionUidOrNumber());
        model.addAttribute("markdown", target.markdown());
        model.addAttribute("contextLabel", target.contextLabel());
        return "markdown-editor";
    }

    @GetMapping("/markdown-editor/exists")
    public ResponseEntity<Void> editorTargetExists(
            @RequestParam(name = "courseId") String courseId,
            @RequestParam(name = "chapterId") String chapterId,
            @RequestParam(name = "kind") String kind,
            @RequestParam(name = "subchapterId", required = false) String subchapterId,
            @RequestParam(name = "questionId", required = false) String questionId) {
        try {
            resolveTarget(courseId, chapterId, kind, subchapterId, questionId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @Transactional
    @GetMapping(value = "/markdown-editor/load", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> load(
            @RequestParam(name = "courseId") String courseId,
            @RequestParam(name = "chapterId") String chapterId,
            @RequestParam(name = "kind") String kind,
            @RequestParam(name = "subchapterId", required = false) String subchapterId,
            @RequestParam(name = "questionId", required = false) String questionId) {
        Map<String, Object> out = new HashMap<>();
        try {
            EditorTarget target = resolveTarget(courseId, chapterId, kind, subchapterId, questionId);
            out.put("ok", true);
            out.put("courseId", target.courseId());
            out.put("chapterId", target.chapterUidOrNumber());
            out.put("kind", target.kind());
            out.put("subchapterId", target.subchapterUidOrNumber());
            out.put("questionId", target.questionUidOrNumber());
            out.put("markdown", target.markdown());
            out.put("contextLabel", target.contextLabel());
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException e) {
            out.put("ok", false);
            out.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(out);
        }
    }

    public record OutlineChapterRenameRequest(String courseId, String chapterId, String name) {
    }

    @Transactional
    @PostMapping(value = "/markdown-editor/outline/chapter/rename", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> renameChapter(@RequestBody OutlineChapterRenameRequest req) {
        Map<String, Object> out = new HashMap<>();
        String courseId = req != null && req.courseId() != null ? req.courseId().trim() : "";
        String chapterId = req != null && req.chapterId() != null ? req.chapterId().trim() : "";
        String name = req != null && req.name() != null ? req.name().trim() : "";
        if (courseId.isBlank() || chapterId.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing courseId or chapterId.");
            return ResponseEntity.badRequest().body(out);
        }
        CourseChapterEntity ch = findChapter(courseId, chapterId);
        if (ch == null || ch.getId() == null) {
            out.put("ok", false);
            out.put("message", "Chapter not found.");
            return ResponseEntity.badRequest().body(out);
        }
        ch.setName(name);
        courseChapterRepository.save(ch);
        out.put("ok", true);
        out.put("chapterId", ensureChapterUid(ch));
        return ResponseEntity.ok(out);
    }

    public record OutlineSubchapterRenameRequest(String courseId, String chapterId, String subchapterId, String name) {
    }

    @Transactional
    @PostMapping(value = "/markdown-editor/outline/subchapter/rename", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> renameSubchapter(@RequestBody OutlineSubchapterRenameRequest req) {
        Map<String, Object> out = new HashMap<>();
        String courseId = req != null && req.courseId() != null ? req.courseId().trim() : "";
        String chapterId = req != null && req.chapterId() != null ? req.chapterId().trim() : "";
        String subchapterId = req != null && req.subchapterId() != null ? req.subchapterId().trim() : "";
        String name = req != null && req.name() != null ? req.name().trim() : "";
        if (courseId.isBlank() || chapterId.isBlank() || subchapterId.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing ids.");
            return ResponseEntity.badRequest().body(out);
        }
        CourseChapterEntity ch = findChapter(courseId, chapterId);
        if (ch == null || ch.getId() == null) {
            out.put("ok", false);
            out.put("message", "Chapter not found.");
            return ResponseEntity.badRequest().body(out);
        }
        CourseSubchapterEntity sc = findSubchapter(ch.getId(), subchapterId);
        if (sc == null || sc.getId() == null) {
            out.put("ok", false);
            out.put("message", "Subchapter not found.");
            return ResponseEntity.badRequest().body(out);
        }
        sc.setName(name);
        courseSubchapterRepository.save(sc);
        out.put("ok", true);
        out.put("subchapterId", ensureSubchapterUid(sc));
        return ResponseEntity.ok(out);
    }

    public record OutlineChapterAddRequest(String courseId, String name) {
    }

    @Transactional
    @PostMapping(value = "/markdown-editor/outline/chapter/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> addChapter(@RequestBody OutlineChapterAddRequest req) {
        Map<String, Object> out = new HashMap<>();
        String courseId = req != null && req.courseId() != null ? req.courseId().trim() : "";
        String name = req != null && req.name() != null ? req.name().trim() : "";
        if (courseId.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing courseId.");
            return ResponseEntity.badRequest().body(out);
        }
        if (courseRepository.findById(courseId).isEmpty()) {
            out.put("ok", false);
            out.put("message", "Course not found.");
            return ResponseEntity.badRequest().body(out);
        }
        int nextNumber = 1;
        for (CourseChapterEntity ch : courseChapterRepository.findByCourseIdOrderByChapterNumberAsc(courseId)) {
            if (ch != null && ch.getChapterNumber() != null) {
                nextNumber = Math.max(nextNumber, ch.getChapterNumber() + 1);
            }
        }
        CourseChapterEntity ch = new CourseChapterEntity();
        ch.setCourseId(courseId);
        ch.setChapterNumber(nextNumber);
        ch.setName(name);
        CourseChapterEntity savedChapter = courseChapterRepository.save(ch);
        String chapterStableId = ensureChapterUid(savedChapter);

        CourseSubchapterEntity sc = new CourseSubchapterEntity();
        sc.setChapterId(savedChapter.getId());
        sc.setSubchapterNumber(1);
        sc.setName("New subchapter");
        sc.setMarkdown("");
        CourseSubchapterEntity savedSub = courseSubchapterRepository.save(sc);
        String subStableId = ensureSubchapterUid(savedSub);

        out.put("ok", true);
        out.put("chapterId", chapterStableId);
        out.put("subchapterId", subStableId);
        return ResponseEntity.ok(out);
    }

    public record OutlineSubchapterAddRequest(String courseId, String chapterId, String name) {
    }

    @Transactional
    @PostMapping(value = "/markdown-editor/outline/subchapter/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> addSubchapter(@RequestBody OutlineSubchapterAddRequest req) {
        Map<String, Object> out = new HashMap<>();
        String courseId = req != null && req.courseId() != null ? req.courseId().trim() : "";
        String chapterId = req != null && req.chapterId() != null ? req.chapterId().trim() : "";
        String name = req != null && req.name() != null ? req.name().trim() : "";
        if (courseId.isBlank() || chapterId.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing courseId or chapterId.");
            return ResponseEntity.badRequest().body(out);
        }
        CourseChapterEntity ch = findChapter(courseId, chapterId);
        if (ch == null || ch.getId() == null) {
            out.put("ok", false);
            out.put("message", "Chapter not found.");
            return ResponseEntity.badRequest().body(out);
        }
        int nextNumber = 1;
        for (CourseSubchapterEntity sc : courseSubchapterRepository.findByChapterIdOrderBySubchapterNumberAsc(ch.getId())) {
            if (sc != null && sc.getSubchapterNumber() != null) {
                nextNumber = Math.max(nextNumber, sc.getSubchapterNumber() + 1);
            }
        }
        CourseSubchapterEntity sc = new CourseSubchapterEntity();
        sc.setChapterId(ch.getId());
        sc.setSubchapterNumber(nextNumber);
        sc.setName(name);
        sc.setMarkdown("");
        CourseSubchapterEntity saved = courseSubchapterRepository.save(sc);
        out.put("ok", true);
        out.put("chapterId", ensureChapterUid(ch));
        out.put("subchapterId", ensureSubchapterUid(saved));
        return ResponseEntity.ok(out);
    }

    public record OutlineSubchapterDeleteRequest(String courseId, String chapterId, String subchapterId) {
    }

    @Transactional
    @PostMapping(value = "/markdown-editor/outline/subchapter/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> deleteSubchapter(@RequestBody OutlineSubchapterDeleteRequest req) {
        Map<String, Object> out = new HashMap<>();
        String courseId = req != null && req.courseId() != null ? req.courseId().trim() : "";
        String chapterId = req != null && req.chapterId() != null ? req.chapterId().trim() : "";
        String subchapterId = req != null && req.subchapterId() != null ? req.subchapterId().trim() : "";
        if (courseId.isBlank() || chapterId.isBlank() || subchapterId.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing ids.");
            return ResponseEntity.badRequest().body(out);
        }
        CourseChapterEntity ch = findChapter(courseId, chapterId);
        if (ch == null || ch.getId() == null) {
            out.put("ok", false);
            out.put("message", "Chapter not found.");
            return ResponseEntity.badRequest().body(out);
        }
        List<CourseSubchapterEntity> subs = courseSubchapterRepository.findByChapterIdOrderBySubchapterNumberAsc(ch.getId());
        if (subs.size() <= 1) {
            out.put("ok", false);
            out.put("message", "Cannot delete the last subchapter. Delete the chapter instead.");
            return ResponseEntity.badRequest().body(out);
        }
        CourseSubchapterEntity sc = findSubchapter(ch.getId(), subchapterId);
        if (sc == null || sc.getId() == null) {
            out.put("ok", false);
            out.put("message", "Subchapter not found.");
            return ResponseEntity.badRequest().body(out);
        }
        courseSubchapterRepository.delete(sc);
        out.put("ok", true);
        return ResponseEntity.ok(out);
    }

    public record OutlineChapterDeleteRequest(String courseId, String chapterId) {
    }

    @Transactional
    @PostMapping(value = "/markdown-editor/outline/chapter/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> deleteChapter(@RequestBody OutlineChapterDeleteRequest req) {
        Map<String, Object> out = new HashMap<>();
        String courseId = req != null && req.courseId() != null ? req.courseId().trim() : "";
        String chapterId = req != null && req.chapterId() != null ? req.chapterId().trim() : "";
        if (courseId.isBlank() || chapterId.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing courseId or chapterId.");
            return ResponseEntity.badRequest().body(out);
        }
        List<CourseChapterEntity> chapters = courseChapterRepository.findByCourseIdOrderByChapterNumberAsc(courseId);
        if (chapters.size() <= 1) {
            out.put("ok", false);
            out.put("message", "Cannot delete the last chapter.");
            return ResponseEntity.badRequest().body(out);
        }
        CourseChapterEntity ch = findChapter(courseId, chapterId);
        if (ch == null || ch.getId() == null) {
            out.put("ok", false);
            out.put("message", "Chapter not found.");
            return ResponseEntity.badRequest().body(out);
        }

        List<CourseQuestionEntity> questions = courseQuestionRepository.findByChapterIdOrderByQuestionNumberAsc(ch.getId());
        for (CourseQuestionEntity q : questions) {
            if (q == null || q.getId() == null) {
                continue;
            }
            List<com.barlarlar.myanmyanlearn.entity.CourseQuestionSlotEntity> slots = courseQuestionSlotRepository
                    .findByQuestionIdOrderBySlotIndexAsc(q.getId());
            for (com.barlarlar.myanmyanlearn.entity.CourseQuestionSlotEntity s : slots) {
                if (s == null || s.getId() == null) {
                    continue;
                }
                List<com.barlarlar.myanmyanlearn.entity.CourseQuestionSlotOptionEntity> opts = courseQuestionSlotOptionRepository
                        .findByQuestionSlotIdOrderByOptionIndexAsc(s.getId());
                if (!opts.isEmpty()) {
                    courseQuestionSlotOptionRepository.deleteAll(opts);
                }
            }
            if (!slots.isEmpty()) {
                courseQuestionSlotRepository.deleteAll(slots);
            }

            List<com.barlarlar.myanmyanlearn.entity.CourseQuestionOptionEntity> legacy = courseQuestionOptionRepository
                    .findByQuestionIdOrderByOptionIndexAsc(q.getId());
            if (!legacy.isEmpty()) {
                courseQuestionOptionRepository.deleteAll(legacy);
            }
        }
        if (!questions.isEmpty()) {
            courseQuestionRepository.deleteAll(questions);
        }

        List<CourseSubchapterEntity> subs = courseSubchapterRepository.findByChapterIdOrderBySubchapterNumberAsc(ch.getId());
        if (!subs.isEmpty()) {
            courseSubchapterRepository.deleteAll(subs);
        }
        courseChapterRepository.delete(ch);
        out.put("ok", true);
        return ResponseEntity.ok(out);
    }

    public record SaveRequest(
            String courseId,
            String chapterId,
            String kind,
            String subchapterId,
            String questionId,
            String markdown) {
    }

    @Transactional
    @PostMapping(value = "/markdown-editor/save", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> save(@RequestBody SaveRequest req) {
        Map<String, Object> out = new HashMap<>();
        if (req == null) {
            out.put("ok", false);
            out.put("message", "Missing request body.");
            return ResponseEntity.badRequest().body(out);
        }
        EditorTarget target;
        try {
            target = resolveTarget(req.courseId(), req.chapterId(), req.kind(), req.subchapterId(), req.questionId());
        } catch (IllegalArgumentException e) {
            out.put("ok", false);
            out.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(out);
        }
        String value = req.markdown() != null ? req.markdown() : "";
        if ("subchapter".equals(target.kind())) {
            CourseSubchapterEntity sc = Objects.requireNonNull(target.subchapterEntity());
            sc.setMarkdown(value);
            courseSubchapterRepository.save(sc);
        } else if ("question".equals(target.kind())) {
            CourseQuestionEntity q = Objects.requireNonNull(target.questionEntity());
            q.setQuestionMarkdown(value);
            courseQuestionRepository.save(q);
        } else if ("explanation".equals(target.kind())) {
            CourseQuestionEntity q = Objects.requireNonNull(target.questionEntity());
            q.setExplanationMarkdown(value);
            courseQuestionRepository.save(q);
        } else {
            out.put("ok", false);
            out.put("message", "Invalid kind.");
            return ResponseEntity.badRequest().body(out);
        }
        out.put("ok", true);
        return ResponseEntity.ok(out);
    }

    @PostMapping(value = "/markdown-editor/image/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam(name = "courseId") String courseId,
            @RequestParam(name = "chapterId") String chapterId,
            @RequestParam(name = "kind") String kind,
            @RequestParam(name = "subchapterId", required = false) String subchapterId,
            @RequestParam(name = "questionId", required = false) String questionId,
            @RequestParam(name = "file") MultipartFile file) {
        Map<String, Object> out = new HashMap<>();
        EditorTarget target;
        try {
            target = resolveTarget(courseId, chapterId, kind, subchapterId, questionId);
        } catch (IllegalArgumentException e) {
            out.put("ok", false);
            out.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(out);
        }

        if (file == null || file.isEmpty()) {
            out.put("ok", false);
            out.put("message", "No file uploaded.");
            return ResponseEntity.badRequest().body(out);
        }
        if (file.getSize() > 5L * 1024L * 1024L) {
            out.put("ok", false);
            out.put("message", "Image must be 5MB or smaller.");
            return ResponseEntity.badRequest().body(out);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            out.put("ok", false);
            out.put("message", "Only image files are allowed.");
            return ResponseEntity.badRequest().body(out);
        }

        String courseFolder = sanitizePathSegment(target.courseId());
        String chapterFolder = sanitizePathSegment(target.chapterStableId());
        String targetFolder = sanitizePathSegment(target.targetStableId());
        String kindFolder = sanitizePathSegment(target.kind());

        String ext = extensionFromContentTypeOrName(contentType, file.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        String key = "courses/"
                + courseFolder
                + "/markdown-editor/"
                + chapterFolder
                + "/"
                + targetFolder
                + "/"
                + kindFolder
                + "/images/"
                + filename;
        try {
            StorageService.StoredObject stored = storageService.put(key, file);
            out.put("ok", true);
            out.put("url", stored.url());
            out.put("name", filename);
            return ResponseEntity.ok(out);
        } catch (IOException e) {
            log.error("Markdown editor image save failed. key={}, courseId={}, chapter={}, kind={}",
                    key, target.courseId(), target.chapterUidOrNumber(), target.kind(), e);
            out.put("ok", false);
            out.put("message", "Failed to save image: " + e.getClass().getSimpleName() + ": "
                    + Objects.toString(e.getMessage(), ""));
            return ResponseEntity.internalServerError().body(out);
        }
    }

    public record DeleteAssetRequest(String courseId, String chapterId, String key) {
    }

    public record AssetItem(
            String key,
            String url,
            String name,
            String kind,
            String targetId,
            String groupKey,
            String groupLabel) {
    }

    @GetMapping(value = "/markdown-editor/assets", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> listAssetsForChapter(
            @RequestParam(name = "courseId") String courseId,
            @RequestParam(name = "chapterId") String chapterId) {
        Map<String, Object> out = new HashMap<>();
        String course = courseId != null ? courseId.trim() : "";
        String chapter = chapterId != null ? chapterId.trim() : "";
        if (course.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing courseId.");
            return ResponseEntity.badRequest().body(out);
        }
        if (chapter.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing chapterId.");
            return ResponseEntity.badRequest().body(out);
        }
        CourseChapterEntity ch = findChapter(course, chapter);
        if (ch == null || ch.getId() == null) {
            out.put("ok", false);
            out.put("message", "Chapter not found.");
            return ResponseEntity.badRequest().body(out);
        }

        String chapterStableId = ch.getChapterUid() != null ? ch.getChapterUid().trim() : "";
        if (chapterStableId.isBlank()) {
            chapterStableId = "chapter_" + ch.getId();
        }

        Map<String, Integer> subchapterNumberByStableId = new HashMap<>();
        for (CourseSubchapterEntity sc : courseSubchapterRepository
                .findByChapterIdOrderBySubchapterNumberAsc(ch.getId())) {
            if (sc == null || sc.getId() == null) {
                continue;
            }
            String stable = sc.getSubchapterUid() != null ? sc.getSubchapterUid().trim() : "";
            if (stable.isBlank()) {
                stable = "subchapter_" + sc.getId();
            }
            Integer num = sc.getSubchapterNumber();
            if (num != null) {
                subchapterNumberByStableId.put(stable, num);
            }
        }

        Map<String, Integer> questionNumberByStableId = new HashMap<>();
        for (CourseQuestionEntity q : courseQuestionRepository.findByChapterIdOrderByQuestionNumberAsc(ch.getId())) {
            if (q == null || q.getId() == null) {
                continue;
            }
            String stable = q.getQuestionUid() != null ? q.getQuestionUid().trim() : "";
            if (stable.isBlank()) {
                stable = "question_" + q.getId();
            }
            Integer num = q.getQuestionNumber();
            if (num != null) {
                questionNumberByStableId.put(stable, num);
            }
        }

        String courseFolder = sanitizePathSegment(course);
        String chapterFolder = sanitizePathSegment(chapterStableId);
        String prefix = "courses/" + courseFolder + "/markdown-editor/" + chapterFolder + "/";

        List<AssetItem> items = new ArrayList<>();
        try {
            for (StorageService.StoredObject obj : storageService.list(prefix)) {
                if (obj == null) {
                    continue;
                }
                String key = obj.key() != null ? obj.key().trim() : "";
                if (key.isBlank() || !key.startsWith(prefix) || !key.contains("/images/")) {
                    continue;
                }
                String rel = key.substring(prefix.length());
                String[] parts = rel.split("/");
                if (parts.length < 4) {
                    continue;
                }
                String targetId = parts[0];
                String kind = parts[1];
                String imagesFolder = parts[2];
                String name = parts[3];
                if (targetId.isBlank() || kind.isBlank() || name.isBlank()) {
                    continue;
                }
                if (!"images".equals(imagesFolder)) {
                    continue;
                }

                String groupKey = targetId + ":" + kind;
                String groupLabel;
                if (targetId.startsWith("subchapter_")) {
                    Integer n = subchapterNumberByStableId.get(targetId);
                    groupLabel = n != null ? ("Subchapter " + n) : targetId;
                } else if (targetId.startsWith("question_")) {
                    Integer n = questionNumberByStableId.get(targetId);
                    String base = n != null ? ("Question " + n) : targetId;
                    if ("explanation".equals(kind)) {
                        groupLabel = base + " (Explanation)";
                    } else if ("question".equals(kind)) {
                        groupLabel = base + " (Question)";
                    } else {
                        groupLabel = base + " (" + kind + ")";
                    }
                } else {
                    groupLabel = targetId + " (" + kind + ")";
                }

                items.add(new AssetItem(key, obj.url(), name, kind, targetId, groupKey, groupLabel));
            }
        } catch (IOException e) {
            out.put("ok", false);
            out.put("message", "Failed to list assets: " + e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(out);
        }

        items.sort(Comparator
                .comparing((AssetItem x) -> x.groupLabel() != null ? x.groupLabel() : "")
                .thenComparing((AssetItem x) -> x.name() != null ? x.name() : ""));

        out.put("ok", true);
        out.put("items", items);
        out.put("prefix", prefix);
        return ResponseEntity.ok(out);
    }

    @PostMapping(value = "/markdown-editor/assets/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> deleteAsset(@RequestBody DeleteAssetRequest req) {
        Map<String, Object> out = new HashMap<>();
        if (req == null) {
            out.put("ok", false);
            out.put("message", "Missing request body.");
            return ResponseEntity.badRequest().body(out);
        }
        String course = req.courseId() != null ? req.courseId().trim() : "";
        String chapter = req.chapterId() != null ? req.chapterId().trim() : "";
        String key = req.key() != null ? req.key().trim() : "";
        if (course.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing courseId.");
            return ResponseEntity.badRequest().body(out);
        }
        if (chapter.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing chapterId.");
            return ResponseEntity.badRequest().body(out);
        }
        if (key.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing key.");
            return ResponseEntity.badRequest().body(out);
        }
        CourseChapterEntity ch = findChapter(course, chapter);
        if (ch == null || ch.getId() == null) {
            out.put("ok", false);
            out.put("message", "Chapter not found.");
            return ResponseEntity.badRequest().body(out);
        }

        String chapterStableId = ch.getChapterUid() != null ? ch.getChapterUid().trim() : "";
        if (chapterStableId.isBlank()) {
            chapterStableId = "chapter_" + ch.getId();
        }
        String courseFolder = sanitizePathSegment(course);
        String chapterFolder = sanitizePathSegment(chapterStableId);
        String prefix = "courses/" + courseFolder + "/markdown-editor/" + chapterFolder + "/";
        if (!key.startsWith(prefix) || !key.contains("/images/")) {
            out.put("ok", false);
            out.put("message", "Invalid asset key.");
            return ResponseEntity.badRequest().body(out);
        }
        try {
            storageService.delete(key);
            out.put("ok", true);
            return ResponseEntity.ok(out);
        } catch (IOException e) {
            out.put("ok", false);
            out.put("message", "Failed to delete asset: " + e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(out);
        }
    }

    public record OutlineSubchapter(
            String id,
            Integer number,
            String name,
            String href,
            boolean active) {
    }

    public record OutlineChapter(
            String id,
            Integer number,
            String name,
            String href,
            boolean active,
            List<OutlineSubchapter> subchapters) {
    }

    @Transactional
    @GetMapping(value = "/markdown-editor/outline", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> outline(
            @RequestParam(name = "courseId") String courseId,
            @RequestParam(name = "chapterId", required = false) String chapterId,
            @RequestParam(name = "subchapterId", required = false) String subchapterId) {
        Map<String, Object> out = new HashMap<>();
        String course = courseId != null ? courseId.trim() : "";
        String chapterRaw = chapterId != null ? chapterId.trim() : "";
        String subRaw = subchapterId != null ? subchapterId.trim() : "";
        if (course.isBlank()) {
            out.put("ok", false);
            out.put("message", "Missing courseId.");
            return ResponseEntity.badRequest().body(out);
        }
        if (courseRepository.findById(course).isEmpty()) {
            out.put("ok", false);
            out.put("message", "Course not found.");
            return ResponseEntity.badRequest().body(out);
        }

        String currentChapterStableId = "";
        String currentSubchapterStableId = "";
        if (!chapterRaw.isBlank()) {
            CourseChapterEntity currentCh = findChapter(course, chapterRaw);
            if (currentCh != null && currentCh.getId() != null) {
                currentChapterStableId = ensureChapterUid(currentCh);
                if (!subRaw.isBlank()) {
                    CourseSubchapterEntity currentSc = findSubchapter(currentCh.getId(), subRaw);
                    if (currentSc != null && currentSc.getId() != null) {
                        currentSubchapterStableId = ensureSubchapterUid(currentSc);
                    }
                }
            }
        }

        List<OutlineChapter> chapters = new ArrayList<>();
        for (CourseChapterEntity ch : courseChapterRepository.findByCourseIdOrderByChapterNumberAsc(course)) {
            if (ch == null || ch.getId() == null) {
                continue;
            }
            String chId = ensureChapterUid(ch);
            Integer chNumber = ch.getChapterNumber();
            String chName = ch.getName() != null ? ch.getName().trim() : "";

            List<OutlineSubchapter> subs = new ArrayList<>();
            for (CourseSubchapterEntity sc : courseSubchapterRepository.findByChapterIdOrderBySubchapterNumberAsc(ch.getId())) {
                if (sc == null || sc.getId() == null) {
                    continue;
                }
                String scId = ensureSubchapterUid(sc);
                Integer scNumber = sc.getSubchapterNumber();
                String scName = sc.getName() != null ? sc.getName().trim() : "";
                boolean active = !currentSubchapterStableId.isBlank() && currentSubchapterStableId.equals(scId);
                String href = "/markdown-editor?courseId="
                        + course
                        + "&chapterId="
                        + chId
                        + "&kind=subchapter&subchapterId="
                        + scId;
                subs.add(new OutlineSubchapter(scId, scNumber, scName, href, active));
            }

            boolean active = !currentChapterStableId.isBlank() && currentChapterStableId.equals(chId);
            String chapterHref = subs.isEmpty()
                    ? ""
                    : ("/markdown-editor?courseId="
                            + course
                            + "&chapterId="
                            + chId
                            + "&kind=subchapter&subchapterId="
                            + subs.getFirst().id());
            chapters.add(new OutlineChapter(chId, chNumber, chName, chapterHref, active, subs));
        }

        out.put("ok", true);
        out.put("chapters", chapters);
        out.put("currentChapterId", currentChapterStableId);
        out.put("currentSubchapterId", currentSubchapterStableId);
        return ResponseEntity.ok(out);
    }

    private record EditorTarget(
            String courseId,
            String chapterUidOrNumber,
            String subchapterUidOrNumber,
            String questionUidOrNumber,
            String kind,
            String markdown,
            String contextLabel,
            CourseChapterEntity chapterEntity,
            CourseSubchapterEntity subchapterEntity,
            CourseQuestionEntity questionEntity,
            String chapterStableId,
            String targetStableId) {
    }

    private EditorTarget resolveTarget(
            String courseIdRaw,
            String chapterIdRaw,
            String kindRaw,
            String subchapterIdRaw,
            String questionIdRaw) {
        String courseId = courseIdRaw != null ? courseIdRaw.trim() : "";
        String chapterId = chapterIdRaw != null ? chapterIdRaw.trim() : "";
        String kind = kindRaw != null ? kindRaw.trim().toLowerCase() : "";
        String subchapterId = subchapterIdRaw != null ? subchapterIdRaw.trim() : "";
        String questionId = questionIdRaw != null ? questionIdRaw.trim() : "";

        if (courseId.isBlank()) {
            throw new IllegalArgumentException("Missing courseId.");
        }
        if (chapterId.isBlank()) {
            throw new IllegalArgumentException("Missing chapterId.");
        }
        if (kind.isBlank()) {
            throw new IllegalArgumentException("Missing kind.");
        }

        CourseChapterEntity chapter = findChapter(courseId, chapterId);
        if (chapter == null || chapter.getId() == null) {
            throw new IllegalArgumentException("Chapter not found.");
        }
        String chapterStableId = ensureChapterUid(chapter);
        String courseTitle = courseRepository.findById(courseId)
                .map(c -> c.getTitle() != null ? c.getTitle().trim() : "")
                .filter(t -> !t.isBlank())
                .orElse(courseId);
        String chapterNumber = chapter.getChapterNumber() != null ? String.valueOf(chapter.getChapterNumber())
                : chapterId;
        String chapterName = chapter.getName() != null ? chapter.getName().trim() : "";
        String chapterPart = chapterName.isBlank()
                ? ("Chapter " + chapterNumber)
                : ("Chapter " + chapterNumber + ": " + chapterName);
        String baseLabel = "Course: " + courseTitle + " · " + chapterPart;

        if ("subchapter".equals(kind)) {
            if (subchapterId.isBlank()) {
                throw new IllegalArgumentException("Missing subchapterId.");
            }
            CourseSubchapterEntity sc = findSubchapter(chapter.getId(), subchapterId);
            if (sc == null || sc.getId() == null) {
                throw new IllegalArgumentException("Subchapter not found.");
            }
            String scStableId = ensureSubchapterUid(sc);
            String markdown = sc.getMarkdown() != null ? sc.getMarkdown() : "";
            String scNumber = sc.getSubchapterNumber() != null ? String.valueOf(sc.getSubchapterNumber())
                    : subchapterId;
            String scName = sc.getName() != null ? sc.getName().trim() : "";
            String subPart = scName.isBlank()
                    ? ("Subchapter " + scNumber)
                    : ("Subchapter " + scNumber + ": " + scName);
            String label = baseLabel + " · " + subPart;
            return new EditorTarget(
                    courseId,
                    chapterId,
                    subchapterId,
                    "",
                    kind,
                    markdown,
                    label,
                    chapter,
                    sc,
                    null,
                    chapterStableId,
                    scStableId);
        }

        if ("question".equals(kind) || "explanation".equals(kind)) {
            if (questionId.isBlank()) {
                throw new IllegalArgumentException("Missing questionId.");
            }
            CourseQuestionEntity q = findQuestion(chapter.getId(), questionId);
            if (q == null || q.getId() == null) {
                throw new IllegalArgumentException("Question not found.");
            }
            String qStableId = ensureQuestionUid(q);
            String markdown = "question".equals(kind)
                    ? (q.getQuestionMarkdown() != null ? q.getQuestionMarkdown() : "")
                    : (q.getExplanationMarkdown() != null ? q.getExplanationMarkdown() : "");
            String qNumber = q.getQuestionNumber() != null ? String.valueOf(q.getQuestionNumber()) : questionId;
            String qPart = "Question " + qNumber + ("explanation".equals(kind) ? " (Explanation)" : " (Question)");
            String label = baseLabel + " · " + qPart;
            return new EditorTarget(
                    courseId,
                    chapterId,
                    "",
                    questionId,
                    kind,
                    markdown,
                    label,
                    chapter,
                    null,
                    q,
                    chapterStableId,
                    qStableId);
        }

        throw new IllegalArgumentException("Invalid kind. Use subchapter, question, or explanation.");
    }

    private CourseChapterEntity findChapter(String courseId, String chapterId) {
        if (isDigits(chapterId)) {
            return courseChapterRepository.findByCourseIdAndChapterNumber(courseId, Integer.valueOf(chapterId));
        }
        CourseChapterEntity byUid = courseChapterRepository.findByCourseIdAndChapterUid(courseId, chapterId);
        if (byUid != null) {
            return byUid;
        }
        Long dbId = extractIdWithPrefix(chapterId, "chapter_");
        if (dbId != null) {
            CourseChapterEntity byId = courseChapterRepository.findById(dbId).orElse(null);
            if (byId != null && courseId.equals(byId.getCourseId())) {
                return byId;
            }
        }
        return null;
    }

    private CourseSubchapterEntity findSubchapter(Long chapterDbId, String subchapterId) {
        if (isDigits(subchapterId)) {
            return courseSubchapterRepository.findByChapterIdAndSubchapterNumber(chapterDbId,
                    Integer.valueOf(subchapterId));
        }
        CourseSubchapterEntity byUid = courseSubchapterRepository.findByChapterIdAndSubchapterUid(chapterDbId,
                subchapterId);
        if (byUid != null) {
            return byUid;
        }
        Long dbId = extractIdWithPrefix(subchapterId, "subchapter_");
        if (dbId != null) {
            CourseSubchapterEntity byId = courseSubchapterRepository.findById(dbId).orElse(null);
            if (byId != null && chapterDbId.equals(byId.getChapterId())) {
                return byId;
            }
        }
        return null;
    }

    private CourseQuestionEntity findQuestion(Long chapterDbId, String questionId) {
        if (isDigits(questionId)) {
            return courseQuestionRepository.findByChapterIdAndQuestionNumber(chapterDbId, Integer.valueOf(questionId));
        }
        CourseQuestionEntity byUid = courseQuestionRepository.findByChapterIdAndQuestionUid(chapterDbId, questionId);
        if (byUid != null) {
            return byUid;
        }
        Long dbId = extractIdWithPrefix(questionId, "question_");
        if (dbId != null) {
            CourseQuestionEntity byId = courseQuestionRepository.findById(dbId).orElse(null);
            if (byId != null && chapterDbId.equals(byId.getChapterId())) {
                return byId;
            }
        }
        return null;
    }

    private String ensureChapterUid(CourseChapterEntity ch) {
        String v = ch.getChapterUid() != null ? ch.getChapterUid().trim() : "";
        if (!v.isBlank()) {
            return v;
        }
        String uid = "chapter_" + ch.getId();
        ch.setChapterUid(uid);
        courseChapterRepository.save(ch);
        return uid;
    }

    private String ensureSubchapterUid(CourseSubchapterEntity sc) {
        String v = sc.getSubchapterUid() != null ? sc.getSubchapterUid().trim() : "";
        if (!v.isBlank()) {
            return v;
        }
        String uid = "subchapter_" + sc.getId();
        sc.setSubchapterUid(uid);
        courseSubchapterRepository.save(sc);
        return uid;
    }

    private String ensureQuestionUid(CourseQuestionEntity q) {
        String v = q.getQuestionUid() != null ? q.getQuestionUid().trim() : "";
        if (!v.isBlank()) {
            return v;
        }
        String uid = "question_" + q.getId();
        q.setQuestionUid(uid);
        courseQuestionRepository.save(q);
        return uid;
    }

    private static boolean isDigits(String v) {
        if (v == null || v.isBlank()) {
            return false;
        }
        for (int i = 0; i < v.length(); i++) {
            if (!Character.isDigit(v.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static Long extractIdWithPrefix(String value, String prefix) {
        if (value == null || prefix == null) {
            return null;
        }
        if (!value.startsWith(prefix)) {
            return null;
        }
        String tail = value.substring(prefix.length());
        if (!isDigits(tail)) {
            return null;
        }
        try {
            return Long.valueOf(tail);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String sanitizePathSegment(String value) {
        String v = (value == null ? "" : value).trim();
        if (v.isBlank()) {
            return "";
        }
        v = v.replaceAll("[^A-Za-z0-9_-]", "_");
        if (v.length() > 120) {
            v = v.substring(v.length() - 120);
        }
        return v;
    }

    private static String extensionFromContentTypeOrName(String contentType, String originalName) {
        String name = originalName != null ? originalName.toLowerCase() : "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            String ext = name.substring(dot);
            if (ext.matches("\\.[a-z0-9]{1,8}")) {
                if (ext.equals(".png") || ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".gif")
                        || ext.equals(".webp") || ext.equals(".svg")) {
                    return ext;
                }
            }
        }
        String ct = contentType != null ? contentType.toLowerCase() : "";
        if (ct.contains("png")) {
            return ".png";
        }
        if (ct.contains("jpeg") || ct.contains("jpg")) {
            return ".jpg";
        }
        if (ct.contains("gif")) {
            return ".gif";
        }
        if (ct.contains("webp")) {
            return ".webp";
        }
        if (ct.contains("svg")) {
            return ".svg";
        }
        return ".png";
    }
}
