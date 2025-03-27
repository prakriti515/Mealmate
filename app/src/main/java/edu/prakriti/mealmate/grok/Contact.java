package edu.prakriti.mealmate.grok;

public class Contact {
    public String name;
    public String number;
    public String email;

    public Contact(String name, String number) {
        this(name, number, null);
    }

    public Contact(String name, String number, String email) {
        this.name = name;
        this.number = number;
        this.email = email;
    }
}