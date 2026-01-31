package com.barlarlar.myanmyanlearn.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barlarlar.myanmyanlearn.entity.AssessmentScoreRecord;
import com.barlarlar.myanmyanlearn.service.AssessmentScoreRecordService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/api/assessment-scores")
public class AssessmentScoreRecordApiController {
    private static final Logger log = LoggerFactory.getLogger(AssessmentScoreRecordApiController.class);
    private final AssessmentScoreRecordService service;
    private final ObjectMapper objectMapper;

    public AssessmentScoreRecordApiController(AssessmentScoreRecordService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestParam(name = "courseId", required = false) String courseId) {
        List<AssessmentScoreRecord> records = service.listForCurrentUser(Optional.ofNullable(courseId));
        List<Map<String, Object>> resp = records.stream().map(this::toSummary).collect(Collectors.toList());
        return ResponseEntity.ok(resp);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody JsonNode payload) throws Exception {
        String userId = null;
        try {
            userId = service.currentUserId();
            AssessmentScoreRecord saved = service.upsertMergedForCurrentUser(payload);
            log.info("Saved assessment score record id={} userId={} courseId={}", saved.getId(), userId,
                    saved.getCourseId());
            return ResponseEntity.ok(toSummary(saved));
        } catch (IllegalStateException e) {
            log.warn("Unauthorized assessment score save userId={}", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "UNAUTHORIZED"));
        } catch (IllegalArgumentException e) {
            log.warn("Bad assessment score payload userId={}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "BAD_REQUEST"));
        } catch (Exception e) {
            log.warn("Failed assessment score save userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "SAVE_FAILED"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable("id") Long id) throws Exception {
        AssessmentScoreRecord record = service.getOwned(id);
        Map<String, Object> resp = toSummary(record);
        resp.put("json", objectMapper.readTree(record.getScoreJson()));
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}/json")
    public ResponseEntity<JsonNode> getJson(
            @PathVariable("id") Long id,
            @RequestParam(name = "pointer", required = false) String pointer) throws Exception {
        AssessmentScoreRecord record = service.getOwned(id);
        JsonNode root = objectMapper.readTree(record.getScoreJson());
        if (pointer == null || pointer.isBlank()) {
            return ResponseEntity.ok(root);
        }
        JsonNode at = root.at(pointer);
        if (at.isMissingNode()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(at);
    }

    @PutMapping("/{id}/json")
    public ResponseEntity<JsonNode> replaceJson(@PathVariable("id") Long id, @RequestBody JsonNode newJson)
            throws Exception {
        String serialized = objectMapper.writeValueAsString(newJson);
        service.replaceJson(id, serialized);
        return ResponseEntity.ok(newJson);
    }

    @PutMapping("/{id}/json/value")
    public ResponseEntity<JsonNode> putValue(
            @PathVariable("id") Long id,
            @RequestParam(name = "pointer") String pointer,
            @RequestBody JsonNode value) throws Exception {
        AssessmentScoreRecord record = service.getOwned(id);
        JsonNode root = objectMapper.readTree(record.getScoreJson());
        JsonNode updated = setAtPointer(root, pointer, value);
        String serialized = objectMapper.writeValueAsString(updated);
        service.replaceJson(id, serialized);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/json/value")
    public ResponseEntity<JsonNode> deleteValue(
            @PathVariable("id") Long id,
            @RequestParam(name = "pointer") String pointer) throws Exception {
        AssessmentScoreRecord record = service.getOwned(id);
        JsonNode root = objectMapper.readTree(record.getScoreJson());
        JsonNode updated = deleteAtPointer(root, pointer);
        String serialized = objectMapper.writeValueAsString(updated);
        service.replaceJson(id, serialized);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.deleteOwned(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toSummary(AssessmentScoreRecord record) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", record.getId());
        resp.put("courseId", record.getCourseId());
        resp.put("createdAt", record.getCreatedAt());
        resp.put("updatedAt", record.getUpdatedAt());
        return resp;
    }

    private JsonNode setAtPointer(JsonNode root, String pointer, JsonNode value) {
        if (pointer == null) {
            throw new IllegalArgumentException("pointer is required");
        }
        if (pointer.isBlank() || "/".equals(pointer)) {
            return value;
        }
        if (!pointer.startsWith("/")) {
            throw new IllegalArgumentException("pointer must start with '/'");
        }

        String[] parts = pointer.substring(1).split("/");
        JsonNode current = root;
        if (current == null || current.isMissingNode() || current.isNull()) {
            current = objectMapper.createObjectNode();
            root = current;
        }

        for (int i = 0; i < parts.length - 1; i++) {
            String raw = parts[i];
            String key = decodePointerSegment(raw);
            String nextRaw = parts[i + 1];
            boolean nextIsIndex = isInteger(decodePointerSegment(nextRaw));

            if (current.isObject()) {
                ObjectNode obj = (ObjectNode) current;
                JsonNode child = obj.get(key);
                if (child == null || child.isNull() || child.isMissingNode()) {
                    JsonNode created = nextIsIndex ? objectMapper.createArrayNode() : objectMapper.createObjectNode();
                    obj.set(key, created);
                    current = created;
                } else {
                    current = child;
                }
            } else if (current.isArray() && isInteger(key)) {
                ArrayNode arr = (ArrayNode) current;
                int idx = Integer.parseInt(key);
                while (arr.size() <= idx) {
                    arr.addNull();
                }
                JsonNode child = arr.get(idx);
                if (child == null || child.isNull() || child.isMissingNode()) {
                    JsonNode created = nextIsIndex ? objectMapper.createArrayNode() : objectMapper.createObjectNode();
                    arr.set(idx, created);
                    current = created;
                } else {
                    current = child;
                }
            } else {
                throw new IllegalArgumentException("Cannot traverse pointer at '" + pointer + "'");
            }
        }

        String last = decodePointerSegment(parts[parts.length - 1]);
        if (current.isObject()) {
            ((ObjectNode) current).set(last, value);
            return root;
        }
        if (current.isArray() && isInteger(last)) {
            ArrayNode arr = (ArrayNode) current;
            int idx = Integer.parseInt(last);
            while (arr.size() <= idx) {
                arr.addNull();
            }
            arr.set(idx, value);
            return root;
        }
        throw new IllegalArgumentException("Cannot set value at '" + pointer + "'");
    }

    private JsonNode deleteAtPointer(JsonNode root, String pointer) {
        if (pointer == null) {
            throw new IllegalArgumentException("pointer is required");
        }
        if (pointer.isBlank() || "/".equals(pointer)) {
            return objectMapper.createObjectNode();
        }
        if (!pointer.startsWith("/")) {
            throw new IllegalArgumentException("pointer must start with '/'");
        }
        String[] parts = pointer.substring(1).split("/");
        JsonNode current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String key = decodePointerSegment(parts[i]);
            if (current.isObject()) {
                current = current.get(key);
            } else if (current.isArray() && isInteger(key)) {
                current = current.get(Integer.parseInt(key));
            } else {
                return root;
            }
            if (current == null) {
                return root;
            }
        }

        String last = decodePointerSegment(parts[parts.length - 1]);
        if (current != null && current.isObject()) {
            ((ObjectNode) current).remove(last);
            return root;
        }
        if (current != null && current.isArray() && isInteger(last)) {
            ((ArrayNode) current).remove(Integer.parseInt(last));
            return root;
        }
        return root;
    }

    private boolean isInteger(String s) {
        if (s == null || s.isEmpty())
            return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
                return false;
        }
        return true;
    }

    private String decodePointerSegment(String segment) {
        return segment.replace("~1", "/").replace("~0", "~");
    }
}
