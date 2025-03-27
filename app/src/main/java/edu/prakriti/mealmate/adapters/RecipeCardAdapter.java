package edu.prakriti.mealmate.adapters;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.RecipeDetailActivity;
import edu.prakriti.mealmate.home.DashboardActivity;
import edu.prakriti.mealmate.model.Recipe;

public class RecipeCardAdapter extends RecyclerView.Adapter<RecipeCardAdapter.RecipeViewHolder> {

    private final Context context;
    private final List<Recipe> recipeList;

    public RecipeCardAdapter(Context context, List<Recipe> recipeList) {
        this.context = context;
        this.recipeList = recipeList;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recipe_card, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);

        // Load recipe image
        if (recipe.getPhotoUrl() != null && !recipe.getPhotoUrl().isEmpty()) {
            Glide.with(context)
                    .load(recipe.getPhotoUrl())
                    .placeholder(R.drawable.input_background) // Fallback image
                    // Error image
                    .into(holder.recipeImage);
        } else {
            holder.recipeImage.setImageResource(R.drawable.no_image_placeholder); // Default image
        }

        // Set recipe name
        holder.recipeName.setText(recipe.getRecipeName());

        // Calculate and set total number of ingredients
        int totalIngredients = 0;
        for (List<String> ingredients : recipe.getIngredients().values()) {
            totalIngredients += ingredients.size();
        }
        holder.totalIngredients.setText(totalIngredients + " Ingredients");

        // Set total number of instructions
        int totalInstructions = recipe.getInstructions().size();
        holder.totalInstructions.setText(totalInstructions + " Steps");
        holder.cookTime.setText(recipe.getCookTime() + " Minutes");
        
        // Set menu button click listener
        if (holder.menuButton != null) {
            holder.menuButton.setOnClickListener(v -> {
                showRecipeMenu(v, recipe);
            });
        }
        
        // Create a single click listener for better code organization
        View.OnClickListener recipeClickListener = v -> {
            openRecipeDetail(recipe);
        };
        
        // Make the entire card clickable, not just the button
        holder.itemView.setOnClickListener(recipeClickListener);
        
        // Also set the button click listener for consistency
        if (holder.recipeBtn != null) {
            holder.recipeBtn.setOnClickListener(recipeClickListener);
        }
    }
    
    /**
     * Opens recipe detail activity
     */
    private void openRecipeDetail(Recipe recipe) {
        Intent intent = new Intent(context, RecipeDetailActivity.class);
        intent.putExtra("RECIPE", recipe);
        
        // Add transition animation
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            context.startActivity(intent);
        }
    }
    
    /**
     * Shows a popup menu with recipe options
     */
    private void showRecipeMenu(View view, Recipe recipe) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.inflate(R.menu.recipe_menu);
        
        // Hide edit and delete options in the popup menu if needed
        Menu menu = popup.getMenu();
        menu.findItem(R.id.action_edit_recipe).setVisible(false);
        menu.findItem(R.id.action_del_recipe).setVisible(false);
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            
            // Handle menu item clicks
            if (itemId == R.id.action_favorite) {
                toggleFavoriteStatus(recipe);
                return true;
            } else if (itemId == R.id.action_share) {
                shareRecipe(recipe);
                return true;

            } else {
                // Open recipe detail for any other action
                openRecipeDetail(recipe);
                return true;
            }
        });
        
        popup.show();
    }
    
    /**
     * Toggles favorite status for a recipe
     */
    private void toggleFavoriteStatus(Recipe recipe) {
        // Navigate to detail activity with favorite action
        Intent intent = new Intent(context, RecipeDetailActivity.class);
        intent.putExtra("RECIPE", recipe);
        intent.putExtra("ACTION", "TOGGLE_FAVORITE");
        context.startActivity(intent);
    }
    
    /**
     * Share recipe with other apps
     */
    private void shareRecipe(Recipe recipe) {
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
    
    /**
     * Adds recipe to meal plan
     */
    private void addToMealPlan(Recipe recipe) {
        // Intent to navigate to DashboardActivity and open MealPlanFragment
        Intent intent = new Intent(context, DashboardActivity.class);
        intent.putExtra("FRAGMENT_INDEX", 1); // Index for MealPlanFragment
        intent.putExtra("RECIPE_TO_ADD", recipe); // Pass recipe to add to meal plan
        context.startActivity(intent);
        
        Toast.makeText(context, "Please select a day and meal to add the recipe.", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Adds ingredients to grocery list
     */
    private void addIngredientsToGroceryList(Recipe recipe) {
        // Navigate to detail activity with grocery action
        Intent intent = new Intent(context, RecipeDetailActivity.class);
        intent.putExtra("RECIPE", recipe);
        intent.putExtra("ACTION", "ADD_TO_GROCERY");
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView recipeImage;
        TextView recipeName, totalIngredients, totalInstructions, cookTime;
        Button recipeBtn;
        ImageButton menuButton;
        
        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeImage = itemView.findViewById(R.id.recipeImage);
            recipeName = itemView.findViewById(R.id.recipeTitle);
            totalIngredients = itemView.findViewById(R.id.recipeIngredients);
            totalInstructions = itemView.findViewById(R.id.recipeSteps);
            cookTime = itemView.findViewById(R.id.recipeCookTime);
            recipeBtn = itemView.findViewById(R.id.recipeButton);
            menuButton = itemView.findViewById(R.id.recipe_menu_button);
        }
    }
}