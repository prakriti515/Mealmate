package edu.prakriti.mealmate.grok;

public class GrokIngredient {
    public String name;
    public String date;
    public String category;
    public boolean isPurchased;
    public float price;
    public float quantity;
    public String unit;

    public GrokIngredient(String name, String date, String category, boolean isPurchased, float price) {
        this(name, date, category, isPurchased, price, 1.0f, "pcs");
    }

    public GrokIngredient(String name, String date, String category, boolean isPurchased, float price, float quantity, String unit) {
        this.name = name;
        this.date = date;
        this.category = category;
        this.isPurchased = isPurchased;
        this.price = price;
        this.quantity = quantity;
        this.unit = unit;
    }
}