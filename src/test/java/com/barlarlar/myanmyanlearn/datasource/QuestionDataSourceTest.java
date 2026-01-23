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
        assertTrue(questions.size() >= 8);
    }

    

    

    

    

    

    @Test
    public void testAllQuestionsHaveValidSlotOptions() {
        List<Question> questions = QuestionDataSource.getSampleQuestions();
        for (Question q : questions) {
            assertNotNull(q.getQuestionContentPath());
            assertTrue(q.getSlotCount() >= 1);
            assertNotNull(q.getSlotOptions());
            assertEquals(q.getSlotCount(), q.getSlotOptions().size());
            for (java.util.List<QuestionOption> slot : q.getSlotOptions()) {
                assertTrue(slot.size() >= 2);
                int correctCount = 0;
                for (QuestionOption opt : slot) {
                    assertNotNull(opt.getOptionContent());
                    assertNotNull(opt.getOptionIndex());
                    if (opt.getIsCorrect()) correctCount++;
                }
                assertTrue(correctCount >= 1);
            }
        }
    }

    @Test
    public void testQuestionsContainCourseAndChapter() {
        List<Question> questions = QuestionDataSource.getSampleQuestions();
        for (Question q : questions) {
            assertEquals("jken", q.getCourseId());
            assertNotNull(q.getChapterId());
            assertTrue(!q.getChapterId().isBlank());
        }
    }

    @Test
    public void testSlotOptionsMarking() {
        List<Question> questions = QuestionDataSource.getSampleQuestions();
        for (Question q : questions) {
            for (java.util.List<QuestionOption> slot : q.getSlotOptions()) {
                long trueCount = slot.stream().filter(QuestionOption::getIsCorrect).count();
                assertTrue(trueCount >= 1);
            }
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
        assertEquals(1, q.getSlotCount());
        assertEquals(1.0f, q.getMarks());
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
        assertEquals(1, q.getSlotCount());
        assertEquals(1.0f, q.getMarks());
        
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
