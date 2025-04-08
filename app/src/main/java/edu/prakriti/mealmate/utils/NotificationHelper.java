package edu.prakriti.mealmate.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Helper class to generate test notifications for the MealMate app
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    
    // Notification types
    public static final String TYPE_RECIPE_SHARED = "recipe_shared";
    public static final String TYPE_MEAL_REMINDER = "meal_reminder";
    public static final String TYPE_GROCERY_REMINDER = "grocery_reminder";
    public static final String TYPE_SYSTEM = "system";
    
    // Sample titles for different notification types
    private static final String[] RECIPE_TITLES = {
            "New Recipe Shared",
            "Recipe Recommendation",
            "Popular Recipe Alert",
            "Recipe of the Day"
    };
    
    private static final String[] MEAL_REMINDER_TITLES = {
            "Meal Plan Reminder",
            "Upcoming Meal",
            "Meal Preparation Time",
            "Don't Forget Your Meal"
    };
    
    private static final String[] GROCERY_REMINDER_TITLES = {
            "Grocery List Updated",
            "Items Running Low",
            "Time to Shop",
            "Weekend Shopping Reminder"
    };
    
    private static final String[] SYSTEM_TITLES = {
            "App Update Available",
            "Profile Incomplete",
            "Welcome to MealMate",
            "New Features Available"
    };
    
    // Sample messages
    private static final String[] RECIPE_MESSAGES = {
            "Someone shared a delicious %s recipe with you!",
            "Check out this amazing %s recipe we found for you.",
            "This %s recipe is trending among MealMate users!",
            "Today's featured recipe: %s. Perfect for your next meal!"
    };
    
    private static final String[] MEAL_REMINDER_MESSAGES = {
            "Don't forget about your %s scheduled for %s!",
            "Your %s is coming up for %s. Time to prepare!",
            "Reminder: You have %s planned for %s today.",
            "%s is on your meal plan for %s. Ingredients ready?"
    };
    
    private static final String[] GROCERY_REMINDER_MESSAGES = {
            "Your grocery list has been updated with %d new items.",
            "You're running low on %d essential ingredients.",
            "Weekend shopping? You have %d items on your list.",
            "Don't forget to pick up %d items from your grocery list."
    };
    
    private static final String[] SYSTEM_MESSAGES = {
            "A new version of MealMate is available with exciting features!",
            "Your profile is 75% complete. Add a profile picture to finish setting up.",
            "Welcome to MealMate! Start by adding your favorite recipes.",
            "We've added meal planning and grocery list features. Try them now!"
    };
    
    // Sample recipe names
    private static final String[] RECIPE_NAMES = {
            "Pasta Carbonara",
            "Chicken Curry",
            "Vegetable Stir Fry",
            "Chocolate Cake",
            "Greek Salad",
            "Beef Tacos",
            "Vegetable Lasagna",
            "Mango Smoothie"
    };
    
    // Sample meal types
    private static final String[] MEAL_TYPES = {
            "Breakfast",
            "Lunch",
            "Dinner",
            "Snack"
    };
    
    // Sample days
    private static final String[] DAYS = {
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday",
            "Sunday",
            "Today",
            "Tomorrow"
    };
    
    private static final Random random = new Random();
    
    /**
     * Create a test notification in Firestore
     * @param type The type of notification to create
     * @return true if notification was created successfully
     */
    public static boolean createTestNotification(String type) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (userId == null) {
            Log.e(TAG, "Cannot create notification: User not logged in");
            return false;
        }

        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference notificationRef = db.collection("notifications").document();
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        
        // Fill based on notification type
        switch (type) {
            case TYPE_RECIPE_SHARED:
                String recipeName = RECIPE_NAMES[random.nextInt(RECIPE_NAMES.length)];
                notification.put("title", RECIPE_TITLES[random.nextInt(RECIPE_TITLES.length)]);
                notification.put("message", String.format(
                        RECIPE_MESSAGES[random.nextInt(RECIPE_MESSAGES.length)], 
                        recipeName));
                notification.put("recipeName", recipeName);
                notification.put("recipeId", "test_recipe_" + System.currentTimeMillis());
                break;
                
            case TYPE_MEAL_REMINDER:
                String mealType = MEAL_TYPES[random.nextInt(MEAL_TYPES.length)];
                String day = DAYS[random.nextInt(DAYS.length)];
                String mealRecipeName = RECIPE_NAMES[random.nextInt(RECIPE_NAMES.length)];
                
                notification.put("title", MEAL_REMINDER_TITLES[random.nextInt(MEAL_REMINDER_TITLES.length)]);
                notification.put("message", String.format(
                        MEAL_REMINDER_MESSAGES[random.nextInt(MEAL_REMINDER_MESSAGES.length)],
                        mealRecipeName, day));
                notification.put("mealType", mealType);
                notification.put("day", day);
                notification.put("recipeName", mealRecipeName);
                notification.put("recipeId", "test_recipe_" + System.currentTimeMillis());
                break;
                
            case TYPE_GROCERY_REMINDER:
                int itemCount = random.nextInt(10) + 1; // 1-10 items
                
                notification.put("title", GROCERY_REMINDER_TITLES[random.nextInt(GROCERY_REMINDER_TITLES.length)]);
                notification.put("message", String.format(
                        GROCERY_REMINDER_MESSAGES[random.nextInt(GROCERY_REMINDER_MESSAGES.length)],
                        itemCount));
                break;
                
            case TYPE_SYSTEM:
            default:
                notification.put("title", SYSTEM_TITLES[random.nextInt(SYSTEM_TITLES.length)]);
                notification.put("message", SYSTEM_MESSAGES[random.nextInt(SYSTEM_MESSAGES.length)]);
                break;
        }
        
        return addNotificationToFirestore(notificationRef, notification);
    }
    
    /**
     * Add the notification to Firestore
     */
    private static boolean addNotificationToFirestore(DocumentReference ref, Map<String, Object> notification) {
        final boolean[] success = {false};
        
        ref.set(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification created successfully with ID: " + ref.getId());
                    success[0] = true;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding notification", e);
                    success[0] = false;
                });
        
        return success[0];
    }
} 