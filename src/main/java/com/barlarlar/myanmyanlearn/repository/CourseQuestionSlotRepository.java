package com.barlarlar.myanmyanlearn.repository;

import com.barlarlar.myanmyanlearn.entity.CourseQuestionSlotEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseQuestionSlotRepository extends JpaRepository<CourseQuestionSlotEntity, Long> {
    List<CourseQuestionSlotEntity> findByQuestionIdOrderBySlotIndexAsc(Long questionId);
}
