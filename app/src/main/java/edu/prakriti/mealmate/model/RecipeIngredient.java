package edu.prakriti.mealmate.model;

public class RecipeIngredient {
    private String name;
    private double quantity;
    private String unit;
    private double price;
    private String category;

    public RecipeIngredient() {
        // Default constructor
    }

    public RecipeIngredient(String name, double quantity, String unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.price = 0.0; // Default price
        this.category = "🥫 Condiments"; // Default category
    }

    public RecipeIngredient(String name, double quantity, String unit, double price) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.category = "🥫 Condiments"; // Default category
    }

    public RecipeIngredient(String name, double quantity, String unit, double price, String category) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        String result = quantity + " " + unit + " " + name;
        if (price > 0) {
            result += " ($" + String.format("%.2f", price) + ")";
        }
        return result;
    }
} 