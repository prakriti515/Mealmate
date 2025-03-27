package edu.prakriti.mealmate.grok;

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
import java.util.Arrays;
import java.util.List;
import edu.prakriti.mealmate.R;

public class GrokIngredientAdapter extends RecyclerView.Adapter<GrokIngredientAdapter.IngredientViewHolder> {
    private final List<GrokIngredient> ingredients;
    private final List<GrokIngredient> selectedIngredients;
    private static final String[] UNITS = new String[]{"pcs", "kg", "g", "L", "ml", "cup", "tbsp", "tsp"};

    public GrokIngredientAdapter(List<GrokIngredient> ingredients, List<GrokIngredient> selectedIngredients) {
        this.ingredients = ingredients;
        this.selectedIngredients = selectedIngredients;
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.delegate_item_ingredient, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        GrokIngredient ingredient = ingredients.get(position);
        holder.textViewIngredientName.setText(ingredient.name);
        holder.checkBoxIngredient.setChecked(selectedIngredients.contains(ingredient));
        holder.editTextPrice.setText(ingredient.price > 0 ? String.valueOf(ingredient.price) : "");
        holder.editTextQuantity.setText(ingredient.quantity > 0 ? String.valueOf(ingredient.quantity) : "");
        
        // Set up unit spinner
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
            holder.spinnerUnit.getContext(),
            android.R.layout.simple_dropdown_item_1line,
            UNITS
        );
        holder.spinnerUnit.setAdapter(unitAdapter);
        holder.spinnerUnit.setText(ingredient.unit, false);

        // Handle checkbox changes
        holder.checkBoxIngredient.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedIngredients.contains(ingredient)) {
                    selectedIngredients.add(ingredient);
                }
            } else {
                selectedIngredients.remove(ingredient);
            }
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
                    ingredient.price = s.length() > 0 ? Float.parseFloat(s.toString()) : 0;
                } catch (NumberFormatException e) {
                    ingredient.price = 0;
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
                    ingredient.quantity = s.length() > 0 ? Float.parseFloat(s.toString()) : 0;
                } catch (NumberFormatException e) {
                    ingredient.quantity = 0;
                }
            }
        });

        // Handle unit changes
        holder.spinnerUnit.setOnItemClickListener((parent, view, pos, id) -> {
            ingredient.unit = UNITS[pos];
        });
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    static class IngredientViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewIngredientName;
        final CheckBox checkBoxIngredient;
        final TextInputEditText editTextPrice;
        final TextInputEditText editTextQuantity;
        final AutoCompleteTextView spinnerUnit;

        IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewIngredientName = itemView.findViewById(R.id.textViewIngredientName);
            checkBoxIngredient = itemView.findViewById(R.id.checkBoxIngredient);
            editTextPrice = itemView.findViewById(R.id.editTextPrice);
            editTextQuantity = itemView.findViewById(R.id.editTextQuantity);
            spinnerUnit = itemView.findViewById(R.id.spinnerUnit);
        }
    }
}