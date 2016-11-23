package com.xlythe.watchface.clock.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.AnyThread;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
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
        final GoogleApiClient client = new GoogleApiClient.Builder(context.getApplicationContext()).addApi(Wearable.API).build();
        sExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                ConnectionResult result = client.blockingConnect(TIMEOUT, TimeUnit.MILLISECONDS);
                if (!result.isSuccess()) {
                    Log.w(TAG, "Failed to connect to GoogleApiClient: [" + result.getErrorCode() + "]" + result.getErrorMessage());
                    return;
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
                return new String(event.getDataItem().getData());
            }
        }
        return null;
    }

    public interface Callback {
        void onCallback(String result);
    }
}
