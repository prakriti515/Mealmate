package edu.prakriti.mealmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.model.RecipeIngredient;

public class RecipeIngredientAdapter extends RecyclerView.Adapter<RecipeIngredientAdapter.IngredientViewHolder> {

    private List<RecipeIngredient> ingredients;
    private OnIngredientDeleteListener deleteListener;

    public interface OnIngredientDeleteListener {
        void onIngredientDelete(int position);
    }

    public RecipeIngredientAdapter(List<RecipeIngredient> ingredients, OnIngredientDeleteListener deleteListener) {
        this.ingredients = ingredients;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ingredient, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        RecipeIngredient ingredient = ingredients.get(position);
        holder.nameTextView.setText(ingredient.getName());
        holder.quantityTextView.setText(ingredient.getQuantity() + " " + ingredient.getUnit());
        
        // Set price text
        if (ingredient.getPrice() > 0) {
            holder.priceTextView.setVisibility(View.VISIBLE);
            holder.priceTextView.setText("$" + String.format("%.2f", ingredient.getPrice()));
        } else {
            holder.priceTextView.setVisibility(View.GONE);
        }

        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onIngredientDelete(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    static class IngredientViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView quantityTextView;
        TextView priceTextView;
        ImageButton deleteButton;

        public IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.tv_ingredient_name);
            quantityTextView = itemView.findViewById(R.id.tv_ingredient_quantity);
            priceTextView = itemView.findViewById(R.id.tv_ingredient_price);
            deleteButton = itemView.findViewById(R.id.btn_delete_ingredient);
        }
    }
} 