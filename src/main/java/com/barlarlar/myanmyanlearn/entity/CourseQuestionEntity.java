package com.barlarlar.myanmyanlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "course_questions")
public class CourseQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "question_uid", length = 120)
    private String questionUid;

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Lob
    @Column(name = "question_markdown", nullable = false, columnDefinition = "LONGTEXT")
    private String questionMarkdown;

    @Lob
    @Column(name = "explanation_markdown", nullable = false, columnDefinition = "LONGTEXT")
    private String explanationMarkdown;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", insertable = false, updatable = false)
    private CourseChapterEntity chapter;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<CourseQuestionOptionEntity> options;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<CourseQuestionSlotEntity> slots;

    public CourseQuestionEntity() {
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public String getQuestionUid() {
        return questionUid;
    }

    public void setQuestionUid(String questionUid) {
        this.questionUid = questionUid;
    }

    public Integer getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(Integer questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getQuestionMarkdown() {
        return questionMarkdown;
    }

    public void setQuestionMarkdown(String questionMarkdown) {
        this.questionMarkdown = questionMarkdown;
    }

    public String getExplanationMarkdown() {
        return explanationMarkdown;
    }

    public void setExplanationMarkdown(String explanationMarkdown) {
        this.explanationMarkdown = explanationMarkdown;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public CourseChapterEntity getChapter() {
        return chapter;
    }

    public void setChapter(CourseChapterEntity chapter) {
        this.chapter = chapter;
    }

    public List<CourseQuestionOptionEntity> getOptions() {
        return options;
    }

    public void setOptions(List<CourseQuestionOptionEntity> options) {
        this.options = options;
    }

    public List<CourseQuestionSlotEntity> getSlots() {
        return slots;
    }

    public void setSlots(List<CourseQuestionSlotEntity> slots) {
        this.slots = slots;
    }
}
