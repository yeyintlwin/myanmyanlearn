package com.barlarlar.myanmyanlearn.datasource;

import com.barlarlar.myanmyanlearn.model.Question;
import com.barlarlar.myanmyanlearn.model.QuestionOption;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QuestionDataSource
 */
public class QuestionDataSourceTest {

    @Test
    public void testGetSampleQuestions() {
        List<Question> questions = QuestionDataSource.getSampleQuestions();

        assertNotNull(questions);
        assertEquals(4, questions.size());
    }

    

    

    

    

    

    @Test
    public void testSampleQuestion6() {
        List<Question> questions = QuestionDataSource.getSampleQuestions();
        Question q6 = questions.stream().filter(q -> q.getQuestionNumber() == 6).findFirst().orElseThrow();

        assertEquals(6, q6.getQuestionNumber());
        assertEquals("jken", q6.getCourseId());
        assertEquals(9.0f, q6.getMarks());
        assertNotNull(q6.getQuestionContentPath());
        assertNotNull(q6.getExplanation());
        assertEquals(9, q6.getSlotCount());
        assertNotNull(q6.getSlotOptions());
        assertEquals(9, q6.getSlotOptions().size());
        for (int i = 0; i < 9; i++) {
            assertEquals(4, q6.getSlotOptions().get(i).size());
            assertTrue(q6.getSlotOptions().get(i).get(0).getIsCorrect());
        }
    }

    @Test
    public void testSampleQuestion7() {
        List<Question> questions = QuestionDataSource.getSampleQuestions();
        Question q7 = questions.stream().filter(q -> q.getQuestionNumber() == 7).findFirst().orElseThrow();

        assertEquals(7, q7.getQuestionNumber());
        assertEquals("jken", q7.getCourseId());
        assertEquals(3.0f, q7.getMarks());
        assertNotNull(q7.getQuestionContentPath());
        assertNotNull(q7.getExplanation());
        assertEquals(3, q7.getSlotCount());
        assertNotNull(q7.getSlotOptions());
        assertEquals(3, q7.getSlotOptions().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(4, q7.getSlotOptions().get(i).size());
            assertTrue(q7.getSlotOptions().get(i).get(0).getIsCorrect());
        }
    }

    @Test
    public void testSampleQuestion8() {
        List<Question> questions = QuestionDataSource.getSampleQuestions();
        Question q8 = questions.stream().filter(q -> q.getQuestionNumber() == 8).findFirst().orElseThrow();

        assertEquals(8, q8.getQuestionNumber());
        assertEquals("jken", q8.getCourseId());
        assertEquals(3.0f, q8.getMarks());
        assertNotNull(q8.getQuestionContentPath());
        assertNotNull(q8.getExplanation());
        assertEquals(3, q8.getSlotCount());
        assertNotNull(q8.getSlotOptions());
        assertEquals(3, q8.getSlotOptions().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(4, q8.getSlotOptions().get(i).size());
            assertTrue(q8.getSlotOptions().get(i).get(0).getIsCorrect());
        }
    }

    @Test
    public void testCreateQuestion() {
        List<QuestionOption> options = new ArrayList<>();
        options.add(new QuestionOption(0, "Correct Answer"));
        options.add(new QuestionOption(1, "Wrong Answer"));

        Question q = QuestionDataSource.createQuestion(
                "test-course",
                1,
                "/courses/test/questions/q1.md",
                0,
                2.0f,
                options);

        assertNotNull(q);
        assertEquals("test-course", q.getCourseId());
        assertEquals(1, q.getQuestionNumber());
        assertEquals("/courses/test/questions/q1.md", q.getQuestionContentPath());
        assertEquals(0, q.getCorrectOptionIndex());
        assertEquals(2.0f, q.getMarks());
        assertEquals(2, q.getOptions().size());
        
    }

    @Test
    public void testCreateCustomQuestion() {
        Question q = QuestionDataSource.createCustomQuestion(
                "custom-course",
                "chapter-1",
                5,
                "/courses/custom/questions/q5.md",
                1,
                "HARD",
                3.0f,
                "Option A",
                "Option B",
                "Option C");

        assertNotNull(q);
        assertEquals("custom-course", q.getCourseId());
        assertEquals("chapter-1", q.getChapterId());
        assertEquals(5, q.getQuestionNumber());
        assertEquals("/courses/custom/questions/q5.md", q.getQuestionContentPath());
        assertEquals(1, q.getCorrectOptionIndex());
        assertEquals(3.0f, q.getMarks());
        
        assertEquals(3, q.getOptions().size());
        

        // Verify correct option is marked
        assertTrue(q.getOptions().get(1).getIsCorrect());
        assertFalse(q.getOptions().get(0).getIsCorrect());
        assertFalse(q.getOptions().get(2).getIsCorrect());
    }

    @Test
    public void testQuestionOptionCreation() {
        QuestionOption option = new QuestionOption(0, "Test option");

        assertEquals(0, option.getOptionIndex());
        assertEquals("Test option", option.getOptionContent());
        assertFalse(option.getIsCorrect());
    }

    @Test
    public void testQuestionOptionCorrectness() {
        QuestionOption option = new QuestionOption(1, "Option content");

        assertFalse(option.getIsCorrect());
        option.setIsCorrect(true);
        assertTrue(option.getIsCorrect());
    }

    @Test
    public void testSampleQuestionsOptions() {
        List<Question> questions = QuestionDataSource.getSampleQuestions();

        // Check all questions have options either per-question or per-slot
        for (Question question : questions) {
            if (question.getSlotCount() > 0 && question.getSlotOptions() != null) {
                assertTrue(question.getSlotOptions().size() > 0);
                for (java.util.List<QuestionOption> slot : question.getSlotOptions()) {
                    assertTrue(slot.size() > 0);
                    for (QuestionOption option : slot) {
                        assertNotNull(option.getOptionIndex());
                        assertNotNull(option.getOptionContent());
                        assertNotNull(option.getIsCorrect());
                    }
                }
            } else {
                assertNotNull(question.getOptions());
                assertTrue(question.getOptions().size() > 0);
                for (QuestionOption option : question.getOptions()) {
                    assertNotNull(option.getOptionIndex());
                    assertNotNull(option.getOptionContent());
                    assertNotNull(option.getIsCorrect());
                }
            }
        }
    }
}
