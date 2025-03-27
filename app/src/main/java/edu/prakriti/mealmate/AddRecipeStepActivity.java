package edu.prakriti.mealmate;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.prakriti.mealmate.fragments.RecipeStepBasicFragment;
import edu.prakriti.mealmate.fragments.RecipeStepIngredientsFragment;
import edu.prakriti.mealmate.fragments.RecipeStepInstructionsFragment;
import edu.prakriti.mealmate.model.InstructionStep;
import edu.prakriti.mealmate.model.Recipe;
import edu.prakriti.mealmate.model.RecipeIngredient;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddRecipeStepActivity extends AppCompatActivity implements 
        RecipeStepBasicFragment.BasicInfoListener,
        RecipeStepIngredientsFragment.IngredientsListener,
        RecipeStepInstructionsFragment.InstructionsListener {

    private static final String TAG = "AddRecipeStepActivity";
    private static final int TOTAL_STEPS = 3;
    
    // UI Components
    private ViewPager2 viewPager;
    private LinearProgressIndicator progressIndicator;
    private TextView stepIndicator;
    private Button btnPrevious, btnNext;
    
    // Recipe Data
    private String recipeName;
    private String prepTime;
    private String cookTime;
    private String servingSize;
    private Uri recipeImageUri;
    private List<RecipeIngredient> ingredients = new ArrayList<>();
    private List<InstructionStep> instructions = new ArrayList<>();
    private boolean addToFavorites = false;
    
    // For image upload
    private static final String IMGUR_CLIENT_ID = "6deebb69c6310e5";
    private static final String IMGUR_UPLOAD_URL = "https://api.imgur.com/3/image";
    
    private CustomProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe_step);
        
        // Initialize UI components
        viewPager = findViewById(R.id.step_view_pager);
        progressIndicator = findViewById(R.id.progress_indicator);
        stepIndicator = findViewById(R.id.step_indicator);
        btnPrevious = findViewById(R.id.btn_previous);
        btnNext = findViewById(R.id.btn_next);
        
        progressDialog = new CustomProgressDialog(this);
        
        // Set up ViewPager with fragments
        RecipeStepAdapter adapter = new RecipeStepAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); // Disable swiping between pages
        
        // Set up progress indicator
        progressIndicator.setMax(TOTAL_STEPS);
        updateProgressIndicator(1);
        
        // Set up button listeners
        btnPrevious.setOnClickListener(v -> goToPreviousStep());
        btnNext.setOnClickListener(v -> goToNextStep());
        
        // Set up ViewPager page change listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateButtonVisibility(position);
                updateProgressIndicator(position + 1);
            }
        });
    }
    
    private void updateButtonVisibility(int position) {
        if (position == 0) {
            btnPrevious.setVisibility(View.GONE);
        } else {
            btnPrevious.setVisibility(View.VISIBLE);
        }
        
        if (position == TOTAL_STEPS - 1) {
            btnNext.setText("Save Recipe");
        } else {
            btnNext.setText("Next");
        }
    }
    
    private void updateProgressIndicator(int step) {
        progressIndicator.setProgress(step);
        stepIndicator.setText("Step " + step + " of " + TOTAL_STEPS);
    }
    
    private void goToPreviousStep() {
        int currentItem = viewPager.getCurrentItem();
        if (currentItem > 0) {
            viewPager.setCurrentItem(currentItem - 1);
        }
    }
    
    private void goToNextStep() {
        int currentItem = viewPager.getCurrentItem();
        if (currentItem < TOTAL_STEPS - 1) {
            viewPager.setCurrentItem(currentItem + 1);
        } else {
            // On last step, save the recipe
            saveRecipe();
        }
    }
    
    private void saveRecipe() {
        progressDialog.show();
        
        // Create a timestamp for the recipe
        long timestamp = System.currentTimeMillis();
        
        // Prepare recipe data
        Map<String, Object> recipeData = new HashMap<>();
        recipeData.put("recipeName", recipeName);
        recipeData.put("prepTime", prepTime);
        recipeData.put("cookTime", cookTime);
        recipeData.put("servingSize", servingSize);
        recipeData.put("timestamp", timestamp);
        recipeData.put("favorite", addToFavorites);
        
        // Add ingredients organized by categories
        Map<String, List<String>> ingredientsMap = new HashMap<>();
        
        // Group ingredients by category
        for (RecipeIngredient ingredient : ingredients) {
            String category = ingredient.getCategory();
            if (!ingredientsMap.containsKey(category)) {
                ingredientsMap.put(category, new ArrayList<>());
            }
            ingredientsMap.get(category).add(ingredient.toString());
        }
        
        recipeData.put("ingredients", ingredientsMap);
        
        // Add instructions - ensure proper format for Firestore
        List<Map<String, Object>> instructionsList = new ArrayList<>();
        
        // Enhanced logging for instructions list
        Log.d(TAG, "Processing " + instructions.size() + " instructions from user input");
        
        // Check if instructions list is empty and provide a default instruction if needed
        if (instructions.isEmpty()) {
            Log.w(TAG, "No instructions provided - adding a default instruction");
            InstructionStep defaultStep = new InstructionStep(1, "No specific instructions provided for this recipe.");
            instructions.add(defaultStep);
        }
        
        for (int i = 0; i < instructions.size(); i++) {
            InstructionStep step = instructions.get(i);
            Map<String, Object> stepMap = new HashMap<>();
            stepMap.put("stepNumber", i + 1);
            
            // Ensure instruction text is not null or empty
            String instructionText = step.getInstruction();
            if (instructionText == null || instructionText.trim().isEmpty()) {
                instructionText = "Step " + (i+1) + " - No details provided";
                Log.w(TAG, "Empty instruction at step " + (i+1) + " - using default text");
            }
            
            stepMap.put("instruction", instructionText);
            
            // Detailed logging of each instruction
            Log.d(TAG, "Instruction " + (i+1) + ": " + instructionText);
            
            instructionsList.add(stepMap);
        }
        
        // Log instructions list size
        Log.d(TAG, "Total instructions processed: " + instructionsList.size());
        
        recipeData.put("instructions", instructionsList);
        
        // Upload image if available
        if (recipeImageUri != null) {
            uploadImageToImgur(recipeImageUri, recipeData);
        } else {
            // No image, save recipe directly
            recipeData.put("photoUrl", ""); // Add empty photo URL to avoid null issues
            saveRecipeToFirestore(recipeData);
        }
    }
    
    private void uploadImageToImgur(Uri imageUri, Map<String, Object> recipeData) {
        try {
            File imageFile = getFileFromUri(imageUri);
            
            // Create a background thread for image upload
            new Thread(() -> {
                try {
                    // Create request body
                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("image", imageFile.getName(),
                                    RequestBody.create(MediaType.parse("image/*"), imageFile))
                            .build();
                    
                    // Create request
                    Request request = new Request.Builder()
                            .url(IMGUR_UPLOAD_URL)
                            .header("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                            .post(requestBody)
                            .build();
                    
                    // Execute request
                    OkHttpClient client = new OkHttpClient();
                    Response response = client.newCall(request).execute();
                    
                    // Parse response
                    String responseBody = response.body().string();
                    String imageUrl = extractImageUrl(responseBody);
                    
                    // Add image URL to recipe data and save to Firestore
                    recipeData.put("photoUrl", imageUrl);
                    runOnUiThread(() -> saveRecipeToFirestore(recipeData));
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error uploading image: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(AddRecipeStepActivity.this, 
                                "Failed to upload image. Saving recipe without image.", 
                                Toast.LENGTH_SHORT).show();
                        saveRecipeToFirestore(recipeData);
                    });
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "Error preparing image for upload: " + e.getMessage());
            Toast.makeText(this, "Failed to prepare image. Saving recipe without image.", 
                    Toast.LENGTH_SHORT).show();
            saveRecipeToFirestore(recipeData);
        }
    }
    
    private String extractImageUrl(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject data = jsonObject.getJSONObject("data");
            return data.getString("link");
        } catch (Exception e) {
            Log.e(TAG, "Error extracting image URL: " + e.getMessage());
            return null;
        }
    }
    
    private File getFileFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File tempFile = File.createTempFile("upload", ".jpg", getCacheDir());
        tempFile.deleteOnExit();
        
        OutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        
        outputStream.close();
        inputStream.close();
        
        return tempFile;
    }
    
    private void saveRecipeToFirestore(Map<String, Object> recipeData) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // First add the recipe to the recipes collection
        db.collection("recipes")
                .add(recipeData)
                .addOnSuccessListener(documentReference -> {
                    String recipeId = documentReference.getId();
                    
                    // Add recipeId to the data for reference
                    recipeData.put("recipeId", recipeId);
                    
                    // Log successful save for debugging
                    Log.d(TAG, "Recipe saved successfully with ID: " + recipeId);
                    
                    // If addToFavorites is checked, also add to user_favorites collection
                    if (addToFavorites) {
                        addRecipeToFavorites(db, recipeId, recipeData);
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Recipe saved successfully!", Toast.LENGTH_SHORT).show();
                        // Open the recipe detail screen with the new recipe ID
                        openRecipeDetail(recipeId, recipeData);
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error saving recipe: " + e.getMessage());
                    Toast.makeText(this, "Failed to save recipe. Please try again.", 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    private void addRecipeToFavorites(FirebaseFirestore db, String recipeId, Map<String, Object> recipeData) {
        // Get current user ID
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (userId == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Recipe saved but couldn't add to favorites: User not logged in", 
                    Toast.LENGTH_LONG).show();
            // Open recipe detail anyway
            openRecipeDetail(recipeId, recipeData);
            return;
        }
        
        // We need to use the document ID from the recipes collection as the document ID in the favorites collection
        // This way we maintain a direct reference and avoid duplicate entries
        db.collection("user_favorites")
                .document(userId)
                .collection("recipes")
                .document(recipeId)  // Use the same document ID
                .set(recipeData)     // Use set instead of add to ensure we don't create duplicates
                .addOnSuccessListener(aVoid -> {
                    // Log successful addition to favorites for debugging
                    Log.d(TAG, "Recipe successfully added to favorites with ID: " + recipeId);
                    progressDialog.dismiss();
                    Toast.makeText(this, "Recipe saved and added to favorites!", Toast.LENGTH_SHORT).show();
                    // Open recipe detail
                    openRecipeDetail(recipeId, recipeData);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding to favorites: " + e.getMessage(), e);
                    progressDialog.dismiss();
                    Toast.makeText(this, "Recipe saved but couldn't add to favorites", 
                            Toast.LENGTH_SHORT).show();
                    // Open recipe detail anyway
                    openRecipeDetail(recipeId, recipeData);
                });
    }
    
    // New method to open the Recipe Detail Activity
    private void openRecipeDetail(String recipeId, Map<String, Object> recipeData) {
        try {
            // Log the original recipeData for debugging
            Log.d(TAG, "Recipe data keys: " + recipeData.keySet());
            
            // Check instructions before creating Recipe object
            List<Map<String, Object>> instructionsList = (List<Map<String, Object>>) recipeData.get("instructions");
            
            // Enhanced debugging for instructions
            if (instructionsList != null) {
                Log.d(TAG, "Instructions list size: " + instructionsList.size());
                
                // Verify structure of each instruction
                boolean allValid = true;
                
                // Log each instruction
                for (int i = 0; i < instructionsList.size(); i++) {
                    Map<String, Object> step = instructionsList.get(i);
                    Log.d(TAG, "Step " + (i+1) + ": " + step);
                    
                    // Verify each instruction has the required fields
                    if (step.containsKey("stepNumber") && step.containsKey("instruction")) {
                        Object stepNumber = step.get("stepNumber");
                        Object instruction = step.get("instruction");
                        
                        // Check for null values or empty strings
                        if (instruction == null || (instruction instanceof String && ((String)instruction).isEmpty())) {
                            Log.w(TAG, "Instruction text is null or empty at step " + (i+1));
                            step.put("instruction", "Step " + (i+1) + " - No details provided");
                            allValid = false;
                        } else {
                            Log.d(TAG, "Valid instruction at step " + stepNumber + ": " + instruction);
                        }
                    } else {
                        Log.w(TAG, "Missing required fields in step " + (i+1) + ". Available keys: " + step.keySet());
                        
                        // Fix the step by adding missing fields
                        if (!step.containsKey("stepNumber")) {
                            step.put("stepNumber", i + 1);
                        }
                        if (!step.containsKey("instruction")) {
                            step.put("instruction", "Step " + (i+1) + " - No details provided");
                        }
                        
                        allValid = false;
                    }
                }
                
                if (!allValid) {
                    Log.d(TAG, "Some instructions were fixed automatically");
                }
            } else {
                Log.w(TAG, "Instructions list is null in recipeData - creating default");
                
                // Create a default instruction if none exists
                instructionsList = new ArrayList<>();
                Map<String, Object> defaultStep = new HashMap<>();
                defaultStep.put("stepNumber", 1);
                defaultStep.put("instruction", "No specific instructions provided for this recipe.");
                instructionsList.add(defaultStep);
                
                // Update recipeData with the default instruction
                recipeData.put("instructions", instructionsList);
            }
            
            // Create a Recipe object from the saved data
            Recipe recipe = new Recipe();
            recipe.setRecipeId(recipeId);
            recipe.setRecipeName((String) recipeData.get("recipeName"));
            recipe.setCookTime((String) recipeData.get("cookTime"));
            recipe.setPhotoUrl((String) recipeData.get("photoUrl"));
            recipe.setTimestamp((Long) recipeData.get("timestamp"));
            recipe.setFavorite((Boolean) recipeData.getOrDefault("favorite", false));
            
            // Handle ingredients map
            Map<String, List<String>> ingredientsMap = (Map<String, List<String>>) recipeData.get("ingredients");
            recipe.setIngredients(ingredientsMap);
            
            // Handle instructions list - now with explicit logging
            recipe.setInstructions(instructionsList);
            
            // Verify Recipe object has instructions after setting
            if (recipe.getInstructions() != null) {
                Log.d(TAG, "Recipe instructions size after setting: " + recipe.getInstructions().size());
                Log.d(TAG, "First instruction: " + (recipe.getInstructions().get(0)));
            } else {
                Log.w(TAG, "Recipe instructions is null after setting!");
                
                // If instructions are null in Recipe but exist in recipeData, create a manual copy
                if (instructionsList != null && !instructionsList.isEmpty()) {
                    Log.d(TAG, "Manually creating instructions list for Recipe object");
                    
                    // Create a deep copy of the instructions to avoid reference issues
                    List<Map<String, Object>> safeInstructionsList = new ArrayList<>();
                    
                    for (Map<String, Object> originalStep : instructionsList) {
                        Map<String, Object> newStep = new HashMap<>();
                        
                        // Copy the original step data, ensuring the critical fields exist
                        for (Map.Entry<String, Object> entry : originalStep.entrySet()) {
                            if (entry.getValue() != null) {
                                newStep.put(entry.getKey(), entry.getValue());
                            }
                        }
                        
                        // Ensure required fields are present
                        if (!newStep.containsKey("stepNumber")) {
                            newStep.put("stepNumber", safeInstructionsList.size() + 1);
                        }
                        if (!newStep.containsKey("instruction") || newStep.get("instruction") == null) {
                            newStep.put("instruction", "Step " + newStep.get("stepNumber") + " - No details provided");
                        }
                        
                        safeInstructionsList.add(newStep);
                    }
                    
                    // Update with the safe copy
                    recipe.setInstructions(safeInstructionsList);
                    
                    // Verify Recipe object has instructions after setting
                    if (recipe.getInstructions() != null) {
                        Log.d(TAG, "Recipe instructions size after setting: " + recipe.getInstructions().size());
                        Log.d(TAG, "First instruction: " + (recipe.getInstructions().get(0)));
                    } else {
                        Log.w(TAG, "Recipe instructions is still null after manual setting!");
                    }
                } else {
                    Log.w(TAG, "Instructions list is empty - setting default instruction in recipe");
                    
                    // Create a default instruction
                    List<Map<String, Object>> defaultInstructions = new ArrayList<>();
                    Map<String, Object> defaultStep = new HashMap<>();
                    defaultStep.put("stepNumber", 1);
                    defaultStep.put("instruction", "No specific instructions provided for this recipe.");
                    defaultInstructions.add(defaultStep);
                    
                    recipe.setInstructions(defaultInstructions);
                }
            }
            
            // Start RecipeDetailActivity with the new recipe
            Intent intent = new Intent(this, RecipeDetailActivity.class);
            
            // Pass either the Recipe object as Parcelable or just the ID
            intent.putExtra("RECIPE", recipe);  // Pass full recipe object
            intent.putExtra("RECIPE_ID", recipeId);  // Also pass ID separately
            
            // Add animation for smooth transition
            android.app.ActivityOptions options = android.app.ActivityOptions
                .makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left);
            
            startActivity(intent, options.toBundle());
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error opening recipe detail: " + e.getMessage(), e);
            e.printStackTrace();
            // Fallback to older redirect method
            redirectToRecipeList(addToFavorites);
        }
    }
    
    // Keep the old method for backward compatibility
    private void redirectToRecipeList(boolean showFavorites) {
        // Create intent to redirect to DashboardActivity
        Intent intent = new Intent(this, edu.prakriti.mealmate.home.DashboardActivity.class);
        
        // Add flags to show the Recipe List fragment or Favorite Recipes fragment
        intent.putExtra("OPEN_RECIPE_LIST", true);
        intent.putExtra("SHOW_FAVORITES", showFavorites);
        
        // Add extra toast message based on where we're redirecting
        if (showFavorites) {
            Toast.makeText(this, "Recipe added to favorites!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Recipe added successfully!", Toast.LENGTH_SHORT).show();
        }
        
        // Clear the back stack so the user can't go back to this activity
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        
        startActivity(intent);
        finish();
    }
    
    // Implement fragment interfaces for data communication
    @Override
    public void onBasicInfoProvided(String name, String prepTime, String cookTime, String servingSize, Uri imageUri) {
        this.recipeName = name;
        this.prepTime = prepTime;
        this.cookTime = cookTime;
        this.servingSize = servingSize;
        this.recipeImageUri = imageUri;
    }
    
    @Override
    public void onIngredientsProvided(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients;
    }
    
    @Override
    public void onInstructionsProvided(List<InstructionStep> instructions, boolean addToFavorites) {
        this.instructions = instructions;
        this.addToFavorites = addToFavorites;
    }
    
    // Adapter for ViewPager
    private class RecipeStepAdapter extends FragmentStateAdapter {
        
        public RecipeStepAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new RecipeStepBasicFragment();
                case 1:
                    return new RecipeStepIngredientsFragment();
                case 2:
                    return new RecipeStepInstructionsFragment();
                default:
                    return new RecipeStepBasicFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return TOTAL_STEPS;
        }
    }
} 