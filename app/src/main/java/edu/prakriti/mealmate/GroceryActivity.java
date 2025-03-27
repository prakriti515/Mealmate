package edu.prakriti.mealmate;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textview.MaterialTextView;

import java.util.Arrays;
import java.util.List;

import edu.prakriti.mealmate.adapters.GroceryPageAdapter;
import edu.prakriti.mealmate.fragments.ProgressUpdateListener;
import edu.prakriti.mealmate.utils.GroceryDatabaseHelper;

public class GroceryActivity extends AppCompatActivity implements ProgressUpdateListener, SensorEventListener {

    private ViewPager2 viewPager;
    private GroceryPageAdapter groceryPagerAdapter;
    private TabLayout tabLayout;

    private LinearProgressIndicator progressIndicator;
    private MaterialTextView summaryTextView;
    
    // Shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate = 0;
    private float lastX, lastY, lastZ;
    private static final int SHAKE_THRESHOLD = 600;
    private boolean isSwipeInProgress = false;
    private static final long COOLDOWN_TIME = 1000; // 1 second cooldown
    private long lastShakeTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery);

        progressIndicator = findViewById(R.id.progressIndicator);
        summaryTextView = findViewById(R.id.summary);

        // Set up Toolbar with Default Back Arrow
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize TabLayout and ViewPager2
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
     //   LinearProgressIndicator progressIndicator = findViewById(R.id.progressIndicator);
       // progressIndicator.setProgress(0); // Set dynamically based on purchased items


        // Grocery List Types
        List<String> groceryTypes = Arrays.asList("Today", "Tomorrow", "Week");

        // Set Up ViewPager Adapter with GroceryListFragment
        groceryPagerAdapter = new GroceryPageAdapter(this, groceryTypes);
        viewPager.setAdapter(groceryPagerAdapter);

        // Link ViewPager with TabLayout
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(groceryTypes.get(position))).attach();
        
        // Track current page for menu handling
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                groceryPagerAdapter.setCurrentPosition(position);
            }
        });
        
        // Initialize shake detection
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onProgressUpdated(int progress, String summaryText) {
        progressIndicator.setProgressCompat(progress, true);
        summaryTextView.setText(summaryText);
    }
    
    // Reset shake detection to prevent conflicts with swipe gestures
    public void resetShakeDetection() {
        isSwipeInProgress = true;
        lastShakeTime = System.currentTimeMillis();
        
        // Reset the swipe in progress flag after a delay
        new Handler().postDelayed(() -> isSwipeInProgress = false, COOLDOWN_TIME);
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            
            long currentTime = System.currentTimeMillis();
            
            // Don't process shake if swipe is in progress or cooldown period active
            if (isSwipeInProgress || (currentTime - lastShakeTime < COOLDOWN_TIME)) {
                return;
            }
            
            if ((currentTime - lastUpdate) > 100) {
                long diffTime = currentTime - lastUpdate;
                lastUpdate = currentTime;
                
                float speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000;
                
                if (speed > SHAKE_THRESHOLD) {
                    lastShakeTime = currentTime;
                    // Handle shake event - start delegate activity
                    handleShakeDetection();
                }
                
                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    // Handle shake detection to start delegate activity
    private void handleShakeDetection() {
        // Check if grocery data is available
        GroceryDatabaseHelper dbHelper = new GroceryDatabaseHelper(this);
        
        if (dbHelper.hasGroceryDataForWeek()) {
            // Start delegate activity
            startActivity(new Intent(this, DelegateActivity.class));
        } else {
            // Show message if no grocery items available
            Snackbar.make(
                findViewById(android.R.id.content),
                "No Grocery Items are available to delegate",
                Snackbar.LENGTH_SHORT
            ).show();
        }
        
        resetShakeDetection();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.grocery_menu, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Let the fragment handle menu preparation if it exists
        if (groceryPagerAdapter != null && groceryPagerAdapter.getCurrentFragment() instanceof Fragment) {
            // This will trigger the fragment's onPrepareOptionsMenu
            return super.onPrepareOptionsMenu(menu);
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        // Handle action bar item clicks
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_add_item) {
            // Forward to current fragment to handle
            Fragment currentFragment = groceryPagerAdapter.getCurrentFragment();
            if (currentFragment != null) {
                return currentFragment.onOptionsItemSelected(item);
            }
            return true;
        } else if (id == R.id.action_shop) {
            // Show shopping actions dialog instead of popup menu
            showPopupMenu(null);
            return true;
        } else if (id == R.id.action_filter_purchased || 
                   id == R.id.action_filter_unpurchased || 
                   id == R.id.action_show_all ||
                   id == R.id.action_import_mealplan ||
                   id == R.id.action_delegate_shopping) {
            // Forward to current fragment to handle
            Fragment currentFragment = groceryPagerAdapter.getCurrentFragment();
            if (currentFragment != null) {
                return currentFragment.onOptionsItemSelected(item);
            }
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    // Add this method to show a simple dialog with options
    private void showPopupMenu(View anchorView) {
        // Simple array of options
        final String[] options = {
            "Show Purchased Items",
            "Show Unpurchased Items",
            "Show All Items",
            "Import from Meal Plan",
            "Delegate Shopping"
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Shopping Options");
        
        builder.setItems(options, (dialog, which) -> {
            // Handle click based on position
            Fragment currentFragment = groceryPagerAdapter.getCurrentFragment();
            if (currentFragment != null) {
                switch (which) {
                    case 0: // Show Purchased
                        currentFragment.onOptionsItemSelected(createBasicMenuItem(R.id.action_filter_purchased));
                        break;
                    case 1: // Show Unpurchased
                        currentFragment.onOptionsItemSelected(createBasicMenuItem(R.id.action_filter_unpurchased));
                        break;
                    case 2: // Show All
                        currentFragment.onOptionsItemSelected(createBasicMenuItem(R.id.action_show_all));
                        break;
                    case 3: // Import from meal plan
                        currentFragment.onOptionsItemSelected(createBasicMenuItem(R.id.action_import_mealplan));
                        break;
                    case 4: // Delegate shopping
                        currentFragment.onOptionsItemSelected(createBasicMenuItem(R.id.action_delegate_shopping));
                        break;
                }
            }
        });
        
        builder.show();
    }
    
    // Helper method to create a basic MenuItem with just an ID
    private MenuItem createBasicMenuItem(final int id) {
        return new MenuItem() {
            @Override public int getItemId() { return id; }
            @Override public int getGroupId() { return 0; }
            @Override public int getOrder() { return 0; }
            @Override public MenuItem setTitle(CharSequence title) { return this; }
            @Override public MenuItem setTitle(int title) { return this; }
            @Override public CharSequence getTitle() { return ""; }
            @Override public MenuItem setTitleCondensed(CharSequence title) { return this; }
            @Override public CharSequence getTitleCondensed() { return null; }
            @Override public MenuItem setIcon(android.graphics.drawable.Drawable icon) { return this; }
            @Override public MenuItem setIcon(int iconRes) { return this; }
            @Override public android.graphics.drawable.Drawable getIcon() { return null; }
            @Override public MenuItem setIntent(Intent intent) { return this; }
            @Override public Intent getIntent() { return null; }
            @Override public MenuItem setShortcut(char numericChar, char alphaChar) { return this; }
            @Override public MenuItem setNumericShortcut(char numericChar) { return this; }
            @Override public char getNumericShortcut() { return 0; }
            @Override public MenuItem setAlphabeticShortcut(char alphaChar) { return this; }
            @Override public char getAlphabeticShortcut() { return 0; }
            @Override public MenuItem setCheckable(boolean checkable) { return this; }
            @Override public boolean isCheckable() { return false; }
            @Override public MenuItem setChecked(boolean checked) { return this; }
            @Override public boolean isChecked() { return false; }
            @Override public MenuItem setVisible(boolean visible) { return this; }
            @Override public boolean isVisible() { return true; }
            @Override public MenuItem setEnabled(boolean enabled) { return this; }
            @Override public boolean isEnabled() { return true; }
            @Override public boolean hasSubMenu() { return false; }
            @Override public android.view.SubMenu getSubMenu() { return null; }
            @Override public MenuItem setOnMenuItemClickListener(android.view.MenuItem.OnMenuItemClickListener menuItemClickListener) { return this; }
            @Override public android.view.ContextMenu.ContextMenuInfo getMenuInfo() { return null; }
            @Override public void setShowAsAction(int actionEnum) { }
            @Override public MenuItem setShowAsActionFlags(int actionEnum) { return this; }
            @Override public MenuItem setActionView(View view) { return this; }
            @Override public MenuItem setActionView(int resId) { return this; }
            @Override public View getActionView() { return null; }
            @Override public MenuItem setActionProvider(android.view.ActionProvider actionProvider) { return this; }
            @Override public android.view.ActionProvider getActionProvider() { return null; }
            @Override public boolean expandActionView() { return false; }
            @Override public boolean collapseActionView() { return false; }
            @Override public boolean isActionViewExpanded() { return false; }
            @Override public MenuItem setOnActionExpandListener(android.view.MenuItem.OnActionExpandListener listener) { return this; }
            @Override public MenuItem setContentDescription(CharSequence contentDescription) { return this; }
            @Override public CharSequence getContentDescription() { return null; }
            @Override public MenuItem setTooltipText(CharSequence tooltipText) { return this; }
            @Override public CharSequence getTooltipText() { return null; }
            @Override public MenuItem setNumericShortcut(char numericChar, int deviceId) { return this; }
            @Override public int getNumericModifiers() { return 0; }
            @Override public MenuItem setAlphabeticShortcut(char alphaChar, int deviceId) { return this; }
            @Override public int getAlphabeticModifiers() { return 0; }
            @Override public MenuItem setIconTintList(android.content.res.ColorStateList tint) { return this; }
            @Override public android.content.res.ColorStateList getIconTintList() { return null; }
            @Override public MenuItem setIconTintMode(android.graphics.PorterDuff.Mode tintMode) { return this; }
            @Override public android.graphics.PorterDuff.Mode getIconTintMode() { return null; }
        };
    }
}
