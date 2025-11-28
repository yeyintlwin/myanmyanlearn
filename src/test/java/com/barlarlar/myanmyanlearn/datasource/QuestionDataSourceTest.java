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
        assertEquals(3, questions.size());
    }

    @Test
    public void testSampleQuestion1() {
        List<Question> questions = QuestionDataSource.getSampleQuestions();
        Question q1 = questions.get(0);

        assertEquals(1, q1.getQuestionNumber());
        assertEquals("jken", q1.getCourseId());
        assertEquals(0, q1.getCorrectOptionIndex());
        assertEquals(1.0f, q1.getMarks());
        assertEquals("EASY", q1.getDifficultyLevel());
        assertTrue(q1.getQuestionContent().contains("to be"));
        assertNotNull(q1.getExplanation());
        assertEquals(3, q1.getOptions().size());
        assertTrue(q1.getOptions().get(0).getIsCorrect());
    }

    @Test
    public void testSampleQuestion2() {
        List<Question> questions = QuestionDataSource.getSampleQuestions();
        Question q2 = questions.get(1);

        assertEquals(2, q2.getQuestionNumber());
        assertEquals("jken", q2.getCourseId());
        assertEquals(0, q2.getCorrectOptionIndex());
        assertEquals(1.0f, q2.getMarks());
        assertEquals("MEDIUM", q2.getDifficultyLevel());
        assertTrue(q2.getQuestionContent().contains("JavaScript"));
        assertNotNull(q2.getExplanation());
        assertEquals(3, q2.getOptions().size());
    }

    @Test
    public void testSampleQuestion3() {
        List<Question> questions = QuestionDataSource.getSampleQuestions();
        Question q3 = questions.get(2);

        assertEquals(3, q3.getQuestionNumber());
        assertEquals("jken", q3.getCourseId());
        assertEquals(0, q3.getCorrectOptionIndex());
        assertEquals(1.0f, q3.getMarks());
        assertEquals("EASY", q3.getDifficultyLevel());
        assertTrue(q3.getQuestionContent().contains("$x$"));
        assertNotNull(q3.getExplanation());
        assertEquals(3, q3.getOptions().size());
    }

    @Test
    public void testCreateQuestion() {
        List<QuestionOption> options = new ArrayList<>();
        options.add(new QuestionOption(0, "Correct Answer"));
        options.add(new QuestionOption(1, "Wrong Answer"));

        Question q = QuestionDataSource.createQuestion(
                "test-course",
                1,
                "Test question",
                0,
                2.0f,
                options);

        assertNotNull(q);
        assertEquals("test-course", q.getCourseId());
        assertEquals(1, q.getQuestionNumber());
        assertEquals("Test question", q.getQuestionContent());
        assertEquals(0, q.getCorrectOptionIndex());
        assertEquals(2.0f, q.getMarks());
        assertEquals(2, q.getOptions().size());
        assertEquals("multiple_choice", q.getQuestionType());
    }

    @Test
    public void testCreateCustomQuestion() {
        Question q = QuestionDataSource.createCustomQuestion(
                "custom-course",
                "chapter-1",
                5,
                "Custom question?",
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
        assertEquals("Custom question?", q.getQuestionContent());
        assertEquals(1, q.getCorrectOptionIndex());
        assertEquals(3.0f, q.getMarks());
        assertEquals("HARD", q.getDifficultyLevel());
        assertEquals(3, q.getOptions().size());
        assertEquals("multiple_choice", q.getQuestionType());

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

        // Check all questions have options
        for (Question question : questions) {
            assertNotNull(question.getOptions());
            assertTrue(question.getOptions().size() > 0);

            // Verify each option has an index and content
            for (QuestionOption option : question.getOptions()) {
                assertNotNull(option.getOptionIndex());
                assertNotNull(option.getOptionContent());
                assertNotNull(option.getIsCorrect());
            }
        }
    }
}
