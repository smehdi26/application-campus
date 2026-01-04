package com.example.coursemanagment;

public class Submission {
    public String studentName;
    public String studentId;
    public String link;
    public String date;
    public String grade;

    public Submission() {} // Needed for Firebase

    public Submission(String studentName, String studentId, String link, String date, String grade) {
        this.studentName = studentName;
        this.studentId = studentId;
        this.link = link;
        this.date = date;
        this.grade = grade;
    }
}