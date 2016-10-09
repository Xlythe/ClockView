package com.xlythe.watchface.clock.utils;

import android.content.Context;
import android.net.Uri;

import com.google.android.gms.common.api.GoogleApiClient;
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
    private static final ExecutorService sExecutorService = Executors.newSingleThreadExecutor();

    /**
     * Send a message to all connected devices
     */
    public static void broadcast(final Context context, final String path, final String message) {
        final GoogleApiClient client = new GoogleApiClient.Builder(context.getApplicationContext()).addApi(Wearable.API).build();
        sExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                client.blockingConnect(1000, TimeUnit.MILLISECONDS);
                broadcast(client, path, message);
                client.disconnect();
            }
        });
    }

    /**
     * Send a message to all connected devices
     */
    public static void broadcast(GoogleApiClient client, final String path, final String message) {
        NodeApi.GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(client).await();
        List<Node> nodes = result.getNodes();
        for (Node n : nodes) {
            Wearable.MessageApi.sendMessage(client, n.getId(), path, message.getBytes());
        }
    }

    /**
     * Send a message to a specific device
     */
    public static void unicast(final Context context, final String id, final String path, final String message) {
        final GoogleApiClient client = new GoogleApiClient.Builder(context.getApplicationContext()).addApi(Wearable.API).build();
        sExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                client.blockingConnect(1000, TimeUnit.MILLISECONDS);
                unicast(client, id, path, message);
                client.disconnect();
            }
        });
    }

    /**
     * Send a message to a specific device
     */
    public static void unicast(GoogleApiClient client, final String id, final String path, final String message) {
        Wearable.MessageApi.sendMessage(client, id, path, message.getBytes());
    }

    /**
     * Put a message that can be read on all devices
     */
    public static void put(Context context, final String key, final String value) {
        final GoogleApiClient client = new GoogleApiClient.Builder(context.getApplicationContext()).addApi(Wearable.API).build();
        sExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                client.blockingConnect(1000, TimeUnit.MILLISECONDS);
                put(client, key, value);
                client.disconnect();
            }
        });
    }

    /**
     * Put a message that can be read on all devices
     */
    public static void put(GoogleApiClient client, final String key, final String value) {
        PutDataRequest request = PutDataRequest.create("/" + key);
        request.setData(value.getBytes());
        Wearable.DataApi.putDataItem(client, request);
    }

    /**
     * Retrieve a message saved via put
     */
    public static void get(final GoogleApiClient client, final String key, final Callback callback) {
        sExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                String value = get(client, key);
                callback.onCallback(value);
            }
        });
    }

    /**
     * Retrieve a message saved via put
     */
    public static String get(GoogleApiClient client, final String key) {
        // We want a node id. Remote is better, I guess.
        Node node;
        List<Node> nodes = Wearable.NodeApi.getConnectedNodes(client).await().getNodes();
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
