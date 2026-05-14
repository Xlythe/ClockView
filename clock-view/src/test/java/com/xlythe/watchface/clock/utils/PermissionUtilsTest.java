package com.xlythe.watchface.clock.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class PermissionUtilsTest {

    @Test
    public void testHasPermissionsGranted() {
        Context mockContext = mock(Context.class);
        when(mockContext.checkPermission(eq("android.permission.CAMERA"), anyInt(), anyInt()))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        assertTrue(PermissionUtils.hasPermissions(mockContext, "android.permission.CAMERA"));
    }

    @Test
    public void testHasPermissionsDenied() {
        Context mockContext = mock(Context.class);
        when(mockContext.checkPermission(eq("android.permission.CAMERA"), anyInt(), anyInt()))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        assertFalse(PermissionUtils.hasPermissions(mockContext, "android.permission.CAMERA"));
    }

    @Test
    public void testHasPermissionsMultiple() {
        Context mockContext = mock(Context.class);
        when(mockContext.checkPermission(eq("android.permission.CAMERA"), anyInt(), anyInt()))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mockContext.checkPermission(eq("android.permission.RECORD_AUDIO"), anyInt(), anyInt()))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        assertFalse(PermissionUtils.hasPermissions(mockContext, "android.permission.CAMERA", "android.permission.RECORD_AUDIO"));
    }

    @Test
    public void testHasPermissionsEmpty() {
        Context mockContext = mock(Context.class);
        assertTrue(PermissionUtils.hasPermissions(mockContext));
    }
}
