package edu.prakriti.mealmate.models;

import java.io.Serializable;
import java.util.List;

public class Recipe implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private String cookingTime;
    private List<String> ingredients;
    private List<String> instructions;
    private String category;
    
    // Empty constructor for Firebase
    public Recipe() {
    }
    
    public Recipe(String id, String name, String description, String imageUrl, String cookingTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.cookingTime = cookingTime;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getCookingTime() {
        return cookingTime;
    }
    
    public void setCookingTime(String cookingTime) {
        this.cookingTime = cookingTime;
    }
    
    public List<String> getIngredients() {
        return ingredients;
    }
    
    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }
    
    public List<String> getInstructions() {
        return instructions;
    }
    
    public void setInstructions(List<String> instructions) {
        this.instructions = instructions;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
} 