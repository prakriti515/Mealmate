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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.adapters.RecipeCardAdapter;
import edu.prakriti.mealmate.model.Recipe;
import edu.prakriti.mealmate.util.SpacingItemDecoration;
import edu.prakriti.mealmate.utils.FirestoreHelper;

public class RecipeListFragment extends Fragment {

    private static final String TAG = "RecipeListFragment";
    
    private RecyclerView recyclerView;
    private RecipeCardAdapter adapter;
    private List<Recipe> recipeList;
    private SearchView searchView;
    private ProgressBar loadingProgressBar;
    private TextView emptyView;
    private FirestoreHelper firestoreHelper;

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
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
        
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
        
        // Initialize recipe list and Firebase helper
        recipeList = new ArrayList<>();
        firestoreHelper = new FirestoreHelper();
        
        // Initialize adapter with empty list
        adapter = new RecipeCardAdapter(getContext(), recipeList);
        recyclerView.setAdapter(adapter);
        
        // Add item decoration for spacing between cards
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.card_spacing);
        recyclerView.addItemDecoration(new SpacingItemDecoration(spacingInPixels));
        
        // Set up SearchView (if added to layout directly)
        searchView = view.findViewById(R.id.recipe_search_view);
        if (searchView != null) {
            setupSearchView();
        }
        
        // Load recipes from Firebase using FirestoreHelper
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
        
        firestoreHelper.loadRecipes(new FirestoreHelper.FirestoreCallback() {
            @Override
            public void onCallback(List<Recipe> recipes) {
                recipeList.clear();
                recipeList.addAll(recipes);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Update adapter with new data
                        adapter.notifyDataSetChanged();
                        
                        // Show empty view if no recipes
                        if (recipeList.isEmpty()) {
                            showEmptyView(true);
                        } else {
                            showEmptyView(false);
                            
                            // Debug log for the recipes loaded
                            for (Recipe recipe : recipeList) {
                                Log.d(TAG, "Loaded recipe: " + recipe.getRecipeName() + 
                                        " (ID: " + recipe.getRecipeId() + ")");
                            }
                        }
                        
                        showLoading(false);
                    });
                }
            }
        });
    }
    
    private void showLoading(boolean isLoading) {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        
        if (isLoading && recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        } else if (recyclerView != null) {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void showEmptyView(boolean isEmpty) {
        if (emptyView != null) {
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
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
        if (query == null || query.isEmpty()) {
            // If query is empty, show all recipes
            if (adapter != null) {
                adapter = new RecipeCardAdapter(getContext(), recipeList);
                recyclerView.setAdapter(adapter);
            }
        } else {
            // Filter recipes by name
            List<Recipe> filteredList = new ArrayList<>();
            String lowercaseQuery = query.toLowerCase();
            
            for (Recipe recipe : recipeList) {
                if (recipe.getRecipeName().toLowerCase().contains(lowercaseQuery)) {
                    filteredList.add(recipe);
                }
            }
            
            // Update adapter with filtered list
            adapter = new RecipeCardAdapter(getContext(), filteredList);
            recyclerView.setAdapter(adapter);
        }
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