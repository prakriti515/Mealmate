package edu.prakriti.mealmate;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;

public class GeoTagActivityTest {

    @Rule
    public ActivityScenarioRule<GeoTagActivity> activityRule =
            new ActivityScenarioRule<>(GeoTagActivity.class);

    @Test
    public void testEmptyStoreFields_ShowsSnackbar() {
        // Click the Save Store button without filling anything
        onView(withId(R.id.saveStoreButton)).perform(click());

        // Check that the correct Snackbar message is shown
        onView(withText("Please enter a store name!"))
                .check(matches(withText("Please enter a store name!")));
    }


}
