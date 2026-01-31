package com.barlarlar.myanmyanlearn.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    private static final int COURSE_ID_MAX_LEN = 100;
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

    @Transactional(readOnly = true)
    public Optional<JsonNode> latestScoreJsonForCurrentUser(Optional<String> courseId) {
        String userId = currentUserId();
        if (userId == null) {
            return Optional.empty();
        }

        String targetCourseId = normalizeCourseId(courseId != null && courseId.isPresent() ? courseId.get() : null);
        String targetKey = courseIdKey(targetCourseId);
        AssessmentScoreRecord record = null;
        if (targetKey == null) {
            record = repository.findTopByUserIdOrderByIdDesc(userId).orElse(null);
        } else {
            record = repository.findTopByUserIdAndCourseIdOrderByIdDesc(userId, targetKey).orElse(null);
            if (record == null && targetCourseId != null && !targetCourseId.equals(targetKey)) {
                record = repository.findTopByUserIdAndCourseIdOrderByIdDesc(userId, targetCourseId).orElse(null);
            }
        }

        if (record == null || record.getScoreJson() == null || record.getScoreJson().isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(objectMapper.readTree(record.getScoreJson()));
        } catch (Exception e) {
            return Optional.empty();
        }
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
        incomingCourseId = normalizeCourseId(incomingCourseId);
        String incomingKey = courseIdKey(incomingCourseId);

        AssessmentScoreRecord record = null;
        if (incomingKey != null) {
            record = repository.findTopByUserIdAndCourseIdOrderByIdDesc(userId, incomingKey).orElse(null);
            if (record == null && incomingCourseId != null && !incomingCourseId.equals(incomingKey)) {
                record = repository.findTopByUserIdAndCourseIdOrderByIdDesc(userId, incomingCourseId).orElse(null);
            }
        } else {
            List<AssessmentScoreRecord> records = repository.findByUserIdOrderByCreatedAtDesc(userId);
            for (AssessmentScoreRecord r : records) {
                if (r == null) {
                    continue;
                }
                String cid = normalizeCourseId(r.getCourseId());
                String jsonCid = extractCourseIdFromScoreJson(r.getScoreJson());
                if (cid == null && jsonCid == null) {
                    record = r;
                    break;
                }
            }
        }

        if (record != null) {
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
            if (incomingKey != null) {
                record.setCourseId(incomingKey);
            }
            return repository.save(record);
        }

        AssessmentScoreRecord created = new AssessmentScoreRecord();
        created.setUserId(userId);
        created.setCourseId(incomingKey);
        created.setScoreJson(incomingPayload.toString());
        return repository.save(created);
    }

    @Transactional(readOnly = true)
    public List<AssessmentScoreRecord> listForCurrentUser(Optional<String> courseId) {
        String userId = currentUserId();
        if (userId == null) {
            throw new IllegalStateException("User must be authenticated");
        }
        String targetCourseId = normalizeCourseId(courseId != null && courseId.isPresent() ? courseId.get() : null);
        String targetKey = courseIdKey(targetCourseId);
        if (targetKey == null) {
            return repository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        List<AssessmentScoreRecord> byKey = repository.findByUserIdAndCourseIdOrderByCreatedAtDesc(userId, targetKey);
        if (!byKey.isEmpty()) {
            return byKey;
        }
        if (targetCourseId != null && !targetCourseId.equals(targetKey)) {
            return repository.findByUserIdAndCourseIdOrderByCreatedAtDesc(userId, targetCourseId);
        }
        return byKey;
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

    private String normalizeCourseId(String courseId) {
        if (courseId == null) {
            return null;
        }
        String trimmed = courseId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String courseIdKey(String courseId) {
        String normalized = normalizeCourseId(courseId);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() <= COURSE_ID_MAX_LEN) {
            return normalized;
        }
        return "h:" + sha256Hex(normalized);
    }

    private String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash courseId", e);
        }
    }

    private String extractCourseIdFromScoreJson(String scoreJson) {
        if (scoreJson == null || scoreJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(scoreJson);
            if (root == null || !root.isObject()) {
                return null;
            }
            JsonNode cid = root.get("courseId");
            if (cid == null || cid.isNull()) {
                return null;
            }
            return normalizeCourseId(cid.asText());
        } catch (Exception e) {
            return null;
        }
    }
}
