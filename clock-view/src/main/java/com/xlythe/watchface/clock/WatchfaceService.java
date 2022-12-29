package com.xlythe.watchface.clock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiContext;
import androidx.wear.watchface.CanvasType;
import androidx.wear.watchface.ComplicationSlot;
import androidx.wear.watchface.ComplicationSlotsManager;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.TapEvent;
import androidx.wear.watchface.TapType;
import androidx.wear.watchface.WatchFace;
import androidx.wear.watchface.WatchFaceService;
import androidx.wear.watchface.WatchFaceType;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import com.xlythe.view.clock.ClockView;
import com.xlythe.view.clock.utils.BitmapUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

import kotlin.coroutines.Continuation;

public abstract class WatchfaceService extends WatchFaceService {

    private WatchfaceRenderer mRenderer;
    private final ClockView.OnTimeTickListener mOnTimeTickListener = this::invalidate;

    public abstract ClockView onCreateClockView(@UiContext Context context);

    @UiContext
    protected Context getThemedContext() {
        return new ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat);
    }

    @Nullable
    @Override
    protected WatchFace createWatchFace(
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull WatchState watchState,
            @NonNull ComplicationSlotsManager complicationSlotsManager,
            @NonNull CurrentUserStyleRepository currentUserStyleRepository,
            @NonNull Continuation<? super WatchFace> continuation) {
        mRenderer = new WatchfaceRenderer(surfaceHolder, watchState, complicationSlotsManager, currentUserStyleRepository);
        WatchFace watchFace = new WatchFace(mRenderer.mWatchface.isDigitalEnabled() ? WatchFaceType.DIGITAL : WatchFaceType.ANALOG, mRenderer);
        watchFace.setTapListener((tapType, tapEvent) -> {
            int action;
            switch (tapType) {
                case TapType.DOWN:
                    action = MotionEvent.ACTION_DOWN;
                    break;
                case TapType.UP:
                    action = MotionEvent.ACTION_UP;
                    break;
                case TapType.CANCEL:
                    action = MotionEvent.ACTION_CANCEL;
                    break;
                default:
                    Log.w(ClockView.TAG, "Unknown tap type: " + tapType);
                    return;
            }
            MotionEvent motionEvent = MotionEvent.obtain(
                    SystemClock.uptimeMillis(),
                    toUptimeMillis(tapEvent.getTapTime()),
                    action,
                    tapEvent.getXPos(),
                    tapEvent.getYPos(),
                    0);
            motionEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            mRenderer.mWatchface.dispatchTouchEvent(motionEvent);
            motionEvent.recycle();
        });
        return watchFace;
    }

    private long toUptimeMillis(Instant instant) {
        long timeSinceInstant = 0;
        if (Build.VERSION.SDK_INT >= 26) {
            timeSinceInstant = Duration.between(instant, Instant.now()).toMillis();
        }
        return SystemClock.uptimeMillis() - timeSinceInstant;
    }

    public void invalidate() {
        if (mRenderer != null) {
            mRenderer.invalidate();
        }
    }

    private class WatchfaceRenderer extends Renderer.CanvasRenderer {
        // Default for how long each frame is displayed at expected frame rate.
        private static final long FRAME_PERIOD_MS_DEFAULT = 1000L;

        private final ComplicationSlotsManager mComplicationsSlotsManager;
        private final ClockView mWatchface;
        private final Drawable.Callback mDrawableCallback;

        WatchfaceRenderer(
                SurfaceHolder surfaceHolder,
                WatchState watchState,
                ComplicationSlotsManager complicationsSlotsManager,
                CurrentUserStyleRepository currentUserStyleRepository) {
            super(surfaceHolder, currentUserStyleRepository, watchState, CanvasType.HARDWARE, FRAME_PERIOD_MS_DEFAULT);
            mComplicationsSlotsManager = complicationsSlotsManager;
            mWatchface = onCreateClockView(getThemedContext());
            mWatchface.setOnInvalidateListener(this::invalidate);
            mWatchface.setOnTimeTickListener(mOnTimeTickListener);

            mWatchface.setHasBurnInProtection(watchState.hasBurnInProtection());
            watchState.isAmbient().addObserver(ambient -> {
                mWatchface.setAmbientModeEnabled(ambient);
                invalidate();
            });

            mDrawableCallback = new Drawable.Callback() {
                @Override
                public void invalidateDrawable(@NonNull Drawable who) {
                    mWatchface.invalidateDrawable(who);
                }

                @Override
                public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
                    mWatchface.scheduleDrawable(who, what, when);
                }

                @Override
                public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
                    mWatchface.unscheduleDrawable(who, what);
                }
            };
        }

        private void interceptDrawableCallbacks(View view) {
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    interceptDrawableCallbacks(viewGroup.getChildAt(i));
                }
            }

            if (view == mWatchface) {
                // Ignore
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                interceptDrawableCallbacks(view.getForeground());
            }
            interceptDrawableCallbacks(view.getBackground());
        }

        private void interceptDrawableCallbacks(@Nullable Drawable drawable) {
            if (drawable == null) {
                return;
            }

            drawable.setCallback(mDrawableCallback);
        }

        @Override
        public void render(@NonNull Canvas canvas, @NonNull Rect bounds, @NonNull ZonedDateTime zonedDateTime) {
            // Some watches are not perfectly square, because of a small bump at the bottom
            // We still want to draw as if they are square, to avoid squishing the watchface
            if (bounds.width() != bounds.height()) {
                if (bounds.width() < bounds.height()) {
                    bounds.right = bounds.left + bounds.height();
                } else {
                    bounds.bottom = bounds.top + bounds.width();
                }
            }
            interceptDrawableCallbacks(mWatchface);

            // Invalidate the old canvas
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // Invalidate the time. Temporarily remove the OnTimeTickListener so that we don't cause an infinite loop
            mWatchface.setOnTimeTickListener(null);
            if (Build.VERSION.SDK_INT >= 26) {
                mWatchface.setTime(zonedDateTime);
            }
            mWatchface.onTimeTick();
            mWatchface.setOnTimeTickListener(mOnTimeTickListener);

            // Override the ambient mode setting to use whatever the render wants us to use.
            switch (getRenderParameters().getDrawMode()) {
                case INTERACTIVE:
                case LOW_BATTERY_INTERACTIVE:
                    mWatchface.setAmbientModeEnabled(false);
                    break;
                case AMBIENT:
                case MUTE:
                default:
                    mWatchface.setAmbientModeEnabled(true);
                    break;
            }

            // Draw the view
            BitmapUtils.draw(mWatchface, canvas, bounds);
        }

        @Override
        public void renderHighlightLayer(@NonNull Canvas canvas, @NonNull Rect rect, @NonNull ZonedDateTime zonedDateTime) {
            if (getRenderParameters().getHighlightLayer() != null) {
                canvas.drawColor(getRenderParameters().getHighlightLayer().getBackgroundTint());
            }

            for (ComplicationSlot complication : mComplicationsSlotsManager.getComplicationSlots().values()) {
                if (complication.isEnabled()) {
                    complication.renderHighlightLayer(canvas, zonedDateTime, getRenderParameters());
                }
            }
        }
    }
}
