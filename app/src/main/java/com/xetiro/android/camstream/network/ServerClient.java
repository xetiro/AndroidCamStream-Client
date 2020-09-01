package com.xetiro.android.camstream.network;

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

    private static String EVENT_AUTHENTICATION = "onAuthentication";

    private Socket mSocket = null;
    private String mServerIp = "localhost";
    private int mServerPort = 8080;

    private String mUsername = null;
    private String mPassword = null;

    // A single callback to the client aimed to be registered by the current Activity
    private ServerResultCallback mSingleCallback = null;

    private static ServerClient mInstance = null;

    private ServerClient() {
        // Private constructor is part of singleton implementation
    }

    public synchronized static ServerClient getInstance() {
        if (mInstance == null) {
            mInstance = new ServerClient();
        }
        return mInstance;
    }

    public void init(String username, String password, String serverIp, int port) {
        mUsername = username;
        mPassword = password;
        mServerIp = serverIp;
        mServerPort = port;

        if (mSocket == null) {
            try {   // Try to create the socket with the server
                IO.Options options = new IO.Options();
                options.forceNew = true;
                options.multiplex = true;
                options.secure = true;
                options.reconnection = true;
                options.reconnectionDelay = 5000;
                options.reconnectionAttempts = 10;
                String serverAddress = "http://" + mServerIp + ":" + mServerPort;
                mSocket = IO.socket(serverAddress, options);
                Log.d(TAG, "ServerClient initialized successfully.");
            } catch (URISyntaxException e) {
                // We failed to connect, consider to inform the user
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "ServerClient already initialized. Clearing sockets. Try again...");
            mSocket.disconnect();
            mSocket = null;
        }
    }

    public void registerCallback(ServerResultCallback callback) {
        mSingleCallback = callback;
    }

    public void unregisterCallback() {
        mSingleCallback = null;
    }

    /**
     * The connection to the server is explicitly issued by client activities.
     * <p>
     * Register the socket listeners just before trying to connect, so we can receive feedback
     * from the connection state.
     */
    public void connect() {
        if (mSocket != null && !mSocket.connected() && mUsername != null) {
            unregisterSocketListeners();
            registerSocketListeners();
            mSocket.connect();
        } else {
            Log.d(TAG, "Cannot connect because socket is null or already connected or username isn't defined.");
        }
    }

    /**
     * This is  main method issued by the client activity to stream pictures to the server.
     */
    public void sendImage(byte[] image) {
        if (mSocket != null && mSocket.connected()) {
            mSocket.emit("receiveImage", image);
        } else {
            Log.d(TAG, "Cannot send message because socket is null or disconnected");
        }
    }

    /**
     * Client activities might issue an explicit disconnect at anytime.
     * <p>
     * Unregister the socket listeners after issuing the disconnect to free resources.
     */
    public void disconnect() {
        if (mSocket != null) {
            mSocket.disconnect();
            unregisterSocketListeners();
        } else {
            Log.d(TAG, "Cannot disconnect because socket is null.");
        }
    }

    private void registerSocketListeners() {
        if (mSocket != null) {
            mSocket.on(Socket.EVENT_CONNECT, onConneted);
            mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectionError);
            mSocket.on(Socket.EVENT_RECONNECT, onReconnecting);
            mSocket.on(Socket.EVENT_RECONNECT_ERROR, onReconnecting);
            mSocket.on(Socket.EVENT_DISCONNECT, onDisconnected);
            mSocket.on(EVENT_AUTHENTICATION, onAuthentication);
            mSocket.on(Socket.EVENT_ERROR, onEventError);
        } else {
            Log.d(TAG, "Cannot register listeners because socket is null.");
        }
    }

    private void unregisterSocketListeners() {
        if (mSocket != null) {
            mSocket.off(Socket.EVENT_CONNECT, onConneted);
            mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectionError);
            mSocket.off(Socket.EVENT_RECONNECT, onReconnecting);
            mSocket.off(Socket.EVENT_RECONNECT_ERROR, onReconnecting);
            mSocket.off(Socket.EVENT_DISCONNECT, onDisconnected);
            mSocket.off(EVENT_AUTHENTICATION, onAuthentication);
            mSocket.off(Socket.EVENT_ERROR, onEventError);
        } else {
            Log.d(TAG, "Cannot unregister listeners because socket is null.");
        }
    }

    /**
     * Callback functions for the socket listeners
     */
    private Emitter.Listener onConneted = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // We connected to the server successfully
            Log.d(TAG, "Connected to the server! Starting authentication...");
            mSocket.emit("authenticate", mUsername, mPassword, EVENT_AUTHENTICATION);
        }
    };

    private Emitter.Listener onConnectionError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // We got an error while trying to connect
            // The socket will try to reconnect automatically as many times we set on the options
            String reason = "no reason received.";
            if (args.length > 0) {
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
            if (args.length > 0) {
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
            if (args.length > 0) {
                reason = args[0].toString();
            }
            Log.d(TAG, "Disconnected from the server: " + reason);
        }
    };

    private Emitter.Listener onAuthentication = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            boolean result = (boolean) args[0];
            if(mSingleCallback != null) {
                mSingleCallback.onConnected(result);
            }
            Log.d(TAG, "onAuthentication: " + result);
        }
    };

    private Emitter.Listener onEventError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // Something went wrong with an event
            String reason = "no reason received.";
            if (args.length > 0) {
                reason = args[0].toString();
            }
            Log.d(TAG, "Something went wrong with the last event: " + reason);
        }
    };
}