package com.barlarlar.myanmyanlearn.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Course {
    private String id;
    private String title;
    private String description;
    private String language;
    private String logo;
    private List<Content> contents;

    public Course() {
    }
}
