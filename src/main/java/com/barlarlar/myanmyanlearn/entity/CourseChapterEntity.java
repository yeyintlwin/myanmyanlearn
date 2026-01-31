package com.barlarlar.myanmyanlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "course_chapters")
public class CourseChapterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", length = 100, nullable = false)
    private String courseId;

    @Column(name = "chapter_uid", length = 120)
    private String chapterUid;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private CourseEntity course;

    @OneToMany(mappedBy = "chapter", fetch = FetchType.LAZY)
    private List<CourseSubchapterEntity> subchapters;

    @OneToMany(mappedBy = "chapter", fetch = FetchType.LAZY)
    private List<CourseQuestionEntity> questions;

    public CourseChapterEntity() {
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

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getChapterUid() {
        return chapterUid;
    }

    public void setChapterUid(String chapterUid) {
        this.chapterUid = chapterUid;
    }

    public Integer getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(Integer chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public CourseEntity getCourse() {
        return course;
    }

    public void setCourse(CourseEntity course) {
        this.course = course;
    }

    public List<CourseSubchapterEntity> getSubchapters() {
        return subchapters;
    }

    public void setSubchapters(List<CourseSubchapterEntity> subchapters) {
        this.subchapters = subchapters;
    }

    public List<CourseQuestionEntity> getQuestions() {
        return questions;
    }

    public void setQuestions(List<CourseQuestionEntity> questions) {
        this.questions = questions;
    }
}
