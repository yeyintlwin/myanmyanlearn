package com.barlarlar.myanmyanlearn.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.barlarlar.myanmyanlearn.entity.AssessmentScoreRecord;
import com.barlarlar.myanmyanlearn.repository.AssessmentScoreRecordRepository;

@Service
public class AssessmentScoreRecordService {
    private final AssessmentScoreRecordRepository repository;
    private final ObjectMapper objectMapper;

    public AssessmentScoreRecordService(AssessmentScoreRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        if ("anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }

    @Transactional
    public AssessmentScoreRecord upsertMergedForCurrentUser(JsonNode incomingPayload) {
        String userId = currentUserId();
        if (userId == null) {
            throw new IllegalStateException("User must be authenticated");
        }
        if (incomingPayload == null || !incomingPayload.isObject()) {
            throw new IllegalArgumentException("payload must be a JSON object");
        }

        String incomingCourseId = incomingPayload.hasNonNull("courseId") ? incomingPayload.get("courseId").asText()
                : null;

        Optional<AssessmentScoreRecord> existing = repository.findTopByUserIdOrderByIdDesc(userId);
        if (existing.isPresent()) {
            AssessmentScoreRecord record = existing.get();
            JsonNode existingJson = null;
            try {
                existingJson = objectMapper.readTree(record.getScoreJson());
            } catch (Exception ignored) {
            }
            JsonNode merged = AssessmentScoreJsonMerger.mergeByChapterNo(existingJson, incomingPayload);
            try {
                record.setScoreJson(objectMapper.writeValueAsString(merged));
            } catch (Exception e) {
                throw new IllegalArgumentException("payload serialization failed", e);
            }
            return repository.save(record);
        }

        AssessmentScoreRecord created = new AssessmentScoreRecord();
        created.setUserId(userId);
        created.setCourseId(incomingCourseId);
        created.setScoreJson(incomingPayload.toString());
        return repository.save(created);
    }

    @Transactional(readOnly = true)
    public List<AssessmentScoreRecord> listForCurrentUser(Optional<String> courseId) {
        String userId = currentUserId();
        if (userId == null) {
            throw new IllegalStateException("User must be authenticated");
        }
        if (courseId.isPresent() && courseId.get() != null && !courseId.get().isBlank()) {
            return repository.findByUserIdAndCourseIdOrderByCreatedAtDesc(userId, courseId.get());
        }
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public AssessmentScoreRecord getOwned(Long id) {
        String userId = currentUserId();
        if (userId == null) {
            throw new IllegalStateException("User must be authenticated");
        }
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Score record not found"));
    }

    @Transactional
    public AssessmentScoreRecord replaceJson(Long id, String newJson) {
        AssessmentScoreRecord record = getOwned(id);
        record.setScoreJson(newJson);
        return repository.save(record);
    }

    @Transactional
    public void deleteOwned(Long id) {
        AssessmentScoreRecord record = getOwned(id);
        repository.delete(record);
    }
}
