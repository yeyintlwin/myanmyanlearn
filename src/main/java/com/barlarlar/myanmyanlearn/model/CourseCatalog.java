package com.barlarlar.myanmyanlearn.model;

import java.util.List;

public class CourseCatalog {
    private List<Course> courses;

    public CourseCatalog() {}

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }
}
