package edu.prakriti.mealmate.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;
import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.model.SavedLocation;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    private final Context context;
    private  List<SavedLocation> storeList;

    public StoreAdapter(Context context, List<SavedLocation> storeList) {
        this.context = context;
        this.storeList = storeList;
    }

    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_store, parent, false);
        return new StoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        SavedLocation store = storeList.get(position);
        
        // Debug log
        Log.d("StoreAdapter", "Binding store at position " + position + ": " + store.getName());

        // Store Name, Address, and Distance
        holder.storeName.setText(store.getName());
        holder.storeDistance.setText(store.getDistance() + " away");
        holder.storeAddress.setText(store.getAddress());

        // Display Matching Count Badge
        int matchingCount = store.getMatchingCount();
        holder.matchingCount.setText(String.valueOf(matchingCount));

        // Load Image using Glide with error handling
        String imageUrl = store.getImageUrl();
        Log.d("StoreAdapter", "Loading image for " + store.getName() + ": " + imageUrl);
        
        try {
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.saved_store)
                .error(R.drawable.saved_store)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, 
                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                            boolean isFirstResource) {
                        Log.e("StoreAdapter", "Failed to load image: " + imageUrl, e);
                        return false; // Let Glide handle the error image
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, 
                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                            com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        Log.d("StoreAdapter", "Successfully loaded image: " + imageUrl);
                        return false; // Let Glide handle setting the image
                    }
                })
                .into(holder.storeImage);
        } catch (Exception e) {
            Log.e("StoreAdapter", "Error loading image for store: " + store.getName(), e);
            holder.storeImage.setImageResource(R.drawable.saved_store);
        }

        // Display Available Ingredients as Grid
        holder.gridContainer.removeAllViews(); // Clear previous views
        List<String> ingredients = store.getAvailableIngredients();
        List<String> matchedIngredients = store.getMatchedIngredients(); // Get matched list
        
        // Debug log ingredients
        Log.d("StoreAdapter", "Store " + store.getName() + " has " + 
            (ingredients != null ? ingredients.size() : 0) + " ingredients and " +
            (matchedIngredients != null ? matchedIngredients.size() : 0) + " matches");
        
        // Log individual ingredients for debugging
        if (ingredients != null && !ingredients.isEmpty()) {
            Log.d("StoreAdapter", "Ingredients for " + store.getName() + ":");
            for (int i = 0; i < ingredients.size(); i++) {
                String ingredient = ingredients.get(i);
                boolean isMatched = matchedIngredients != null && matchedIngredients.contains(ingredient);
                Log.d("StoreAdapter", "  " + i + ": " + ingredient + " (matched: " + isMatched + ")");
            }
        }
        
        // Check if there are any ingredients
        if (ingredients == null || ingredients.isEmpty()) {
            TextView noIngredientsText = new TextView(context);
            noIngredientsText.setText("No ingredients listed for this store");
            noIngredientsText.setPadding(24, 12, 24, 12);
            noIngredientsText.setTextSize(14);
            noIngredientsText.setTextColor(context.getColor(R.color.white));
            holder.gridContainer.addView(noIngredientsText);
            Log.d("StoreAdapter", "Added 'No ingredients' text view");
        } else {
            int addedCount = 0;
            for (String ingredient : ingredients) {
                TextView chip = new TextView(context);
                chip.setText(ingredient);
                chip.setPadding(24, 12, 24, 12);
                chip.setTextSize(12);
                
                // Check if the ingredient is in the matched list
                boolean isMatched = matchedIngredients != null && matchedIngredients.contains(ingredient);
                if (isMatched) {
                    chip.setBackground(context.getDrawable(R.drawable.chip_avilable)); // Matched -> Green
                    chip.setTextColor(context.getColor(R.color.white));
                    Log.d("StoreAdapter", "Added matched ingredient chip: " + ingredient);
                } else {
                    chip.setBackground(context.getDrawable(R.drawable.chip_background));
                    chip.setTextColor(context.getColor(R.color.white));
                    Log.d("StoreAdapter", "Added regular ingredient chip: " + ingredient);
                }

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.setMargins(8, 8, 8, 8);
                chip.setLayoutParams(params);

                holder.gridContainer.addView(chip);
                addedCount++;
            }
            Log.d("StoreAdapter", "Added " + addedCount + " ingredient chips to the grid");
        }

        // Set initial state of ingredients grid and icon
        holder.gridContainer.setVisibility(View.GONE);
        holder.expandIcon.setRotation(0f);

        // Toggle the grid visibility on clicking the ingredients card
        holder.ingredientsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (holder.gridContainer.getVisibility() == View.GONE) {
                    holder.gridContainer.setVisibility(View.VISIBLE);
                    holder.expandIcon.setRotation(180f);
                } else {
                    holder.gridContainer.setVisibility(View.GONE);
                    holder.expandIcon.setRotation(0f);
                }
            }
        });

        // Handle Get Directions Button
        holder.getDirectionsButton.setOnClickListener(v -> {
            String uri = "google.navigation:q=" + store.getLatitude() + "," + store.getLongitude() + "&mode=d";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");

            // Check if Google Maps is installed to handle the intent
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                String webUri = "https://www.google.com/maps/dir/?api=1&destination="
                        + store.getLatitude() + "," + store.getLongitude() + "&travelmode=driving";
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
                context.startActivity(webIntent);    }



        });
    }

    @Override
    public int getItemCount() {
        return storeList.size();
    }

    public void updateList(List<SavedLocation> newList) {
        Log.d("StoreAdapter", "Updating list with " + newList.size() + " items");
        this.storeList.clear();
        this.storeList.addAll(newList);
        notifyDataSetChanged();
    }

    public static class StoreViewHolder extends RecyclerView.ViewHolder {
        ImageView storeImage, expandIcon;
        TextView storeName, storeDistance, matchingCount, storeAddress;
        GridLayout gridContainer;
        Button getDirectionsButton;
        CardView ingredientsCard;

        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);
            storeImage = itemView.findViewById(R.id.storeImage);
            storeName = itemView.findViewById(R.id.storeName);
            storeAddress = itemView.findViewById(R.id.storeAddress);
            storeDistance = itemView.findViewById(R.id.storeDistance);
            matchingCount = itemView.findViewById(R.id.matchingCount);
            gridContainer = itemView.findViewById(R.id.ingredientsGrid);
            getDirectionsButton = itemView.findViewById(R.id.getDirectionsButton);
            // New views for collapsible ingredients section
            ingredientsCard = itemView.findViewById(R.id.ingredientsCard);
            expandIcon = itemView.findViewById(R.id.expandIcon);
        }
    }
}
