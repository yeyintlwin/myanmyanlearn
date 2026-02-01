package com.barlarlar.myanmyanlearn.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseCatalog {
    private List<Course> courses;

    public CourseCatalog() {}
}
