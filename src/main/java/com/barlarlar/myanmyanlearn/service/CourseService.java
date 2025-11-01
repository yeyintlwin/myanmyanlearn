package com.barlarlar.myanmyanlearn.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.barlarlar.myanmyanlearn.model.Course;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CourseService {
    private volatile List<Course> cachedCourses;

    public List<Course> getAllCourses() {
        ensureLoaded();
        return cachedCourses;
    }

    public Course findById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        ensureLoaded();
        return cachedCourses.stream()
                .filter(c -> id.equalsIgnoreCase(c.getId()))
                .findFirst()
                .orElse(null);
    }

    public Course findByTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        ensureLoaded();
        return cachedCourses.stream()
                .filter(c -> title.equalsIgnoreCase(c.getTitle()))
                .findFirst()
                .orElse(null);
    }

    private synchronized void ensureLoaded() {
        if (cachedCourses != null) {
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:courses/*/course.json");

            List<Course> loaded = new ArrayList<>();
            for (Resource res : resources) {
                try {
                    Course course = mapper.readValue(res.getInputStream(), Course.class);
                    if (course != null) {
                        loaded.add(course);
                    }
                } catch (IOException ignoreOne) {
                    // skip malformed files
                }
            }
            cachedCourses = loaded.isEmpty() ? Collections.emptyList() : loaded;
        } catch (IOException e) {
            cachedCourses = Collections.emptyList();
        }
    }
}
