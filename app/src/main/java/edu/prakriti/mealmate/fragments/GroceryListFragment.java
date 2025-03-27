package edu.prakriti.mealmate.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.prakriti.mealmate.CustomProgressDialog;
import edu.prakriti.mealmate.DelegateActivity;
import edu.prakriti.mealmate.GroceryActivity;
import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.adapters.GroceryAdapter;
import edu.prakriti.mealmate.adapters.GroceryIngredientAdapter;
import edu.prakriti.mealmate.model.Meal;
import edu.prakriti.mealmate.model.Recipe;
import edu.prakriti.mealmate.models.GroceryIngredient;
import edu.prakriti.mealmate.utils.GroceryDatabaseHelper;
import edu.prakriti.mealmate.utils.SwipeToDeleteCallback;
import edu.prakriti.mealmate.utils.IngredientConstants;


public class GroceryListFragment extends Fragment {

    private ProgressUpdateListener progressUpdateListener;
    private static final String ARG_GROCERY_TYPE = "grocery_type";
    private String groceryType;
    private RecyclerView recyclerView;
    private GroceryAdapter groceryAdapter;
    private FloatingActionButton fabMain, fabAddItem, fabImportItems;
    private boolean isFabOpen = false;
    private Animation fabOpen, fabClose, rotateForward, rotateBackward;
    private MaterialTextView labelAddItem, labelImportItems;
    private GroceryDatabaseHelper dbHelper;
    private String selectedTab = "Today"; // Default to "Today"
    private String currentDate; // Current date being displayed

    private FloatingActionButton fabDelegateShopping;
    private MaterialTextView labelDelegateShopping;

    private boolean showOnlyPurchased = false;
    private boolean showOnlyUnpurchased = false;

