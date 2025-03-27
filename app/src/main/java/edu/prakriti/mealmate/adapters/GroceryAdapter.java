package edu.prakriti.mealmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.models.GroceryIngredient;
import edu.prakriti.mealmate.utils.GroceryDatabaseHelper;

public class GroceryAdapter extends RecyclerView.Adapter<GroceryAdapter.GroceryViewHolder> {

    private final Context context;
    private final GroceryDatabaseHelper dbHelper;
    private final Map<String, List<GroceryIngredient>> groceryMap;
    private final String date;
    private final String selectedTab;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private OnIngredientAdapterCreatedListener onIngredientAdapterCreatedListener;

    public interface OnIngredientAdapterCreatedListener {
        void onIngredientAdapterCreated(GroceryIngredientAdapter adapter, String category);
    }

    public void setOnIngredientAdapterCreatedListener(OnIngredientAdapterCreatedListener listener) {
        this.onIngredientAdapterCreatedListener = listener;
    }

    public GroceryAdapter(Context context, Map<String, List<GroceryIngredient>> groceryMap, GroceryDatabaseHelper dbHelper, 
                         String date, String selectedTab) {
        this.context = context;
        this.groceryMap = groceryMap;
        this.dbHelper = dbHelper;
        this.date = date;
        this.selectedTab = selectedTab;
    }

    @NonNull
    @Override
    public GroceryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grocery, parent, false);
        return new GroceryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroceryViewHolder holder, int position) {
        String category = (String) groceryMap.keySet().toArray()[position];
        List<GroceryIngredient> items = groceryMap.get(category);

        int totalItems = items.size();
        int purchasedItems = 0;
        double totalCost = 0.0;

        for (GroceryIngredient ingredient : items) {
            if (ingredient.isPurchased()) {
                purchasedItems++;
            }
            totalCost += ingredient.getPrice();
        }

        // Update Category Title with Purchased/Total and total cost
        String costInfo = String.format(Locale.US, " - Total: $%.2f", totalCost);
        holder.categoryTitle.setText(category + " (" + purchasedItems + "/" + totalItems + " Purchased)" + costInfo);

        // Set up the nested RecyclerView
        GroceryIngredientAdapter groceryIngredientAdapter = new GroceryIngredientAdapter(
            context, items, date, dbHelper, new GroceryIngredientAdapter.OnItemCheckListener() {
                @Override
                public void onItemChecked(GroceryIngredient ingredient, boolean isChecked) {
                    notifyItemChanged(position);
                }
                
                @Override
                public void onItemCheckedChange() {
                    notifyItemChanged(position);
                }
            });
        holder.ingredientRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.ingredientRecyclerView.setAdapter(groceryIngredientAdapter);
        
        // Assign RecyclerView to the adapter for swipe functionality
        groceryIngredientAdapter.setRecyclerView(holder.ingredientRecyclerView);
        
        // Notify the listener about the new ingredient adapter
        if (onIngredientAdapterCreatedListener != null) {
            onIngredientAdapterCreatedListener.onIngredientAdapterCreated(groceryIngredientAdapter, category);
        }

        // Calculate and display category progress
        int progress = totalItems > 0 ? (int) ((purchasedItems / (float) totalItems) * 100) : 0;
        holder.categoryProgress.setProgress(progress);
        holder.progressText.setText(progress + "% Purchased");
    }

    @Override
    public int getItemCount() {
        return groceryMap.size();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        executorService.shutdown();
    }

    static class GroceryViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView categoryTitle;
        RecyclerView ingredientRecyclerView;
        CircularProgressIndicator categoryProgress;
        TextView progressText;

        GroceryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTitle = itemView.findViewById(R.id.mealTitle);
            ingredientRecyclerView = itemView.findViewById(R.id.ingredientRecyclerView);
            categoryProgress = itemView.findViewById(R.id.mealProgressIndicator);
            progressText = itemView.findViewById(R.id.mealProgressText);
        }
    }
}