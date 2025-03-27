package edu.prakriti.mealmate.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.adapters.StoreAdapter;
import edu.prakriti.mealmate.adapters.StoreSelectionAdapter;
import edu.prakriti.mealmate.model.SavedLocation;

public class StoreSelectionFragment extends Fragment {

    private RecyclerView recyclerViewStores;
    private RadioGroup radioGroupStoreMode;
    private View layoutManualEntry;
    private View layoutStoreList;

    private StoreSelectionAdapter storeAdapter;
    private List<SavedLocation> storeList = new ArrayList<>();
    private TextInputEditText editTextSearchStores;
    private ViewSwitcher viewSwitcher;

    private ArrayList<String> selectedIngredients;


    private FusedLocationProviderClient fusedLocationClient;
    private View view;
    private List<SavedLocation> masterStoreList = new ArrayList<>();

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    public static StoreSelectionFragment newInstance(ArrayList<String> selectedIngredients) {
        StoreSelectionFragment fragment = new StoreSelectionFragment();
        Bundle args = new Bundle();
        args.putStringArrayList("selectedIngredients", selectedIngredients);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


            selectedIngredients = getArguments() != null ?
                    getArguments().getStringArrayList("selectedIngredients") :
                    new ArrayList<>();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
         view = inflater.inflate(R.layout.fragment_store_selection, container, false);

        // Initialize Views
        recyclerViewStores = view.findViewById(R.id.recyclerview_stores);
        editTextSearchStores = view.findViewById(R.id.edittext_search_stores);

        radioGroupStoreMode = view.findViewById(R.id.radiogroup_store_mode);
        viewSwitcher = view.findViewById(R.id.view_switcher);

        // Setup RecyclerView
        recyclerViewStores.setLayoutManager(new LinearLayoutManager(requireContext()));
        storeAdapter = new StoreSelectionAdapter(requireContext(), storeList);
        recyclerViewStores.setAdapter(storeAdapter);

        // Load Stores from Firebase

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Store Mode Selection Logic
        radioGroupStoreMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_manual_entry) {
                viewSwitcher.setDisplayedChild(0); // Show Manual Entry
            } else if (checkedId == R.id.radio_select_from_server) {
                viewSwitcher.setDisplayedChild(1); // Show Store List
            }
        });
        // Search Functionality
        editTextSearchStores.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStores(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    // Reset to full list when search text is cleared
                    storeAdapter.updateList(storeList);
                }

            }
        });

        loadFavStoreDataFromFirebase();

        return view;
    }



    private void loadFavStoreDataFromFirebase() {
        storeList.clear();
        //fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
        if (checkLocationPermission()) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    double currentLat = location.getLatitude();
                    double currentLng = location.getLongitude();


                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("savedLocations")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                storeList.clear();
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    Map<String, Object> data = document.getData();
                                    if (data == null) continue;
                                    
                                    // Get store name and address from the consistent field names
                                    String storeName = (String) data.get("name");
                                    String address = (String) data.get("address");
                                    
                                    if (storeName == null || storeName.isEmpty()) continue;
                                    
                                    // Get latitude and longitude directly from the fields
                                    double storeLat = 0.0;
                                    double storeLng = 0.0;
                                    
                                    // Safely parse latitude and longitude
                                    if (data.get("latitude") instanceof Number) {
                                        storeLat = ((Number) data.get("latitude")).doubleValue();
                                    } else if (data.get("latitude") instanceof String) {
                                        try {
                                            storeLat = Double.parseDouble((String) data.get("latitude"));
                                        } catch (NumberFormatException e) {
                                            Log.e("StoreSelectionFragment", "Error parsing latitude", e);
                                            continue;
                                        }
                                    }
                                    
                                    if (data.get("longitude") instanceof Number) {
                                        storeLng = ((Number) data.get("longitude")).doubleValue();
                                    } else if (data.get("longitude") instanceof String) {
                                        try {
                                            storeLng = Double.parseDouble((String) data.get("longitude"));
                                        } catch (NumberFormatException e) {
                                            Log.e("StoreSelectionFragment", "Error parsing longitude", e);
                                            continue;
                                        }
                                    }
                                    
                                    // Calculate distance
                                    String distance = calculateDistance(currentLat, currentLng, storeLat, storeLng);
                                    
                                    // Get image URL
                                    String imageUrl = (String) data.get("imageUrl");
                                    if (imageUrl == null || imageUrl.isEmpty()) {
                                        imageUrl = "https://example.com/default_image.jpg";
                                    }

                                    // Get ingredients from either field
                                    List<String> ingredients = new ArrayList<>();
                                    if (data.get("availableIngredients") instanceof List) {
                                        ingredients = (List<String>) data.get("availableIngredients");
                                    } else if (data.get("ingredients") instanceof List) {
                                        ingredients = (List<String>) data.get("ingredients");
                                    }

                                    // Calculate Matched Ingredients
                                    List<String> matchedIngredients = new ArrayList<>();
                                    if (selectedIngredients != null && ingredients != null) {
                                        for (String ingredient : ingredients) {
                                            for (String selectedItem : selectedIngredients) {
                                                if (ingredient.equalsIgnoreCase(selectedItem.trim())) {
                                                    matchedIngredients.add(ingredient);
                                                }
                                            }
                                        }
                                    }

                                    SavedLocation savedLocation = new SavedLocation(
                                            storeName,
                                            imageUrl,
                                            address != null ? address : "No address",
                                            storeLat,
                                            storeLng,
                                            distance,
                                            ingredients,
                                            matchedIngredients.size()
                                    );

                                    savedLocation.setMatchedIngredients(matchedIngredients);

                                    storeList.add(savedLocation);
                                    masterStoreList.add(savedLocation);  // Also add to master list
                                    
                                    Log.d("StoreSelectionFragment", "Added store: " + storeName + 
                                        ", LatLng: " + storeLat + "," + storeLng + 
                                        ", Matches: " + matchedIngredients.size());
                                }
                                storeAdapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                showSnackbar("Failed to load data");
                              //  Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
                                Log.e("StoreSelectionFragment", "Error loading data", e);
                            });

                }

            });
        }
    }

    private String calculateDistance(double currentLat, double currentLng, double storeLat, double storeLng) {
        float[] results = new float[1];
        Location.distanceBetween(currentLat, currentLng, storeLat, storeLng, results);
        float distanceInMeters = results[0];

        if (distanceInMeters < 1000) {
            return String.format(Locale.getDefault(), "%.0f m", distanceInMeters);
        } else {
            return String.format(Locale.getDefault(), "%.1f km", distanceInMeters / 1000);
        }
    }


    private void filterStores(String query) {
        if (query.isEmpty()) {
            // If search query is empty, show full master list
            storeAdapter.updateList(masterStoreList);
        } else {
            // Filter the list based on the query
            List<SavedLocation> filteredList = new ArrayList<>();
            for (SavedLocation location : masterStoreList) {
                if (location.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(location);
                }
            }
            storeAdapter.updateList(filteredList);
        }
    }




    private boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request Permissions
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with getting location

            } else {
                // Permission denied, show a message to the user
                showSnackbar("Location permission denied. Please enable it in settings to get location.");

            }
        }
    }


    private void showSnackbar(String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

}
