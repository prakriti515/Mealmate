package edu.prakriti.mealmate.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.RecipeDetailActivity;
import edu.prakriti.mealmate.models.Recipe;
import com.google.android.material.button.MaterialButton;
import android.widget.ImageButton;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;
    private Context context;

    public RecipeAdapter(List<Recipe> recipeList, Context context) {
        this.recipeList = recipeList;
        this.context = context;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recipe_card, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);
        
        holder.recipeName.setText(recipe.getName());
        holder.cookTime.setText(recipe.getCookingTime());
        
        // Set count for ingredients and steps
        int ingredientCount = recipe.getIngredients() != null ? recipe.getIngredients().size() : 0;
        int instructionCount = recipe.getInstructions() != null ? recipe.getInstructions().size() : 0;
        
        holder.ingredients.setText(ingredientCount + " Ingredients");
        holder.steps.setText(instructionCount + " Steps");
        
        // If there's an image URL in the recipe, load it here
        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            Glide.with(context)
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.ic_men)  // Use an existing placeholder
                .into(holder.recipeImage);
        }
        
        // Load recipe image with improved error handling
        String imageUrl = recipe.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d("RecipeAdapter", "Loading image: " + imageUrl);
            
            // Handle edge cases like missing http/https prefix
            if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && 
                !imageUrl.startsWith("file://") && !imageUrl.startsWith("content://")) {
                Log.w("RecipeAdapter", "Image URL missing protocol, adding https://");
                imageUrl = "https://" + imageUrl;
            }
            
            try {
                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_men)
                    .error(R.drawable.no_image_placeholder)
                    .timeout(10000) // 10 second timeout
                    .into(holder.recipeImage);
            } catch (Exception e) {
                Log.e("RecipeAdapter", "Error loading image", e);
                holder.recipeImage.setImageResource(R.drawable.no_image_placeholder);
            }
        } else {
            holder.recipeImage.setImageResource(R.drawable.no_image_placeholder);
        }
        
        // Set menu button click listener
        holder.menuButton.setOnClickListener(v -> {
            showRecipeMenu(v, recipe);
        });
        
        // Set card click listener to open RecipeDetailActivity
        holder.itemView.setOnClickListener(v -> {
            openRecipeDetail(recipe);
        });
        
        // Set button click listener
        if (holder.viewButton != null) {
            holder.viewButton.setOnClickListener(v -> {
                openRecipeDetail(recipe);
            });
        }
    }
    
    /**
     * Shows a popup menu with recipe options
     */
    private void showRecipeMenu(View view, Recipe recipe) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.inflate(R.menu.recipe_menu);
        
        // Handle menu item clicks
        popup.setOnMenuItemClickListener(item -> {
            try {
                edu.prakriti.mealmate.model.Recipe detailRecipe = convertToDetailRecipe(recipe);
                
                if (detailRecipe == null) {
                    throw new IllegalStateException("Failed to convert recipe");
                }
                
                // Open detail activity with appropriate action
                Intent intent = new Intent(context, RecipeDetailActivity.class);
                intent.putExtra("RECIPE", detailRecipe);
                
                // Add current position info for navigation
                int currentPosition = recipeList.indexOf(recipe);
                if (currentPosition >= 0) {
                    intent.putExtra("CURRENT_POSITION", currentPosition);
                    intent.putExtra("TOTAL_RECIPES", recipeList.size());
                    
                    if (currentPosition > 0) {
                        Recipe prevRecipe = recipeList.get(currentPosition - 1);
                        intent.putExtra("PREV_RECIPE_ID", prevRecipe.getId());
                    }
                    
                    if (currentPosition < recipeList.size() - 1) {
                        Recipe nextRecipe = recipeList.get(currentPosition + 1);
                        intent.putExtra("NEXT_RECIPE_ID", nextRecipe.getId());
                    }
                }
                
                // Handle action based on menu item
                int itemId = item.getItemId();
                if (itemId == R.id.action_edit_recipe) {
                    intent.putExtra("ACTION", "EDIT_RECIPE");
                } else if (itemId == R.id.action_del_recipe) {
                    intent.putExtra("ACTION", "DELETE_RECIPE");
                } else if (itemId == R.id.action_share) {
                    intent.putExtra("ACTION", "SHARE_RECIPE");
                } else if (itemId == R.id.action_favorite) {
                    intent.putExtra("ACTION", "TOGGLE_FAVORITE");
                }
                
                // Start activity with transition
                if (context instanceof android.app.Activity) {
                    android.app.Activity activity = (android.app.Activity) context;
                    activity.startActivity(intent);
                    activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else {
                    context.startActivity(intent);
                }
                
                return true;
            } catch (Exception e) {
                Log.e("RecipeAdapter", "Error processing menu action", e);
                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        
        popup.show();
    }
    
    /**
     * Opens recipe detail activity
     */
    private void openRecipeDetail(Recipe recipe) {
        try {
            // Convert the models.Recipe to model.Recipe for RecipeDetailActivity
            Log.d("RecipeAdapter", "Recipe clicked: " + recipe.getName());
            edu.prakriti.mealmate.model.Recipe detailRecipe = convertToDetailRecipe(recipe);
            
            // Validate recipe before sending to detail activity
            if (detailRecipe == null) {
                throw new IllegalStateException("Failed to convert recipe");
            }
            
            // Create intent and add recipe as extra
            Intent intent = new Intent(context, RecipeDetailActivity.class);
            intent.putExtra("RECIPE", detailRecipe);
            
            // Add additional info for recipe navigation
            int currentPosition = recipeList.indexOf(recipe);
            if (currentPosition >= 0) {
                intent.putExtra("CURRENT_POSITION", currentPosition);
                intent.putExtra("TOTAL_RECIPES", recipeList.size());
                
                // Pre-load next and previous recipe IDs if they exist
                if (currentPosition > 0) {
                    Recipe prevRecipe = recipeList.get(currentPosition - 1);
                    intent.putExtra("PREV_RECIPE_ID", prevRecipe.getId());
                }
                
                if (currentPosition < recipeList.size() - 1) {
                    Recipe nextRecipe = recipeList.get(currentPosition + 1);
                    intent.putExtra("NEXT_RECIPE_ID", nextRecipe.getId());
                }
            }
            
            // Add transition animation
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Log.e("RecipeAdapter", "Error opening recipe details", e);
            Toast.makeText(context, "Error opening recipe details: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Converts a models.Recipe to model.Recipe for use in RecipeDetailActivity
     */
    private edu.prakriti.mealmate.model.Recipe convertToDetailRecipe(Recipe recipe) {
        try {
            // Create a new model.Recipe object
            edu.prakriti.mealmate.model.Recipe detailRecipe = new edu.prakriti.mealmate.model.Recipe();
            
            // Set basic properties with null checks
            detailRecipe.setRecipeName(recipe.getName() != null ? recipe.getName() : "Unnamed Recipe");
            detailRecipe.setCookTime(recipe.getCookingTime() != null ? recipe.getCookingTime() : "0");
            detailRecipe.setPhotoUrl(recipe.getImageUrl() != null ? recipe.getImageUrl() : "");
            
            // Log any issues with image URL
            if (recipe.getImageUrl() == null) {
                Log.w("RecipeAdapter", "Recipe has null imageUrl: " + recipe.getName());
            }
            
            // Generate a timestamp if none exists
            detailRecipe.setTimestamp(System.currentTimeMillis());
            
            // Set recipe ID with null check
            detailRecipe.setRecipeId(recipe.getId() != null ? recipe.getId() : String.valueOf(System.currentTimeMillis()));
            
            // Set recipe ID with better handling
            if (recipe.getId() != null && !recipe.getId().isEmpty()) {
                Log.d("RecipeAdapter", "Setting recipeId: " + recipe.getId());
                detailRecipe.setRecipeId(recipe.getId());
            } else {
                String generatedId = String.valueOf(System.currentTimeMillis());
                Log.w("RecipeAdapter", "Recipe has no ID, generating: " + generatedId);
                detailRecipe.setRecipeId(generatedId);
                // Also update the original recipe ID to maintain consistency
                recipe.setId(generatedId);
            }
            
            // Log recipe ID for debugging
            Log.d("RecipeAdapter", "Recipe ID for Firestore: " + detailRecipe.getRecipeId());
            
            // Convert ingredients list to Map<String, List<String>>
            Map<String, List<String>> ingredientsMap = new HashMap<>();
            if (recipe.getIngredients() != null) {
                ingredientsMap.put("All", recipe.getIngredients());
            } else {
                ingredientsMap.put("All", new ArrayList<>());
            }
            detailRecipe.setIngredients(ingredientsMap);
            
            // Convert instructions to List<Map<String, Object>>
            List<Map<String, Object>> instructionsList = new ArrayList<>();
            if (recipe.getInstructions() != null) {
                for (int i = 0; i < recipe.getInstructions().size(); i++) {
                    String instruction = recipe.getInstructions().get(i);
                    if (instruction != null) {
                        Map<String, Object> stepMap = new HashMap<>();
                        stepMap.put("stepNumber", i + 1);
                        stepMap.put("instruction", instruction);
                        instructionsList.add(stepMap);
                    }
                }
            }
            detailRecipe.setInstructions(instructionsList);
            
            // Set favorite status (default to false)
            detailRecipe.setFavorite(false);
            
            // Log conversion success for debugging
            Log.d("RecipeAdapter", "Successfully converted recipe: " + recipe.getName());
            
            return detailRecipe;
        } catch (Exception e) {
            Log.e("RecipeAdapter", "Error converting recipe", e);
            return null;
        }
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }
    
    public void updateList(List<Recipe> newList) {
        this.recipeList = newList;
        notifyDataSetChanged();
    }

    /**
     * Share recipe with other apps
     */
    private void shareRecipe(edu.prakriti.mealmate.model.Recipe recipe) {
        // Create a sharable text content
        StringBuilder shareText = new StringBuilder();
        shareText.append("Check out this recipe from MealMate!\n\n");
        shareText.append("Recipe: ").append(recipe.getRecipeName()).append("\n");
        shareText.append("Cooking Time: ").append(recipe.getCookTime()).append(" minutes\n\n");
        
        // Create share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "MealMate Recipe: " + recipe.getRecipeName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        
        // Start share activity
        context.startActivity(Intent.createChooser(shareIntent, "Share Recipe Using"));
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView recipeImage;
        TextView recipeName;
        TextView cookTime;
        TextView ingredients;
        TextView steps;
        ImageButton menuButton;
        MaterialButton viewButton;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeImage = itemView.findViewById(R.id.recipeImage);
            recipeName = itemView.findViewById(R.id.recipeTitle);
            cookTime = itemView.findViewById(R.id.recipeCookTime);
            ingredients = itemView.findViewById(R.id.recipeIngredients);
            steps = itemView.findViewById(R.id.recipeSteps);
            menuButton = itemView.findViewById(R.id.recipe_menu_button);
            viewButton = itemView.findViewById(R.id.recipeButton);
        }
    }
} 