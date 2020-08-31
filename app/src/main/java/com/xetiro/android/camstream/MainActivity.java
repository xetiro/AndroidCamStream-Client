package com.xetiro.android.camstream;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.xetiro.android.camstream.network.ServerClient;
import com.xetiro.android.camstream.utils.ImageConverter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * The main activity is the one that connects the camera, grab the frames and streams the frames
 * to the server.
 * <p>
 * Created by xetiro (aka Ruben Geraldes) on 27/09/2020.
 */
public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivityDebug";
    private static int ACCESS_CAMERA_REQUEST_CODE = 1;
    private static int FRAME_RATE = 5;
    private static long DELAY_MS = 1000 / FRAME_RATE;

    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    private PreviewView mCameraPreview;
    private ServerClient mServer;

    // The Bitmap image from camera preview is converted to JPEG without artifacts
    // The YUV image from the image Analysis when converted to JPEG sometimes can create artifacts
    // because the conversion might fail for some cameras
    private boolean useImageFromCameraPreview = true;

    private int mTargetWidth = 320;
    private int mTargetHeight = 240;

    private long mLastTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setResolutionSpinner();

        mServer = ServerClient.getInstance();
        mServer.init("192.168.3.6", 9000);

        mCameraPreview = findViewById(R.id.cameraView);

        if (cameraPermissionGranted()) {
            startCameraPreview();
        } else {
            requestCameraPermission();
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        mServer.connect();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        mServer.disconnect();
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
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

        // Preview use-case will render a preview image on the screen as defined by the PreviewView
        // element on the main's layout activity. The resolution of the layout is relative to the
        // screen size and defined in dp, which means the final resolution in pixels will be decided
        // at run-time when the layout is inflated to the device screen. But will always be proportional
        // to the resolution defined on the layout.
        preview.setSurfaceProvider(mCameraPreview.createSurfaceProvider());

        // Image Analysis use-case will not render the image on the screen, but will
        // deliver a frame by frame image (stream) directly from the camera buffer to the analyser.
        // On this case we can set an actual target resolution as defined by the user. CameraX will
        // try to match the captured resolution to the target resolution. If it cannot match, will
        // capture the frame with the resolution immediately above.
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(mTargetWidth, mTargetHeight))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)   // non blocking
                .build();

        imageAnalysis.setAnalyzer(Executors.newFixedThreadPool(1), image -> {
            long elapsedTime = System.currentTimeMillis() - mLastTime;
            if (elapsedTime > DELAY_MS) {   // Bound the image processing by the user defined frame-rate
                byte[] byteArray;
                if (useImageFromCameraPreview) {
                    byteArray = ImageConverter.BitmaptoJPEG(mCameraPreview.getBitmap());
                } else {
                    byteArray = ImageConverter.YUV_420_800toJPEG(image);
                }
                mServer.sendImage(byteArray);
                mLastTime = System.currentTimeMillis();
            }
            image.close();
        });

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
        Log.d(TAG, "Camera rotation: " + camera.getCameraInfo().getSensorRotationDegrees());
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