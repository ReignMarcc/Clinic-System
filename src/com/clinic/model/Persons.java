package com.clinic.model;

public abstract class Persons {
    protected String name;
    protected String contactNumber;
    protected int age ;

    public Persons() {
        this.name = "";
        this.age = 0;
        this.contactNumber= "";
    }

    public Persons(String name){
        this();
        this.name = name;
    }

    public Persons(String name, int age) {
        this(name);
        this.age = age;
    }

    public Persons(String name, int age, String contactNumber) {
        this(name, age);
        this.contactNumber = contactNumber;
    }

    public String getName() {
        return name;
    }
    public int getAge() {
        return age;
    }
    public String getContactNumber() {
        return contactNumber;
    }

    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public abstract String displayInfo();

    @Override
    public String toString() {
        return displayInfo();
    }
}