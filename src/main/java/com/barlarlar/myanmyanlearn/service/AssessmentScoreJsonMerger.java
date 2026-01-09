package com.barlarlar.myanmyanlearn.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class AssessmentScoreJsonMerger {
    private AssessmentScoreJsonMerger() {
    }

    public static ObjectNode mergeByChapterNo(JsonNode existingJson, JsonNode incomingJson) {
        ObjectNode out = JsonNodeFactory.instance.objectNode();

        JsonNode existingCourseId = (existingJson != null && existingJson.isObject()) ? existingJson.get("courseId")
                : null;
        JsonNode incomingCourseId = (incomingJson != null && incomingJson.isObject()) ? incomingJson.get("courseId")
                : null;
        JsonNode courseId = (incomingCourseId != null && !incomingCourseId.isNull()) ? incomingCourseId
                : existingCourseId;
        if (courseId != null && !courseId.isNull()) {
            out.set("courseId", courseId.deepCopy());
        }

        JsonNode existingChapters = (existingJson != null && existingJson.isObject()) ? existingJson.get("chapters")
                : null;
        JsonNode incomingChapters = (incomingJson != null && incomingJson.isObject()) ? incomingJson.get("chapters")
                : null;
        ArrayNode mergedChapters = mergeChaptersArray(existingChapters, incomingChapters);
        if (mergedChapters != null) {
            out.set("chapters", mergedChapters);
        }

        return out;
    }

    private static ArrayNode mergeChaptersArray(JsonNode existingChaptersNode, JsonNode incomingChaptersNode) {
        if (incomingChaptersNode == null || !incomingChaptersNode.isArray()) {
            return null;
        }

        ArrayNode existingChapters = (existingChaptersNode != null && existingChaptersNode.isArray())
                ? (ArrayNode) existingChaptersNode
                : JsonNodeFactory.instance.arrayNode();

        List<ObjectNode> ordered = new ArrayList<>();
        Map<Integer, Integer> chapterNoToIndex = new HashMap<>();

        for (JsonNode ch : existingChapters) {
            if (ch == null || !ch.isObject()) {
                continue;
            }
            Integer chapterNo = readChapterNo(ch);
            if (chapterNo == null) {
                continue;
            }
            ObjectNode copy = (ObjectNode) ch.deepCopy();
            int idx = ordered.size();
            ordered.add(copy);
            chapterNoToIndex.put(chapterNo, idx);
        }

        List<ObjectNode> append = new ArrayList<>();
        for (JsonNode ch : incomingChaptersNode) {
            if (ch == null || !ch.isObject()) {
                continue;
            }
            Integer chapterNo = readChapterNo(ch);
            if (chapterNo == null) {
                continue;
            }
            ObjectNode normalized = (ObjectNode) ch.deepCopy();
            normalized.put("chapter_no", chapterNo);

            Integer existingIndex = chapterNoToIndex.get(chapterNo);
            if (existingIndex != null) {
                ordered.set(existingIndex, normalized);
            } else {
                append.add(normalized);
            }
        }

        ArrayNode out = JsonNodeFactory.instance.arrayNode();
        for (ObjectNode n : ordered) {
            out.add(n);
        }
        for (ObjectNode n : append) {
            out.add(n);
        }
        return out;
    }

    private static Integer readChapterNo(JsonNode chapterNode) {
        if (chapterNode == null || !chapterNode.isObject()) {
            return null;
        }
        JsonNode v = chapterNode.get("chapter_no");
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.isInt() || v.isLong()) {
            return v.asInt();
        }
        if (v.isTextual()) {
            String s = v.asText();
            if (s == null) {
                return null;
            }
            s = s.trim();
            if (s.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
