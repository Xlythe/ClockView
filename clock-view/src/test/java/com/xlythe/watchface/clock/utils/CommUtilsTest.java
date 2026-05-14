package com.xlythe.watchface.clock.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class CommUtilsTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testUnicast() throws Exception {
        CommUtils.unicast(mContext, "node_id", "/test/path", "test_message");
        Thread.sleep(100); // Allow background executor to process
    }

    @Test
    public void testPutWithActivityContext() throws Exception {
        Activity mockActivity = mock(Activity.class);
        Application mockApplication = mock(Application.class);
        when(mockActivity.getApplication()).thenReturn(mockApplication);
        when(mockActivity.getApplicationContext()).thenReturn(mContext);

        CommUtils.put(mockActivity, "test_key", "test_value");
        Thread.sleep(100); // Allow background executor to process

        // Verify lifecycle callbacks are registered
        ArgumentCaptor<Application.ActivityLifecycleCallbacks> captor = ArgumentCaptor.forClass(Application.ActivityLifecycleCallbacks.class);
        verify(mockApplication).registerActivityLifecycleCallbacks(captor.capture());

        Application.ActivityLifecycleCallbacks callbacks = captor.getValue();
        callbacks.onActivityCreated(mockActivity, new Bundle());
        callbacks.onActivityStarted(mockActivity);
        callbacks.onActivityResumed(mockActivity);
        callbacks.onActivityPaused(mockActivity);
        callbacks.onActivitySaveInstanceState(mockActivity, new Bundle());
        callbacks.onActivityStopped(mockActivity);
        callbacks.onActivityDestroyed(mockActivity);
    }

    @Test
    public void testGetWithCallback() throws Exception {
        CommUtils.Callback mockCallback = mock(CommUtils.Callback.class);
        CommUtils.get(mContext, "test_key", mockCallback);
        Thread.sleep(100); // Allow background executor to process
    }

    @Test
    public void testGetFromDataEventBuffer() {
        DataEventBuffer mockBuffer = mock(DataEventBuffer.class);
        DataEvent mockEvent = mock(DataEvent.class);
        DataItem mockDataItem = mock(DataItem.class);

        Uri uri = new Uri.Builder().scheme("wear").authority("node").path("/test_key").build();
        when(mockDataItem.getUri()).thenReturn(uri);
        when(mockDataItem.getData()).thenReturn("test_value".getBytes());
        when(mockEvent.getDataItem()).thenReturn(mockDataItem);

        when(mockBuffer.iterator()).thenReturn(Arrays.asList(mockEvent).iterator());

        String result = CommUtils.get(mockBuffer, "test_key");
        assertEquals("test_value", result);
    }

    @Test
    public void testGetFromDataEventBufferEmptyValue() {
        DataEventBuffer mockBuffer = mock(DataEventBuffer.class);
        DataEvent mockEvent = mock(DataEvent.class);
        DataItem mockDataItem = mock(DataItem.class);

        Uri uri = new Uri.Builder().scheme("wear").authority("node").path("/test_key").build();
        when(mockDataItem.getUri()).thenReturn(uri);
        when(mockDataItem.getData()).thenReturn("com.xlythe.watchface.clock.utils.EMPTY_VALUE".getBytes());
        when(mockEvent.getDataItem()).thenReturn(mockDataItem);

        when(mockBuffer.iterator()).thenReturn(Arrays.asList(mockEvent).iterator());

        String result = CommUtils.get(mockBuffer, "test_key");
        assertNull(result);
    }

    @Test
    public void testGetFromDataEventBufferNotFound() {
        DataEventBuffer mockBuffer = mock(DataEventBuffer.class);
        DataEvent mockEvent = mock(DataEvent.class);
        DataItem mockDataItem = mock(DataItem.class);

        Uri uri = new Uri.Builder().scheme("wear").authority("node").path("/other_key").build();
        when(mockDataItem.getUri()).thenReturn(uri);
        when(mockDataItem.getData()).thenReturn("test_value".getBytes());
        when(mockEvent.getDataItem()).thenReturn(mockDataItem);

        when(mockBuffer.iterator()).thenReturn(Arrays.asList(mockEvent).iterator());

        String result = CommUtils.get(mockBuffer, "test_key");
        assertNull(result);
    }
}
