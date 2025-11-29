package com.barlarlar.myanmyanlearn.datasource;

import com.barlarlar.myanmyanlearn.model.Question;
import com.barlarlar.myanmyanlearn.model.QuestionOption;

import java.util.ArrayList;
import java.util.List;

public class QuestionDataSource {
    public static List<Question> getSampleQuestions() {
        List<Question> questions = new ArrayList<>();

        List<QuestionOption> q1Options = new ArrayList<>();
        QuestionOption q1o1 = new QuestionOption(0, "I am happy.");
        q1o1.setIsCorrect(true);
        q1Options.add(q1o1);
        q1Options.add(new QuestionOption(1, "I is happy."));
        q1Options.add(new QuestionOption(2, "I be happy."));
        Question q1 = new Question("jken", 1, "Which sentence uses the correct form of to be?", 0);
        q1.setMarks(1.0f);
        
        q1.setExplanation("Singular subjects take is, plural take are, and I takes am.");
        q1.setOptions(q1Options);
        questions.add(q1);

        List<QuestionOption> q2Options = new ArrayList<>();
        QuestionOption q2o1 = new QuestionOption(0, "The sum of a and b.");
        q2o1.setIsCorrect(true);
        q2Options.add(q2o1);
        q2Options.add(new QuestionOption(1, "The product of a and b."));
        q2Options.add(new QuestionOption(2, "The string \"add\"."));
        Question q2 = new Question("jken", 2, "What does the JavaScript function return?", 0);
        q2.setMarks(1.0f);
        
        q2.setExplanation("Function add returns a + b.");
        q2.setOptions(q2Options);
        questions.add(q2);

        List<QuestionOption> q3Options = new ArrayList<>();
        QuestionOption q3o1 = new QuestionOption(0, "x = 3");
        q3o1.setIsCorrect(true);
        q3Options.add(q3o1);
        q3Options.add(new QuestionOption(1, "x = 6"));
        q3Options.add(new QuestionOption(2, "x = 2"));
        Question q3 = new Question("jken", 3, "Solve for $x$: 2x + 3 = 9", 0);
        q3.setMarks(1.0f);
        
        q3.setExplanation("Subtract 3 then divide by 2.");
        q3.setOptions(q3Options);
        questions.add(q3);

        List<QuestionOption> q4Options = new ArrayList<>();
        q4Options.add(new QuestionOption(0, "4"));
        q4Options.add(new QuestionOption(1, "6"));
        q4Options.add(new QuestionOption(2, "9"));
        QuestionOption q4o4 = new QuestionOption(3, "7");
        q4o4.setIsCorrect(true);
        q4Options.add(q4o4);
        Question q4 = new Question("jken", 4, "Which is a prime number?", 3);
        q4.setMarks(1.0f);
        
        q4.setExplanation("Prime numbers are divisible only by 1 and themselves.");
        q4.setOptions(q4Options);
        questions.add(q4);

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
        
        q.setMarks(marks);
        
        q.setOptions(options);
        for (QuestionOption option : options) {
            option.setIsCorrect(option.getOptionIndex() == correctOptionIndex);
        }
        return q;
    }
}
