package com.barlarlar.myanmyanlearn.datasource;

import com.barlarlar.myanmyanlearn.model.Question;
import com.barlarlar.myanmyanlearn.model.QuestionOption;

import java.util.ArrayList;
import java.util.List;

public class QuestionDataSource {
    public static List<Question> getSampleQuestions() {
        List<Question> questions = new ArrayList<>();

        List<QuestionOption> q1Options = new ArrayList<>();
        QuestionOption q1o1 = new QuestionOption(0, "Option 1");
        q1o1.setIsCorrect(true);
        q1Options.add(q1o1);
        q1Options.add(new QuestionOption(1, "Option 2"));
        q1Options.add(new QuestionOption(2, "Option 3"));
        Question q1 = new Question("jken", 1, "Grammar: to be", 0);
        q1.setMarks(1.0f);
        q1.setDifficultyLevel("EASY");
        q1.setExplanation("Explanation for question 1");
        q1.setOptions(q1Options);
        questions.add(q1);

        List<QuestionOption> q2Options = new ArrayList<>();
        QuestionOption q2o1 = new QuestionOption(0, "Option A");
        q2o1.setIsCorrect(true);
        q2Options.add(q2o1);
        q2Options.add(new QuestionOption(1, "Option B"));
        q2Options.add(new QuestionOption(2, "Option C"));
        Question q2 = new Question("jken", 2, "JavaScript basics", 0);
        q2.setMarks(1.0f);
        q2.setDifficultyLevel("MEDIUM");
        q2.setExplanation("Explanation for question 2");
        q2.setOptions(q2Options);
        questions.add(q2);

        List<QuestionOption> q3Options = new ArrayList<>();
        QuestionOption q3o1 = new QuestionOption(0, "Opt 1");
        q3o1.setIsCorrect(true);
        q3Options.add(q3o1);
        q3Options.add(new QuestionOption(1, "Opt 2"));
        q3Options.add(new QuestionOption(2, "Opt 3"));
        Question q3 = new Question("jken", 3, "Solve for $x$", 0);
        q3.setMarks(1.0f);
        q3.setDifficultyLevel("EASY");
        q3.setExplanation("Explanation for question 3");
        q3.setOptions(q3Options);
        questions.add(q3);

        return questions;
    }

    public static Question createQuestion(
            String courseId,
            int questionNumber,
            String questionContent,
            int correctOptionIndex,
            float marks,
            List<QuestionOption> options) {
        Question q = new Question(courseId, questionNumber, questionContent, correctOptionIndex);
        q.setMarks(marks);
        q.setOptions(options);
        q.setQuestionType("multiple_choice");

        for (QuestionOption option : options) {
            option.setIsCorrect(option.getOptionIndex() == correctOptionIndex);
        }
        return q;
    }

    public static Question createCustomQuestion(
            String courseId,
            String chapterId,
            int questionNumber,
            String questionContent,
            int correctOptionIndex,
            String difficultyLevel,
            float marks,
            String optA,
            String optB,
            String optC) {
        List<QuestionOption> options = new ArrayList<>();
        options.add(new QuestionOption(0, optA));
        options.add(new QuestionOption(1, optB));
        options.add(new QuestionOption(2, optC));

        Question q = new Question(courseId, questionNumber, questionContent, correctOptionIndex);
        q.setChapterId(chapterId);
        q.setDifficultyLevel(difficultyLevel);
        q.setMarks(marks);
        q.setQuestionType("multiple_choice");
        q.setOptions(options);

        for (QuestionOption option : options) {
            option.setIsCorrect(option.getOptionIndex() == correctOptionIndex);
        }
        return q;
    }
}
