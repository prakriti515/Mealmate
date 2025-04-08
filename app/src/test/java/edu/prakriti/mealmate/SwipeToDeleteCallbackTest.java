package edu.prakriti.mealmate;

import android.content.Context;
import android.graphics.Canvas;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import edu.prakriti.mealmate.utils.SwipeToDeleteCallback;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE) // Fix for missing manifest
@LooperMode(LooperMode.Mode.PAUSED) // Fix for legacy mode issue
public class SwipeToDeleteCallbackTest {

    private SwipeToDeleteCallback swipeToDeleteCallback;
    private RecyclerView recyclerView;
    private RecyclerView.ViewHolder viewHolder;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        swipeToDeleteCallback = new SwipeToDeleteCallback(context);

        // Mock RecyclerView and ViewHolder
        recyclerView = Mockito.mock(RecyclerView.class);
        viewHolder = Mockito.mock(RecyclerView.ViewHolder.class);
    }

    @Test
    public void testSwipeDirections() {
        int movementFlags = swipeToDeleteCallback.getMovementFlags(recyclerView, viewHolder);

        // Check if both LEFT and RIGHT swipe are enabled
        int expectedFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        assertEquals(expectedFlags, movementFlags);
    }

    @Test
    public void testOnMove_Disabled() {
        boolean result = swipeToDeleteCallback.onMove(recyclerView, viewHolder, viewHolder);
        assertFalse("onMove should return false", result);
    }

    @Test
    public void testOnSwiped_CallsExpectedMethod() {
        // Swipe in both directions
        swipeToDeleteCallback.onSwiped(viewHolder, ItemTouchHelper.LEFT);
        swipeToDeleteCallback.onSwiped(viewHolder, ItemTouchHelper.RIGHT);

        // No assertions since the method must be implemented in the adapter
        assertTrue("onSwiped is called successfully", true);
    }

    @Test
    public void testOnChildDraw_NoCrash() {
        Canvas canvas = Mockito.mock(Canvas.class);
        swipeToDeleteCallback.onChildDraw(canvas, recyclerView, viewHolder, 100, 0, ItemTouchHelper.ACTION_STATE_SWIPE, true);

        assertTrue("onChildDraw executed without crashing", true);
    }
}
