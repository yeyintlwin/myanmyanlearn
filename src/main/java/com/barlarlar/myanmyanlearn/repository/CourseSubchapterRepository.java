package com.barlarlar.myanmyanlearn.repository;

import com.barlarlar.myanmyanlearn.entity.CourseSubchapterEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseSubchapterRepository extends JpaRepository<CourseSubchapterEntity, Long> {
    List<CourseSubchapterEntity> findByChapterIdOrderBySubchapterNumberAsc(Long chapterId);

    CourseSubchapterEntity findByChapterIdAndSubchapterNumber(Long chapterId, Integer subchapterNumber);

    CourseSubchapterEntity findByChapterIdAndSubchapterUid(Long chapterId, String subchapterUid);
}
