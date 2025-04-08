package edu.prakriti.mealmate;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class PermissionTest {

    @Test
    public void testPermissionGranted() {
        Context context = ApplicationProvider.getApplicationContext();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        shadowApplication.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);

        int permissionCheck = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        assertEquals(PackageManager.PERMISSION_GRANTED, permissionCheck);
    }
}
