package edu.prakriti.mealmate.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.prakriti.mealmate.CustomProgressDialog;
import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.WeeklyPlanActivity;
import edu.prakriti.mealmate.adapters.MealAdapter;
import edu.prakriti.mealmate.model.Meal;
import edu.prakriti.mealmate.model.Recipe;

public class HomeFragment extends Fragment implements MealAdapter.OnMealRemoveListener {

    private RecyclerView todaysMealRecyclerView, tomorrowsMealRecyclerView;
    private MealAdapter todayMealAdapter, tomorrowMealAdapter;
    private List<Meal> mealList;
    private TextView noMealText, noTomorrowMealText;
    private MaterialButton viewWeeklyPlanButton;
    private CustomProgressDialog customProgressDialog;

    private int completedRequests = 0;
    private int totalRequests = 0;
    private int tomorrowCompletedRequests = 0;
    private int tomorrowTotalRequests = 0;

    private View view;
    private static final String TAG = "HomeFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
         view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Views
        todaysMealRecyclerView = view.findViewById(R.id.todaysMealRecyclerView);
        tomorrowsMealRecyclerView = view.findViewById(R.id.tomorrowsMealRecyclerView);
        
        todaysMealRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        tomorrowsMealRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        
        noMealText = view.findViewById(R.id.noMealText);
        noTomorrowMealText = view.findViewById(R.id.noTomorrowMealText);
        
        viewWeeklyPlanButton = view.findViewById(R.id.viewWeeklyPlanButton);
        customProgressDialog = new CustomProgressDialog(getActivity());

        // Load meal data
        loadDataMealToday(true);
        loadDataMealTomorrow();

