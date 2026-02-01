package com.barlarlar.myanmyanlearn.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Subcontent {
    private int order;
    private String title;
    private String markdownPath;

    public Subcontent() {}
}
