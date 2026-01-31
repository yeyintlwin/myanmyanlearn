package com.barlarlar.myanmyanlearn.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barlarlar.myanmyanlearn.entity.CourseEntity;
import com.barlarlar.myanmyanlearn.entity.CourseChapterEntity;
import com.barlarlar.myanmyanlearn.entity.CourseQuestionEntity;
import com.barlarlar.myanmyanlearn.entity.CourseQuestionOptionEntity;
import com.barlarlar.myanmyanlearn.entity.CourseQuestionSlotEntity;
import com.barlarlar.myanmyanlearn.entity.CourseQuestionSlotOptionEntity;
import com.barlarlar.myanmyanlearn.entity.CourseSubchapterEntity;
import com.barlarlar.myanmyanlearn.repository.CourseChapterRepository;
import com.barlarlar.myanmyanlearn.repository.CourseQuestionOptionRepository;
import com.barlarlar.myanmyanlearn.repository.CourseQuestionRepository;
import com.barlarlar.myanmyanlearn.repository.CourseQuestionSlotOptionRepository;
import com.barlarlar.myanmyanlearn.repository.CourseQuestionSlotRepository;
import com.barlarlar.myanmyanlearn.repository.CourseRepository;
import com.barlarlar.myanmyanlearn.repository.CourseSubchapterRepository;
import com.barlarlar.myanmyanlearn.service.storage.StorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminCourseDbService {
    private final ObjectMapper objectMapper;
    private final CourseRepository courseRepository;
    private final CourseChapterRepository courseChapterRepository;
    private final CourseSubchapterRepository courseSubchapterRepository;
    private final CourseQuestionRepository courseQuestionRepository;
    private final CourseQuestionSlotRepository courseQuestionSlotRepository;
    private final CourseQuestionSlotOptionRepository courseQuestionSlotOptionRepository;
    private final CourseQuestionOptionRepository courseQuestionOptionRepository;
    private final EntityManager entityManager;
    private final StorageService storageService;

    public AdminCourseDbService(
            ObjectMapper objectMapper,
            CourseRepository courseRepository,
            CourseChapterRepository courseChapterRepository,
            CourseSubchapterRepository courseSubchapterRepository,
            CourseQuestionRepository courseQuestionRepository,
            CourseQuestionSlotRepository courseQuestionSlotRepository,
            CourseQuestionSlotOptionRepository courseQuestionSlotOptionRepository,
            CourseQuestionOptionRepository courseQuestionOptionRepository,
            EntityManager entityManager,
            StorageService storageService) {
        this.objectMapper = objectMapper;
        this.courseRepository = courseRepository;
        this.courseChapterRepository = courseChapterRepository;
        this.courseSubchapterRepository = courseSubchapterRepository;
        this.courseQuestionRepository = courseQuestionRepository;
        this.courseQuestionSlotRepository = courseQuestionSlotRepository;
        this.courseQuestionSlotOptionRepository = courseQuestionSlotOptionRepository;
        this.courseQuestionOptionRepository = courseQuestionOptionRepository;
        this.entityManager = entityManager;
        this.storageService = storageService;
    }

    public record TargetStudents(List<String> schoolYears, List<String> classes) {
    }

    public record CourseSummary(
            String id,
            String title,
            String description,
            String language,
            boolean published,
            TargetStudents targetStudents,
            String coverImageDataUrl) {
    }

    public record SlotOption(int optionIndex, String optionContent, boolean isCorrect) {
    }

    public record EditorQuestion(
            String id,
            int questionNumber,
            String questionMarkdown,
            String explanationMarkdown,
            List<List<SlotOption>> slotOptions) {
    }

    public record EditorSubchapter(String id, int number, String name, String markdown) {
    }

    public record EditorChapter(
            String id,
            int number,
            String name,
            List<EditorSubchapter> subchapters,
            List<EditorQuestion> questions) {
    }

    public record CourseEditor(
            String id,
            String title,
            String description,
            String language,
            boolean published,
            TargetStudents targetStudents,
            String coverImageDataUrl,
            List<EditorChapter> chapters) {
    }

    public CourseEditor loadCourseEditor(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            return null;
        }
        CourseEntity course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return null;
        }

        TargetStudents targetStudents = parseTargetStudents(course.getTargetStudentsJson());
        boolean published = course.getPublished() != null && course.getPublished();

        List<CourseChapterEntity> chapters = courseChapterRepository.findByCourseIdOrderByChapterNumberAsc(courseId);
        List<EditorChapter> editorChapters = new ArrayList<>();
        for (CourseChapterEntity ch : chapters) {
            if (ch == null || ch.getId() == null) {
                continue;
            }
            long chapterId = ch.getId();
            String chapterUid = normalizeUid(ch.getChapterUid(), "chapter", chapterId);
            if (ch.getChapterUid() == null || ch.getChapterUid().isBlank()) {
                ch.setChapterUid(chapterUid);
                courseChapterRepository.save(ch);
            }

            List<CourseSubchapterEntity> subs = courseSubchapterRepository.findByChapterIdOrderBySubchapterNumberAsc(
                    chapterId);
            List<EditorSubchapter> editorSubs = new ArrayList<>();
            for (CourseSubchapterEntity sc : subs) {
                if (sc == null || sc.getId() == null) {
                    continue;
                }
                String subUid = normalizeUid(sc.getSubchapterUid(), "subchapter", sc.getId());
                if (sc.getSubchapterUid() == null || sc.getSubchapterUid().isBlank()) {
                    sc.setSubchapterUid(subUid);
                    courseSubchapterRepository.save(sc);
                }
                editorSubs.add(new EditorSubchapter(
                        subUid,
                        sc.getSubchapterNumber() != null ? sc.getSubchapterNumber() : 1,
                        sc.getName() != null ? sc.getName() : "",
                        sc.getMarkdown() != null ? sc.getMarkdown() : ""));
            }

            List<CourseQuestionEntity> qs = courseQuestionRepository.findByChapterIdOrderByQuestionNumberAsc(chapterId);
            List<EditorQuestion> editorQuestions = new ArrayList<>();
            for (CourseQuestionEntity q : qs) {
                if (q == null || q.getId() == null) {
                    continue;
                }
                String qUid = normalizeUid(q.getQuestionUid(), "question", q.getId());
                if (q.getQuestionUid() == null || q.getQuestionUid().isBlank()) {
                    q.setQuestionUid(qUid);
                    courseQuestionRepository.save(q);
                }
                List<List<SlotOption>> slotOptions = loadSlotOptionsForQuestion(q.getId());
                editorQuestions.add(new EditorQuestion(
                        qUid,
                        q.getQuestionNumber() != null ? q.getQuestionNumber() : 1,
                        q.getQuestionMarkdown() != null ? q.getQuestionMarkdown() : "",
                        q.getExplanationMarkdown() != null ? q.getExplanationMarkdown() : "",
                        slotOptions));
            }

            editorChapters.add(new EditorChapter(
                    chapterUid,
                    ch.getChapterNumber() != null ? ch.getChapterNumber() : 1,
                    ch.getName() != null ? ch.getName() : "",
                    editorSubs,
                    editorQuestions));
        }

        return new CourseEditor(
                course.getCourseId(),
                course.getTitle(),
                course.getDescription(),
                course.getLanguage(),
                published,
                targetStudents,
                course.getCoverImageUrl(),
                editorChapters);
    }

    @Transactional
    public void upsertCourseMeta(CourseSummary meta) {
        if (meta == null || meta.id() == null || meta.id().isBlank()) {
            throw new IllegalArgumentException("course id is required");
        }
        String targetStudentsJson = writeTargetStudents(meta.targetStudents());
        String coverUrl = normalizeCoverUrl(meta.coverImageDataUrl());

        CourseEntity entity = courseRepository.findById(meta.id()).orElseGet(() -> {
            CourseEntity created = new CourseEntity();
            created.setCourseId(meta.id());
            return created;
        });
        entity.setTitle(meta.title() != null ? meta.title() : "Untitled course");
        entity.setDescription(meta.description());
        entity.setLanguage(meta.language());
        entity.setCoverImageUrl(coverUrl);
        entity.setTargetStudentsJson(targetStudentsJson);
        entity.setPublished(meta.published());
        courseRepository.save(entity);
    }

    @Transactional
    public void deleteCourse(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            return;
        }
        deleteCourseChildren(courseId);
        courseRepository.deleteById(courseId);
    }

    @Transactional
    public String uploadCourseCoverImage(String courseId, MultipartFile file) throws IOException {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("course id is required");
        }
        CourseEntity course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            throw new IllegalArgumentException("Course not found.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded.");
        }
        if (file.getSize() > 5L * 1024L * 1024L) {
            throw new IllegalArgumentException("Image must be 5MB or smaller.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed.");
        }

        String ext = extensionFromContentTypeOrName(contentType, file.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        String key = "courses/" + sanitizePathSegment(courseId) + "/cover/" + filename;

        StorageService.StoredObject stored = storageService.put(key, file);
        String url = stored != null ? stored.url() : null;
        course.setCoverImageUrl(normalizeCoverUrl(url));
        courseRepository.save(course);
        return course.getCoverImageUrl();
    }

    @Transactional
    public void saveCourseEditor(CourseEditor editor) {
        if (editor == null || editor.id() == null || editor.id().isBlank()) {
            throw new IllegalArgumentException("course id is required");
        }

        CourseSummary meta = new CourseSummary(
                editor.id(),
                editor.title(),
                editor.description(),
                editor.language(),
                editor.published(),
                editor.targetStudents(),
                editor.coverImageDataUrl());
        upsertCourseMeta(meta);
        courseRepository.findByCourseIdForUpdate(editor.id());
        deleteCourseChildren(editor.id());
        entityManager.flush();
        entityManager.clear();

        List<EditorChapter> chapters = editor.chapters() != null ? editor.chapters() : List.of();
        for (EditorChapter ch : chapters) {
            CourseChapterEntity chEntity = new CourseChapterEntity();
            chEntity.setCourseId(editor.id());
            chEntity.setChapterUid(ch != null ? ch.id() : null);
            chEntity.setChapterNumber(ch != null ? ch.number() : 1);
            chEntity.setName(ch != null && ch.name() != null ? ch.name() : "");
            CourseChapterEntity savedChapter = courseChapterRepository.save(chEntity);
            long chapterId = savedChapter.getId();

            List<EditorSubchapter> subs = ch != null && ch.subchapters() != null ? ch.subchapters() : List.of();
            for (EditorSubchapter sc : subs) {
                CourseSubchapterEntity scEntity = new CourseSubchapterEntity();
                scEntity.setChapterId(chapterId);
                scEntity.setSubchapterUid(sc != null ? sc.id() : null);
                scEntity.setSubchapterNumber(sc != null ? sc.number() : 1);
                scEntity.setName(sc != null && sc.name() != null ? sc.name() : "");
                scEntity.setMarkdown(sc != null && sc.markdown() != null ? sc.markdown() : "");
                courseSubchapterRepository.save(scEntity);
            }

            List<EditorQuestion> qs = ch != null && ch.questions() != null ? ch.questions() : List.of();
            for (EditorQuestion q : qs) {
                CourseQuestionEntity qEntity = new CourseQuestionEntity();
                qEntity.setChapterId(chapterId);
                qEntity.setQuestionUid(q != null ? q.id() : null);
                qEntity.setQuestionNumber(q != null ? q.questionNumber() : 1);
                qEntity.setQuestionMarkdown(q != null && q.questionMarkdown() != null ? q.questionMarkdown() : "");
                qEntity.setExplanationMarkdown(
                        q != null && q.explanationMarkdown() != null ? q.explanationMarkdown() : "");
                CourseQuestionEntity savedQuestion = courseQuestionRepository.save(qEntity);
                long questionId = savedQuestion.getId();

                List<List<SlotOption>> slots = q != null && q.slotOptions() != null ? q.slotOptions() : List.of();
                for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
                    CourseQuestionSlotEntity slotEntity = new CourseQuestionSlotEntity();
                    slotEntity.setQuestionId(questionId);
                    slotEntity.setSlotIndex(slotIndex);
                    CourseQuestionSlotEntity savedSlot = courseQuestionSlotRepository.save(slotEntity);

                    List<SlotOption> options = slots.get(slotIndex) != null ? slots.get(slotIndex) : List.of();
                    for (SlotOption opt : options) {
                        if (opt == null) {
                            continue;
                        }
                        CourseQuestionSlotOptionEntity optEntity = new CourseQuestionSlotOptionEntity();
                        optEntity.setQuestionSlotId(savedSlot.getId());
                        optEntity.setOptionIndex(opt.optionIndex());
                        optEntity.setOptionContent(opt.optionContent() != null ? opt.optionContent() : "");
                        optEntity.setCorrect(opt.isCorrect());
                        courseQuestionSlotOptionRepository.save(optEntity);
                    }
                }
            }
        }
    }

    private void deleteCourseChildren(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            return;
        }
        List<CourseChapterEntity> chapters = courseChapterRepository.findByCourseIdOrderByChapterNumberAsc(courseId);
        for (CourseChapterEntity ch : chapters) {
            if (ch == null || ch.getId() == null) {
                continue;
            }
            Long chapterId = ch.getId();

            List<CourseQuestionEntity> questions = courseQuestionRepository.findByChapterIdOrderByQuestionNumberAsc(
                    chapterId);
            for (CourseQuestionEntity q : questions) {
                if (q == null || q.getId() == null) {
                    continue;
                }
                Long qId = q.getId();

                List<CourseQuestionSlotEntity> slots = courseQuestionSlotRepository.findByQuestionIdOrderBySlotIndexAsc(
                        qId);
                for (CourseQuestionSlotEntity s : slots) {
                    if (s == null || s.getId() == null) {
                        continue;
                    }
                    List<CourseQuestionSlotOptionEntity> slotOpts = courseQuestionSlotOptionRepository
                            .findByQuestionSlotIdOrderByOptionIndexAsc(s.getId());
                    if (!slotOpts.isEmpty()) {
                        courseQuestionSlotOptionRepository.deleteAll(slotOpts);
                    }
                }
                if (!slots.isEmpty()) {
                    courseQuestionSlotRepository.deleteAll(slots);
                }

                List<CourseQuestionOptionEntity> legacy = courseQuestionOptionRepository
                        .findByQuestionIdOrderByOptionIndexAsc(qId);
                if (!legacy.isEmpty()) {
                    courseQuestionOptionRepository.deleteAll(legacy);
                }
            }
            if (!questions.isEmpty()) {
                courseQuestionRepository.deleteAll(questions);
            }

            List<CourseSubchapterEntity> subs = courseSubchapterRepository.findByChapterIdOrderBySubchapterNumberAsc(
                    chapterId);
            if (!subs.isEmpty()) {
                courseSubchapterRepository.deleteAll(subs);
            }
        }
        if (!chapters.isEmpty()) {
            courseChapterRepository.deleteAll(chapters);
        }
    }

    private List<List<SlotOption>> loadSlotOptionsForQuestion(Long questionId) {
        if (questionId == null) {
            return List.of();
        }
        List<CourseQuestionSlotEntity> slots = courseQuestionSlotRepository.findByQuestionIdOrderBySlotIndexAsc(
                questionId);
        if (!slots.isEmpty()) {
            List<List<SlotOption>> out = new ArrayList<>();
            for (CourseQuestionSlotEntity s : slots) {
                if (s == null || s.getId() == null) {
                    continue;
                }
                int idx = s.getSlotIndex() != null ? s.getSlotIndex() : 0;
                while (out.size() <= idx) {
                    out.add(new ArrayList<>());
                }
                List<CourseQuestionSlotOptionEntity> opts = courseQuestionSlotOptionRepository
                        .findByQuestionSlotIdOrderByOptionIndexAsc(s.getId());
                List<SlotOption> mapped = new ArrayList<>();
                for (CourseQuestionSlotOptionEntity o : opts) {
                    if (o == null) {
                        continue;
                    }
                    mapped.add(new SlotOption(
                            o.getOptionIndex() != null ? o.getOptionIndex() : 0,
                            o.getOptionContent() != null ? o.getOptionContent() : "",
                            o.getCorrect() != null && o.getCorrect()));
                }
                out.set(idx, mapped);
            }
            return out;
        }

        List<CourseQuestionOptionEntity> legacy = courseQuestionOptionRepository
                .findByQuestionIdOrderByOptionIndexAsc(questionId);
        if (legacy.isEmpty()) {
            return List.of();
        }
        List<SlotOption> mapped = new ArrayList<>();
        for (CourseQuestionOptionEntity o : legacy) {
            if (o == null) {
                continue;
            }
            mapped.add(new SlotOption(
                    o.getOptionIndex() != null ? o.getOptionIndex() : 0,
                    o.getOptionContent() != null ? o.getOptionContent() : "",
                    o.getCorrect() != null && o.getCorrect()));
        }
        return List.of(mapped);
    }

    private String normalizeUid(String uid, String prefix, Object fallbackId) {
        if (uid != null && !uid.isBlank()) {
            return uid;
        }
        String p = prefix != null && !prefix.isBlank() ? prefix : "id";
        return p + "_" + Objects.toString(fallbackId, "0");
    }

    private TargetStudents parseTargetStudents(String json) {
        if (json == null || json.isBlank()) {
            return new TargetStudents(List.of(), List.of());
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            List<String> years = normalizeStringList(map.get("schoolYears"));
            List<String> classes = normalizeStringList(map.get("classes"));
            return new TargetStudents(years, classes);
        } catch (Exception e) {
            return new TargetStudents(List.of(), List.of());
        }
    }

    private String writeTargetStudents(TargetStudents ts) {
        if (ts == null) {
            return null;
        }
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("schoolYears", ts.schoolYears() != null ? ts.schoolYears() : List.of());
            map.put("classes", ts.classes() != null ? ts.classes() : List.of());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> normalizeStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                String s = o instanceof String str ? str.trim() : Objects.toString(o, "").trim();
                if (!s.isBlank()) {
                    out.add(s);
                }
            }
            return out;
        }
        if (value instanceof String s && !s.isBlank()) {
            return List.of(s.trim());
        }
        return List.of();
    }

    private String normalizeCoverUrl(String coverImageDataUrl) {
        if (coverImageDataUrl == null || coverImageDataUrl.isBlank()) {
            return null;
        }
        String v = coverImageDataUrl.trim();
        if (v.startsWith("http://") || v.startsWith("https://")) {
            return v;
        }
        if (v.startsWith("/")) {
            return v;
        }
        return null;
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
