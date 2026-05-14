package com.xlythe.view.clock;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class PercentBasedLayoutTest {

    private Context mContext;
    private PercentBasedLayout mLayout;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mLayout = new PercentBasedLayout(mContext);
    }

    @Test
    public void testConstructors() {
        PercentBasedLayout layout1 = new PercentBasedLayout(mContext);
        assertNotNull(layout1);

        PercentBasedLayout layout2 = new PercentBasedLayout(mContext, null);
        assertNotNull(layout2);

        PercentBasedLayout layout3 = new PercentBasedLayout(mContext, null, 0);
        assertNotNull(layout3);
    }

    @Test
    public void testGenerateDefaultLayoutParams() {
        ViewGroup.LayoutParams lp = mLayout.generateDefaultLayoutParams();
        assertNotNull(lp);
        assertTrue(lp instanceof PercentBasedLayout.LayoutParams);
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, lp.width);
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, lp.height);
    }

    @Test
    public void testCheckLayoutParams() {
        assertTrue(mLayout.checkLayoutParams(new PercentBasedLayout.LayoutParams(100, 100)));
        assertFalse(mLayout.checkLayoutParams(new ViewGroup.LayoutParams(100, 100)));
    }

    @Test
    public void testGenerateLayoutParamsFromViewGroupParams() {
        ViewGroup.LayoutParams baseParams = new ViewGroup.LayoutParams(100, 200);
        ViewGroup.LayoutParams generated = mLayout.generateLayoutParams(baseParams);
        assertNotNull(generated);
        assertTrue(generated instanceof PercentBasedLayout.LayoutParams);
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, generated.width);
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, generated.height);
    }

    @Test
    public void testIterator() {
        View child1 = new View(mContext);
        View child2 = new View(mContext);
        mLayout.addView(child1);
        mLayout.addView(child2);

        Iterator<View> iterator = mLayout.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(child1, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(child2, iterator.next());
        assertFalse(iterator.hasNext());

        // Test iterator removal
        iterator = mLayout.iterator();
        assertTrue(iterator.hasNext());
        iterator.next();
        iterator.remove();
        assertEquals(1, mLayout.getChildCount());
        assertEquals(child2, mLayout.getChildAt(0));
    }

    @Test
    public void testLayoutParamsConstructors() {
        // Test (int, int)
        PercentBasedLayout.LayoutParams lp1 = new PercentBasedLayout.LayoutParams(100, 200);
        assertEquals(100, lp1.width);
        assertEquals(200, lp1.height);

        // Test (float, float)
        PercentBasedLayout.LayoutParams lp2 = new PercentBasedLayout.LayoutParams(0.5f, 0.25f);
        assertEquals(PercentBasedLayout.LayoutParams.PARENT_PERCENT, lp2.width);
        assertEquals(PercentBasedLayout.LayoutParams.PARENT_PERCENT, lp2.height);
        assertEquals(0.5f, lp2.widthPercent, 0.001f);
        assertEquals(0.25f, lp2.heightPercent, 0.001f);
        assertTrue(lp2.hasWidthPercent);
        assertTrue(lp2.hasHeightPercent);

        // Test copy constructor
        lp2.leftMarginPercent = 0.1f;
        lp2.topMarginPercent = 0.2f;
        lp2.rightMarginPercent = 0.3f;
        lp2.bottomMarginPercent = 0.4f;
        lp2.leftMargin = 10;
        lp2.topMargin = 20;
        lp2.rightMargin = 30;
        lp2.bottomMargin = 40;
        lp2.gravity = Gravity.CENTER;

        PercentBasedLayout.LayoutParams copy = new PercentBasedLayout.LayoutParams(lp2);
        assertEquals(PercentBasedLayout.LayoutParams.PARENT_PERCENT, copy.width);
        assertEquals(PercentBasedLayout.LayoutParams.PARENT_PERCENT, copy.height);
        assertEquals(0.5f, copy.widthPercent, 0.001f);
        assertEquals(0.25f, copy.heightPercent, 0.001f);
        assertTrue(copy.hasWidthPercent);
        assertTrue(copy.hasHeightPercent);
        assertEquals(0.1f, copy.leftMarginPercent, 0.001f);
        assertEquals(0.2f, copy.topMarginPercent, 0.001f);
        assertEquals(0.3f, copy.rightMarginPercent, 0.001f);
        assertEquals(0.4f, copy.bottomMarginPercent, 0.001f);
        assertEquals(10, copy.leftMargin);
        assertEquals(20, copy.topMargin);
        assertEquals(30, copy.rightMargin);
        assertEquals(40, copy.bottomMargin);
        assertEquals(Gravity.CENTER, copy.gravity);

        // Test copy constructor with generic ViewGroup.LayoutParams
        ViewGroup.LayoutParams genericParams = new ViewGroup.LayoutParams(300, 400);
        PercentBasedLayout.LayoutParams genericCopy = new PercentBasedLayout.LayoutParams(genericParams);
        assertEquals(300, genericCopy.width);
        assertEquals(400, genericCopy.height);
    }

    private Context createMockContextWithAttributes(
            Integer widthType, Object widthVal,
            Integer heightType, Object heightVal,
            Integer marginType, Object marginVal,
            Integer marginLeftType, Object marginLeftVal,
            Integer marginTopType, Object marginTopVal,
            Integer marginRightType, Object marginRightVal,
            Integer marginBottomType, Object marginBottomVal,
            Integer gravityVal) {
        Context context = mock(Context.class);
        TypedArray arr = mock(TypedArray.class);
        when(context.obtainStyledAttributes(any(), eq(R.styleable.PercentBasedLayout_Layout))).thenReturn(arr);

        configureAttr(arr, R.styleable.PercentBasedLayout_Layout_layout_width, widthType, widthVal);
        configureAttr(arr, R.styleable.PercentBasedLayout_Layout_layout_height, heightType, heightVal);
        configureAttr(arr, R.styleable.PercentBasedLayout_Layout_layout_margin, marginType, marginVal);
        configureAttr(arr, R.styleable.PercentBasedLayout_Layout_layout_marginLeft, marginLeftType, marginLeftVal);
        configureAttr(arr, R.styleable.PercentBasedLayout_Layout_layout_marginTop, marginTopType, marginTopVal);
        configureAttr(arr, R.styleable.PercentBasedLayout_Layout_layout_marginRight, marginRightType, marginRightVal);
        configureAttr(arr, R.styleable.PercentBasedLayout_Layout_layout_marginBottom, marginBottomType, marginBottomVal);

        if (gravityVal != null) {
            when(arr.getInt(eq(R.styleable.PercentBasedLayout_Layout_layout_gravity), anyInt())).thenReturn(gravityVal);
        } else {
            when(arr.getInt(eq(R.styleable.PercentBasedLayout_Layout_layout_gravity), anyInt())).thenAnswer(inv -> inv.getArgument(1));
        }

        return context;
    }

    private void configureAttr(TypedArray arr, int index, Integer type, Object val) {
        if (type == null) {
            when(arr.hasValue(index)).thenReturn(false);
        } else {
            when(arr.hasValue(index)).thenReturn(true);
            when(arr.getType(index)).thenReturn(type);
            if (type == TypedValue.TYPE_DIMENSION) {
                when(arr.getDimensionPixelSize(eq(index), anyInt())).thenReturn((Integer) val);
            } else if (type == TypedValue.TYPE_FLOAT) {
                when(arr.getFloat(eq(index), anyFloat())).thenReturn((Float) val);
            } else {
                when(arr.getInt(eq(index), anyInt())).thenReturn((Integer) val);
            }
        }
    }

    @Test
    public void testGenerateLayoutParamsFromAttributeSet() {
        AttributeSet attrs = Robolectric.buildAttributeSet()
                .addAttribute(R.attr.layout_width, "100dp")
                .addAttribute(R.attr.layout_height, "200dp")
                .build();

        PercentBasedLayout.LayoutParams lp = mLayout.generateLayoutParams(attrs);
        assertNotNull(lp);
        assertEquals(100, lp.width);
        assertEquals(200, lp.height);
    }

    @Test
    public void testLayoutParamsFromAttributesDimensions() {
        Context mockContext = createMockContextWithAttributes(
                TypedValue.TYPE_DIMENSION, 100,
                TypedValue.TYPE_DIMENSION, 200,
                TypedValue.TYPE_DIMENSION, 10,
                null, null,
                null, null,
                null, null,
                null, null,
                Gravity.CENTER);
        AttributeSet mockAttrs = mock(AttributeSet.class);

        PercentBasedLayout.LayoutParams lp = new PercentBasedLayout.LayoutParams(mockContext, mockAttrs);
        assertNotNull(lp);
        assertEquals(100, lp.width);
        assertEquals(200, lp.height);
        assertEquals(10, lp.leftMargin);
        assertEquals(10, lp.topMargin);
        assertEquals(10, lp.rightMargin);
        assertEquals(10, lp.bottomMargin);
        assertEquals(Gravity.CENTER, lp.gravity);
    }

    @Test
    public void testLayoutParamsFromAttributesPercents() {
        Context mockContext = createMockContextWithAttributes(
                TypedValue.TYPE_FLOAT, 0.5f,
                TypedValue.TYPE_FLOAT, 0.25f,
                TypedValue.TYPE_FLOAT, 0.1f,
                TypedValue.TYPE_FLOAT, 0.2f,
                TypedValue.TYPE_FLOAT, 0.3f,
                TypedValue.TYPE_FLOAT, 0.4f,
                TypedValue.TYPE_FLOAT, 0.5f,
                null);
        AttributeSet mockAttrs = mock(AttributeSet.class);

        PercentBasedLayout.LayoutParams lp = new PercentBasedLayout.LayoutParams(mockContext, mockAttrs);
        assertNotNull(lp);
        assertTrue(lp.hasWidthPercent);
        assertTrue(lp.hasHeightPercent);
        assertEquals(0.5f, lp.widthPercent, 0.001f);
        assertEquals(0.25f, lp.heightPercent, 0.001f);
        assertEquals(0.2f, lp.leftMarginPercent, 0.001f);
        assertEquals(0.3f, lp.topMarginPercent, 0.001f);
        assertEquals(0.4f, lp.rightMarginPercent, 0.001f);
        assertEquals(0.5f, lp.bottomMarginPercent, 0.001f);
    }

    @Test
    public void testLayoutParamsFromAttributesMatchParentWrapContent() {
        Context mockContext = createMockContextWithAttributes(
                TypedValue.TYPE_INT_DEC, ViewGroup.LayoutParams.MATCH_PARENT,
                TypedValue.TYPE_INT_DEC, ViewGroup.LayoutParams.WRAP_CONTENT,
                null, null,
                TypedValue.TYPE_DIMENSION, 5,
                TypedValue.TYPE_DIMENSION, 15,
                TypedValue.TYPE_DIMENSION, 25,
                TypedValue.TYPE_DIMENSION, 35,
                null);
        AttributeSet mockAttrs = mock(AttributeSet.class);

        PercentBasedLayout.LayoutParams lp = new PercentBasedLayout.LayoutParams(mockContext, mockAttrs);
        assertNotNull(lp);
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, lp.width);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, lp.height);
        assertEquals(5, lp.leftMargin);
        assertEquals(15, lp.topMargin);
        assertEquals(25, lp.rightMargin);
        assertEquals(35, lp.bottomMargin);
    }

    @Test
    public void testLayoutParamsMissingWidthThrowsException() {
        Context mockContext = createMockContextWithAttributes(
                null, null,
                TypedValue.TYPE_DIMENSION, 200,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null);
        AttributeSet mockAttrs = mock(AttributeSet.class);

        try {
            new PercentBasedLayout.LayoutParams(mockContext, mockAttrs);
            fail("Expected RuntimeException for missing layout_width");
        } catch (RuntimeException e) {
            assertEquals("You must supply a layout_width attribute.", e.getMessage());
        }
    }

    @Test
    public void testLayoutParamsMissingHeightThrowsException() {
        Context mockContext = createMockContextWithAttributes(
                TypedValue.TYPE_DIMENSION, 100,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null);
        AttributeSet mockAttrs = mock(AttributeSet.class);

        try {
            new PercentBasedLayout.LayoutParams(mockContext, mockAttrs);
            fail("Expected RuntimeException for missing layout_height");
        } catch (RuntimeException e) {
            assertEquals("You must supply a layout_height attribute.", e.getMessage());
        }
    }

    @Test
    public void testOnMeasure() {
        View childExact = new View(mContext);
        childExact.setLayoutParams(new PercentBasedLayout.LayoutParams(100, 200));
        mLayout.addView(childExact);

        View childPercent = new View(mContext);
        childPercent.setLayoutParams(new PercentBasedLayout.LayoutParams(0.5f, 0.25f));
        mLayout.addView(childPercent);

        View childMatch = new View(mContext);
        childMatch.setLayoutParams(new PercentBasedLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mLayout.addView(childMatch);

        View childWrap = new View(mContext);
        childWrap.setLayoutParams(new PercentBasedLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mLayout.addView(childWrap);

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY);

        mLayout.measure(widthMeasureSpec, heightMeasureSpec);

        assertEquals(1000, mLayout.getMeasuredWidth());
        assertEquals(800, mLayout.getMeasuredHeight());

        assertEquals(100, childExact.getMeasuredWidth());
        assertEquals(200, childExact.getMeasuredHeight());

        assertEquals(500, childPercent.getMeasuredWidth());
        assertEquals(200, childPercent.getMeasuredHeight());

        assertEquals(1000, childMatch.getMeasuredWidth());
        assertEquals(800, childMatch.getMeasuredHeight());
    }

    @Test
    public void testOnLayoutGravityAndMarginsExact() {
        View childCenter = new View(mContext) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                setMeasuredDimension(100, 100);
            }
        };
        PercentBasedLayout.LayoutParams lpCenter = new PercentBasedLayout.LayoutParams(100, 100);
        lpCenter.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        lpCenter.leftMargin = 10;
        lpCenter.rightMargin = 20;
        lpCenter.topMargin = 30;
        lpCenter.bottomMargin = 40;
        childCenter.setLayoutParams(lpCenter);
        mLayout.addView(childCenter);

        View childBottomRight = new View(mContext) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                setMeasuredDimension(100, 100);
            }
        };
        PercentBasedLayout.LayoutParams lpBottomRight = new PercentBasedLayout.LayoutParams(100, 100);
        lpBottomRight.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        lpBottomRight.rightMargin = 15;
        lpBottomRight.bottomMargin = 25;
        childBottomRight.setLayoutParams(lpBottomRight);
        mLayout.addView(childBottomRight);

        View childTopLeft = new View(mContext) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                setMeasuredDimension(100, 100);
            }
        };
        PercentBasedLayout.LayoutParams lpTopLeft = new PercentBasedLayout.LayoutParams(100, 100);
        lpTopLeft.gravity = Gravity.LEFT | Gravity.TOP;
        lpTopLeft.leftMargin = 5;
        lpTopLeft.topMargin = 15;
        childTopLeft.setLayoutParams(lpTopLeft);
        mLayout.addView(childTopLeft);

        // Measure and layout in a 1000x1000 parent
        mLayout.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        mLayout.layout(0, 0, 1000, 1000);

        // Center horizontal: parentLeft(0) + (1000 - 100)/2 + leftMargin(10) - rightMargin(20) = 450 + 10 - 20 = 440
        // Center vertical: parentTop(0) + (1000 - 100)/2 + topMargin(30) - bottomMargin(40) = 450 + 30 - 40 = 440
        assertEquals(440, childCenter.getLeft());
        assertEquals(440, childCenter.getTop());
        assertEquals(540, childCenter.getRight());
        assertEquals(540, childCenter.getBottom());

        // Bottom Right: parentRight(1000) - width(100) - rightMargin(15) = 885
        // Bottom: parentBottom(1000) - height(100) - bottomMargin(25) = 875
        assertEquals(885, childBottomRight.getLeft());
        assertEquals(875, childBottomRight.getTop());
        assertEquals(985, childBottomRight.getRight());
        assertEquals(975, childBottomRight.getBottom());

        // Top Left: parentLeft(0) + leftMargin(5) = 5
        // Top: parentTop(0) + topMargin(15) = 15
        assertEquals(5, childTopLeft.getLeft());
        assertEquals(15, childTopLeft.getTop());
        assertEquals(105, childTopLeft.getRight());
        assertEquals(115, childTopLeft.getBottom());
    }

    @Test
    public void testOnLayoutGravityAndMarginsPercent() {
        View childCenter = new View(mContext) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                setMeasuredDimension(100, 100);
            }
        };
        PercentBasedLayout.LayoutParams lpCenter = new PercentBasedLayout.LayoutParams(100, 100);
        lpCenter.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        lpCenter.leftMarginPercent = 0.01f; // 10px in 1000px parent
        lpCenter.rightMarginPercent = 0.02f; // 20px
        lpCenter.topMarginPercent = 0.03f; // 30px
        lpCenter.bottomMarginPercent = 0.04f; // 40px
        childCenter.setLayoutParams(lpCenter);
        mLayout.addView(childCenter);

        View childBottomRight = new View(mContext) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                setMeasuredDimension(100, 100);
            }
        };
        PercentBasedLayout.LayoutParams lpBottomRight = new PercentBasedLayout.LayoutParams(100, 100);
        lpBottomRight.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        lpBottomRight.rightMarginPercent = 0.015f; // 15px
        lpBottomRight.bottomMarginPercent = 0.025f; // 25px
        childBottomRight.setLayoutParams(lpBottomRight);
        mLayout.addView(childBottomRight);

        View childTopLeft = new View(mContext) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                setMeasuredDimension(100, 100);
            }
        };
        PercentBasedLayout.LayoutParams lpTopLeft = new PercentBasedLayout.LayoutParams(100, 100);
        lpTopLeft.gravity = Gravity.LEFT | Gravity.TOP;
        lpTopLeft.leftMarginPercent = 0.005f; // 5px
        lpTopLeft.topMarginPercent = 0.015f; // 15px
        childTopLeft.setLayoutParams(lpTopLeft);
        mLayout.addView(childTopLeft);

        // Measure and layout in a 1000x1000 parent
        mLayout.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        mLayout.layout(0, 0, 1000, 1000);

        // Center horizontal: parentLeft(0) + (1000 - 100)/2 + leftMargin(10) - rightMargin(20) = 440
        // Center vertical: parentTop(0) + (1000 - 100)/2 + topMargin(30) - bottomMargin(40) = 440
        assertEquals(440, childCenter.getLeft());
        assertEquals(440, childCenter.getTop());
        assertEquals(540, childCenter.getRight());
        assertEquals(540, childCenter.getBottom());

        // Bottom Right: parentRight(1000) - width(100) - rightMargin(15) = 885
        // Bottom: parentBottom(1000) - height(100) - bottomMargin(25) = 875
        assertEquals(885, childBottomRight.getLeft());
        assertEquals(875, childBottomRight.getTop());
        assertEquals(985, childBottomRight.getRight());
        assertEquals(975, childBottomRight.getBottom());

        // Top Left: parentLeft(0) + leftMargin(5) = 5
        // Top: parentTop(0) + topMargin(15) = 15
        assertEquals(5, childTopLeft.getLeft());
        assertEquals(15, childTopLeft.getTop());
        assertEquals(105, childTopLeft.getRight());
        assertEquals(115, childTopLeft.getBottom());
    }

    @Test
    public void testOnLayoutDefaultGravity() {
        View child = new View(mContext) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                setMeasuredDimension(100, 100);
            }
        };
        PercentBasedLayout.LayoutParams lp = new PercentBasedLayout.LayoutParams(100, 100);
        lp.gravity = -1; // Should fallback to DEFAULT_CHILD_GRAVITY (Gravity.TOP | Gravity.START)
        child.setLayoutParams(lp);
        mLayout.addView(child);

        mLayout.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        mLayout.layout(0, 0, 1000, 1000);

        assertEquals(0, child.getLeft());
        assertEquals(0, child.getTop());
    }

    @Test
    public void testOnLayoutRtl() {
        PercentBasedLayout layout = new PercentBasedLayout(mContext) {
            @Override
            public int getLayoutDirection() {
                return View.LAYOUT_DIRECTION_RTL;
            }
        };

        View child = new View(mContext) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                setMeasuredDimension(100, 100);
            }
        };
        PercentBasedLayout.LayoutParams lp = new PercentBasedLayout.LayoutParams(100, 100);
        lp.gravity = Gravity.START | Gravity.TOP;
        child.setLayoutParams(lp);
        layout.addView(child);

        layout.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        layout.layout(0, 0, 1000, 1000);

        // In RTL, START becomes RIGHT. So childLeft = parentRight(1000) - width(100) = 900.
        assertEquals(900, child.getLeft());
        assertEquals(0, child.getTop());
    }
}
