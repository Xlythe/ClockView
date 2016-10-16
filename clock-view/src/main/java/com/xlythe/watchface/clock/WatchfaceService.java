package com.xlythe.watchface.clock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.xlythe.view.clock.ClockView;
import com.xlythe.view.clock.utils.BitmapUtils;

public abstract class WatchfaceService extends CanvasWatchFaceService {

    private Engine mEngine;

    public abstract ClockView onCreateClockView(Context context);

    @Override
    public Engine onCreateEngine() {
        mEngine = new Engine();
        return mEngine;
    }

    public void invalidate() {
        if (mEngine != null) {
            mEngine.invalidate();
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        private ClockView mWatchface;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchfaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle
                            .BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            mWatchface = onCreateClockView(WatchfaceService.this);
            mWatchface.setOnTimeTickListener(new ClockView.OnTimeTickListener() {
                @Override
                public void onTimeTick() {
                    invalidate();
                }
            });
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            // Start a timer for the seconds hand while we're visible, assuming we support seconds
            mWatchface.setSecondHandEnabled(visible);
            if (mWatchface.isSecondHandEnabled()) {
                mWatchface.start();
            } else {
                mWatchface.stop();
            }
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mWatchface.setAmbientModeEnabled(inAmbientMode);
            if (mWatchface.isSecondHandEnabled()) {
                mWatchface.start();
            } else {
                mWatchface.stop();
            }
            invalidate();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            if (Build.VERSION.SDK_INT >= 20) {
                mWatchface.onApplyWindowInsets(insets);
            }
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mWatchface.setLowBitAmbient(properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false));
            mWatchface.setBurnInProtection(properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false));
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Some watches are not perfectly square, because of a small bump at the bottom
            // We still want to draw as if they are square, to avoid squishing the watchface
            if (bounds.width() != bounds.height()) {
                if (bounds.width() < bounds.height()) {
                    bounds.right = bounds.left + bounds.height();
                } else {
                    bounds.bottom = bounds.top + bounds.width();
                }
            }

            // Invalidate the old canvas
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // Invalidate the time
            mWatchface.onTimeTick();

            // Draw the view
            BitmapUtils.draw(mWatchface, canvas, bounds);
        }
    }
}
