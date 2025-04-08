package edu.prakriti.mealmate.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.RecipeDetailActivity;
import edu.prakriti.mealmate.home.DashboardActivity;
import edu.prakriti.mealmate.models.Notification;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notificationList;
    private Context context;
    private FirebaseFirestore db;

    public NotificationAdapter(List<Notification> notificationList, Context context) {
        this.notificationList = notificationList;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        try {
            Notification notification = notificationList.get(position);
            Log.d("NotificationAdapter", "Binding notification: " + notification.getTitle());
            
            // Set text content
            holder.title.setText(notification.getTitle());
            holder.message.setText(notification.getMessage());
            holder.time.setText(notification.getRelativeTime());
            
            // Set read/unread status
            if (notification.isRead()) {
                holder.card.setCardBackgroundColor(context.getResources().getColor(R.color.white));
                holder.unreadIndicator.setVisibility(View.GONE);
            } else {
                holder.card.setCardBackgroundColor(context.getResources().getColor(R.color.light_primary));
                holder.unreadIndicator.setVisibility(View.VISIBLE);
            }
            
            // Set icon based on notification type
            if (notification.getTitle().contains("Reminder")) {
                holder.icon.setImageResource(R.drawable.ic_bell);
            } else {
                holder.icon.setImageResource(R.drawable.ic_info);
            }
            
            // Set click listener
            holder.card.setOnClickListener(v -> {
                // Mark as read if not already
                if (!notification.isRead()) {
                    markAsRead(notification);
                }
                
                // Navigate based on notification content
                if (notification.getRecipeId() != null && !notification.getRecipeId().isEmpty()) {
                    openRecipeDetail(notification);
                } else if (notification.getDay() != null && notification.getMealType() != null) {
                    openMealPlan();
                }
            });
        } catch (Exception e) {
            Log.e("NotificationAdapter", "Error binding view holder: " + e.getMessage(), e);
        }
    }
    
    private void markAsRead(Notification notification) {
        db.collection("notifications")
            .document(notification.getId())
            .update("read", true)
            .addOnSuccessListener(aVoid -> {
                notification.setRead(true);
                notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                Log.e("NotificationAdapter", "Error marking notification as read: " + e.getMessage());
            });
    }
    
    private void openRecipeDetail(Notification notification) {
        // Open RecipeDetailActivity directly using recipeId
        if (notification.getRecipeId() != null && !notification.getRecipeId().isEmpty()) {
            Intent intent = new Intent(context, RecipeDetailActivity.class);
            
            // Pass the recipe ID - this will cause the activity to fetch the latest data
            intent.putExtra("RECIPE_ID", notification.getRecipeId());
            
            // If recipe name is available, pass it for display while loading
            if (notification.getRecipeName() != null && !notification.getRecipeName().isEmpty()) {
                // Create a minimal Recipe object with just the essential info
                edu.prakriti.mealmate.model.Recipe minimalRecipe = new edu.prakriti.mealmate.model.Recipe();
                minimalRecipe.setRecipeId(notification.getRecipeId());
                minimalRecipe.setRecipeName(notification.getRecipeName());
                intent.putExtra("RECIPE", minimalRecipe);
            }
            
            Log.d("NotificationAdapter", "Opening recipe detail with ID: " + notification.getRecipeId());
            
            // Add transition animation
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                context.startActivity(intent);
            }
        } else {
            Log.e("NotificationAdapter", "Cannot open recipe: No recipeId provided");
            Toast.makeText(context, "Cannot open recipe: Details not available", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openMealPlan() {
        // Open meal plan fragment in dashboard
        Intent intent = new Intent(context, DashboardActivity.class);
        intent.putExtra("FRAGMENT_INDEX", 1); // Index for MealPlanFragment
        
        // Add transition animation
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            android.app.ActivityOptions options = android.app.ActivityOptions
                .makeCustomAnimation(context, R.anim.slide_in_right, R.anim.slide_out_left);
            activity.startActivity(intent, options.toBundle());
        } else {
            context.startActivity(intent);
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, time;
        ImageView icon, unreadIndicator;
        MaterialCardView card;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notification_title);
            message = itemView.findViewById(R.id.notification_message);
            time = itemView.findViewById(R.id.notification_time);
            icon = itemView.findViewById(R.id.notification_icon);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
            card = itemView.findViewById(R.id.notification_card);
        }
    }
} 