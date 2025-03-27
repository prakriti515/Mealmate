package edu.prakriti.mealmate;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import edu.prakriti.mealmate.adapters.NotificationAdapter;
import edu.prakriti.mealmate.models.Notification;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import edu.prakriti.mealmate.utils.NotificationHelper;

public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationsActivity";
    
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private ProgressBar loadingProgressBar;
    private TextView emptyView;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        
        Log.d(TAG, "onCreate: Creating NotificationsActivity");
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Notifications");
        
        try {
            // Initialize Firebase
            db = FirebaseFirestore.getInstance();
            userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                    FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
            Log.d(TAG, "onCreate: User ID: " + userId);
            
            // Set up RecyclerView
            recyclerView = findViewById(R.id.notifications_recycler_view);
            if (recyclerView == null) {
                Log.e(TAG, "RecyclerView not found in layout");
                Toast.makeText(this, "Error: RecyclerView not found", Toast.LENGTH_SHORT).show();
                return;
            }
            
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            
            // Initialize loading indicator and empty view
            loadingProgressBar = findViewById(R.id.loading_progress);
            emptyView = findViewById(R.id.empty_view);
            
            if (loadingProgressBar == null) {
                Log.e(TAG, "Loading progress bar not found in layout");
            }
            
            if (emptyView == null) {
                Log.e(TAG, "Empty view not found in layout");
            }
            
            // Initialize notification list and adapter
            notificationList = new ArrayList<>();
            adapter = new NotificationAdapter(notificationList, this);
            recyclerView.setAdapter(adapter);
            
            // Add divider for spacing between items
            recyclerView.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(
                    this, LinearLayoutManager.VERTICAL));
            
            // Load notifications
            loadNotifications();
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up NotificationsActivity: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.notifications_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            // Apply custom animation when going back
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        } else if (id == R.id.action_create_test_notification) {
            showCreateTestNotificationDialog();
            return true;
        } else if (id == R.id.action_mark_all_as_read) {
            markAllAsRead();
            return true;
        } else if (id == R.id.action_clear_all) {
            clearAllNotifications();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Apply custom animation when going back
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
    
    private void loadNotifications() {
        if (userId == null) {
            Log.e(TAG, "Cannot load notifications: User ID is null");
            showEmptyView("You need to be logged in to view notifications");
            return;
        }
        
        Log.d(TAG, "loadNotifications: Loading notifications for user: " + userId);
        showLoading(true);
        
        // Add a debug notification for testing
        Notification testNotification = new Notification();
        testNotification.setId("test_notification");
        testNotification.setTitle("Test Notification");
        testNotification.setMessage("This is a test notification to check if the recycler view is working");
        testNotification.setTimestamp(System.currentTimeMillis());
        testNotification.setRead(false);
        
        notificationList.clear();
        notificationList.add(testNotification);
        adapter.notifyDataSetChanged();
        
        // After showing the debug notification, try to load from Firestore
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "loadNotifications: Successfully queried Firestore");
                    
                    // Clear the test notification if we get real data
                    if (!task.getResult().isEmpty()) {
                        notificationList.clear();
                    }
                    
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        try {
                            Log.d(TAG, "Processing notification document: " + document.getId());
                            
                            Notification notification = new Notification();
                            notification.setId(document.getId());
                            notification.setTitle(document.getString("title"));
                            notification.setMessage(document.getString("message"));
                            notification.setTimestamp(document.getLong("timestamp"));
                            notification.setRead(document.getBoolean("read"));
                            notification.setRecipeId(document.getString("recipeId"));
                            notification.setRecipeName(document.getString("recipeName"));
                            notification.setDay(document.getString("day"));
                            notification.setMealType(document.getString("mealType"));
                            
                            notificationList.add(notification);
                            Log.d(TAG, "Added notification: " + notification.getTitle());
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing notification: " + e.getMessage(), e);
                        }
                    }
                    
                    // Update RecyclerView
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "Notifications loaded: " + notificationList.size());
                    
                    showLoading(false);
                    
                    // Show empty view if no notifications (but keep the test one if it exists)
                    if (notificationList.isEmpty()) {
                        showEmptyView("No notifications available");
                    } else {
                        showEmptyView(false);
                    }
                    
                } else {
                    Log.e(TAG, "Error getting notifications: ", task.getException());
                    showLoading(false);
                    showError("Error loading notifications. Please try again.");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load notifications: " + e.getMessage(), e);
                showLoading(false);
                showError("Failed to load notifications: " + e.getMessage());
            });
    }
    
    private void showLoading(boolean isLoading) {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }
    
    private void showEmptyView(boolean isEmpty) {
        if (emptyView != null) {
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }
    
    private void showEmptyView(String message) {
        if (emptyView != null) {
            emptyView.setText(message);
            emptyView.setVisibility(View.VISIBLE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
    }
    
    private void showError(String errorMessage) {
        Snackbar.make(findViewById(android.R.id.content), errorMessage, Snackbar.LENGTH_LONG).show();
    }

    private void showCreateTestNotificationDialog() {
        final String[] notificationTypes = {
                "Recipe Shared", 
                "Meal Reminder", 
                "Grocery Reminder", 
                "System Notification"
        };
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Create Test Notification")
                .setItems(notificationTypes, (dialog, which) -> {
                    String type;
                    switch (which) {
                        case 0:
                            type = NotificationHelper.TYPE_RECIPE_SHARED;
                            break;
                        case 1:
                            type = NotificationHelper.TYPE_MEAL_REMINDER;
                            break;
                        case 2:
                            type = NotificationHelper.TYPE_GROCERY_REMINDER;
                            break;
                        case 3:
                        default:
                            type = NotificationHelper.TYPE_SYSTEM;
                            break;
                    }
                    createTestNotification(type);
                })
                .show();
    }
    
    private void createTestNotification(String type) {
        // Show a loading indicator
        Toast.makeText(this, "Creating test notification...", Toast.LENGTH_SHORT).show();
        
        // Create the notification in a background thread
        new Thread(() -> {
            boolean success = NotificationHelper.createTestNotification(type);
            
            // Update UI on main thread
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(NotificationsActivity.this, 
                            "Test notification created", Toast.LENGTH_SHORT).show();
                    // Reload notifications
                    loadNotifications();
                } else {
                    Toast.makeText(NotificationsActivity.this, 
                            "Failed to create test notification", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    private void markAllAsRead() {
        if (userId == null || notificationList.isEmpty()) {
            Toast.makeText(this, "No notifications to mark as read", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showLoading(true);
        
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            document.getReference().update("read", true);
                            count++;
                        }
                        
                        // Update local data
                        for (Notification notification : notificationList) {
                            notification.setRead(true);
                        }
                        adapter.notifyDataSetChanged();
                        
                        showLoading(false);
                        Toast.makeText(this, count + " notifications marked as read", 
                                Toast.LENGTH_SHORT).show();
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Failed to mark notifications as read", 
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error marking notifications as read", task.getException());
                    }
                });
    }
    
    private void clearAllNotifications() {
        if (userId == null || notificationList.isEmpty()) {
            Toast.makeText(this, "No notifications to clear", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear All Notifications")
                .setMessage("Are you sure you want to delete all notifications? This cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    showLoading(true);
                    
                    db.collection("notifications")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    int count = 0;
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        document.getReference().delete();
                                        count++;
                                    }
                                    
                                    // Clear local data
                                    notificationList.clear();
                                    adapter.notifyDataSetChanged();
                                    
                                    showLoading(false);
                                    showEmptyView("No notifications available");
                                    
                                    Toast.makeText(this, count + " notifications cleared", 
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    showLoading(false);
                                    Toast.makeText(this, "Failed to clear notifications", 
                                            Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Error clearing notifications", task.getException());
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
} 