        // Button Listeners
        viewWeeklyPlanButton.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), WeeklyPlanActivity.class));
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mealList == null || mealList.isEmpty()) {
            loadDataMealToday(false);
            loadDataMealTomorrow();
        }
    }

    private void loadDataMealToday(boolean showLoad) {
        completedRequests = 0;
        totalRequests = 0;

        if (showLoad) {
            customProgressDialog.show();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.enableNetwork();
        DocumentReference mealRef = db.collection("meals").document(todayDate);

        mealRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    List<Long> breakfastTimestamps = (List<Long>) document.get("Breakfast");
                    List<Long> lunchTimestamps = (List<Long>) document.get("Lunch");
                    List<Long> dinnerTimestamps = (List<Long>) document.get("Dinner");

                    if (breakfastTimestamps == null) breakfastTimestamps = new ArrayList<>();
                    if (lunchTimestamps == null) lunchTimestamps = new ArrayList<>();
                    if (dinnerTimestamps == null) dinnerTimestamps = new ArrayList<>();

                    totalRequests = breakfastTimestamps.size() + lunchTimestamps.size() + dinnerTimestamps.size();
                    List<Meal> allMeals = new ArrayList<>();

                    fetchAllMeals(breakfastTimestamps, lunchTimestamps, dinnerTimestamps, allMeals);
                } else {
                    updateMealRecyclerView(new ArrayList<>());
                    if (showLoad) customProgressDialog.dismiss();
                }
            } else {
                showSnackbar("error");
                if (showLoad) customProgressDialog.dismiss();
            }
        });
    }
    
    private void loadDataMealTomorrow() {
        tomorrowCompletedRequests = 0;
        tomorrowTotalRequests = 0;

        // Calculate tomorrow's date
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date tomorrow = calendar.getTime();
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String tomorrowDate = dateFormat.format(tomorrow);
        
        Log.d(TAG, "Loading meals for tomorrow: " + tomorrowDate);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference mealRef = db.collection("meals").document(tomorrowDate);

        mealRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    List<Long> breakfastTimestamps = (List<Long>) document.get("Breakfast");
                    List<Long> lunchTimestamps = (List<Long>) document.get("Lunch");
                    List<Long> dinnerTimestamps = (List<Long>) document.get("Dinner");

                    if (breakfastTimestamps == null) breakfastTimestamps = new ArrayList<>();
                    if (lunchTimestamps == null) lunchTimestamps = new ArrayList<>();
                    if (dinnerTimestamps == null) dinnerTimestamps = new ArrayList<>();

                    tomorrowTotalRequests = breakfastTimestamps.size() + lunchTimestamps.size() + dinnerTimestamps.size();
                    List<Meal> allTomorrowMeals = new ArrayList<>();

                    fetchTomorrowMeals(breakfastTimestamps, lunchTimestamps, dinnerTimestamps, allTomorrowMeals);
                } else {
                    Log.d(TAG, "No meal plan found for tomorrow");
                    updateTomorrowMealRecyclerView(new ArrayList<>());
                }
            } else {
                Log.e(TAG, "Error loading tomorrow's meals", task.getException());
                updateTomorrowMealRecyclerView(new ArrayList<>());
            }
        });
    }

    private void fetchAllMeals(List<Long> breakfastTimestamps, List<Long> lunchTimestamps,
                               List<Long> dinnerTimestamps, List<Meal> allMeals) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (totalRequests == 0) {
            updateMealRecyclerView(new ArrayList<>());
            customProgressDialog.dismiss();
            return;
        }

        for (Long timestamp : breakfastTimestamps) {
            fetchRecipeByTimestamp(db, timestamp, "Breakfast", allMeals);
        }
        for (Long timestamp : lunchTimestamps) {
            fetchRecipeByTimestamp(db, timestamp, "Lunch", allMeals);
        }
        for (Long timestamp : dinnerTimestamps) {
            fetchRecipeByTimestamp(db, timestamp, "Dinner", allMeals);
        }
    }
    
    private void fetchTomorrowMeals(List<Long> breakfastTimestamps, List<Long> lunchTimestamps,
                               List<Long> dinnerTimestamps, List<Meal> allTomorrowMeals) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (tomorrowTotalRequests == 0) {
            updateTomorrowMealRecyclerView(new ArrayList<>());
            return;
        }

        for (Long timestamp : breakfastTimestamps) {
            fetchTomorrowRecipeByTimestamp(db, timestamp, "Breakfast", allTomorrowMeals);
        }
        for (Long timestamp : lunchTimestamps) {
            fetchTomorrowRecipeByTimestamp(db, timestamp, "Lunch", allTomorrowMeals);
        }
        for (Long timestamp : dinnerTimestamps) {
            fetchTomorrowRecipeByTimestamp(db, timestamp, "Dinner", allTomorrowMeals);
        }
    }

    private void fetchRecipeByTimestamp(FirebaseFirestore db, Long timestamp, String mealType, List<Meal> allMeals) {
        db.collection("recipes")
                .whereEqualTo("timestamp", timestamp)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            Recipe recipe = document.toObject(Recipe.class);
                            if (recipe != null) {
                                allMeals.add(new Meal(recipe, mealType));
                            }
                        }
                    }
                    checkAndUpdateRecyclerView(allMeals);
                })
                .addOnFailureListener(e -> checkAndUpdateRecyclerView(allMeals));
    }
    
    private void fetchTomorrowRecipeByTimestamp(FirebaseFirestore db, Long timestamp, String mealType, List<Meal> allTomorrowMeals) {
        db.collection("recipes")
                .whereEqualTo("timestamp", timestamp)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            Recipe recipe = document.toObject(Recipe.class);
                            if (recipe != null) {
                                allTomorrowMeals.add(new Meal(recipe, mealType));
                            }
                        }
                    }
                    checkAndUpdateTomorrowRecyclerView(allTomorrowMeals);
                })
                .addOnFailureListener(e -> checkAndUpdateTomorrowRecyclerView(allTomorrowMeals));
    }

    private void checkAndUpdateRecyclerView(List<Meal> allMeals) {
        completedRequests++;
        if (completedRequests == totalRequests) {
            updateMealRecyclerView(allMeals);
            customProgressDialog.dismiss();
        }
    }
    
    private void checkAndUpdateTomorrowRecyclerView(List<Meal> allTomorrowMeals) {
        tomorrowCompletedRequests++;
        if (tomorrowCompletedRequests == tomorrowTotalRequests) {
            updateTomorrowMealRecyclerView(allTomorrowMeals);
        }
    }

    private void updateMealRecyclerView(List<Meal> allMeals) {
        boolean hasMeals = !allMeals.isEmpty();
        noMealText.setVisibility(hasMeals ? View.GONE : View.VISIBLE);
        todaysMealRecyclerView.setVisibility(hasMeals ? View.VISIBLE : View.GONE);

        if (hasMeals) {
            todayMealAdapter = new MealAdapter(requireContext(), allMeals, false, this);
            todaysMealRecyclerView.setAdapter(todayMealAdapter);
        } else {
            todaysMealRecyclerView.setAdapter(null);
        }
    }
    
    private void updateTomorrowMealRecyclerView(List<Meal> allTomorrowMeals) {
        boolean hasMeals = !allTomorrowMeals.isEmpty();
        noTomorrowMealText.setVisibility(hasMeals ? View.GONE : View.VISIBLE);
        tomorrowsMealRecyclerView.setVisibility(hasMeals ? View.VISIBLE : View.GONE);

        if (hasMeals) {
            tomorrowMealAdapter = new MealAdapter(requireContext(), allTomorrowMeals, false, this);
            tomorrowsMealRecyclerView.setAdapter(tomorrowMealAdapter);
            Log.d(TAG, "Tomorrow's meals loaded: " + allTomorrowMeals.size());
        } else {
            tomorrowsMealRecyclerView.setAdapter(null);
            Log.d(TAG, "No meals for tomorrow");
        }
    }

    @Override
    public void onMealRemove(Meal meal, int position) {
        // Implement meal removal logic if needed
    }

    private void showSnackbar(String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }
}