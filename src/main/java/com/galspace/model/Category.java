package com.galspace.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    private String id;
    private String name;
    private String color;
    private int sortOrder;
    private Long createdTime;
    private int gameCount;
}
