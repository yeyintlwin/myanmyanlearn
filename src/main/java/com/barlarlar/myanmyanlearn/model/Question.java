package com.barlarlar.myanmyanlearn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Question {
    private String courseId;
    private String chapterId;
    private int questionNumber;
    private String questionContentPath;
    private List<QuestionOption> options;
    private List<List<QuestionOption>> slotOptions;
    private String updatedAt;

    public Question() {
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
