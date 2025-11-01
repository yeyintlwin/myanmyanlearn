package com.barlarlar.myanmyanlearn.model;

public class Subcontent {
    private int order;
    private String title;
    private String markdownPath;

    public Subcontent() {}

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMarkdownPath() {
        return markdownPath;
    }

    public void setMarkdownPath(String markdownPath) {
        this.markdownPath = markdownPath;
    }
}
