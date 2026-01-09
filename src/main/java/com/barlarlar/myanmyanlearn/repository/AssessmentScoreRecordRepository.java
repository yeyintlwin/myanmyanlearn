package com.barlarlar.myanmyanlearn.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barlarlar.myanmyanlearn.entity.AssessmentScoreRecord;

@Repository
public interface AssessmentScoreRecordRepository extends JpaRepository<AssessmentScoreRecord, Long> {
    List<AssessmentScoreRecord> findByUserIdOrderByCreatedAtDesc(String userId);

    List<AssessmentScoreRecord> findByUserIdAndCourseIdOrderByCreatedAtDesc(String userId, String courseId);

    Optional<AssessmentScoreRecord> findByIdAndUserId(Long id, String userId);

    Optional<AssessmentScoreRecord> findTopByUserIdOrderByIdDesc(String userId);
}
