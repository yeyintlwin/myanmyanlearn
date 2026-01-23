package com.barlarlar.myanmyanlearn.datasource;

import com.barlarlar.myanmyanlearn.model.Question;
import com.barlarlar.myanmyanlearn.model.QuestionOption;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QuestionDataSource {
    public static List<Question> getSampleQuestions() {
        List<Question> questions = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:courses/*/questions/*.json");

            for (Resource res : resources) {
                try {
                    Question q = mapper.readValue(res.getInputStream(), Question.class);
                    if (q != null) {
                        questions.add(q);
                    }
                } catch (IOException ignoreOne) {
                }
            }
            questions.sort((a, b) -> Integer.compare(a.getQuestionNumber(), b.getQuestionNumber()));
        } catch (IOException e) {
        }
        return questions;
    }

    public static Question createQuestion(
            String courseId,
            int questionNumber,
            String questionContentPath,
            int correctOptionIndex,
            float marks,
            List<QuestionOption> options) {
        Question q = new Question();
        q.setCourseId(courseId);
        q.setQuestionNumber(questionNumber);
        q.setQuestionContentPath(questionContentPath);
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
            String questionContentPath,
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

        Question q = new Question();
        q.setCourseId(courseId);
        q.setQuestionNumber(questionNumber);
        q.setQuestionContentPath(questionContentPath);
        q.setChapterId(chapterId);
        q.setOptions(options);
        for (QuestionOption option : options) {
            option.setIsCorrect(option.getOptionIndex() == correctOptionIndex);
        }
        return q;
    }
}
