package com.barlarlar.myanmyanlearn.model;

import java.util.List;

public class Question {
    private String courseId;
    private String chapterId;
    private int questionNumber;
    private String questionContent;
    private int correctOptionIndex;
    private float marks;
    
    private String explanation;
    private List<QuestionOption> options;

    public Question(String courseId, int questionNumber, String questionContent, int correctOptionIndex) {
        this.courseId = courseId;
        this.questionNumber = questionNumber;
        this.questionContent = questionContent;
        this.correctOptionIndex = correctOptionIndex;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(String chapterId) {
        this.chapterId = chapterId;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getQuestionContent() {
        return questionContent;
    }

    public void setQuestionContent(String questionContent) {
        this.questionContent = questionContent;
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public void setCorrectOptionIndex(int correctOptionIndex) {
        this.correctOptionIndex = correctOptionIndex;
    }

    public float getMarks() {
        return marks;
    }

    public void setMarks(float marks) {
        this.marks = marks;
    }

    

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<QuestionOption> getOptions() {
        return options;
    }

    public void setOptions(List<QuestionOption> options) {
        this.options = options;
    }

    @Override
    public String toString() {
        return "Question{" +
                "courseId='" + courseId + '\'' +
                ", chapterId='" + chapterId + '\'' +
                ", questionNumber=" + questionNumber +
                ", questionContent='" + questionContent + '\'' +
                ", correctOptionIndex=" + correctOptionIndex +
                ", marks=" + marks +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
