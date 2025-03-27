package edu.prakriti.mealmate.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.adapters.RecipeAdapter;
import edu.prakriti.mealmate.models.Recipe;
import edu.prakriti.mealmate.util.SpacingItemDecoration;

public class RecipeListFragment extends Fragment {

    private static final String TAG = "RecipeListFragment";
    
    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private List<Recipe> recipeList;
    private SearchView searchView;
    private ProgressBar loadingProgressBar;
    private TextView emptyView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Enable options menu for the search icon
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe_list, container, false);
        
        // Set up RecyclerView
        recyclerView = view.findViewById(R.id.recipe_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize loading indicator and empty view
        loadingProgressBar = view.findViewById(R.id.loading_progress);
        emptyView = view.findViewById(R.id.empty_view);
        
        if (loadingProgressBar == null) {
            // If progress bar doesn't exist in layout, log a message
            Log.w(TAG, "Progress bar not found in layout");
        }
        
        if (emptyView == null) {
            // If empty view doesn't exist in layout, log a message
            Log.w(TAG, "Empty view not found in layout");
        }
        
        // Initialize recipe list and adapter
        recipeList = new ArrayList<>();
        adapter = new RecipeAdapter(recipeList, getContext());
        recyclerView.setAdapter(adapter);
        
        // Add item decoration for spacing between cards
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.card_spacing);
        recyclerView.addItemDecoration(new SpacingItemDecoration(spacingInPixels));
        
        // Set up SearchView (if added to layout directly)
        searchView = view.findViewById(R.id.recipe_search_view);
        if (searchView != null) {
            setupSearchView();
        }
        
        // Load recipes from Firebase
        loadRecipesFromFirebase();
        
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.recipe_search_menu, menu);
        
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            try {
                searchView = (SearchView) searchItem.getActionView();
                if (searchView != null) {
                    setupSearchView();
                } else {
                    Log.w(TAG, "SearchView is null from action view");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting up search view: " + e.getMessage(), e);
            }
        } else {
            Log.w(TAG, "Search menu item not found");
        }
        
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    private void setupSearchView() {
        if (searchView == null) {
            Log.w(TAG, "SearchView is null, cannot set up query listener");
            return;
        }
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterRecipes(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterRecipes(newText);
                return true;
            }
        });
    }
    
    private void loadRecipesFromFirebase() {
        showLoading(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("recipes")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Order by latest timestamp first
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    recipeList.clear();
                    
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        try {
                            // Extract recipe data
                            String id = document.getId();
                            String name = document.getString("recipeName");
                            String cookingTime = document.getString("cookTime");
                            String photoUrl = document.getString("photoUrl");
                            
                            // Create Recipe object and add to list
                            Recipe recipe = new Recipe();
                            recipe.setId(id);
                            recipe.setName(name);
                            recipe.setCookingTime(cookingTime);
                            recipe.setImageUrl(photoUrl);
                            
                            // Add to list
                            recipeList.add(recipe);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing recipe: " + e.getMessage());
                        }
                    }
                    
                    // Update RecyclerView
                    adapter.updateList(recipeList);
                    showLoading(false);
                    
                    // Show empty view if no recipes
                    if (recipeList.isEmpty()) {
                        showEmptyView(true);
                    } else {
                        showEmptyView(false);
                    }
                    
                } else {
                    Log.e(TAG, "Error getting recipes: ", task.getException());
                    showLoading(false);
                    showError("Error loading recipes. Please try again.");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load recipes: " + e.getMessage());
                showLoading(false);
                showError("Failed to load recipes: " + e.getMessage());
            });
    }
    
    private void showLoading(boolean isLoading) {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }
    
    private void showEmptyView(boolean isEmpty) {
        if (emptyView != null) {
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            emptyView.setText("No recipes found. Add a recipe to get started!");
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }
    
    private void showError(String errorMessage) {
        if (getView() != null) {
            Snackbar.make(getView(), errorMessage, Snackbar.LENGTH_LONG).show();
        }
    }
    
    private void filterRecipes(String query) {
        List<Recipe> filteredList = new ArrayList<>();
        
        if (query == null || query.isEmpty()) {
            filteredList.addAll(recipeList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            
            for (Recipe recipe : recipeList) {
                if (recipe.getName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(recipe);
                }
            }
        }
        
        adapter.updateList(filteredList);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Reload recipes when returning to this fragment
        loadRecipesFromFirebase();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_add_recipe) {
            openAddNewRecipe();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Navigate to the recipe creation screen
     */
    private void openAddNewRecipe() {
        Intent intent = new Intent(getActivity(), edu.prakriti.mealmate.AddRecipeStepActivity.class);
        startActivity(intent);
    }
} 