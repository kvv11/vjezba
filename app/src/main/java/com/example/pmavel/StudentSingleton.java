package com.example.pmavel;

import java.util.ArrayList;
import java.util.List;

public class StudentSingleton {
    private static StudentSingleton instance;
    private List<Student> studenti = new ArrayList<>();

    private static StudentSingleton oInstance = null;
    protected StudentSingleton() {
    }
    public static StudentSingleton getInstance() {
        if(oInstance == null) {
            oInstance = new StudentSingleton();
        }
        return oInstance;
    }
    public List<Student> getStudenti(){
        return studenti;
    }

    public void setStudent (Student student) {
        studenti.add(student);
    }
}