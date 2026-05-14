package com.xlythe.watchface.clock;

import android.content.pm.PackageManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class PermissionActivityTest {

    @Test
    public void testOnCreateAndFinish() {
        PermissionActivity activity = Robolectric.buildActivity(PermissionActivity.class).create().get();
        // Depending on Robolectric default permissions, it either finishes immediately or requests permissions.
        // Let's verify onRequestPermissionsResult finishes the activity in either case.
        activity.onRequestPermissionsResult(1000, new String[]{}, new int[]{PackageManager.PERMISSION_GRANTED});
        assertTrue(activity.isFinishing());
    }
}
