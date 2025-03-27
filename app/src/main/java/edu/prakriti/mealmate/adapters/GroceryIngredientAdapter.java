package edu.prakriti.mealmate.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.textview.MaterialTextView;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.models.GroceryIngredient;
import edu.prakriti.mealmate.utils.GroceryDatabaseHelper;

public class GroceryIngredientAdapter extends RecyclerView.Adapter<GroceryIngredientAdapter.ViewHolder> {
    private final Context context;
    private List<GroceryIngredient> ingredients;
    private final String date;
    private final GroceryDatabaseHelper dbHelper;
    private OnItemCheckListener onItemCheckListener;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private RecyclerView recyclerView;
    
    // Flag to track when operations are in progress
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    
    // Currency formatter for price display
    private NumberFormat currencyFormatter;

    /**
     * Interface for handling item check events
     */
    public interface OnItemCheckListener {
        /**
         * Called when an item's checked state changes
         * @param ingredient The ingredient that was checked/unchecked
         * @param isChecked The new checked state
         */
        void onItemChecked(GroceryIngredient ingredient, boolean isChecked);
        
        /**
         * Called when any item in the list is modified
         */
        void onItemCheckedChange();
    }

    public GroceryIngredientAdapter(Context context, List<GroceryIngredient> ingredients, String date, GroceryDatabaseHelper dbHelper, OnItemCheckListener listener) {
        this.context = context;
        this.ingredients = ingredients;
        this.date = date;
        this.dbHelper = dbHelper;
        this.onItemCheckListener = listener;
        
        // Setup currency formatter
        currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        currencyFormatter.setCurrency(Currency.getInstance("USD"));
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }
    
