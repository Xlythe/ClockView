package com.xlythe.view.clock.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.annotation.Config;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class BitmapUtilsTest {

    @Test
    public void testGetHourAsBitmap() {
        Context context = ApplicationProvider.getApplicationContext();
        Bitmap mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        Calendar mockCalendar = mock(Calendar.class);
        when(mockCalendar.get(Calendar.HOUR)).thenReturn(3); // 3 * 30 = 90 degrees

        try (MockedStatic<Calendar> calendarStatic = mockStatic(Calendar.class);
             MockedStatic<BitmapFactory> factoryStatic = mockStatic(BitmapFactory.class)) {

            calendarStatic.when(Calendar::getInstance).thenReturn(mockCalendar);
            factoryStatic.when(() -> BitmapFactory.decodeResource(any(), eq(123))).thenReturn(mockBitmap);

            Bitmap result = BitmapUtils.getHourAsBitmap(context, 123);
            assertNotNull(result);
            assertEquals(100, result.getWidth());
            assertEquals(100, result.getHeight());
        }
    }

    @Test
    public void testGetMinuteAsBitmap() {
        Context context = ApplicationProvider.getApplicationContext();
        Bitmap mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        Calendar mockCalendar = mock(Calendar.class);
        when(mockCalendar.get(Calendar.MINUTE)).thenReturn(15); // 15 * 6 = 90 degrees

        try (MockedStatic<Calendar> calendarStatic = mockStatic(Calendar.class);
             MockedStatic<BitmapFactory> factoryStatic = mockStatic(BitmapFactory.class)) {

            calendarStatic.when(Calendar::getInstance).thenReturn(mockCalendar);
            factoryStatic.when(() -> BitmapFactory.decodeResource(any(), eq(456))).thenReturn(mockBitmap);

            Bitmap result = BitmapUtils.getMinuteAsBitmap(context, 456);
            assertNotNull(result);
            assertEquals(100, result.getWidth());
            assertEquals(100, result.getHeight());
        }
    }

    @Test
    public void testGetSecondAsBitmap() {
        Context context = ApplicationProvider.getApplicationContext();
        Bitmap mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        Calendar mockCalendar = mock(Calendar.class);
        when(mockCalendar.get(Calendar.SECOND)).thenReturn(30); // 30 * 6 = 180 degrees

        try (MockedStatic<Calendar> calendarStatic = mockStatic(Calendar.class);
             MockedStatic<BitmapFactory> factoryStatic = mockStatic(BitmapFactory.class)) {

            calendarStatic.when(Calendar::getInstance).thenReturn(mockCalendar);
            factoryStatic.when(() -> BitmapFactory.decodeResource(any(), eq(789))).thenReturn(mockBitmap);

            Bitmap result = BitmapUtils.getSecondAsBitmap(context, 789);
            assertNotNull(result);
            assertEquals(100, result.getWidth());
            assertEquals(100, result.getHeight());
        }
    }

    @Test
    public void testRotateZeroDegrees() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Bitmap result = BitmapUtils.rotate(bitmap, 0f);
        assertSame(bitmap, result);
    }

    @Test
    public void testRotateNoCropNeeded() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Bitmap result = BitmapUtils.rotate(bitmap, 90f);
        assertNotNull(result);
        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    public void testRotateCropNeeded() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Bitmap enlargedBitmap = Bitmap.createBitmap(142, 142, Bitmap.Config.ARGB_8888);

        try (MockedStatic<Bitmap> bitmapStatic = mockStatic(Bitmap.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            bitmapStatic.when(() -> Bitmap.createBitmap(eq(bitmap), eq(0), eq(0), eq(100), eq(100), any(), eq(true)))
                    .thenReturn(enlargedBitmap);

            Bitmap result = BitmapUtils.rotate(bitmap, 45f); // 45 degrees enlarges bounding box, triggering crop
            assertNotNull(result);
            assertEquals(100, result.getWidth());
            assertEquals(100, result.getHeight());
        }
    }

    @Test
    public void testFlatten() {
        Bitmap bitmap1 = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888);
        Bitmap bitmap2 = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888);
        Bitmap result = BitmapUtils.flatten(bitmap1, null, bitmap2);
        assertNotNull(result);
        assertEquals(200, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    public void testAsBitmapFromBitmapDrawableValid() {
        Context context = ApplicationProvider.getApplicationContext();
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
        Bitmap result = BitmapUtils.asBitmap(drawable);
        assertSame(bitmap, result);
    }

    @Test
    public void testAsBitmapFromBitmapDrawableRecycled() {
        Context context = ApplicationProvider.getApplicationContext();
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        bitmap.recycle();
        BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
        Bitmap result = BitmapUtils.asBitmap(drawable);
        assertNotNull(result);
        assertFalse(result.isRecycled());
    }

    @Test
    public void testAsBitmapFromNonBitmapDrawable() {
        ColorDrawable drawable = new ColorDrawable(Color.RED);
        Bitmap result = BitmapUtils.asBitmap(drawable);
        assertNotNull(result);
        assertEquals(1, result.getWidth()); // Math.max(1, intrinsicWidth)
        assertEquals(1, result.getHeight());
    }

    @Test
    public void testCloneWithNullConstantState() {
        Drawable mockDrawable = mock(Drawable.class);
        when(mockDrawable.getConstantState()).thenReturn(null);
        Drawable result = BitmapUtils.clone(mockDrawable);
        assertSame(mockDrawable, result);
    }

    @Test
    public void testCloneWithValidConstantState() {
        ColorDrawable drawable = new ColorDrawable(Color.BLUE);
        Drawable result = BitmapUtils.clone(drawable);
        assertNotNull(result);
        assertNotSame(drawable, result);
    }

    @Test
    public void testResizeWithoutKeepAspectRatio() {
        Bitmap bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888);
        Bitmap result = BitmapUtils.resize(bitmap, 150, 150, false);
        assertEquals(150, result.getWidth());
        assertEquals(150, result.getHeight());
    }

    @Test
    public void testResizeKeepAspectRatioWidthDominant() {
        Bitmap bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888); // aspectRatio = 2.0
        Bitmap result = BitmapUtils.resize(bitmap, 100, 100, true);
        assertEquals(100, result.getWidth());
        assertEquals(50, result.getHeight());
    }

    @Test
    public void testResizeKeepAspectRatioHeightDominant() {
        Bitmap bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888); // aspectRatio = 0.5
        Bitmap result = BitmapUtils.resize(bitmap, 100, 100, true);
        assertEquals(50, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    public void testMeasure() {
        Context context = ApplicationProvider.getApplicationContext();
        View view = new View(context);
        Rect bounds = new Rect(0, 0, 100, 200);
        BitmapUtils.measure(view, bounds);
        assertEquals(100, view.getMeasuredWidth());
        assertEquals(200, view.getMeasuredHeight());
        assertEquals(100, view.getWidth());
        assertEquals(200, view.getHeight());
    }

    @Test
    public void testDrawViewBounds() {
        Context context = ApplicationProvider.getApplicationContext();
        View view = new View(context);
        view.layout(0, 0, 100, 200);
        Rect bounds = new Rect(0, 0, 100, 200);
        Bitmap result = BitmapUtils.draw(view, bounds);
        assertNotNull(result);
        assertEquals(100, result.getWidth());
        assertEquals(200, result.getHeight());
    }

    @Test
    public void testDrawViewBoundsForceResize() {
        Context context = ApplicationProvider.getApplicationContext();
        View view = new View(context);
        Rect bounds = new Rect(0, 0, 100, 200);
        Bitmap result = BitmapUtils.draw(view, bounds, true);
        assertNotNull(result);
        assertEquals(100, result.getWidth());
        assertEquals(200, result.getHeight());
        assertEquals(100, view.getWidth());
        assertEquals(200, view.getHeight());
    }

    @Test
    public void testDrawViewCanvasBoundsWithLayoutRequested() {
        Context context = ApplicationProvider.getApplicationContext();
        View view = new View(context);
        view.requestLayout();
        Bitmap container = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(container);
        Rect bounds = new Rect(0, 0, 100, 200);
        BitmapUtils.draw(view, canvas, bounds);
        assertEquals(100, view.getWidth());
        assertEquals(200, view.getHeight());
    }

    @Test
    public void testSetForceSoftwareRecursion() {
        Context context = ApplicationProvider.getApplicationContext();
        FrameLayout viewGroup = new FrameLayout(context);
        View childView = new View(context);
        viewGroup.addView(childView);

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{new ColorDrawable(Color.RED)});
        childView.setBackground(layerDrawable);

        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{}, new ColorDrawable(Color.BLUE));
        childView.setForeground(stateListDrawable);

        Bitmap container = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(container);
        Rect bounds = new Rect(0, 0, 100, 200);

        // Exercises the entire setForceSoftware view and drawable hierarchy under Robolectric (SDK 34)
        BitmapUtils.draw(viewGroup, canvas, bounds, true);
        assertEquals(100, viewGroup.getWidth());
        assertEquals(200, viewGroup.getHeight());
    }

    @Test
    public void testSetForceSoftwareRippleDrawable() {
        Context context = ApplicationProvider.getApplicationContext();
        View view = new View(context);
        RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(Color.RED), new ColorDrawable(Color.WHITE), null);
        view.setBackground(rippleDrawable);

        Bitmap container = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(container);
        Rect bounds = new Rect(0, 0, 100, 200);

        BitmapUtils.draw(view, canvas, bounds, true);
        assertEquals(100, view.getWidth());
        assertEquals(200, view.getHeight());
    }

    @Test
    public void testSetForceSoftwareDrawableGetCurrentDifferent() {
        Context context = ApplicationProvider.getApplicationContext();
        View view = new View(context);
        ScaleDrawable scaleDrawable = new ScaleDrawable(new ColorDrawable(Color.GREEN), Gravity.CENTER, 0.5f, 0.5f);
        view.setBackground(scaleDrawable);

        Bitmap container = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(container);
        Rect bounds = new Rect(0, 0, 100, 200);

        BitmapUtils.draw(view, canvas, bounds, true);
        assertEquals(100, view.getWidth());
        assertEquals(200, view.getHeight());
    }
}
