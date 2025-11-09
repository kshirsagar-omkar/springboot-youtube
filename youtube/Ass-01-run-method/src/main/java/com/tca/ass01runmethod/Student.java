package com.tca.ass01runmethod;


import org.springframework.stereotype.Component;

//@Component
public class Student {

    private String name = "omi";

    public Student(String name) {
        this.name = name;
    }

    public Student() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
