package edu.prakriti.mealmate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import edu.prakriti.mealmate.models.GroceryIngredient;
import edu.prakriti.mealmate.utils.GroceryDatabaseHelper;

@RunWith(MockitoJUnitRunner.class)
public class GroceryDatabaseHelperTest {

    @Mock
    private SQLiteDatabase mockDatabase;

    @Mock
    private Cursor mockCursor;

    private GroceryDatabaseHelper groceryDatabaseHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        groceryDatabaseHelper = mock(GroceryDatabaseHelper.class);
    }

    @Test
    public void testAddGroceryIngredient() {
        GroceryIngredient ingredient = new GroceryIngredient(1, "Milk", "Dairy", 2.0, "Liters", 5.0, false, "2025-04-01", "Breakfast");

        when(mockDatabase.insert(anyString(), isNull(), any(ContentValues.class))).thenReturn(1L);
        long id = groceryDatabaseHelper.addGroceryIngredient(ingredient);

        assertEquals(1L, id);
        verify(groceryDatabaseHelper).addGroceryIngredient(ingredient);
    }

    @Test
    public void testGetGroceryIngredientsByDate() {
        String date = "2025-04-01";
        List<GroceryIngredient> expectedList = new ArrayList<>();
        expectedList.add(new GroceryIngredient(1, "Eggs", "Protein", 12.0, "Pieces", 3.0, false, date, "Breakfast"));

        when(groceryDatabaseHelper.getGroceryIngredientsByDate(date)).thenReturn(expectedList);
        List<GroceryIngredient> actualList = groceryDatabaseHelper.getGroceryIngredientsByDate(date);

        assertNotNull(actualList);
        assertEquals(1, actualList.size());
        assertEquals("Eggs", actualList.get(0).getName());
        verify(groceryDatabaseHelper).getGroceryIngredientsByDate(date);
    }
}
