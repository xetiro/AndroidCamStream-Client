package com.example.mytvapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * The main activity is the one that connects the camera, grab the frames and streams the frames
 * to the server.
 *
 * Created by xetiro (aka Ruben Geraldes) on 27/09/2020.
 */
public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivityDebug";
    private static int ACCESS_CAMERA_REQUEST_CODE = 1;

    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    private PreviewView mCameraPreview;
    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setResolutionSpinner();

        try {
            mSocket = IO.socket("http://192.168.1.14:9000");
            Log.d(TAG, "Socket.IO");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mCameraPreview = findViewById(R.id.cameraView);

        if (cameraPermissionGranted()) {
            startCameraPreview();
        } else {
            requestCameraPermission();
        }
    }

    private Emitter.Listener onConnect = args -> {
        Log.d(TAG, "onConnect");
        // TODO start broadcasting the camera image
        mSocket.emit("newImage", "Hello from android...");
    };

    private Emitter.Listener onConnectionError = args -> Log.d(TAG, "onConnectionError" + args[0].toString());
    private Emitter.Listener onDisconnect = args -> Log.d(TAG, "onDisconnect");

    @Override
    public void onResume() {
        super.onResume();

        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectionError);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);

        mSocket.connect();

        Log.d(TAG, "Socket connected: " + mSocket.connected());
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: " + permissions.length);
        if (requestCode == ACCESS_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PERMISSION_GRANTED) {
                Log.d(TAG, "Camera Permission Granted");
                startCameraPreview();
            } else {
                Log.d(TAG, "Camera Permission Denied");
            }
        }
    }

    private void startCameraPreview() {
        Log.d(TAG, "startCameraPreview");
        mCameraProviderFuture = ProcessCameraProvider.getInstance(this);
        mCameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = mCameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {

            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9).setTargetRotation(Surface.ROTATION_0).build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

        // Preview use-case
        preview.setSurfaceProvider(mCameraPreview.createSurfaceProvider());

        // Image Analysis use-case
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setTargetRotation(Surface.ROTATION_0)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)   // non blocking
                .build();

        imageAnalysis.setAnalyzer(Executors.newFixedThreadPool(3), image -> {


            byte[] img64 = imageToBase64(image);
//            byte[] byteArray = image.getPlanes()[0].getBuffer().array();
            //String encodedImage = Base64.encodeToString(, Base64.DEFAULT);
            //Log.d(TAG, encodedImage);
            //Log.d(TAG, "Analysize: w=" + width + ", h=" + height);
            // TODO send to server at a given frequency
            mSocket.emit("newImage", img64);
            image.close();
        });

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
        Log.d(TAG, "Camera rotation: " + camera.getCameraInfo().getSensorRotationDegrees());
    }

    private byte[] imageToBase64(ImageProxy image) {
        // Conversion based on
        // https://stackoverflow.com/questions/56772967/converting-imageproxy-to-bitmap
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        // U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        byte[] imageBytes = yuvImage.getYuvData();

        // Base64.DEFAULT adheres to RFC 2045
        return imageBytes;//Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private boolean cameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Log.d(TAG, "requestCameraPermission: Permission needed. Explain the user why you need to use the camera");
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.request_camera_title))
                    .setMessage(getString(R.string.request_camera_message))
                    .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, ACCESS_CAMERA_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        } else {
            Log.d(TAG, "requestCameraPermission: Permission needed but no explanation needed. Show the permission dialog prompt.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, ACCESS_CAMERA_REQUEST_CODE);
        }
    }

    private void setResolutionSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.cameraResolutionSpinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.resolution_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
    }
}