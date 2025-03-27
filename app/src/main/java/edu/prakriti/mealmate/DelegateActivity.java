package edu.prakriti.mealmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.prakriti.mealmate.adapters.DelegateIngredientAdapter;
import edu.prakriti.mealmate.grok.Contact;
import edu.prakriti.mealmate.utils.GroceryDatabaseHelper;

public class DelegateActivity extends AppCompatActivity {
    private RecyclerView recyclerViewIngredients;
    private DelegateIngredientAdapter delegateIngredientAdapter;
    private GroceryDatabaseHelper dbHelper;
    private Map<String, Map<String, List<String>>> weeklyGroceryMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delegate);

        // Initialize database helper
        dbHelper = new GroceryDatabaseHelper(this);

        // Initialize RecyclerView for ingredients only
        recyclerViewIngredients = findViewById(R.id.recyclerViewIngredients);
        recyclerViewIngredients.setLayoutManager(new LinearLayoutManager(this));

        // Fetch grocery items for the week
        weeklyGroceryMap = dbHelper.getGroceryItemsForWeek();

        // Load all ingredients without meal association
        loadAllIngredients();

        // Set up button click listener for Send Request
        findViewById(R.id.buttonSendSMS).setOnClickListener(v -> checkSelectedItems());
    }

    private void loadAllIngredients() {
        // Collect all ingredients from all meals and dates
        List<String> allIngredients = new ArrayList<>();
        Set<String> uniqueIngredients = new HashSet<>();
        
        // Process all ingredients from weekly grocery map
        for (Map.Entry<String, Map<String, List<String>>> mealEntry : weeklyGroceryMap.entrySet()) {
            Map<String, List<String>> dateMap = mealEntry.getValue();
            for (Map.Entry<String, List<String>> dateEntry : dateMap.entrySet()) {
                for (String ingredient : dateEntry.getValue()) {
                    if (!uniqueIngredients.contains(ingredient)) {
                        allIngredients.add(ingredient);
                        uniqueIngredients.add(ingredient);
                    }
                }
            }
        }
        
        // Create and set the adapter for ingredients
        delegateIngredientAdapter = new DelegateIngredientAdapter(allIngredients);
        recyclerViewIngredients.setAdapter(delegateIngredientAdapter);
    }

    private void checkSelectedItems() {
        List<String> selectedIngredients = new ArrayList<>();

        if (delegateIngredientAdapter != null) {
            selectedIngredients = delegateIngredientAdapter.getCheckedIngredients();
        }

        if (selectedIngredients.isEmpty()) {
            Toast.makeText(this, "Please select ingredients", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show contact dialog
        showContactDialog(selectedIngredients);
    }

    private void showContactDialog(List<String> selectedIngredients) {
        View view = getLayoutInflater().inflate(R.layout.dialog_contact_details, null);
        TextInputEditText numberInput = view.findViewById(R.id.numberInput);
        TextInputEditText emailInput = view.findViewById(R.id.emailInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Share Shopping List")
                .setView(view)
                .setPositiveButton("Share", (dialog, which) -> {
                    String number = numberInput.getText().toString().trim();
                    String email = emailInput.getText().toString().trim();
                    
                    Contact contact = new Contact("Contact", 
                            number.isEmpty() ? null : number, 
                            email.isEmpty() ? null : email);
                    
                    // Share the request
                    sendRequest(selectedIngredients, contact);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendRequest(List<String> selectedIngredients, Contact contact) {
        StringBuilder message = new StringBuilder("Will you shop for me? Here's the list:\n\n");
        
        // Get pricing, quantity and unit data
        Map<String, Float> itemPrices = delegateIngredientAdapter.getItemPrices();
        Map<String, Float> itemQuantities = delegateIngredientAdapter.getItemQuantities();
        Map<String, String> itemUnits = delegateIngredientAdapter.getItemUnits();
        
        // Build message with all ingredients
        float totalPrice = 0;
        message.append("Shopping List:\n");
        for (String ingredient : selectedIngredients) {
            float price = itemPrices.getOrDefault(ingredient, 0.0f);
            float quantity = itemQuantities.getOrDefault(ingredient, 1.0f);
            String unit = itemUnits.getOrDefault(ingredient, "pcs");
            
            message.append("- ").append(ingredient)
                   .append(": ").append(quantity).append(" ").append(unit);
            
            if (price > 0) {
                message.append(" (NPR ").append(String.format("%.2f", price)).append(")");
                totalPrice += price;
            }
            
            message.append("\n");
        }
        
        // Add total price if any prices were entered
        if (totalPrice > 0) {
            message.append("\nTotal: NPR ").append(String.format("%.2f", totalPrice));
        }

        // Create sharing intent
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Shopping List Request");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, message.toString());
        
        // Add phone number and email as extras if available
        if (contact != null) {
            if (contact.number != null && !contact.number.isEmpty()) {
                sharingIntent.putExtra("address", contact.number);
            }
            if (contact.email != null && !contact.email.isEmpty()) {
                sharingIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{contact.email});
            }
        }
        
        // Show chooser dialog
        startActivity(Intent.createChooser(sharingIntent, "Share Shopping List via"));
    }
}