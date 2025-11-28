package com.barlarlar.myanmyanlearn.model;

public class QuestionOption {
    private int optionIndex;
    private String optionContent;
    private boolean isCorrect;

    public QuestionOption(int optionIndex, String optionContent) {
        this.optionIndex = optionIndex;
        this.optionContent = optionContent;
        this.isCorrect = false;
    }

    public int getOptionIndex() {
        return optionIndex;
    }

    public void setOptionIndex(int optionIndex) {
        this.optionIndex = optionIndex;
    }

    public String getOptionContent() {
        return optionContent;
    }

    public void setOptionContent(String optionContent) {
        this.optionContent = optionContent;
    }

    public boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
}
