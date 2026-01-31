package com.barlarlar.myanmyanlearn.repository;

import com.barlarlar.myanmyanlearn.entity.CourseQuestionOptionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseQuestionOptionRepository extends JpaRepository<CourseQuestionOptionEntity, Long> {
    List<CourseQuestionOptionEntity> findByQuestionIdOrderByOptionIndexAsc(Long questionId);
}
