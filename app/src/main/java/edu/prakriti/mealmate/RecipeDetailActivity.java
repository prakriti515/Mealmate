package edu.prakriti.mealmate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.DataSource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONException;

import edu.prakriti.mealmate.adapters.InstructionDetailAdapter;

import edu.prakriti.mealmate.home.DashboardActivity;
import edu.prakriti.mealmate.home.MainActivity;
import edu.prakriti.mealmate.model.Recipe;

import android.view.GestureDetector;

import androidx.annotation.NonNull;

public class RecipeDetailActivity extends AppCompatActivity {

    private ViewPager2 instructionsViewPager;
    private MaterialButton prevButton, nextButton;
    private InstructionDetailAdapter instructionsAdapter;
    private TextView stepIndicator;
    private com.google.android.material.progressindicator.LinearProgressIndicator progressIndicator;

    private ImageView recipeImage;
    private TextView recipeNameTv, cookTime, totalIngredients;
    private LinearLayout ingredientsContainer;
    private Recipe recipe;
    private CustomProgressDialog customProgressDialog;
    private FirebaseFirestore db;
    private String userId;
    private boolean isFavorite = false;
    private MaterialToolbar toolbar;

    private int currentPosition = -1;
    private int totalRecipes = 0;
    private String nextRecipeId;
    private String prevRecipeId;

    private TextView prevRecipeIndicator;
    private TextView nextRecipeIndicator;
    
