package edu.prakriti.mealmate.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.adapters.NotificationAdapter;
import edu.prakriti.mealmate.models.Notification;

public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";
    
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private ProgressBar loadingProgressBar;
    private TextView emptyView;
    private FirebaseFirestore db;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating NotificationsFragment view");
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        
        try {
            // Initialize Firebase
            db = FirebaseFirestore.getInstance();
            userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                    FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
            Log.d(TAG, "onCreateView: User ID: " + userId);
            
            // Set up RecyclerView
            recyclerView = view.findViewById(R.id.notifications_recycler_view);
            if (recyclerView == null) {
                Log.e(TAG, "RecyclerView not found in layout");
                Toast.makeText(getContext(), "Error: RecyclerView not found", Toast.LENGTH_SHORT).show();
                return view;
            }
            
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            
            // Initialize loading indicator and empty view
            loadingProgressBar = view.findViewById(R.id.loading_progress);
            emptyView = view.findViewById(R.id.empty_view);
            
            if (loadingProgressBar == null) {
                Log.e(TAG, "Loading progress bar not found in layout");
            }
            
            if (emptyView == null) {
                Log.e(TAG, "Empty view not found in layout");
            }
            
            // Initialize notification list and adapter
            notificationList = new ArrayList<>();
            adapter = new NotificationAdapter(notificationList, getContext());
            recyclerView.setAdapter(adapter);
            
            // Add divider for spacing between items
            recyclerView.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(
                    getContext(), LinearLayoutManager.VERTICAL));
            
            // Load notifications
            loadNotifications();
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up NotificationsFragment: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: NotificationsFragment resumed");
        // Reload notifications when fragment resumes
        if (recyclerView != null && adapter != null) {
            loadNotifications();
        }
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
        if (getView() != null) {
            Snackbar.make(getView(), errorMessage, Snackbar.LENGTH_LONG).show();
        }
    }
    
    public void markAllAsRead() {
        if (userId == null || notificationList.isEmpty()) return;
        
        for (Notification notification : notificationList) {
            if (!notification.isRead()) {
                db.collection("notifications")
                    .document(notification.getId())
                    .update("read", true)
                    .addOnSuccessListener(aVoid -> {
                        notification.setRead(true);
                        adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error marking notification as read: " + e.getMessage());
                    });
            }
        }
    }
} 