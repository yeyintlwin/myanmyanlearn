package com.barlarlar.myanmyanlearn.repository;

import com.barlarlar.myanmyanlearn.entity.CourseEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, String> {
    Optional<CourseEntity> findByTitleIgnoreCase(String title);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CourseEntity c where c.courseId = :courseId")
    Optional<CourseEntity> findByCourseIdForUpdate(@Param("courseId") String courseId);
}
