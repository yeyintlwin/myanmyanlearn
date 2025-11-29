package com.barlarlar.myanmyanlearn.datasource;

import com.barlarlar.myanmyanlearn.model.Question;
import com.barlarlar.myanmyanlearn.model.QuestionOption;

import java.util.ArrayList;
import java.util.List;

public class QuestionDataSource {
    public static List<Question> getSampleQuestions() {
        List<Question> questions = new ArrayList<>();

        

        // Q6: 9 blanks, each with 4 options
        List<List<QuestionOption>> q6SlotOptions = new ArrayList<>();
        java.util.List<Integer> q6Correct = new java.util.ArrayList<>();
        for (int s = 0; s < 9; s++) {
            List<QuestionOption> slot = new ArrayList<>();
            slot.add(new QuestionOption(0, "A" + (s + 1)));
            slot.add(new QuestionOption(1, "B" + (s + 1)));
            slot.add(new QuestionOption(2, "C" + (s + 1)));
            slot.add(new QuestionOption(3, "D" + (s + 1)));
            q6SlotOptions.add(slot);
            q6Correct.add(0); // correct: A for each slot
        }
        Question q6 = new Question("jken", 6,
                "Complete the sequence: [1] [2] [3] [4] [5] [6] [7] [8] [9]",
                0);
        q6.setMarks(9.0f);
        q6.setSlotCount(9);
        q6.setCorrectSlotOptionIndices(q6Correct);
        q6.setSlotOptions(q6SlotOptions);
        // mark correctness flags per slot
        for (int i = 0; i < q6SlotOptions.size(); i++) {
            for (QuestionOption opt : q6SlotOptions.get(i)) {
                opt.setIsCorrect(opt.getOptionIndex() == q6Correct.get(i));
            }
        }
        q6.setExplanation("Pick the appropriate option for each blank (A1..A9 correct).");
        questions.add(q6);

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
