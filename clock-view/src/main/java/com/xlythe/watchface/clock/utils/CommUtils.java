package com.xlythe.watchface.clock.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
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
                    Log.w(TAG, "Failed to connect to GoogleApiClient: [" + result.getErrorCode() + "]" + result.getErrorMessage());
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
            Log.w(TAG, "Failed to call sendMessage: [" + result.getStatus().getStatusCode() + "]" + result.getStatus().getStatusMessage());
            return;
        }

        List<Node> nodes = result.getNodes();
        for (Node n : nodes) {
            Status status = Wearable.MessageApi.sendMessage(client, n.getId(), path, message.getBytes()).await(TIMEOUT, TimeUnit.MILLISECONDS).getStatus();
            if (!status.isSuccess()) {
                Log.w(TAG, "Failed to call sendMessage: [" + status.getStatusCode() + "]" + status.getStatusMessage());
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
                    Log.w(TAG, "Failed to connect to GoogleApiClient: [" + result.getErrorCode() + "]" + result.getErrorMessage());
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
            Log.w(TAG, "Failed to call sendMessage: [" + status.getStatusCode() + "]" + status.getStatusMessage());
        }
    }

    /**
     * Put a message that can be read on all devices
     */
    @AnyThread
    public static void put(Context context, final String key, final String value) {
        GoogleApiClient client = new GoogleApiClient.Builder(context.getApplicationContext())
                .addApi(Wearable.API)
                .build();
        put(context, client, key, value, true /* isFirstRun */);
    }

    /**
     * Put a message that can be read on all devices
     */
    @AnyThread
    private static void put(final Context context, final GoogleApiClient client, final String key, final String value, final boolean isFirstRun) {
        final BugfixHelper bugfixHelper = isFirstRun ? new BugfixHelper(context, client) : null;
        sExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected()) {
                    ConnectionResult result = client.blockingConnect(TIMEOUT, TimeUnit.MILLISECONDS);
                    if (!result.isSuccess()) {
                        Log.w(TAG, "Failed to connect to GoogleApiClient: [" + result.getErrorCode() + "]" + result.getErrorMessage());
                        return;
                    }
                }

                if (bugfixHelper != null) {
                    bugfixHelper.onConnected(key, value);
                }

                put(client, key, value);

                // Only disconnect if the bugfix helper isn't running
                if (bugfixHelper != null) {
                    client.disconnect();
                }
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
            Log.w(TAG, "Failed to call putDataItem: [" + status.getStatusCode() + "]" + status.getStatusMessage());
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
        NodeApi.GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(client).await(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!result.getStatus().isSuccess()) {
            Log.w(TAG, "Failed to call putDataItem: [" + result.getStatus().getStatusCode() + "]" + result.getStatus().getStatusMessage());
            return null;
        }

        List<Node> nodes = result.getNodes();
        Node node;
        if (nodes.isEmpty()) {
            node = Wearable.NodeApi.getLocalNode(client).await().getNode();
        } else {
            node = nodes.get(0);
        }
        String nodeId = node.getId();

        // Figure out the uri...
        Uri uri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(nodeId).path("/" + key).build();

        // Open up the uri
        DataItem dataItem = Wearable.DataApi.getDataItem(client, uri).await().getDataItem();

        if (dataItem != null) {
            // Parse the bytes into something useful
            return new String(dataItem.getData());
        } else {
            return null;
        }
    }

    /**
     * Retrieve a message saved via put
     */
    @AnyThread
    public static String get(DataEventBuffer dataEvents, String key) {
        for (DataEvent event : dataEvents) {
            if (event.getDataItem().getUri().getPath().equals("/" + key)) {
                // Parse the bytes into something useful
                if (event.getDataItem().getData() == null) {
                    return null;
                }
                return new String(event.getDataItem().getData());
            }
        }
        return null;
    }

    public interface Callback {
        void onCallback(String result);
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
     * Due to a bug in GMSCore, put does not always work.
     * We'll listen for changes and, if we don't hear any, we'll try again.
     */
    private static class BugfixHelper {
        private static final long BUGFIX_DELAY = 300;

        private final Context mContext;
        private final GoogleApiClient mClient;

        private final Handler mHandler = new Handler(Looper.getMainLooper());
        private final DataApi.DataListener mListener;

        @Nullable
        private final Activity mActivity;
        @Nullable
        private final Application mApplication;
        @Nullable
        private final ActivityLifecycleCallbacks mActivityLifecycleCallbacks;

        BugfixHelper(Context context, final GoogleApiClient client) {
            mContext = context;
            mClient = client;
            mListener = new DataApi.DataListener() {
                @Override
                public void onDataChanged(DataEventBuffer dataEventBuffer) {
                    cleanup(true /* disconnect */);
                }
            };

            if (context instanceof Activity) {
                mActivity = (Activity) context;
                mApplication = mActivity.getApplication();
                mActivityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityStopped(Activity activity) {
                        if (mActivity == activity) {
                            cleanup(true /* disconnect */);
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

        void onConnected(final String key, final String value) {
            Wearable.DataApi.addListener(mClient, mListener);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    put(mContext, mClient, key, null, false /* isFirstRun */);
                    put(mContext, mClient, key, value, false /* isFirstRun */);
                    cleanup(false /* disconnect */);
                }
            }, BUGFIX_DELAY);
        }

        private void cleanup(boolean disconnect) {
            mHandler.removeCallbacksAndMessages(null);
            Wearable.DataApi.removeListener(mClient, mListener);
            if (mApplication != null) {
                mApplication.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
            }
            if (disconnect) {
                mClient.disconnect();
            }
        }
    }
}
