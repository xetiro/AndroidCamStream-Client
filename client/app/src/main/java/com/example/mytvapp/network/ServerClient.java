package com.example.mytvapp.network;

import android.util.Log;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Implementation of a client based on Socket.IO websockets. This is a singleton class to keep
 * the state alive through different Activities of the Application.
 * <p>
 * Created by xetiro (aka Ruben Geraldes) on 2020/08/29.
 */
public class ServerClient {
    public static String TAG = "ServerClientDebug";

    private Socket mSocket = null;
    private String mServerIp = "http://localhost";
    private int mServerPort = 8080;

    private static ServerClient mInstance = null;

    private ServerClient() {
        // Private constructor is part of singleton implementation
    }

    public synchronized static ServerClient getmInstance() {
        if(mInstance == null) {
            mInstance = new ServerClient();
        }
        return mInstance;
    }

    public void init(String serverIp, int port) {
        mServerIp = serverIp;
        mServerPort = port;

        if(mSocket == null) {
            try {
                IO.Options options = new IO.Options();
                options.host = mServerIp;
                options.port = mServerPort;
                options.forceNew = true;
                options.reconnection = true;
                options.reconnectionAttempts = 5;
                options.reconnectionDelay = 1000;
                options.secure = true;
                String serverAddress = mServerIp + ":" + mServerPort;
                mSocket = IO.socket(serverAddress, options);
            } catch(URISyntaxException e) {
                // TODO consider to recover by asking the user to submit a new URI
                // Otherwise we are not recovering from this failure anymore and the client will
                // never connect to the server.
                e.printStackTrace();
            }
        }
    }

    /**
     * The connection to the server is explicitly issued by client activities.
     *
     * Register the socket listeners just before trying to connect, so we can receive feedback
     * from the connection state.
     */
    public void connect() {
        registerSocketListeners();
        mSocket.connect();
    }

    /**
     * This is  main method issued by the client activity to stream pictures to the server.
     */
    public void sendPicture() {
        mSocket.emit("sendPicture", "Testing sending picture: " + System.currentTimeMillis() + ".jpg");
    }

    /**
     * Client activities might issue an explicit disconnect at anytime.
     *
     * Unregister the socket listeners after issuing the disconnect to free resources.
     */
    public void disconnect() {
        mSocket.disconnect();
        unregisterSocketListeners();
    }

    private void registerSocketListeners() {
        mSocket.on(Socket.EVENT_CONNECT, onConneted);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectionError);
        mSocket.on(Socket.EVENT_RECONNECT, onReconnecting);
        mSocket.on(Socket.EVENT_RECONNECT_ERROR, onReconnecting);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnected);
        mSocket.on(Socket.EVENT_ERROR, onEventError);
    }

    private void unregisterSocketListeners() {
        mSocket.off(Socket.EVENT_CONNECT, onConneted);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectionError);
        mSocket.off(Socket.EVENT_RECONNECT, onReconnecting);
        mSocket.off(Socket.EVENT_RECONNECT_ERROR, onReconnecting);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnected);
        mSocket.off(Socket.EVENT_ERROR, onEventError);
    }

    /**
     * Callback functions for the socket listeners
     */
    private Emitter.Listener onConneted = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // We connected to the server successfully
            String reason = "no reason received.";
            if(args.length > 1) {
                reason = args[0].toString();
            }
            Log.d(TAG, "Connected to the server: " + reason);
        }
    };

    private Emitter.Listener onConnectionError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // We got an error while trying to connect
            // The socket will try to reconnect automatically as many times we set on the options
            String reason = "no reason received.";
            if(args.length > 1) {
                reason = args[0].toString();
            }
            Log.d(TAG, "Error while trying to connect: " + reason);
        }
    };

    private Emitter.Listener onReconnecting = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // Socket is trying to reconnect automatically
            Log.d(TAG, "Reconnecting to the server...");
        }
    };

    private Emitter.Listener onReconnectionError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // We fail to reconnect
            String reason = "no reason received.";
            if(args.length > 1) {
                reason = args[0].toString();
            }
            Log.d(TAG, "Reconnection failed: " + reason);
        }
    };

    private Emitter.Listener onDisconnected = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // We were disconnected from the server
            String reason = "no reason received.";
            if(args.length > 1) {
                reason = args[0].toString();
            }
            Log.d(TAG, "Disconnected from the server: " + reason);
        }
    };

    private Emitter.Listener onEventError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // Something went wrong with an event
            String reason = "no reason received.";
            if(args.length > 1) {
                reason = args[0].toString();
            }
            Log.d(TAG, "Something went wrong with the last event: " + reason);
        }
    };
}
