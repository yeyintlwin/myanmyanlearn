package com.barlarlar.myanmyanlearn.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Content {
    private int order;
    private String chapter;
    private String title;
    private List<Subcontent> subcontents;

    public Content() {}
}
