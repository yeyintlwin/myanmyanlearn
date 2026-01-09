package com.barlarlar.myanmyanlearn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AssessmentScoreJsonMergerTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void merge_appendsNewChapter() throws Exception {
    JsonNode existing = objectMapper.readTree(
        """
            {
              "courseId": "jken",
              "chapters": [
                { "chapter_no": 1, "questions": [ { "question_no": 1, "slopes": [ { "slope_no": 1, "is_correct": true } ] } ] }
              ]
            }
            """);
    JsonNode incoming = objectMapper.readTree(
        """
            {
              "courseId": "jken",
              "chapters": [
                { "chapter_no": 2, "questions": [ { "question_no": 1, "slopes": [ { "slope_no": 1, "is_correct": false } ] } ] }
              ]
            }
            """);

    JsonNode merged = AssessmentScoreJsonMerger.mergeByChapterNo(existing, incoming);
    assertNotNull(merged);
    assertEquals(2, merged.get("chapters").size());
    assertEquals(1, merged.get("chapters").get(0).get("chapter_no").asInt());
    assertEquals(2, merged.get("chapters").get(1).get("chapter_no").asInt());
  }

  @Test
  public void merge_updatesExistingChapter() throws Exception {
    JsonNode existing = objectMapper.readTree(
        """
            {
              "courseId": "jken",
              "chapters": [
                { "chapter_no": 1, "questions": [ { "question_no": 1, "slopes": [ { "slope_no": 1, "is_correct": false } ] } ] }
              ]
            }
            """);
    JsonNode incoming = objectMapper.readTree(
        """
            {
              "courseId": "jken",
              "chapters": [
                { "chapter_no": 1, "questions": [ { "question_no": 1, "slopes": [ { "slope_no": 1, "is_correct": true } ] } ] }
              ]
            }
            """);

    JsonNode merged = AssessmentScoreJsonMerger.mergeByChapterNo(existing, incoming);
    assertNotNull(merged);
    assertEquals(1, merged.get("chapters").size());
    JsonNode chapter1 = merged.get("chapters").get(0);
    assertEquals(1, chapter1.get("chapter_no").asInt());
    JsonNode isCorrect = chapter1.get("questions").get(0).get("slopes").get(0).get("is_correct");
    assertEquals(true, isCorrect.asBoolean());
  }

  @Test
  public void merge_removesDeprecatedFields() throws Exception {
    JsonNode existing = objectMapper.readTree("""
        {
          "courseId": "jken",
          "yourScore": 10,
          "totalPossible": 20,
          "answers": { "q1-s1": 0 },
          "chapters": [
            { "chapter_no": 1, "questions": [] }
          ]
        }
        """);
    JsonNode incoming = objectMapper.readTree("""
        {
          "courseId": "jken",
          "yourScore": 11,
          "totalPossible": 21,
          "answers": { "q1-s1": 1 },
          "chapters": [
            { "chapter_no": 2, "questions": [] }
          ]
        }
        """);

    JsonNode merged = AssessmentScoreJsonMerger.mergeByChapterNo(existing, incoming);
    assertNotNull(merged);
    assertNull(merged.get("yourScore"));
    assertNull(merged.get("totalPossible"));
    assertNull(merged.get("answers"));
    assertEquals(2, merged.get("chapters").size());
  }
}
