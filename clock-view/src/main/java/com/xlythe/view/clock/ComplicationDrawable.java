package com.xlythe.view.clock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.xlythe.watchface.clock.utils.ResourceUtils;

public class ComplicationDrawable extends Drawable {
    private final static float ICON_RATIO_VERTICAL = 0.33f;
    private final static float ICON_RATIO_HORIZONTAL = 0.6f;
    private final static int MIN_TEXT_CHARACTERS = 7;
    private final static int MIN_TEXT_SIZE_SP = 5;
    private final static int MAX_TEXT_SIZE_SP = 30;
    private final static int DOT_STROKE_WIDTH_DP = 1;
    private final static int LINE_STROKE_WIDTH_DP = 1;
    @ColorInt final static int DEFAULT_COLOR = Color.WHITE;
    private final static int BACKGROUND_ALPHA = 76;
    private final static int TITLE_ALPHA = 180;

    private final Context mContext;
    private final Paint mBackgroundPaint = new Paint();
    private final Paint mIconPaint = new Paint();
    private final TextPaint mTextPaint = new TextPaint();
    private final TextPaint mTitlePaint = new TextPaint();
    private final Paint mDebugPaint = new Paint();
    boolean mShowBackground = true;
    @Nullable private final Drawable mIcon;
    @Nullable private final CharSequence mText;
    @Nullable private StaticLayout mTextLayout;
    @Nullable private Rect mTextLayoutRect;
    @Nullable private final CharSequence mTitle;
    @Nullable private StaticLayout mTitleLayout;
    @Nullable private Rect mTitleLayoutRect;

    @Nullable private ColorStateList mTint;

    public enum Style {
        FILL, LINE, DOT, EMPTY
    }

    public ComplicationDrawable(Context context) {
        mContext = context;
        mIcon = null;
        mText = null;
        mTitle = null;
        init();
    }

    ComplicationDrawable(Context context, @Nullable Drawable icon, @Nullable CharSequence text, @Nullable CharSequence title) {
        mContext = context;
        mIcon = icon;
        mText = text;
        mTitle = title;
        init();
    }

    private void init() {
        mBackgroundPaint.setColor(DEFAULT_COLOR);
        mBackgroundPaint.setAlpha(BACKGROUND_ALPHA);
        mBackgroundPaint.setAntiAlias(true);

        mIconPaint.setColor(DEFAULT_COLOR);
        mIconPaint.setAntiAlias(true);

        mTextPaint.setColor(DEFAULT_COLOR);
        mTextPaint.setTextSize(getMaxTextSize());
        mTextPaint.setAntiAlias(true);

        mTitlePaint.setColor(DEFAULT_COLOR);
        mTitlePaint.setAlpha(TITLE_ALPHA);
        mTitlePaint.setTextSize(getMaxTextSize());
        mTitlePaint.setAntiAlias(true);

        mDebugPaint.setColor(0xffff0000);
        mDebugPaint.setStyle(Paint.Style.STROKE);
        mDebugPaint.setStrokeWidth(1);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        mTint = tint;

        if (tint == null) {
            tint = ColorStateList.valueOf(DEFAULT_COLOR);
        }

        mBackgroundPaint.setColor(tint.getColorForState(getState(), tint.getDefaultColor()));
        mBackgroundPaint.setAlpha(BACKGROUND_ALPHA);
        if (mIcon != null) {
            mIcon.setTintList(tint);
        }
        mTextPaint.setColor(tint.getColorForState(getState(), tint.getDefaultColor()));
        mTitlePaint.setColor(tint.getColorForState(getState(), tint.getDefaultColor()));
        mTitlePaint.setAlpha(TITLE_ALPHA);
    }

