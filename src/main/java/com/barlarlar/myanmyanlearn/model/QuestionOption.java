package com.barlarlar.myanmyanlearn.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionOption {
    private int optionIndex;
    private String optionContent;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean isCorrect;

    public QuestionOption() {}

    public QuestionOption(int optionIndex, String optionContent) {
        this.optionIndex = optionIndex;
        this.optionContent = optionContent;
        this.isCorrect = false;
    }

    public boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
}
