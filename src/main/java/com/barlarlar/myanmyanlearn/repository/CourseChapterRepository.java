package com.barlarlar.myanmyanlearn.repository;

import com.barlarlar.myanmyanlearn.entity.CourseChapterEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseChapterRepository extends JpaRepository<CourseChapterEntity, Long> {
    List<CourseChapterEntity> findByCourseIdOrderByChapterNumberAsc(String courseId);

    CourseChapterEntity findByCourseIdAndChapterNumber(String courseId, Integer chapterNumber);

    CourseChapterEntity findByCourseIdAndChapterUid(String courseId, String chapterUid);
}
