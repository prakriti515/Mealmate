package edu.prakriti.mealmate.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.RecipeDetailActivity;
import edu.prakriti.mealmate.model.Recipe;
import edu.prakriti.mealmate.utils.FirestoreHelper;

public class FavoriteRecipesFragment extends Fragment {

    private static final String TAG = "FavoriteRecipesFragment";
    
    private RecyclerView recyclerView;
    private TextView emptyView;
    private List<Recipe> favoriteRecipes = new ArrayList<>();
    private FavoriteRecipeAdapter adapter;
    private FirestoreHelper firestoreHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite_recipes, container, false);
        
        Log.d(TAG, "onCreateView: Initializing FavoriteRecipesFragment");
        
        // Initialize views
        recyclerView = view.findViewById(R.id.rv_favorite_recipes);
        emptyView = view.findViewById(R.id.tv_empty_favorites);
        
        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d(TAG, "User is not logged in");
            showEmptyView("Please sign in to view your favorite recipes");
            Toast.makeText(getContext(), "You need to be signed in to view favorites", Toast.LENGTH_SHORT).show();
            return view;
        }
        
        Log.d(TAG, "User is logged in: " + FirebaseAuth.getInstance().getCurrentUser().getUid());
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
        adapter = new FavoriteRecipeAdapter(getContext(), favoriteRecipes);
        recyclerView.setAdapter(adapter);
        
        // Initialize FirestoreHelper
        firestoreHelper = new FirestoreHelper();
        
        // Load favorite recipes
        loadFavoriteRecipes();
        
        return view;
    }
    
    private void loadFavoriteRecipes() {
        Log.d(TAG, "loadFavoriteRecipes: Loading favorite recipes");
        firestoreHelper.loadFavoriteRecipes(recipes -> {
            Log.d(TAG, "loadFavoriteRecipes callback: Received " + recipes.size() + " favorite recipes");
            
            favoriteRecipes.clear();
            favoriteRecipes.addAll(recipes);
            
            if (favoriteRecipes.isEmpty()) {
                Log.d(TAG, "No favorite recipes found");
                showEmptyView("No favorite recipes yet");
            } else {
                Log.d(TAG, "Showing " + favoriteRecipes.size() + " favorite recipes");
                for (Recipe recipe : favoriteRecipes) {
                    Log.d(TAG, "Favorite recipe: " + recipe.getRecipeName() + " (ID: " + recipe.getRecipeId() + ")");
                }
                hideEmptyView();
                adapter.notifyDataSetChanged();
            }
        });
    }
    
    private void showEmptyView(String message) {
        Log.d(TAG, "showEmptyView: " + message);
        if (emptyView != null) {
            emptyView.setText(message);
            emptyView.setVisibility(View.VISIBLE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
    }
    
    private void hideEmptyView() {
        Log.d(TAG, "hideEmptyView: Showing recipe list");
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Reloading favorite recipes");
        // Reload favorites when returning to this fragment
        loadFavoriteRecipes();
    }
    
    /**
     * Custom adapter for favorite recipes that includes a remove from favorites option
     */
    private class FavoriteRecipeAdapter extends RecyclerView.Adapter<FavoriteRecipeAdapter.RecipeViewHolder> {

        private final Context context;
        private final List<Recipe> recipeList;

        public FavoriteRecipeAdapter(Context context, List<Recipe> recipeList) {
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
                        .placeholder(R.drawable.input_background)
                        .into(holder.recipeImage);
            } else {
                holder.recipeImage.setImageResource(R.drawable.no_image_placeholder);
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
                    showRecipeMenu(v, recipe, position);
                });
            }
            
            // Create a single click listener for better code organization
            View.OnClickListener recipeClickListener = v -> {
                openRecipeDetail(recipe);
            };
            
            // Make the entire card clickable
            holder.itemView.setOnClickListener(recipeClickListener);
            
            // Also set the button click listener
            if (holder.recipeBtn != null) {
                holder.recipeBtn.setOnClickListener(recipeClickListener);
            }
        }
        
        @Override
        public int getItemCount() {
            return recipeList.size();
        }

        /**
         * Opens recipe detail activity
         */
        private void openRecipeDetail(Recipe recipe) {
            Intent intent = new Intent(context, RecipeDetailActivity.class);
            
            // Pass the recipe ID to ensure it's properly fetched from Firebase
            intent.putExtra("RECIPE_ID", recipe.getRecipeId());
            
            // Also pass the Recipe object as a backup
            intent.putExtra("RECIPE", recipe);
            
            // Add transition animation
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        }
        
        /**
         * Shows a popup menu with recipe options including Remove from Favorites
         */
        private void showRecipeMenu(View view, Recipe recipe, int position) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.inflate(R.menu.recipe_menu);
            
            // Configure menu items
            Menu menu = popup.getMenu();
            menu.findItem(R.id.action_edit_recipe).setVisible(false);
            menu.findItem(R.id.action_del_recipe).setVisible(false);
            
            // Change the favorite option to "Remove from Favorites"
            menu.findItem(R.id.action_favorite).setTitle("Remove from Favorites");
            
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                
                // Handle menu item clicks
                if (itemId == R.id.action_favorite) {
                    // Remove from favorites
                    removeFromFavorites(recipe, position);
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
            startActivity(Intent.createChooser(shareIntent, "Share Recipe Using"));
        }
        
        /**
         * Removes a recipe from favorites
         */
        private void removeFromFavorites(Recipe recipe, int position) {
            if (recipe.getTimestamp() <= 0) {
                Toast.makeText(context, "Cannot remove: Invalid recipe data", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // First, remove from UI for immediate feedback
            recipeList.remove(position);
            notifyItemRemoved(position);
            
            // Then remove from Firebase
            boolean removed = firestoreHelper.removeFromFavorites(recipe.getTimestamp());
            
            if (removed) {
                Toast.makeText(context, "Recipe removed from favorites", Toast.LENGTH_SHORT).show();
                
                // If no recipes left, show empty view
                if (recipeList.isEmpty()) {
                    showEmptyView("No favorite recipes yet");
                }
            } else {
                Toast.makeText(context, "Failed to remove recipe from favorites", Toast.LENGTH_SHORT).show();
                
                // Reload the list in case of failure
                loadFavoriteRecipes();
            }
        }
        
        // ViewHolder class
        class RecipeViewHolder extends RecyclerView.ViewHolder {
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
} 