package edu.prakriti.mealmate.adapters;

import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.prakriti.mealmate.R;

public class DelegateIngredientAdapter extends RecyclerView.Adapter<DelegateIngredientAdapter.IngredientViewHolder> {
    private List<String> ingredientList;
    private Map<String, Boolean> checkedItems;
    private Map<String, Float> itemPrices;
    private Map<String, Float> itemQuantities;
    private Map<String, String> itemUnits;
    private static final String[] UNITS = new String[]{"pcs", "kg", "g", "L", "ml", "cup", "tbsp", "tsp"};

    public DelegateIngredientAdapter(List<String> ingredientList) {
        this.ingredientList = ingredientList;
        this.checkedItems = new HashMap<>();
        this.itemPrices = new HashMap<>();
        this.itemQuantities = new HashMap<>();
        this.itemUnits = new HashMap<>();
        
        for (String ingredient : ingredientList) {
            // Remove the purchased status from the ingredient name if it exists
            String ingredientName = ingredient.contains("|") ? ingredient.split("\\|")[0] : ingredient;
            checkedItems.put(ingredientName, false);
            itemPrices.put(ingredientName, 0.0f);
            itemQuantities.put(ingredientName, 1.0f);
            itemUnits.put(ingredientName, "pcs");
        }
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.delegate_item_ingredient, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        String ingredient = ingredientList.get(position);
        boolean isPurchased = ingredient.endsWith("|true");

        // Remove the purchased status from the ingredient name
        String ingredientName = ingredient.contains("|") ? ingredient.split("\\|")[0] : ingredient;

        // Set ingredient name
        holder.textViewIngredientName.setText(ingredientName);

        // Set checkbox state based on checked status
        holder.checkBoxIngredient.setChecked(checkedItems.getOrDefault(ingredientName, false));

        // Set price, quantity and unit
        if (itemPrices.containsKey(ingredientName)) {
            holder.editTextPrice.setText(itemPrices.get(ingredientName) > 0 ? String.valueOf(itemPrices.get(ingredientName)) : "");
        }
        
        if (itemQuantities.containsKey(ingredientName)) {
            holder.editTextQuantity.setText(itemQuantities.get(ingredientName) > 0 ? String.valueOf(itemQuantities.get(ingredientName)) : "");
        }
        
        // Set up unit spinner
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
            holder.spinnerUnit.getContext(),
            android.R.layout.simple_dropdown_item_1line,
            UNITS
        );
        holder.spinnerUnit.setAdapter(unitAdapter);
        
        if (itemUnits.containsKey(ingredientName)) {
            holder.spinnerUnit.setText(itemUnits.get(ingredientName), false);
        } else {
            holder.spinnerUnit.setText(UNITS[0], false);
        }

        // Strikethrough text if purchased
        if (isPurchased) {
            holder.textViewIngredientName.setPaintFlags(holder.textViewIngredientName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.checkBoxIngredient.setEnabled(false); // Disable checkbox for purchased items
        } else {
            holder.textViewIngredientName.setPaintFlags(holder.textViewIngredientName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.checkBoxIngredient.setEnabled(true); // Enable checkbox for unpurchased items
        }

        // Handle checkbox click
        holder.checkBoxIngredient.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkedItems.put(ingredientName, isChecked);
        });

        // Handle price changes
        holder.editTextPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    float price = s.length() > 0 ? Float.parseFloat(s.toString()) : 0;
                    itemPrices.put(ingredientName, price);
                } catch (NumberFormatException e) {
                    itemPrices.put(ingredientName, 0.0f);
                }
            }
        });

        // Handle quantity changes
        holder.editTextQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    float quantity = s.length() > 0 ? Float.parseFloat(s.toString()) : 0;
                    itemQuantities.put(ingredientName, quantity);
                } catch (NumberFormatException e) {
                    itemQuantities.put(ingredientName, 1.0f);
                }
            }
        });

        // Handle unit changes
        holder.spinnerUnit.setOnItemClickListener((parent, view, pos, id) -> {
            itemUnits.put(ingredientName, UNITS[pos]);
        });
    }

    @Override
    public int getItemCount() {
        return ingredientList.size();
    }

    public List<String> getIngredientList() {
        return ingredientList;
    }

    public List<String> getCheckedIngredients() {
        List<String> checked = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : checkedItems.entrySet()) {
            if (entry.getValue()) {
                checked.add(entry.getKey());
            }
        }
        return checked;
    }

    public Map<String, Float> getItemPrices() {
        return itemPrices;
    }

    public Map<String, Float> getItemQuantities() {
        return itemQuantities;
    }

    public Map<String, String> getItemUnits() {
        return itemUnits;
    }

    public void updateIngredients(List<String> newIngredients) {
        this.ingredientList.clear();
        this.ingredientList.addAll(newIngredients);
        
        // Update checked items map
        for (String ingredient : newIngredients) {
            String ingredientName = ingredient.contains("|") ? ingredient.split("\\|")[0] : ingredient;
            if (!checkedItems.containsKey(ingredientName)) {
                checkedItems.put(ingredientName, false);
                itemPrices.put(ingredientName, 0.0f);
                itemQuantities.put(ingredientName, 1.0f);
                itemUnits.put(ingredientName, "pcs");
            }
        }
        
        notifyDataSetChanged(); // Notify the adapter that the data has changed
    }

    static class IngredientViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBoxIngredient;
        TextView textViewIngredientName;
        TextInputEditText editTextPrice;
        TextInputEditText editTextQuantity;
        AutoCompleteTextView spinnerUnit;

        IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBoxIngredient = itemView.findViewById(R.id.checkBoxIngredient);
            textViewIngredientName = itemView.findViewById(R.id.textViewIngredientName);
            editTextPrice = itemView.findViewById(R.id.editTextPrice);
            editTextQuantity = itemView.findViewById(R.id.editTextQuantity);
            spinnerUnit = itemView.findViewById(R.id.spinnerUnit);
        }
    }
}