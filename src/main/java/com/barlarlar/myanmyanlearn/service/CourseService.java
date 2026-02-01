package com.barlarlar.myanmyanlearn.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.barlarlar.myanmyanlearn.entity.CourseChapterEntity;
import com.barlarlar.myanmyanlearn.entity.CourseEntity;
import com.barlarlar.myanmyanlearn.entity.CourseSubchapterEntity;
import com.barlarlar.myanmyanlearn.model.Course;
import com.barlarlar.myanmyanlearn.model.Content;
import com.barlarlar.myanmyanlearn.model.Question;
import com.barlarlar.myanmyanlearn.model.QuestionOption;
import com.barlarlar.myanmyanlearn.model.Subcontent;
import com.barlarlar.myanmyanlearn.repository.CourseChapterRepository;
import com.barlarlar.myanmyanlearn.repository.CourseRepository;
import com.barlarlar.myanmyanlearn.repository.CourseQuestionOptionRepository;
import com.barlarlar.myanmyanlearn.repository.CourseQuestionRepository;
import com.barlarlar.myanmyanlearn.repository.CourseQuestionSlotOptionRepository;
import com.barlarlar.myanmyanlearn.repository.CourseQuestionSlotRepository;
import com.barlarlar.myanmyanlearn.repository.CourseSubchapterRepository;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseChapterRepository courseChapterRepository;
    private final CourseSubchapterRepository courseSubchapterRepository;
    private final CourseQuestionRepository courseQuestionRepository;
    private final CourseQuestionOptionRepository courseQuestionOptionRepository;
    private final CourseQuestionSlotRepository courseQuestionSlotRepository;
    private final CourseQuestionSlotOptionRepository courseQuestionSlotOptionRepository;

    public List<Course> getAllCoursesFromDatabase() {
        List<CourseEntity> entities = courseRepository.findAll();
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        List<Course> out = new ArrayList<>();
        for (CourseEntity entity : entities) {
            if (entity == null || entity.getCourseId() == null || entity.getCourseId().isBlank()) {
                continue;
            }

            Course course = new Course();
            course.setId(entity.getCourseId());
            course.setTitle(entity.getTitle());
            course.setDescription(entity.getDescription());
            course.setLanguage(entity.getLanguage());
            course.setLogo(entity.getCoverImageUrl());

            List<CourseChapterEntity> chapters = courseChapterRepository
                    .findByCourseIdOrderByChapterNumberAsc(entity.getCourseId());
            if (chapters != null && !chapters.isEmpty()) {
                List<Content> contents = new ArrayList<>();
                for (CourseChapterEntity chapter : chapters) {
                    if (chapter == null || chapter.getChapterNumber() == null) {
                        continue;
                    }
                    Content content = new Content();
                    content.setOrder(chapter.getChapterNumber());
                    content.setChapter(chapter.getChapterUid());
                    content.setTitle(chapter.getName());
                    contents.add(content);
                }
                course.setContents(contents);
            }

            out.add(course);
        }
        return out;
    }

    public Course findByIdFromDatabase(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return courseRepository.findById(id)
                .map(this::mapCourseEntityToCourseWithContents)
                .orElse(null);
    }

    public Course findByTitleFromDatabase(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        return courseRepository.findByTitleIgnoreCase(title.trim())
                .map(this::mapCourseEntityToCourseWithContents)
                .orElse(null);
    }

    public String findMarkdownFromDatabase(String courseId, Integer chapterOrder, Integer subOrder) {
        SubchapterMarkdownInfo info = findSubchapterMarkdownInfoFromDatabase(courseId, chapterOrder, subOrder);
        return info == null ? null : info.markdown;
    }

    public SubchapterMarkdownInfo findSubchapterMarkdownInfoFromDatabase(String courseId, Integer chapterOrder,
            Integer subOrder) {
        if (courseId == null || courseId.isBlank() || chapterOrder == null || subOrder == null) {
            return null;
        }
        CourseChapterEntity chapter = courseChapterRepository.findByCourseIdAndChapterNumber(courseId, chapterOrder);
        if (chapter == null || chapter.getId() == null) {
            return null;
        }
        CourseSubchapterEntity sub = courseSubchapterRepository.findByChapterIdAndSubchapterNumber(chapter.getId(),
                subOrder);
        if (sub == null) {
            return null;
        }
        SubchapterMarkdownInfo out = new SubchapterMarkdownInfo();
        out.markdown = sub.getMarkdown();
        out.updatedAt = sub.getUpdatedAt();
        return out;
    }

    public static class SubchapterMarkdownInfo {
        public String markdown;
        public java.time.LocalDateTime updatedAt;
    }

    public List<Question> getAssessmentQuestionsFromDatabase(String courseId, List<String> chapterList) {
        if (courseId == null || courseId.isBlank()) {
            return Collections.emptyList();
        }

        List<CourseChapterEntity> chapters;
        if (chapterList == null || chapterList.isEmpty()) {
            chapters = courseChapterRepository.findByCourseIdOrderByChapterNumberAsc(courseId);
        } else {
            chapters = new ArrayList<>();
            for (String ch : chapterList) {
                if (ch == null || ch.isBlank()) {
                    continue;
                }
                Integer chapterNo;
                try {
                    chapterNo = Integer.parseInt(ch.trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                CourseChapterEntity chapter = courseChapterRepository.findByCourseIdAndChapterNumber(courseId, chapterNo);
                if (chapter != null) {
                    chapters.add(chapter);
                }
            }
        }

        if (chapters == null || chapters.isEmpty()) {
            return Collections.emptyList();
        }

        List<Question> out = new ArrayList<>();
        for (CourseChapterEntity chapter : chapters) {
            if (chapter == null || chapter.getId() == null || chapter.getChapterNumber() == null) {
                continue;
            }
            List<com.barlarlar.myanmyanlearn.entity.CourseQuestionEntity> questions = courseQuestionRepository
                    .findByChapterIdOrderByQuestionNumberAsc(chapter.getId());
            if (questions == null || questions.isEmpty()) {
                continue;
            }
            for (var q : questions) {
                if (q == null || q.getId() == null || q.getQuestionNumber() == null) {
                    continue;
                }
                Question qm = new Question();
                qm.setCourseId(courseId);
                qm.setChapterId(String.valueOf(chapter.getChapterNumber()));
                qm.setQuestionNumber(q.getQuestionNumber());
                qm.setQuestionContentPath("/assessment/md?courseId=" + courseId + "&ch=" + chapter.getChapterNumber()
                        + "&q=" + q.getQuestionNumber());

                List<com.barlarlar.myanmyanlearn.entity.CourseQuestionSlotEntity> slots = courseQuestionSlotRepository
                        .findByQuestionIdOrderBySlotIndexAsc(q.getId());
                if (slots != null && !slots.isEmpty()) {
                    List<List<QuestionOption>> slotOptions = new ArrayList<>();
                    for (var slot : slots) {
                        if (slot == null || slot.getId() == null) {
                            continue;
                        }
                        List<com.barlarlar.myanmyanlearn.entity.CourseQuestionSlotOptionEntity> opts = courseQuestionSlotOptionRepository
                                .findByQuestionSlotIdOrderByOptionIndexAsc(slot.getId());
                        List<QuestionOption> mapped = new ArrayList<>();
                        if (opts != null) {
                            for (var opt : opts) {
                                if (opt == null || opt.getOptionIndex() == null) {
                                    continue;
                                }
                                QuestionOption o = new QuestionOption();
                                o.setOptionIndex(opt.getOptionIndex());
                                o.setOptionContent(opt.getOptionContent());
                                o.setIsCorrect(Boolean.TRUE.equals(opt.getCorrect()));
                                mapped.add(o);
                            }
                        }
                        slotOptions.add(mapped);
                    }
                    qm.setSlotOptions(slotOptions);
                } else {
                    List<com.barlarlar.myanmyanlearn.entity.CourseQuestionOptionEntity> opts = courseQuestionOptionRepository
                            .findByQuestionIdOrderByOptionIndexAsc(q.getId());
                    List<QuestionOption> mapped = new ArrayList<>();
                    if (opts != null) {
                        for (var opt : opts) {
                            if (opt == null || opt.getOptionIndex() == null) {
                                continue;
                            }
                            QuestionOption o = new QuestionOption();
                            o.setOptionIndex(opt.getOptionIndex());
                            o.setOptionContent(opt.getOptionContent());
                            o.setIsCorrect(Boolean.TRUE.equals(opt.getCorrect()));
                            mapped.add(o);
                        }
                    }
                    qm.setOptions(mapped);
                }

                out.add(qm);
            }
        }
        out.sort((a, b) -> {
            if (a == null && b == null) {
                return 0;
            }
            if (a == null) {
                return 1;
            }
            if (b == null) {
                return -1;
            }
            int aCh;
            int bCh;
            try {
                aCh = Integer.parseInt(a.getChapterId());
            } catch (Exception e) {
                aCh = Integer.MAX_VALUE;
            }
            try {
                bCh = Integer.parseInt(b.getChapterId());
            } catch (Exception e) {
                bCh = Integer.MAX_VALUE;
            }
            if (aCh != bCh) {
                return Integer.compare(aCh, bCh);
            }
            return Integer.compare(a.getQuestionNumber(), b.getQuestionNumber());
        });
        return out;
    }

    public String findAssessmentQuestionMarkdownFromDatabase(String courseId, Integer chapterOrder, Integer questionNo) {
        if (courseId == null || courseId.isBlank() || chapterOrder == null || questionNo == null) {
            return null;
        }
        CourseChapterEntity chapter = courseChapterRepository.findByCourseIdAndChapterNumber(courseId, chapterOrder);
        if (chapter == null || chapter.getId() == null) {
            return null;
        }
        List<com.barlarlar.myanmyanlearn.entity.CourseQuestionEntity> questions = courseQuestionRepository
                .findByChapterIdOrderByQuestionNumberAsc(chapter.getId());
        if (questions == null || questions.isEmpty()) {
            return null;
        }
        for (var q : questions) {
            if (q != null && q.getQuestionNumber() != null && q.getQuestionNumber().equals(questionNo)) {
                return q.getQuestionMarkdown();
            }
        }
        return null;
    }

    public String findAssessmentExplanationMarkdownFromDatabase(String courseId, Integer chapterOrder, Integer questionNo) {
        if (courseId == null || courseId.isBlank() || chapterOrder == null || questionNo == null) {
            return null;
        }
        CourseChapterEntity chapter = courseChapterRepository.findByCourseIdAndChapterNumber(courseId, chapterOrder);
        if (chapter == null || chapter.getId() == null) {
            return null;
        }
        List<com.barlarlar.myanmyanlearn.entity.CourseQuestionEntity> questions = courseQuestionRepository
                .findByChapterIdOrderByQuestionNumberAsc(chapter.getId());
        if (questions == null || questions.isEmpty()) {
            return null;
        }
        for (var q : questions) {
            if (q != null && q.getQuestionNumber() != null && q.getQuestionNumber().equals(questionNo)) {
                return q.getExplanationMarkdown();
            }
        }
        return null;
    }

    private Course mapCourseEntityToCourseWithContents(CourseEntity entity) {
        if (entity == null || entity.getCourseId() == null || entity.getCourseId().isBlank()) {
            return null;
        }

        Course course = new Course();
        course.setId(entity.getCourseId());
        course.setTitle(entity.getTitle());
        course.setDescription(entity.getDescription());
        course.setLanguage(entity.getLanguage());
        course.setLogo(entity.getCoverImageUrl());

        List<CourseChapterEntity> chapters = courseChapterRepository
                .findByCourseIdOrderByChapterNumberAsc(entity.getCourseId());
        if (chapters == null || chapters.isEmpty()) {
            course.setContents(Collections.emptyList());
            return course;
        }

        List<Content> contents = new ArrayList<>();
        for (CourseChapterEntity chapter : chapters) {
            if (chapter == null || chapter.getId() == null || chapter.getChapterNumber() == null) {
                continue;
            }

            Content content = new Content();
            content.setOrder(chapter.getChapterNumber());
            content.setChapter(chapter.getChapterUid());
            content.setTitle(chapter.getName());

            var subEntities = courseSubchapterRepository.findByChapterIdOrderBySubchapterNumberAsc(chapter.getId());
            if (subEntities != null && !subEntities.isEmpty()) {
                List<Subcontent> subs = new ArrayList<>();
                for (var sub : subEntities) {
                    if (sub == null || sub.getSubchapterNumber() == null) {
                        continue;
                    }
                    Subcontent s = new Subcontent();
                    s.setOrder(sub.getSubchapterNumber());
                    s.setTitle(sub.getName());
                    s.setMarkdownPath("/reader/md?courseId=" + entity.getCourseId() + "&ch=" + chapter.getChapterNumber()
                            + "&sc=" + sub.getSubchapterNumber());
                    subs.add(s);
                }
                content.setSubcontents(subs);
            } else {
                content.setSubcontents(Collections.emptyList());
            }

            contents.add(content);
        }
        course.setContents(contents);
        return course;
    }
}
