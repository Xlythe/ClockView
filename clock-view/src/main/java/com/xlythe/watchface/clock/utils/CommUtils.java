package com.xlythe.watchface.clock.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.AnyThread;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableStatusCodes;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    public static void broadcast(final Context context, final String path, final String message) {
        final GoogleApiClient client = new GoogleApiClient.Builder(context.getApplicationContext()).addApi(Wearable.API).build();
        sExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                ConnectionResult result = client.blockingConnect(TIMEOUT, TimeUnit.MILLISECONDS);
                if (!result.isSuccess()) {
                    Log.w(TAG, "Failed to connect to GoogleApiClient: " + CommUtils.toString(result));
                    return;
                }
                broadcast(client, path, message);
                client.disconnect();
            }
        });
    }

    /**
     * Send a message to all connected devices
     */
    @WorkerThread
    public static void broadcast(GoogleApiClient client, final String path, final String message) {
        NodeApi.GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(client).await(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!result.getStatus().isSuccess()) {
            Log.w(TAG, "Failed to call sendMessage: " + CommUtils.toString(result.getStatus()));
            return;
        }

        List<Node> nodes = result.getNodes();
        for (Node n : nodes) {
            Status status = Wearable.MessageApi.sendMessage(client, n.getId(), path, message.getBytes()).await(TIMEOUT, TimeUnit.MILLISECONDS).getStatus();
            if (!status.isSuccess()) {
                Log.w(TAG, "Failed to call sendMessage: " + CommUtils.toString(status));
                return;
            }
        }
    }

    /**
     * Send a message to a specific device
     */
    @AnyThread
    public static void unicast(final Context context, final String id, final String path, final String message) {
        final GoogleApiClient client = new GoogleApiClient.Builder(context.getApplicationContext()).addApi(Wearable.API).build();
        sExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                ConnectionResult result = client.blockingConnect(TIMEOUT, TimeUnit.MILLISECONDS);
                if (!result.isSuccess()) {
                    Log.w(TAG, "Failed to connect to GoogleApiClient: " + CommUtils.toString(result));
                    return;
                }
                unicast(client, id, path, message);
                client.disconnect();
            }
        });
    }

    /**
     * Send a message to a specific device
     */
    @WorkerThread
    public static void unicast(GoogleApiClient client, final String id, final String path, final String message) {
        Status status = Wearable.MessageApi.sendMessage(client, id, path, message.getBytes()).await(TIMEOUT, TimeUnit.MILLISECONDS).getStatus();
        if (!status.isSuccess()) {
            Log.w(TAG, "Failed to call sendMessage: " + CommUtils.toString(status));
        }
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
    private static void put(final Context context,
                            final String key,
                            final String value,
                            final boolean runBugfixHelper) {
        final GoogleApiClient client = new GoogleApiClient.Builder(context.getApplicationContext())
                .addApi(Wearable.API)
                .build();
        final BugfixHelper bugfixHelper = runBugfixHelper ? new BugfixHelper(context) : null;
        sExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                ConnectionResult result = client.blockingConnect(TIMEOUT, TimeUnit.MILLISECONDS);
                if (!result.isSuccess()) {
                    Log.w(TAG, "Failed to connect to GoogleApiClient: " + CommUtils.toString(result));
                    return;
                }

                if (bugfixHelper != null) {
                    bugfixHelper.onConnected(key, value);
                }

                put(client, key, value);

                client.disconnect();
            }
        });
    }

    /**
     * Put a message that can be read on all devices
     */
    @WorkerThread
    public static void put(GoogleApiClient client, final String key, final String value) {
        PutDataRequest request = PutDataRequest.create("/" + key);
        request.setData(value.getBytes());
        Status status = Wearable.DataApi.putDataItem(client, request).await(TIMEOUT, TimeUnit.MILLISECONDS).getStatus();
        if (!status.isSuccess()) {
            Log.w(TAG, "Failed to call putDataItem: " + CommUtils.toString(status));
        }
    }

    /**
     * Retrieve a message saved via put
     */
    @AnyThread
    public static void get(final GoogleApiClient client, final String key, final Callback callback) {
        sExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                final String value = get(client, key);
                sHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onCallback(value);
                    }
                });
            }
        });
    }

    /**
     * Retrieve a message saved via put
     */
    @WorkerThread
    @Nullable
    public static String get(GoogleApiClient client, final String key) {
        // We want a node id. Remote is better, I guess.
        NodeApi.GetConnectedNodesResult nodeResult = Wearable.NodeApi.getConnectedNodes(client).await(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!nodeResult.getStatus().isSuccess()) {
            Log.w(TAG, "Failed to call getConnectedNodes: " + CommUtils.toString(nodeResult.getStatus()));
            return null;
        }

        List<Node> nodes = nodeResult.getNodes();
        Node node;
        if (nodes.isEmpty()) {
            NodeApi.GetLocalNodeResult localNodeResult = Wearable.NodeApi.getLocalNode(client).await(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!localNodeResult.getStatus().isSuccess()) {
                Log.w(TAG, "Failed to call getLocalNode: " + CommUtils.toString(localNodeResult.getStatus()));
                return null;
            }
            node = localNodeResult.getNode();
        } else {
            node = nodes.get(0);
        }
        String nodeId = node.getId();

        // Figure out the uri...
        Uri uri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(nodeId).path("/" + key).build();

        // Open up the uri
        DataApi.DataItemResult dataResult = Wearable.DataApi.getDataItem(client, uri).await(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!dataResult.getStatus().isSuccess()) {
            Log.w(TAG, "Failed to call getDataItem: " + CommUtils.toString(dataResult.getStatus()));
            return null;
        }

        return CommUtils.toString(dataResult.getDataItem());
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
        private final GoogleApiClient mClient;

        private final Handler mHandler = new Handler(Looper.getMainLooper());

        @Nullable
        private DataApi.DataListener mListener;

        @Nullable
        private final Activity mActivity;
        @Nullable
        private final Application mApplication;
        @Nullable
        private final ActivityLifecycleCallbacks mActivityLifecycleCallbacks;

        @AnyThread
        BugfixHelper(Context context) {
            mContext = context;
            mClient = new GoogleApiClient.Builder(context.getApplicationContext())
                    .addApi(Wearable.API)
                    .build();

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
            ConnectionResult result = mClient.blockingConnect(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!result.isSuccess()) {
                Log.w(TAG, "Failed to connect to GoogleApiClient: " + CommUtils.toString(result));
                return;
            }
            mListener = new DataApi.DataListener() {
                @Override
                public void onDataChanged(DataEventBuffer dataEventBuffer) {
                    if (equals(value, get(dataEventBuffer, key))) {
                        Log.v(TAG, "Sync detected properly. Bugfix canceled.");
                        cleanup();
                    }
                }

                private boolean equals(@Nullable Object a, @Nullable Object b) {
                    return a == b || (a != null && a.equals(b));
                }
            };
            Wearable.DataApi.addListener(mClient, mListener);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "Failed to detect sync. Retrying.");
                    CommUtils.put(mContext, key, EMPTY_VALUE, false /* runBugfixHelper */);
                    CommUtils.put(mContext, key, value, false /* runBugfixHelper */);
                    cleanup();
                }
            }, BUGFIX_DELAY);
        }

        private void cleanup() {
            mHandler.removeCallbacksAndMessages(null);

            if (mApplication != null) {
                mApplication.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
            }

            if (mClient.isConnected()) {
                if (mListener != null) {
                    Wearable.DataApi.removeListener(mClient, mListener);
                }
                mClient.disconnect();
            }
        }
    }

    @Nullable
    private static String toString(@Nullable DataItem dataItem) {
        if (dataItem != null && dataItem.getData() != null) {
            // Parse the bytes into something useful
            String value = new String(dataItem.getData());
            if (!EMPTY_VALUE.equals(value)) {
                return value;
            }
        }
        return null;
    }

    private static String toString(ConnectionResult result) {
        String errorMessage = result.getErrorMessage() == null
                ? WearableStatusCodes.getStatusCodeString(result.getErrorCode())
                : result.getErrorMessage();
        return "[" + result.getErrorCode() + "]" + errorMessage;
    }

    private static String toString(Status status) {
        String errorMessage = status.getStatusMessage() == null
                ? WearableStatusCodes.getStatusCodeString(status.getStatusCode())
                : status.getStatusMessage();
        return "[" + status.getStatusCode() + "]" + errorMessage;
    }
}
