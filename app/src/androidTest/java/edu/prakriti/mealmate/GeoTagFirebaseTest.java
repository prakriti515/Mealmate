package edu.prakriti.mealmate;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.CollectionReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.prakriti.mealmate.utils.APIKey;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GeoTagFirebaseTest {

    @Rule
    public ActivityScenarioRule<GeoTagActivity> activityRule =
            new ActivityScenarioRule<>(GeoTagActivity.class);

    @Test
    public void testMockFirebaseSaveStore_Success() {
        // Mock Firestore
        FirebaseFirestore mockFirestore = mock(FirebaseFirestore.class);
        CollectionReference mockCollection = mock(CollectionReference.class);
        DocumentReference mockDocument = mock(DocumentReference.class);

        // Mock chain
        when(mockFirestore.collection("favstore")).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocument);

        // Simulate success callback
        doAnswer(invocation -> {
            // Success callback triggered
            System.out.println("Mock Save Success");
            return null;
        }).when(mockDocument).set(anyMap());

        // Call the logic here → Normally you'd pass mockFirestore to your method
        mockFirestore.collection("favstore").document("123").set(new java.util.HashMap<>());

        // Verify set() called
        verify(mockFirestore.collection("favstore").document("123")).set(anyMap());
    }

    @Test
    public void testMockFirebaseSaveStore_Failure() {
        FirebaseFirestore mockFirestore = mock(FirebaseFirestore.class);
        CollectionReference mockCollection = mock(CollectionReference.class);
        DocumentReference mockDocument = mock(DocumentReference.class);

        when(mockFirestore.collection("favstore")).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocument);

        // Simulate failure
        doThrow(new RuntimeException("Save failed"))
                .when(mockDocument).set(anyMap());

        try {
            mockFirestore.collection("favstore").document("123").set(new java.util.HashMap<>());
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("Save failed", e.getMessage());
        }

        verify(mockFirestore.collection("favstore").document("123")).set(anyMap());
    }
}
