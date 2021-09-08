package com.example.cameraxwithguides;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2CameraFilter;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.Surface;

import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CameraActivity extends AppCompatActivity {
    private PreviewView mPreviewView;
    private String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int REQUEST_PERMISSIONS = 1001;
    private ProcessCameraProvider mProcessCameraProvider;
    private Preview mPreview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mPreviewView = findViewById(R.id.preview_activity_video_recode);

        startPreview();
        initCamera();
    }

    private void initCamera() {
        CameraManager cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameras = cameraManager.getCameraIdList();
            CameraCharacteristics c = cameraManager.getCameraCharacteristics(cameras[1]);
            Integer facing = c.get(CameraCharacteristics.LENS_FACING);



//
//            LinkedHashSet<CameraFilter> cs = new CameraSelector.Builder().build().getCameraFilterSet();

            Log.d("initCamera", "number:"+Camera.getNumberOfCameras());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void startPreview() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, REQUEST_PERMISSIONS);
            } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, REQUEST_PERMISSIONS);
            } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, REQUEST_PERMISSIONS);
            } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, REQUEST_PERMISSIONS);
            } else {
                preview();
            }
        } else {
            preview();
        }
    }

    private void preview() {
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);
        try {
            mProcessCameraProvider = listenableFuture.get();
            boolean b1 = mProcessCameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
            boolean b2 = mProcessCameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
            @SuppressLint("WrongConstant") CameraSelector cs2 = new CameraSelector.Builder().requireLensFacing(1).build();


            mPreview = new Preview.Builder()
                    .setTargetResolution(new Size(1280, 720))
                    .build();
            mPreview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .build();
            imageAnalysis
                    .setAnalyzer(ContextCompat.getMainExecutor(this),
                            new ImageAnalysis.Analyzer() {
                                @Override
                                public void analyze(@NonNull ImageProxy image) {
//                                    Log.e(TAG, "analyze: " + image);
                                    image.close();
                                }
                            }
                    );
            mProcessCameraProvider.bindToLifecycle((LifecycleOwner) this, cs2,
                    mPreview,
                    imageAnalysis
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        Log.d("onKeyDown", "keycode:"+keyCode);
        return super.onKeyDown(keyCode, event);

    }





}