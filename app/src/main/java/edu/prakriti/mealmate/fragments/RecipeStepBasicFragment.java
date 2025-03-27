package edu.prakriti.mealmate.fragments;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;

import edu.prakriti.mealmate.R;

public class RecipeStepBasicFragment extends Fragment {
    
    private TextInputEditText etRecipeName, etPrepTime, etCookTime, etServingSize;
    private ImageView ivRecipeImage;
    private Button btnUploadImage;
    private Uri recipeImageUri;
    private Uri cameraImageUri;
    
    private BasicInfoListener listener;
    
    public interface BasicInfoListener {
        void onBasicInfoProvided(String name, String prepTime, String cookTime, String servingSize, Uri imageUri);
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BasicInfoListener) {
            listener = (BasicInfoListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement BasicInfoListener");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe_step_basic, container, false);
        
        // Initialize views
        etRecipeName = view.findViewById(R.id.et_recipe_name);
        etPrepTime = view.findViewById(R.id.et_prep_time);
        etCookTime = view.findViewById(R.id.et_cook_time);
        etServingSize = view.findViewById(R.id.et_serving_size);
        ivRecipeImage = view.findViewById(R.id.iv_recipe_image);
        btnUploadImage = view.findViewById(R.id.btn_upload_image);
        
        // Set up image upload button
        btnUploadImage.setOnClickListener(v -> showImagePickerDialog());
        
        return view;
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Pass data to activity when fragment is paused
        provideBasicInfo();
    }
    
    private void provideBasicInfo() {
        String name = etRecipeName.getText() != null ? etRecipeName.getText().toString() : "";
        String prepTime = etPrepTime.getText() != null ? etPrepTime.getText().toString() : "";
        String cookTime = etCookTime.getText() != null ? etCookTime.getText().toString() : "";
        String servingSize = etServingSize.getText() != null ? etServingSize.getText().toString() : "";
        
        listener.onBasicInfoProvided(name, prepTime, cookTime, servingSize, recipeImageUri);
    }
    
    private void showImagePickerDialog() {
        Intent pickGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickGalleryIntent.setType("image/*");
        
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraImageUri = requireActivity().getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        
        Intent chooser = Intent.createChooser(pickGalleryIntent, "Select or Capture Image");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});
        imagePickerLauncher.launch(chooser);
    }
    
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getData() != null) { // Picked from Gallery
                        recipeImageUri = data.getData();
                    } else { // Captured from Camera
                        recipeImageUri = cameraImageUri;
                    }
                    
                    if (recipeImageUri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    requireActivity().getContentResolver(), recipeImageUri);
                            ivRecipeImage.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error loading image.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
} 