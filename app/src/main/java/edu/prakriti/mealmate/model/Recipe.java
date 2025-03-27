package edu.prakriti.mealmate.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Recipe implements Parcelable {
    private static final String TAG = "Recipe";
    
    private String recipeName;
    private String cookTime;
    private String photoUrl;
    private Map<String, List<String>> ingredients;
    private List<Map<String, Object>> instructions;
    private long timestamp;
    private boolean favorite;
    private String recipeId;

    // Default constructor required for Firestore
    public Recipe() {}

    public Recipe(String recipeName, String cookTime, String photoUrl,
                  Map<String, List<String>> ingredients, List<Map<String, Object>> instructions, long timestamp) {
        this.recipeName = recipeName;
        this.cookTime = cookTime;
        this.photoUrl = photoUrl;
        this.ingredients = ingredients != null ? ingredients : new HashMap<>();
        this.instructions = instructions != null ? instructions : new ArrayList<>();
        this.timestamp = timestamp;
        this.favorite = false;
    }

    // Constructor with favorite and recipeId parameters
    public Recipe(String recipeName, String cookTime, String photoUrl,
                 Map<String, List<String>> ingredients, List<Map<String, Object>> instructions, 
                 long timestamp, boolean favorite, String recipeId) {
        this.recipeName = recipeName;
        this.cookTime = cookTime;
        this.photoUrl = photoUrl;
        this.ingredients = ingredients != null ? ingredients : new HashMap<>();
        this.instructions = instructions != null ? instructions : new ArrayList<>();
        this.timestamp = timestamp;
        this.favorite = favorite;
        this.recipeId = recipeId;
    }

    // Parcelable implementation
    protected Recipe(Parcel in) {
        try {
            recipeName = in.readString();
            cookTime = in.readString();
            photoUrl = in.readString();
            
            // Read ingredients map
            int ingredientsSize = in.readInt();
            Log.d(TAG, "Reading ingredients map of size: " + ingredientsSize);
            
            ingredients = new HashMap<>();
            for (int i = 0; i < ingredientsSize; i++) {
                String category = in.readString();
                int listSize = in.readInt();
                List<String> ingredientList = new ArrayList<>();
                
                for (int j = 0; j < listSize; j++) {
                    ingredientList.add(in.readString());
                }
                
                ingredients.put(category, ingredientList);
            }
            
            // Read instructions list with enhanced error handling
            int instructionsSize = in.readInt();
            Log.d(TAG, "Reading instructions list of size: " + instructionsSize);
            
            instructions = new ArrayList<>();
            for (int i = 0; i < instructionsSize; i++) {
                try {
                    Map<String, Object> step = new HashMap<>();
                    
                    // Read step number
                    int stepNumber = in.readInt();
                    if (stepNumber <= 0) {
                        Log.w(TAG, "Invalid step number " + stepNumber + " at position " + i + ", using position");
                        stepNumber = i + 1;
                    }
                    
                    // Read instruction text
                    String instruction = in.readString();
                    if (instruction == null || instruction.isEmpty()) {
                        Log.w(TAG, "Empty instruction at position " + i + ", using default text");
                        instruction = "Step " + stepNumber + " - No details provided";
                    }
                    
                    // Store data in the step map
                    step.put("stepNumber", stepNumber);
                    step.put("instruction", instruction);
                    instructions.add(step);
                    
                    Log.d(TAG, "Read instruction " + i + ": " + stepNumber + " - " + instruction);
                } catch (Exception e) {
                    Log.e(TAG, "Error reading instruction at position " + i, e);
                    
                    // Create a default instruction in case of error
                    Map<String, Object> defaultStep = new HashMap<>();
                    defaultStep.put("stepNumber", i + 1);
                    defaultStep.put("instruction", "Step " + (i + 1) + " - Error loading details");
                    instructions.add(defaultStep);
                }
            }
            
            timestamp = in.readLong();
            favorite = in.readByte() != 0;
            recipeId = in.readString();
            
            Log.d(TAG, "Parcelable deserialized: " + recipeName + " (ID: " + recipeId + ")" + 
                  ", Instructions: " + (instructions != null ? instructions.size() : "null"));
        } catch (Exception e) {
            Log.e(TAG, "Error deserializing Recipe from Parcel", e);
            // Initialize with defaults to prevent null pointer exceptions
            ingredients = new HashMap<>();
            instructions = new ArrayList<>();
        }
    }

    public static final Creator<Recipe> CREATOR = new Creator<Recipe>() {
        @Override
        public Recipe createFromParcel(Parcel in) {
            return new Recipe(in);
        }

        @Override
        public Recipe[] newArray(int size) {
            return new Recipe[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        try {
            // Write basic properties
            dest.writeString(recipeName);
            dest.writeString(cookTime);
            dest.writeString(photoUrl);
            
            // Write ingredients map
            dest.writeInt(ingredients != null ? ingredients.size() : 0);
            Log.d(TAG, "Writing ingredients map of size: " + (ingredients != null ? ingredients.size() : 0));
            
            if (ingredients != null) {
                for (Map.Entry<String, List<String>> entry : ingredients.entrySet()) {
                    dest.writeString(entry.getKey());
                    List<String> list = entry.getValue();
                    dest.writeInt(list != null ? list.size() : 0);
                    
                    if (list != null) {
                        for (String item : list) {
                            dest.writeString(item);
                        }
                    }
                }
            }
            
            // Write instructions list with enhanced error handling
            int instructionsSize = instructions != null ? instructions.size() : 0;
            dest.writeInt(instructionsSize);
            Log.d(TAG, "Writing instructions list of size: " + instructionsSize);
            
            if (instructions != null) {
                for (int i = 0; i < instructions.size(); i++) {
                    try {
                        Map<String, Object> step = instructions.get(i);
                        if (step == null) {
                            Log.w(TAG, "Instruction at position " + i + " is null, using default values");
                            // Write default values for null steps
                            dest.writeInt(i + 1);
                            dest.writeString("Step " + (i + 1) + " - No details available");
                            continue;
                        }
                        
                        // Get step number with type handling
                        Object stepNumberObj = step.get("stepNumber");
                        int stepNumber = 0;
                        
                        if (stepNumberObj instanceof Integer) {
                            stepNumber = (Integer) stepNumberObj;
                        } else if (stepNumberObj instanceof Long) {
                            stepNumber = ((Long) stepNumberObj).intValue();
                        } else if (stepNumberObj instanceof String) {
                            try {
                                stepNumber = Integer.parseInt((String) stepNumberObj);
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Could not parse step number: " + stepNumberObj);
                                stepNumber = i + 1;  // Fallback to position
                            }
                        } else if (stepNumberObj == null) {
                            Log.w(TAG, "Step number is null for instruction " + i);
                            stepNumber = i + 1;  // Fallback to position
                        }
                        
                        // Ensure step number is valid
                        if (stepNumber <= 0) {
                            Log.w(TAG, "Invalid step number " + stepNumber + " at position " + i);
                            stepNumber = i + 1;  // Fallback to position
                        }
                        
                        // Get instruction text with null checking
                        Object instructionObj = step.get("instruction");
                        String instructionText;
                        
                        if (instructionObj != null) {
                            instructionText = instructionObj.toString();
                            
                            // Check for empty text
                            if (instructionText.trim().isEmpty()) {
                                Log.w(TAG, "Empty instruction text for step " + stepNumber);
                                instructionText = "Step " + stepNumber + " - No details available";
                            }
                        } else {
                            Log.w(TAG, "Null instruction text for step " + stepNumber);
                            instructionText = "Step " + stepNumber + " - No details available";
                        }
                        
                        // Write to parcel
                        dest.writeInt(stepNumber);
                        dest.writeString(instructionText);
                        
                        Log.d(TAG, "Writing instruction " + i + ": " + stepNumber + " - " + instructionText);
                    } catch (Exception e) {
                        Log.e(TAG, "Error writing instruction at position " + i, e);
                        
                        // Write fallback values in case of error
                        dest.writeInt(i + 1);
                        dest.writeString("Step " + (i + 1) + " - Error saving details");
                    }
                }
            }
            
            dest.writeLong(timestamp);
            dest.writeByte((byte) (favorite ? 1 : 0));
            dest.writeString(recipeId);
            
            Log.d(TAG, "Parcelable serialized: " + recipeName + " (ID: " + recipeId + ")" + 
                  ", Instructions: " + (instructions != null ? instructions.size() : "null"));
        } catch (Exception e) {
            Log.e(TAG, "Error serializing Recipe to Parcel", e);
            e.printStackTrace();
        }
    }

    // Getters and Setters
    public String getRecipeName() { return recipeName; }
    public void setRecipeName(String recipeName) { this.recipeName = recipeName; }

    public String getCookTime() { return cookTime; }
    public void setCookTime(String cookTime) { this.cookTime = cookTime; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Map<String, List<String>> getIngredients() { return ingredients; }
    public void setIngredients(Map<String, List<String>> ingredients) {
        this.ingredients = ingredients != null ? ingredients : new HashMap<>();
    }

    public List<Map<String, Object>> getInstructions() { return instructions; }
    public void setInstructions(List<Map<String, Object>> instructions) {
        this.instructions = instructions != null ? instructions : new ArrayList<>();
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    
    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }

    /**
     * Checks if this recipe is empty or missing essential data
     * @return true if the recipe is incomplete, false otherwise
     */
    public boolean isEmpty() {
        boolean basicInfoMissing = recipeName == null || recipeName.isEmpty() || 
                                  cookTime == null || cookTime.isEmpty();
        
        boolean ingredientsMissing = ingredients == null || ingredients.isEmpty();
        
        boolean instructionsMissing = instructions == null || instructions.isEmpty();
        
        return basicInfoMissing || ingredientsMissing || instructionsMissing;
    }
}