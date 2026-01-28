package com.barlarlar.myanmyanlearn.service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AdminCourseDbService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AdminCourseDbService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
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

    public List<CourseSummary> listCourses() {
        String sql = """
                SELECT
                  course_id,
                  title,
                  description,
                  language,
                  cover_image_url,
                  target_students_json,
                  published
                FROM courses
                ORDER BY updated_at DESC, created_at DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String id = rs.getString("course_id");
            String title = rs.getString("title");
            String description = rs.getString("description");
            String language = rs.getString("language");
            String cover = rs.getString("cover_image_url");
            String targetStudentsJson = rs.getString("target_students_json");
            boolean published = rs.getInt("published") == 1;
            TargetStudents targetStudents = parseTargetStudents(targetStudentsJson);
            return new CourseSummary(id, title, description, language, published, targetStudents, cover);
        });
    }

    public CourseEditor loadCourseEditor(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            return null;
        }

        String courseSql = """
                SELECT
                  course_id,
                  title,
                  description,
                  language,
                  cover_image_url,
                  target_students_json,
                  published
                FROM courses
                WHERE course_id = ?
                """;

        List<CourseEditor> courseRows = jdbcTemplate.query(courseSql, (rs, rowNum) -> {
            String id = rs.getString("course_id");
            String title = rs.getString("title");
            String description = rs.getString("description");
            String language = rs.getString("language");
            String cover = rs.getString("cover_image_url");
            String targetStudentsJson = rs.getString("target_students_json");
            boolean published = rs.getInt("published") == 1;
            TargetStudents targetStudents = parseTargetStudents(targetStudentsJson);
            return new CourseEditor(id, title, description, language, published, targetStudents, cover, List.of());
        }, courseId);

        if (courseRows.isEmpty()) {
            return null;
        }

        CourseEditor base = courseRows.get(0);

        String chapterSql = """
                SELECT id, chapter_uid, chapter_number, name
                FROM course_chapters
                WHERE course_id = ?
                ORDER BY chapter_number ASC, id ASC
                """;
        List<Map<String, Object>> chapters = jdbcTemplate.queryForList(chapterSql, courseId);
        List<Long> chapterIds = chapters.stream()
                .map(r -> ((Number) r.get("id")).longValue())
                .collect(Collectors.toList());

        Map<Long, List<EditorSubchapter>> subchaptersByChapterId = new HashMap<>();
        if (!chapterIds.isEmpty()) {
            String in = chapterIds.stream().map(x -> "?").collect(Collectors.joining(","));
            String subSql = "SELECT chapter_id, subchapter_uid, subchapter_number, name, markdown "
                    + "FROM course_subchapters "
                    + "WHERE chapter_id IN (" + in + ") "
                    + "ORDER BY chapter_id ASC, subchapter_number ASC, id ASC";
            List<Map<String, Object>> subs = jdbcTemplate.queryForList(subSql, chapterIds.toArray());
            for (Map<String, Object> row : subs) {
                Long chapterId = ((Number) row.get("chapter_id")).longValue();
                String uid = Objects.toString(row.get("subchapter_uid"), null);
                int number = ((Number) row.get("subchapter_number")).intValue();
                String name = Objects.toString(row.get("name"), "");
                String markdown = Objects.toString(row.get("markdown"), "");
                subchaptersByChapterId.computeIfAbsent(chapterId, k -> new ArrayList<>())
                        .add(new EditorSubchapter(uid, number, name, markdown));
            }
        }

        Map<Long, List<Long>> questionIdsByChapterId = new HashMap<>();
        Map<Long, EditorQuestion> questionById = new HashMap<>();
        if (!chapterIds.isEmpty()) {
            String in = chapterIds.stream().map(x -> "?").collect(Collectors.joining(","));
            String qSql = "SELECT id, chapter_id, question_uid, question_number, question_markdown, explanation_markdown "
                    + "FROM course_questions "
                    + "WHERE chapter_id IN (" + in + ") "
                    + "ORDER BY chapter_id ASC, question_number ASC, id ASC";
            List<Map<String, Object>> qs = jdbcTemplate.queryForList(qSql, chapterIds.toArray());
            for (Map<String, Object> row : qs) {
                Long qId = ((Number) row.get("id")).longValue();
                Long chapterId = ((Number) row.get("chapter_id")).longValue();
                String uid = Objects.toString(row.get("question_uid"), null);
                int number = ((Number) row.get("question_number")).intValue();
                String qMd = Objects.toString(row.get("question_markdown"), "");
                String eMd = Objects.toString(row.get("explanation_markdown"), "");
                questionIdsByChapterId.computeIfAbsent(chapterId, k -> new ArrayList<>()).add(qId);
                questionById.put(qId, new EditorQuestion(uid, number, qMd, eMd, List.of()));
            }
        }

        Map<Long, List<List<SlotOption>>> slotOptionsByQuestionId;
        try {
            slotOptionsByQuestionId = loadSlotOptionsByQuestionIds(questionById.keySet());
        } catch (DataAccessException e) {
            slotOptionsByQuestionId = loadLegacyQuestionOptionsByQuestionIds(questionById.keySet());
        }

        List<EditorChapter> editorChapters = new ArrayList<>();
        for (Map<String, Object> ch : chapters) {
            Long chapterId = ((Number) ch.get("id")).longValue();
            String uid = Objects.toString(ch.get("chapter_uid"), null);
            int number = ((Number) ch.get("chapter_number")).intValue();
            String name = Objects.toString(ch.get("name"), "");
            List<EditorSubchapter> subs = subchaptersByChapterId.getOrDefault(chapterId, List.of());
            List<Long> qIds = questionIdsByChapterId.getOrDefault(chapterId, List.of());
            List<EditorQuestion> qs = new ArrayList<>(qIds.size());
            for (Long qId : qIds) {
                EditorQuestion q = questionById.get(qId);
                if (q == null) {
                    continue;
                }
                List<List<SlotOption>> slots = slotOptionsByQuestionId.getOrDefault(qId, List.of());
                qs.add(new EditorQuestion(q.id(), q.questionNumber(), q.questionMarkdown(), q.explanationMarkdown(),
                        slots));
            }
            editorChapters.add(new EditorChapter(uid, number, name, subs, qs));
        }

        return new CourseEditor(
                base.id(),
                base.title(),
                base.description(),
                base.language(),
                base.published(),
                base.targetStudents(),
                base.coverImageDataUrl(),
                editorChapters);
    }

    @Transactional
    public void upsertCourseMeta(CourseSummary meta) {
        if (meta == null || meta.id() == null || meta.id().isBlank()) {
            throw new IllegalArgumentException("course id is required");
        }
        String targetStudentsJson = writeTargetStudents(meta.targetStudents());
        String coverUrl = normalizeCoverUrl(meta.coverImageDataUrl());
        jdbcTemplate.update("""
                INSERT INTO courses (
                  course_id, title, description, language, cover_image_url, target_students_json, published
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  title = VALUES(title),
                  description = VALUES(description),
                  language = VALUES(language),
                  cover_image_url = VALUES(cover_image_url),
                  target_students_json = VALUES(target_students_json),
                  published = VALUES(published)
                """,
                meta.id(),
                meta.title(),
                meta.description(),
                meta.language(),
                coverUrl,
                targetStudentsJson,
                meta.published() ? 1 : 0);
    }

    @Transactional
    public void deleteCourse(String courseId) {
        jdbcTemplate.update("DELETE FROM courses WHERE course_id = ?", courseId);
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

        jdbcTemplate.update("DELETE FROM course_chapters WHERE course_id = ?", editor.id());

        List<EditorChapter> chapters = editor.chapters() != null ? editor.chapters() : List.of();
        for (EditorChapter ch : chapters) {
            long chapterId = insertChapter(editor.id(), ch);
            List<EditorSubchapter> subs = ch.subchapters() != null ? ch.subchapters() : List.of();
            for (EditorSubchapter sc : subs) {
                insertSubchapter(chapterId, sc);
            }

            List<EditorQuestion> qs = ch.questions() != null ? ch.questions() : List.of();
            for (EditorQuestion q : qs) {
                long questionId = insertQuestion(chapterId, q);
                List<List<SlotOption>> slots = q.slotOptions() != null ? q.slotOptions() : List.of();
                boolean savedSlots = false;
                if (questionId > 0 && !slots.isEmpty()) {
                    try {
                        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
                            long slotId = insertSlot(questionId, slotIndex);
                            List<SlotOption> options = slots.get(slotIndex) != null ? slots.get(slotIndex) : List.of();
                            for (SlotOption opt : options) {
                                insertSlotOption(slotId, opt);
                            }
                        }
                        savedSlots = true;
                    } catch (DataAccessException e) {
                        savedSlots = false;
                    }
                }

                if (!savedSlots && questionId > 0 && !slots.isEmpty()) {
                    List<SlotOption> legacyOptions = slots.get(0) != null ? slots.get(0) : List.of();
                    for (SlotOption opt : legacyOptions) {
                        insertLegacyQuestionOption(questionId, opt);
                    }
                }
            }
        }
    }

    private long insertChapter(String courseId, EditorChapter ch) {
        String uid = ch != null ? ch.id() : null;
        int number = ch != null ? ch.number() : 1;
        String name = ch != null ? ch.name() : "";

        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO course_chapters (course_id, chapter_uid, chapter_number, name) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, courseId);
            ps.setString(2, uid);
            ps.setInt(3, number);
            ps.setString(4, name);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key != null ? key.longValue() : fetchChapterId(courseId, number);
    }

    private long fetchChapterId(String courseId, int chapterNumber) {
        Long v = jdbcTemplate.queryForObject(
                "SELECT id FROM course_chapters WHERE course_id = ? AND chapter_number = ?",
                Long.class,
                courseId,
                chapterNumber);
        return v != null ? v : 0L;
    }

    private void insertSubchapter(long chapterId, EditorSubchapter sc) {
        jdbcTemplate.update("""
                INSERT INTO course_subchapters (
                  chapter_id, subchapter_uid, subchapter_number, name, markdown
                ) VALUES (?, ?, ?, ?, ?)
                """,
                chapterId,
                sc.id(),
                sc.number(),
                sc.name(),
                sc.markdown());
    }

    private long insertQuestion(long chapterId, EditorQuestion q) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    """
                            INSERT INTO course_questions (
                              chapter_id, question_uid, question_number, question_markdown, explanation_markdown
                            ) VALUES (?, ?, ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, chapterId);
            ps.setString(2, q.id());
            ps.setInt(3, q.questionNumber());
            ps.setString(4, q.questionMarkdown());
            ps.setString(5, q.explanationMarkdown());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key != null ? key.longValue() : 0L;
    }

    private long insertSlot(long questionId, int slotIndex) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO course_question_slots (question_id, slot_index) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, questionId);
            ps.setInt(2, slotIndex);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key != null ? key.longValue() : 0L;
    }

    private void insertSlotOption(long slotId, SlotOption opt) {
        jdbcTemplate.update("""
                INSERT INTO course_question_slot_options (
                  question_slot_id, option_index, option_content, is_correct
                ) VALUES (?, ?, ?, ?)
                """,
                slotId,
                opt.optionIndex(),
                opt.optionContent(),
                opt.isCorrect() ? 1 : 0);
    }

    private void insertLegacyQuestionOption(long questionId, SlotOption opt) {
        jdbcTemplate.update("""
                INSERT INTO course_question_options (
                  question_id, option_index, option_content, is_correct
                ) VALUES (?, ?, ?, ?)
                """,
                questionId,
                opt.optionIndex(),
                opt.optionContent(),
                opt.isCorrect() ? 1 : 0);
    }

    private Map<Long, List<List<SlotOption>>> loadSlotOptionsByQuestionIds(java.util.Set<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }

        List<Long> qIds = new ArrayList<>(questionIds);
        String in = qIds.stream().map(x -> "?").collect(Collectors.joining(","));

        String slotsSql = "SELECT id, question_id, slot_index "
                + "FROM course_question_slots "
                + "WHERE question_id IN (" + in + ") "
                + "ORDER BY question_id ASC, slot_index ASC, id ASC";
        List<Map<String, Object>> slots = jdbcTemplate.queryForList(slotsSql, qIds.toArray());

        Map<Long, List<Long>> slotIdsByQuestion = new LinkedHashMap<>();
        Map<Long, Integer> slotIndexBySlotId = new HashMap<>();
        for (Map<String, Object> row : slots) {
            Long slotId = ((Number) row.get("id")).longValue();
            Long qId = ((Number) row.get("question_id")).longValue();
            int slotIndex = ((Number) row.get("slot_index")).intValue();
            slotIndexBySlotId.put(slotId, slotIndex);
            slotIdsByQuestion.computeIfAbsent(qId, k -> new ArrayList<>()).add(slotId);
        }

        List<Long> slotIds = slots.stream().map(r -> ((Number) r.get("id")).longValue()).collect(Collectors.toList());
        Map<Long, List<SlotOption>> optionsBySlotId = new HashMap<>();
        if (!slotIds.isEmpty()) {
            String sin = slotIds.stream().map(x -> "?").collect(Collectors.joining(","));
            String optSql = "SELECT question_slot_id, option_index, option_content, is_correct "
                    + "FROM course_question_slot_options "
                    + "WHERE question_slot_id IN (" + sin + ") "
                    + "ORDER BY question_slot_id ASC, option_index ASC";
            List<Map<String, Object>> opts = jdbcTemplate.queryForList(optSql, slotIds.toArray());
            for (Map<String, Object> row : opts) {
                Long slotId = ((Number) row.get("question_slot_id")).longValue();
                int optionIndex = ((Number) row.get("option_index")).intValue();
                String content = Objects.toString(row.get("option_content"), "");
                boolean correct = ((Number) row.get("is_correct")).intValue() == 1;
                optionsBySlotId.computeIfAbsent(slotId, k -> new ArrayList<>())
                        .add(new SlotOption(optionIndex, content, correct));
            }
        }

        Map<Long, List<List<SlotOption>>> out = new HashMap<>();
        for (Map.Entry<Long, List<Long>> e : slotIdsByQuestion.entrySet()) {
            Long qId = e.getKey();
            List<Long> ids = e.getValue();
            int maxIndex = -1;
            for (Long sid : ids) {
                Integer idx = slotIndexBySlotId.get(sid);
                if (idx != null) {
                    maxIndex = Math.max(maxIndex, idx);
                }
            }
            List<List<SlotOption>> slotsForQuestion = new ArrayList<>();
            for (int i = 0; i <= maxIndex; i++) {
                slotsForQuestion.add(new ArrayList<>());
            }
            for (Long sid : ids) {
                Integer idx = slotIndexBySlotId.get(sid);
                if (idx == null) {
                    continue;
                }
                List<SlotOption> opts = optionsBySlotId.getOrDefault(sid, List.of());
                slotsForQuestion.set(idx, new ArrayList<>(opts));
            }
            out.put(qId, slotsForQuestion);
        }

        return out;
    }

    private Map<Long, List<List<SlotOption>>> loadLegacyQuestionOptionsByQuestionIds(java.util.Set<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }

        List<Long> qIds = new ArrayList<>(questionIds);
        String in = qIds.stream().map(x -> "?").collect(Collectors.joining(","));

        String optSql = "SELECT question_id, option_index, option_content, is_correct "
                + "FROM course_question_options "
                + "WHERE question_id IN (" + in + ") "
                + "ORDER BY question_id ASC, option_index ASC, id ASC";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(optSql, qIds.toArray());

        Map<Long, List<List<SlotOption>>> out = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long qId = ((Number) row.get("question_id")).longValue();
            int optionIndex = ((Number) row.get("option_index")).intValue();
            String content = Objects.toString(row.get("option_content"), "");
            boolean correct = ((Number) row.get("is_correct")).intValue() == 1;
            out.computeIfAbsent(qId, k -> new ArrayList<>(List.of(new ArrayList<>())))
                    .get(0)
                    .add(new SlotOption(optionIndex, content, correct));
        }

        return out;
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
        return null;
    }
}
