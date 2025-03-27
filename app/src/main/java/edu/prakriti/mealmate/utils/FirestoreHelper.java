package edu.prakriti.mealmate.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.prakriti.mealmate.model.Recipe;

public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface FirestoreCallback {
        void onCallback(List<Recipe> recipeList);
    }

    public void loadRecipes(FirestoreCallback callback) {
        db.collection("recipes")
                .orderBy("timestamp", Query.Direction.DESCENDING) // 🔥 Order by latest timestamp
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Recipe> recipeList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Recipe recipe = new Recipe();
                                recipe.setRecipeName(document.getString("recipeName"));
                                recipe.setCookTime(document.getString("cookTime"));
                                recipe.setPhotoUrl(document.getString("photoUrl"));
                                recipe.setTimestamp(document.getLong("timestamp"));
                                recipe.setRecipeId(document.getId());

                                // ✅ Convert Ingredients Map
                                Map<String, List<String>> ingredientsMap = new HashMap<>();
                                Map<String, Object> firestoreIngredients = (Map<String, Object>) document.get("ingredients");
                                if (firestoreIngredients != null) {
                                    for (Map.Entry<String, Object> entry : firestoreIngredients.entrySet()) {
                                        ingredientsMap.put(entry.getKey(), (List<String>) entry.getValue());
                                    }
                                }
                                recipe.setIngredients(ingredientsMap);

                                // ✅ Convert Instructions Array
                                List<Map<String, Object>> instructionsList = (List<Map<String, Object>>) document.get("instructions");
                                if (instructionsList != null) {
                                    recipe.setInstructions(instructionsList);
                                }

                                // ✅ Add to list
                                recipeList.add(recipe);

                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing recipe: " + e.getMessage());
                            }
                        }
                        callback.onCallback(recipeList);
                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                    }
                });
    }

    /**
     * Load favorite recipes for the current user
     * @param callback Callback to handle the loaded favorite recipes
     */
    public void loadFavoriteRecipes(FirestoreCallback callback) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                
        if (userId == null) {
            Log.d(TAG, "Cannot load favorites: User not logged in");
            callback.onCallback(new ArrayList<>()); // Return empty list if user is not signed in
            return;
        }
        
        Log.d(TAG, "Loading favorite recipes for user: " + userId);
        db.collection("user_favorites")
                .document(userId)
                .collection("recipes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Recipe> favoriteRecipes = new ArrayList<>();
                        if (task.getResult().isEmpty()) {
                            Log.d(TAG, "No favorite recipes found for user: " + userId);
                            callback.onCallback(favoriteRecipes);
                            return;
                        }
                        
                        Log.d(TAG, "Found " + task.getResult().size() + " favorite recipes");
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Recipe recipe = new Recipe();
                                recipe.setRecipeName(document.getString("recipeName"));
                                recipe.setCookTime(document.getString("cookTime"));
                                recipe.setPhotoUrl(document.getString("photoUrl"));
                                recipe.setTimestamp(document.getLong("timestamp"));
                                recipe.setFavorite(true);
                                
                                // Set the recipeId - could be either the document ID or a field
                                String recipeId = document.getString("recipeId");
                                if (recipeId != null) {
                                    recipe.setRecipeId(recipeId);
                                } else {
                                    recipe.setRecipeId(document.getId());
                                }
                                
                                // ✅ Convert Ingredients Map
                                Map<String, List<String>> ingredientsMap = new HashMap<>();
                                Map<String, Object> firestoreIngredients = (Map<String, Object>) document.get("ingredients");
                                if (firestoreIngredients != null) {
                                    for (Map.Entry<String, Object> entry : firestoreIngredients.entrySet()) {
                                        ingredientsMap.put(entry.getKey(), (List<String>) entry.getValue());
                                    }
                                }
                                recipe.setIngredients(ingredientsMap);

                                // ✅ Convert Instructions Array
                                List<Map<String, Object>> instructionsList = (List<Map<String, Object>>) document.get("instructions");
                                if (instructionsList != null) {
                                    recipe.setInstructions(instructionsList);
                                }
                                
                                favoriteRecipes.add(recipe);
                                Log.d(TAG, "Added favorite recipe: " + recipe.getRecipeName());
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing favorite recipe: " + e.getMessage(), e);
                            }
                        }
                        callback.onCallback(favoriteRecipes);
                    } else {
                        Log.e(TAG, "Error getting favorite recipes: ", task.getException());
                        callback.onCallback(new ArrayList<>()); // Return empty list on error
                    }
                });
    }

    /**
     * Add a recipe to favorites
     * @param recipe The recipe to add to favorites
     * @return true if added successfully, false otherwise
     */
    public boolean addToFavorites(Recipe recipe) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                
        if (userId == null || recipe == null) {
            Log.e(TAG, "Cannot add to favorites: User not logged in or recipe is null");
            return false;
        }
        
        try {
            recipe.setFavorite(true);
            
            // If recipe has a recipeId, use it as the document ID
            if (recipe.getRecipeId() != null && !recipe.getRecipeId().isEmpty()) {
                db.collection("user_favorites")
                        .document(userId)
                        .collection("recipes")
                        .document(recipe.getRecipeId())
                        .set(recipe);
                Log.d(TAG, "Added recipe to favorites with ID: " + recipe.getRecipeId());
            } else {
                // If no recipeId, create a new document
                db.collection("user_favorites")
                        .document(userId)
                        .collection("recipes")
                        .add(recipe)
                        .addOnSuccessListener(docRef -> {
                            Log.d(TAG, "Added recipe to favorites with new ID: " + docRef.getId());
                        });
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error adding to favorites: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Remove a recipe from favorites
     * @param recipeTimestamp The timestamp of the recipe to remove
     * @return true if removed successfully, false otherwise
     */
    public boolean removeFromFavorites(long recipeTimestamp) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                
        if (userId == null) {
            return false;
        }
        
        try {
            db.collection("user_favorites")
                    .document(userId)
                    .collection("recipes")
                    .whereEqualTo("timestamp", recipeTimestamp)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                document.getReference().delete();
                            }
                        }
                    });
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error removing from favorites: " + e.getMessage());
            return false;
        }
    }
}
