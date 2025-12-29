package com.example.coursemanagment;

import java.io.Serializable;

public class Course implements Serializable {
    public String courseId;
    public String courseName;
    public String courseCode;
    public String description;
    public String timing;
    public String location;

    // NEW FIELDS
    public String classId;      // Which class this course belongs to
    public String teacherId;    // Which teacher teaches it
    public String teacherName;  // For display purposes

    public Course() {}

    public Course(String courseId, String courseName, String courseCode, String description, String timing, String location, String classId, String teacherId, String teacherName) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.description = description;
        this.timing = timing;
        this.location = location;
        this.classId = classId;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
    }
}