    // Image selection variables
    private Uri cameraImageUri;
    private Uri selectedImageUri = null;
    private static final String IMGUR_CLIENT_ID = "6deebb69c6310e5";
    private static final String IMGUR_UPLOAD_URL = "https://api.imgur.com/3/image";
    private com.google.android.material.floatingactionbutton.FloatingActionButton uploadImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);
        
        // Debug log for activity creation
        Log.d("RecipeDetailActivity", "onCreate started");
        
        // Check for notification permission on Android 13+
        checkNotificationPermission();
        
        // Log all intent extras for debugging
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                Log.d("RecipeDetailActivity", "Intent extra: " + key + " = " + (value != null ? value.toString() : "null"));
            }
        } else {
            Log.d("RecipeDetailActivity", "No intent extras found");
        }

        // Initialize Firestore and userId
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        Log.d("RecipeDetailActivity", "User ID: " + (userId != null ? userId : "not logged in"));

        // Setup toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
        // Initialize progress dialog early to avoid null pointer
        customProgressDialog = new CustomProgressDialog(RecipeDetailActivity.this);
        
        // Get recipe navigation data
        currentPosition = getIntent().getIntExtra("CURRENT_POSITION", -1);
        totalRecipes = getIntent().getIntExtra("TOTAL_RECIPES", 0);
        nextRecipeId = getIntent().getStringExtra("NEXT_RECIPE_ID");
        prevRecipeId = getIntent().getStringExtra("PREV_RECIPE_ID");
        
        // Setup navigation UI if we have position data
        setupRecipeNavigation();
        
        // Get recipe data - either from Parcelable or by ID from Firestore
        recipe = getIntent().getParcelableExtra("RECIPE");
        String recipeId = getIntent().getStringExtra("RECIPE_ID");
        
        Log.d("RecipeDetailActivity", "Intent data - Recipe: " + (recipe != null ? "present" : "null") 
                + ", RecipeID: " + (recipeId != null ? recipeId : "null"));
        
        // Additional debug logging to track the source
        if (getIntent().hasExtra("RECIPE") && recipe != null) {
            Log.d("RecipeDetailActivity", "Recipe from intent: " + recipe.getRecipeName() + ", ID: " + recipe.getRecipeId());
        }
        
        // Always prioritize loading from Firestore if we have a recipe ID
        if (recipeId != null) {
            Log.d("RecipeDetailActivity", "Starting to fetch recipe from Firebase with ID: " + recipeId);
            // Fetch fresh data from Firestore by ID
            fetchRecipeFromFirestore(recipeId);
        } else if (recipe != null) {
            // Recipe provided as Parcelable and no ID available
            Log.d("RecipeDetailActivity", "Loading recipe from Parcelable: " + recipe.getRecipeName());
            
            // Detailed log of the Recipe object
            Log.d("RecipeDetailActivity", "Recipe details: " +
                    "ID=" + recipe.getRecipeId() + 
                    ", Name=" + recipe.getRecipeName() + 
                    ", CookTime=" + recipe.getCookTime() + 
                    ", PhotoUrl=" + (recipe.getPhotoUrl() != null ? "exists" : "null") +
                    ", Ingredients count=" + (recipe.getIngredients() != null ? recipe.getIngredients().size() : 0) +
                    ", Instructions count=" + (recipe.getInstructions() != null ? recipe.getInstructions().size() : 0));
            
            // Initialize UI with provided recipe
            initializeRecipeUI();
        } else {
            // No recipe data available
            Log.e("RecipeDetailActivity", "No recipe data or ID provided");
            showSnackbar("Error: No recipe data or ID provided");
            finish();
        }

        // Handle any actions passed to this activity
        String action = getIntent().getStringExtra("ACTION");
        if (action != null && recipe != null) {
            // Handle the action after view is fully initialized
            toolbar.post(() -> {
                Log.d("RecipeDetailActivity", "Handling action: " + action);
                if (action.equals("EDIT_RECIPE")) {
                    openEditRecipe();
                } else if (action.equals("DELETE_RECIPE")) {
                    deleteRecipe();
                } else if (action.equals("SHARE_RECIPE")) {
                    shareRecipe();
                } else if (action.equals("TOGGLE_FAVORITE")) {
                    toggleFavoriteStatus();
                }
            });
        }
        
        // Setup the upload image button
        uploadImageButton = findViewById(R.id.uploadImageButton);
        if (uploadImageButton != null) {
            uploadImageButton.setOnClickListener(v -> {
                if (recipe != null) {
                    showImagePickerDialog();
                } else {
                    showSnackbar("Please wait for recipe to load");
                }
            });
        }
    }
    
    /**
     * Shows dialog to pick image from gallery or camera
     */
    private void showImagePickerDialog() {
        Intent pickGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickGalleryIntent.setType("image/*");

        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

        Intent chooser = Intent.createChooser(pickGalleryIntent, "Select or Capture Image");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});
        imagePickerLauncher.launch(chooser);
    }

    /**
     * ActivityResultLauncher for handling image selection result
     */
    private final androidx.activity.result.ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getData() != null) { // Picked from Gallery
                        selectedImageUri = data.getData();
                    } else { // Captured from Camera
                        selectedImageUri = cameraImageUri;
                    }

                    if (selectedImageUri != null) {
                        try {
                            // Show the selected image immediately
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            recipeImage.setImageBitmap(bitmap);
                            
                            // Upload the image and update the recipe
                            customProgressDialog.show();
                            uploadImageToImgur(selectedImageUri);
                        } catch (IOException e) {
                            Log.e("RecipeDetailActivity", "Error loading selected image", e);
                            showSnackbar("Error loading image");
                        }
                    }
                }
            });

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit_recipe) {
            openEditRecipe();
            return true;
        } else if (item.getItemId() == R.id.action_del_recipe) {
            deleteRecipe();
            return true;
        } else if (item.getItemId() == R.id.action_share) {
            shareRecipe();
            return true;
        } else if (item.getItemId() == R.id.action_favorite) {
            toggleFavoriteStatus();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.recipe_menu, menu);
        
        // Check if recipe is favorite after menu is inflated
        if (userId != null && recipe != null) {
            toolbar.post(() -> checkIfFavorite());
        }
        
        return true;
    }

    void openEditRecipe() {
        if (recipe != null) {
            Intent intent = new Intent(RecipeDetailActivity.this, edu.prakriti.mealmate.home.RecipeEditActivity.class);
            intent.putExtra("RECIPE", recipe);
            
            // Add transition animation
            android.app.ActivityOptions options = android.app.ActivityOptions
                .makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left);
            startActivity(intent, options.toBundle());
        } else {
            showSnackbar("Error: Recipe data is missing!");
        }
    }

    void deleteRecipe() {
        new MaterialAlertDialogBuilder(RecipeDetailActivity.this)
                .setTitle("Delete")
                .setMessage("Are you sure you want to delete this recipe?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Show progress dialog before starting Firestore operations
                    customProgressDialog.show();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    
                    // Directly delete recipe without checking meal plans
                    deleteRecipeFromFirestore(db);
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Helper method to delete the recipe from Firestore
    private void deleteRecipeFromFirestore(FirebaseFirestore db) {
        // Check if we have a valid recipeId first
        String recipeId = recipe.getRecipeId();
        
        if (recipeId != null && !recipeId.isEmpty()) {
            // Use direct document reference with the recipe ID for more reliable deletion
            Log.d("RecipeDetailActivity", "Attempting to delete recipe with ID: " + recipeId);
            
            db.collection("recipes")
                    .document(recipeId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("RecipeDetailActivity", "Recipe deleted successfully");
                        
                        // Also remove from favorites if it exists
                        if (userId != null) {
                            removeFromFavorites(db, recipeId);
                        } else {
                            showSnackbar("Recipe deleted successfully!");
                            customProgressDialog.dismiss();
                            navigateToDashboard();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("RecipeDetailActivity", "Error deleting recipe by ID", e);
                        
                        // Fallback to delete by timestamp if ID-based deletion fails
                        deleteRecipeByTimestamp(db);
                    });
        } else {
            // Fallback to timestamp-based deletion if no recipeId is available
            Log.w("RecipeDetailActivity", "No recipe ID available, falling back to timestamp deletion");
            deleteRecipeByTimestamp(db);
        }
    }
    
    // Fallback method to delete recipe by timestamp
    private void deleteRecipeByTimestamp(FirebaseFirestore db) {
        if (recipe.getTimestamp() <= 0) {
            Log.e("RecipeDetailActivity", "No valid timestamp for deletion");
            showSnackbar("Error: Cannot delete this recipe (invalid data)");
            customProgressDialog.dismiss();
            return;
        }
        
        Log.d("RecipeDetailActivity", "Attempting to delete recipe by timestamp: " + recipe.getTimestamp());
        
        db.collection("recipes")
                .whereEqualTo("timestamp", recipe.getTimestamp())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        Log.d("RecipeDetailActivity", "Found " + task.getResult().size() + " matching documents");
                        final boolean[] anySuccess = {false};
                        final int totalDeletions = task.getResult().size();
                        final int[] completedDeletions = {0};
                        
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            final String docId = document.getId();
                            Log.d("RecipeDetailActivity", "Deleting document with ID: " + docId);
                            
                            db.collection("recipes").document(docId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        anySuccess[0] = true;
                                        completedDeletions[0]++;
                                        Log.d("RecipeDetailActivity", "Successfully deleted document " + docId);
                                        
                                        if (completedDeletions[0] >= totalDeletions) {
                                            if (userId != null && recipe.getRecipeId() != null) {
                                                removeFromFavorites(db, recipe.getRecipeId());
                                            } else {
                                                showSnackbar("Recipe deleted successfully!");
                                                customProgressDialog.dismiss();
                                                navigateToDashboard();
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("RecipeDetailActivity", "Failed to delete document " + docId, e);
                                        completedDeletions[0]++;
                                        
                                        if (completedDeletions[0] >= totalDeletions) {
                                            if (anySuccess[0]) {
                                                showSnackbar("Recipe partially deleted.");
                                            } else {
                                                showSnackbar("Error deleting recipe: " + e.getMessage());
                                            }
                                            customProgressDialog.dismiss();
                                        }
                                    });
                        }
                    } else {
                        Log.w("RecipeDetailActivity", "No documents found matching timestamp " + recipe.getTimestamp());
                        showSnackbar("Recipe not found in database.");
                        customProgressDialog.dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RecipeDetailActivity", "Error querying for recipe", e);
                    showSnackbar("Error searching for recipe: " + e.getMessage());
                    customProgressDialog.dismiss();
                });
    }
    
    // Helper method to remove recipe from favorites collection
    private void removeFromFavorites(FirebaseFirestore db, String recipeId) {
        if (userId == null || recipeId == null || recipeId.isEmpty()) {
            Log.d("RecipeDetailActivity", "Cannot remove from favorites - missing userId or recipeId");
            showSnackbar("Recipe deleted successfully!");
            customProgressDialog.dismiss();
            navigateToDashboard();
            return;
        }
        
        Log.d("RecipeDetailActivity", "Removing recipe from favorites: " + recipeId);
        
        db.collection("user_favorites")
                .document(userId)
                .collection("recipes")
                .document(recipeId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("RecipeDetailActivity", "Successfully removed from favorites");
                    showSnackbar("Recipe deleted successfully!");
                    customProgressDialog.dismiss();
                    navigateToDashboard();
                })
                .addOnFailureListener(e -> {
                    Log.w("RecipeDetailActivity", "Failed to remove from favorites", e);
                    showSnackbar("Recipe deleted but couldn't remove from favorites.");
                    customProgressDialog.dismiss();
                    navigateToDashboard();
                });
    }

    // 🔥 Helper method to navigate to the Dashboard after deletion
    private void navigateToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("FRAGMENT_INDEX", 0);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        
        // Add transition animation
        android.app.ActivityOptions options = android.app.ActivityOptions
            .makeCustomAnimation(this, R.anim.slide_in_left, R.anim.slide_out_right);
        startActivity(intent, options.toBundle());
        finish();
    }

    private void loadIngredients(Map<String, List<String>> ingredients) {
        Log.d("RecipeDetailActivity", "loadIngredients called with " + 
                (ingredients != null ? ingredients.size() : 0) + " categories");
                
        if (ingredients == null || ingredients.isEmpty()) {
            Log.w("RecipeDetailActivity", "No ingredients to display");
            // Display "No ingredients" message
            TextView noIngredientsMessage = new TextView(this);
            noIngredientsMessage.setText("No ingredients available for this recipe.");
            noIngredientsMessage.setTextSize(16);
            noIngredientsMessage.setPadding(16, 16, 16, 16);
            noIngredientsMessage.setTextColor(getResources().getColor(R.color.on_surface));
            ingredientsContainer.addView(noIngredientsMessage);
            return;
        }
        
        ingredientsContainer.removeAllViews(); // Clear any previous data
        
        // Set animation for loading ingredients
        int animationDelay = 50; // milliseconds between each ingredient animation
        
        // Counter for animation delay
        final int[] counter = {0};

        for (Map.Entry<String, List<String>> entry : ingredients.entrySet()) {
            String category = entry.getKey(); // Example: "Vegetables"
            List<String> ingredientList = entry.getValue(); // Example: ["Spinach", "Carrots"]
            
            Log.d("RecipeDetailActivity", "Processing category: " + category + 
                    " with " + (ingredientList != null ? ingredientList.size() : 0) + " ingredients");

            if (ingredientList == null || ingredientList.isEmpty()) {
                Log.w("RecipeDetailActivity", "Empty ingredient list for category: " + category);
                continue;
            }

            // Create Category Header with better styling
            TextView categoryTitle = new TextView(this);
            categoryTitle.setText(category);
            categoryTitle.setTextSize(18);
            categoryTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            categoryTitle.setTextColor(getResources().getColor(R.color.on_surface));
            categoryTitle.setPadding(0, 24, 0, 12);
            
            // Apply fade-in animation
            categoryTitle.setAlpha(0f);
            categoryTitle.animate().alpha(1f).setDuration(300).setStartDelay(counter[0] * animationDelay);
            counter[0]++;
            
            ingredientsContainer.addView(categoryTitle);

            // Create a ChipGroup with horizontal flow layout
            ChipGroup chipGroup = new ChipGroup(this);
            chipGroup.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            chipGroup.setChipSpacingHorizontal(8);
            chipGroup.setChipSpacingVertical(8);

            // Add Chips for Each Ingredient with better styling
            for (String ingredient : ingredientList) {
                Log.d("RecipeDetailActivity", "Adding ingredient chip: " + ingredient);
                Chip chip = new Chip(this);
                chip.setText(ingredient);
                chip.setChipBackgroundColorResource(R.color.on_surface_variant);
                chip.setTextColor(getResources().getColor(R.color.white));
                chip.setChipCornerRadius(16f);
                chip.setElevation(2f);
                
                // Apply fade-in and slide-in animation
                chip.setAlpha(0f);
                chip.setTranslationY(20f);
                chip.animate().alpha(1f).translationY(0f).setDuration(300).setStartDelay(counter[0] * animationDelay);
                counter[0]++;
                
                chipGroup.addView(chip);
            }

            // Add ChipGroup to the Ingredients Container
            ingredientsContainer.addView(chipGroup);
        }
        
        Log.d("RecipeDetailActivity", "Finished loading ingredients with " + counter[0] + " total UI components");
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.primary_variant))
                .setTextColor(getResources().getColor(R.color.white))
                .show();
    }

    /**
     * Share recipe details with other apps
     */
    private void shareRecipe() {
        if (recipe == null) {
            showSnackbar("Recipe not available for sharing.");
            return;
        }
        
        try {
            // Build shareable text content
            StringBuilder shareText = new StringBuilder();
            shareText.append("Check out this recipe from MealMate!\n\n");
            shareText.append("Recipe: ").append(recipe.getRecipeName()).append("\n");
            shareText.append("Cooking Time: ").append(recipe.getCookTime()).append(" minutes\n\n");
            
            // Add ingredients
            shareText.append("INGREDIENTS:\n");
            if (recipe.getIngredients() != null && !recipe.getIngredients().isEmpty()) {
                for (Map.Entry<String, List<String>> entry : recipe.getIngredients().entrySet()) {
                    if (!"All".equals(entry.getKey())) {
                        shareText.append(entry.getKey()).append(":\n");
                    }
                    
                    for (String ingredient : entry.getValue()) {
                        shareText.append("• ").append(ingredient).append("\n");
                    }
                    shareText.append("\n");
                }
            } else {
                shareText.append("No ingredients listed.\n\n");
            }
            
            // Add instructions
            shareText.append("INSTRUCTIONS:\n");
            if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
                int stepNumber = 1;
                for (Map<String, Object> step : recipe.getInstructions()) {
                    Object instruction = step.get("instruction");
                    if (instruction != null) {
                        shareText.append("Step ").append(stepNumber++).append(": ");
                        shareText.append(instruction.toString()).append("\n");
                    }
                }
            } else {
                shareText.append("No instructions listed.\n");
            }
            
            // Create and start share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "MealMate Recipe: " + recipe.getRecipeName());
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
            
            startActivity(Intent.createChooser(shareIntent, "Share Recipe Using"));
            
        } catch (Exception e) {
            Log.e("RecipeDetailActivity", "Error sharing recipe", e);
            showSnackbar("Error sharing recipe: " + e.getMessage());
        }
    }

    // Add onBackPressed method to handle back button animation
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Apply a custom animation when going back
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // Setup swipe gestures for better navigation
    private void setupSwipeGestures() {
        View rootView = findViewById(android.R.id.content);
        rootView.setOnTouchListener(new OnSwipeTouchListener(this));
    }
    
    // Inner class for handling swipe gestures
    private class OnSwipeTouchListener implements View.OnTouchListener {
        private final GestureDetector gestureDetector;
        
        public OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }
        
        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;
            
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
            
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                try {
                    if (e1 == null || e2 == null) return false;
                    
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    
                    if (Math.abs(diffX) > Math.abs(diffY) && 
                        Math.abs(diffX) > SWIPE_THRESHOLD && 
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        
                        if (diffX > 0) {
                            // Right swipe
                            onSwipeRight();
                        } else {
                            // Left swipe
                            onSwipeLeft();
                        }
                        return true;
                    }
                } catch (Exception exception) {
                    Log.e("RecipeDetailActivity", "Error in swipe gesture", exception);
                }
                return false;
            }
        }
        
        public void onSwipeRight() {
            // Go to previous instruction
            if (instructionsViewPager != null && instructionsViewPager.getCurrentItem() > 0) {
                instructionsViewPager.setCurrentItem(instructionsViewPager.getCurrentItem() - 1);
            }
        }
        
        public void onSwipeLeft() {
            // Go to next instruction
            if (instructionsViewPager != null && 
                instructionsViewPager.getCurrentItem() < instructionsAdapter.getItemCount() - 1) {
                instructionsViewPager.setCurrentItem(instructionsViewPager.getCurrentItem() + 1);
            }
        }
    }

    private void setupRecipeNavigation() {
        // Only add navigation UI if we have recipe position information
        if (currentPosition >= 0 && totalRecipes > 1) {
            View rootView = findViewById(android.R.id.content);
            
            // Update swipe gesture to handle recipe navigation
            rootView.setOnTouchListener(new RecipeNavigationTouchListener(this));
            
            // Add subtitle to show position in list
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle("Recipe " + (currentPosition + 1) + " of " + totalRecipes);
            }
            
            // Setup navigation indicators
            prevRecipeIndicator = findViewById(R.id.prev_recipe_indicator);
            nextRecipeIndicator = findViewById(R.id.next_recipe_indicator);
            
            if (prevRecipeIndicator != null && nextRecipeIndicator != null) {
                // Show indicators based on position
                prevRecipeIndicator.setVisibility(prevRecipeId != null ? View.VISIBLE : View.GONE);
                nextRecipeIndicator.setVisibility(nextRecipeId != null ? View.VISIBLE : View.GONE);
                
                // Set click listeners
                prevRecipeIndicator.setOnClickListener(v -> navigateToPreviousRecipe());
                nextRecipeIndicator.setOnClickListener(v -> navigateToNextRecipe());
            }
        }
    }
    
    // Inner class for handling recipe navigation swipes
    private class RecipeNavigationTouchListener implements View.OnTouchListener {
        private final GestureDetector gestureDetector;
        
        public RecipeNavigationTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new RecipeNavigationGestureListener());
        }
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }
        
        private final class RecipeNavigationGestureListener extends GestureDetector.SimpleOnGestureListener {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;
            
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
            
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                try {
                    if (e1 == null || e2 == null) return false;
                    
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    
                    if (Math.abs(diffX) > Math.abs(diffY) && 
                        Math.abs(diffX) > SWIPE_THRESHOLD && 
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        
                        if (diffX > 0) {
                            // Right swipe - go to previous recipe
                            if (prevRecipeId != null) {
                                navigateToPreviousRecipe();
                                return true;
                            }
                        } else {
                            // Left swipe - go to next recipe
                            if (nextRecipeId != null) {
                                navigateToNextRecipe();
                                return true;
                            }
                        }
                    }
                } catch (Exception exception) {
                    Log.e("RecipeDetailActivity", "Error in navigation swipe gesture", exception);
                }
                return false;
            }
        }
    }
    
    private void navigateToNextRecipe() {
        if (nextRecipeId == null) return;
        
        // Show loading indicator
        customProgressDialog.show();
        
        // Fetch next recipe from Firestore
        db.collection("recipes")
            .document(nextRecipeId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                try {
                    if (documentSnapshot.exists()) {
                        // Convert document to Recipe model
                        edu.prakriti.mealmate.model.Recipe nextRecipe = documentSnapshot.toObject(edu.prakriti.mealmate.model.Recipe.class);
                        
                        if (nextRecipe != null) {
                            // Update current recipe and UI
                            recipe = nextRecipe;
                            
                            // Update navigation variables
                            currentPosition++;
                            updateNavigationData();
                            
                            // Refresh UI
                            refreshRecipeUI();
                            
                            // Apply slide animation
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        }
                    } else {
                        showSnackbar("Recipe not found");
                    }
                } catch (Exception e) {
                    Log.e("RecipeDetailActivity", "Error loading next recipe", e);
                    showSnackbar("Error: " + e.getMessage());
                } finally {
                    customProgressDialog.dismiss();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("RecipeDetailActivity", "Error fetching next recipe", e);
                showSnackbar("Error: " + e.getMessage());
                customProgressDialog.dismiss();
            });
    }
    
    private void navigateToPreviousRecipe() {
        if (prevRecipeId == null) return;
        
        // Show loading indicator
        customProgressDialog.show();
        
        // Fetch previous recipe from Firestore
        db.collection("recipes")
            .document(prevRecipeId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                try {
                    if (documentSnapshot.exists()) {
                        // Convert document to Recipe model
                        edu.prakriti.mealmate.model.Recipe prevRecipe = documentSnapshot.toObject(edu.prakriti.mealmate.model.Recipe.class);
                        
                        if (prevRecipe != null) {
                            // Update current recipe and UI
                            recipe = prevRecipe;
                            
                            // Update navigation variables
                            currentPosition--;
                            updateNavigationData();
                            
                            // Refresh UI
                            refreshRecipeUI();
                            
                            // Apply slide animation
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                        }
                    } else {
                        showSnackbar("Recipe not found");
                    }
                } catch (Exception e) {
                    Log.e("RecipeDetailActivity", "Error loading previous recipe", e);
                    showSnackbar("Error: " + e.getMessage());
                } finally {
                    customProgressDialog.dismiss();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("RecipeDetailActivity", "Error fetching previous recipe", e);
                showSnackbar("Error: " + e.getMessage());
                customProgressDialog.dismiss();
            });
    }
    
    private void updateNavigationData() {
        // Update subtitle
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle("Recipe " + (currentPosition + 1) + " of " + totalRecipes);
        }
        
        // We would need to fetch next/prev IDs from server or adapter, but for now just disable navigation
        // at the ends of the list
        prevRecipeId = (currentPosition > 0) ? prevRecipeId : null;
        nextRecipeId = (currentPosition < totalRecipes - 1) ? nextRecipeId : null;
        
        // Update navigation indicators
        if (prevRecipeIndicator != null && nextRecipeIndicator != null) {
            prevRecipeIndicator.setVisibility(prevRecipeId != null ? View.VISIBLE : View.GONE);
            nextRecipeIndicator.setVisibility(nextRecipeId != null ? View.VISIBLE : View.GONE);
        }
    }
    
    private void refreshRecipeUI() {
        if (recipe != null) {
            // Set the activity title to the recipe name
            getSupportActionBar().setTitle(recipe.getRecipeName());
            
            // Clear and reload ingredients
            loadIngredients(recipe.getIngredients());
            
            // Refresh instructions view
            if (recipe.getInstructions() == null || recipe.getInstructions().isEmpty()) {
                // Handle empty instructions case
                if (instructionsViewPager != null && instructionsViewPager.getParent() != null) {
                    ViewGroup container = (ViewGroup) instructionsViewPager.getParent();
                    int index = container.indexOfChild(instructionsViewPager);
                    
                    // Remove existing view
                    container.removeViewAt(index);
                    
                    // Add empty message
                    TextView noInstructionsMessage = new TextView(this);
                    noInstructionsMessage.setText("No instructions available for this recipe.");
                    noInstructionsMessage.setTextSize(16);
                    noInstructionsMessage.setPadding(16, 16, 16, 16);
                    noInstructionsMessage.setTextColor(getResources().getColor(R.color.on_surface));
                    container.addView(noInstructionsMessage, index);
                    
                    // Hide navigation buttons
                    hideStepUIComponents();
                }
            } else {
                // Update instructions adapter
                if (instructionsAdapter != null) {
                    instructionsAdapter = new InstructionDetailAdapter(this, recipe.getInstructions());
                    instructionsViewPager.setAdapter(instructionsAdapter);
                    
                    // Show navigation buttons
                    if (prevButton != null) prevButton.setVisibility(View.VISIBLE);
                    if (nextButton != null) nextButton.setVisibility(View.VISIBLE);
                    
                    // Initialize step indicator and progress
                    if (stepIndicator != null && progressIndicator != null) {
                        // Reset to the first step
                        instructionsViewPager.setCurrentItem(0);
                        updateStepProgress(0, instructionsAdapter.getItemCount());
                    }

                    // Show all step UI components and initialize them
                    showStepUIComponents(instructionsAdapter.getItemCount());
                }
            }
            
            // Load recipe image using Glide with improved error handling
            String imageUrl = recipe.getPhotoUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Log.d("RecipeDetailActivity", "Original recipe image URL: " + imageUrl);
                
                // Clean up the URL if needed
                imageUrl = imageUrl.trim();
                if (imageUrl.contains(" ")) {
                    imageUrl = imageUrl.replace(" ", "%20");
                    Log.d("RecipeDetailActivity", "URL contained spaces, cleaned to: " + imageUrl);
                }
                
                // Check for imgur links that might be missing the full URL
                if (imageUrl.startsWith("i.imgur.com/")) {
                    Log.d("RecipeDetailActivity", "Fixing imgur URL format");
                    imageUrl = "https://" + imageUrl;
                }
                
                // Handle edge cases like missing http/https prefix
                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && 
                    !imageUrl.startsWith("file://") && !imageUrl.startsWith("content://")) {
                    Log.w("RecipeDetailActivity", "Image URL missing protocol, adding https://");
                    imageUrl = "https://" + imageUrl;
                }
                
                // Store the final URL
                final String finalImageUrl = imageUrl;
                Log.d("RecipeDetailActivity", "Attempting to load image from: " + finalImageUrl);
                
                try {
                    // Use Glide with full error handling and disk caching
                    Glide.with(this)
                        .load(finalImageUrl)
                        .placeholder(R.drawable.input_background)
                        .error(R.drawable.no_image_placeholder)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                        .timeout(15000) // 15 second timeout
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                Log.e("RecipeDetailActivity", "Failed to load image: " + finalImageUrl, e);
                                
                                // Try an alternative format for imgur URLs if this looks like an imgur URL
                                if (finalImageUrl.contains("imgur.com") && !finalImageUrl.contains("i.imgur.com")) {
                                    String altImgurUrl = finalImageUrl.replace("imgur.com", "i.imgur.com");
                                    if (!altImgurUrl.endsWith(".jpg") && !altImgurUrl.endsWith(".png") && !altImgurUrl.endsWith(".gif")) {
                                        altImgurUrl += ".jpg";
                                    }
                                    Log.d("RecipeDetailActivity", "Trying alternative imgur URL: " + altImgurUrl);
                                    Glide.with(RecipeDetailActivity.this)
                                        .load(altImgurUrl)
                                        .error(R.drawable.no_image_placeholder)
                                        .into(recipeImage);
                                }
                                return false; // false to allow error drawable to be set
                            }
                            
                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                Log.d("RecipeDetailActivity", "Successfully loaded image from: " + finalImageUrl);
                                return false; // false to allow Glide to set the resource
                            }
                        })
                        .into(recipeImage);
                } catch (Exception e) {
                    Log.e("RecipeDetailActivity", "Exception during Glide image loading", e);
                    recipeImage.setImageResource(R.drawable.no_image_placeholder);
                }
            } else {
                Log.d("RecipeDetailActivity", "No image URL available for recipe");
                recipeImage.setImageResource(R.drawable.no_image_placeholder);
            }
            
            // Update text views
            recipeNameTv.setText(recipe.getRecipeName());
            cookTime.setText(recipe.getCookTime() + " Minutes");
            Log.d("RecipeDetailActivity", "Set text fields: " + recipe.getRecipeName() + ", " + recipe.getCookTime() + " Minutes");
        }
        
        Log.d("RecipeDetailActivity", "Recipe UI initialization complete");
    }

    /**
     * Navigate to the recipe creation screen
     */
    void openAddNewRecipe() {
        Intent intent = new Intent(RecipeDetailActivity.this, AddRecipeStepActivity.class);
        // We don't pass a recipe, so a new one will be created
        
        // Add transition animation
        android.app.ActivityOptions options = android.app.ActivityOptions
            .makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left);
        startActivity(intent, options.toBundle());
    }

    private void toggleFavoriteStatus() {
        if (recipe == null) {
            showSnackbar("Recipe data missing");
            return;
        }
        
        if (isFavorite) {
            // Remove from favorites
            db.collection("user_favorites")
                    .document(userId)
                    .collection("recipes")
                    .whereEqualTo("timestamp", recipe.getTimestamp())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                document.getReference().delete()
                                        .addOnSuccessListener(aVoid -> {
                                            isFavorite = false;
                                            updateFavoriteIcon();
                                            showSnackbar("Removed from favorites");
                                        })
                                        .addOnFailureListener(e -> {
                                            showSnackbar("Error removing from favorites: " + e.getMessage());
                                        });
                            }
                        }
                    });
        } else {
            // Add to favorites
            recipe.setFavorite(true);
            db.collection("user_favorites")
                    .document(userId)
                    .collection("recipes")
                    .add(recipe)
                    .addOnSuccessListener(documentReference -> {
                        isFavorite = true;
                        updateFavoriteIcon();
                        showSnackbar("Added to favorites");
                        
                        // Check if the user wants to set a reminder for this recipe
                        promptForReminder();
                    })
                    .addOnFailureListener(e -> {
                        showSnackbar("Error adding to favorites: " + e.getMessage());
                    });
        }
    }

    private void checkIfFavorite() {
        if (db == null || userId == null || recipe == null) {
            Log.w("RecipeDetailActivity", "Cannot check favorites: db, userId, or recipe is null");
            return;
        }
        
        db.collection("user_favorites")
                .document(userId)
                .collection("recipes")
                .whereEqualTo("timestamp", recipe.getTimestamp())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Recipe is already a favorite
                        isFavorite = true;
                        updateFavoriteIcon();
                    } else {
                        // Recipe is not a favorite
                        isFavorite = false;
                        updateFavoriteIcon();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RecipeDetailActivity", "Error checking favorite status: " + e.getMessage(), e);
                    showSnackbar("Error checking favorite status: " + e.getMessage());
                });
    }
    
    private void updateFavoriteIcon() {
        if (toolbar != null && toolbar.getMenu() != null) {
            // Check if the menu item exists
            android.view.MenuItem favoriteItem = toolbar.getMenu().findItem(R.id.action_favorite);
            if (favoriteItem != null) {
                if (isFavorite) {
                    favoriteItem.setIcon(R.drawable.ic_favorite);
                    favoriteItem.setTitle("Remove from Favorites");
                } else {
                    favoriteItem.setIcon(R.drawable.ic_favorite_border);
                    favoriteItem.setTitle("Add to Favorites");
                }
            } else {
                Log.w("RecipeDetailActivity", "Favorite menu item not found");
            }
        } else {
            Log.w("RecipeDetailActivity", "Toolbar or menu is null");
        }
    }
    
    private void promptForReminder() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Set Reminder")
            .setMessage("Would you like to set a reminder to cook this recipe in your weekly plan?")
            .setPositiveButton("Yes", (dialog, which) -> {
                showWeeklyPlanDialog();
            })
            .setNegativeButton("No", (dialog, which) -> {
                dialog.dismiss();
            })
            .show();
    }
    
    private void showWeeklyPlanDialog() {
        // Create array of days
        final String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        final String[] mealTypes = {"Breakfast", "Lunch", "Dinner"};
        
        // Create layout for the dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);
        
        // Create day spinner
        TextView dayLabel = new TextView(this);
        dayLabel.setText("Select Day:");
        dayLabel.setTextSize(16);
        dayLabel.setPadding(0, 16, 0, 8);
        layout.addView(dayLabel);
        
        android.widget.Spinner daySpinner = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> dayAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, days);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daySpinner.setAdapter(dayAdapter);
        layout.addView(daySpinner);
        
        // Create meal type spinner
        TextView mealLabel = new TextView(this);
        mealLabel.setText("Select Meal:");
        mealLabel.setTextSize(16);
        mealLabel.setPadding(0, 24, 0, 8);
        layout.addView(mealLabel);
        
        android.widget.Spinner mealSpinner = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> mealAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, mealTypes);
        mealAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mealSpinner.setAdapter(mealAdapter);
        layout.addView(mealSpinner);
        
        // Show dialog
        new MaterialAlertDialogBuilder(this)
            .setTitle("Add to Weekly Plan")
            .setView(layout)
            .setPositiveButton("Add", (dialog, which) -> {
                String selectedDay = days[daySpinner.getSelectedItemPosition()];
                String selectedMeal = mealTypes[mealSpinner.getSelectedItemPosition()];
                addToWeeklyPlan(selectedDay, selectedMeal);
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .show();
    }
    
    private void addToWeeklyPlan(String day, String mealType) {
        if (recipe == null || db == null || userId == null) {
            showSnackbar("Cannot add recipe to weekly plan");
            return;
        }
        
        // Get current date and calculate the next occurrence of the selected day
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);
        int selectedDayOfWeek = getDayOfWeekFromString(day);
        
        // Calculate days to add to get to the selected day
        int daysToAdd = selectedDayOfWeek - currentDayOfWeek;
        if (daysToAdd <= 0) {
            daysToAdd += 7; // Go to next week if day has passed
        }
        
        calendar.add(java.util.Calendar.DAY_OF_YEAR, daysToAdd);
        
        // Format date as YYYY-MM-DD for Firestore
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        final String mealDate = dateFormat.format(calendar.getTime());
        
        // Create a document reference for the meal
        final DocumentSnapshot[] mealDoc = {null};
        final String finalDay = day;
        final String finalMealType = mealType;
        
        // First check if there's an existing document for this date
        db.collection("meals")
            .document(mealDate)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    mealDoc[0] = task.getResult();
                    
                    // Create or update meal plan
                    Map<String, Object> mealData = new HashMap<>();
                    
                    if (mealDoc[0] != null && mealDoc[0].exists()) {
                        // Update existing document
                        List<Long> mealList = (List<Long>) mealDoc[0].get(finalMealType);
                        if (mealList == null) {
                            mealList = new ArrayList<>();
                        }
                        
                        final List<Long> finalMealList = new ArrayList<>(mealList);
                        
                        if (!finalMealList.contains(recipe.getTimestamp())) {
                            finalMealList.add(recipe.getTimestamp());
                            mealData.put(finalMealType, finalMealList);
                            
                            db.collection("meals")
                                .document(mealDate)
                                .update(mealData)
                                .addOnSuccessListener(aVoid -> {
                                    createReminderNotification(finalDay, finalMealType);
                                    showSnackbar("Recipe added to " + finalMealType + " on " + finalDay);
                                })
                                .addOnFailureListener(e -> {
                                    showSnackbar("Error adding to meal plan: " + e.getMessage());
                                });
                        } else {
                            showSnackbar("Recipe already in this meal");
                        }
                    } else {
                        // Create new document
                        List<Long> mealList = new ArrayList<>();
                        mealList.add(recipe.getTimestamp());
                        mealData.put(finalMealType, mealList);
                        
                        // Initialize other meal types as empty
                        for (String type : new String[]{"Breakfast", "Lunch", "Dinner"}) {
                            if (!type.equals(finalMealType)) {
                                mealData.put(type, new ArrayList<Long>());
                            }
                        }
                        
                        db.collection("meals")
                            .document(mealDate)
                            .set(mealData)
                            .addOnSuccessListener(aVoid -> {
                                createReminderNotification(finalDay, finalMealType);
                                showSnackbar("Recipe added to " + finalMealType + " on " + finalDay);
                            })
                            .addOnFailureListener(e -> {
                                showSnackbar("Error adding to meal plan: " + e.getMessage());
                            });
                    }
                } else {
                    showSnackbar("Error checking meal plan: " + task.getException().getMessage());
                }
            });
    }
    
    private int getDayOfWeekFromString(String day) {
        switch (day.toLowerCase()) {
            case "sunday": return java.util.Calendar.SUNDAY;
            case "monday": return java.util.Calendar.MONDAY;
            case "tuesday": return java.util.Calendar.TUESDAY;
            case "wednesday": return java.util.Calendar.WEDNESDAY;
            case "thursday": return java.util.Calendar.THURSDAY;
            case "friday": return java.util.Calendar.FRIDAY;
            case "saturday": return java.util.Calendar.SATURDAY;
            default: return java.util.Calendar.SUNDAY;
        }
    }
    
    private void createReminderNotification(String day, String mealType) {
        // Save reminder to SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MealMateNotifications", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Create unique key for this reminder
        String reminderKey = day + "_" + mealType + "_" + recipe.getRecipeName();
        
        // Store the reminder data as JSON
        try {
            org.json.JSONObject reminderData = new org.json.JSONObject();
            reminderData.put("recipeName", recipe.getRecipeName());
            reminderData.put("recipeId", recipe.getRecipeId());
            reminderData.put("day", day);
            reminderData.put("mealType", mealType);
            reminderData.put("timestamp", System.currentTimeMillis());
            
            editor.putString(reminderKey, reminderData.toString());
            editor.apply();
            
            // Add to notification center and show instant notification
            addToNotificationCenter(day, mealType);
            
            // Show instant user notification
            showRecipeReminderNotification(day, mealType);
            
        } catch (Exception e) {
            Log.e("RecipeDetailActivity", "Error creating reminder: " + e.getMessage());
        }
    }
    
    private void addToNotificationCenter(String day, String mealType) {
        // Add notification for this reminder to the Firestore notifications collection
        if (userId == null || db == null) {
            Log.e("RecipeDetailActivity", "Cannot add notification: userId or db is null. userId: " + userId);
            Toast.makeText(this, "Cannot create notification: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d("RecipeDetailActivity", "Adding notification to Firestore for userId: " + userId);
        
        // Ensure recipe has required data
        if (recipe == null || recipe.getRecipeId() == null || recipe.getRecipeName() == null) {
            Log.e("RecipeDetailActivity", "Cannot add notification: Recipe is missing data. Recipe: " + 
                  (recipe != null ? "ID=" + recipe.getRecipeId() + ", Name=" + recipe.getRecipeName() : "null"));
            Toast.makeText(this, "Cannot create notification: Recipe data incomplete", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create string-only copy of userId to ensure exact field equality when querying
        String userIdString = userId;
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userIdString);
        notification.put("title", "Recipe Reminder");
        notification.put("message", recipe.getRecipeName() + " planned for " + mealType + " on " + day);
        notification.put("recipeName", recipe.getRecipeName());
        notification.put("recipeId", recipe.getRecipeId());
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        notification.put("day", day);
        notification.put("mealType", mealType);
        
        // Log notification data for debugging
        Log.d("RecipeDetailActivity", "Notification data being saved: " + notification);
        
        // Check if we already have this notification to avoid duplicates
        db.collection("notifications")
            .whereEqualTo("userId", userIdString)
            .whereEqualTo("recipeId", recipe.getRecipeId())
            .whereEqualTo("day", day)
            .whereEqualTo("mealType", mealType)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d("RecipeDetailActivity", "Query for existing notifications returned " + 
                      queryDocumentSnapshots.size() + " results");
                
                if (queryDocumentSnapshots.isEmpty()) {
                    // No existing notification with the same parameters, add a new one
                    db.collection("notifications")
                        .add(notification)
                        .addOnSuccessListener(documentReference -> {
                            Log.d("RecipeDetailActivity", "✅ Notification added with ID: " + documentReference.getId());
                            Toast.makeText(this, "Recipe added to " + mealType + " on " + day, Toast.LENGTH_SHORT).show();
                            
                            // Ask user if they want to view notifications
                            new android.app.AlertDialog.Builder(this)
                                .setTitle("Notification Created")
                                .setMessage("Recipe has been added to your meal plan. Would you like to view your notifications?")
                                .setPositiveButton("View Notifications", (dialog, which) -> {
                                    // Navigate to notifications screen
                                    navigateToNotifications();
                                })
                                .setNegativeButton("Continue", (dialog, which) -> dialog.dismiss())
                                .show();
                            
                            // Reload the NotificationsFragment if it's visible
                            reloadNotificationsFragmentIfVisible();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("RecipeDetailActivity", "Error adding notification: " + e.getMessage(), e);
                            Toast.makeText(this, "Error adding notification", Toast.LENGTH_SHORT).show();
                        });
                } else {
                    // Update the existing notification with a new timestamp
                    String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                    Log.d("RecipeDetailActivity", "Found existing notification with ID: " + docId + ", updating timestamp");
                    
                    db.collection("notifications")
                        .document(docId)
                        .update("timestamp", System.currentTimeMillis(),
                               "message", recipe.getRecipeName() + " planned for " + mealType + " on " + day,
                               "read", false)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("RecipeDetailActivity", "✅ Notification updated with ID: " + docId);
                            Toast.makeText(this, "Recipe reminder updated for " + mealType + " on " + day, Toast.LENGTH_SHORT).show();
                            
                            // Reload the NotificationsFragment if it's visible
                            reloadNotificationsFragmentIfVisible();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("RecipeDetailActivity", "Error updating notification: " + e.getMessage(), e);
                            Toast.makeText(this, "Error updating notification", Toast.LENGTH_SHORT).show();
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e("RecipeDetailActivity", "Error checking for existing notifications: " + e.getMessage(), e);
                // Fallback to direct add if the query fails
                db.collection("notifications")
                    .add(notification)
                    .addOnSuccessListener(documentReference -> {
                        Log.d("RecipeDetailActivity", "✅ Notification added with ID: " + documentReference.getId() + " (fallback method)");
                        Toast.makeText(this, "Recipe added to " + mealType + " on " + day, Toast.LENGTH_SHORT).show();
                        
                        // Reload the NotificationsFragment if it's visible
                        reloadNotificationsFragmentIfVisible();
                    })
                    .addOnFailureListener(e2 -> {
                        Log.e("RecipeDetailActivity", "Error adding notification: " + e2.getMessage(), e2);
                        Toast.makeText(this, "Error adding notification", Toast.LENGTH_SHORT).show();
                    });
            });
    }
    
    /**
     * Attempt to reload the NotificationsFragment if it's currently visible
     */
    private void reloadNotificationsFragmentIfVisible() {
        try {
            // Use a broadcast to notify any active fragments to reload their data
            Intent intent = new Intent("edu.prakriti.mealmate.RELOAD_NOTIFICATIONS");
            sendBroadcast(intent);
            Log.d("RecipeDetailActivity", "Broadcast sent to reload notifications");
        } catch (Exception e) {
            Log.w("RecipeDetailActivity", "Failed to broadcast reload: " + e.getMessage());
        }
    }
    
    /**
     * Show an instant notification to the user about the recipe reminder
     */
    private void showRecipeReminderNotification(String day, String mealType) {
        try {
            // For Android 13+, check permission before showing notification
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.d("RecipeDetailActivity", "Cannot show notification: Permission not granted");
                    // Don't show the notification if permission isn't granted
                    return;
                }
            }
            
            // Get notification manager
            android.app.NotificationManager notificationManager = 
                    (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            
            // Create notification channel for Android 8.0+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.app.NotificationChannel channel = new android.app.NotificationChannel(
                        "meal_reminders",
                        "Meal Reminders",
                        android.app.NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("Notifications for meal plan reminders");
                notificationManager.createNotificationChannel(channel);
            }
            
            // Create an intent for the notification to open the recipe detail
            Intent intent = new Intent(this, RecipeDetailActivity.class);
            intent.putExtra("RECIPE_ID", recipe.getRecipeId());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                    this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE);
            
            // Build the notification
            androidx.core.app.NotificationCompat.Builder builder = 
                    new androidx.core.app.NotificationCompat.Builder(this, "meal_reminders")
                    .setSmallIcon(R.drawable.ic_bell)
                    .setContentTitle("Recipe Added to Meal Plan")
                    .setContentText(recipe.getRecipeName() + " has been added to " + mealType + " on " + day)
                    .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle()
                            .bigText("Remember to cook " + recipe.getRecipeName() + " for " + 
                                    mealType + " on " + day + ". Tap to view recipe details."))
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            
            // Generate a unique notification ID based on the current time
            int notificationId = (int) System.currentTimeMillis() / 1000;
            
            // Show the notification
            notificationManager.notify(notificationId, builder.build());
            
            Log.d("RecipeDetailActivity", "Instant notification shown for recipe: " + recipe.getRecipeName());
        } catch (Exception e) {
            Log.e("RecipeDetailActivity", "Error showing notification: " + e.getMessage(), e);
        }
    }

    // Add a new method to fetch recipe data from Firestore by ID
    private void fetchRecipeFromFirestore(String recipeId) {
        customProgressDialog.show();
        
        Log.d("RecipeDetailActivity", "Starting Firestore query for recipe ID: " + recipeId);
        
        db.collection("recipes").document(recipeId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                Log.d("RecipeDetailActivity", "Firestore query successful, document exists: " + documentSnapshot.exists());
                
                if (documentSnapshot.exists()) {
                    try {
                        Log.d("RecipeDetailActivity", "Recipe document data: " + documentSnapshot.getData());
                        
                        // Log fields for debugging
                        for (String field : documentSnapshot.getData().keySet()) {
                            Log.d("RecipeDetailActivity", "Field: " + field + ", Type: " + 
                                    (documentSnapshot.get(field) != null ? documentSnapshot.get(field).getClass().getSimpleName() : "null"));
                        }
                        
                        // Create Recipe object from document
                        recipe = new Recipe();
                        recipe.setRecipeId(recipeId);
                        
                        // Get basic data with null checks
                        String name = documentSnapshot.getString("recipeName");
                        String cookTime = documentSnapshot.getString("cookTime");
                        String photoUrl = documentSnapshot.getString("photoUrl");
                        Long timestamp = documentSnapshot.getLong("timestamp");
                        
                        Log.d("RecipeDetailActivity", "Basic recipe data - Name: " + name + 
                                ", CookTime: " + cookTime + 
                                ", PhotoUrl: " + (photoUrl != null ? "exists" : "null") + 
                                ", Timestamp: " + timestamp);
                        
                        // Set values with null checks
                        recipe.setRecipeName(name != null ? name : "Unnamed Recipe");
                        recipe.setCookTime(cookTime != null ? cookTime : "0");
                        recipe.setPhotoUrl(photoUrl);
                        recipe.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());
                        
                        // Handle favorite flag
                        if (documentSnapshot.contains("favorite")) {
                            Boolean favorite = documentSnapshot.getBoolean("favorite");
                            recipe.setFavorite(favorite != null ? favorite : false);
                            Log.d("RecipeDetailActivity", "Recipe favorite status: " + recipe.isFavorite());
                        }
                        
                        // Handle ingredients map
                        Map<String, Object> rawIngredients = (Map<String, Object>) documentSnapshot.get("ingredients");
                        Log.d("RecipeDetailActivity", "Raw ingredients map: " + (rawIngredients != null ? rawIngredients.keySet() : "null"));
                        
                        Map<String, List<String>> processedIngredients = new HashMap<>();
                        
                        if (rawIngredients != null) {
                            for (Map.Entry<String, Object> entry : rawIngredients.entrySet()) {
                                String category = entry.getKey();
                                Object value = entry.getValue();
                                
                                // Handle different data types that might come from Firestore
                                if (value instanceof List) {
                                    List<String> ingredientsList = (List<String>) value;
                                    processedIngredients.put(category, ingredientsList);
                                    Log.d("RecipeDetailActivity", "Category: " + category + " with " + 
                                            (ingredientsList != null ? ingredientsList.size() : 0) + " ingredients");
                                } else if (value instanceof Map) {
                                    // Handle if ingredients are stored in a Map format
                                    Map<String, Object> ingredientsMap = (Map<String, Object>) value;
                                    List<String> ingredientsList = new ArrayList<>();
                                    
                                    for (Object ingredient : ingredientsMap.values()) {
                                        if (ingredient instanceof String) {
                                            ingredientsList.add((String) ingredient);
                                        }
                                    }
                                    
                                    processedIngredients.put(category, ingredientsList);
                                    Log.d("RecipeDetailActivity", "Category (from map): " + category + " with " + 
                                            ingredientsList.size() + " ingredients");
                                }
                            }
                        } else {
                            Log.w("RecipeDetailActivity", "No ingredients found in recipe document");
                        }
                        
                        recipe.setIngredients(processedIngredients);
                        
                        // Handle instructions list with better error handling
                        List<Map<String, Object>> instructionsList = new ArrayList<>();
                        Object rawInstructions = documentSnapshot.get("instructions");
                        
                        if (rawInstructions instanceof List) {
                            List<?> rawList = (List<?>) rawInstructions;
                            
                            // Convert each item to a proper instruction map
                            for (int i = 0; i < rawList.size(); i++) {
                                Object item = rawList.get(i);
                                Map<String, Object> instructionMap = new HashMap<>();
                                
                                if (item instanceof Map) {
                                    // Standard format: map with stepNumber and instruction fields
                                    Map<String, Object> itemMap = (Map<String, Object>) item;
                                    instructionMap.put("stepNumber", itemMap.getOrDefault("stepNumber", (i+1)));
                                    instructionMap.put("instruction", itemMap.getOrDefault("instruction", ""));
                                } else if (item instanceof String) {
                                    // Simple format: just strings in a list
                                    instructionMap.put("stepNumber", i+1);
                                    instructionMap.put("instruction", item);
                                }
                                
                                instructionsList.add(instructionMap);
                                Log.d("RecipeDetailActivity", "Processed instruction: " + instructionMap);
                            }
                        } else {
                            Log.w("RecipeDetailActivity", "Instructions not found or not in expected format");
                        }
                        
                        recipe.setInstructions(instructionsList);
                        
                        // Log the created recipe object
                        Log.d("RecipeDetailActivity", "Created Recipe object: " +
                                "ID=" + recipe.getRecipeId() + 
                                ", Name=" + recipe.getRecipeName() + 
                                ", CookTime=" + recipe.getCookTime() + 
                                ", PhotoUrl=" + (recipe.getPhotoUrl() != null ? "exists" : "null") +
                                ", Ingredients count=" + (recipe.getIngredients() != null ? recipe.getIngredients().size() : 0) +
                                ", Instructions count=" + (recipe.getInstructions() != null ? recipe.getInstructions().size() : 0));
                        
                        // Initialize UI with fetched recipe
                        initializeRecipeUI();
                        
                        // Check if it's a favorite
                        if (userId != null) {
                            checkIfFavorite();
                        }
                        
                        Log.d("RecipeDetailActivity", "Recipe loaded successfully: " + recipe.getRecipeName());
                    } catch (Exception e) {
                        Log.e("RecipeDetailActivity", "Error processing recipe data", e);
                        showSnackbar("Error processing recipe data: " + e.getMessage());
                        // Still try to show UI with whatever data we have
                        if (recipe != null) {
                            initializeRecipeUI();
                        } else {
                            finish();
                        }
                    }
                } else {
                    Log.e("RecipeDetailActivity", "Recipe document does not exist for ID: " + recipeId);
                    showSnackbar("Error: Recipe not found");
                    finish();
                }
                customProgressDialog.dismiss();
            })
            .addOnFailureListener(e -> {
                customProgressDialog.dismiss();
                Log.e("RecipeDetailActivity", "Error fetching recipe", e);
                showSnackbar("Error loading recipe: " + e.getMessage());
                finish();
            });
    }

    // Add a method to initialize the UI with recipe data
    private void initializeRecipeUI() {
        Log.d("RecipeDetailActivity", "Initializing UI with recipe: " + (recipe != null ? recipe.getRecipeName() : "null"));
        
        if (recipe == null) {
            Log.e("RecipeDetailActivity", "Cannot initialize UI - recipe is null");
            showSnackbar("Error: Recipe data is missing");
            return;
        }
        
        // Safety check for recipe data completeness
        if (!verifyRecipeData()) {
            Log.e("RecipeDetailActivity", "Recipe data verification failed");
            showSnackbar("Error: Recipe data is incomplete");
            // Will continue anyway with fixed data from verifyRecipeData
        }
        
        // Set the activity title to the recipe name
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(recipe.getRecipeName());
            Log.d("RecipeDetailActivity", "Set action bar title to: " + recipe.getRecipeName());
        } else {
            Log.w("RecipeDetailActivity", "SupportActionBar is null - couldn't set title");
        }
        
        // Initialize UI components
        recipeImage = findViewById(R.id.recipeImage);
        recipeNameTv = findViewById(R.id.recipeName);
        cookTime = findViewById(R.id.cookTime);
        ingredientsContainer = findViewById(R.id.ingredientsContainer);
        
        if (recipeImage == null || recipeNameTv == null || cookTime == null || ingredientsContainer == null) {
            Log.e("RecipeDetailActivity", "One or more UI components not found");
            showSnackbar("Error: UI components missing");
            return;
        }
        
        Log.d("RecipeDetailActivity", "UI components initialized successfully");
        
        // Load ingredients
        Log.d("RecipeDetailActivity", "Loading ingredients into UI - count: " + 
                (recipe.getIngredients() != null ? recipe.getIngredients().size() : 0) + 
                ", categories: " + (recipe.getIngredients() != null ? recipe.getIngredients().keySet() : "none"));
        loadIngredients(recipe.getIngredients());
        
        // Set up ViewPager2 for instructions
        instructionsViewPager = findViewById(R.id.instructionsViewPager);
        if (instructionsViewPager == null) {
            Log.e("RecipeDetailActivity", "instructionsViewPager is null - can't display instructions");
            showSnackbar("Error displaying recipe instructions");
            return;
        }

        // Check if instructions exist and handle empty case
        try {
            Log.d("RecipeDetailActivity", "Preparing to display instructions. Count: " + 
                  (recipe.getInstructions() != null ? recipe.getInstructions().size() : "null"));
            
            if (recipe.getInstructions() == null || recipe.getInstructions().isEmpty()) {
                Log.w("RecipeDetailActivity", "No instructions available - showing message");
                
                // Display a message that there are no instructions
                TextView noInstructionsMessage = new TextView(this);
                noInstructionsMessage.setText("No instructions available for this recipe.");
                noInstructionsMessage.setTextSize(16);
                noInstructionsMessage.setPadding(16, 16, 16, 16);
                noInstructionsMessage.setTextColor(getResources().getColor(R.color.on_surface));
                
                // Add this TextView in place of ViewPager2 container
                ViewGroup container = (ViewGroup) instructionsViewPager.getParent();
                if (container != null) {
                    int index = container.indexOfChild(instructionsViewPager);
                    container.removeView(instructionsViewPager);
                    container.addView(noInstructionsMessage, index);
                    
                    // Hide navigation buttons
                    hideStepUIComponents();
                } else {
                    Log.e("RecipeDetailActivity", "ViewPager parent container not found");
                }
            } else {
                // Set up ViewPager with instructions
                Log.d("RecipeDetailActivity", "Setting up instructionsViewPager with " + 
                      recipe.getInstructions().size() + " instructions");
                
                // Initialize the step indicator and progress indicator
                stepIndicator = findViewById(R.id.step_indicator);
                progressIndicator = findViewById(R.id.progress_indicator);
                
                // Set up the progress indicator if it exists
                if (progressIndicator != null) {
                    progressIndicator.setMax(100);
                    progressIndicator.setProgress(0);
                }
                
                // Verify each instruction has the required fields
                boolean hasValidInstructions = true;
                List<Map<String, Object>> validInstructions = new ArrayList<>();
                
                for (int i = 0; i < recipe.getInstructions().size(); i++) {
                    Map<String, Object> step = recipe.getInstructions().get(i);
                    
                    // Log instruction details
                    Log.d("RecipeDetailActivity", "Instruction " + i + ": " + step);
                    
                    // Check if instruction has required fields
                    if (step == null) {
                        Log.e("RecipeDetailActivity", "Instruction at index " + i + " is null");
                        continue;
                    }
                    
                    // Create a valid step map
                    Map<String, Object> validStep = new HashMap<>();
                    
                    // Handle step number
                    if (step.containsKey("stepNumber")) {
                        Object stepNumber = step.get("stepNumber");
                        if (stepNumber != null) {
                            validStep.put("stepNumber", stepNumber);
                        } else {
                            Log.w("RecipeDetailActivity", "Step number is null for instruction " + i);
                            validStep.put("stepNumber", i + 1);
                        }
                    } else {
                        Log.w("RecipeDetailActivity", "No step number for instruction " + i);
                        validStep.put("stepNumber", i + 1);
                    }
                    
                    // Handle instruction text
                    if (step.containsKey("instruction")) {
                        Object instructionText = step.get("instruction");
                        if (instructionText != null && !instructionText.toString().trim().isEmpty()) {
                            validStep.put("instruction", instructionText);
                        } else {
                            Log.w("RecipeDetailActivity", "Instruction text is null or empty for step " + i);
                            validStep.put("instruction", "Step " + (i + 1) + " - No details provided");
                        }
                    } else {
                        Log.w("RecipeDetailActivity", "No instruction text for step " + i);
                        validStep.put("instruction", "Step " + (i + 1) + " - No details provided");
                    }
                    
                    // Add valid step to list
                    validInstructions.add(validStep);
                }
                
                // If we had to fix any instructions, update the recipe
                if (validInstructions.size() != recipe.getInstructions().size()) {
                    Log.d("RecipeDetailActivity", "Fixed instructions list: original size=" + 
                          recipe.getInstructions().size() + ", valid size=" + validInstructions.size());
                    recipe.setInstructions(validInstructions);
                }
                
                // Create adapter with explicit list to avoid reference issues
                try {
                    // Make a deep copy of the instructions to avoid any reference issues
                    List<Map<String, Object>> adapterInstructions = new ArrayList<>();
                    
                    for (Map<String, Object> step : recipe.getInstructions()) {
                        Map<String, Object> copiedStep = new HashMap<>(step);
                        adapterInstructions.add(copiedStep);
                    }
                    
                    Log.d("RecipeDetailActivity", "Creating adapter with " + adapterInstructions.size() + " instructions");
                    
                    // Create the adapter
                    instructionsAdapter = new InstructionDetailAdapter(this, adapterInstructions);
                    
                    // Verify adapter contains instructions
                    Log.d("RecipeDetailActivity", "Adapter created with " + instructionsAdapter.getItemCount() + " items");
                    
                    // Configure ViewPager2 settings to ensure proper layout
                    instructionsViewPager.setOffscreenPageLimit(1);
                    
                    // Fix for ViewPager2 height issues
                    ViewGroup.LayoutParams layoutParams = instructionsViewPager.getLayoutParams();
                    if (layoutParams.height <= 0) {
                        // Set a minimum height if none is specified
                        layoutParams.height = 300; // 300dp minimum
                        instructionsViewPager.setLayoutParams(layoutParams);
                        Log.d("RecipeDetailActivity", "Set minimum height for instructionsViewPager");
                    }
                    
                    try {
                        // Set adapter to ViewPager
                        instructionsViewPager.setAdapter(instructionsAdapter);
                        
                        // Fix ViewPager2 height issues - critical to avoid the "Pages must fill the whole ViewPager2" crash
                        instructionsViewPager.setPageTransformer((page, position) -> {
                            // Ensure child views use match_parent
                            ViewGroup.LayoutParams params = page.getLayoutParams();
                            if (params.width != ViewGroup.LayoutParams.MATCH_PARENT || 
                                params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                                page.setLayoutParams(params);
                                Log.d("RecipeDetailActivity", "Fixed page dimensions in transformer");
                            }
                        });
                        
                        // Log after ViewPager setup
                        Log.d("RecipeDetailActivity", "ViewPager adapter set successfully");
                        
                        // Add swipe gesture functionality
                        setupSwipeGestures();
                        
                        // Set up navigation buttons
                        prevButton = findViewById(R.id.prevButton);
                        nextButton = findViewById(R.id.nextButton);
                        
                        if (prevButton != null && nextButton != null) {
                            prevButton.setOnClickListener(v -> {
                                if (instructionsViewPager.getCurrentItem() > 0) {
                                    instructionsViewPager.setCurrentItem(instructionsViewPager.getCurrentItem() - 1);
                                }
                            });
                            
                            nextButton.setOnClickListener(v -> {
                                if (instructionsViewPager.getCurrentItem() < instructionsAdapter.getItemCount() - 1) {
                                    instructionsViewPager.setCurrentItem(instructionsViewPager.getCurrentItem() + 1);
                                }
                            });
                            
                            // Add page change listener for instruction steps
                            instructionsViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                                @Override
                                public void onPageSelected(int position) {
                                    // Update button state based on position
                                    prevButton.setEnabled(position > 0);
                                    nextButton.setEnabled(position < instructionsAdapter.getItemCount() - 1);
                                    
                                    // Update button text for last item
                                    if (position == instructionsAdapter.getItemCount() - 1) {
                                        nextButton.setText("Finish");
                                    } else {
                                        nextButton.setText("Next");
                                    }
                                    
                                    // Update step indicator and progress
                                    updateStepProgress(position, instructionsAdapter.getItemCount());
                                }
                            });

                            // Initialize step indicator and progress for first step
                            updateStepProgress(0, instructionsAdapter.getItemCount());
                        } else {
                            Log.w("RecipeDetailActivity", "Navigation buttons not found");
                        }
                    } catch (IllegalStateException viewPagerException) {
                        // Catch specific ViewPager2 "Pages must fill the whole ViewPager2" exception
                        Log.e("RecipeDetailActivity", "ViewPager2 layout error - falling back to plain text view", viewPagerException);
                        
                        // Create a fallback LinearLayout instead of ViewPager2
                        LinearLayout fallbackInstructionsLayout = new LinearLayout(this);
                        fallbackInstructionsLayout.setOrientation(LinearLayout.VERTICAL);
                        fallbackInstructionsLayout.setPadding(32, 16, 32, 16);
                        
                        // Add each instruction as a TextView to the LinearLayout
                        for (int i = 0; i < adapterInstructions.size(); i++) {
                            final int position = i;
                            final Map<String, Object> step = adapterInstructions.get(position);
                            final int totalSteps = adapterInstructions.size();
                            
                            // Create a container for each step
                            LinearLayout stepLayout = new LinearLayout(this);
                            stepLayout.setOrientation(LinearLayout.VERTICAL);
                            stepLayout.setPadding(16, 24, 16, 24);
                            stepLayout.setBackground(getResources().getDrawable(R.drawable.rounded_border_bg));
                            
                            LinearLayout.LayoutParams stepParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            stepParams.setMargins(0, 16, 0, 16);
                            stepLayout.setLayoutParams(stepParams);
                            
                            // Add step title
                            TextView stepTitle = new TextView(this);
                            int stepNumber = position + 1;
                            if (step.containsKey("stepNumber") && step.get("stepNumber") != null) {
                                Object stepObj = step.get("stepNumber");
                                if (stepObj instanceof Long) {
                                    stepNumber = ((Long) stepObj).intValue();
                                } else if (stepObj instanceof Integer) {
                                    stepNumber = (Integer) stepObj;
                                }
                            }
                            
                            final int finalStepNumber = stepNumber;
                            stepTitle.setText("Step " + finalStepNumber + " of " + totalSteps);
                            stepTitle.setTextSize(18);
                            stepTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                            stepTitle.setTextColor(getResources().getColor(R.color.on_surface));
                            stepTitle.setPadding(0, 0, 0, 16);
                            stepLayout.addView(stepTitle);
                            
                            // Add instruction text
                            TextView instructionText = new TextView(this);
                            String instruction = "No details available for this step";
                            
                            if (step.containsKey("instruction") && step.get("instruction") != null) {
                                instruction = step.get("instruction").toString();
                            }
                            
                            instructionText.setText(instruction);
                            instructionText.setTextSize(16);
                            instructionText.setTextColor(getResources().getColor(R.color.on_surface_variant));
                            stepLayout.addView(instructionText);
                            
                            // Add step container to the main layout
                            fallbackInstructionsLayout.addView(stepLayout);
                        }
                        
                        // Replace ViewPager with the fallback layout
                        ViewGroup parent = (ViewGroup) instructionsViewPager.getParent();
                        int index = parent.indexOfChild(instructionsViewPager);
                        parent.removeView(instructionsViewPager);
                        parent.addView(fallbackInstructionsLayout, index);
                        
                        // Hide navigation buttons
                        hideStepUIComponents();
                    }
                } catch (Exception e) {
                    Log.e("RecipeDetailActivity", "Error setting up instructions adapter", e);
                    showSnackbar("Error displaying instructions");
                    
                    // Display fallback message
                    TextView errorInstructionsMessage = new TextView(this);
                    errorInstructionsMessage.setText("Error displaying instructions. Please try again later.");
                    errorInstructionsMessage.setTextSize(16);
                    errorInstructionsMessage.setPadding(16, 16, 16, 16);
                    errorInstructionsMessage.setTextColor(getResources().getColor(R.color.on_surface));
                    
                    // Add this TextView in place of ViewPager2 container
                    ViewGroup container = (ViewGroup) instructionsViewPager.getParent();
                    if (container != null) {
                        int index = container.indexOfChild(instructionsViewPager);
                        container.removeView(instructionsViewPager);
                        container.addView(errorInstructionsMessage, index);
                        
                        // Hide navigation buttons
                        hideStepUIComponents();
                    }
                }
            }
        } catch (Exception e) {
            Log.e("RecipeDetailActivity", "Error processing instructions", e);
            showSnackbar("Error loading recipe instructions");
        }

        // Load recipe image using Glide with improved error handling
        String imageUrl = recipe.getPhotoUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d("RecipeDetailActivity", "Original recipe image URL: " + imageUrl);
            
            // Clean up the URL if needed
            imageUrl = imageUrl.trim();
            if (imageUrl.contains(" ")) {
                imageUrl = imageUrl.replace(" ", "%20");
                Log.d("RecipeDetailActivity", "URL contained spaces, cleaned to: " + imageUrl);
            }
            
            // Check for imgur links that might be missing the full URL
            if (imageUrl.startsWith("i.imgur.com/")) {
                Log.d("RecipeDetailActivity", "Fixing imgur URL format");
                imageUrl = "https://" + imageUrl;
            }
            
            // Handle edge cases like missing http/https prefix
            if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && 
                !imageUrl.startsWith("file://") && !imageUrl.startsWith("content://")) {
                Log.w("RecipeDetailActivity", "Image URL missing protocol, adding https://");
                imageUrl = "https://" + imageUrl;
            }
            
            // Store the final URL
            final String finalImageUrl = imageUrl;
            Log.d("RecipeDetailActivity", "Attempting to load image from: " + finalImageUrl);
            
            try {
                // Use Glide with full error handling and disk caching
                Glide.with(this)
                    .load(finalImageUrl)
                    .placeholder(R.drawable.input_background)
                    .error(R.drawable.no_image_placeholder)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .timeout(15000) // 15 second timeout
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            Log.e("RecipeDetailActivity", "Failed to load image: " + finalImageUrl, e);
                            
                            // Try an alternative format for imgur URLs if this looks like an imgur URL
                            if (finalImageUrl.contains("imgur.com") && !finalImageUrl.contains("i.imgur.com")) {
                                String altImgurUrl = finalImageUrl.replace("imgur.com", "i.imgur.com");
                                if (!altImgurUrl.endsWith(".jpg") && !altImgurUrl.endsWith(".png") && !altImgurUrl.endsWith(".gif")) {
                                    altImgurUrl += ".jpg";
                                }
                                Log.d("RecipeDetailActivity", "Trying alternative imgur URL: " + altImgurUrl);
                                Glide.with(RecipeDetailActivity.this)
                                    .load(altImgurUrl)
                                    .error(R.drawable.no_image_placeholder)
                                    .into(recipeImage);
                            }
                            return false; // false to allow error drawable to be set
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            Log.d("RecipeDetailActivity", "Successfully loaded image from: " + finalImageUrl);
                            return false; // false to allow Glide to set the resource
                        }
                    })
                    .into(recipeImage);
            } catch (Exception e) {
                Log.e("RecipeDetailActivity", "Exception during Glide image loading", e);
                recipeImage.setImageResource(R.drawable.no_image_placeholder);
            }
        } else {
            Log.d("RecipeDetailActivity", "No image URL available for recipe");
            recipeImage.setImageResource(R.drawable.no_image_placeholder);
        }
        
        // Set recipe name and cook time
        recipeNameTv.setText(recipe.getRecipeName());
        cookTime.setText(recipe.getCookTime() + " Minutes");
        Log.d("RecipeDetailActivity", "Set text fields: " + recipe.getRecipeName() + ", " + recipe.getCookTime() + " Minutes");
        
        Log.d("RecipeDetailActivity", "Recipe UI initialization complete");
    }

    /**
     * Checks and fixes any issues with the recipe data to ensure it can be displayed correctly
     * @return true if recipe data is valid, false if it needed repairs
     */
    private boolean verifyRecipeData() {
        boolean isValid = true;
        
        // Check recipe name
        if (recipe.getRecipeName() == null || recipe.getRecipeName().isEmpty()) {
            Log.w("RecipeDetailActivity", "Recipe name is missing, setting a default");
            recipe.setRecipeName("Unnamed Recipe");
            isValid = false;
        }
        
        // Check cook time 
        if (recipe.getCookTime() == null || recipe.getCookTime().isEmpty()) {
            Log.w("RecipeDetailActivity", "Cook time is missing, setting a default");
            recipe.setCookTime("0");
            isValid = false;
        }
        
        // Check ingredients
        if (recipe.getIngredients() == null) {
            Log.w("RecipeDetailActivity", "Ingredients map is null, initializing empty");
            recipe.setIngredients(new HashMap<>());
            isValid = false;
        }
        
        // Check instructions - crucial part
        if (recipe.getInstructions() == null) {
            Log.w("RecipeDetailActivity", "Instructions list is null, initializing empty");
            recipe.setInstructions(new ArrayList<>());
            isValid = false;
        }
        
        // If instructions exist but are malformed, try to fix them
        if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
            List<Map<String, Object>> fixedInstructions = new ArrayList<>();
            
            for (int i = 0; i < recipe.getInstructions().size(); i++) {
                Map<String, Object> step = recipe.getInstructions().get(i);
                
                if (step == null) {
                    Log.w("RecipeDetailActivity", "Instruction at index " + i + " is null, creating default");
                    step = new HashMap<>();
                    isValid = false;
                }
                
                // Create fixed step with available data
                Map<String, Object> fixedStep = new HashMap<>();
                
                // Check for stepNumber field
                if (!step.containsKey("stepNumber")) {
                    Log.w("RecipeDetailActivity", "Missing stepNumber in instruction " + i);
                    fixedStep.put("stepNumber", i + 1);
                    isValid = false;
                } else {
                    Object stepNumberObj = step.get("stepNumber");
                    if (stepNumberObj == null) {
                        fixedStep.put("stepNumber", i + 1);
                        isValid = false;
                    } else if (stepNumberObj instanceof Integer || stepNumberObj instanceof Long) {
                        fixedStep.put("stepNumber", stepNumberObj);
                    } else {
                        try {
                            // Try to convert to number
                            int stepNumber = Integer.parseInt(stepNumberObj.toString());
                            fixedStep.put("stepNumber", stepNumber);
                            isValid = false;
                        } catch (NumberFormatException e) {
                            fixedStep.put("stepNumber", i + 1);
                            isValid = false;
                        }
                    }
                }
                
                // Check for instruction field
                if (!step.containsKey("instruction")) {
                    Log.w("RecipeDetailActivity", "Missing instruction text in step " + i);
                    fixedStep.put("instruction", "Step " + (i + 1) + " - No details provided");
                    isValid = false;
                } else {
                    Object instructionObj = step.get("instruction");
                    if (instructionObj == null) {
                        fixedStep.put("instruction", "Step " + (i + 1) + " - No details provided");
                        isValid = false;
                    } else {
                        fixedStep.put("instruction", instructionObj.toString());
                    }
                }
                
                // Copy any other fields
                for (Map.Entry<String, Object> entry : step.entrySet()) {
                    String key = entry.getKey();
                    if (!key.equals("stepNumber") && !key.equals("instruction") && entry.getValue() != null) {
                        fixedStep.put(key, entry.getValue());
                    }
                }
                
                fixedInstructions.add(fixedStep);
            }
            
            // Replace with fixed instructions
            if (!isValid) {
                Log.d("RecipeDetailActivity", "Replacing malformed instructions with fixed version");
                recipe.setInstructions(fixedInstructions);
            }
        } else if (recipe.getInstructions() != null && recipe.getInstructions().isEmpty()) {
            // Add a default instruction if list is empty
            Log.w("RecipeDetailActivity", "Instructions list is empty, adding default instruction");
            Map<String, Object> defaultStep = new HashMap<>();
            defaultStep.put("stepNumber", 1);
            defaultStep.put("instruction", "No specific instructions provided for this recipe.");
            
            List<Map<String, Object>> defaultInstructions = new ArrayList<>();
            defaultInstructions.add(defaultStep);
            recipe.setInstructions(defaultInstructions);
            isValid = false;
        }
        
        return isValid;
    }

    private void updateStepProgress(int position, int totalSteps) {
        if (stepIndicator != null && progressIndicator != null) {
            stepIndicator.setText("Step " + (position + 1) + " of " + totalSteps);
            progressIndicator.setProgress((position + 1) * 100 / totalSteps);
        }
    }

    /**
     * Hides all step-related UI components when there are no instructions
     */
    private void hideStepUIComponents() {
        // Hide navigation buttons
        if (prevButton != null) prevButton.setVisibility(View.GONE);
        if (nextButton != null) nextButton.setVisibility(View.GONE);
        
        // Hide step indicator and progress
        if (stepIndicator != null) stepIndicator.setVisibility(View.GONE);
        if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
    }

    /**
     * Shows all step-related UI components when there are instructions
     */
    private void showStepUIComponents(int totalSteps) {
        // Show navigation buttons
        if (prevButton != null) prevButton.setVisibility(View.VISIBLE);
        if (nextButton != null) nextButton.setVisibility(View.VISIBLE);
        
        // Show and initialize step indicator and progress
        if (stepIndicator != null) {
            stepIndicator.setVisibility(View.VISIBLE);
            stepIndicator.setText("Step 1 of " + totalSteps);
        }
        
        if (progressIndicator != null) {
            progressIndicator.setVisibility(View.VISIBLE);
            progressIndicator.setMax(100);
            progressIndicator.setProgress(100 / totalSteps);
        }
    }

    /**
     * Upload image to Imgur and update the recipe with the new image URL
     */
    private void uploadImageToImgur(Uri imageUri) {
        new Thread(() -> {
            try {
                // Convert the image to a File
                java.io.File imageFile = getFileFromUri(imageUri);
                if (imageFile == null) {
                    runOnUiThread(() -> {
                        customProgressDialog.dismiss();
                        showSnackbar("Failed to process image file");
                    });
                    return;
                }

                // Create OkHttpClient for the network request
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

                // Create request body with the image file
                okhttp3.RequestBody requestBody = new okhttp3.MultipartBody.Builder()
                        .setType(okhttp3.MultipartBody.FORM)
                        .addFormDataPart("image", imageFile.getName(),
                                okhttp3.RequestBody.create(imageFile, okhttp3.MediaType.parse("image/*")))
                        .build();

                // Build the request with Imgur API headers
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(IMGUR_UPLOAD_URL)
                        .header("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                        .post(requestBody)
                        .build();

                // Execute the request
                okhttp3.Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        customProgressDialog.dismiss();
                        showSnackbar("Failed to upload image: " + response.message());
                    });
                    return;
                }

                // Extract image URL from response
                String responseBody = response.body().string();
                String imageUrl = extractImageUrl(responseBody);

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // Update recipe with new image URL
                    updateRecipeWithNewImage(imageUrl);
                } else {
                    runOnUiThread(() -> {
                        customProgressDialog.dismiss();
                        showSnackbar("Failed to get image URL from server response");
                    });
                }

            } catch (Exception e) {
                Log.e("RecipeDetailActivity", "Error uploading image", e);
                runOnUiThread(() -> {
                    customProgressDialog.dismiss();
                    showSnackbar("Error uploading image: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Extract the image URL from the Imgur JSON response
     */
    private String extractImageUrl(String jsonResponse) {
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject(jsonResponse);
            boolean success = jsonObject.getBoolean("success");
            
            if (success) {
                org.json.JSONObject data = jsonObject.getJSONObject("data");
                String link = data.getString("link");
                Log.d("RecipeDetailActivity", "Extracted image URL: " + link);
                return link;
            } else {
                Log.e("RecipeDetailActivity", "Imgur upload unsuccessful: " + jsonResponse);
                return null;
            }
        } catch (org.json.JSONException e) {
            Log.e("RecipeDetailActivity", "Error parsing JSON response", e);
            return null;
        }
    }

    /**
     * Get a File from the content URI
     */
    private java.io.File getFileFromUri(Uri uri) {
        try {
            // Create a temporary file to store the image
            java.io.File outputFile = java.io.File.createTempFile("image", ".jpg", getCacheDir());
            
            // Copy the content from the URI to the file
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(outputFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            outputStream.close();
            inputStream.close();
            
            return outputFile;
        } catch (Exception e) {
            Log.e("RecipeDetailActivity", "Error creating file from URI", e);
            return null;
        }
    }

    /**
     * Update the recipe with the new image URL
     */
    private void updateRecipeWithNewImage(String imageUrl) {
        if (recipe == null || recipe.getRecipeId() == null || recipe.getRecipeId().isEmpty()) {
            runOnUiThread(() -> {
                customProgressDialog.dismiss();
                showSnackbar("Cannot update recipe: recipe ID is missing");
            });
            return;
        }
        
        // Set the new image URL in the recipe object
        recipe.setPhotoUrl(imageUrl);
        
        // Update the recipe in Firestore
        db.collection("recipes")
            .document(recipe.getRecipeId())
            .update("photoUrl", imageUrl)
            .addOnSuccessListener(aVoid -> {
                Log.d("RecipeDetailActivity", "Recipe image updated successfully");
                
                // Also update in user_favorites if it's a favorite
                if (userId != null && recipe.isFavorite()) {
                    db.collection("user_favorites")
                        .document(userId)
                        .collection("recipes")
                        .document(recipe.getRecipeId())
                        .update("photoUrl", imageUrl)
                        .addOnSuccessListener(aVoid2 -> {
                            Log.d("RecipeDetailActivity", "Recipe image updated in favorites");
                            runOnUiThread(() -> {
                                customProgressDialog.dismiss();
                                showSnackbar("Recipe image updated successfully");
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e("RecipeDetailActivity", "Error updating favorite recipe image", e);
                            runOnUiThread(() -> {
                                customProgressDialog.dismiss();
                                showSnackbar("Recipe image updated, but failed to update in favorites");
                            });
                        });
                } else {
                    runOnUiThread(() -> {
                        customProgressDialog.dismiss();
                        showSnackbar("Recipe image updated successfully");
                    });
                }
            })
            .addOnFailureListener(e -> {
                Log.e("RecipeDetailActivity", "Error updating recipe image", e);
                runOnUiThread(() -> {
                    customProgressDialog.dismiss();
                    showSnackbar("Error updating recipe image: " + e.getMessage());
                });
            });
    }

    /**
     * Check for notification permission on Android 13+
     */
    private void checkNotificationPermission() {
        // Only needed for Android 13+ (TIRAMISU)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Request permission
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            // Check if the permission is granted
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d("RecipeDetailActivity", "Notification permission granted");
            } else {
                Log.d("RecipeDetailActivity", "Notification permission denied");
                // Show a message to the user about missing notification permission
                Toast.makeText(this, "Notification permission denied. You won't receive meal reminders.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Navigate to the notifications screen in the Dashboard
     */
    private void navigateToNotifications() {
        Intent intent = new Intent(this, edu.prakriti.mealmate.home.DashboardActivity.class);
        intent.putExtra("FRAGMENT_INDEX", 3); // Index for notifications fragment
        
        // Add transition animation
        android.app.ActivityOptions options = android.app.ActivityOptions
            .makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left);
        startActivity(intent, options.toBundle());
    }
}