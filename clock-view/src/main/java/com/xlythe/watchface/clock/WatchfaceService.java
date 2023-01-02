package com.xlythe.watchface.clock;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
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
import androidx.annotation.RequiresApi;
import androidx.annotation.UiContext;
import androidx.wear.watchface.CanvasType;
import androidx.wear.watchface.ComplicationSlot;
import androidx.wear.watchface.ComplicationSlotsManager;
import androidx.wear.watchface.RenderParameters;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.TapType;
import androidx.wear.watchface.WatchFace;
import androidx.wear.watchface.WatchFaceService;
import androidx.wear.watchface.WatchFaceType;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.complications.ComplicationSlotBounds;
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy;
import androidx.wear.watchface.complications.data.NoDataComplicationData;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import com.xlythe.view.clock.ClockView;
import com.xlythe.view.clock.ComplicationView;
import com.xlythe.view.clock.utils.BitmapUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import kotlin.coroutines.Continuation;

import static com.xlythe.watchface.clock.utils.KotlinUtils.addObserver;

@TargetApi(Build.VERSION_CODES.O)
@RequiresApi(Build.VERSION_CODES.O)
public abstract class WatchfaceService extends WatchFaceService {
    private ClockView mWatchface;
    private WatchfaceRenderer mRenderer;
    private final ClockView.OnTimeTickListener mOnTimeTickListener = this::invalidate;

    public abstract ClockView onCreateClockView(@UiContext Context context);

    @UiContext
    protected Context getThemedContext() {
        return new ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat);
    }

    private void createClockView() {
        mWatchface = onCreateClockView(getThemedContext());
    }

    @NonNull
    @Override
    protected ComplicationSlotsManager createComplicationSlotsManager(@NonNull CurrentUserStyleRepository currentUserStyleRepository) {
        createClockView();

        Collection<ComplicationSlot> complicationSlots = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (ComplicationView complicationView : mWatchface.getComplicationViews()) {
                // Note: We'll be drawing the ComplicationSlots ourselves, so it doesn't matter
                // what builder we want to use. However, BACKGROUND is limited to 1 so we'll avoid that.
                complicationSlots.add(ComplicationSlot.createRoundRectComplicationSlotBuilder(
                                complicationView.getComplicationId(),
                                new DefaultCanvasComplicationFactory(complicationView),
                                complicationView.getSupportedComplicationTypes(),
                                new DefaultComplicationDataSourcePolicy(),
                                // Note: Complications steal touch focus before our watchface is given it. It's important to hide them.
                                new ComplicationSlotBounds(new RectF(0, 0, 0, 0)))
                        .build());
            }
        }
        return new ComplicationSlotsManager(complicationSlots, currentUserStyleRepository);
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
        WatchFace watchFace = new WatchFace(mWatchface.isDigitalEnabled() ? WatchFaceType.DIGITAL : WatchFaceType.ANALOG, mRenderer);
        watchFace.setTapListener((tapType, tapEvent, complicationSlot) -> {
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
            mWatchface.dispatchTouchEvent(motionEvent);
            motionEvent.recycle();
        });

        return watchFace;
    }

    private long toUptimeMillis(Instant instant) {
        long timeSinceInstant = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            timeSinceInstant = Duration.between(instant, Instant.now()).toMillis();
        }
        return SystemClock.uptimeMillis() - timeSinceInstant;
    }

    public void invalidate() {
        if (mRenderer != null) {
            mRenderer.invalidate();
        }
    }

    private class WatchfaceRenderer extends Renderer.CanvasRenderer2<Renderer.SharedAssets> {
        // Default for how long each frame is displayed at expected frame rate.
        private static final long FRAME_PERIOD_MS_DEFAULT = 1000L;

        private final Drawable.Callback mDrawableCallback;

        WatchfaceRenderer(
                SurfaceHolder surfaceHolder,
                WatchState watchState,
                ComplicationSlotsManager complicationsSlotsManager,
                CurrentUserStyleRepository currentUserStyleRepository) {
            super(surfaceHolder, currentUserStyleRepository, watchState, CanvasType.HARDWARE, FRAME_PERIOD_MS_DEFAULT, false);
            mWatchface.setOnInvalidateListener(this::invalidate);
            mWatchface.setOnTimeTickListener(mOnTimeTickListener);

            mWatchface.setHasBurnInProtection(watchState.hasBurnInProtection());
            mWatchface.setLowBitAmbient(watchState.hasLowBitAmbient());
            addObserver(watchState.isAmbient(), ambient -> {
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Map<Integer, ComplicationSlot> complicationSlots = complicationsSlotsManager.getComplicationSlots();
                for (ComplicationView view : mWatchface.getComplicationViews()) {
                    ComplicationSlot complicationSlot = complicationSlots.get(view.getComplicationId());
                    if (complicationSlot == null) {
                        view.setComplicationData(new NoDataComplicationData());
                        Log.d(ClockView.TAG, "Failed to find a valid complication slots. Returning dummy data.");
                        continue;
                    }

                    view.setComplicationData(complicationSlot.getComplicationData().getValue());
                    addObserver(complicationSlot.getComplicationData(), view::setComplicationData);
                }
            }
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
        public void render(@NonNull Canvas canvas, @NonNull Rect bounds, @NonNull ZonedDateTime zonedDateTime, @NonNull SharedAssets sharedAssets) {
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

            // Reset time so that complications can update themselves even if the timer hasn't ticked yet.
            mWatchface.resetTime();
        }

        @Override
        public void renderHighlightLayer(@NonNull Canvas canvas, @NonNull Rect rect, @NonNull ZonedDateTime zonedDateTime, @NonNull SharedAssets sharedAssets) {
            RenderParameters.HighlightLayer highlightLayer = getRenderParameters().getHighlightLayer();
            if (highlightLayer != null) {
                canvas.drawColor(highlightLayer.getBackgroundTint());
            }
        }

        @SuppressWarnings("rawtypes")
        @NonNull
        @Override
        protected SharedAssets createSharedAssets(@NonNull Continuation continuation) {
            return () -> {};
        }
    }
}
