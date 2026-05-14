package com.xlythe.view.clock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ClockHandViewTest {

    private Context mContext;
    private ClockHandView mClockHandView;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mClockHandView = new ClockHandView(mContext);
    }

    @Test
    public void testConstructors() {
        ClockHandView view1 = new ClockHandView(mContext);
        assertNotNull(view1);

        AttributeSet attrs = Robolectric.buildAttributeSet().build();
        ClockHandView view2 = new ClockHandView(mContext, attrs);
        assertNotNull(view2);

        ClockHandView view3 = new ClockHandView(mContext, attrs, 0);
        assertNotNull(view3);

        ClockHandView view4 = new ClockHandView(mContext, attrs, 0, 0);
        assertNotNull(view4);
    }

    @Test
    public void testSetImageResource() {
        mClockHandView.setImageResource(android.R.drawable.sym_def_app_icon);
        Drawable drawable = mClockHandView.getDrawable();
        assertNotNull(drawable);
    }

    @Test
    public void testSetRotationInvalidation() {
        ClockHandView spyView = spy(mClockHandView);

        // Initial rotation is NaN. Setting a valid rotation should invalidate.
        spyView.setRotation(45f);
        verify(spyView).postInvalidate();

        // Setting the same rotation again should not invalidate a second time.
        clearInvocations(spyView);
        spyView.setRotation(45f);
        verify(spyView, never()).postInvalidate();

        // Setting a different rotation should invalidate again.
        spyView.setRotation(90f);
        verify(spyView).postInvalidate();
    }

    @Test
    public void testOnDrawWithoutRotation() {
        ClockHandView spyView = spy(mClockHandView);
        Canvas mockCanvas = mock(Canvas.class);

        // By default, rotation is NaN, so rotate() should not be called
        spyView.onDraw(mockCanvas);

        verify(mockCanvas).save();
        verify(mockCanvas, never()).rotate(anyFloat(), anyFloat(), anyFloat());
        verify(mockCanvas).restore();
    }

    @Test
    public void testOnDrawWithRotation() {
        ClockHandView spyView = spy(mClockHandView);
        spyView.layout(0, 0, 100, 200);
        Canvas mockCanvas = mock(Canvas.class);

        spyView.setRotation(45f);
        spyView.onDraw(mockCanvas);

        verify(mockCanvas).save();
        verify(mockCanvas).rotate(eq(45f), eq(50f), eq(100f));
        verify(mockCanvas).restore();
    }
}
