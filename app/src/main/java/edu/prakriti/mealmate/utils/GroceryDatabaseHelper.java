package edu.prakriti.mealmate.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.prakriti.mealmate.models.GroceryIngredient;

public class GroceryDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "grocery.db";
    private static final int DATABASE_VERSION = 2; // Incremented for schema update

    // Table and Columns
    private static final String TABLE_GROCERY = "grocery_items";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_IS_PURCHASED = "isPurchased";
    private static final String COLUMN_QUANTITY = "quantity";
    private static final String COLUMN_UNIT = "unit";
    private static final String COLUMN_PRICE = "price";
    private static final String COLUMN_MEAL_NAME = "meal_name";

    public GroceryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_GROCERY + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_CATEGORY + " TEXT DEFAULT 'Wish List', " +
                COLUMN_DATE + " TEXT, " +
                COLUMN_IS_PURCHASED + " INTEGER DEFAULT 0, " +
                COLUMN_QUANTITY + " REAL DEFAULT 1.0, " +
                COLUMN_UNIT + " TEXT DEFAULT '', " +
                COLUMN_PRICE + " REAL DEFAULT 0.0, " +
                COLUMN_MEAL_NAME + " TEXT DEFAULT '')";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add new columns
            db.execSQL("ALTER TABLE " + TABLE_GROCERY + " ADD COLUMN " + COLUMN_QUANTITY + " REAL DEFAULT 1.0");
            db.execSQL("ALTER TABLE " + TABLE_GROCERY + " ADD COLUMN " + COLUMN_UNIT + " TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_GROCERY + " ADD COLUMN " + COLUMN_PRICE + " REAL DEFAULT 0.0");
            db.execSQL("ALTER TABLE " + TABLE_GROCERY + " ADD COLUMN " + COLUMN_MEAL_NAME + " TEXT DEFAULT ''");
        }
    }

    // Add Grocery Item
    public void addGroceryItem(String name, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_CATEGORY, "Wish List");
        db.insert(TABLE_GROCERY, null, values);
        db.close();
    }

    public void addGroceryItem(String name, String date, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_CATEGORY, category);
        db.insert(TABLE_GROCERY, null, values);
        db.close();
    }

    // Add GroceryIngredient with all details
    public long addGroceryIngredient(GroceryIngredient ingredient) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, ingredient.getName());
        values.put(COLUMN_CATEGORY, ingredient.getCategory());
        values.put(COLUMN_DATE, ingredient.getDate());
        values.put(COLUMN_IS_PURCHASED, ingredient.isPurchased() ? 1 : 0);
        values.put(COLUMN_QUANTITY, ingredient.getQuantity());
        values.put(COLUMN_UNIT, ingredient.getUnit());
        values.put(COLUMN_PRICE, ingredient.getPrice());
        values.put(COLUMN_MEAL_NAME, ingredient.getMealName());
        
        long id = db.insert(TABLE_GROCERY, null, values);
        db.close();
        return id;
    }

    // Get Grocery Items by Date as GroceryIngredient objects
    public List<GroceryIngredient> getGroceryIngredientsByDate(String date) {
        List<GroceryIngredient> ingredientList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String[] columns = {
            COLUMN_ID, COLUMN_NAME, COLUMN_CATEGORY, COLUMN_IS_PURCHASED,
            COLUMN_QUANTITY, COLUMN_UNIT, COLUMN_PRICE, COLUMN_MEAL_NAME
        };
        
        Cursor cursor = db.query(TABLE_GROCERY, columns,
                COLUMN_DATE + "=?",
                new String[]{date},
                null, null, COLUMN_CATEGORY + " ASC");

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                boolean isPurchased = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PURCHASED)) == 1;
                
                // Get additional details with defaults if columns don't exist
                double quantity = 1.0;
                String unit = "";
                double price = 0.0;
                String mealName = "";
                
                try {
                    quantity = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY));
                    unit = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UNIT));
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE));
                    mealName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_NAME));
                } catch (IllegalArgumentException e) {
                    // Column doesn't exist in older database versions
                }
                
                GroceryIngredient ingredient = new GroceryIngredient(
                    id, name, category, quantity, unit, price, isPurchased, date, mealName
                );
                ingredientList.add(ingredient);
                
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return ingredientList;
    }

    // Get all ingredients categorized by category
    public Map<String, List<GroceryIngredient>> getGroceryIngredientsByDateGrouped(String date) {
        Map<String, List<GroceryIngredient>> groceryMap = new HashMap<>();
        List<GroceryIngredient> ingredients = getGroceryIngredientsByDate(date);
        
        for (GroceryIngredient ingredient : ingredients) {
            String category = ingredient.getCategory();
            if (!groceryMap.containsKey(category)) {
                groceryMap.put(category, new ArrayList<>());
            }
            groceryMap.get(category).add(ingredient);
        }
        
        return groceryMap;
    }

    // Get Today's Date
    public String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
    
    /**
     * Updates a grocery ingredient in the database
     * @param ingredient The grocery ingredient to update
     * @return true if the update was successful, false otherwise
     */
    public boolean updateGroceryIngredient(GroceryIngredient ingredient) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, ingredient.getName());
        values.put(COLUMN_CATEGORY, ingredient.getCategory());
        values.put(COLUMN_QUANTITY, ingredient.getQuantity());
        values.put(COLUMN_UNIT, ingredient.getUnit());
        values.put(COLUMN_PRICE, ingredient.getPrice());
        values.put(COLUMN_IS_PURCHASED, ingredient.isPurchased() ? 1 : 0);
        values.put(COLUMN_DATE, ingredient.getDate());
        values.put(COLUMN_MEAL_NAME, ingredient.getMealName());

        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.update(
                TABLE_GROCERY,
                values,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(ingredient.getId())}
        );
        db.close();
        return rowsAffected > 0;
    }

    // Update Purchased Status of Item
    public void updateItemPurchasedStatus(String itemName, String date, boolean isPurchased) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_PURCHASED, isPurchased ? 1 : 0);
        db.update(TABLE_GROCERY, values, COLUMN_NAME + "=? AND " + COLUMN_DATE + "=?",
                new String[]{itemName, date});
        db.close();
    }

    // Update Purchased Status of Item by ID
    public void updateIngredientPurchasedStatus(long id, boolean isPurchased) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_PURCHASED, isPurchased ? 1 : 0);
        db.update(TABLE_GROCERY, values, COLUMN_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    // Delete a GroceryIngredient by ID
    public void deleteGroceryIngredient(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GROCERY, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Delete a grocery item by name and date
    public void deleteGroceryItem(String itemName, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GROCERY, COLUMN_NAME + "=? AND " + COLUMN_DATE + "=?", new String[]{itemName, date});
        db.close();
    }

    // Clear all grocery data
    public void clearGroceryData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_GROCERY);
        db.close();
    }

    // Get Total Count of Grocery Items
    // Get Total Count of Grocery Items for the Week (Today + Next 7 Days)
    public int getWeeklyGroceryItemCount() {
        SQLiteDatabase db = this.getReadableDatabase();

        // Get today's date
        String todayDate = getTodayDate();

        // Get the date 7 days from today
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 7);
        String endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        // Query for total count between today and the next 7 days
        String query = "SELECT COUNT(*) FROM " + TABLE_GROCERY +
                " WHERE " + COLUMN_DATE + " BETWEEN ? AND ?";

        Cursor cursor = db.rawQuery(query, new String[]{todayDate, endDate});

        int totalCount = 0;
        if (cursor.moveToFirst()) {
            totalCount = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return totalCount;
    }


    public boolean hasGroceryDataForWeek() {
        SQLiteDatabase db = this.getReadableDatabase();

        // Get today's date
        String todayDate = getTodayDate();

        // Get the date 7 days from today
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 7);
        String endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        // Query to check if there is any data between today and the next 7 days
        String query = "SELECT COUNT(*) FROM " + TABLE_GROCERY +
                " WHERE " + COLUMN_DATE + " BETWEEN ? AND ?";

        Cursor cursor = db.rawQuery(query, new String[]{todayDate, endDate});

        boolean hasData = false;
        if (cursor.moveToFirst()) {
            hasData = cursor.getInt(0) > 0;
        }
        cursor.close();
        db.close();
        return hasData;
    }

    // Check if Item Exists for Given Date
    public boolean isItemExistsForDate(String itemName, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_GROCERY,
                new String[]{COLUMN_NAME},
                COLUMN_NAME + "=? AND " + COLUMN_DATE + "=?",
                new String[]{itemName, date},
                null, null, null);

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // Check if Item is Purchased for a specific date
    public boolean isItemPurchased(String itemName, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_GROCERY,
                new String[]{COLUMN_IS_PURCHASED},
                COLUMN_NAME + "=? AND " + COLUMN_DATE + "=?",
                new String[]{itemName, date},
                null, null, null);

        boolean isPurchased = false;
        if (cursor.moveToFirst()) {
            isPurchased = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PURCHASED)) == 1;
        }
        cursor.close();
        db.close();
        return isPurchased;
    }

    // Get all Grocery Items for Week (Today + Next 7 Days) - for backwards compatibility
    public Map<String, Map<String, List<String>>> getGroceryItemsForWeek() {
        Map<String, Map<String, List<String>>> weeklyGroceryMap = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Get today's date
        String todayDate = getTodayDate();

        // Get the date 7 days from today
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        String endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        // Query for items between today and the next 7 days
        String query = "SELECT " + COLUMN_NAME + ", " + COLUMN_CATEGORY + ", " + COLUMN_DATE + ", " + COLUMN_IS_PURCHASED +
                " FROM " + TABLE_GROCERY +
                " WHERE " + COLUMN_DATE + " BETWEEN ? AND ? ORDER BY " + COLUMN_DATE + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{todayDate, endDate});

        if (cursor.moveToFirst()) {
            do {
                String itemName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
                boolean isPurchased = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PURCHASED)) == 1;

                // Group by Category then by Date
                if (!weeklyGroceryMap.containsKey(category)) {
                    weeklyGroceryMap.put(category, new HashMap<>());
                }
                Map<String, List<String>> dateMap = weeklyGroceryMap.get(category);

                // Group by Date under the same Category
                if (!dateMap.containsKey(date)) {
                    dateMap.put(date, new ArrayList<>());
                }
                dateMap.get(date).add(itemName + "|" + isPurchased); // Append purchased status to the item name
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return weeklyGroceryMap;
    }

    // Get unpurchased Grocery Items for Week - for backwards compatibility
    public Map<String, Map<String, List<String>>> getGroceryItemsForWeekUnpurchased() {
        Map<String, Map<String, List<String>>> weeklyGroceryMap = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Get today's date
        String todayDate = getTodayDate();

        // Get the date 7 days from today
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        String endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        // Query for unpurchased items between today and the next 7 days
        String query = "SELECT " + COLUMN_NAME + ", " + COLUMN_CATEGORY + ", " + COLUMN_DATE +
                " FROM " + TABLE_GROCERY +
                " WHERE " + COLUMN_DATE + " BETWEEN ? AND ? " +
                " AND " + COLUMN_IS_PURCHASED + " = 0" +   // Only Unpurchased Items
                " ORDER BY " + COLUMN_DATE + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{todayDate, endDate});

        if (cursor.moveToFirst()) {
            do {
                String itemName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));

                // Group by Category then by Date
                if (!weeklyGroceryMap.containsKey(category)) {
                    weeklyGroceryMap.put(category, new HashMap<>());
                }
                Map<String, List<String>> dateMap = weeklyGroceryMap.get(category);

                // Group by Date under the same Category
                if (!dateMap.containsKey(date)) {
                    dateMap.put(date, new ArrayList<>());
                }
                dateMap.get(date).add(itemName);

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return weeklyGroceryMap;
    }

    // Get Tomorrow's Date - for backwards compatibility
    public String getTomorrowDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date today = new Date();
        long tomorrowMillis = today.getTime() + (1000 * 60 * 60 * 24);
        return sdf.format(new Date(tomorrowMillis));
    }

    public void removeGroceryItem(String itemName, String date) {
    }
}
