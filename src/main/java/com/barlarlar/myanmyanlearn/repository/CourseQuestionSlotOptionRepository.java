package com.barlarlar.myanmyanlearn.repository;

import com.barlarlar.myanmyanlearn.entity.CourseQuestionSlotOptionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseQuestionSlotOptionRepository extends JpaRepository<CourseQuestionSlotOptionEntity, Long> {
    List<CourseQuestionSlotOptionEntity> findByQuestionSlotIdOrderByOptionIndexAsc(Long questionSlotId);
}
