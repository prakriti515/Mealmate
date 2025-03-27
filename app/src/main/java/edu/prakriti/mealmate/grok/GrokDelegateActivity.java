package edu.prakriti.mealmate.grok;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.utils.GroceryDatabaseHelper;

public class GrokDelegateActivity extends AppCompatActivity {
    private GroceryDatabaseHelper dbHelper;
    private RecyclerView mealRecyclerView, ingredientRecyclerView;
    private ChipGroup contactChipGroup;
    private GrokMealAdapter mealAdapter;
    private GrokIngredientAdapter ingredientAdapter;
    private List<String> selectedCategories = new ArrayList<>();
    private List<GrokIngredient> allIngredients = new ArrayList<>(); // All available ingredients
    private List<GrokIngredient> selectedIngredients = new ArrayList<>(); // Ingredients with prices set
    private List<Contact> selectedContacts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grok_delegate);

        dbHelper = new GroceryDatabaseHelper(this);
        mealRecyclerView = findViewById(R.id.mealRecyclerView);
        ingredientRecyclerView = findViewById(R.id.ingredientRecyclerView);
        contactChipGroup = findViewById(R.id.contactChipGroup);

        mealRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        ingredientRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.addContactButton).setOnClickListener(v -> showAddContactDialog());
        findViewById(R.id.sendRequestButton).setOnClickListener(v -> sendRequest());

        loadMeals();
    }

    private void loadMeals() {
        Map<String, Map<String, List<String>>> weeklyItems = dbHelper.getGroceryItemsForWeek();
        List<String> categories = new ArrayList<>(weeklyItems.keySet());
        mealAdapter = new GrokMealAdapter(categories, this::updateIngredients);
        mealRecyclerView.setAdapter(mealAdapter);

        // Preload all ingredients for reference
        allIngredients.clear();
        for (String category : categories) {
            Map<String, List<String>> dateMap = weeklyItems.get(category);
            if (dateMap != null) {
                for (String date : dateMap.keySet()) {
                    for (String itemName : dateMap.get(date)) {
                        boolean isPurchased = dbHelper.isItemPurchased(itemName, date);
                        allIngredients.add(new GrokIngredient(itemName, date, category, isPurchased, 0.0f));
                    }
                }
            }
        }
    }

    private void updateIngredients() {
        selectedCategories.clear();
        for (int i = 0; i < mealAdapter.categories.size(); i++) {
            if (mealAdapter.selectedCategories.contains(mealAdapter.categories.get(i))) {
                selectedCategories.add(mealAdapter.categories.get(i));
            }
        }

        List<GrokIngredient> ingredients = new ArrayList<>();
        for (GrokIngredient ingredient : allIngredients) {
            if (selectedCategories.contains(ingredient.category)) {
                float price = 0.0f;
                for (GrokIngredient selected : selectedIngredients) {
                    if (selected.name.equals(ingredient.name) && selected.date.equals(ingredient.date)) {
                        price = selected.price;
                        break;
                    }
                }
                ingredients.add(new GrokIngredient(ingredient.name, ingredient.date, ingredient.category, ingredient.isPurchased, price));
            }
        }
        ingredientAdapter = new GrokIngredientAdapter(ingredients, selectedIngredients); // Pass selectedIngredients
        ingredientRecyclerView.setAdapter(ingredientAdapter);
    }

    private void showAddContactDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_contact, null);
        TextInputEditText numberInput = view.findViewById(R.id.numberInput);
        TextInputEditText emailInput = view.findViewById(R.id.emailInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Contact Details")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    String number = numberInput.getText().toString().trim();
                    String email = emailInput.getText().toString().trim();
                    
                    if (number.isEmpty()) {
                        Toast.makeText(this, "Phone number is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Contact contact = new Contact("Contact " + (selectedContacts.size() + 1), number, email.isEmpty() ? null : email);
                    selectedContacts.add(contact);
                    updateContactChips();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateContactChips() {
        contactChipGroup.removeAllViews();
        for (Contact contact : selectedContacts) {
            Chip chip = new Chip(this);
            String chipText = contact.number;
            if (contact.email != null && !contact.email.isEmpty()) {
                chipText += "\n" + contact.email;
            }
            chip.setText(chipText);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                selectedContacts.remove(contact);
                contactChipGroup.removeView(chip);
            });
            chip.setChipBackgroundColorResource(com.google.android.material.R.color.m3_chip_background_color);
            chip.setMaxLines(2);
            contactChipGroup.addView(chip);
        }
    }

    private void sendRequest() {
        if (selectedIngredients.isEmpty() || selectedContacts.isEmpty()) {
            Toast.makeText(this, "Please complete all selections", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder message = new StringBuilder("Will you shop for me? Here's the list:\n\n");
        float totalPrice = 0;
        
        // Group ingredients by recipe
        Map<String, List<GrokIngredient>> recipeGroups = new HashMap<>();
        for (GrokIngredient ingredient : selectedIngredients) {
            recipeGroups.computeIfAbsent(ingredient.category, k -> new ArrayList<>()).add(ingredient);
        }

        // Build message by recipe
        for (Map.Entry<String, List<GrokIngredient>> entry : recipeGroups.entrySet()) {
            message.append("Recipe: ").append(entry.getKey()).append("\n");
            for (GrokIngredient ingredient : entry.getValue()) {
                message.append("- ").append(ingredient.name)
                      .append(": ").append(ingredient.quantity).append(" ").append(ingredient.unit)
                      .append(" (NPR ").append(String.format("%.2f", ingredient.price)).append(")\n");
                totalPrice += ingredient.price;
            }
            message.append("\n");
        }
        
        message.append("Total: NPR ").append(String.format("%.2f", totalPrice));

        // Send SMS
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message.toString());
            for (Contact contact : selectedContacts) {
                smsManager.sendMultipartTextMessage(contact.number, null, parts, null, null);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
        }

        // Send Email if available
        for (Contact contact : selectedContacts) {
            if (contact.email != null && !contact.email.isEmpty()) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:" + contact.email));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Shopping List Request");
                emailIntent.putExtra(Intent.EXTRA_TEXT, message.toString());
                
                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(emailIntent, "Send email"));
                }
            }
        }

        Toast.makeText(this, "Request sent!", Toast.LENGTH_SHORT).show();
        finish();
    }
}