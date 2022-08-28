package com.xlythe.watchface.clock.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

/**
 * Shorthand for communicating with Android Wear
 */
public class CommUtils {
    private static final String TAG = CommUtils.class.getSimpleName();
    private static final ExecutorService sExecutorService = Executors.newSingleThreadExecutor();
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static final long TIMEOUT = 1000;
    private static final String EMPTY_VALUE = "com.xlythe.watchface.clock.utils.EMPTY_VALUE";

    /**
     * Send a message to all connected devices
     */
    @AnyThread
    private static void broadcast(Context context, String path, String message) {
        sExecutorService.submit(() -> {
            try {
                List<Node> nodes = Tasks.await(Wearable.getNodeClient(context).getConnectedNodes(), TIMEOUT, TimeUnit.MILLISECONDS);
                for (Node n : nodes) {
                    Tasks.await(Wearable.getMessageClient(context).sendMessage(n.getId(), path, message.getBytes()), TIMEOUT, TimeUnit.MILLISECONDS);
                }
            } catch (ExecutionException e) {
                Log.w(TAG, "Failed to call sendMessage", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "Failed to call sendMessage", e);
            } catch (TimeoutException e) {
                Log.w(TAG, "Failed to call sendMessage", e);
            }
        });
    }

    /**
     * Send a message to a specific device
     */
    @AnyThread
    public static void unicast(Context context, String id, String path, String message) {
        sExecutorService.submit(() -> {
            try {
                Tasks.await(Wearable.getMessageClient(context).sendMessage(id, path, message.getBytes()), TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                Log.w(TAG, "Failed to call sendMessage", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "Failed to call sendMessage", e);
            } catch (TimeoutException e) {
                Log.w(TAG, "Failed to call sendMessage", e);
            }
        });
    }

    /**
     * Put a message that can be read on all devices
     */
    @AnyThread
    public static void put(Context context, final String key, final String value) {
        put(context, key, value, true /* runBugfixHelper */);
    }

    /**
     * Put a message that can be read on all devices
     */
    @AnyThread
    private static void put(Context context, String key, String value, boolean runBugfixHelper) {
        final BugfixHelper bugfixHelper = runBugfixHelper ? new BugfixHelper(context) : null;
        sExecutorService.submit(() -> {
            if (bugfixHelper != null) {
                bugfixHelper.onConnected(key, value);
            }

            PutDataRequest request = PutDataRequest.create("/" + key);
            request.setData(value.getBytes());

            try {
                Tasks.await(Wearable.getDataClient(context).putDataItem(request), TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                Log.w(TAG, "Failed to call putDataItem", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "Failed to call putDataItem", e);
            } catch (TimeoutException e) {
                Log.w(TAG, "Failed to call putDataItem", e);
            }
        });
    }

    /**
     * Retrieve a message saved via put
     */
    @AnyThread
    public static void get(Context context, String key, Callback callback) {
        sExecutorService.submit(() -> {
            String value = get(context, key);
            sHandler.post(() -> callback.onCallback(value));
        });
    }

    /**
     * Retrieve a message saved via put
     */
    @WorkerThread
    @Nullable
    private static String get(Context context, String key) {
        // We want a node id. Remote is better, I guess.
        try {
            List<Node> nodes = Tasks.await(Wearable.getNodeClient(context).getConnectedNodes(), TIMEOUT, TimeUnit.MILLISECONDS);

            Node node = nodes.isEmpty() ?
                    Tasks.await(Wearable.getNodeClient(context).getLocalNode(), TIMEOUT, TimeUnit.MILLISECONDS) : nodes.get(0);

            // Figure out the uri...
            Uri uri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(node.getId()).path("/" + key).build();

            // Open up the uri
            DataItem dataItem = Tasks.await(Wearable.getDataClient(context).getDataItem(uri), TIMEOUT, TimeUnit.MILLISECONDS);
            return CommUtils.toString(dataItem);
        } catch (ExecutionException e) {
            Log.w(TAG, "Failed to call getDataItem", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "Failed to call getDataItem", e);
        } catch (TimeoutException e) {
            Log.w(TAG, "Failed to call getDataItem", e);
        }

        return null;
    }

    /**
     * Retrieve a message saved via put
     */
    @AnyThread
    @Nullable
    public static String get(DataEventBuffer dataEvents, String key) {
        for (DataEvent event : dataEvents) {
            if (event.getDataItem().getUri().getPath().equals("/" + key)) {
                return CommUtils.toString(event.getDataItem());
            }
        }
        return null;
    }

    public interface Callback {
        @UiThread
        void onCallback(@Nullable String result);
    }

    private static class ActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {

        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    }

    /**
     * Due to a bug in GMSCore, sending data sometimes fails because it thinks it's already in the state given.
     * (eg. You send 'true' and it thinks it already is in 'true' mode so it does nothing). To get around this
     * bug, we first send our original message. Then, if we notice an eerie silence, we send it again with the wrong
     * value and immediately again with the right value.
     */
    private static class BugfixHelper {
        private static final long BUGFIX_DELAY = 350;

        private final Context mContext;

        private final Handler mHandler = new Handler(Looper.getMainLooper());

        @Nullable
        private DataClient.OnDataChangedListener mListener;

        @Nullable
        private final Activity mActivity;
        @Nullable
        private final Application mApplication;
        @Nullable
        private final ActivityLifecycleCallbacks mActivityLifecycleCallbacks;

        @AnyThread
        BugfixHelper(Context context) {
            mContext = context;

            if (context instanceof Activity) {
                mActivity = (Activity) context;
                mApplication = mActivity.getApplication();
                mActivityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityStopped(Activity activity) {
                        if (mActivity == activity) {
                            Log.v(TAG, "Activity stopped. Canceling bugfix.");
                            cleanup();
                        }
                    }
                };
                mApplication.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
            } else {
                mActivity = null;
                mApplication = null;
                mActivityLifecycleCallbacks = null;
            }
        }

        @WorkerThread
        void onConnected(final String key, final String value) {
            mListener = new DataClient.OnDataChangedListener() {
                @Override
                public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
                    if (equals(value, get(dataEventBuffer, key))) {
                        Log.v(TAG, "Sync detected properly. Bugfix canceled.");
                        cleanup();
                    }
                }

                private boolean equals(@Nullable Object a, @Nullable Object b) {
                    return a == b || (a != null && a.equals(b));
                }
            };
            Wearable.getDataClient(mContext).addListener(mListener);
            mHandler.postDelayed(() -> {
                Log.v(TAG, "Failed to detect sync. Retrying.");
                CommUtils.put(mContext, key, EMPTY_VALUE, false /* runBugfixHelper */);
                CommUtils.put(mContext, key, value, false /* runBugfixHelper */);
                cleanup();
            }, BUGFIX_DELAY);
        }

        private void cleanup() {
            mHandler.removeCallbacksAndMessages(null);

            if (mApplication != null) {
                mApplication.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
            }

            if (mListener != null) {
                Wearable.getDataClient(mContext).removeListener(mListener);
                mListener = null;
            }
        }
    }

    @Nullable
    private static String toString(@Nullable DataItem dataItem) {
        if (dataItem != null) {
            // Parse the bytes into something useful
            String value = new String(dataItem.getData());
            if (!EMPTY_VALUE.equals(value)) {
                return value;
            }
        }
        return null;
    }
}
