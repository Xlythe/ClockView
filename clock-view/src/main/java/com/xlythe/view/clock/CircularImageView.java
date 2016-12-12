package com.xlythe.view.clock;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/**
 * Custom ImageView for circular images in Android while maintaining the
 * best draw performance and supporting custom borders & selectors.
 */
public class CircularImageView extends ImageView implements Animator.OnInvalidateListener {
    private static final String STATE_SUPER = "super";
    private static final String STATE_ENABLED = "circular_enabled";

    // For logging purposes
    private static final String TAG = CircularImageView.class.getSimpleName();

    // Border & Selector configuration variables
    private int mCanvasSize;

    // Objects used for the actual drawing
    private BitmapShader mShader;
    private Bitmap mImage;
    private Paint mPaint;

    private boolean mEnableCircular = true;

    public CircularImageView(Context context) {
        super(context);
        init();
    }

    public CircularImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CircularImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * Initializes paint objects
     */
    private void init() {
        // Initialize paint objects
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }

    @Override
    public void onInvalidate() {
        invalidateImage();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(STATE_SUPER, super.onSaveInstanceState());
        bundle.putBoolean(STATE_ENABLED, mEnableCircular);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        mEnableCircular = bundle.getBoolean(STATE_ENABLED);
        super.onRestoreInstanceState(bundle.getParcelable(STATE_SUPER));
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (!mEnableCircular) {
            super.onDraw(canvas);
            return;
        }

        // Don't draw anything without an image
        if (mImage == null) {
            mImage = drawableToBitmap(getDrawable());
            if (mImage == null) {
                return;
            }
        }

        // Nothing to draw (Empty bounds)
        if (mImage.getHeight() == 0 || mImage.getWidth() == 0) {
            return;
        }

        // Update mShader if canvas size has changed
        int oldCanvasSize = mCanvasSize;
        mCanvasSize = getWidth() < getHeight() ? getWidth() : getHeight();
        if (oldCanvasSize != mCanvasSize) {
            updateBitmapShader();
        }

        // Apply mShader to mPaint
        mPaint.setShader(mShader);

        // Draw the circular image itself
        canvas.save();
        canvas.translate(-mCanvasSize / 2, -mCanvasSize / 2);
        canvas.translate(getWidth() / 2, getHeight() / 2);
        canvas.drawCircle(mCanvasSize / 2, mCanvasSize / 2, mCanvasSize / 2, mPaint);
        canvas.restore();
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        overrideCallback();
        invalidateImage();
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        overrideCallback();
        invalidateImage();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        overrideCallback();
        invalidateImage();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        invalidateImage(bm);
    }

    /**
     * In order to support animations, we need to listen to the drawable's request for invalidation.
     */
    private void overrideCallback() {
        if (getDrawable() == null) {
            return;
        }

        if (getDrawable() instanceof Animator) {
            Animator animator = (Animator) getDrawable();
            animator.setOnInvalidateListener(this);
        }
    }

    private void invalidateImage() {
        invalidateImage(drawableToBitmap(getDrawable()));
    }

    private void invalidateImage(Bitmap bitmap) {
        mImage = bitmap;
        if (mCanvasSize > 0) {
            updateBitmapShader();
        }
    }

    /**
     * Convert a drawable object into a Bitmap.
     *
     * @param drawable Drawable to extract a Bitmap from.
     * @return A Bitmap created from the drawable parameter.
     */
    @Nullable
    public Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            // Don't do anything without a proper drawable
            return null;
        } else if (drawable instanceof BitmapDrawable) {
            // Use the getBitmap() method instead if BitmapDrawable
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (!(intrinsicWidth > 0 && intrinsicHeight > 0)) {
            if (!(getWidth() > 0 && getHeight() > 0)) {
                return null;
            } else {
                intrinsicWidth = getWidth();
                intrinsicHeight = getHeight();
            }
        }

        try {
            // Create Bitmap object out of the drawable
            Bitmap bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Encountered OutOfMemoryError while generating bitmap!", e);
            return null;
        }
    }

    /**
     * Re-initializes the shader texture used to fill in
     * the Circle upon drawing.
     */
    public void updateBitmapShader() {
        if (mImage == null)
            return;
        mShader = new BitmapShader(mImage, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        if (mCanvasSize != mImage.getWidth() || mCanvasSize != mImage.getHeight()) {
            Matrix matrix = new Matrix();
            float scale = (float) mCanvasSize / (float) mImage.getWidth();
            matrix.setScale(scale, scale);
            mShader.setLocalMatrix(matrix);
        }
    }

    public void setCircularEnabled(boolean enabled) {
        mEnableCircular = enabled;
    }
}
