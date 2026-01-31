package com.barlarlar.myanmyanlearn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.barlarlar.myanmyanlearn.entity.AssessmentScoreRecord;
import com.barlarlar.myanmyanlearn.repository.AssessmentScoreRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AssessmentScoreRecordServiceTest {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @AfterEach
        void cleanup() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void upsertMergedForCurrentUser_doesNotMergeIntoOtherCourseWhenIncomingHasNoCourseId() throws Exception {
                AssessmentScoreRecordRepository repo = org.mockito.Mockito.mock(AssessmentScoreRecordRepository.class);
                AssessmentScoreRecordService service = new AssessmentScoreRecordService(repo, objectMapper);
                SecurityContextHolder.getContext()
                                .setAuthentication(new UsernamePasswordAuthenticationToken("u1", "pw",
                                                java.util.List.of()));

                AssessmentScoreRecord existing = new AssessmentScoreRecord();
                existing.setUserId("u1");
                existing.setCourseId("jken");
                existing.setScoreJson("""
                                { "courseId": "jken", "chapters": [ { "chapter_no": 1, "questions": [] } ] }
                                """);

                when(repo.findByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of(existing));
                when(repo.save(any(AssessmentScoreRecord.class))).thenAnswer(inv -> inv.getArgument(0));

                JsonNode incoming = objectMapper.readTree("""
                                { "courseId": null, "chapters": [ { "chapter_no": 2, "questions": [] } ] }
                                """);

                AssessmentScoreRecord saved = service.upsertMergedForCurrentUser(incoming);
                assertNotNull(saved);

                ArgumentCaptor<AssessmentScoreRecord> captor = ArgumentCaptor.forClass(AssessmentScoreRecord.class);
                verify(repo).save(captor.capture());
                AssessmentScoreRecord savedArg = captor.getValue();
                assertEquals(null, savedArg.getCourseId());
                com.fasterxml.jackson.databind.JsonNode merged = objectMapper.readTree(savedArg.getScoreJson());
                assertEquals(1, merged.get("chapters").size());
                assertEquals(2, merged.get("chapters").get(0).get("chapter_no").asInt());
        }

        @Test
        void upsertMergedForCurrentUser_mergesWithinSameCourse() throws Exception {
                AssessmentScoreRecordRepository repo = org.mockito.Mockito.mock(AssessmentScoreRecordRepository.class);
                AssessmentScoreRecordService service = new AssessmentScoreRecordService(repo, objectMapper);
                SecurityContextHolder.getContext()
                                .setAuthentication(new UsernamePasswordAuthenticationToken("u1", "pw",
                                                java.util.List.of()));

                AssessmentScoreRecord existing = new AssessmentScoreRecord();
                existing.setUserId("u1");
                existing.setCourseId("c1");
                existing.setScoreJson("""
                                { "courseId": "c1", "chapters": [ { "chapter_no": 1, "questions": [] } ] }
                                """);

                when(repo.findTopByUserIdAndCourseIdOrderByIdDesc("u1", "c1")).thenReturn(Optional.of(existing));
                when(repo.save(any(AssessmentScoreRecord.class))).thenAnswer(inv -> inv.getArgument(0));

                JsonNode incoming = objectMapper.readTree("""
                                { "courseId": "c1", "chapters": [ { "chapter_no": 2, "questions": [] } ] }
                                """);

                AssessmentScoreRecord saved = service.upsertMergedForCurrentUser(incoming);
                assertNotNull(saved);

                ArgumentCaptor<AssessmentScoreRecord> captor = ArgumentCaptor.forClass(AssessmentScoreRecord.class);
                verify(repo).save(captor.capture());
                String mergedJson = captor.getValue().getScoreJson();
                assertNotNull(mergedJson);
                com.fasterxml.jackson.databind.JsonNode merged = objectMapper.readTree(mergedJson);
                assertEquals(2, merged.get("chapters").size());
        }

        @Test
        void upsertMergedForCurrentUser_createsRecordWithCourseIdForNewCourse() throws Exception {
                AssessmentScoreRecordRepository repo = org.mockito.Mockito.mock(AssessmentScoreRecordRepository.class);
                AssessmentScoreRecordService service = new AssessmentScoreRecordService(repo, objectMapper);
                SecurityContextHolder.getContext()
                                .setAuthentication(new UsernamePasswordAuthenticationToken("u1", "pw",
                                                java.util.List.of()));

                when(repo.findTopByUserIdAndCourseIdOrderByIdDesc("u1", "new-course")).thenReturn(Optional.empty());
                when(repo.save(any(AssessmentScoreRecord.class))).thenAnswer(inv -> inv.getArgument(0));

                JsonNode incoming = objectMapper.readTree("""
                                { "courseId": "new-course", "chapters": [ { "chapter_no": 1, "questions": [] } ] }
                                """);

                AssessmentScoreRecord saved = service.upsertMergedForCurrentUser(incoming);
                assertNotNull(saved);

                ArgumentCaptor<AssessmentScoreRecord> captor = ArgumentCaptor.forClass(AssessmentScoreRecord.class);
                verify(repo).save(captor.capture());
                assertEquals("new-course", captor.getValue().getCourseId());
        }

        @Test
        void upsertMergedForCurrentUser_hashesCourseIdWhenTooLongForColumn() throws Exception {
                AssessmentScoreRecordRepository repo = org.mockito.Mockito.mock(AssessmentScoreRecordRepository.class);
                AssessmentScoreRecordService service = new AssessmentScoreRecordService(repo, objectMapper);
                SecurityContextHolder.getContext()
                                .setAuthentication(new UsernamePasswordAuthenticationToken("u1", "pw",
                                                java.util.List.of()));

                when(repo.findTopByUserIdAndCourseIdOrderByIdDesc(org.mockito.ArgumentMatchers.eq("u1"),
                                org.mockito.ArgumentMatchers.anyString()))
                                .thenReturn(Optional.empty());
                when(repo.save(any(AssessmentScoreRecord.class))).thenAnswer(inv -> inv.getArgument(0));

                String longCourseId = "c".repeat(150);
                JsonNode incoming = objectMapper.readTree("""
                                { "courseId": "%s", "chapters": [ { "chapter_no": 1, "questions": [] } ] }
                                """.formatted(longCourseId));

                AssessmentScoreRecord saved = service.upsertMergedForCurrentUser(incoming);
                assertNotNull(saved);

                ArgumentCaptor<AssessmentScoreRecord> captor = ArgumentCaptor.forClass(AssessmentScoreRecord.class);
                verify(repo).save(captor.capture());
                String stored = captor.getValue().getCourseId();
                assertNotNull(stored);
                assertEquals(true, stored.startsWith("h:"));
                assertEquals(true, stored.length() <= 100);
        }

        @Test
        void upsertMergedForCurrentUser_createsNewRecordForDifferentCourse() throws Exception {
                AssessmentScoreRecordRepository repo = org.mockito.Mockito.mock(AssessmentScoreRecordRepository.class);
                AssessmentScoreRecordService service = new AssessmentScoreRecordService(repo, objectMapper);
                SecurityContextHolder.getContext()
                                .setAuthentication(new UsernamePasswordAuthenticationToken("u1", "pw",
                                                java.util.List.of()));

                AssessmentScoreRecord existing = new AssessmentScoreRecord();
                existing.setUserId("u1");
                existing.setCourseId("c1");
                existing.setScoreJson("""
                                { "courseId": "c1", "chapters": [ { "chapter_no": 1, "questions": [] } ] }
                                """);

                when(repo.findTopByUserIdAndCourseIdOrderByIdDesc("u1", "c2")).thenReturn(Optional.empty());
                when(repo.save(any(AssessmentScoreRecord.class))).thenAnswer(inv -> inv.getArgument(0));

                JsonNode incoming = objectMapper.readTree("""
                                { "courseId": "c2", "chapters": [ { "chapter_no": 2, "questions": [] } ] }
                                """);

                AssessmentScoreRecord saved = service.upsertMergedForCurrentUser(incoming);
                assertNotNull(saved);

                ArgumentCaptor<AssessmentScoreRecord> captor = ArgumentCaptor.forClass(AssessmentScoreRecord.class);
                verify(repo).save(captor.capture());
                AssessmentScoreRecord finalSaved = captor.getValue();
                assertEquals("c2", finalSaved.getCourseId());
                JsonNode root = objectMapper.readTree(finalSaved.getScoreJson());
                assertEquals("c2", root.get("courseId").asText());
        }
}
