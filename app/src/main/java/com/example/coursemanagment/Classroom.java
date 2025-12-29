package com.example.coursemanagment;

import java.io.Serializable;

public class Classroom implements Serializable {
    public String id;
    public String name;
    public String code;
    public int maxStudents = 35;

    public Classroom() {}

    public Classroom(String id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.maxStudents = 35;
    }
}