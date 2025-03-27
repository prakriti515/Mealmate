package edu.prakriti.mealmate.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.adapters.RecipeCardAdapter;
import edu.prakriti.mealmate.model.Recipe;
import edu.prakriti.mealmate.utils.FirestoreHelper;

public class FavoriteRecipesFragment extends Fragment {

    private static final String TAG = "FavoriteRecipesFragment";
    
    private RecyclerView recyclerView;
    private TextView emptyView;
    private List<Recipe> favoriteRecipes = new ArrayList<>();
    private RecipeCardAdapter adapter;
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
        adapter = new RecipeCardAdapter(getContext(), favoriteRecipes);
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
} 