package edu.prakriti.mealmate.home;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.prakriti.mealmate.CustomProgressDialog;
import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.adapters.CategoryExpandableListAdapter;
import edu.prakriti.mealmate.adapters.InstructionAdapter;
import edu.prakriti.mealmate.model.InstructionStep;
import edu.prakriti.mealmate.model.Recipe;
import edu.prakriti.mealmate.utils.CustomExpandableListView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecipeEditActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private InstructionAdapter instructionAdapter;
    private List<InstructionStep> instructionList;
    private Button addInstructionButton;
    private CustomExpandableListView expandableListView; // Use CustomExpandableListView
    private CategoryExpandableListAdapter expandableListAdapter;
    private List<String> categoryList = new ArrayList<>();
    private HashMap<String, List<String>> ingredientMap = new HashMap<>();
    private List<String> selectedIngredients = new ArrayList<>();

    private EditText newIngredientInput;
    private Button addIngredientButton;
    private final String othersCategory = "🆕 Others";

    private EditText recipeNameInput, cookTimeInput;

    private Uri cameraImageUri;
    private Uri selectedImageUri = null;

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final String IMGUR_CLIENT_ID = "6deebb69c6310e5";
    private static final String IMGUR_UPLOAD_URL = "https://api.imgur.com/3/image";

    ImageView recipeImage;

    private long timeStamp;


    private CustomProgressDialog progressDialog;
    Recipe recipe;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recipe_edit);

        recipe = getIntent().getParcelableExtra("RECIPE");


        recyclerView = findViewById(R.id.instructionRecyclerView);
        addInstructionButton = findViewById(R.id.addInstructionButton);
        expandableListView = findViewById(R.id.expandableListView); // Use CustomExpandableListView
        newIngredientInput = findViewById(R.id.newIngredientInput);
        addIngredientButton = findViewById(R.id.addIngredientButton);
        recipeNameInput = findViewById(R.id.recipeName);
        cookTimeInput = findViewById(R.id.cookTime);
        recipeImage = findViewById(R.id.recipeImage);
        Button saveRecipeButton = findViewById(R.id.saveRecipeButton);
        Button uploadImageButton = findViewById(R.id.uploadImageButton);
        progressDialog = new CustomProgressDialog(RecipeEditActivity.this);

        // Setup Expandable ListView for ingredients
        setupExpandableListView();

        // Setup RecyclerView for instructions
        setupRecyclerView();

        // ✅ If recipe is received, populate the UI



        // Handle Add Instruction button click
        addInstructionButton.setOnClickListener(v -> addInstruction());

        addIngredientButton.setOnClickListener(v -> {
            String newIngredient = newIngredientInput.getText().toString().trim();

            if (!newIngredient.isEmpty()) {
                // Check if "Others" category exists, if not, add it
                if (!categoryList.contains(othersCategory)) {
                    categoryList.add(othersCategory);
                    ingredientMap.put(othersCategory, new ArrayList<>());
                }

                String ingredientWithEmoji = "🆕 " + newIngredient;

                // Add new ingredient under "Others" and check it by default
                ingredientMap.get(othersCategory).add(ingredientWithEmoji);
                selectedIngredients.add(ingredientWithEmoji); // ✅ Auto-check it

                // Notify adapter and expand the "Others" category
                expandableListAdapter.notifyDataSetChanged();
                expandableListView.expandGroup(categoryList.indexOf(othersCategory));

                // Clear input field for next entry
                newIngredientInput.setText("");
            } else {
                showSnackbar("Please enter an ingredient!");
            }
        });
        uploadImageButton.setOnClickListener(v -> showImagePickerDialog());

        saveRecipeButton.setOnClickListener(v -> saveRecipe());

        if (recipe != null) {
            timeStamp = recipe.getTimestamp();
            setRecipeData(recipe);

        }

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("RECIPE", recipe); // Save the recipe object
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        recipe = savedInstanceState.getParcelable("RECIPE"); // Restore the recipe object
    }


    /**
     * Populates the UI with data from the received Recipe object.
     */
    private void setRecipeData(Recipe recipe) {
        if (recipe == null) {
            Log.e("RecipeEditActivity", "Cannot set recipe data - recipe is null");
            return;
        }
        
        Log.d("RecipeEditActivity", "Populating UI with recipe: " + recipe.getRecipeName() +
                ", ID: " + recipe.getRecipeId() +
                ", Timestamp: " + recipe.getTimestamp());

        // Set Recipe Name and Cook Time
        recipeNameInput.setText(recipe.getRecipeName());
        cookTimeInput.setText(recipe.getCookTime());

        // Load Recipe Image
        String imageUrl = recipe.getPhotoUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d("RecipeEditActivity", "Loading image from URL: " + imageUrl);
            
            // Clean up the URL if needed
            if (imageUrl.contains(" ")) {
                imageUrl = imageUrl.replace(" ", "%20");
                Log.d("RecipeEditActivity", "URL contained spaces, cleaned to: " + imageUrl);
            }
            
            // Check for imgur links that might be missing the full URL
            if (imageUrl.startsWith("i.imgur.com/")) {
                Log.d("RecipeEditActivity", "Fixing imgur URL format");
                imageUrl = "https://" + imageUrl;
            }
            
            // Handle edge cases like missing http/https prefix
            if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && 
                !imageUrl.startsWith("file://") && !imageUrl.startsWith("content://")) {
                Log.w("RecipeEditActivity", "Image URL missing protocol, adding https://");
                imageUrl = "https://" + imageUrl;
            }
            
            // Store the final URL
            final String finalImageUrl = imageUrl;
            
            try {
                Glide.with(this)
                    .load(finalImageUrl)
                    .placeholder(R.drawable.ic_men)
                    .error(R.drawable.input_background)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .timeout(15000) // 15 second timeout
                    .into(recipeImage);
            } catch (Exception e) {
                Log.e("RecipeEditActivity", "Error loading image with Glide", e);
                recipeImage.setImageResource(R.drawable.input_background);
            }
        } else {
            Log.d("RecipeEditActivity", "No image URL available");
            recipeImage.setImageResource(R.drawable.input_background);
        }

        // Load Selected Ingredients from Firestore Recipe Data
        if (recipe.getIngredients() != null) {
            Map<String, List<String>> recipeIngredients = recipe.getIngredients();
            selectedIngredients.clear();
            
            Log.d("RecipeEditActivity", "Loading " + recipeIngredients.size() + " ingredient categories");
            
            // For each category in the recipe's ingredients
            for (Map.Entry<String, List<String>> entry : recipeIngredients.entrySet()) {
                String category = entry.getKey();
                List<String> ingredients = entry.getValue();
                
                Log.d("RecipeEditActivity", "Processing category: " + category + 
                        " with " + (ingredients != null ? ingredients.size() : 0) + " ingredients");
                
                // Make sure the category exists in our categoryList
                if (!categoryList.contains(category)) {
                    categoryList.add(category);
                    ingredientMap.put(category, new ArrayList<>());
                    Log.d("RecipeEditActivity", "Added new category: " + category);
                }
                
                // Add the ingredients to the category and mark them as selected
                if (ingredients != null) {
                    for (String ingredient : ingredients) {
                        if (ingredient != null && !ingredient.trim().isEmpty()) {
                            // Only add if not already in the list
                            if (!ingredientMap.get(category).contains(ingredient)) {
                                ingredientMap.get(category).add(ingredient);
                                Log.d("RecipeEditActivity", "Added ingredient: " + ingredient + " to category: " + category);
                            }
                            
                            // Mark as selected
                            if (!selectedIngredients.contains(ingredient)) {
                                selectedIngredients.add(ingredient);
                            }
                        }
                    }
                }
            }
            
            // Notify adapter about the changes
            if (expandableListAdapter != null) {
                expandableListAdapter.notifyDataSetChanged();
                
                // Expand all categories that have selected ingredients
                for (int i = 0; i < categoryList.size(); i++) {
                    String category = categoryList.get(i);
                    boolean hasSelectedIngredients = false;
                    
                    List<String> categoryIngredients = ingredientMap.get(category);
                    if (categoryIngredients != null) {
                        for (String ingredient : categoryIngredients) {
                            if (selectedIngredients.contains(ingredient)) {
                                hasSelectedIngredients = true;
                                break;
                            }
                        }
                    }
                    
                    if (hasSelectedIngredients) {
                        expandableListView.expandGroup(i);
                    }
                }
            }
            
            Log.d("RecipeEditActivity", "Selected Ingredients Loaded: " + selectedIngredients.size());
        } else {
            Log.w("RecipeEditActivity", "Recipe has no ingredients");
        }
        
        // Load Instructions
        if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
            instructionList.clear();
            
            Log.d("RecipeEditActivity", "Loading " + recipe.getInstructions().size() + " instructions");
            
            for (Map<String, Object> instructionMap : recipe.getInstructions()) {
                // Get step number
                int stepNumber = 1;
                if (instructionMap.containsKey("stepNumber")) {
                    Object stepObj = instructionMap.get("stepNumber");
                    if (stepObj instanceof Long) {
                        stepNumber = ((Long) stepObj).intValue();
                    } else if (stepObj instanceof Integer) {
                        stepNumber = (Integer) stepObj;
                    } else if (stepObj instanceof String) {
                        try {
                            stepNumber = Integer.parseInt((String) stepObj);
                        } catch (NumberFormatException e) {
                            Log.w("RecipeEditActivity", "Invalid step number: " + stepObj);
                        }
                    }
                }
                
                // Get instruction text
                String instructionText = "";
                if (instructionMap.containsKey("instruction")) {
                    Object textObj = instructionMap.get("instruction");
                    if (textObj != null) {
                        instructionText = textObj.toString();
                    }
                }
                
                // Create instruction step object
                InstructionStep step = new InstructionStep(stepNumber, instructionText);
                instructionList.add(step);
                
                Log.d("RecipeEditActivity", "Added instruction step " + stepNumber + ": " + instructionText);
            }
            
            // Update the adapter
            if (instructionAdapter != null) {
                instructionAdapter.notifyDataSetChanged();
            }
            
            Log.d("RecipeEditActivity", "Instructions loaded: " + instructionList.size());
        } else {
            Log.w("RecipeEditActivity", "Recipe has no instructions");
            instructionList.clear();
            if (instructionAdapter != null) {
                instructionAdapter.notifyDataSetChanged();
            }
        }
    }





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

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
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
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            recipeImage.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            //Toast.makeText(getContext(), "Error loading image.", Toast.LENGTH_SHORT).show();
                                showSnackbar("Error loading image");
                        }
                    }
                }
            });


    private void saveRecipe() {
        // Validate recipe data
        String recipeName = recipeNameInput.getText().toString().trim();
        String cookTime = cookTimeInput.getText().toString().trim();

        if (recipeName.isEmpty()) {
            showSnackbar("Please enter a recipe name.");
            return;
        }

        if (cookTime.isEmpty()) {
            showSnackbar("Please enter cook time.");
            return;
        }

        if (selectedIngredients.isEmpty()) {
            showSnackbar("Please select at least one ingredient.");
            return;
        }

        if (instructionList.isEmpty()) {
            showSnackbar("Please add at least one instruction step.");
            return;
        }

        // Show progress dialog
        progressDialog.show();

        // Prepare recipe data
        Map<String, Object> recipeData = new HashMap<>();
        recipeData.put("recipeName", recipeName);
        recipeData.put("cookTime", cookTime);
        
        // Use existing timestamp for updates, or create a new one for new recipes
        if (recipe != null && recipe.getTimestamp() > 0) {
            recipeData.put("timestamp", recipe.getTimestamp());
        } else {
            recipeData.put("timestamp", System.currentTimeMillis());
        }

        // Add favorite status (preserve existing status if editing)
        if (recipe != null) {
            recipeData.put("favorite", recipe.isFavorite());
        } else {
            recipeData.put("favorite", false);
        }

        // Process ingredients - Group ingredients by category
        Map<String, List<String>> ingredientsMap = new HashMap<>();
        for (String category : categoryList) {
            List<String> categoryIngredients = new ArrayList<>();
            for (String ingredient : ingredientMap.get(category)) {
                if (selectedIngredients.contains(ingredient)) {
                    // Clean up ingredient text for storage (remove emoji prefixes if present)
                    String cleanIngredient = removeEmojis(ingredient).trim();
                    if (!cleanIngredient.isEmpty()) {
                        categoryIngredients.add(cleanIngredient);
                    }
                }
            }
            
            // Only add categories that have selected ingredients
            if (!categoryIngredients.isEmpty()) {
                // Clean up category name (remove emoji prefixes if present)
                String cleanCategory = removeEmojis(category).trim();
                if (cleanCategory.isEmpty()) {
                    cleanCategory = "Other";
                }
                ingredientsMap.put(cleanCategory, categoryIngredients);
            }
        }
        
        // Enhanced logging for ingredients map
        Log.d("RecipeEditActivity", "Processed ingredients: " + ingredientsMap.size() + " categories");
        for (Map.Entry<String, List<String>> entry : ingredientsMap.entrySet()) {
            Log.d("RecipeEditActivity", "Category: " + entry.getKey() + " with " + 
                    entry.getValue().size() + " ingredients");
        }
        
        recipeData.put("ingredients", ingredientsMap);

        // Process instructions - ensure proper format for Firestore
        List<Map<String, Object>> instructionsList = new ArrayList<>();
        
        // Enhanced logging for instructions list
        Log.d("RecipeEditActivity", "Processing " + instructionList.size() + " instructions");
        
        // Check if instructions list is empty and provide a default instruction if needed
        if (instructionList.isEmpty()) {
            Log.w("RecipeEditActivity", "No instructions provided - adding a default instruction");
            InstructionStep defaultStep = new InstructionStep(1, "No specific instructions provided for this recipe.");
            instructionList.add(defaultStep);
        }
        
        for (int i = 0; i < instructionList.size(); i++) {
            InstructionStep step = instructionList.get(i);
            Map<String, Object> stepMap = new HashMap<>();
            stepMap.put("stepNumber", i + 1); // Ensure steps are numbered correctly
            
            // Ensure instruction text is not null or empty
            String instructionText = step.getInstruction();
            if (instructionText == null || instructionText.trim().isEmpty()) {
                instructionText = "Step " + (i+1) + " - No details provided";
                Log.w("RecipeEditActivity", "Empty instruction at step " + (i+1) + " - using default text");
            }
            
            stepMap.put("instruction", instructionText);
            
            // Detailed logging of each instruction
            Log.d("RecipeEditActivity", "Instruction " + (i+1) + ": " + instructionText);
            
            instructionsList.add(stepMap);
        }
        
        // Log instructions list size
        Log.d("RecipeEditActivity", "Total instructions processed: " + instructionsList.size());
        
        recipeData.put("instructions", instructionsList);

        // Check if we're updating an existing recipe or creating a new one
        boolean isUpdating = (recipe != null && recipe.getRecipeId() != null && !recipe.getRecipeId().isEmpty());
        
        // Upload image or proceed with saving recipe
        if (selectedImageUri != null) {
            uploadImageToImgur(selectedImageUri, recipeData);
        } else if (recipe != null && recipe.getPhotoUrl() != null && !recipe.getPhotoUrl().isEmpty()) {
            // Keep existing image if no new one selected
            recipeData.put("photoUrl", recipe.getPhotoUrl());
            RecipeDataSend(recipe.getPhotoUrl(), recipeData);
        } else {
            // No image, save recipe directly with empty photoUrl
            recipeData.put("photoUrl", "");
            RecipeDataSend("", recipeData);
        }
    }

    void RecipeDataSend(String photoUrl, Map<String, Object> recipeData) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Add photo URL to recipe data
        recipeData.put("photoUrl", photoUrl);
        
        // Check if we're updating an existing recipe or creating a new one
        if (recipe != null && recipe.getRecipeId() != null && !recipe.getRecipeId().isEmpty()) {
            // Update existing recipe
            String recipeId = recipe.getRecipeId();
            Log.d("RecipeEditActivity", "Updating existing recipe with ID: " + recipeId);
            
            // Add recipeId to the data for consistency
            recipeData.put("recipeId", recipeId);
            
            // Update in the recipes collection
            db.collection("recipes")
                .document(recipeId)
                .update(recipeData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("RecipeEditActivity", "Recipe updated successfully in main collection");
                    
                    // If it's a favorite, also update in user_favorites
                    boolean isFavorite = (boolean) recipeData.getOrDefault("favorite", false);
                    if (isFavorite) {
                        updateFavoriteRecipe(db, recipeId, recipeData);
                    } else {
                        progressDialog.dismiss();
                        showSnackbar("Recipe updated successfully!");
                        navigateBack();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("RecipeEditActivity", "Error updating recipe", e);
                    showSnackbar("Failed to update recipe: " + e.getMessage());
                });
        } else {
            // Create new recipe
            Log.d("RecipeEditActivity", "Creating new recipe");
            db.collection("recipes")
                .add(recipeData)
                .addOnSuccessListener(documentReference -> {
                    String recipeId = documentReference.getId();
                    Log.d("RecipeEditActivity", "New recipe created with ID: " + recipeId);
                    
                    // Update the document with its own ID
                    documentReference.update("recipeId", recipeId)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("RecipeEditActivity", "Recipe ID field updated");
                        })
                        .addOnFailureListener(e -> {
                            Log.w("RecipeEditActivity", "Failed to update recipe ID field", e);
                        });
                    
                    // Check if favorite is set to true
                    boolean isFavorite = (boolean) recipeData.getOrDefault("favorite", false);
                    if (isFavorite) {
                        // Add recipeId to the data
                        recipeData.put("recipeId", recipeId);
                        addToFavorites(db, recipeId, recipeData);
                    } else {
                        progressDialog.dismiss();
                        showSnackbar("Recipe saved successfully!");
                        navigateBack();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("RecipeEditActivity", "Error saving recipe", e);
                    showSnackbar("Failed to save recipe: " + e.getMessage());
                });
        }
    }
    
    private void updateFavoriteRecipe(FirebaseFirestore db, String recipeId, Map<String, Object> recipeData) {
        // Add recipeId to the data for favorites reference
        recipeData.put("recipeId", recipeId);
        
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                
        if (userId == null) {
            progressDialog.dismiss();
            showSnackbar("Recipe updated but couldn't update favorite: User not logged in");
            navigateBack();
            return;
        }
        
        // Update in favorites collection
        db.collection("user_favorites")
                .document(userId)
                .collection("recipes")
                .document(recipeId)
                .update(recipeData)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    showSnackbar("Recipe and favorite updated successfully!");
                    navigateBack();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("RecipeEditActivity", "Error updating favorite recipe", e);
                    showSnackbar("Recipe updated but couldn't update favorite");
                    navigateBack();
                });
    }
    
    private void addToFavorites(FirebaseFirestore db, String recipeId, Map<String, Object> recipeData) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                
        if (userId == null) {
            progressDialog.dismiss();
            showSnackbar("Recipe saved but couldn't add to favorites: User not logged in");
            navigateBack();
            return;
        }
        
        db.collection("user_favorites")
                .document(userId)
                .collection("recipes")
                .document(recipeId)
                .set(recipeData)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    showSnackbar("Recipe saved and added to favorites!");
                    navigateBack();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("RecipeEditActivity", "Error adding to favorites", e);
                    showSnackbar("Recipe saved but couldn't add to favorites");
                    navigateBack();
                });
    }
    
    private void navigateBack() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void uploadImageToImgur(Uri imageUri, Map<String, Object> recipeData) {
        try {
            File imageFile = getFileFromUri(imageUri);
            if (imageFile == null) {
                // Fall back to proceeding without an image if file conversion fails
                Log.e("RecipeEditActivity", "Failed to convert image URI to file, continuing without image");
                recipeData.put("photoUrl", "");
                RecipeDataSend("", recipeData);
                return;
            }
            
            // Create a background thread for image upload
            new Thread(() -> {
                try {
                    Log.d("RecipeEditActivity", "Starting image upload to Imgur");
                    
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
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                            .build();
                            
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        // Get response body
                        String jsonResponse = response.body().string();
                        
                        Log.d("RecipeEditActivity", "Imgur response received: " + 
                               (jsonResponse.length() > 100 ? jsonResponse.substring(0, 100) + "..." : jsonResponse));
                        
                        // Extract image URL
                        String imageUrl = extractImageUrl(jsonResponse);
                        
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            // Run on UI thread
                            runOnUiThread(() -> {
                                Log.d("RecipeEditActivity", "Image uploaded successfully: " + imageUrl);
                                RecipeDataSend(imageUrl, recipeData);
                            });
                        } else {
                            // Failed to extract image URL
                            runOnUiThread(() -> {
                                Log.e("RecipeEditActivity", "Failed to extract image URL from response");
                                showSnackbar("Failed to upload image. Using default.");
                                RecipeDataSend("", recipeData);
                            });
                        }
                    } else {
                        // Upload failed
                        final String errorMessage = response.message();
                        runOnUiThread(() -> {
                            Log.e("RecipeEditActivity", "Image upload failed: " + errorMessage);
                            showSnackbar("Image upload failed: " + errorMessage);
                            RecipeDataSend("", recipeData);
                        });
                    }
                } catch (Exception e) {
                    // Handle any exceptions during upload
                    final String errorMessage = e.getMessage();
                    runOnUiThread(() -> {
                        Log.e("RecipeEditActivity", "Exception during image upload", e);
                        showSnackbar("Error uploading image: " + 
                                    (errorMessage != null ? errorMessage : "Unknown error"));
                        RecipeDataSend("", recipeData);
                    });
                }
            }).start();
        } catch (Exception e) {
            // Handle any exceptions during file preparation
            Log.e("RecipeEditActivity", "Exception preparing image for upload", e);
            showSnackbar("Error preparing image: " + e.getMessage());
            RecipeDataSend("", recipeData);
        }
    }

    private String extractImageUrl(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            
            // Check if the response has the success flag
            if (jsonObject.has("success") && jsonObject.getBoolean("success")) {
                JSONObject dataObject = jsonObject.getJSONObject("data");
                
                // Try different fields where the URL might be
                if (dataObject.has("link")) {
                    // This is the standard field
                    return dataObject.getString("link");
                } else if (dataObject.has("url")) {
                    // Alternative field name
                    return dataObject.getString("url");
                }
            } else if (jsonObject.has("data") && jsonObject.getJSONObject("data").has("error")) {
                // Log the error from Imgur
                String error = jsonObject.getJSONObject("data").getString("error");
                Log.e("RecipeEditActivity", "Imgur API error: " + error);
            }
        } catch (Exception e) {
            Log.e("RecipeEditActivity", "Error parsing JSON response", e);
        }
        return null;
    }

    private File getFileFromUri(Uri uri) {
        try {
            // Create temp file
            File outputFile = new File(getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
            
            // Open input stream from uri
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e("RecipeEditActivity", "Failed to open input stream from URI: " + uri);
                return null;
            }
            
            // Open output stream to temp file
            OutputStream outputStream = new FileOutputStream(outputFile);
            
            // Copy bytes
            byte[] buffer = new byte[4 * 1024]; // 4k buffer
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            
            // Close streams
            outputStream.flush();
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.e("RecipeEditActivity", "Error closing output stream", e);
            }
            
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e("RecipeEditActivity", "Error closing input stream", e);
            }
            
            // Check if file was created successfully
            if (outputFile.exists() && outputFile.length() > 0) {
                Log.d("RecipeEditActivity", "Image file created successfully: " + 
                        outputFile.getAbsolutePath() + " (" + outputFile.length() + " bytes)");
                return outputFile;
            } else {
                Log.e("RecipeEditActivity", "Failed to create image file or file is empty");
                return null;
            }
        } catch (Exception e) {
            Log.e("RecipeEditActivity", "Error creating file from URI", e);
            return null;
        }
    }

    private void setupExpandableListView() {
   // ✅ Ensure it's empty before loading

        // ✅ Predefined Categories & Ingredients (with emojis)
        categoryList.add("🥦 Vegetables");
        categoryList.add("🍎 Fruits");
        categoryList.add("🌾 Grains & Legumes");
        categoryList.add("🍗 Proteins");
        categoryList.add("🧀 Dairy");
        categoryList.add("🌿 Herbs & Spices");
        categoryList.add("🛢️ Oils & Condiments");
        categoryList.add("🆕 Others"); // ✅ Dynamic category for user-added ingredients

        ingredientMap.put("🥦 Vegetables", new ArrayList<>(List.of("🥕 Carrot", "🥦 Broccoli", "🌿 Spinach", "🍅 Tomato", "🧅 Onion", "🧄 Garlic", "🌶 Bell Pepper", "🥒 Zucchini", "🥬 Cabbage", "🥬 Kale", "🥗 Lettuce", "🥔 Cauliflower")));
        ingredientMap.put("🍎 Fruits", new ArrayList<>(List.of("🍏 Apple", "🍌 Banana", "🍊 Orange", "🍓 Strawberries", "🍇 Grapes", "🥭 Mango", "🍍 Pineapple", "🍋 Lemon/Lime")));
        ingredientMap.put("🌾 Grains & Legumes", new ArrayList<>(List.of("🍚 Rice", "🌾 Quinoa", "🥣 Oats", "🌰 Lentils", "🫘 Chickpeas", "🌽 Corn", "🥜 Peanuts")));
        ingredientMap.put("🍗 Proteins", new ArrayList<>(List.of("🍗 Chicken", "🥩 Beef", "🐖 Pork", "🐟 Fish", "🍳 Eggs", "🌱 Tofu", "🫘 Beans")));
        ingredientMap.put("🧀 Dairy", new ArrayList<>(List.of("🥛 Milk", "🍦 Yogurt", "🧀 Cheese", "🧈 Butter", "🥥 Coconut Milk", "🌱 Soy/Oat Milk")));
        ingredientMap.put("🌿 Herbs & Spices", new ArrayList<>(List.of("🌿 Basil", "🌿 Oregano", "🌿 Thyme", "🌿 Rosemary", "🧂 Salt", "🌶 Chili Powder", "🟠 Turmeric", "🟡 Ginger", "🟤 Cumin")));
        ingredientMap.put("🛢️ Oils & Condiments", new ArrayList<>(List.of("🫒 Olive Oil", "🥥 Coconut Oil", "🥫 Soy Sauce", "🔥 Hot Sauce", "🍯 Honey", "🥄 Mayonnaise", "🍶 Vinegar", "🧂 Salt", "🍬 Sugar")));
        ingredientMap.put("🆕 Others", new ArrayList<>()); // ✅ Allow dynamic user-added ingredients

        // ✅ Load Selected Ingredients from Recipe (if available)
        if (recipe != null && recipe.getIngredients() != null) {
            Log.d("checkrecipe", "Recipe Ingredients: " + recipe.getIngredients().toString());

            for (Map.Entry<String, List<String>> entry : recipe.getIngredients().entrySet()) {
                String category = entry.getKey(); // Category from recipe (with emojis)
                List<String> ingredients = entry.getValue(); // Ingredients from recipe (with emojis)

                // ✅ Find the corresponding category in ingredientMap
                if (ingredientMap.containsKey(category)) {
                    // ✅ Add ingredients with emojis to the selectedIngredients list
                    for (String ingredient : ingredients) {
                        String ingredientWithoutEmoji = removeEmojis(ingredient); // Remove emojis for comparison
                        String emojiIngredient = findIngredientWithEmoji(category, ingredientWithoutEmoji);
                        if (emojiIngredient != null) {
                            selectedIngredients.add(emojiIngredient);
                        }
                    }

                    // ✅ Ensure "Others" category captures dynamically added ingredients
                    if (category.equals("🆕 Others") && !ingredients.isEmpty()) {
                        for (String ingredient : ingredients) {
                            // ✅ Add only if the ingredient is not already in the "Others" category
                            if (!ingredientMap.get("🆕 Others").contains(ingredient)) {
                                ingredientMap.get("🆕 Others").add(ingredient);
                            }
                            // ✅ Mark the ingredient as selected
                            if (!selectedIngredients.contains(ingredient)) {
                                selectedIngredients.add(ingredient);
                            }
                        }
                    }
                }
            }
        }

        // ✅ Log selectedIngredients for debugging
        Log.d("SelectedIngredients", "Selected Ingredients: " + selectedIngredients.toString());

        // ✅ Initialize the ExpandableListAdapter
        expandableListAdapter = new CategoryExpandableListAdapter(RecipeEditActivity.this, categoryList, ingredientMap, selectedIngredients);

        // ✅ Set the adapter to the ExpandableListView
        expandableListView.setAdapter(expandableListAdapter);

        // ✅ Handle ingredient selection
        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            String selectedIngredient = ingredientMap.get(categoryList.get(groupPosition)).get(childPosition);

            if (!selectedIngredients.contains(selectedIngredient)) {
                selectedIngredients.add(selectedIngredient);
                showSnackbar("added!");
            } else {
                selectedIngredients.remove(selectedIngredient);
                showSnackbar("removed!");
            }

            // ✅ Notify the adapter of changes
            expandableListAdapter.notifyDataSetChanged();

            return true;
        });
    }

    // ✅ Helper method to find the ingredient with emojis
    private String findIngredientWithEmoji(String category, String ingredientWithoutEmoji) {
        List<String> ingredients = ingredientMap.get(category);
        if (ingredients != null) {
            for (String emojiIngredient : ingredients) {
                if (removeEmojis(emojiIngredient).equals(ingredientWithoutEmoji)) {
                    return emojiIngredient;
                }
            }
        }
        return null;
    }

    // ✅ Helper method to remove emojis from a string
    private String removeEmojis(String input) {
        return input.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "").trim();
    }
    /**
     * Sets up the RecyclerView for dynamic instructions.
     */
    private void setupRecyclerView() {
        instructionList = new ArrayList<>();
        instructionAdapter = new InstructionAdapter(instructionList, position -> removeInstruction(position));

        recyclerView.setLayoutManager(new LinearLayoutManager(RecipeEditActivity.this));
        recyclerView.setAdapter(instructionAdapter);
    }

    /**
     * Adds a new instruction step dynamically.
     */
    private void addInstruction() {
        instructionList.add(new InstructionStep(instructionList.size() + 1, ""));
        instructionAdapter.notifyItemInserted(instructionList.size() - 1);
    }

    /**
     * Removes an instruction step dynamically.
     */
    private void removeInstruction(int position) {
        instructionList.remove(position);
        instructionAdapter.notifyDataSetChanged();
    }



    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.primary_variant))
                .setTextColor(getResources().getColor(R.color.white))
                .show();
    }


}