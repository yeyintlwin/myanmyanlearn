package com.barlarlar.myanmyanlearn.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Question model
 */
public class QuestionTest {

    private Question question;
    private List<QuestionOption> options;

    @BeforeEach
    public void setUp() {
        question = new Question(
                "jken",
                1,
                "Test question content",
                0);
        question.setMarks(1.0f);
        question.setDifficultyLevel("EASY");

        options = new ArrayList<>();
        QuestionOption opt1 = new QuestionOption(0, "Option 1");
        opt1.setIsCorrect(true);
        options.add(opt1);
        options.add(new QuestionOption(1, "Option 2"));
        options.add(new QuestionOption(2, "Option 3"));
        question.setOptions(options);
    }

    @Test
    public void testQuestionCreation() {
        assertNotNull(question);
        assertEquals("jken", question.getCourseId());
        assertEquals(1, question.getQuestionNumber());
        assertEquals("Test question content", question.getQuestionContent());
        assertEquals(0, question.getCorrectOptionIndex());
    }

    @Test
    public void testQuestionMarks() {
        assertEquals(1.0f, question.getMarks());
        question.setMarks(2.5f);
        assertEquals(2.5f, question.getMarks());
    }

    @Test
    public void testQuestionDifficulty() {
        assertEquals("EASY", question.getDifficultyLevel());
        question.setDifficultyLevel("HARD");
        assertEquals("HARD", question.getDifficultyLevel());
    }

    @Test
    public void testQuestionOptions() {
        assertNotNull(question.getOptions());
        assertEquals(3, question.getOptions().size());
        assertEquals("Option 1", question.getOptions().get(0).getOptionContent());
        assertTrue(question.getOptions().get(0).getIsCorrect());
        assertFalse(question.getOptions().get(1).getIsCorrect());
    }

    @Test
    public void testQuestionActive() {
        assertTrue(question.getIsActive());
        question.setIsActive(false);
        assertFalse(question.getIsActive());
    }

    @Test
    public void testQuestionChapterId() {
        assertNull(question.getChapterId());
        question.setChapterId("chapter_1");
        assertEquals("chapter_1", question.getChapterId());
    }

    @Test
    public void testQuestionExplanation() {
        assertNull(question.getExplanation());
        String explanation = "This is the correct explanation.";
        question.setExplanation(explanation);
        assertEquals(explanation, question.getExplanation());
    }

    @Test
    public void testQuestionType() {
        assertEquals("multiple_choice", question.getQuestionType());
    }

    @Test
    public void testQuestionToString() {
        String toString = question.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Question{"));
        assertTrue(toString.contains("questionNumber=1"));
        assertTrue(toString.contains("marks=1.0"));
    }
}
