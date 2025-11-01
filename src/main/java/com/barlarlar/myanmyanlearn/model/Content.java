package com.barlarlar.myanmyanlearn.model;

import java.util.List;

public class Content {
    private int order;
    private String chapter;
    private String title;
    private List<Subcontent> subcontents;

    public Content() {}

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Subcontent> getSubcontents() {
        return subcontents;
    }

    public void setSubcontents(List<Subcontent> subcontents) {
        this.subcontents = subcontents;
    }
}
