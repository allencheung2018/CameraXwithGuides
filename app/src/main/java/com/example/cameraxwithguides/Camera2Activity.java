package com.example.cameraxwithguides;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.Image;
import android.media.ImageReader;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Camera2Activity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener, Camera2Listener{
    private TextureView textureView;
    private ImageViewDrawable imageView;
    private Button btnGrid;
    private Button btnFront;
    private Button btnBack;
    private Button btnLeft;
    private Button btnRight;
    private Button btnCancel;
    private Button btnSave;
    private Camera2Helper camera2Helper;
    private Image image;
    private boolean picture = false;

    private static final String TAG = "MainActivity";
    // 需要的权限
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };
    private static final int ACTION_REQUEST_PERMISSIONS = 1;
    // 默认打开的CAMERA
    private static String CAMERA_ID = Camera2Helper.CAMERA_ID_BACK;
    // 显示的旋转角度
    private int displayOrientation;
    // 是否手动镜像预览
    private boolean isMirrorPreview;
    // 实际打开的cameraId
    private String openedCameraId;
    // 线程池
    private ExecutorService imageProcessExecutor;

    private int cameraName = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        imageProcessExecutor = Executors.newSingleThreadExecutor();
        textureView = findViewById(R.id.textureView);
        imageView = findViewById(R.id.imageView);
        btnGrid = findViewById(R.id.button3);
        btnFront = findViewById(R.id.button4);
        btnBack = findViewById(R.id.button5);
        btnLeft = findViewById(R.id.button6);
        btnRight = findViewById(R.id.button7);
        btnCancel = findViewById(R.id.button8);
        btnSave = findViewById(R.id.button9);
        cameraName = getIntent().getIntExtra("camera", 0);

        init();
    }

    private void init() {
        textureView.getViewTreeObserver().addOnGlobalLayoutListener((ViewTreeObserver.OnGlobalLayoutListener) this);
        UsbManager usbManager = (UsbManager) getSystemService(getApplicationContext().USB_SERVICE);
        HashMap<String, UsbDevice> hashMap = usbManager.getDeviceList();
        Log.d(TAG, "UsbDevice:"+hashMap.size());
        CameraManager cameraManager = (CameraManager) getSystemService(getApplicationContext().CAMERA_SERVICE);
        try {
            String[] cameras = cameraManager.getCameraIdList();
            Log.d(TAG, "cameras:"+cameras.length);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (cameraName == 1){
            btnGrid.setVisibility(View.GONE);
            btnFront.setVisibility(View.GONE);
            btnBack.setVisibility(View.GONE);
            btnLeft.setVisibility(View.GONE);
            btnRight.setVisibility(View.GONE);
        }

        btnGrid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setShow(0);
                imageView.invalidate();
            }
        });
        btnFront.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setShow(2);
                imageView.invalidate();
            }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setShow(3);
                imageView.invalidate();
            }
        });
        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setShow(4);
                imageView.invalidate();
            }
        });
        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setShow(5);
                imageView.invalidate();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setImageDrawable(null);
                imageView.invalidate();
                btnCancel.setVisibility(View.INVISIBLE);
                btnSave.setVisibility(View.INVISIBLE);
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePhoto();
                imageView.setImageDrawable(null);
                imageView.invalidate();
                btnCancel.setVisibility(View.INVISIBLE);
                btnSave.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void savePhoto() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",
                Locale.US);

        String path = Environment.getExternalStorageDirectory().getPath() + "/image";
        File outfile = new File(path);
        // 如果文件不存在，则创建一个新文件
        if (!outfile.isDirectory()) {
            try {
                outfile.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String fname = outfile + "/" + sdf.format(new Date()) + ".jpg";
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fname);
            if (null != fos) {
                Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.flush();
                fos.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "savePhoto name:"+fname);
    }

    private boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    void selectCamera(int id){
        camera2Helper  = new Camera2Helper(getApplicationContext());
        if(camera2Helper.findCamera(id)){
            CAMERA_ID = String.valueOf(id);
            if (CAMERA_ID.equals("1")){
                initCamera(1280, 720);
            }else {
                initCamera(1280, 720);
            }

        }else {
            Toast.makeText(getApplicationContext(), "Please Connect Camera!", Toast.LENGTH_SHORT).show();
        }
    }

    void initCamera(int x, int y) {
        camera2Helper = new Camera2Helper.Builder()
                .cameraListener(this)
                .maxPreviewSize(new Point(1920, 1080))
                .minPreviewSize(new Point(1280, 720))
                .specificCameraId(CAMERA_ID)
                .context(getApplicationContext())
                .previewOn(textureView)
                .previewViewSize(new Point(textureView.getWidth(), textureView.getHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .previewSize(new Point(x, y))
                .isMirror(true)
                .rotation(180)
                .build();
        camera2Helper.start();
    }

    public void switchCamera(View view) {
        if (camera2Helper != null) {
            camera2Helper.switchCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            boolean isAllGranted = true;
            for (int grantResult : grantResults) {
                isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
            }
            if (isAllGranted) {
                initCamera(1280, 720);
            } else {
                Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onGlobalLayout() {
        textureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        } else {
//            initCamera();
            selectCamera(cameraName);
        }
    }

    @Override
    public void onCameraOpened(CameraDevice cameraDevice, String cameraId, Size previewSize, int displayOrientation, boolean isMirror) {
        Log.i(TAG, "onCameraOpened:  previewSize = " + previewSize.getWidth() + "x" + previewSize.getHeight()
            + " orientation="+displayOrientation + " mirror:"+isMirror);
        this.displayOrientation = displayOrientation;
        this.isMirrorPreview = isMirror;
        this.openedCameraId = cameraId;
    }

    @Override
    public void onPreview(byte[] y, byte[] u, byte[] v, Size previewSize, int stride) {
//            Log.d(TAG, "onPreview");
    //        imageProcessExecutor.execute(new Runnable() {
    //            @Override
    //            public void run() {
    //                Matrix matrix = new Matrix();
    //                matrix.postScale(-1, 1);
    //            }
    //        });
    }

    @Override
    public void onPreview(Image image) {
        if (picture){
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();

            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Bitmap bitmap1 = convert(bitmap);
            if (bitmap1 != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap1);
                        btnCancel.setVisibility(View.VISIBLE);
                        btnSave.setVisibility(View.VISIBLE);
                    }
                });
            }
            picture = false;
        }
    }

    public static Bitmap convert(Bitmap a) {
        int w = a.getWidth();
        int h = a.getHeight();
//    Bitmap newb = Bitmap.createBitmap(w, h, Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图
//    Canvas cv = new Canvas(newb);
        Matrix m = new Matrix();
        // m.postScale(1, -1); //镜像垂直翻转
        m.postScale(-1, 1); // 镜像水平翻转

        Bitmap new2 = Bitmap.createBitmap(a, 0, 0, w, h, m, true);
//    cv.drawBitmap(new2, new Rect(0, 0, new2.getWidth(), new2.getHeight()),
//          new Rect(0, 0, w, h), null);
        return new2;
    }

    Bitmap adjustPhotoRotation(Bitmap bm, final int orientationDegree) {

        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);

        try {
            Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);

            return bm1;

        } catch (OutOfMemoryError ex) {
        }
        return null;

    }

    @Override
    public void onCameraClosed() {

    }

    @Override
    public void onCameraError(Exception e) {

    }

    @Override
    protected void onPause() {
        if (camera2Helper != null) {
            camera2Helper.stop();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera2Helper != null) {
            camera2Helper.start();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestory");
        if (imageProcessExecutor != null) {
            imageProcessExecutor.shutdown();
            imageProcessExecutor = null;
        }
        if (camera2Helper != null) {
            camera2Helper.release();
            camera2Helper = null;
        }
        finish();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "keyCode:"+keyCode);

        picture = true;



        return super.onKeyDown(keyCode, event);
    }
}