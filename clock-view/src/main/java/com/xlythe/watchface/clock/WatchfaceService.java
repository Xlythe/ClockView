package com.xlythe.watchface.clock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;
import com.xlythe.view.clock.ClockView;
import com.xlythe.view.clock.utils.BitmapUtils;

public abstract class WatchfaceService extends CanvasWatchFaceService {
    private static final String TAG = WatchfaceService.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    public abstract ClockView onCreateClockView(Context context);

    protected GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    protected void onConnected(Bundle bundle) {
    }

    protected void onDataChanged(DataEventBuffer dataEvents) {
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements
            DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {
        private static final int MAX_DELAY = 30 * 60 * 1000; // 30min
        private static final int DEFAULT_DELAY = 1000; // 1 sec

        // Comm variables
        private int mDelay = DEFAULT_DELAY;
        private Handler mHandler = new Handler();

        // View variables
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
            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();

            mWatchface.setCircular(false);
            mWatchface.setOnTimeTickListener(new ClockView.OnTimeTickListener() {
                @Override
                public void onTimeTick() {
                    invalidate();
                }
            });
        }

        @Override
        public void onConnected(Bundle bundle) {
            WatchfaceService.this.onConnected(bundle);
            Wearable.DataApi.addListener(mGoogleApiClient, this);
            mDelay = DEFAULT_DELAY;
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "onConnectionSuspended: " + i);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mGoogleApiClient.reconnect();
                }
            }, Math.min(MAX_DELAY, mDelay *= 2));
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d(TAG, "onConnectionFailed: " + connectionResult.getErrorCode());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mGoogleApiClient.connect();
                }
            }, Math.min(MAX_DELAY, mDelay *= 2));
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            WatchfaceService.this.onDataChanged(dataEvents);
            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    invalidate();
                    return;
                }
            }
        }

        @Override
        public void onDestroy() {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
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
            mWatchface.setSecondHandEnabled(!inAmbientMode);
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
            boolean lowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
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

            // Update the view dimensions
            BitmapUtils.measure(mWatchface, bounds);

            // Draw the view
            BitmapUtils.draw(mWatchface, canvas, bounds);
        }
    }
}