    CustomProgressDialog customProgressDialog;
    public static GroceryListFragment newInstance(String groceryType) {
        GroceryListFragment fragment = new GroceryListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GROCERY_TYPE, groceryType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ProgressUpdateListener) {
            progressUpdateListener = (ProgressUpdateListener) context;
        } else {
            throw new ClassCastException(context.toString() + " must implement ProgressUpdateListener");
        }
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Enable options menu
        if (getArguments() != null) {
            groceryType = getArguments().getString(ARG_GROCERY_TYPE);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Don't inflate menu here since it's already inflated in GroceryActivity
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        // These menu items no longer exist in the main menu since we moved them to a popup menu
        // They will be null here, so we need to check before using them
        MenuItem purchasedItem = menu.findItem(R.id.action_filter_purchased);
        MenuItem unpurchasedItem = menu.findItem(R.id.action_filter_unpurchased);
        MenuItem showAllItem = menu.findItem(R.id.action_show_all);
        
        // Only proceed if all menu items are found (which they won't be in our new menu structure)
        if (purchasedItem != null && unpurchasedItem != null && showAllItem != null) {
            // Show/hide menu items based on current filter state
            if (showOnlyPurchased) {
                // Currently showing purchased items
                purchasedItem.setVisible(false);
                unpurchasedItem.setVisible(true);
                showAllItem.setVisible(true);
            } else if (showOnlyUnpurchased) {
                // Currently showing unpurchased items
                purchasedItem.setVisible(true);
                unpurchasedItem.setVisible(false);
                showAllItem.setVisible(true);
            } else {
                // Currently showing all items
                purchasedItem.setVisible(true);
                unpurchasedItem.setVisible(true);
                showAllItem.setVisible(false);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_filter_purchased) {
            showOnlyPurchased = true;
            showOnlyUnpurchased = false;
            // Refresh options menu
            requireActivity().invalidateOptionsMenu();
            // Reload data with filter
            loadData(getDateForTab(selectedTab), selectedTab);
            return true;
        } else if (id == R.id.action_filter_unpurchased) {
            showOnlyPurchased = false;
            showOnlyUnpurchased = true;
            // Refresh options menu
            requireActivity().invalidateOptionsMenu();
            // Reload data with filter
            loadData(getDateForTab(selectedTab), selectedTab);
            return true;
        } else if (id == R.id.action_show_all) {
            // Reset filters
            showOnlyPurchased = false;
            showOnlyUnpurchased = false;
            // Refresh options menu
            requireActivity().invalidateOptionsMenu();
            // Reload data without filter
            loadData(getDateForTab(selectedTab), selectedTab);
            return true;
        } else if (id == R.id.action_add_item) {
            String date = getDateForTab(selectedTab);
            showAddItemDialog(date);
            return true;
        } else if (id == R.id.action_shop) {
            // Show a popup with all the shopping actions
            showShoppingActionsDialog();
            return true;
        } else if (id == R.id.action_import_mealplan) {
            showImportConfirmationDialog();
            return true;
        } else if (id == R.id.action_delegate_shopping) {
            if (dbHelper.hasGroceryDataForWeek()) {
                Intent intent = new Intent(getActivity(), DelegateActivity.class);
                startActivity(intent);
            } else {
                showSnackbar("No grocery items available for shopping list");
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grocery_list, container, false);
        dbHelper = new GroceryDatabaseHelper(requireContext());
        customProgressDialog = new CustomProgressDialog(getContext());

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_fall_down));
        recyclerView.scheduleLayoutAnimation();

        currentDate = getDateForTab(groceryType);
        loadData(currentDate, selectedTab);
        fabMain = view.findViewById(R.id.fab_main);
        fabAddItem = view.findViewById(R.id.fab_add_item);
        fabImportItems = view.findViewById(R.id.fab_import_items);
        labelAddItem = view.findViewById(R.id.label_add_item);
        labelImportItems = view.findViewById(R.id.label_import_items);
        fabDelegateShopping = view.findViewById(R.id.fab_delegate_shopping);
        labelDelegateShopping = view.findViewById(R.id.label_delegate_shopping);


        fabOpen = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close);
        rotateForward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_forward);
        rotateBackward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_backward);

        fabMain.setOnClickListener(v -> {
            toggleFabMenu();

        });
        fabAddItem.setOnClickListener(v -> {
            toggleFabMenu();
            String dates = getDateForTab(groceryType);
            showAddItemDialog(dates);
        });
        fabImportItems.setOnClickListener(v -> {toggleFabMenu();
            showImportConfirmationDialog();
        });

        fabDelegateShopping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if( dbHelper.hasGroceryDataForWeek()){
                Intent intent = new Intent(getActivity(), DelegateActivity.class );
                startActivity(intent);
               }
               else {
                   showSnackbar("No grocery items available for shopping list");
               }
            }
        });

        TabLayout tabLayout = requireActivity().findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getText().toString();
                currentDate = getDateForTab(selectedTab);
                Log.d("GroceryListFragment", "Tab Selected: " + selectedTab + ", Date: " + currentDate);
                loadData(currentDate, selectedTab);

                // Force update the listener for the first switch
                if (progressUpdateListener != null) {
                    int progress = calculateProgressForTab(selectedTab);
                    String summaryText = getSummaryTextForTab(selectedTab, progress);
                    progressUpdateListener.onProgressUpdated(progress, summaryText);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onTabSelected(tab);
            }
        });

        return view;
    }


    private void showImportConfirmationDialog() {
        // Create Material AlertDialog
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Import from Meal Plan")
                .setMessage("Are you sure you want to import from the meal plan for the upcoming week? " +
                        "This will remove and replace all the grocery list items you have saved before.")
                .setPositiveButton("Yes, Import", (dialog, which) -> {
                    // If user confirms, proceed with import
                    importFromMealPlan();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // If user cancels, dismiss the dialog
                    dialog.dismiss();
                })
                .setCancelable(false) // Prevent dismissing by clicking outside
                .show();
    }

    private void importFromMealPlan() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        GroceryDatabaseHelper dbHelper = new GroceryDatabaseHelper(requireContext());

        // Show Loading Indicator
        customProgressDialog.show();

        // Get Dates from Today to Next 6 Days
        List<String> datesToFetch = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i <= 7; i++) {
            datesToFetch.add(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Clear Existing Grocery Data only once
        dbHelper.clearGroceryData();
        Log.d("GroceryListFragment", "Cleared existing grocery data.");

        // Counter for recipe fetch completion tracking
        final int[] totalRecipesToFetch = {0};
        final int[] completedRecipeFetches = {0};
        
        // Loop Through Dates and Fetch Data
        for (String date : datesToFetch) {
            Log.d("GroceryListFragment", "Fetching data for date: " + date);
            db.collection("meals").document(date).get().addOnCompleteListener(mealTask -> {
                if (mealTask.isSuccessful()) {
                    DocumentSnapshot document = mealTask.getResult();
                    if (document.exists() && document.getData() != null) {
                        Log.d("GroceryListFragment", "Data found for date: " + date);
                        
                        Map<String, Object> mealData = document.getData();
                        
                        // Count the total recipes we need to fetch
                        for (String mealType : mealData.keySet()) {
                            Object mealTypeObj = mealData.get(mealType);
                            if (mealTypeObj instanceof List<?>) {
                                List<?> timestampList = (List<?>) mealTypeObj;
                                totalRecipesToFetch[0] += timestampList.size();
                            }
                        }
                        
                        // Process each meal type (Breakfast, Lunch, Dinner)
                        for (String mealType : mealData.keySet()) {
                            Object mealTypeObj = mealData.get(mealType);
                            if (mealTypeObj instanceof List<?>) {
                                List<?> timestampList = (List<?>) mealTypeObj;
                                
                                if (!timestampList.isEmpty()) {
                                    for (Object timestampObj : timestampList) {
                                        if (timestampObj instanceof Long) {
                                            Long timestamp = (Long) timestampObj;
                                            // Query recipes by timestamp
                                            db.collection("recipes")
                                              .whereEqualTo("timestamp", timestamp)
                                              .get()
                                              .addOnSuccessListener(queryDocumentSnapshots -> {
                                                  if (!queryDocumentSnapshots.isEmpty()) {
                                                      DocumentSnapshot recipeSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                                                      
                                                      // Create Recipe object from document
                                                      Recipe recipe = recipeSnapshot.toObject(Recipe.class);
                                                      if (recipe != null) {
                                                          // Set recipe ID if it's not already set
                                                          if (recipe.getRecipeId() == null || recipe.getRecipeId().isEmpty()) {
                                                              recipe.setRecipeId(recipeSnapshot.getId());
                                                          }
                                                          
                                                          // Add ingredients to grocery database
                                                          Map<String, List<String>> ingredientsMap = recipe.getIngredients();
                                                          if (ingredientsMap != null) {
                                                              for (String category : ingredientsMap.keySet()) {
                                                                  List<String> ingredients = ingredientsMap.get(category);
                                                                  if (ingredients != null) {
                                                                      for (String ingredient : ingredients) {
                                                                          // Use Recipe Name as Category for better organization
                                                                          dbHelper.addGroceryItem(ingredient, date, recipe.getRecipeName());
                                                                          Log.d("GroceryListFragment", "Added ingredient: " + ingredient + 
                                                                                " from recipe: " + recipe.getRecipeName() +
                                                                                " for date: " + date);
                                                                      }
                                                                  }
                                                              }
                                                          }
                                                      }
                                                  }
                                                  
                                                  // Increment completed counter and check if we're done
                                                  completedRecipeFetches[0]++;
                                                  checkImportCompletion(totalRecipesToFetch[0], completedRecipeFetches[0]);
                                              })
                                              .addOnFailureListener(e -> {
                                                  Log.e("GroceryListFragment", "Error fetching recipe with timestamp: " + timestamp, e);
                                                  completedRecipeFetches[0]++;
                                                  checkImportCompletion(totalRecipesToFetch[0], completedRecipeFetches[0]);
                                              });
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Log.d("GroceryListFragment", "No data found for date: " + date);
                    }
                } else {
                    Log.e("GroceryListFragment", "Error fetching data for date: " + date, mealTask.getException());
                }
            });
        }
        
        // If we didn't find any recipes to import, finish the process
        new android.os.Handler().postDelayed(() -> {
            if (totalRecipesToFetch[0] == 0) {
                customProgressDialog.dismiss();
                loadData(getDateForTab(selectedTab), selectedTab);
                showSnackbar("No meals found to import");
            }
        }, 3000); // Give firebase a few seconds to return results
    }
    
    // Helper method to check if import is complete
    private void checkImportCompletion(int total, int completed) {
        if (completed >= total && total > 0) {
            // Run on UI thread
            requireActivity().runOnUiThread(() -> {
                customProgressDialog.dismiss();
                loadData(getDateForTab(selectedTab), selectedTab);
                showSnackbar("Successfully imported " + total + " recipes");
                
                // Update progress for parent activity
                if (progressUpdateListener != null) {
                    int progress = calculateProgressForTab(selectedTab);
                    String summaryText = getSummaryTextForTab(selectedTab, progress);
                    progressUpdateListener.onProgressUpdated(progress, summaryText);
                }
            });
        }
    }





    private int calculateProgressForTab(String tab) {
        int totalItems = 0;
        int purchasedItems = 0;

        if ("Week".equals(tab)) {
            // For the week tab, we need to get data across multiple dates
            // This logic needs to be updated to use the new GroceryIngredient model
            Calendar calendar = Calendar.getInstance();
            String startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            
            calendar.add(Calendar.DAY_OF_YEAR, 7);
            String endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            
            // We would need a method that gets all ingredients across a date range
            // For now, we'll approximate using the existing method
            for (int i = 0; i < 7; i++) {
                calendar.add(Calendar.DAY_OF_YEAR, -6 + i); // Reset and iterate through days
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
                
                List<GroceryIngredient> ingredients = dbHelper.getGroceryIngredientsByDate(date);
                totalItems += ingredients.size();
                
                for (GroceryIngredient ingredient : ingredients) {
                    if (ingredient.isPurchased()) {
                        purchasedItems++;
                    }
                }
            }
        } else {
            // For a specific date tab (Today/Tomorrow)
            String date = getDateForTab(tab);
            List<GroceryIngredient> ingredients = dbHelper.getGroceryIngredientsByDate(date);
            totalItems = ingredients.size();
            
            for (GroceryIngredient ingredient : ingredients) {
                if (ingredient.isPurchased()) {
                    purchasedItems++;
                }
            }
        }

        return totalItems > 0 ? (int) ((purchasedItems / (float) totalItems) * 100) : 0;
    }

    private String getSummaryTextForTab(String tab, int progress) {
        if ("Today".equals(tab)) {
            return "Today (" + progress + "% Completed)";
        } else if ("Tomorrow".equals(tab)) {
            return "Tomorrow (" + progress + "% Completed)";
        } else if ("Week".equals(tab)) {
            return "This Week (" + progress + "% Completed)";
        } else {
            return tab + " (" + progress + "% Completed)";
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        progressUpdateListener = null; // Avoid memory leaks
    }

    private void toggleFabMenu() {
        if (isFabOpen) {
            // Close Animations
            fabAddItem.startAnimation(fabClose);
            fabImportItems.startAnimation(fabClose);
            fabDelegateShopping.startAnimation(fabClose); // New FAB
            labelAddItem.startAnimation(fabClose);
            labelImportItems.startAnimation(fabClose);
            labelDelegateShopping.startAnimation(fabClose); // New Label

            fabAddItem.setVisibility(View.GONE);
            fabImportItems.setVisibility(View.GONE);
            fabDelegateShopping.setVisibility(View.GONE); // New FAB
            labelAddItem.setVisibility(View.GONE);
            labelImportItems.setVisibility(View.GONE);
            labelDelegateShopping.setVisibility(View.GONE); // New Label

            fabMain.startAnimation(rotateBackward);
        } else {
            // Open Animations
            fabAddItem.setVisibility(View.VISIBLE);
            fabImportItems.setVisibility(View.VISIBLE);
            fabDelegateShopping.setVisibility(View.VISIBLE); // New FAB
            labelAddItem.setVisibility(View.VISIBLE);
            labelImportItems.setVisibility(View.VISIBLE);
            labelDelegateShopping.setVisibility(View.VISIBLE); // New Label

            fabAddItem.startAnimation(fabOpen);
            fabImportItems.startAnimation(fabOpen);
            fabDelegateShopping.startAnimation(fabOpen); // New FAB
            labelAddItem.startAnimation(fabOpen);
            labelImportItems.startAnimation(fabOpen);
            labelDelegateShopping.startAnimation(fabOpen); // New Label

            fabMain.startAnimation(rotateForward);
        }
        isFabOpen = !isFabOpen;
    }


    private void loadData(String date, String selectedTab) {
        Log.d("GroceryListFragment", "Loading data for date: " + date + ", tab: " + selectedTab);
        Map<String, List<GroceryIngredient>> groceryCategories = dbHelper.getGroceryIngredientsByDateGrouped(date);
        
        // Apply filter if needed
        if (showOnlyPurchased || showOnlyUnpurchased) {
            // Filter to show only purchased or non-purchased items
            Map<String, List<GroceryIngredient>> filteredCategories = new HashMap<>();
            for (String category : groceryCategories.keySet()) {
                List<GroceryIngredient> items = groceryCategories.get(category);
                List<GroceryIngredient> filteredItems = new ArrayList<>();
                
                for (GroceryIngredient ingredient : items) {
                    boolean isPurchased = ingredient.isPurchased();
                    if ((showOnlyPurchased && isPurchased) || 
                        (showOnlyUnpurchased && !isPurchased)) {
                        filteredItems.add(ingredient);
                    }
                }
                
                if (!filteredItems.isEmpty()) {
                    filteredCategories.put(category, filteredItems);
                }
            }
            groceryCategories = filteredCategories;
        }
        
        // Show message if no items after filtering
        if (groceryCategories.isEmpty()) {
            if (showOnlyPurchased) {
                showSnackbar("No purchased items to display");
            } else if (showOnlyUnpurchased) {
                showSnackbar("No unpurchased items to display");
            }
        }
        
        groceryAdapter = new GroceryAdapter(requireContext(), groceryCategories, dbHelper, date, selectedTab);
        recyclerView.setAdapter(groceryAdapter);
        recyclerView.scheduleLayoutAnimation();

        // Get a reference to the ingredient adapter for later use
        groceryAdapter.setOnIngredientAdapterCreatedListener(this::setupSwipeToDelete);

        if (progressUpdateListener != null) {
            int progress = calculateProgressForTab(selectedTab);
            String summaryText = getSummaryTextForTab(selectedTab, progress);
            progressUpdateListener.onProgressUpdated(progress, summaryText);
        }
    }

    // Setup swipe-to-delete for each ingredient adapter
    private void setupSwipeToDelete(GroceryIngredientAdapter adapter, String category) {
        ItemTouchHelper.SimpleCallback swipeCallback = new SwipeToDeleteCallback(requireContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                try {
                    int position = viewHolder.getAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) {
                        // Position is invalid, reset the view and return
                        adapter.notifyDataSetChanged();
                        return;
                    }
                    
                    // Perform the swipe action
                    adapter.handleSwipe(position, direction);
                    
                    // If all items of this category are removed, reload data
                    if (adapter.getItemCount() == 0) {
                        loadData(currentDate, selectedTab);
                    }
                    
                    // Update progress
                    if (progressUpdateListener != null) {
                        int progress = calculateProgressForTab(selectedTab);
                        String summaryText = getSummaryTextForTab(selectedTab, progress);
                        progressUpdateListener.onProgressUpdated(progress, summaryText);
                    }
                } catch (Exception e) {
                    // If any error occurs, reset the view to avoid crash
                    e.printStackTrace();
                    adapter.notifyDataSetChanged();
                }
            }
            
            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                // Disable swipe if adapter is updating data
                if (adapter.isUpdating()) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }
        };
        
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(adapter.getRecyclerView());
    }


    private void showAddItemDialog(String date) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_grocery_item, null);
        
        // Initialize views
        EditText inputName = dialogView.findViewById(R.id.inputName);
        EditText inputQuantity = dialogView.findViewById(R.id.inputQuantity);
        EditText inputUnit = dialogView.findViewById(R.id.inputUnit);
        EditText inputPrice = dialogView.findViewById(R.id.inputPrice);
        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);

        // Set up category spinner with predefined categories
        List<String> categories = new ArrayList<>(IngredientConstants.CATEGORY_LIST);
        categories.add(0, "Select Category"); // Add default option
        
        Log.d("GroceryListFragment", "Categories loaded: " + categories.size());
        
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), 
            android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Log the selected category when it changes
        categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String category = parent.getItemAtPosition(position).toString();
                Log.d("GroceryListFragment", "Category selected: " + category);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                Log.d("GroceryListFragment", "No category selected");
            }
        });

        builder.setView(dialogView)
               .setTitle("Add New Item")
               .setNegativeButton("Cancel", null)
               .setPositiveButton("Add", (dialog, which) -> {
                   String itemName = inputName.getText().toString().trim();
                   String selectedCategory = categorySpinner.getSelectedItem().toString();
                   
                   Log.d("GroceryListFragment", "Adding item: " + itemName + " with category: " + selectedCategory);
                   
                   if (!itemName.isEmpty() && !selectedCategory.equals("Select Category")) {
                       if (dbHelper.isItemExistsForDate(itemName, date)) {
                           showSnackbar("Item already exists for this date!");
                       } else {
                           // Get other values
                           double quantity = 1.0;
                           try {
                               quantity = Double.parseDouble(inputQuantity.getText().toString());
                           } catch (NumberFormatException e) {
                               // Use default if not valid
                           }
                           
                           String unit = inputUnit.getText().toString().trim();
                           
                           double price = 0.0;
                           try {
                               price = Double.parseDouble(inputPrice.getText().toString());
                           } catch (NumberFormatException e) {
                               // Use default if not valid
                           }
                           
                           // Create a new GroceryIngredient object with selected category
                           GroceryIngredient ingredient = new GroceryIngredient(
                               0, // ID will be set by the database
                               itemName,
                               selectedCategory, // Use selected category instead of "Wish List"
                               quantity,
                               unit,
                               price,
                               false, // Not purchased by default
                               date,
                               "" // No meal name
                           );
                           
                           // Add to database
                           dbHelper.addGroceryIngredient(ingredient);
                           
                           // Reload data and show confirmation
                           loadData(date, selectedTab);
                           showSnackbar("Item added successfully!");
                       }
                   } else {
                       showSnackbar("Please enter item name and select a category!");
                   }
               });

        builder.show();
    }

    private String getDateForTab(String type) {
        if ("Today".equals(type)) {
            return dbHelper.getTodayDate();
        } else if ("Tomorrow".equals(type)) {
            // Get tomorrow's date using Calendar since getTomorrowDate may not exist anymore
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
        }
        return dbHelper.getTodayDate();  // Default to today if no match
    }

    private void showSnackbar(String message) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
                .setAction("OK", v -> {})
                .show();
    }

    // Add a new method to show a dialog with shopping actions
    public void showShoppingActionsDialog() {
        String[] options = {
            "Show Purchased Items", 
            "Show Unpurchased Items", 
            "Show All Items", 
            "Import from Meal Plan", 
            "Generate Shopping List"
        };
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Shopping Actions")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Show Purchased Items
                        showOnlyPurchased = true;
                        showOnlyUnpurchased = false;
                        break;
                    case 1: // Show Unpurchased Items
                        showOnlyPurchased = false;
                        showOnlyUnpurchased = true;
                        break;
                    case 2: // Show All Items
                        showOnlyPurchased = false;
                        showOnlyUnpurchased = false;
                        break;
                    case 3: // Import from Meal Plan
                        showImportConfirmationDialog();
                        return;
                    case 4: // Generate Shopping List (previously Delegate Shopping)
                        if (dbHelper.hasGroceryDataForWeek()) {
                            Intent intent = new Intent(getActivity(), DelegateActivity.class);
                            startActivity(intent);
                        } else {
                            showSnackbar("No grocery items available for shopping list");
                        }
                        return;
                }
                
                // Refresh options menu
                requireActivity().invalidateOptionsMenu();
                // Reload data with filter
                loadData(getDateForTab(selectedTab), selectedTab);
            })
            .show();
    }
}

