package com.barlarlar.myanmyanlearn.repository;

import com.barlarlar.myanmyanlearn.entity.CourseQuestionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseQuestionRepository extends JpaRepository<CourseQuestionEntity, Long> {
    List<CourseQuestionEntity> findByChapterIdOrderByQuestionNumberAsc(Long chapterId);

    CourseQuestionEntity findByChapterIdAndQuestionNumber(Long chapterId, Integer questionNumber);

    CourseQuestionEntity findByChapterIdAndQuestionUid(Long chapterId, String questionUid);

    long countByChapterIdIn(List<Long> chapterIds);
}
