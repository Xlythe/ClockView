package com.xlythe.view.clock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ComplicationDrawableTest {

    private Context mContext;
    private Drawable mSampleIcon;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mSampleIcon = spy(new ColorDrawable(Color.BLUE));
    }

    @Test
    public void testConstructorsAndBuilder() {
        ComplicationDrawable drawable1 = new ComplicationDrawable(mContext);
        assertNotNull(drawable1);
        assertEquals(ComplicationDrawable.Style.FILL, drawable1.getStyle());
        assertNull(drawable1.getIcon());
        assertEquals(mContext, drawable1.getContext());

        ComplicationDrawable drawable2 = new ComplicationDrawable(mContext, mSampleIcon, "Test Text", "Test Title");
        assertNotNull(drawable2);
        assertEquals(mSampleIcon, drawable2.getIcon());

        ComplicationDrawable drawable3 = new ComplicationDrawable.Builder(mContext)
                .icon(mSampleIcon)
                .text("Builder Text")
                .title("Builder Title")
                .style(ComplicationDrawable.Style.LINE)
                .build();
        assertNotNull(drawable3);
        assertEquals(mSampleIcon, drawable3.getIcon());
        assertEquals(ComplicationDrawable.Style.LINE, drawable3.getStyle());
    }

    @Test
    public void testStyleConfigurations() {
        ComplicationDrawable drawable = new ComplicationDrawable(mContext);
        drawable.setBounds(0, 0, 200, 100);
        Canvas mockCanvas = mock(Canvas.class);

        // Fill style draws a solid background without path effects
        drawable.setStyle(ComplicationDrawable.Style.FILL);
        assertEquals(ComplicationDrawable.Style.FILL, drawable.getStyle());
        drawable.draw(mockCanvas);
        ArgumentCaptor<Paint> paintCaptorFill = ArgumentCaptor.forClass(Paint.class);
        verify(mockCanvas).drawRoundRect(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), paintCaptorFill.capture());
        assertEquals(Paint.Style.FILL, paintCaptorFill.getValue().getStyle());
        assertNull(paintCaptorFill.getValue().getPathEffect());

        // Line style draws a stroked border matching the line stroke width
        clearInvocations(mockCanvas);
        drawable.setStyle(ComplicationDrawable.Style.LINE);
        assertEquals(ComplicationDrawable.Style.LINE, drawable.getStyle());
        drawable.draw(mockCanvas);
        ArgumentCaptor<Paint> paintCaptorLine = ArgumentCaptor.forClass(Paint.class);
        verify(mockCanvas).drawRoundRect(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), paintCaptorLine.capture());
        assertEquals(Paint.Style.STROKE, paintCaptorLine.getValue().getStyle());
        assertEquals(drawable.getLineStrokeWidth(), paintCaptorLine.getValue().getStrokeWidth(), 0.01f);
        assertNull(paintCaptorLine.getValue().getPathEffect());

        // Dot style draws a dashed stroked border matching the dot stroke width
        clearInvocations(mockCanvas);
        drawable.setStyle(ComplicationDrawable.Style.DOT);
        assertEquals(ComplicationDrawable.Style.DOT, drawable.getStyle());
        drawable.draw(mockCanvas);
        ArgumentCaptor<Paint> paintCaptorDot = ArgumentCaptor.forClass(Paint.class);
        verify(mockCanvas).drawRoundRect(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), paintCaptorDot.capture());
        assertEquals(Paint.Style.STROKE, paintCaptorDot.getValue().getStyle());
        assertEquals(drawable.getDotStrokeWidth(), paintCaptorDot.getValue().getStrokeWidth(), 0.01f);
        assertNotNull(paintCaptorDot.getValue().getPathEffect());

        // Empty style disables background drawing entirely
        clearInvocations(mockCanvas);
        drawable.setStyle(ComplicationDrawable.Style.EMPTY);
        assertEquals(ComplicationDrawable.Style.EMPTY, drawable.getStyle());
        drawable.draw(mockCanvas);
        verify(mockCanvas, never()).drawRoundRect(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), any(Paint.class));
    }

    @Test
    public void testGettersAndSetters() {
        ComplicationDrawable drawable = new ComplicationDrawable(mContext, mSampleIcon, "Text", "Title");
        drawable.setBounds(0, 0, 200, 100);

        assertEquals(PixelFormat.TRANSLUCENT, drawable.getOpacity());

        // Setting alpha executes without throwing exceptions
        drawable.setAlpha(128);

        ColorFilter filter = mock(ColorFilter.class);
        drawable.setColorFilter(filter);
        Canvas mockCanvas = mock(Canvas.class);
        drawable.draw(mockCanvas);
        verify(mSampleIcon).setColorFilter(filter);

        assertEquals(200, drawable.getWidth());
        assertEquals(100, drawable.getHeight());
    }

    @Test
    public void testTintListAndStateChange() {
        ComplicationDrawable drawable = new ComplicationDrawable(mContext, mSampleIcon, "Text", "Title");
        drawable.setBounds(0, 0, 200, 100);
        Canvas mockCanvas = mock(Canvas.class);

        ColorStateList tint = ColorStateList.valueOf(Color.RED);
        drawable.setTintList(tint);
        drawable.draw(mockCanvas);

        ArgumentCaptor<Paint> bgPaintCaptor = ArgumentCaptor.forClass(Paint.class);
        verify(mockCanvas).drawRoundRect(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), bgPaintCaptor.capture());
        assertEquals(Color.RED, bgPaintCaptor.getValue().getColor());
        verify(mSampleIcon).setTintList(tint);

        int[] state = new int[]{1};
        assertTrue(drawable.onStateChange(state));
        verify(mSampleIcon).setState(state);

        // Clearing tint list falls back to the default color
        clearInvocations(mockCanvas);
        drawable.setTintList(null);
        drawable.draw(mockCanvas);
        ArgumentCaptor<Paint> fallbackPaintCaptor = ArgumentCaptor.forClass(Paint.class);
        verify(mockCanvas).drawRoundRect(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), fallbackPaintCaptor.capture());
        assertEquals(ComplicationDrawable.DEFAULT_COLOR, fallbackPaintCaptor.getValue().getColor());
    }

    @Test
    public void testHorizontalLayoutVariations() {
        Canvas mockCanvas = mock(Canvas.class);

        // Icon only layout triggers only the primary canvas save
        ComplicationDrawable drawableIcon = new ComplicationDrawable(mContext, mSampleIcon, null, null);
        drawableIcon.setBounds(0, 0, 200, 100);
        drawableIcon.draw(mockCanvas);
        verify(mockCanvas, times(1)).save();

        // Text only layout triggers an additional canvas save for the text block
        clearInvocations(mockCanvas);
        ComplicationDrawable drawableText = new ComplicationDrawable(mContext, null, "Only Text", null);
        drawableText.setBounds(0, 0, 200, 100);
        drawableText.draw(mockCanvas);
        verify(mockCanvas, times(2)).save();

        // Title only layout triggers an additional canvas save for the title block
        clearInvocations(mockCanvas);
        ComplicationDrawable drawableTitle = new ComplicationDrawable(mContext, null, null, "Only Title");
        drawableTitle.setBounds(0, 0, 200, 100);
        drawableTitle.draw(mockCanvas);
        verify(mockCanvas, times(2)).save();

        // Title and text in single-line layout triggers canvas saves for both text and title blocks
        clearInvocations(mockCanvas);
        ComplicationDrawable drawableBoth = new ComplicationDrawable(mContext, null, "Text", "Title");
        drawableBoth.setBounds(0, 0, 200, 100);
        drawableBoth.draw(mockCanvas);
        verify(mockCanvas, times(3)).save();

        // Icon, title, and text layout triggers canvas saves for both text and title blocks
        clearInvocations(mockCanvas);
        ComplicationDrawable drawableAll = new ComplicationDrawable(mContext, mSampleIcon, "Text", "Title");
        drawableAll.setBounds(0, 0, 200, 100);
        drawableAll.draw(mockCanvas);
        verify(mockCanvas, times(3)).save();

        // Extremely short text and title in single-line layout centers the content
        clearInvocations(mockCanvas);
        ComplicationDrawable drawableShort = new ComplicationDrawable(mContext, null, "A", "B");
        drawableShort.setBounds(0, 0, 400, 100);
        drawableShort.draw(mockCanvas);
        verify(mockCanvas, times(3)).save();

        // Long text triggers dynamic text size shrinking to fit within bounds
        clearInvocations(mockCanvas);
        String longText = "This is a very long text intended to exceed the bounds and trigger the shrinking logic in while loop.";
        ComplicationDrawable drawableLong = new ComplicationDrawable(mContext, null, longText, longText);
        drawableLong.setBounds(0, 0, 100, 50);
        drawableLong.draw(mockCanvas);
        verify(mockCanvas, times(3)).save();
    }

    @Test
    public void testVerticalLayoutVariations() {
        Canvas mockCanvas = mock(Canvas.class);

        // Icon only layout triggers only the primary canvas save
        ComplicationDrawable drawableIcon = new ComplicationDrawable(mContext, mSampleIcon, null, null);
        drawableIcon.setBounds(0, 0, 100, 200);
        drawableIcon.draw(mockCanvas);
        verify(mockCanvas, times(1)).save();

        // Text only layout triggers an additional canvas save for the text block
        clearInvocations(mockCanvas);
        ComplicationDrawable drawableText = new ComplicationDrawable(mContext, null, "Only Text", null);
        drawableText.setBounds(0, 0, 100, 200);
        drawableText.draw(mockCanvas);
        verify(mockCanvas, times(2)).save();

        // Title only layout triggers an additional canvas save for the title block
        clearInvocations(mockCanvas);
        ComplicationDrawable drawableTitle = new ComplicationDrawable(mContext, null, null, "Only Title");
        drawableTitle.setBounds(0, 0, 100, 200);
        drawableTitle.draw(mockCanvas);
        verify(mockCanvas, times(2)).save();

        // Title and text layout triggers canvas saves for both text and title blocks
        clearInvocations(mockCanvas);
        ComplicationDrawable drawableBoth = new ComplicationDrawable(mContext, null, "Text", "Title");
        drawableBoth.setBounds(0, 0, 100, 200);
        drawableBoth.draw(mockCanvas);
        verify(mockCanvas, times(3)).save();

        // Split-pane layout with icon, title, and text triggers canvas saves for active layout blocks
        clearInvocations(mockCanvas);
        ComplicationDrawable drawableAll = new ComplicationDrawable(mContext, mSampleIcon, "Text", "Title");
        drawableAll.setBounds(0, 0, 100, 200);
        drawableAll.draw(mockCanvas);
        verify(mockCanvas, times(2)).save();
    }

    @Test
    public void testDraw() {
        ComplicationDrawable drawable = new ComplicationDrawable(mContext, mSampleIcon, "Text", "Title");
        drawable.setBounds(0, 0, 200, 100);

        Canvas mockCanvas = mock(Canvas.class);
        drawable.draw(mockCanvas);

        verify(mockCanvas, atLeastOnce()).save();
        verify(mockCanvas, atLeastOnce()).translate(anyFloat(), anyFloat());
        verify(mockCanvas, atLeastOnce()).drawRoundRect(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas, atLeastOnce()).restore();
        verify(mSampleIcon, atLeastOnce()).draw(mockCanvas);
    }
}
