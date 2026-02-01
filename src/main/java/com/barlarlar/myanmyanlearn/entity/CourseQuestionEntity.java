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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_questions")
@Getter
@Setter
@NoArgsConstructor
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
}
