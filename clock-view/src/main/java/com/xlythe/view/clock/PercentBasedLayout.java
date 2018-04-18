package com.xlythe.view.clock;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.util.Iterator;

public class PercentBasedLayout extends ViewGroup implements Iterable<View> {
    private static final int DEFAULT_CHILD_GRAVITY = Gravity.TOP | Gravity.START;

    public PercentBasedLayout(Context context) {
        this(context, null);
    }

    public PercentBasedLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PercentBasedLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @NonNull
    @Override
    public Iterator<View> iterator() {
        return new Iterator<View>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < getChildCount();
            }

            @Override
            public View next() {
                return getChildAt(i++);
            }

            @Override
            public void remove() {
                removeViewAt(i);
            }
        };
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int parentLeft = getPaddingLeft();
        final int parentRight = r - l - getPaddingRight();

        final int parentTop = getPaddingTop();
        final int parentBottom = b - t - getPaddingBottom();

        for (View child : this) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            final int width = child.getMeasuredWidth();
            final int height = child.getMeasuredHeight();

            int childLeft;
            int childTop;

            int gravity = lp.gravity;
            if (gravity == -1) {
                gravity = DEFAULT_CHILD_GRAVITY;
            }

            final int horizontalGravity;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                final int layoutDirection = getLayoutDirection();
                final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
                horizontalGravity = absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
            } else {
                horizontalGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
            }
            final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

            switch (horizontalGravity) {
                case Gravity.CENTER_HORIZONTAL:
                    childLeft = parentLeft + (parentRight - parentLeft - width) / 2 +
                            getChildMarginLeft(lp) - getChildMarginRight(lp);
                    break;
                case Gravity.RIGHT:
                    childLeft = parentRight - width - getChildMarginRight(lp);
                    break;
                case Gravity.LEFT:
                default:
                    childLeft = parentLeft + getChildMarginLeft(lp);
            }

            switch (verticalGravity) {
                case Gravity.TOP:
                    childTop = parentTop + getChildMarginTop(lp);
                    break;
                case Gravity.CENTER_VERTICAL:
                    childTop = parentTop + (parentBottom - parentTop - height) / 2 +
                            getChildMarginTop(lp) - getChildMarginBottom(lp);
                    break;
                case Gravity.BOTTOM:
                    childTop = parentBottom - height - getChildMarginBottom(lp);
                    break;
                default:
                    childTop = parentTop + getChildMarginTop(lp);
            }

            child.layout(childLeft, childTop, childLeft + width, childTop + height);
        }
    }

    private int getChildMarginLeft(LayoutParams params) {
        if (params.leftMargin == 0) {
            return (int) (getWidth() * params.leftMarginPercent);
        }
        return params.leftMargin;
    }

    private int getChildMarginTop(LayoutParams params) {
        if (params.topMargin == 0) {
            return (int) (getHeight() * params.topMarginPercent);
        }
        return params.topMargin;
    }

    private int getChildMarginRight(LayoutParams params) {
        if (params.rightMargin == 0) {
            return (int) (getWidth() * params.rightMarginPercent);
        }
        return params.rightMargin;
    }

    private int getChildMarginBottom(LayoutParams params) {
        if (params.bottomMargin == 0) {
            return (int) (getHeight() * params.bottomMarginPercent);
        }
        return params.bottomMargin;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(widthSize, heightSize);

        for (View child : this) {
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            int width;
            int height;
            if (params.width == LayoutParams.WRAP_CONTENT) {
                width = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.AT_MOST);
            } else if (params.width == LayoutParams.MATCH_PARENT) {
                width = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
            } else if (params.width == LayoutParams.PARENT_PERCENT) {
                width = MeasureSpec.makeMeasureSpec((int) (widthSize * params.widthPercent), MeasureSpec.EXACTLY);
            } else {
                width = MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY);
            }
            if (params.height == LayoutParams.WRAP_CONTENT) {
                height = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST);
            } else if (params.height == LayoutParams.MATCH_PARENT) {
                height = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
            } else if (params.height == LayoutParams.PARENT_PERCENT) {
                height = MeasureSpec.makeMeasureSpec((int) (heightSize * params.heightPercent), MeasureSpec.EXACTLY);
            } else {
                height = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY);
            }
            child.measure(width, height);
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return generateDefaultLayoutParams();
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        public static final int PARENT_PERCENT = 0;

        public float widthPercent;
        public float heightPercent;
        public float leftMarginPercent;
        public float topMarginPercent;
        public float rightMarginPercent;
        public float bottomMarginPercent;
        public int leftMargin;
        public int topMargin;
        public int rightMargin;
        public int bottomMargin;
        public int gravity;

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
            if (layoutParams instanceof LayoutParams) {
                LayoutParams params = (LayoutParams) layoutParams;
                widthPercent = params.widthPercent;
                heightPercent = params.heightPercent;
                leftMarginPercent = params.leftMarginPercent;
                topMarginPercent = params.topMarginPercent;
                rightMarginPercent = params.rightMarginPercent;
                bottomMarginPercent = params.bottomMarginPercent;
                leftMargin = params.leftMargin;
                topMargin = params.topMargin;
                rightMargin = params.rightMargin;
                bottomMargin = params.bottomMargin;
                gravity = params.gravity;
            }
        }

        public LayoutParams(float width, float height) {
            super(0, 0);
            widthPercent = width;
            heightPercent = height;
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(Context context, AttributeSet attrs) {
            super(0, 0);
            TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.PercentBasedLayout_Layout);

            // Grab width
            if (!arr.hasValue(R.styleable.PercentBasedLayout_Layout_layout_width)) {
                throw new RuntimeException("You must supply a layout_width attribute.");
            } else if (TypedValue.TYPE_DIMENSION == getType(arr, R.styleable.PercentBasedLayout_Layout_layout_width)) {
                width = arr.getDimensionPixelSize(R.styleable.PercentBasedLayout_Layout_layout_width, 0);
            } else if (TypedValue.TYPE_FLOAT == getType(arr, R.styleable.PercentBasedLayout_Layout_layout_width)) {
                widthPercent = arr.getFloat(R.styleable.PercentBasedLayout_Layout_layout_width, 0);
            } else {
                width = arr.getInt(R.styleable.PercentBasedLayout_Layout_layout_width, MATCH_PARENT);
            }

            // Grab height
            if (!arr.hasValue(R.styleable.PercentBasedLayout_Layout_layout_height)) {
                throw new RuntimeException("You must supply a layout_height attribute.");
            } else if (TypedValue.TYPE_DIMENSION == getType(arr, R.styleable.PercentBasedLayout_Layout_layout_height)) {
                height = arr.getDimensionPixelSize(R.styleable.PercentBasedLayout_Layout_layout_height, 0);
            } else if (TypedValue.TYPE_FLOAT == getType(arr, R.styleable.PercentBasedLayout_Layout_layout_height)) {
                heightPercent = arr.getFloat(R.styleable.PercentBasedLayout_Layout_layout_height, 0);
            } else {
                height = arr.getInt(R.styleable.PercentBasedLayout_Layout_layout_height, MATCH_PARENT);
            }

            // Grab margin
            if (arr.hasValue(R.styleable.PercentBasedLayout_Layout_layout_margin)) {
                if (TypedValue.TYPE_DIMENSION == getType(arr, R.styleable.PercentBasedLayout_Layout_layout_margin)) {
                    leftMargin
                            = topMargin
                            = rightMargin
                            = bottomMargin
                            = arr.getDimensionPixelSize(R.styleable.PercentBasedLayout_Layout_layout_margin, 0);
                } else {
                    leftMarginPercent
                            = topMarginPercent
                            = rightMarginPercent
                            = bottomMarginPercent
                            = arr.getFloat(R.styleable.PercentBasedLayout_Layout_layout_margin, 0);
                }
            }

            // Grab margin left
            if (arr.hasValue(R.styleable.PercentBasedLayout_Layout_layout_marginLeft)) {
                if (TypedValue.TYPE_DIMENSION == getType(arr, R.styleable.PercentBasedLayout_Layout_layout_marginLeft)) {
                    leftMargin = arr.getDimensionPixelSize(R.styleable.PercentBasedLayout_Layout_layout_marginLeft, leftMargin);
                } else {
                    leftMarginPercent = arr.getFloat(R.styleable.PercentBasedLayout_Layout_layout_marginLeft, leftMarginPercent);
                }
            }

            // Grab margin top
            if (arr.hasValue(R.styleable.PercentBasedLayout_Layout_layout_marginTop)) {
                if (TypedValue.TYPE_DIMENSION == getType(arr, R.styleable.PercentBasedLayout_Layout_layout_marginTop)) {
                    topMargin = arr.getDimensionPixelSize(R.styleable.PercentBasedLayout_Layout_layout_marginTop, topMargin);
                } else {
                    topMarginPercent = arr.getFloat(R.styleable.PercentBasedLayout_Layout_layout_marginTop, topMarginPercent);
                }
            }

            // Grab margin right
            if (arr.hasValue(R.styleable.PercentBasedLayout_Layout_layout_marginRight)) {
                if (TypedValue.TYPE_DIMENSION == getType(arr, R.styleable.PercentBasedLayout_Layout_layout_marginRight)) {
                    rightMargin = arr.getDimensionPixelSize(R.styleable.PercentBasedLayout_Layout_layout_marginRight, rightMargin);
                } else {
                    rightMarginPercent = arr.getFloat(R.styleable.PercentBasedLayout_Layout_layout_marginRight, rightMarginPercent);
                }
            }

            // Grab margin bottom
            if (arr.hasValue(R.styleable.PercentBasedLayout_Layout_layout_marginBottom)) {
                if (TypedValue.TYPE_DIMENSION == getType(arr, R.styleable.PercentBasedLayout_Layout_layout_marginBottom)) {
                    bottomMargin = arr.getDimensionPixelSize(R.styleable.PercentBasedLayout_Layout_layout_marginBottom, bottomMargin);
                } else {
                    bottomMarginPercent = arr.getFloat(R.styleable.PercentBasedLayout_Layout_layout_marginBottom, bottomMarginPercent);
                }
            }

            gravity = arr.getInt(R.styleable.PercentBasedLayout_Layout_layout_gravity, DEFAULT_CHILD_GRAVITY);

            arr.recycle();
        }

        private int getType(TypedArray array, int index) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return array.getType(index);
            }
            try {
                Field numEntriesField = AssetManager.class.getDeclaredField("STYLE_NUM_ENTRIES");
                numEntriesField.setAccessible(true);
                final int STYLE_NUM_ENTRIES = numEntriesField.getInt(null);

                Field typeField = AssetManager.class.getDeclaredField("STYLE_TYPE");
                typeField.setAccessible(true);
                final int STYLE_TYPE = typeField.getInt(null);

                Field mDataField = TypedArray.class.getDeclaredField("mData");
                mDataField.setAccessible(true);

                final int[] mData = (int[]) mDataField.get(array);
                index *= STYLE_NUM_ENTRIES;
                return mData[index + STYLE_TYPE];
            } catch (Exception e) {
                e.printStackTrace();
            }

            return TypedValue.TYPE_NULL;
        }
    }
}
