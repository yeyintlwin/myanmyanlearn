package com.barlarlar.myanmyanlearn.model;

import java.util.List;

public class Question {
    private String courseId;
    private String chapterId;
    private int questionNumber;
    private String questionContent;
    private int correctOptionIndex;
    private float marks;
    private int slotCount;
    private java.util.List<Integer> correctSlotOptionIndices;
    
    private String explanation;
    private List<QuestionOption> options;
    private List<List<QuestionOption>> slotOptions;

    public Question() {}

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

    public int getSlotCount() {
        return slotCount;
    }

    public void setSlotCount(int slotCount) {
        this.slotCount = slotCount;
    }

    public java.util.List<Integer> getCorrectSlotOptionIndices() {
        return correctSlotOptionIndices;
    }

    public void setCorrectSlotOptionIndices(java.util.List<Integer> correctSlotOptionIndices) {
        this.correctSlotOptionIndices = correctSlotOptionIndices;
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

    public List<List<QuestionOption>> getSlotOptions() {
        return slotOptions;
    }

    public void setSlotOptions(List<List<QuestionOption>> slotOptions) {
        this.slotOptions = slotOptions;
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
                ", slotCount=" + slotCount +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
