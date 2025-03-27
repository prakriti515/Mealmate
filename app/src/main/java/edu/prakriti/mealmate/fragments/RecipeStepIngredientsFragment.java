package edu.prakriti.mealmate.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.adapters.RecipeIngredientAdapter;
import edu.prakriti.mealmate.model.RecipeIngredient;
import edu.prakriti.mealmate.utils.IngredientConstants;

public class RecipeStepIngredientsFragment extends Fragment implements RecipeIngredientAdapter.OnIngredientDeleteListener {
    
    private RecyclerView rvIngredients;
    private TextInputEditText etIngredientName, etIngredientQuantity, etIngredientPrice;
    private AutoCompleteTextView actvUnit, actvCategory;
    private Button btnAddIngredient;
    
    private List<RecipeIngredient> ingredientsList = new ArrayList<>();
    private RecipeIngredientAdapter adapter;
    
    private IngredientsListener listener;
    
    public interface IngredientsListener {
        void onIngredientsProvided(List<RecipeIngredient> ingredients);
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof IngredientsListener) {
            listener = (IngredientsListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement IngredientsListener");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe_step_ingredients, container, false);
        
        // Initialize views
        rvIngredients = view.findViewById(R.id.rv_ingredients);
        etIngredientName = view.findViewById(R.id.et_ingredient_name);
        etIngredientQuantity = view.findViewById(R.id.et_ingredient_quantity);
        etIngredientPrice = view.findViewById(R.id.et_ingredient_price);
        actvUnit = view.findViewById(R.id.auto_complete_unit);
        actvCategory = view.findViewById(R.id.auto_complete_category);
        btnAddIngredient = view.findViewById(R.id.btn_add_ingredient);
        
        // Set up RecyclerView
        rvIngredients.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecipeIngredientAdapter(ingredientsList, this);
        rvIngredients.setAdapter(adapter);
        
        // Set up unit dropdown
        setupUnitDropdown();
        
        // Set up category dropdown
        setupCategoryDropdown();
        
        // Set up add ingredient button
        btnAddIngredient.setOnClickListener(v -> addIngredient());
        
        return view;
    }
    
    private void setupUnitDropdown() {
        String[] units = {"g", "kg", "ml", "L", "tsp", "tbsp", "cup", "oz", "lb", "pcs"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_dropdown_item_1line, units);
        actvUnit.setAdapter(adapter);
    }
    
    private void setupCategoryDropdown() {
        // Get categories from IngredientConstants
        List<String> categories = IngredientConstants.CATEGORY_LIST;
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_dropdown_item_1line, categories);
        actvCategory.setAdapter(adapter);
        
        // Set default selection
        if (!categories.isEmpty()) {
            actvCategory.setText(categories.get(0), false);
        }
    }
    
    private void addIngredient() {
        View view = getView();
        String name = etIngredientName.getText() != null ? etIngredientName.getText().toString().trim() : "";
        String quantityStr = etIngredientQuantity.getText() != null ? etIngredientQuantity.getText().toString().trim() : "";
        String unit = actvUnit.getText() != null ? actvUnit.getText().toString().trim() : "";
        String priceStr = etIngredientPrice.getText() != null ? etIngredientPrice.getText().toString().trim() : "";
        String category = actvCategory.getText() != null ? actvCategory.getText().toString().trim() : "";
        
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), "Please enter ingredient name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (TextUtils.isEmpty(quantityStr)) {
            Toast.makeText(getContext(), "Please enter ingredient quantity", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (TextUtils.isEmpty(unit)) {
            Toast.makeText(getContext(), "Please enter ingredient unit", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (TextUtils.isEmpty(category)) {
            // If no category is selected, use a default one
            category = IngredientConstants.CATEGORY_LIST.get(0);
        }
        
        double quantity = 0;
        try {
            quantity = Double.parseDouble(quantityStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
            return;
        }
        
        double price = 0;
        if (!TextUtils.isEmpty(priceStr)) {
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter a valid price", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // Create new ingredient and add to list
        RecipeIngredient ingredient = new RecipeIngredient(name, quantity, unit, price, category);
        ingredientsList.add(ingredient);
        adapter.notifyItemInserted(ingredientsList.size() - 1);
        
        // Clear input fields
        etIngredientName.setText("");
        etIngredientQuantity.setText("");
        actvUnit.setText("");
        etIngredientPrice.setText("");
        // Don't reset category as user might add multiple items to same category
        etIngredientName.requestFocus();
    }
    
    @Override
    public void onIngredientDelete(int position) {
        ingredientsList.remove(position);
        adapter.notifyItemRemoved(position);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Pass data to activity when fragment is paused
        if (listener != null) {
            listener.onIngredientsProvided(ingredientsList);
        }
    }
} 