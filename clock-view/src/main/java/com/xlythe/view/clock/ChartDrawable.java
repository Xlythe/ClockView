package com.xlythe.view.clock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChartDrawable extends ComplicationDrawable {
    private final float mWeightSum;
    private final List<Float> mWeights;
    private final List<Integer> mColors;

    private final Paint mOutlinePaint = new Paint();
    private final Paint mForegroundPaint = new Paint();
    private final RectF mPieChartBounds = new RectF();

    ChartDrawable(
            Context context,
            @Nullable Drawable icon,
            @Nullable CharSequence text,
            @Nullable CharSequence title,
            @ColorInt int backgroundColor,
            float weightSum,
            List<Float> weights,
            List<Integer> colors) {
        super(context, icon, text, title);
        mWeightSum = weightSum;
        mWeights = weights;
        mColors = colors;

        mOutlinePaint.setColor(backgroundColor);
        mOutlinePaint.setAntiAlias(true);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setStrokeWidth(getLineStrokeWidth());

        mForegroundPaint.setAntiAlias(true);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int startingAngle = -90;
        for (int i = 0; i < mWeights.size(); i++) {
            float weight = mWeights.get(i);
            @ColorInt int color = mColors.get(i);
            float degrees = 360f / mWeightSum * weight;

            mForegroundPaint.setColor(color);
            canvas.drawArc(mPieChartBounds, startingAngle, degrees, true, mForegroundPaint);
            canvas.drawArc(mPieChartBounds, startingAngle, degrees, true, mOutlinePaint);
            startingAngle += degrees;
        }

        if (getIcon() != null) {
            getIcon().draw(canvas);
        }
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);

        float width = Math.min(getWidth(), getHeight()) - 2 * getLineStrokeWidth();
        float height = width;

        mPieChartBounds.set(getWidth() / 2f - width / 2f, getHeight() / 2f - height / 2f, getWidth() / 2f + width / 2f, getHeight() / 2f + height / 2f);

        if (getIcon() != null) {
            int iconSize = (int) (width / 4);
            getIcon().setBounds((int) mPieChartBounds.right - iconSize, (int) mPieChartBounds.bottom - iconSize, (int) mPieChartBounds.right, (int) mPieChartBounds.bottom);
        }
    }

    protected void setStyle(Style style) {
        super.setStyle(Style.EMPTY); // We'll draw our own background
        switch (style) {
            case FILL:
                mOutlinePaint.setAlpha(BACKGROUND_ALPHA);
                break;
            case LINE:
                mOutlinePaint.setStyle(Paint.Style.STROKE);
                mOutlinePaint.setStrokeWidth(getLineStrokeWidth());
                break;
            case DOT:
                mOutlinePaint.setStyle(Paint.Style.STROKE);
                mOutlinePaint.setStrokeWidth(getDotStrokeWidth());
                mOutlinePaint.setPathEffect(new DashPathEffect(new float[] { 6f, 3f}, 0));
                break;
            case EMPTY:
                mOutlinePaint.setAlpha(0);
                break;
        }
    }

    public static class Builder {
        private final Context mContext;
        private Style mStyle = Style.FILL;
        private CharSequence mTitle;
        private CharSequence mText;
        private Drawable mIcon;
        @ColorInt private int mBackgroundColor;
        private float mWeightSum;
        private final List<Float> mWeights = new ArrayList<>();
        private final List<Integer> mColors = new ArrayList<>();

        public Builder(Context context) {
            mContext = context;
        }

        public Builder style(Style style) {
            mStyle = style;
            return this;
        }

        public Builder title(CharSequence title) {
            mTitle = title;
            return this;
        }

        public Builder text(CharSequence text) {
            mText = text;
            return this;
        }

        public Builder icon(Drawable icon) {
            mIcon = icon;
            return this;
        }

        public Builder backgroundColor(@ColorInt int color) {
            mBackgroundColor = color;
            return this;
        }

        public Builder addElement(float weight, @ColorInt int color) {
            mWeightSum += weight;
            mWeights.add(weight);
            mColors.add(color);
            return this;
        }

        public ChartDrawable build() {
            ChartDrawable drawable = new ChartDrawable(
                    mContext,
                    mIcon,
                    mText,
                    mTitle,
                    mBackgroundColor,
                    mWeightSum,
                    mWeights,
                    mColors);
            drawable.setStyle(mStyle);
            return drawable;
        }
    }
}
