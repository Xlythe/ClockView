package com.xlythe.view.clock;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * A clock hand, turned about the middle of the face.
 *
 * <p>The view covers the whole face and the hand turns about the view's own middle, so a hand
 * drawn on a face-sized canvas needs nothing else: whatever the art puts at the middle of the
 * canvas is what the hand turns about. That is the default, and it costs a canvas of transparent
 * pixels around every hand - most of the file, and all of it once decoded.
 *
 * <p>Art cropped to the hand can say where the crop came from instead, with {@link
 * #setHandBounds} or the {@code handOffsetX}/{@code handOffsetY}/{@code handWidth}/{@code
 * handHeight} attributes. The view still covers the face and still turns about the middle of it;
 * only where the art is drawn changes. The middle of the face is then wherever those bounds put
 * it, which is the point: it can be inside the art, where a hand carries a counterweight or a
 * disc past the pivot, or outside it, where a hand stops short of the middle.
 */
public class ClockHandView extends AppCompatImageView {
    private float rotation = Float.NaN;

    private float handOffsetX = 0f;
    private float handOffsetY = 0f;
    private float handWidth = 1f;
    private float handHeight = 1f;

    /**
     * Whether the bounds were asked for. Filling the view is not the same as being told to fill
     * it: left alone, the hand is drawn the way an ImageView draws anything, honouring its scale
     * type and its padding, and that is what every hand already relies on.
     */
    private boolean hasHandBounds = false;

    public ClockHandView(Context context) {
        super(context);
    }

    public ClockHandView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        readHandBounds(context, attrs);
    }

    public ClockHandView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readHandBounds(context, attrs);
    }

    @TargetApi(21)
    public ClockHandView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        readHandBounds(context, attrs);
    }

    private void readHandBounds(Context context, @Nullable AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ClockHandView);
        try {
            if (a.hasValue(R.styleable.ClockHandView_handOffsetX)
                    || a.hasValue(R.styleable.ClockHandView_handOffsetY)
                    || a.hasValue(R.styleable.ClockHandView_handWidth)
                    || a.hasValue(R.styleable.ClockHandView_handHeight)) {
                setHandBounds(
                        a.getFloat(R.styleable.ClockHandView_handOffsetX, handOffsetX),
                        a.getFloat(R.styleable.ClockHandView_handOffsetY, handOffsetY),
                        a.getFloat(R.styleable.ClockHandView_handWidth, handWidth),
                        a.getFloat(R.styleable.ClockHandView_handHeight, handHeight));
            }
        } finally {
            a.recycle();
        }
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
    }

    /**
     * Places cropped hand art on the face. Every value is a fraction of this view, and the view
     * is expected to cover the face, so they are the same numbers the crop was taken with: a hand
     * cut from rows 201 to 498 of a thousand-pixel canvas is {@code offsetY = 0.201} and {@code
     * height = 0.297}.
     *
     * <p>Call it again whenever the art changes. Two hands cropped from the same canvas rarely
     * share a crop, so the bounds belong to the drawable rather than to the view.
     */
    public void setHandBounds(float offsetX, float offsetY, float width, float height) {
        if (hasHandBounds && handOffsetX == offsetX && handOffsetY == offsetY
                && handWidth == width && handHeight == height) {
            return;
        }
        handOffsetX = offsetX;
        handOffsetY = offsetY;
        handWidth = width;
        handHeight = height;
        hasHandBounds = true;
        postInvalidate();
    }

    /** Goes back to drawing the hand across the whole view, for art drawn on a full canvas. */
    public void clearHandBounds() {
        if (!hasHandBounds) {
            return;
        }
        handOffsetX = 0f;
        handOffsetY = 0f;
        handWidth = 1f;
        handHeight = 1f;
        hasHandBounds = false;
        postInvalidate();
    }

    public boolean hasHandBounds() {
        return hasHandBounds;
    }

    @Override
    public void setRotation(float rotation) {
        if (this.rotation != rotation) {
            this.rotation = rotation;
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        if (!Float.isNaN(rotation)) {
            canvas.rotate(rotation, getWidth() / 2f, getHeight() / 2f);
        }
        if (hasHandBounds) {
            drawHand(canvas);
        } else {
            super.onDraw(canvas);
        }
        canvas.restore();
    }

    /**
     * Draws the art where the crop says it belongs, rather than letting the scale type decide.
     * A scale type fits art to the view, and art that no longer fills the view is exactly what
     * this is for.
     */
    private void drawHand(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }
        int width = getWidth();
        int height = getHeight();
        int left = Math.round(handOffsetX * width);
        int top = Math.round(handOffsetY * height);
        drawable.setBounds(left, top,
                left + Math.round(handWidth * width),
                top + Math.round(handHeight * height));
        drawable.draw(canvas);
    }
}
