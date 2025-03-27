package edu.prakriti.mealmate.models;

/**
 * Model class for grocery ingredients that includes details like price, quantity, etc.
 */
public class GroceryIngredient {
    private long id;
    private String name;
    private String category;
    private double quantity;
    private String unit;
    private double price;
    private boolean purchased;
    private String date;
    private String mealName;

    public GroceryIngredient() {
        // Default constructor
    }

    public GroceryIngredient(long id, String name, String category, double quantity, String unit, 
                           double price, boolean purchased, String date, String mealName) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.purchased = purchased;
        this.date = date;
        this.mealName = mealName;
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMealName() {
        return mealName;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    @Override
    public String toString() {
        return name + " - " + quantity + " " + unit;
    }
} 