    @Override
    protected boolean onStateChange(@NonNull int[] state) {
        if (mIcon != null) {
            mIcon.setState(state);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTintList(mTint);
        }
        return mTint != null;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.save();
        canvas.translate(getBounds().left, getBounds().top);

        // Draw the background
        if (mShowBackground) {
            float radius = getHeight() / 2f;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                float offset = mBackgroundPaint.getStyle() == Paint.Style.STROKE ? mBackgroundPaint.getStrokeWidth() : 0;
                canvas.drawRoundRect(offset, offset, getWidth() - offset, getHeight() - offset, radius, radius, mBackgroundPaint);
            } else {
                canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, mBackgroundPaint);
            }
        }

        if (mIcon != null) {
            mIcon.setColorFilter(mIconPaint.getColorFilter());
            mIcon.draw(canvas);
        }

        if (mTextLayout != null && mTextLayoutRect != null) {
            canvas.save();
            canvas.translate(mTextLayoutRect.left, mTextLayoutRect.top);
            mTextLayout.draw(canvas);
            canvas.restore();

            if (ClockView.DEBUG) {
                canvas.drawRect(mTextLayoutRect, mDebugPaint);
            }
        }

        if (mTitleLayout != null && mTitleLayoutRect != null) {
            canvas.save();
            canvas.translate(mTitleLayoutRect.left, mTitleLayoutRect.top);
            mTitleLayout.draw(canvas);
            canvas.restore();

            if (ClockView.DEBUG) {
                canvas.drawRect(mTitleLayoutRect, mDebugPaint);
            }
        }

        canvas.restore();
    }

    private boolean isHorizontal() {
        return getWidth() > getHeight();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);

        mTextPaint.setTextSize(getMaxTextSize());
        mTitlePaint.setTextSize(getMaxTextSize());
        if (isHorizontal()) {
            setHorizontalLayout();
        } else {
            setVerticalLayout();
        }
    }

    private void setHorizontalLayout() {
        Rect textBounds = new Rect();
        textBounds.left = getTextPaddingHorizontal();
        textBounds.top = getTextPaddingVertical();
        textBounds.right = getWidth() - getTextPaddingHorizontal();
        textBounds.bottom = getHeight() - getTextPaddingVertical();

        Rect titleBounds = new Rect();
        titleBounds.left = getTextPaddingHorizontal();
        titleBounds.top = getTextPaddingVertical();
        titleBounds.right = getWidth() - getTextPaddingHorizontal();
        titleBounds.bottom = getHeight() - getTextPaddingVertical();

        if (mIcon != null) {
            int iconSize = getIconSize();
            int iconPadding = getIconPadding();
            mIcon.setBounds(
                    iconPadding,
                    getHeight() / 2 - iconSize / 2,
                    iconSize + iconPadding,
                    getHeight() / 2 + iconSize / 2);
            textBounds.left = iconSize + 2 * iconPadding;
            titleBounds.left = iconSize + 2 * iconPadding;
        }

        boolean singleLine = mTitle != null && mText != null;
        if (singleLine) {
            titleBounds.bottom = titleBounds.top + titleBounds.height() / 2;
            textBounds.top = titleBounds.bottom;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mText != null) {
            mTextLayout = StaticLayout.Builder.obtain(mText, 0, mText.length(), mTextPaint, textBounds.width())
                    .setMaxLines(singleLine ? 1 : 2)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build();

            while ((mTextLayout.getHeight() > textBounds.height()
                    || (mTextLayout.getEllipsisCount(0) > 0 && mTextLayout.getEllipsisStart(0) < MIN_TEXT_CHARACTERS)
                    || (mTextLayout.getEllipsisCount(1) > 0 && mTextLayout.getEllipsisStart(1) < MIN_TEXT_CHARACTERS))
                    && mTextPaint.getTextSize() > getMinTextSize()) {
                mTextPaint.setTextSize(Math.max(mTextPaint.getTextSize() - 2f, getMinTextSize()));
                mTextLayout = StaticLayout.Builder.obtain(mText, 0, mText.length(), mTextPaint, textBounds.width())
                        .setMaxLines(singleLine ? 1 : 2)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .build();
            }

            mTextLayoutRect = textBounds;

            // Center the rect
            mTextLayoutRect.top = mTextLayoutRect.top + mTextLayoutRect.height() / 2 - mTextLayout.getHeight() / 2;
            mTextLayoutRect.bottom = mTextLayoutRect.top + mTextLayout.getHeight();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mTitle != null) {
            mTitleLayout = StaticLayout.Builder.obtain(mTitle, 0, mTitle.length(), mTitlePaint, titleBounds.width())
                    .setMaxLines(singleLine ? 1 : 2)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build();

            while ((mTitleLayout.getHeight() > titleBounds.height()
                    || (mTitleLayout.getEllipsisCount(0) > 0 && mTitleLayout.getEllipsisStart(0) < MIN_TEXT_CHARACTERS)
                    || (mTitleLayout.getEllipsisCount(1) > 0 && mTitleLayout.getEllipsisStart(1) < MIN_TEXT_CHARACTERS))
                    && mTitlePaint.getTextSize() > getMinTextSize()) {
                mTitlePaint.setTextSize(Math.max(mTitlePaint.getTextSize() - 2f, getMinTextSize()));
                mTitleLayout = StaticLayout.Builder.obtain(mTitle, 0, mTitle.length(), mTitlePaint, titleBounds.width())
                        .setMaxLines(singleLine ? 1 : 2)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .build();
            }

            mTitleLayoutRect = titleBounds;

            // Center the rect
            mTitleLayoutRect.top = mTitleLayoutRect.top + mTitleLayoutRect.height() / 2 - mTitleLayout.getHeight() / 2;
            mTitleLayoutRect.bottom = mTitleLayoutRect.top + mTitleLayout.getHeight();
        }

        // If the text is incredibly short, we'll center the text instead.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && singleLine) {
            if (mTextPaint.measureText(mText.toString()) < getWidth() / 2f
                    && mTitlePaint.measureText(mTitle.toString()) < getWidth() / 2f) {
                mTextLayout = StaticLayout.Builder.obtain(mText, 0, mText.length(), mTextPaint, textBounds.width())
                        .setMaxLines(1)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .build();
                mTitleLayout = StaticLayout.Builder.obtain(mTitle, 0, mTitle.length(), mTitlePaint, titleBounds.width())
                        .setMaxLines(1)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .build();
            }
        }
    }

    private void setVerticalLayout() {
        Rect textBounds = new Rect();
        textBounds.left = getTextPaddingHorizontal();
        textBounds.top = getTextPaddingVertical();
        textBounds.right = getWidth() - getTextPaddingHorizontal();
        textBounds.bottom = getHeight() - getTextPaddingVertical();

        Rect titleBounds = new Rect();
        titleBounds.left = getTextPaddingHorizontal();
        titleBounds.top = getTextPaddingVertical();
        titleBounds.right = getWidth() - getTextPaddingHorizontal();
        titleBounds.bottom = getHeight() - getTextPaddingVertical();

        boolean splitPane = (mIcon != null && (mText != null || mTitle != null))
                || (mText != null && mTitle != null);

        if (mIcon != null) {
            int iconSize = getIconSize();
            mIcon.setBounds(
                    getWidth() / 2 - iconSize / 2,
                    splitPane ? getHeight() / 2 - iconSize : getHeight() / 2 + iconSize / 2,
                    getWidth() / 2 + iconSize / 2,
                    splitPane ? getHeight() / 2 : getHeight() / 2 + iconSize / 2);
            textBounds.top = mIcon.getBounds().bottom;
            titleBounds.top = mIcon.getBounds().bottom;
        }

        if (mIcon == null && mText != null && mTitle != null) {
            textBounds.bottom = textBounds.top + textBounds.height() / 2;
            titleBounds.top = textBounds.bottom;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mText != null) {
            mTextLayout = StaticLayout.Builder.obtain(mText, 0, mText.length(), mTextPaint, textBounds.width())
                    .setMaxLines(1)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build();

            while ((mTextLayout.getHeight() > textBounds.height()
                    || (mTextLayout.getEllipsisCount(0) > 0 && mTextLayout.getEllipsisStart(0) < MIN_TEXT_CHARACTERS))
                    && mTextPaint.getTextSize() > getMinTextSize()) {
                mTextPaint.setTextSize(Math.max(mTextPaint.getTextSize() - 2f, getMinTextSize()));
                mTextLayout = StaticLayout.Builder.obtain(mText, 0, mText.length(), mTextPaint, textBounds.width())
                        .setMaxLines(1)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .build();
            }

            mTextLayoutRect = textBounds;

            // Center the rect
            mTextLayoutRect.top = mTextLayoutRect.top + mTextLayoutRect.height() / 2 - mTextLayout.getHeight() / 2;
            mTextLayoutRect.bottom = mTextLayoutRect.top + mTextLayout.getHeight();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && mTitle != null
                && (mIcon == null || mText == null)) {
            mTitleLayout = StaticLayout.Builder.obtain(mTitle, 0, mTitle.length(), mTitlePaint, titleBounds.width())
                    .setMaxLines(1)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build();

            while ((mTitleLayout.getHeight() > titleBounds.height()
                    || (mTitleLayout.getEllipsisCount(0) > 0 && mTitleLayout.getEllipsisStart(0) < MIN_TEXT_CHARACTERS))
                    && mTitlePaint.getTextSize() > getMinTextSize()) {
                mTitlePaint.setTextSize(Math.max(mTitlePaint.getTextSize() - 2f, getMinTextSize()));
                mTitleLayout = StaticLayout.Builder.obtain(mTitle, 0, mTitle.length(), mTitlePaint, titleBounds.width())
                        .setMaxLines(1)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .build();
            }

            mTitleLayoutRect = titleBounds;

            // Center the rect
            mTitleLayoutRect.top = mTitleLayoutRect.top + mTitleLayoutRect.height() / 2 - mTitleLayout.getHeight() / 2;
            mTitleLayoutRect.bottom = mTitleLayoutRect.top + mTitleLayout.getHeight();
        }
    }

    protected float getMinTextSize() {
        return ResourceUtils.inScaleIndependentPixels(getContext(), MIN_TEXT_SIZE_SP);
    }

    protected float getMaxTextSize() {
        return ResourceUtils.inScaleIndependentPixels(getContext(), MAX_TEXT_SIZE_SP);
    }

    protected float getDotStrokeWidth() {
        return ResourceUtils.inDisplayIndependentPixels(getContext(), DOT_STROKE_WIDTH_DP);
    }

    protected float getLineStrokeWidth() {
        return ResourceUtils.inDisplayIndependentPixels(getContext(), LINE_STROKE_WIDTH_DP);
    }

    @Override
    public void setAlpha(int alpha) {
        mIconPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mIconPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    private int getIconSize() {
        return (int) (getHeight() * (isHorizontal() ? ICON_RATIO_HORIZONTAL : ICON_RATIO_VERTICAL));
    }

    private int getIconPadding() {
        return (getHeight() - getIconSize()) / 2;
    }

    private int getTextPaddingHorizontal() {
        return getHeight() / (isHorizontal() ? 2 : 6);
    }

    private int getTextPaddingVertical() {
        return getHeight() / 6;
    }

    protected int getWidth() {
        return getBounds().width();
    }

    protected int getHeight() {
        return getBounds().height();
    }

    protected Context getContext() {
        return mContext;
    }

    public static class Builder {
        private final Context mContext;
        private Style mStyle = Style.FILL;
        private CharSequence mTitle;
        private CharSequence mText;
        private Drawable mIcon;

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

        public ComplicationDrawable build() {
            ComplicationDrawable drawable = new ComplicationDrawable(mContext, mIcon, mText, mTitle);
            switch (mStyle) {
                case FILL:
                    drawable.mShowBackground = true;
                    break;
                case LINE:
                    drawable.mBackgroundPaint.setStyle(Paint.Style.STROKE);
                    drawable.mBackgroundPaint.setStrokeWidth(drawable.getLineStrokeWidth());
                    break;
                case DOT:
                    drawable.mBackgroundPaint.setStyle(Paint.Style.STROKE);
                    drawable.mBackgroundPaint.setStrokeWidth(drawable.getDotStrokeWidth());
                    drawable.mBackgroundPaint.setPathEffect(new DashPathEffect(new float[] { 6f, 3f}, 0));
                    break;
                case EMPTY:
                    drawable.mShowBackground = false;
                    break;
            }
            return drawable;
        }
    }
}
