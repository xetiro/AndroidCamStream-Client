package com.example.mytvapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivityDebug";
    private static int ACCESS_CAMERA_REQUEST_CODE = 1;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView cameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setResolutionSpinner();

        cameraPreview = findViewById(R.id.cameraView);

        if (cameraPermissionGranted()) {
            startCameraPreview();
        } else {
            requestCameraPermission();
        }
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
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {

            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
        preview.setSurfaceProvider(cameraPreview.createSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview);

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