    // Method to check if the adapter is currently updating
    public boolean isUpdating() {
        return isUpdating.get();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.grocery_item_ingredient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroceryIngredient ingredient = ingredients.get(position);
        
        // Set the ingredient name
        holder.nameText.setText(ingredient.getName());
        
        // Set the category chip text
        holder.categoryChip.setText(ingredient.getCategory());
        
        // Format and set price
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
        format.setCurrency(Currency.getInstance("NPR"));
        holder.priceText.setText(format.format(ingredient.getPrice()));
        
        // Set checkbox state without triggering listener
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(ingredient.isPurchased());
        
        // Apply strikethrough text style if purchased
        if (ingredient.isPurchased()) {
            holder.nameText.setPaintFlags(holder.nameText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.nameText.setPaintFlags(holder.nameText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
        
        // Set checkbox listener
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Update the model
            ingredient.setPurchased(isChecked);
            
            // Update the UI
            if (isChecked) {
                holder.nameText.setPaintFlags(holder.nameText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                holder.nameText.setPaintFlags(holder.nameText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }
            
            // Update the database
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                // Create a flag to track completion
                AtomicBoolean completedFlag = new AtomicBoolean(false);
                
                // Update in the database
                dbHelper.updateGroceryIngredient(ingredient);
                completedFlag.set(true);
                
                // Notify listener on the main thread after database update is complete
                if (onItemCheckListener != null) {
                    ((Activity) context).runOnUiThread(() -> {
                        onItemCheckListener.onItemChecked(ingredient, isChecked);
                    });
                }
            });
            executor.shutdown();
        });
        
        // Set item click listener to show edit dialog
        holder.itemView.setOnClickListener(v -> {
            showEditDialog(ingredient, position);
        });
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    public void updateData(List<GroceryIngredient> newIngredients) {
        isUpdating.set(true);
        this.ingredients = newIngredients;
        notifyDataSetChanged();
        isUpdating.set(false);
    }

    public void setOnItemCheckListener(OnItemCheckListener listener) {
        this.onItemCheckListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        MaterialTextView nameText;
        MaterialTextView priceText;
        Chip categoryChip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.ingredientCheckbox);
            nameText = itemView.findViewById(R.id.ingredientName);
            priceText = itemView.findViewById(R.id.ingredientPrice);
            categoryChip = itemView.findViewById(R.id.categoryChip);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        executorService.shutdown();
    }

    // Add method to delete item at position
    public void deleteItem(int position) {
        if (position >= 0 && position < ingredients.size()) {
            GroceryIngredient itemToDelete = ingredients.get(position);
            
            // Set updating flag
            isUpdating.set(true);
            
            // Remove from database in background thread
            executorService.execute(() -> {
                try {
                    dbHelper.deleteGroceryItem(itemToDelete.getName(), date);
                    
                    // Update UI on main thread with proper context handling
                    if (context instanceof Activity && !((Activity) context).isFinishing() && !((Activity) context).isDestroyed()) {
                        ((Activity) context).runOnUiThread(() -> {
                            try {
                                // Check if position is still valid
                                if (position < ingredients.size()) {
                                    // Remove from list
                                    ingredients.remove(position);
                                    // Notify adapter
                                    notifyItemRemoved(position);
                                    // Update parent if listener is set
                                    if (onItemCheckListener != null) {
                                        onItemCheckListener.onItemChecked(itemToDelete, false);
                                    }
                                } else {
                                    // If position is no longer valid, refresh all data
                                    notifyDataSetChanged();
                                }
                            } catch (Exception e) {
                                // Log error but prevent crash
                                e.printStackTrace();
                                notifyDataSetChanged();
                            } finally {
                                // Reset updating flag
                                isUpdating.set(false);
                            }
                        });
                    } else {
                        isUpdating.set(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    isUpdating.set(false);
                }
            });
        }
    }

    // Add method to mark item as purchased at position
    public void togglePurchaseStatus(int position) {
        if (position >= 0 && position < ingredients.size()) {
            GroceryIngredient itemName = ingredients.get(position);
            // Get the current status
            boolean currentStatus = itemName.isPurchased();
            // Toggle the status
            boolean newStatus = !currentStatus;
            
            // Set updating flag
            isUpdating.set(true);
            
            // Update in database
            executorService.execute(() -> {
                try {
                    dbHelper.updateIngredientPurchasedStatus(itemName.getId(), newStatus);
                    
                    // Update UI on main thread with proper context handling
                    if (context instanceof Activity && !((Activity) context).isFinishing() && !((Activity) context).isDestroyed()) {
                        ((Activity) context).runOnUiThread(() -> {
                            try {
                                // Check if position is still valid
                                if (position < ingredients.size()) {
                                    // Update the model
                                    ingredients.get(position).setPurchased(newStatus);
                                    // Notify adapter
                                    notifyItemChanged(position);
                                    // Update parent if listener is set
                                    if (onItemCheckListener != null) {
                                        onItemCheckListener.onItemChecked(itemName, newStatus);
                                    }
                                } else {
                                    // If position is no longer valid, refresh all data
                                    notifyDataSetChanged();
                                }
                            } catch (Exception e) {
                                // Log error but prevent crash
                                e.printStackTrace();
                                notifyDataSetChanged();
                            } finally {
                                // Reset updating flag
                                isUpdating.set(false);
                            }
                        });
                    } else {
                        isUpdating.set(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    isUpdating.set(false);
                }
            });
        }
    }
    
    // Add method to handle swipe in either direction
    public void handleSwipe(int position, int direction) {
        try {
            if (isUpdating.get()) {
                // If already updating, just refresh the view and return
                notifyDataSetChanged();
                return;
            }
            
            if (androidx.recyclerview.widget.ItemTouchHelper.LEFT == direction) {
                // Swipe left to delete
                deleteItem(position);
            } else if (androidx.recyclerview.widget.ItemTouchHelper.RIGHT == direction) {
                // Swipe right to toggle purchase status
                togglePurchaseStatus(position);
            } else {
                // Unknown direction, just refresh the view
                notifyDataSetChanged();
            }
        } catch (Exception e) {
            // If any error occurs, reset the adapter state
            e.printStackTrace();
            notifyDataSetChanged();
        }
    }

    // Get item name at position
    public String getItemName(int position) {
        if (position >= 0 && position < ingredients.size()) {
            return ingredients.get(position).getName();
        }
        return "";
    }

    // Method to show edit dialog for a grocery item
    private void showEditDialog(GroceryIngredient ingredient, int position) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_grocery_item, null);
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setView(dialogView);

        // Get references to all views in the dialog
        com.google.android.material.textfield.TextInputEditText nameEditText = dialogView.findViewById(R.id.edit_name);
        com.google.android.material.textfield.TextInputEditText categoryEditText = dialogView.findViewById(R.id.edit_category);
        com.google.android.material.textfield.TextInputEditText quantityEditText = dialogView.findViewById(R.id.edit_quantity);
        AutoCompleteTextView unitDropdown = dialogView.findViewById(R.id.edit_unit);
        com.google.android.material.textfield.TextInputEditText priceEditText = dialogView.findViewById(R.id.edit_price);
        com.google.android.material.checkbox.MaterialCheckBox purchasedCheckbox = dialogView.findViewById(R.id.edit_purchased);

        // Create array adapter for units dropdown
        String[] units = context.getResources().getStringArray(R.array.ingredient_units);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, units);
        unitDropdown.setAdapter(adapter);

        // Set current values
        nameEditText.setText(ingredient.getName());
        categoryEditText.setText(ingredient.getCategory());
        quantityEditText.setText(String.valueOf(ingredient.getQuantity()));
        unitDropdown.setText(ingredient.getUnit(), false);
        priceEditText.setText(String.valueOf(ingredient.getPrice()));
        purchasedCheckbox.setChecked(ingredient.isPurchased());

        // Set up dialog buttons
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        dialogView.findViewById(R.id.cancel_button).setOnClickListener(v -> dialog.dismiss());
        
        dialogView.findViewById(R.id.save_button).setOnClickListener(v -> {
            // Validate and save the data
            String name = nameEditText.getText().toString().trim();
            String category = categoryEditText.getText().toString().trim();
            String quantityStr = quantityEditText.getText().toString().trim();
            String unit = unitDropdown.getText().toString().trim();
            String priceStr = priceEditText.getText().toString().trim();
            boolean purchased = purchasedCheckbox.isChecked();
            
            if (name.isEmpty() || category.isEmpty() || quantityStr.isEmpty() || unit.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                double quantity = Double.parseDouble(quantityStr);
                double price = Double.parseDouble(priceStr);
                
                // Update the ingredient
                ingredient.setName(name);
                ingredient.setCategory(category);
                ingredient.setQuantity(quantity);
                ingredient.setUnit(unit);
                ingredient.setPrice(price);
                ingredient.setPurchased(purchased);
                
                // Update database
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    dbHelper.updateGroceryIngredient(ingredient);
                    // Update UI on main thread
                    ((Activity) context).runOnUiThread(() -> {
                        notifyItemChanged(position);
                        if (onItemCheckListener != null) {
                            onItemCheckListener.onItemCheckedChange();
                        }
                    });
                });
                executor.shutdown();
                
                dialog.dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid number format", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }
}