package com.xlythe.view.clock;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextPaint;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 28, manifest = Config.NONE)
public class AutoResizeTextViewTest {

    private Context mContext;
    private AutoResizeTextView mTextView;

    // Subclass to provide deterministic, realistic font height measuring in Robolectric
    // bypassing ASM ClassReader / StaticLayout shadow limitations on JDK 26.
    private static class MockAutoResizeTextView extends AutoResizeTextView {
        public MockAutoResizeTextView(Context context) {
            super(context);
        }

        public MockAutoResizeTextView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MockAutoResizeTextView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected int getTextHeight(CharSequence source, TextPaint paint, int width, float textSize) {
            if (source == null || source.length() == 0 || width <= 0) {
                return 0;
            }
            // Proportional simulation: each char is approx (textSize * 0.6f) wide
            float charWidth = textSize * 0.6f;
            int charsPerLine = Math.max(1, (int) (width / charWidth));
            int lines = (int) Math.ceil((double) source.length() / charsPerLine);
            return (int) (lines * textSize);
        }
    }

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mTextView = new MockAutoResizeTextView(mContext);
    }

    @Test
    public void testInitialDefaultState() {
        assertEquals(AutoResizeTextView.MIN_TEXT_SIZE, mTextView.getMinTextSize(), 0.01f);
        assertEquals(0f, mTextView.getMaxTextSize(), 0.01f);
        assertTrue(mTextView.getAddEllipsis());
    }

    @Test
    public void testResizeTextShrinksToFit() {
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50f);
        float initialSize = mTextView.getTextSize();
        mTextView.setText("This is a very long string that will definitely not fit in a 100x100 box at 50sp.");
        mTextView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        mTextView.resizeText(100, 100);
        assertTrue("Text size should shrink below initial size", mTextView.getTextSize() < initialSize);
    }

    @Test
    public void testMaxTextSizeBounds() {
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        mTextView.setMaxTextSize(100f);
        mTextView.setText("Short");
        mTextView.setLayoutParams(new ViewGroup.LayoutParams(1000, 1000));
        mTextView.resizeText(1000, 1000);
        assertTrue("Text size should increase but not exceed max text size", mTextView.getTextSize() <= 100f);
    }

    @Test
    public void testMinTextSizeAndEllipsis() {
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f);
        mTextView.setMinTextSize(20f);
        mTextView.setAddEllipsis(true);
        mTextView.setText("This is an extremely long text that cannot possibly fit within a tiny 100x20 box even at 20sp.");
        mTextView.setLayoutParams(new ViewGroup.LayoutParams(100, 20));
        mTextView.resizeText(100, 20);
        assertEquals("Text size should not shrink below min text size", 20f, mTextView.getTextSize(), 0.01f);
        assertTrue("Text should end with ellipsis", mTextView.getText().toString().endsWith("..."));
    }

    @Test
    public void testWrapContentInfiniteHeightPrevention() {
        mTextView.resetMaxTextSizeWithoutLayout(); // Reset max text size without triggering shadow layout relayer
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f);
        float wrapInitialSize = mTextView.getTextSize();
        mTextView.setText("Wrap content test");
        mTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mTextView.resizeText(500, Integer.MAX_VALUE);
        assertEquals("Text size should remain at initial size when height is wrap_content", wrapInitialSize, mTextView.getTextSize(), 0.01f);
    }

    @Test
    public void testOnResizeListenerNotification() {
        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        AtomicReference<Float> oldSizeRef = new AtomicReference<>(0f);
        AtomicReference<Float> newSizeRef = new AtomicReference<>(0f);
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f);
        mTextView.setOnResizeListener((textView, oldSize, newSize) -> {
            listenerCalled.set(true);
            oldSizeRef.set(oldSize);
            newSizeRef.set(newSize);
        });
        mTextView.setText("Testing listener callback with long text");
        mTextView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        mTextView.resizeText(100, 100);
        assertTrue("Resize listener should be called", listenerCalled.get());
        assertTrue("Old size should be greater than new size", oldSizeRef.get() > newSizeRef.get());
    }

    @Test
    public void testEmptyTextAndZeroDimensionsGracefulHandling() {
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f);
        float emptyInitialSize = mTextView.getTextSize();
        mTextView.setText("");
        mTextView.resizeText(100, 100);
        assertEquals(emptyInitialSize, mTextView.getTextSize(), 0.01f);
        mTextView.setText("Valid text");
        mTextView.resizeText(0, 0);
        assertEquals(emptyInitialSize, mTextView.getTextSize(), 0.01f);
    }

    @Test
    public void testConstructors() {
        AutoResizeTextView view1 = new AutoResizeTextView(mContext);
        assertNotNull(view1);
        assertEquals(AutoResizeTextView.MIN_TEXT_SIZE, view1.getMinTextSize(), 0.01f);

        AutoResizeTextView view2 = new AutoResizeTextView(mContext, null);
        assertNotNull(view2);
        assertEquals(AutoResizeTextView.MIN_TEXT_SIZE, view2.getMinTextSize(), 0.01f);

        AutoResizeTextView view3 = new AutoResizeTextView(mContext, null, 0);
        assertNotNull(view3);
        assertEquals(AutoResizeTextView.MIN_TEXT_SIZE, view3.getMinTextSize(), 0.01f);

        MockAutoResizeTextView mock1 = new MockAutoResizeTextView(mContext, null);
        assertNotNull(mock1);

        MockAutoResizeTextView mock2 = new MockAutoResizeTextView(mContext, null, 0);
        assertNotNull(mock2);
    }

    @Test
    public void testSetTextSizeWithUnits() {
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30f);
        assertTrue(mTextView.getTextSize() > 0);

        // Test mTextSize == 0 edge case in resizeText
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 0f);
        mTextView.setText("Some text");
        mTextView.resizeText(100, 100);
        assertEquals(0f, mTextView.getTextSize(), 0.01f);
    }

    @Test
    public void testSetLineSpacing() {
        mTextView.setLineSpacing(5.0f, 1.5f);
        assertEquals(5.0f, mTextView.getLineSpacingExtra(), 0.01f);
        assertEquals(1.5f, mTextView.getLineSpacingMultiplier(), 0.01f);

        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f);
        mTextView.setText("Line spacing test with multiple lines of text to verify spacing preservation.");
        mTextView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        mTextView.resizeText(100, 100);

        assertEquals(5.0f, mTextView.getLineSpacingExtra(), 0.01f);
        assertEquals(1.5f, mTextView.getLineSpacingMultiplier(), 0.01f);
    }

    @Test
    public void testTransformationMethod() {
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f);
        mTextView.setText("Short");
        mTextView.setTransformationMethod(new TransformationMethod() {
            @Override
            public CharSequence getTransformation(CharSequence source, View view) {
                return "This is a very long transformed string that will force a resize.";
            }

            @Override
            public void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction, Rect previouslyFocusedRect) {
            }
        });
        mTextView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        float initialSize = mTextView.getTextSize();
        mTextView.resizeText(100, 100);
        assertTrue("Text size should shrink due to long transformed text", mTextView.getTextSize() < initialSize);
    }

    @Test
    public void testEllipsisExtremeSmallHeight() {
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f);
        mTextView.setMinTextSize(20f);
        mTextView.setAddEllipsis(true);
        mTextView.setText("Long text for small height");
        // Height 2 is too small for even one line at 20sp, but AutoResizeTextView preserves line 0 with ellipsis
        mTextView.setLayoutParams(new ViewGroup.LayoutParams(100, 2));
        mTextView.resizeText(100, 2);
        assertTrue("Text should preserve line 0 with ellipsis when height is extremely small", mTextView.getText().toString().endsWith("..."));
    }

    @Test
    public void testEllipsisExtremeNarrowWidth() {
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f);
        mTextView.setMinTextSize(20f);
        mTextView.setAddEllipsis(true);
        mTextView.setText("Long text for narrow width");
        // Width 2 is too narrow for even one char + ellipsis
        mTextView.setLayoutParams(new ViewGroup.LayoutParams(2, 100));
        mTextView.resizeText(2, 100);
        assertEquals("Text should be empty when width cannot fit ellipsis", "", mTextView.getText().toString());
    }

    @Test
    public void testAddEllipsisDisabled() {
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f);
        mTextView.setMinTextSize(20f);
        mTextView.setAddEllipsis(false);
        String originalText = "This is a very long text that will not fit in a 100x20 box at 20sp.";
        mTextView.setText(originalText);
        mTextView.setLayoutParams(new ViewGroup.LayoutParams(100, 20));
        mTextView.resizeText(100, 20);
        assertEquals(20f, mTextView.getTextSize(), 0.01f);
        assertEquals("Text should not be truncated with ellipsis when addEllipsis is false", originalText, mTextView.getText().toString());
    }

    @Test
    public void testOnSizeChangedAndOnTextChanged() {
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f);
        float expectedSize = mTextView.getTextSize();

        // onTextChanged is called internally by setText
        mTextView.setText("A very long text string that will require resizing when layout runs.");
        assertEquals(expectedSize, mTextView.getTextSize(), 0.01f);

        // Trigger layout to verify mNeedsResize flag was set by onTextChanged / onSizeChanged
        mTextView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        mTextView.layout(0, 0, 100, 100);
        assertTrue("Layout should trigger resizeText and shrink text", mTextView.getTextSize() < expectedSize);
    }

    @Test
    public void testResizeTextNoArgs() {
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50f);
        float initialSize = mTextView.getTextSize();
        mTextView.setText("This is a very long string for testing no-args resizeText.");
        mTextView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        // Simulate view having dimensions
        mTextView.layout(0, 0, 100, 100);
        mTextView.resizeText();
        assertTrue("Text size should shrink with no-args resizeText", mTextView.getTextSize() < initialSize);
    }

    @Test
    public void testResetMaxTextSizeWithoutLayout() {
        mTextView.setMaxTextSize(50f);
        assertEquals(50f, mTextView.getMaxTextSize(), 0.01f);
        mTextView.resetMaxTextSizeWithoutLayout();
        assertEquals(0f, mTextView.getMaxTextSize(), 0.01f);
    }
}
