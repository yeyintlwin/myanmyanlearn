package com.barlarlar.myanmyanlearn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Question {
    private String courseId;
    private String chapterId;
    private int questionNumber;
    private String questionContentPath;
    private List<QuestionOption> options;
    private List<List<QuestionOption>> slotOptions;

    public Question() {}

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

    public String getQuestionContentPath() {
        return questionContentPath;
    }

    public void setQuestionContentPath(String questionContentPath) {
        this.questionContentPath = questionContentPath;
    }

    public float getMarks() {
        return (float) getSlotCount();
    }

    public int getSlotCount() {
        if (slotOptions != null && !slotOptions.isEmpty()) {
            return slotOptions.size();
        }
        return 1;
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
                ", questionContentPath='" + questionContentPath + '\'' +
                ", marks=" + getMarks() +
                ", slotCount=" + getSlotCount() +
                '}';
    }
}
