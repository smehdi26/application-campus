package com.example.coursemanagment;

import java.io.Serializable;

public class Classroom implements Serializable {
    public String id;
    public String name;
    public String code;
    public int maxStudents = 35;
    public String departmentId; // NEW FIELD

    public Classroom() {}

    public Classroom(String id, String name, String code, String departmentId) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.departmentId = departmentId;
        this.maxStudents = 35;
    }
}