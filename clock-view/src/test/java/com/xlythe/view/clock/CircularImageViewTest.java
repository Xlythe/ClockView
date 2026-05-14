package com.xlythe.view.clock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class CircularImageViewTest {

    private Context mContext;
    private CircularImageView mCircularImageView;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mCircularImageView = new CircularImageView(mContext);
    }

    @Test
    public void testConstructors() {
        CircularImageView view1 = new CircularImageView(mContext);
        assertNotNull(view1);

        AttributeSet attrs = Robolectric.buildAttributeSet().build();
        CircularImageView view2 = new CircularImageView(mContext, attrs);
        assertNotNull(view2);

        CircularImageView view3 = new CircularImageView(mContext, attrs, 0);
        assertNotNull(view3);

        CircularImageView view4 = new CircularImageView(mContext, attrs, 0, 0);
        assertNotNull(view4);
    }

    @Test
    public void testSetCircularEnabledAndOnDraw() {
        CircularImageView spyView = spy(mCircularImageView);
        Canvas mockCanvas = mock(Canvas.class);

        // When circular is disabled, it calls super.onDraw(canvas)
        spyView.setCircularEnabled(false);
        spyView.onDraw(mockCanvas);
        verify(mockCanvas, never()).drawCircle(any(Float.class), any(Float.class), any(Float.class), any(Paint.class));

        // When circular is enabled but no image is set, it does not draw circle
        clearInvocations(mockCanvas);
        spyView.setCircularEnabled(true);
        spyView.setImageDrawable(null);
        spyView.onDraw(mockCanvas);
        verify(mockCanvas, never()).drawCircle(any(Float.class), any(Float.class), any(Float.class), any(Paint.class));
    }

    @Test
    public void testSaveAndRestoreInstanceState() {
        mCircularImageView.setCircularEnabled(false);
        Parcelable state = mCircularImageView.onSaveInstanceState();
        assertNotNull(state);

        CircularImageView restoredView = new CircularImageView(mContext);
        restoredView.onRestoreInstanceState(state);

        // Set a valid bitmap and layout so that it WOULD draw a circle if circular was enabled
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        restoredView.setImageBitmap(bitmap);
        restoredView.layout(0, 0, 100, 100);

        Canvas mockCanvas = mock(Canvas.class);
        restoredView.onDraw(mockCanvas);

        // Because mEnableCircular was restored to false, drawCircle should never be called
        verify(mockCanvas, never()).drawCircle(any(Float.class), any(Float.class), any(Float.class), any(Paint.class));

        // Now enable it and verify it draws
        restoredView.setCircularEnabled(true);
        restoredView.onDraw(mockCanvas);
        verify(mockCanvas, atLeastOnce()).drawCircle(any(Float.class), any(Float.class), any(Float.class), any(Paint.class));
    }

    @Test
    public void testSetImageMethods() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        mCircularImageView.layout(0, 0, 100, 100);
        Canvas mockCanvas = mock(Canvas.class);

        // Test setImageBitmap
        mCircularImageView.setImageBitmap(bitmap);
        mCircularImageView.onDraw(mockCanvas);
        verify(mockCanvas, atLeastOnce()).drawCircle(any(Float.class), any(Float.class), any(Float.class), any(Paint.class));

        // Test setImageDrawable
        clearInvocations(mockCanvas);
        mCircularImageView.setImageDrawable(new BitmapDrawable(mContext.getResources(), bitmap));
        mCircularImageView.onDraw(mockCanvas);
        verify(mockCanvas, atLeastOnce()).drawCircle(any(Float.class), any(Float.class), any(Float.class), any(Paint.class));

        // Test setImageResource
        clearInvocations(mockCanvas);
        mCircularImageView.setImageResource(android.R.drawable.sym_def_app_icon);
        mCircularImageView.onDraw(mockCanvas);
        verify(mockCanvas, atLeastOnce()).drawCircle(any(Float.class), any(Float.class), any(Float.class), any(Paint.class));

        // Test setImageURI
        clearInvocations(mockCanvas);
        Uri uri = Uri.parse("content://test/image");
        byte[] pngBytes = new byte[] {
            (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0D,
            (byte) 0x49, (byte) 0x48, (byte) 0x44, (byte) 0x52, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x08,
            (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1F, (byte) 0x15, (byte) 0xC4, (byte) 0x89, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0A, (byte) 0x49,
            (byte) 0x44, (byte) 0x41, (byte) 0x54, (byte) 0x78, (byte) 0x9C, (byte) 0x63, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x01,
            (byte) 0x0D, (byte) 0x0A, (byte) 0x2D, (byte) 0xB4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x49, (byte) 0x45, (byte) 0x4E, (byte) 0x44, (byte) 0xAE,
            (byte) 0x42, (byte) 0x60, (byte) 0x82
        };
        org.robolectric.Shadows.shadowOf(mContext.getContentResolver()).registerInputStream(uri, new java.io.ByteArrayInputStream(pngBytes));
        mCircularImageView.setImageURI(uri);
    }

    private static class AnimatorDrawable extends ColorDrawable implements Animator {
        OnInvalidateListener listener;
        AnimatorDrawable() {
            super(Color.BLUE);
        }
        @Override
        public void setOnInvalidateListener(OnInvalidateListener l) {
            this.listener = l;
        }
    }

    @Test
    public void testOverrideCallbackWithAnimatorDrawable() {
        AnimatorDrawable animatorDrawable = new AnimatorDrawable();
        animatorDrawable.setBounds(0, 0, 100, 100);

        mCircularImageView.layout(0, 0, 100, 100);
        mCircularImageView.setImageDrawable(animatorDrawable);

        assertNotNull(animatorDrawable.listener);
        assertEquals(mCircularImageView, animatorDrawable.listener);

        // Trigger onInvalidate
        Canvas mockCanvas = mock(Canvas.class);
        animatorDrawable.listener.onInvalidate();
        mCircularImageView.onDraw(mockCanvas);
        verify(mockCanvas, atLeastOnce()).drawCircle(any(Float.class), any(Float.class), any(Float.class), any(Paint.class));
    }

    @Test
    public void testDrawableToBitmapVariations() {
        // Null drawable
        assertNull(mCircularImageView.drawableToBitmap(null));

        // BitmapDrawable
        Bitmap bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), bitmap);
        assertEquals(bitmap, mCircularImageView.drawableToBitmap(bitmapDrawable));

        // Regular drawable with positive intrinsic dimensions
        ColorDrawable colorDrawable = new ColorDrawable(Color.RED);
        Drawable spyDrawable = spy(colorDrawable);
        when(spyDrawable.getIntrinsicWidth()).thenReturn(60);
        when(spyDrawable.getIntrinsicHeight()).thenReturn(60);

        Bitmap generatedBitmap = mCircularImageView.drawableToBitmap(spyDrawable);
        assertNotNull(generatedBitmap);
        assertEquals(60, generatedBitmap.getWidth());
        assertEquals(60, generatedBitmap.getHeight());

        // Regular drawable with non-positive intrinsic dimensions, but view has no dimensions (width/height <= 0)
        Drawable noIntrinsicDrawable = mock(Drawable.class);
        when(noIntrinsicDrawable.getIntrinsicWidth()).thenReturn(0);
        when(noIntrinsicDrawable.getIntrinsicHeight()).thenReturn(0);
        assertNull(mCircularImageView.drawableToBitmap(noIntrinsicDrawable));

        // Regular drawable with non-positive intrinsic dimensions, but view has positive dimensions
        mCircularImageView.layout(0, 0, 80, 80);
        Bitmap viewSizedBitmap = mCircularImageView.drawableToBitmap(noIntrinsicDrawable);
        assertNotNull(viewSizedBitmap);
        assertEquals(80, viewSizedBitmap.getWidth());
        assertEquals(80, viewSizedBitmap.getHeight());

        // Reuse existing mutable mImage bitmap
        Bitmap mutableBitmap = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888);
        mCircularImageView.setImageBitmap(mutableBitmap);
        Bitmap reusedBitmap = mCircularImageView.drawableToBitmap(spyDrawable);
        assertEquals(mutableBitmap, reusedBitmap);
    }

    @Test
    public void testDrawableToBitmapOutOfMemoryError() {
        Drawable mockDrawable = mock(Drawable.class);
        when(mockDrawable.getIntrinsicWidth()).thenReturn(1000);
        when(mockDrawable.getIntrinsicHeight()).thenReturn(1000);

        try (MockedStatic<Bitmap> mockedBitmap = mockStatic(Bitmap.class)) {
            mockedBitmap.when(() -> Bitmap.createBitmap(anyInt(), anyInt(), any(Bitmap.Config.class)))
                    .thenThrow(new OutOfMemoryError("Simulated OOM"));

            Bitmap result = mCircularImageView.drawableToBitmap(mockDrawable);
            assertNull(result);
        }
    }

    @Test
    public void testOnDrawEdgeCasesAndShaderUpdates() {
        Canvas mockCanvas = mock(Canvas.class);

        // Test mImage with 0 width or height
        Bitmap emptyBitmap = mock(Bitmap.class);
        when(emptyBitmap.getWidth()).thenReturn(0);
        when(emptyBitmap.getHeight()).thenReturn(100);
        mCircularImageView.setImageBitmap(emptyBitmap);
        mCircularImageView.onDraw(mockCanvas);
        verify(mockCanvas, never()).drawCircle(any(Float.class), any(Float.class), any(Float.class), any(Paint.class));

        when(emptyBitmap.getWidth()).thenReturn(100);
        when(emptyBitmap.getHeight()).thenReturn(0);
        mCircularImageView.onDraw(mockCanvas);
        verify(mockCanvas, never()).drawCircle(any(Float.class), any(Float.class), any(Float.class), any(Paint.class));

        // Test shader update when canvas size changes
        Bitmap validBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        mCircularImageView.setImageBitmap(validBitmap);

        // Initial layout 50x50
        mCircularImageView.layout(0, 0, 50, 50);
        mCircularImageView.onDraw(mockCanvas);
        verify(mockCanvas, atLeastOnce()).drawCircle(any(Float.class), any(Float.class), any(Float.class), any(Paint.class));

        // Change layout to 150x100 (testing min(width, height) logic where width != height)
        clearInvocations(mockCanvas);
        mCircularImageView.layout(0, 0, 150, 100);
        mCircularImageView.onDraw(mockCanvas);
        verify(mockCanvas, atLeastOnce()).drawCircle(any(Float.class), any(Float.class), any(Float.class), any(Paint.class));
    }

    @Test
    public void testUpdateBitmapShaderUniformScaling() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888);
        mCircularImageView.setImageBitmap(bitmap);
        mCircularImageView.layout(0, 0, 100, 100); // mCanvasSize = 100

        // Trigger updateBitmapShader via onDraw
        Canvas mockCanvas = mock(Canvas.class);
        mCircularImageView.onDraw(mockCanvas);

        // Access mShader via reflection
        java.lang.reflect.Field shaderField = CircularImageView.class.getDeclaredField("mShader");
        shaderField.setAccessible(true);
        android.graphics.BitmapShader shader = (android.graphics.BitmapShader) shaderField.get(mCircularImageView);
        assertNotNull(shader);

        android.graphics.Matrix matrix = new android.graphics.Matrix();
        shader.getLocalMatrix(matrix);

        float[] values = new float[9];
        matrix.getValues(values);

        float scaleX = values[android.graphics.Matrix.MSCALE_X];
        float scaleY = values[android.graphics.Matrix.MSCALE_Y];
        float transX = values[android.graphics.Matrix.MTRANS_X];
        float transY = values[android.graphics.Matrix.MTRANS_Y];

        // For canvas 100x100 and image 200x100:
        // scaleX = 100/200 = 0.5, scaleY = 100/100 = 1.0 -> Math.max = 1.0
        // dx = (100 - 200*1.0)/2 = -50
        // dy = (100 - 100*1.0)/2 = 0
        assertEquals(1.0f, scaleX, 0.01f);
        assertEquals(1.0f, scaleY, 0.01f);
        assertEquals(-50.0f, transX, 0.01f);
        assertEquals(0.0f, transY, 0.01f);
    }
}
