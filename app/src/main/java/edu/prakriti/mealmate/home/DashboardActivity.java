package edu.prakriti.mealmate.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import edu.prakriti.mealmate.GroceryActivity;
import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.fragments.AddRecipeFragment;
import edu.prakriti.mealmate.fragments.FavoriteStoresFragment;
import edu.prakriti.mealmate.fragments.GroceryListFragment;
import edu.prakriti.mealmate.fragments.HomeFragment;
import edu.prakriti.mealmate.fragments.ProfileFragment;
import edu.prakriti.mealmate.fragments.ProgressUpdateListener;
import edu.prakriti.mealmate.fragments.RecipeListFragment;
import edu.prakriti.mealmate.fragments.FavoriteRecipesFragment;
import edu.prakriti.mealmate.fragments.NotificationsFragment;
import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ProgressUpdateListener {

    private DrawerLayout drawerLayout;
    private LinearProgressIndicator progressIndicator;
    private TextView summaryTextView;
    private TabLayout groceryTabLayout; // Added for Grocery List fragment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme preference before inflating UI
        loadSavedTheme();
        
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Add hamburger icon to open drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Set up progress indicator for grocery list
        progressIndicator = findViewById(R.id.progressIndicator);
        summaryTextView = findViewById(R.id.summary);
        
        if (progressIndicator != null) {
            progressIndicator.setProgress(0);
        }

        // Set the initial state of the dark mode toggle
        boolean isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
        Menu navMenu = navigationView.getMenu();
        MenuItem darkModeItem = navMenu.findItem(R.id.nav_dark_mode);
        if (darkModeItem != null) {
            darkModeItem.setChecked(isDarkMode);
        }

        loadLocalProfile(); // Load User Profile Data

        // Handle intent extras
        handleIntentExtras(navigationView, savedInstanceState);
    }
    
    /**
     * Load the saved theme preference
     */
    private void loadSavedTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppThemePrefs", MODE_PRIVATE);
        int savedMode = sharedPreferences.getInt("NightMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedMode);
    }
    
    /**
     * Handle intent extras to determine which fragment to show
     */
    private void handleIntentExtras(NavigationView navigationView, Bundle savedInstanceState) {
        Intent intent = getIntent();
        boolean handledIntent = false;
        
        // Check if launched from recipe creation with open recipe list flag
        if (intent != null && intent.getBooleanExtra("OPEN_RECIPE_LIST", false)) {
            if (intent.getBooleanExtra("SHOW_FAVORITES", false)) {
                // Show Favorite Recipes
                switchToFragment(new FavoriteRecipesFragment(), "Favorite Recipes", false);
                navigationView.setCheckedItem(R.id.nav_favorite_recipes);
                Toast.makeText(this, "Viewing your favorite recipes", Toast.LENGTH_SHORT).show();
            } else {
                // Show Recipe List
                switchToFragment(new RecipeListFragment(), "Recipe List", false);
                navigationView.setCheckedItem(R.id.nav_recipes);
                Toast.makeText(this, "Viewing all recipes", Toast.LENGTH_SHORT).show();
            }
            handledIntent = true;
        }
        // Check if launched from a grocery list intent
        else if (intent != null && intent.hasExtra("openGroceryList")) {
            switchToFragment(GroceryListFragment.newInstance("Today"), "Grocery List", true);
            navigationView.setCheckedItem(R.id.nav_grocery);
            handledIntent = true;
        }
        
        // Default Fragment (Home Screen) if no intent extras and first creation
        if (!handledIntent && savedInstanceState == null) {
            switchToFragment(new HomeFragment(), "Home", false);
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            switchToFragment(new HomeFragment(), "Home", false);
        } else if (itemId == R.id.nav_profile) {
            switchToFragment(new ProfileFragment(), "Profile", false);
        } else if (itemId == R.id.nav_add_recipe) {
            // Directly launch the AddRecipeStepActivity instead of using the fragment
            Intent intent = new Intent(this, edu.prakriti.mealmate.AddRecipeStepActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_recipes) {
            switchToFragment(new RecipeListFragment(), "Recipe List", false);
        } else if (itemId == R.id.nav_favorite_recipes) {
            switchToFragment(new FavoriteRecipesFragment(), "Favorite Recipes", false);
        } else if (itemId == R.id.nav_grocery) {
            // Launch the grocery list fragment
            openGroceryList();
        } else if (itemId == R.id.nav_favorite_stores) {
            switchToFragment(new FavoriteStoresFragment(), "Favorite Stores", false);
        } else if (itemId == R.id.nav_notifications) {
            // Launch standalone notifications activity 
            launchNotificationsActivity();
        } else if (itemId == R.id.nav_dark_mode) {
            // Toggle dark mode
            toggleDarkMode(item);
            return true; // Return without closing drawer to show the change
        } else if (itemId == R.id.nav_sign_out) {
            signOutUser();
            return true; // Return true to indicate we've handled this event
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    /**
     * Helper method to open the grocery list
     */
    private void openGroceryList() {
        // Use the full grocery activity for better user experience with tabs
        Intent intent = new Intent(this, GroceryActivity.class);
        startActivity(intent);
        
        // Alternative approach: Use the fragment inside the dashboard
        // switchToFragment(GroceryListFragment.newInstance("Today"), "Grocery List", true);
    }
    
    /**
     * Helper method to launch the standalone notifications activity
     */
    private void launchNotificationsActivity() {
        Intent intent = new Intent(this, edu.prakriti.mealmate.NotificationsActivity.class);
        startActivity(intent);
        // Apply a custom animation for the transition
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
    
    /**
     * Helper method to switch fragments
     * @param fragment The fragment to switch to
     * @param title The title to display in the action bar
     * @param showProgress Whether to show the progress indicator
     */
    private void switchToFragment(Fragment fragment, String title, boolean showProgress) {
        if (showProgress) {
            showProgressIndicator();
        } else {
            hideProgressIndicator();
        }
        
        setTitle(title);
        
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }
    
    private void showProgressIndicator() {
        if (progressIndicator != null) {
            progressIndicator.setVisibility(View.VISIBLE);
        }
        if (summaryTextView != null) {
            summaryTextView.setVisibility(View.VISIBLE);
        }
    }
    
    private void hideProgressIndicator() {
        if (progressIndicator != null) {
            progressIndicator.setVisibility(View.GONE);
        }
        if (summaryTextView != null) {
            summaryTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        // Close the drawer if it's open when back is pressed
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    void loadLocalProfile() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("USER_ID", null);
        String name = sharedPreferences.getString("USER_NAME", null);
        String mobile = sharedPreferences.getString("USER_MOBILE", null);
        String dob = sharedPreferences.getString("USER_DOB", null);
        String gender = sharedPreferences.getString("USER_GENDER", null);
        String photoUrl = sharedPreferences.getString("USER_PHOTO", null);

        Log.d("userID", "User ID: " + (userId != null ? userId : "null"));
        Log.d("name", "Name: " + (name != null ? name : "null"));
        Log.d("mobile", "Mobile: " + (mobile != null ? mobile : "null"));
        Log.d("dob", "DOB: " + (dob != null ? dob : "null"));
        Log.d("gender", "Gender: " + (gender != null ? gender : "null"));
        Log.d("photoUrl", "Photo URL: " + (photoUrl != null ? photoUrl : "null"));
    }
    
    @Override
    public void onProgressUpdated(int progress, String summaryText) {
        if (progressIndicator != null) {
            progressIndicator.setProgress(progress);
        }
        if (summaryTextView != null) {
            summaryTextView.setText(summaryText);
        }
    }

    private void signOutUser() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Firebase Sign Out
                    FirebaseAuth.getInstance().signOut();

                    // Clear SharedPreferences
                    SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear(); // Clears all saved values
                    editor.apply();

                    // Navigate to MainActivity and clear backstack
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_notifications) {
            launchNotificationsActivity();
            return true;
        } else if (id == R.id.action_search) {
            // Handle search action (implementation would vary based on app requirements)
            Toast.makeText(this, "Search action clicked", Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    /**
     * Toggle between light and dark mode
     * @param item The menu item that was clicked
     */
    private void toggleDarkMode(MenuItem item) {
        // Check current night mode
        boolean isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
        
        // Toggle to opposite mode
        int newMode = isDarkMode ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
        AppCompatDelegate.setDefaultNightMode(newMode);
        
        // Update menu item check state
        item.setChecked(!isDarkMode);
        
        // Save preference
        SharedPreferences sharedPreferences = getSharedPreferences("AppThemePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("NightMode", newMode);
        editor.apply();
        
        // Show toast message
        Toast.makeText(this, isDarkMode ? "Light mode enabled" : "Dark mode enabled", Toast.LENGTH_SHORT).show();
    }
}
