package edu.prakriti.mealmate.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.util.Map;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.adapters.NotificationAdapter;
import edu.prakriti.mealmate.models.Notification;

public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";
    public static final String ACTION_RELOAD_NOTIFICATIONS = "edu.prakriti.mealmate.RELOAD_NOTIFICATIONS";
    
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private ProgressBar loadingProgressBar;
    private TextView emptyView;
    private FirebaseFirestore db;
    private String userId;
    private BroadcastReceiver notificationReloadReceiver;

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
            
            // Set up toolbar with refresh button
            setupToolbar();
            
            // Test Firestore connectivity
            testFirestoreConnectivity();
            
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
            
            // Set up debug button
            com.google.android.material.floatingactionbutton.FloatingActionButton debugButton = 
                view.findViewById(R.id.debug_add_notification_button);
            if (debugButton != null) {
                debugButton.setOnClickListener(v -> {
                    Log.d(TAG, "Debug button clicked - creating test notification");
                    createTestNotification();
                });
            }
            
            // Register broadcast receiver for reload notifications
            notificationReloadReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "Received broadcast to reload notifications");
                    if (isAdded() && !isDetached()) {
                        loadNotifications();
                    }
                }
            };
            
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
        
        // Register for reload broadcasts
        if (getActivity() != null) {
            IntentFilter filter = new IntentFilter(ACTION_RELOAD_NOTIFICATIONS);
            getActivity().registerReceiver(notificationReloadReceiver, filter);
            Log.d(TAG, "Broadcast receiver registered");
        }
        
        // ALWAYS reload notifications when fragment resumes, regardless of adapter state
        if (getContext() != null) {
            Toast.makeText(getContext(), "Loading notifications...", Toast.LENGTH_SHORT).show();
        }
        
        loadNotifications();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: NotificationsFragment paused");
        
        // Unregister our broadcast receiver
        if (getActivity() != null && notificationReloadReceiver != null) {
            try {
                getActivity().unregisterReceiver(notificationReloadReceiver);
                Log.d(TAG, "Broadcast receiver unregistered");
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
            }
        }
    }
    
    /**
     * Load notifications from Firestore.
     * This method is public so it can be called from other activities to refresh the notifications.
     */
    public void loadNotifications() {
        if (userId == null) {
            Log.e(TAG, "Cannot load notifications: User ID is null");
            showEmptyView("You need to be logged in to view notifications");
            return;
        }
        
        Log.d(TAG, "loadNotifications: Loading notifications for user: " + userId);
        showLoading(true);
        
        notificationList.clear();
        adapter.notifyDataSetChanged();
        
        // Log the query we're about to make
        Log.d(TAG, "Executing Firestore query: collection('notifications').whereEqualTo('userId', '" + userId + "')");
        
        // First check if the collection exists - added for debugging
        db.collection("notifications")
            .limit(1)
            .get()
            .addOnSuccessListener(snapshot -> {
                Log.d(TAG, "Collection check: 'notifications' collection " + 
                      (snapshot.isEmpty() ? "is empty" : "has documents"));
                
                // Now proceed with the actual query
                loadNotificationsWithCheck();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to check notifications collection: " + e.getMessage(), e);
                showError("Error checking notifications: " + e.getMessage());
                showLoading(false);
            });
    }
    
    // Separated query method for better debugging
    private void loadNotificationsWithCheck() {
        // Load from Firestore
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    int resultCount = task.getResult().size();
                    Log.d(TAG, "loadNotifications: Successfully queried Firestore, found " + 
                            resultCount + " notifications");
                    
                    if (resultCount == 0) {
                        // Try loading without the userId filter to see if any notifications exist
                        loadAllNotificationsForDebugging();
                    }
                    
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        try {
                            // Get the complete document data for debugging
                            Map<String, Object> data = document.getData();
                            Log.d(TAG, "Processing notification document ID: " + document.getId());
                            Log.d(TAG, "Document data: " + data);
                            
                            // Check that the userId matches exactly by comparing strings
                            String notificationUserId = document.getString("userId");
                            Log.d(TAG, "Comparing userIds - Document: '" + notificationUserId + 
                                   "', Current: '" + userId + "', Equal: " + 
                                   (userId != null && userId.equals(notificationUserId)));
                            
                            if (notificationUserId == null) {
                                Log.e(TAG, "⚠️ Document is missing userId field: " + document.getId());
                                continue; // Skip this document
                            }
                            
                            if (!userId.equals(notificationUserId)) {
                                Log.w(TAG, "⚠️ Notification userId (" + notificationUserId + 
                                       ") doesn't match current user (" + userId + ")");
                                continue; // Skip this document
                            }
                            
                            // Check for required fields
                            if (!document.contains("title") || !document.contains("message")) {
                                Log.e(TAG, "⚠️ Document is missing required fields: " + document.getId());
                                continue; // Skip this document
                            }
                            
                            Notification notification = new Notification();
                            notification.setId(document.getId());
                            notification.setTitle(document.getString("title"));
                            notification.setMessage(document.getString("message"));
                            
                            // Get timestamp with fallback to current time
                            Long timestamp = document.getLong("timestamp");
                            notification.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());
                            
                            // Get read status with fallback to false
                            Boolean read = document.getBoolean("read");
                            notification.setRead(read != null ? read : false);
                            
                            // Get recipe related data
                            notification.setRecipeId(document.getString("recipeId"));
                            notification.setRecipeName(document.getString("recipeName"));
                            notification.setDay(document.getString("day"));
                            notification.setMealType(document.getString("mealType"));
                            
                            notificationList.add(notification);
                            Log.d(TAG, "✅ Added notification: " + notification.getTitle() + 
                                   " - " + notification.getMessage());
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing notification document " + document.getId() + ": " + e.getMessage(), e);
                        }
                    }
                    
                    // Check if the adapter is still attached to the fragment
                    if (adapter != null && isAdded() && !isDetached()) {
                        // Update RecyclerView
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "Notifications loaded: " + notificationList.size());
                    } else {
                        Log.e(TAG, "Cannot update adapter - fragment is detached or adapter is null");
                    }
                    
                    showLoading(false);
                    
                    // Show empty view if no notifications
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
    
    /**
     * For debugging purposes only - load all notifications regardless of userId
     * to check if there are any in the collection
     */
    private void loadAllNotificationsForDebugging() {
        Log.d(TAG, "⚠️ DEBUG: Checking for ANY notifications in the collection");
        
        db.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    int resultCount = task.getResult().size();
                    Log.d(TAG, "⚠️ DEBUG: Found " + resultCount + " total notifications in collection");
                    
                    if (resultCount > 0) {
                        // Log details of each notification for debugging
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String notificationUserId = document.getString("userId");
                            String title = document.getString("title");
                            
                            Log.d(TAG, "⚠️ DEBUG: Notification in collection - ID: " + document.getId() + 
                                   ", userId: " + notificationUserId + ", title: " + title);
                            
                            // Check if this should have matched our query
                            if (userId.equals(notificationUserId)) {
                                Log.e(TAG, "⚠️ ERROR: This notification should have been included in results!");
                            }
                        }
                    }
                }
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
    
    /**
     * Test Firestore to verify connectivity and permissions
     */
    private void testFirestoreConnectivity() {
        Log.d(TAG, "📱 Testing Firestore connectivity...");
        
        // Test access to the 'notifications' collection
        db.collection("notifications")
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d(TAG, "📱 Firestore connectivity test successful! Collection exists.");
                Log.d(TAG, "📱 Query returned " + querySnapshot.size() + " documents");
                
                // Try recipes collection too as a comparison
                db.collection("recipes")
                    .limit(1)
                    .get()
                    .addOnSuccessListener(recipeSnapshot -> {
                        Log.d(TAG, "📱 Recipe collection test successful! Returned " + recipeSnapshot.size() + " documents");
                        
                        // If no notifications were found, try creating a test notification
                        if (querySnapshot.isEmpty() && userId != null) {
                            createTestNotification();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "📱 Recipe collection test failed: " + e.getMessage(), e);
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "📱 Firestore connectivity test FAILED: " + e.getMessage(), e);
            });
    }
    
    /**
     * Create a test notification directly in Firestore
     * Only used for debugging purposes
     */
    private void createTestNotification() {
        if (userId == null) return;
        
        Log.d(TAG, "🧪 Creating test notification for debugging");
        
        // First get a random recipeId from Firestore to include in the notification
        db.collection("recipes")
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                String recipeId = null;
                String recipeName = "Test Recipe";
                
                // If we have a recipe, use its ID
                if (!querySnapshot.isEmpty()) {
                    recipeId = querySnapshot.getDocuments().get(0).getId();
                    recipeName = querySnapshot.getDocuments().get(0).getString("recipeName");
                    if (recipeName == null) {
                        recipeName = "Unknown Recipe";
                    }
                    Log.d(TAG, "🧪 Using recipe for test: " + recipeName + " (ID: " + recipeId + ")");
                } else {
                    Log.d(TAG, "🧪 No recipes found, using placeholder data");
                }
                
                // Create notification data
                java.util.Map<String, Object> testNotification = new java.util.HashMap<>();
                testNotification.put("userId", userId);
                testNotification.put("title", "Recipe Reminder");
                testNotification.put("message", recipeName + " planned for Dinner on Tuesday");
                testNotification.put("timestamp", System.currentTimeMillis());
                testNotification.put("read", false);
                
                // Add recipe data for navigation testing
                testNotification.put("recipeId", recipeId);
                testNotification.put("recipeName", recipeName);
                testNotification.put("day", "Tuesday");
                testNotification.put("mealType", "Dinner");
                
                // Add to Firestore
                db.collection("notifications")
                    .add(testNotification)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "🧪 Test notification created with ID: " + documentReference.getId());
                        
                        // Reload notifications to include the test one
                        loadNotifications();
                        
                        // Show a toast for feedback
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Test notification created", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "🧪 Failed to create test notification: " + e.getMessage(), e);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Failed to create test notification", Toast.LENGTH_SHORT).show();
                        }
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "🧪 Failed to fetch recipe for test notification: " + e.getMessage(), e);
                createFallbackTestNotification();
            });
    }
    
    /**
     * Create a test notification without recipe data as fallback
     */
    private void createFallbackTestNotification() {
        if (userId == null || getContext() == null) return;
        
        Log.d(TAG, "🧪 Creating fallback test notification without recipe data");
        
        // Create notification data
        java.util.Map<String, Object> testNotification = new java.util.HashMap<>();
        testNotification.put("userId", userId);
        testNotification.put("title", "Test Notification");
        testNotification.put("message", "This is a test notification (no recipe data available)");
        testNotification.put("timestamp", System.currentTimeMillis());
        testNotification.put("read", false);
        
        // Add to Firestore
        db.collection("notifications")
            .add(testNotification)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "🧪 Fallback test notification created with ID: " + documentReference.getId());
                loadNotifications();
                Toast.makeText(getContext(), "Test notification created", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "🧪 Failed to create fallback test notification: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Failed to create test notification", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void setupToolbar() {
        if (getActivity() == null) return;
        
        // Get toolbar if it exists
        androidx.appcompat.widget.Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            // Add a manual refresh button
            toolbar.getMenu().clear();
            toolbar.inflateMenu(R.menu.menu_notifications);
            
            // Set click listener for refresh button
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_refresh) {
                    Log.d(TAG, "Manual refresh requested");
                    Toast.makeText(getContext(), "Refreshing notifications...", Toast.LENGTH_SHORT).show();
                    loadNotifications();
                    return true;
                }
                return false;
            });
        }
    }
} 