package com.xlythe.view.clock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
    private Drawable mDrawable;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mClockHandView = new ClockHandView(mContext);
        mDrawable = new ColorDrawable(0xFFFFFFFF);
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

    @Test
    public void testHandBoundsAreOffByDefault() {
        assertFalse(mClockHandView.hasHandBounds());
    }

    @Test
    public void testSetHandBoundsInvalidatesOnceForTheSameValues() {
        ClockHandView spyView = spy(mClockHandView);

        spyView.setHandBounds(0f, 0.201f, 1f, 0.297f);
        verify(spyView).postInvalidate();
        assertTrue(spyView.hasHandBounds());

        clearInvocations(spyView);
        spyView.setHandBounds(0f, 0.201f, 1f, 0.297f);
        verify(spyView, never()).postInvalidate();
    }

    @Test
    public void testClearHandBounds() {
        mClockHandView.setHandBounds(0f, 0.201f, 1f, 0.297f);
        mClockHandView.clearHandBounds();

        assertFalse(mClockHandView.hasHandBounds());
    }

    @Test
    public void testHandBoundsPlaceTheArtWhereTheCropCameFrom() {
        mClockHandView.setImageDrawable(mDrawable);
        mClockHandView.layout(0, 0, 1000, 1000);
        // A hand cut from rows 201 to 498 of a thousand-pixel canvas.
        mClockHandView.setHandBounds(0f, 0.201f, 1f, 0.297f);

        mClockHandView.onDraw(mock(Canvas.class));

        assertEquals(new Rect(0, 201, 1000, 498), mDrawable.getBounds());
    }

    @Test
    public void testHandBoundsStillTurnAboutTheMiddleOfTheFace() {
        ClockHandView spyView = spy(mClockHandView);
        spyView.setImageDrawable(mDrawable);
        spyView.layout(0, 0, 1000, 1000);
        // Art that stops short of the middle: the pivot is outside it, and unmoved.
        spyView.setHandBounds(0f, 0.193f, 1f, 0.270f);
        Canvas mockCanvas = mock(Canvas.class);

        spyView.setRotation(90f);
        spyView.onDraw(mockCanvas);

        verify(mockCanvas).rotate(eq(90f), eq(500f), eq(500f));
    }

    @Test
    public void testHandBoundsReadFromXml() {
        AttributeSet attrs = Robolectric.buildAttributeSet()
                .addAttribute(R.attr.handOffsetY, "0.201")
                .addAttribute(R.attr.handHeight, "0.297")
                .build();

        ClockHandView view = new ClockHandView(mContext, attrs);
        view.setImageDrawable(mDrawable);
        view.layout(0, 0, 1000, 1000);
        view.onDraw(mock(Canvas.class));

        assertTrue(view.hasHandBounds());
        // The axes left out keep their defaults, so the art still spans the full width.
        assertEquals(new Rect(0, 201, 1000, 498), mDrawable.getBounds());
    }

    @Test
    public void testDrawingIsSkippedWithoutADrawable() {
        mClockHandView.layout(0, 0, 1000, 1000);
        mClockHandView.setHandBounds(0f, 0.201f, 1f, 0.297f);

        mClockHandView.onDraw(mock(Canvas.class));
    }
}
