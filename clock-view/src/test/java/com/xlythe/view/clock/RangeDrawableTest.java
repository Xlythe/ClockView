package com.xlythe.view.clock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class RangeDrawableTest {

    private Context mContext;
    private Drawable mSampleIcon;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mSampleIcon = spy(new ColorDrawable(Color.BLUE));
    }

    @Test
    public void testBuilderAndConstructors() {
        RangeDrawable drawable1 = new RangeDrawable.Builder(mContext)
                .icon(mSampleIcon)
                .text("Test Text")
                .title("Test Title")
                .style(ComplicationDrawable.Style.LINE)
                .range(0f, 100f)
                .value(50f)
                .colors(new int[]{Color.RED, Color.GREEN, Color.BLUE}, true)
                .build();

        assertNotNull(drawable1);
        assertEquals(mSampleIcon, drawable1.getIcon());
        assertEquals(ComplicationDrawable.Style.LINE, drawable1.getStyle());

        // Test colors with empty array
        RangeDrawable drawable2 = new RangeDrawable.Builder(mContext)
                .range(10f, 50f)
                .value(20f)
                .colors(new int[0], false)
                .build();

        assertNotNull(drawable2);
    }

    @Test
    public void testBuilderStyles() {
        RangeDrawable drawableFill = new RangeDrawable.Builder(mContext)
                .style(ComplicationDrawable.Style.FILL)
                .range(0f, 100f)
                .value(50f)
                .build();
        assertEquals(ComplicationDrawable.Style.FILL, drawableFill.getStyle());

        RangeDrawable drawableDot = new RangeDrawable.Builder(mContext)
                .style(ComplicationDrawable.Style.DOT)
                .range(0f, 100f)
                .value(50f)
                .build();
        assertEquals(ComplicationDrawable.Style.DOT, drawableDot.getStyle());

        RangeDrawable drawableEmpty = new RangeDrawable.Builder(mContext)
                .style(ComplicationDrawable.Style.EMPTY)
                .range(0f, 100f)
                .value(50f)
                .build();
        assertEquals(ComplicationDrawable.Style.EMPTY, drawableEmpty.getStyle());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderInvalidRange() {
        new RangeDrawable.Builder(mContext).range(100f, 50f);
    }

    @Test
    public void testDrawDefault() {
        RangeDrawable drawable = new RangeDrawable.Builder(mContext)
                .range(0f, 100f)
                .value(50f)
                .build();
        drawable.setBounds(0, 0, 200, 200);

        Canvas mockCanvas = mock(Canvas.class);
        drawable.draw(mockCanvas);

        verify(mockCanvas, atLeastOnce()).save();
        verify(mockCanvas, atLeastOnce()).translate(any(Float.class), any(Float.class));
        // Should draw background path and foreground path
        ArgumentCaptor<Paint> paintCaptor = ArgumentCaptor.forClass(Paint.class);
        verify(mockCanvas, times(2)).drawPath(any(Path.class), paintCaptor.capture());
        verify(mockCanvas, atLeastOnce()).restore();

        // Foreground paint should have a path effect for progress < 1f
        Paint foregroundPaint = paintCaptor.getAllValues().get(1);
        assertNotNull(foregroundPaint.getPathEffect());
    }

    @Test
    public void testDrawDefaultMaxProgress() {
        RangeDrawable drawable = new RangeDrawable.Builder(mContext)
                .range(0f, 100f)
                .value(100f) // max progress
                .build();
        drawable.setBounds(0, 0, 200, 200);

        Canvas mockCanvas = mock(Canvas.class);
        drawable.draw(mockCanvas);

        ArgumentCaptor<Paint> paintCaptor = ArgumentCaptor.forClass(Paint.class);
        verify(mockCanvas, times(2)).drawPath(any(Path.class), paintCaptor.capture());

        // Foreground paint should NOT have a path effect when progress == 1f
        Paint foregroundPaint = paintCaptor.getAllValues().get(1);
        assertNull(foregroundPaint.getPathEffect());
    }

    @Test
    public void testDrawValueExceedingMax() {
        RangeDrawable drawable = new RangeDrawable.Builder(mContext)
                .range(0f, 100f)
                .value(150f) // value > max
                .build();
        drawable.setBounds(0, 0, 200, 200);

        Canvas mockCanvas = mock(Canvas.class);
        drawable.draw(mockCanvas);

        ArgumentCaptor<Paint> paintCaptor = ArgumentCaptor.forClass(Paint.class);
        verify(mockCanvas, times(2)).drawPath(any(Path.class), paintCaptor.capture());

        // getProgress() should clamp to 1f, so path effect should be null
        Paint foregroundPaint = paintCaptor.getAllValues().get(1);
        assertNull(foregroundPaint.getPathEffect());
    }

    @Test
    public void testDrawMultiColoredSmooth() {
        RangeDrawable drawable = new RangeDrawable.Builder(mContext)
                .range(0f, 100f)
                .value(75f)
                .colors(new int[]{Color.RED, Color.YELLOW, Color.GREEN}, true)
                .build();
        drawable.setBounds(0, 0, 200, 200);

        Canvas mockCanvas = mock(Canvas.class);
        drawable.draw(mockCanvas);

        ArgumentCaptor<Paint> paintCaptor = ArgumentCaptor.forClass(Paint.class);
        verify(mockCanvas, times(2)).drawPath(any(Path.class), paintCaptor.capture());

        // Second paint is foreground, should have a SweepGradient shader
        Paint foregroundPaint = paintCaptor.getAllValues().get(1);
        assertNotNull(foregroundPaint.getShader());
    }

    @Test
    public void testDrawMultiColoredSteps() {
        RangeDrawable drawable = new RangeDrawable.Builder(mContext)
                .range(0f, 100f)
                .value(40f)
                .colors(new int[]{Color.RED, Color.YELLOW, Color.GREEN}, false)
                .build();
        drawable.setBounds(0, 0, 200, 200);

        Canvas mockCanvas = mock(Canvas.class);
        drawable.draw(mockCanvas);

        // With 3 colors and value 40 (progress 0.4), stepSize is 0.333.
        // stepsToDraw = ceil(0.4 / 0.333) = 2.
        // So 1 background path + 2 foreground step paths = 3 drawPath calls.
        verify(mockCanvas, times(3)).drawPath(any(Path.class), any(Paint.class));
    }

    @Test
    public void testDrawMultiColoredStepsMaxProgress() {
        RangeDrawable drawable = new RangeDrawable.Builder(mContext)
                .range(0f, 100f)
                .value(100f) // max progress
                .colors(new int[]{Color.RED, Color.YELLOW, Color.GREEN}, false)
                .build();
        drawable.setBounds(0, 0, 200, 200);

        Canvas mockCanvas = mock(Canvas.class);
        drawable.draw(mockCanvas);

        // 1 background path + 3 foreground step paths = 4 drawPath calls.
        ArgumentCaptor<Paint> paintCaptor = ArgumentCaptor.forClass(Paint.class);
        verify(mockCanvas, times(4)).drawPath(any(Path.class), paintCaptor.capture());

        // Check that path effect is null for the last progress step (since progress == 1f)
        // Note: mMultiColoredForegroundPaints is reversed, so the paint with progress 1f is at index 0 of mMultiColoredForegroundPaints, which is drawn first among foreground paints (index 1 in paintCaptor).
        Paint maxProgressPaint = paintCaptor.getAllValues().get(1);
        assertNull(maxProgressPaint.getPathEffect());
    }

    @Test
    public void testSetTintList() {
        // Case 1: mColors == null
        RangeDrawable drawable1 = new RangeDrawable.Builder(mContext)
                .range(0f, 100f)
                .value(50f)
                .build();
        drawable1.setBounds(0, 0, 200, 200);

        drawable1.setTintList(ColorStateList.valueOf(Color.MAGENTA));
        Canvas mockCanvas1 = mock(Canvas.class);
        drawable1.draw(mockCanvas1);

        ArgumentCaptor<Paint> paintCaptor1 = ArgumentCaptor.forClass(Paint.class);
        verify(mockCanvas1, times(2)).drawPath(any(Path.class), paintCaptor1.capture());
        assertEquals(Color.MAGENTA, paintCaptor1.getAllValues().get(1).getColor());

        // Test setTintList(null) fallback
        drawable1.setTintList(null);
        Canvas mockCanvasNull = mock(Canvas.class);
        drawable1.draw(mockCanvasNull);
        ArgumentCaptor<Paint> paintCaptorNull = ArgumentCaptor.forClass(Paint.class);
        verify(mockCanvasNull, times(2)).drawPath(any(Path.class), paintCaptorNull.capture());
        assertEquals(ComplicationDrawable.DEFAULT_COLOR, paintCaptorNull.getAllValues().get(1).getColor());

        // Case 2: mColors != null
        RangeDrawable drawable2 = new RangeDrawable.Builder(mContext)
                .range(0f, 100f)
                .value(50f)
                .colors(new int[]{Color.RED, Color.BLUE}, true)
                .build();
        drawable2.setBounds(0, 0, 200, 200);

        drawable2.setTintList(ColorStateList.valueOf(Color.CYAN));
        Canvas mockCanvas2 = mock(Canvas.class);
        drawable2.draw(mockCanvas2);

        ArgumentCaptor<Paint> paintCaptor2 = ArgumentCaptor.forClass(Paint.class);
        verify(mockCanvas2, times(2)).drawPath(any(Path.class), paintCaptor2.capture());
        // Foreground paint should have shader, color not overridden by cyan
        assertNotNull(paintCaptor2.getAllValues().get(1).getShader());
    }

    @Test
    public void testSweepGradientCollision() {
        int[] colors = new int[]{Color.RED, Color.GREEN, Color.BLUE};

        RangeDrawable drawable1 = new RangeDrawable.Builder(mContext)
                .range(0f, 100f)
                .value(50f)
                .colors(colors, true)
                .build();
        drawable1.setBounds(0, 0, 200, 200);

        // Create second drawable with exact same colors to trigger collision in sReservedSweepGradientColors
        RangeDrawable drawable2 = new RangeDrawable.Builder(mContext)
                .range(0f, 100f)
                .value(50f)
                .colors(colors, true)
                .build();
        drawable2.setBounds(0, 0, 200, 200);

        // Re-bind drawable1 to trigger sReservedSweepGradientColors.remove
        drawable1.setBounds(0, 0, 250, 250);

        Canvas mockCanvas = mock(Canvas.class);
        drawable1.draw(mockCanvas);
        drawable2.draw(mockCanvas);

        verify(mockCanvas, times(4)).drawPath(any(Path.class), any(Paint.class));
    }
}
