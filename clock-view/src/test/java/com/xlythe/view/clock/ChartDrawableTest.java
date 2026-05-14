package com.xlythe.view.clock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ChartDrawableTest {

    private Context mContext;
    private Drawable mSampleIcon;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mSampleIcon = spy(new ColorDrawable(Color.BLUE));
    }

    @Test
    public void testBuilderAndConstructors() {
        ChartDrawable drawable = new ChartDrawable.Builder(mContext)
                .icon(mSampleIcon)
                .text("Test Text")
                .title("Test Title")
                .backgroundColor(Color.DKGRAY)
                .style(ComplicationDrawable.Style.LINE)
                .addElement(10f, Color.RED)
                .addElement(20f, Color.GREEN)
                .build();

        assertNotNull(drawable);
        assertEquals(mSampleIcon, drawable.getIcon());
    }

    @Test
    public void testBuilderStyles() {
        ChartDrawable drawableFill = new ChartDrawable.Builder(mContext)
                .style(ComplicationDrawable.Style.FILL)
                .addElement(10f, Color.RED)
                .build();
        assertNotNull(drawableFill);

        ChartDrawable drawableDot = new ChartDrawable.Builder(mContext)
                .style(ComplicationDrawable.Style.DOT)
                .addElement(10f, Color.RED)
                .build();
        assertNotNull(drawableDot);

        ChartDrawable drawableEmpty = new ChartDrawable.Builder(mContext)
                .style(ComplicationDrawable.Style.EMPTY)
                .addElement(10f, Color.RED)
                .build();
        assertNotNull(drawableEmpty);
    }

    @Test
    public void testDrawWithElementsAndIcon() {
        ChartDrawable drawable = new ChartDrawable.Builder(mContext)
                .icon(mSampleIcon)
                .backgroundColor(Color.BLACK)
                .addElement(10f, Color.RED)
                .addElement(10f, Color.BLUE)
                .build();

        drawable.setBounds(0, 0, 200, 200);

        Canvas mockCanvas = mock(Canvas.class);
        drawable.draw(mockCanvas);

        verify(mockCanvas, atLeastOnce()).drawArc(any(), anyFloat(), anyFloat(), anyBoolean(), any());
        verify(mSampleIcon, atLeastOnce()).draw(mockCanvas);
    }

    @Test
    public void testDrawZeroWeightSum() {
        ChartDrawable drawable = new ChartDrawable.Builder(mContext)
                .backgroundColor(Color.BLACK)
                .build();

        drawable.setBounds(0, 0, 200, 200);

        Canvas mockCanvas = mock(Canvas.class);
        drawable.draw(mockCanvas);
    }
}
