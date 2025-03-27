package edu.prakriti.mealmate.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.prakriti.mealmate.CustomProgressDialog;
import edu.prakriti.mealmate.GeoTagActivity;
import edu.prakriti.mealmate.MapExplorerActivity;
import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.adapters.StoreAdapter;
import edu.prakriti.mealmate.model.SavedLocation;
import edu.prakriti.mealmate.utils.GroceryDatabaseHelper;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class FavoriteStoresFragment extends Fragment {
    
    private RecyclerView favStoreRecyclerView;
    private com.google.android.material.button.MaterialButton addFavStoreButton;
    private com.google.android.material.chip.Chip filterStoresChip, viewMapChip;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    
    private List<SavedLocation> storeList = new ArrayList<>();
    private StoreAdapter storeAdapter;
    private CustomProgressDialog customProgressDialog;
    private View view;
    private View emptyStateView;
    private View loadingOverlay;
    // Comment out SwipeRefreshLayout until dependency is added
    // private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    
    private ActivityResultLauncher<Intent> geoTagLauncher;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_favorite_stores, container, false);
        
        Log.d("FavoriteStoresFragment", "onCreateView called");
        
        // Initialize views
        favStoreRecyclerView = view.findViewById(R.id.favStoresRecyclerView);
        addFavStoreButton = view.findViewById(R.id.addFavStoreButton);
        filterStoresChip = view.findViewById(R.id.filterStoresChip);
        viewMapChip = view.findViewById(R.id.viewMapChip);
        emptyStateView = view.findViewById(R.id.emptyStateView);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        
        // Comment out until dependency is added
        // swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        Button emptyStateAddButton = view.findViewById(R.id.emptyStateAddButton);
        
        // Replace custom progress dialog with the overlay for better UX
        customProgressDialog = new CustomProgressDialog(getActivity()) {
            @Override
            public void show() {
                loadingOverlay.setVisibility(View.VISIBLE);
            }
            
            @Override
            public void dismiss() {
                loadingOverlay.setVisibility(View.GONE);
                // Comment out until dependency is added
                // if (swipeRefreshLayout.isRefreshing()) {
                //     swipeRefreshLayout.setRefreshing(false);
                // }
            }
            
            @Override
            public boolean isRefreshing() {
                // Temporarily return false until dependency is added
                // return swipeRefreshLayout.isRefreshing();
                return false;
            }
        };
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        
        // Set up the RecyclerView
        favStoreRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        
        // Initialize adapter if we already have data
        if (!storeList.isEmpty()) {
            Log.d("FavoriteStoresFragment", "Creating adapter in onCreateView with existing data: " + storeList.size() + " items");
            storeAdapter = new StoreAdapter(requireContext(), new ArrayList<>(storeList));
            favStoreRecyclerView.setAdapter(storeAdapter);
        }
        
        // Set up empty state button
        emptyStateAddButton.setOnClickListener(v -> {
            startGeoTagActivity();
        });
        
        // Setup swipe refresh - comment out until dependency is added
        /*
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d("FavoriteStoresFragment", "Swipe refresh triggered");
            refreshStores();
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.accent);
        */
        
        // Load stores from Firestore
        loadFavStoreDataFromFireStore();
        
        // Set up click listeners
        setupClickListeners();
        
        return view;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Register for activity result
        geoTagLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d("FavoriteStoresFragment", "GeoTagActivity result received, refreshing stores");
                // Always refresh on return from GeoTagActivity
                loadFavStoreDataFromFireStore();
            }
        );
    }
    
    private void setupClickListeners() {
        filterStoresChip.setOnClickListener(v -> {
            if(storeAdapter != null && !storeList.isEmpty()) {
                showFilterBottomSheet();
            } else {
                Snackbar.make(view, "No stores available to filter", Snackbar.LENGTH_SHORT).show();
            }
        });
        
        addFavStoreButton.setOnClickListener(v -> {
            startGeoTagActivity();
        });
        
        viewMapChip.setOnClickListener(v -> {
            if (!storeList.isEmpty()) {
                Intent intent = new Intent(getContext(), MapExplorerActivity.class);
                intent.putParcelableArrayListExtra("storeList", new ArrayList<>(storeList));
                startActivity(intent);
            } else {
                Snackbar.make(view, "No stores available to view on map", Snackbar.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showFilterBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
        View bottomSheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_sort_options, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Set up click listeners for each option
        bottomSheetView.findViewById(R.id.sort_by_distance).setOnClickListener(option -> {
            sortStoresByDistance();
            bottomSheetDialog.dismiss();
        });

        bottomSheetView.findViewById(R.id.sort_by_grocery).setOnClickListener(option -> {
            sortStoresByGroceryMatches();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }
    
    // Update visibility handling for empty state
    private void updateVisibility() {
        if (storeList.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            favStoreRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            favStoreRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // First check if we need to migrate data
        Log.d("FavoriteStoresFragment", "onResume called - checking for data migration");
        checkAndMigrateStores();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Log.d("FavoriteStoresFragment", "onStart called");
        
        // Ensure adapter is initialized if needed
        if (storeAdapter == null && !storeList.isEmpty()) {
            Log.d("FavoriteStoresFragment", "Initializing adapter in onStart with " + storeList.size() + " items");
            storeAdapter = new StoreAdapter(requireContext(), new ArrayList<>(storeList));
            favStoreRecyclerView.setAdapter(storeAdapter);
        }
        
        // Always refresh stores when fragment becomes visible
        refreshStores();
    }
    
    /**
     * Public method to refresh stores data - can be called from outside
     */
    public void refreshStores() {
        if (isAdded()) {
            Log.d("FavoriteStoresFragment", "refreshStores called - Fragment is attached");
            
            // Make sure we're on the main thread for UI operations
            if (getActivity() != null) {
                Log.d("FavoriteStoresFragment", "Activity is not null, running on UI thread");
                getActivity().runOnUiThread(() -> {
                    loadFavStoreDataFromFireStore();
                });
            } else {
                Log.d("FavoriteStoresFragment", "Activity is null, can't run on UI thread");
                loadFavStoreDataFromFireStore();
            }
        } else {
            Log.d("FavoriteStoresFragment", "Fragment not added to activity, skipping refresh");
        }
    }
    
    /**
     * Check if there are stores in the old "favstore" collection and migrate them to "savedLocations"
     */
    private void checkAndMigrateStores() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // First check if favstore collection exists and has data
        db.collection("favstore").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                // Old collection exists and has data, migrate it
                Log.d("FavoriteStoresFragment", "Found " + task.getResult().size() + 
                      " stores in old 'favstore' collection - migrating to 'savedLocations'");
                migrateStoresData(task.getResult());
            } else {
                // No migration needed, load data normally
                refreshStores();
            }
        }).addOnFailureListener(e -> {
            // If there's an error, just try to load the data normally
            Log.e("FavoriteStoresFragment", "Error checking favstore collection", e);
            refreshStores();
        });
    }
    
    /**
     * Migrate stores from old favstore collection to new savedLocations collection
     */
    private void migrateStoresData(QuerySnapshot querySnapshot) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final int[] migratedCount = {0};
        
        for (DocumentSnapshot document : querySnapshot) {
            Map<String, Object> oldData = document.getData();
            if (oldData == null) continue;
            
            Map<String, Object> newData = new HashMap<>();
            
            // Map fields from old format to new format
            String storeName = (String) oldData.get("storeName");
            if (storeName == null || storeName.isEmpty()) continue;
            
            newData.put("name", storeName);
            newData.put("address", oldData.get("address"));
            
            // Parse latitude and longitude from latLong string
            String latLong = (String) oldData.get("latLong");
            if (latLong != null && !latLong.isEmpty()) {
                try {
                    String[] latLngParts = latLong.replace("Lat:", "").replace("Long:", "").split(",");
                    double latitude = Double.parseDouble(latLngParts[0].trim());
                    double longitude = Double.parseDouble(latLngParts[1].trim());
                    
                    newData.put("latitude", latitude);
                    newData.put("longitude", longitude);
                } catch (Exception e) {
                    Log.e("FavoriteStoresFragment", "Error parsing latLong: " + latLong, e);
                    continue;
                }
            } else {
                continue; // Skip if no location data
            }
            
            // Copy ingredients
            List<String> ingredients = (List<String>) oldData.get("ingredients");
            if (ingredients != null) {
                newData.put("availableIngredients", ingredients);
                newData.put("ingredients", ingredients); // For backward compatibility
            }
            
            // Add default values
            newData.put("distance", "0.0 km");
            newData.put("matchingCount", 0);
            newData.put("timestamp", System.currentTimeMillis());
            
            // Generate image URL
            double lat = (double) newData.get("latitude");
            double lng = (double) newData.get("longitude");
            String imageUrl = "https://maps.googleapis.com/maps/api/staticmap?center=" + 
                    lat + "," + lng + "&zoom=16&size=400x200&markers=color:red%7C" + 
                    lat + "," + lng;
            newData.put("imageUrl", imageUrl);
            
            // Create new document ID
            String documentId = String.valueOf(System.currentTimeMillis() + migratedCount[0]);
            
            // Save to new collection
            db.collection("savedLocations")
                .document(documentId)
                .set(newData)
                .addOnSuccessListener(aVoid -> {
                    migratedCount[0]++;
                    Log.d("FavoriteStoresFragment", "Successfully migrated store: " + storeName);
                    
                    // Delete from old collection after migration
                    document.getReference().delete();
                    
                    // Check if migration is complete
                    if (migratedCount[0] >= querySnapshot.size()) {
                        Log.d("FavoriteStoresFragment", "Migration complete. Migrated " + migratedCount[0] + " stores.");
                        refreshStores();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FavoriteStoresFragment", "Error migrating store: " + storeName, e);
                    
                    // Check if we've processed all stores, even with errors
                    migratedCount[0]++;
                    if (migratedCount[0] >= querySnapshot.size()) {
                        Log.d("FavoriteStoresFragment", "Migration complete with errors. Migrated " + migratedCount[0] + " stores.");
                        refreshStores();
                    }
                });
        }
        
        // If there are no documents to migrate, just load the data
        if (querySnapshot.isEmpty()) {
            refreshStores();
        }
    }
    
    void loadFavStoreDataFromFireStore() {
        // Get FireStore reference
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Clear any existing items
        storeList.clear();
        
        // Show loading indicator
        customProgressDialog.show();

        // Add detailed debug log
        Log.d("FavoriteStoresFragment", "Starting to load data from Firestore...");

        // Get grocery items for matching
        GroceryDatabaseHelper groceryDbHelper = new GroceryDatabaseHelper(requireContext());
        Map<String, Map<String, List<String>>> weeklyGroceryMap = groceryDbHelper.getGroceryItemsForWeekUnpurchased();
        Set<String> uniqueGroceryItems = new HashSet<>();

        for (Map<String, List<String>> categoryMap : weeklyGroceryMap.values()) {
            for (List<String> items : categoryMap.values()) {
                uniqueGroceryItems.addAll(items);
            }
        }

        List<String> groceryItems = new ArrayList<>(uniqueGroceryItems);
        Log.d("FavoriteStoresFragment", "Found " + groceryItems.size() + " grocery items for matching");
        
        // Direct query to savedLocations with error handling
        try {
            // Use orderBy to ensure consistent results and limit to prevent excessive data loading
            db.collection("savedLocations")
              .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
              .get()
              .addOnSuccessListener(queryDocumentSnapshots -> {
                  Log.d("FavoriteStoresFragment", "Firestore query successful");
                  if (queryDocumentSnapshots.isEmpty()) {
                      Log.d("FavoriteStoresFragment", "No stores found in savedLocations");
                      checkFavStoreCollection(groceryItems);
                  } else {
                      Log.d("FavoriteStoresFragment", "Found " + queryDocumentSnapshots.size() + " stores in savedLocations");
                      processDocuments(queryDocumentSnapshots.getDocuments(), groceryItems);
                  }
              })
              .addOnFailureListener(e -> {
                  Log.e("FavoriteStoresFragment", "Firestore query error: " + e.getMessage(), e);
                  // Check for specific Firestore errors
                  if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                      com.google.firebase.firestore.FirebaseFirestoreException firestoreException = 
                          (com.google.firebase.firestore.FirebaseFirestoreException) e;
                      Log.e("FavoriteStoresFragment", "Firestore error code: " + firestoreException.getCode());
                      
                      // Handle specific error cases
                      switch (firestoreException.getCode()) {
                          case PERMISSION_DENIED:
                              showError("Permission denied: Check your Firestore security rules");
                              break;
                          case UNAVAILABLE:
                              showError("Firestore is currently unavailable. Check your internet connection");
                              break;
                          default:
                              showError("Failed to load stores: " + e.getMessage());
                              break;
                      }
                  } else {
                      showError("Failed to load stores: " + e.getMessage());
                  }
                  checkFavStoreCollection(groceryItems);
              });
        } catch (Exception e) {
            Log.e("FavoriteStoresFragment", "Exception during Firestore query setup", e);
            showError("Error setting up Firestore query: " + e.getMessage());
            customProgressDialog.dismiss();
        }
    }
    
    /**
     * Checks the old favstore collection for backward compatibility
     */
    private void checkFavStoreCollection(List<String> groceryItems) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("favstore").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                int docCount = documents.size();
                Log.d("FavoriteStoresFragment", "Found " + docCount + " documents in favstore collection");
                
                if (docCount > 0) {
                    // Process documents from favstore collection 
                    processDocuments(documents, groceryItems);
                } else {
                    // No documents in either collection
                    Log.d("FavoriteStoresFragment", "No stores found in any collection");
                    emptyStateView.setVisibility(View.VISIBLE);
                    favStoreRecyclerView.setVisibility(View.GONE);
                    customProgressDialog.dismiss();
                }
            } else {
                Log.e("FavoriteStoresFragment", "Error getting favstore collection: ", task.getException());
                showError("Failed to load stores: " + task.getException().getMessage());
            }
        }).addOnFailureListener(e -> {
            Log.e("FavoriteStoresFragment", "Failed to load favstore collection", e);
            showError("Failed to load stores: " + e.getMessage());
        });
    }
    
    /**
     * Process documents from either collection
     */
    private void processDocuments(List<DocumentSnapshot> documents, List<String> groceryItems) {
        // Clear the list before processing
        storeList.clear();
        
        Log.d("FavoriteStoresFragment", "Processing " + documents.size() + " documents");
        
        // Use a map to temporarily store documents by ID to prevent duplicates
        Map<String, SavedLocation> locationMap = new HashMap<>();
        
        for (DocumentSnapshot document : documents) {
            try {
                Map<String, Object> data = document.getData();
                if (data == null) {
                    Log.e("FavoriteStoresFragment", "Document data is null for ID: " + document.getId());
                    continue;
                }
                
                // Get store name - try both name and storeName fields
                String storeName = getString(data, "name");
                if (storeName == null || storeName.isEmpty()) {
                    storeName = getString(data, "storeName");
                }
                
                if (storeName == null || storeName.isEmpty()) {
                    Log.e("FavoriteStoresFragment", "Store name is missing in document: " + document.getId());
                    continue;
                }
                
                Log.d("FavoriteStoresFragment", "Processing store: " + storeName + " (ID: " + document.getId() + ")");
                
                // Get store address
                String address = getString(data, "address");
                
                // Get image URL if available
                String imageUrl = getString(data, "imageUrl");
                
                // Get distance (default to 0 km)
                String distance = getString(data, "distance");
                if (distance == null || distance.isEmpty()) {
                    distance = "0.0 km";
                }
                
                // Try to get latitude and longitude directly
                Double latitude = getDouble(data, "latitude");
                Double longitude = getDouble(data, "longitude");
                
                // If not available, try to parse from latLong string
                if (latitude == null || longitude == null) {
                    String latLong = getString(data, "latLong");
                    if (latLong != null && !latLong.isEmpty()) {
                        try {
                            String[] latLngParts = latLong.replace("Lat:", "").replace("Long:", "").split(",");
                            latitude = Double.parseDouble(latLngParts[0].trim());
                            longitude = Double.parseDouble(latLngParts[1].trim());
                        } catch (Exception e) {
                            Log.e("FavoriteStoresFragment", "Error parsing latLong: " + latLong, e);
                        }
                    }
                }
                
                // Get ingredients - try both fields for compatibility
                List<String> ingredients = getStringList(data, "availableIngredients");
                if (ingredients == null || ingredients.isEmpty()) {
                    ingredients = getStringList(data, "ingredients");
                }
                
                if (ingredients == null) {
                    ingredients = new ArrayList<>(); // Default to empty list
                }
                
                // Find matching ingredients
                List<String> matchedIngredients = new ArrayList<>();
                for (String groceryItem : groceryItems) {
                    if (ingredients.contains(groceryItem)) {
                        matchedIngredients.add(groceryItem);
                    }
                }
                
                int matchCount = matchedIngredients.size();
                
                // Create a SavedLocation object
                SavedLocation savedLocation = new SavedLocation(
                        storeName,
                        imageUrl != null ? imageUrl : "https://example.com/default_image.jpg",
                        address != null ? address : "No address provided",
                        latitude != null ? latitude : 0.0,
                        longitude != null ? longitude : 0.0,
                        distance,
                        ingredients,
                        matchCount
                );
                
                // Set matched ingredients
                savedLocation.setMatchedIngredients(matchedIngredients);
                
                // Add to map using document ID as key to prevent duplicates
                locationMap.put(document.getId(), savedLocation);
                
                Log.d("FavoriteStoresFragment", "Added store: " + storeName + ", Ingredients: " + ingredients.size() + ", Matched: " + matchCount);
            } catch (Exception e) {
                Log.e("FavoriteStoresFragment", "Error parsing document: " + document.getId(), e);
            }
        }
        
        // Add all stores from the map to the list
        storeList.addAll(locationMap.values());
        
        // Log total stores found
        Log.d("FavoriteStoresFragment", "Total stores loaded: " + storeList.size());
        
        // Sort stores by name (can be changed to sort by distance, etc.)
        Collections.sort(storeList, (s1, s2) -> s1.getName().compareToIgnoreCase(s2.getName()));
        
        // Update the UI
        requireActivity().runOnUiThread(() -> {
            // Update visibility based on whether we have stores
            updateVisibility();
            
            // Update adapter
            if (storeList.isEmpty()) {
                Log.d("FavoriteStoresFragment", "No stores available - showing empty state");
            } else {
                // If adapter exists, update it; otherwise create a new one
                if (storeAdapter == null) {
                    Log.d("FavoriteStoresFragment", "Creating new StoreAdapter with " + storeList.size() + " items");
                    storeAdapter = new StoreAdapter(requireContext(), new ArrayList<>(storeList));
                    favStoreRecyclerView.setAdapter(storeAdapter);
                } else {
                    Log.d("FavoriteStoresFragment", "Updating existing StoreAdapter with " + storeList.size() + " items");
                    // Use the updateList method, which properly handles list updates
                    storeAdapter.updateList(new ArrayList<>(storeList));
                }
            }
            
            // Dismiss progress dialog and refresh indicator
            customProgressDialog.dismiss();
            
            // Inspect adapter to verify contents
            inspectStoreAdapter();
        });
    }
    
    // Helper methods for safe data extraction
    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof String ? (String) value : null;
    }
    
    private Double getDouble(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof List<?>) {
            try {
                return (List<String>) value;
            } catch (ClassCastException e) {
                Log.e("FavoriteStoresFragment", "Failed to cast " + key + " to List<String>", e);
                // Try to convert the list items to strings
                List<?> list = (List<?>) value;
                List<String> result = new ArrayList<>();
                for (Object item : list) {
                    result.add(String.valueOf(item));
                }
                return result;
            }
        }
        return null;
    }
    
    private void showError(String message) {
        if (getActivity() == null) return;
        
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
        emptyStateView.setVisibility(View.VISIBLE);
        favStoreRecyclerView.setVisibility(View.GONE);
        
        customProgressDialog.dismiss();
    }
    
    private void sortStoresByDistance() {
        if (storeList.size() > 1) {
            storeList.sort((store1, store2) -> {
                // Parse distances (remove units and convert to double)
                double dist1 = parseDistance(store1.getDistance());
                double dist2 = parseDistance(store2.getDistance());
                return Double.compare(dist1, dist2);
            });
            storeAdapter.notifyDataSetChanged();
        }
    }
    
    private double parseDistance(String distance) {
        try {
            // Extract numeric part from strings like "3.2 km"
            return Double.parseDouble(distance.replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private void sortStoresByGroceryMatches() {
        if (storeList.size() > 1) {
            storeList.sort((store1, store2) -> Integer.compare(store2.getMatchedIngredients().size(), store1.getMatchedIngredients().size()));
            storeAdapter.notifyDataSetChanged();
        }
    }
    
    /**
     * Start the GeoTagActivity with a request code to get result back
     */
    private void startGeoTagActivity() {
        Log.d("FavoriteStoresFragment", "Starting GeoTagActivity with launcher");
        Intent intent = new Intent(getContext(), GeoTagActivity.class);
        geoTagLauncher.launch(intent);
    }
    
    private void inspectStoreAdapter() {
        if (storeAdapter != null) {
            try {
                int count = storeAdapter.getItemCount();
                Log.d("FavoriteStoresFragment", "StoreAdapter contains " + count + " items");

                // Inspect each item
                for (int i = 0; i < Math.min(count, storeList.size()); i++) {
                    SavedLocation store = storeList.get(i);
                    Log.d("FavoriteStoresFragment", "Item " + i + ": " + store.getName() 
                         + ", Ingredients: " + (store.getAvailableIngredients() != null ? store.getAvailableIngredients().size() : 0)
                         + ", Matched: " + (store.getMatchedIngredients() != null ? store.getMatchedIngredients().size() : 0));
                }
            } catch (Exception e) {
                Log.e("FavoriteStoresFragment", "Error inspecting adapter", e);
            }
        } else {
            Log.d("FavoriteStoresFragment", "StoreAdapter is null");
        }
    }
    
    // Add a listener to watch for changes in Firestore with better error handling
    private void setupFirestoreListener() {
        Log.d("FavoriteStoresFragment", "Setting up Firestore listener");
        
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("savedLocations")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("FavoriteStoresFragment", "Firestore listener error: " + e.getMessage(), e);
                        return;
                    }
                    
                    if (snapshots != null) {
                        Log.d("FavoriteStoresFragment", "Firestore update detected: " + 
                              (snapshots.isEmpty() ? "empty" : snapshots.size() + " documents"));
                        if (!snapshots.isEmpty()) {
                            refreshStores();
                        }
                    }
                });
        } catch (Exception e) {
            Log.e("FavoriteStoresFragment", "Failed to set up Firestore listener", e);
        }
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("FavoriteStoresFragment", "onViewCreated called");
        
        // Set up Firestore listener for real-time updates
        setupFirestoreListener();
    }
} 