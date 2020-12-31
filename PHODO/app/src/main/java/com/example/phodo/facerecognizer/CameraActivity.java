/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.phodo.facerecognizer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.Size;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.phodo.AlbumPreviewActivity;
import com.example.phodo.R;
import com.example.phodo.facerecognizer.env.ImageUtils;
import com.example.phodo.facerecognizer.env.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

//TODO: Camera 화면이 front일 때, tracking box가 옆으로 밀려서 출력.
public abstract class CameraActivity extends AppCompatActivity
        implements OnImageAvailableListener, View.OnClickListener{
    private static final Logger LOGGER = new Logger();

    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private boolean debug = false;

    private Integer useFacing = null;
    BluetoothSPP bt;

    private Handler handler;
    private HandlerThread handlerThread;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private static final String KEY_USE_FACING = "use_facing";

    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private Runnable postInferenceCallback;
    private Runnable imageConverter;

    public FrameLayout mainTopLayout;
    public FrameLayout flashLayout;
    public FrameLayout saveLayout;
    public FrameLayout timerLayout;

    public ImageButton timerBtn;
    public ImageButton timer2StateBtn;
    public ImageButton timer5StateBtn;
    public ImageButton timer10StateBtn;
    public ImageButton captureBtn;

    public int setTimerMode = 0;

    private String currentPhotoPath;

    // Camera Timer
    public CountDownTimer countDownTimer;
    public TextView timerInfoText;
    public boolean isTimerOn = false;

    // Essential - We have to use some variables and methods from this class
    protected CameraConnectionFragment camera2Fragment;
    // 줌을 조절할 수 있는 상단 바
    protected SeekBar seekBar;
    /* Variables for Auto Zoom Feature */
    protected boolean autoZoomFlag = false;
    float Zoom;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        super.onCreate(null);
        // 상태바 제거
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 타이틀바 제거.
        // https://omty.tistory.com/12
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        Intent intent = getIntent();
        useFacing = intent.getIntExtra(KEY_USE_FACING, CameraCharacteristics.LENS_FACING_FRONT);
        setContentView(R.layout.activity_camera_tf);

        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }

        mainTopLayout = findViewById(R.id.mainTopLayout);
        saveLayout = findViewById(R.id.saveLayout);
        timerLayout = findViewById(R.id.timerLayout);
        flashLayout = findViewById(R.id.flashLayout);

        ImageButton switchFace = findViewById(R.id.switchface);

        switchFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        //TODO: Take Photo Btn
//        ImageButton captrueBtn = findViewById(R.id.capturePicture);
//        captrueBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Bitmap bitmap = getPhotoImage();
//
//                String pathFileName = currentDateFormat();
//                storePhotoToStorage(bitmap, pathFileName);
////                bitmap.recycle();
//            }
//        });

        /** ==============================================================================
         * Bluetooth Code
         * =============================================================================*/
        bt = new BluetoothSPP(this);

        if (!bt.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
            finish();
        }

        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                /** 추후 없어질 codes */
                Toast.makeText(CameraActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(getApplicationContext()
                        , "Connected to " + name + "\n" + address
                        , Toast.LENGTH_SHORT).show();
            }

            public void onDeviceDisconnected() {
                Toast.makeText(getApplicationContext()
                        , "Connection lost", Toast.LENGTH_SHORT).show();
            }

            public void onDeviceConnectionFailed() {
                Toast.makeText(getApplicationContext()
                        , "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        });

        captureBtn = findViewById(R.id.capturePicture);
        captureBtn.setOnClickListener(this);
        //        findViewById(R.id.capturePicture).setOnClickListener(this);
//        findViewById(R.id.capturePictureSnapshot).setOnClickListener(this);
//        findViewById(R.id.captureVideo).setOnClickListener(this);
//        findViewById(R.id.captureVideoSnapshot).setOnClickListener(this);
//        findViewById(R.id.toggleCameraButton).setOnClickListener(this);
        findViewById(R.id.blue_btn).setOnClickListener(this);
        findViewById(R.id.saveBtn).setOnClickListener(this);

        timerBtn = findViewById(R.id.timerBtn);
        timerBtn.setOnClickListener(this);

        timer2StateBtn = findViewById(R.id.timer2StateBtn);
        timer2StateBtn.setOnClickListener(this);

        timer5StateBtn = findViewById(R.id.timer5StateBtn);
        timer5StateBtn.setOnClickListener(this);

        timer10StateBtn = findViewById(R.id.timer10StateBtn);
        timer10StateBtn.setOnClickListener(this);

        timerInfoText = findViewById(R.id.timerInfoText);

        findViewById(R.id.timer5Btn).setOnClickListener(this);
        findViewById(R.id.timer10Btn).setOnClickListener(this);
        findViewById(R.id.timer2Btn).setOnClickListener(this);
        findViewById(R.id.timerDisableBtn).setOnClickListener(this);

        findViewById(R.id.album).setOnClickListener(this);

        /* 줌을 조절할 수 있는 상단바 */
        seekBar = findViewById(R.id.zoom);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!autoZoomFlag) {
                    Zoom = (float) ((float) 1 + progress * 0.15);
                    camera2Fragment.zoom.setZoom(camera2Fragment.previewRequestBuilder, (float) ((float) 1 + progress * 0.15));
                    if (camera2Fragment.zoom.maxZoom < (float) ((float) 1 + progress * 0.15)) {
                        camera2Fragment.zoom.setZoom(camera2Fragment.previewRequestBuilder, camera2Fragment.zoom.maxZoom);
                    }
                    try {
                        camera2Fragment.captureSession.setRepeatingRequest(
                                camera2Fragment.previewRequestBuilder.build(), camera2Fragment.captureCallback, camera2Fragment.backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @SuppressLint("ResourceType")
    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.capturePicture:
                if(setTimerMode > 0 ) {
                    if (setTimerMode == 1) {
                        isTimerOn = true;
                        countDownTimer = new CountDownTimer(2000, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                // Show Text 2sec Timer info!
                                timerInfoText.setText(String.format(Locale.getDefault(), "%d", millisUntilFinished / 1000L));
                            }

                            @Override
                            public void onFinish() {
                                timerInfoText.setText("");
                                takePhoto();
                            }
                        };
                        countDownTimer.start();
                    } else if (setTimerMode == 2) {
                        isTimerOn = true;
                        countDownTimer = new CountDownTimer(5000, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                // Show Text 5sec Timer info!
                                timerInfoText.setText(String.format(Locale.getDefault(), "%d", millisUntilFinished / 1000L));
                            }

                            @Override
                            public void onFinish() {
                                timerInfoText.setText("");
                                takePhoto();
                            }
                        };
                        countDownTimer.start();
                    } else if (setTimerMode == 3) {
                        isTimerOn = true;
                        countDownTimer = new CountDownTimer(10000, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                // Show Text 10sec Timer info!
                                timerInfoText.setText(String.format(Locale.getDefault(), "%d", millisUntilFinished / 1000L));
                            }

                            @Override
                            public void onFinish() {
                                timerInfoText.setText("");
                                takePhoto();
                            }
                        };
                        countDownTimer.start();
                    }
                } else {
                    isTimerOn = false;
                    takePhoto();
                }
                break;
            case R.id.blue_btn:
                saveLayout.setVisibility(View.INVISIBLE);
                mainTopLayout.setVisibility(View.VISIBLE);
                bluetoothStart();
                break;
            case R.id.saveBtn:
                showSaveBtn();
                break;
            case R.id.timerBtn:
            case R.id.timer2StateBtn:
            case R.id.timer5StateBtn:
            case R.id.timer10StateBtn:
                showTimerLayout();
                break;
            case R.id.timer10Btn:
                if(timerLayout.getVisibility() == View.VISIBLE){
                    // set timer 10 sec and then take photo!
                    setTimerMode = 3;

                    timer10StateBtn.setVisibility(View.VISIBLE);
                    timerBtn.setVisibility(View.INVISIBLE);
                    timer5StateBtn.setVisibility(View.INVISIBLE);
                    timer2StateBtn.setVisibility(View.INVISIBLE);

                    timerLayout.setVisibility(View.INVISIBLE);
                    mainTopLayout.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.timer5Btn:
                if(timerLayout.getVisibility() == View.VISIBLE){
                    // set timer 5 sec and then take photo!
                    setTimerMode = 2;

                    timer5StateBtn.setVisibility(View.VISIBLE);
                    timer2StateBtn.setVisibility(View.INVISIBLE);
                    timer10StateBtn.setVisibility(View.INVISIBLE);
                    timerBtn.setVisibility(View.INVISIBLE);

                    timerLayout.setVisibility(View.INVISIBLE);
                    mainTopLayout.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.timer2Btn:
                if(timerLayout.getVisibility() == View.VISIBLE){
                    // set timer 2 sec and then take photo!
                    setTimerMode = 1;

                    timer2StateBtn.setVisibility(View.VISIBLE);
                    timer5StateBtn.setVisibility(View.INVISIBLE);
                    timer10StateBtn.setVisibility(View.INVISIBLE);
                    timerBtn.setVisibility(View.INVISIBLE);

                    timerLayout.setVisibility(View.INVISIBLE);
                    mainTopLayout.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.timerDisableBtn:
                if(timerLayout.getVisibility() == View.VISIBLE){
                    // set timer disabled!
                    setTimerMode = 0;

                    timerBtn.setVisibility(View.VISIBLE);
                    timer2StateBtn.setVisibility(View.INVISIBLE);
                    timer5StateBtn.setVisibility(View.INVISIBLE);
                    timer10StateBtn.setVisibility(View.INVISIBLE);

                    timerLayout.setVisibility(View.INVISIBLE);
                    mainTopLayout.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.album:
                getAlbum();
                break;
        }
    }

    public void takePhoto() {
        Toast.makeText(CameraActivity.this, "Take Photo", Toast.LENGTH_SHORT).show();
        Bitmap bitmap = getPhotoImage();

        Matrix rotateMatrix = new Matrix();
        rotateMatrix.postRotate(-90); //-360~360

        Bitmap rotateImg = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), rotateMatrix, false);
        String pathFileName = currentDateFormat();
        storePhotoToStorage(rotateImg, pathFileName);
    }

    private void bluetoothStart() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(CameraActivity.this);
        // dialog 속성 설정
        mBuilder.setTitle("Bluetooth 기능 활성화 대화상자")
                .setMessage("Bluetooth를 활성화 하시겠습니까?")
                .setCancelable(false)
                .setPositiveButton("On", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
                            bt.disconnect();
                        } else {
                            Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                        }
                    }
                })
                .setNegativeButton("Off", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        AlertDialog dialog = mBuilder.create();
        dialog.show();
    }

    private void showSaveBtn() {
        mainTopLayout.setVisibility(View.INVISIBLE);
        saveLayout.setVisibility(View.VISIBLE);
    }

    private void showTimerLayout() {
        mainTopLayout.setVisibility(View.INVISIBLE);
        timerLayout.setVisibility(View.VISIBLE);
    }

    @SuppressLint("WrongThread")
    private void storePhotoToStorage(Bitmap cbmp, String pathFileName) {
        File outputFile = new File(Environment.getExternalStorageDirectory(), "/DCIM/"+"photo"+pathFileName+".jpg");
        currentPhotoPath = "/DCIM/"+"photo"+ pathFileName + ".jpg";
        try {
            FileOutputStream fileOutputStream  = new FileOutputStream(outputFile);
            cbmp.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private String currentDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HH_mm_ss");
        String currentTime = dateFormat.format(new Date());
        return currentTime;
    }

    private void getAlbum() {
        Intent intent = new Intent(CameraActivity.this, AlbumPreviewActivity.class);
        startActivity(intent);
    }

    public void switchCamera() {
        Intent intent = getIntent();

        /** camera 2 */
        if (useFacing == CameraCharacteristics.LENS_FACING_FRONT) {
            useFacing = CameraCharacteristics.LENS_FACING_BACK;
        } else {
            useFacing = CameraCharacteristics.LENS_FACING_FRONT;
        }

        /** camera 1 */
//    if (useFacing == CameraInfo.CAMERA_FACING_FRONT) {
//      useFacing = CameraInfo.CAMERA_FACING_BACK;
//    } else {
//      useFacing = CameraInfo.CAMERA_FACING_FRONT;
//    }

        intent.putExtra(KEY_USE_FACING, useFacing);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        restartWith(intent);
    }

    private void restartWith(Intent intent) {
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    protected Integer getCameraFacing() {
        return useFacing;
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    protected int getLuminanceStride() {
        return yRowStride;
    }

    protected byte[] getLuminance() {
        return yuvBytes[0];
    }

    /**
    * Callback for Camera2 API
    */
    @Override
    public void onImageAvailable(final ImageReader reader) {
        //We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    () -> ImageUtils.convertYUV420ToARGB8888(
                            yuvBytes[0],
                            yuvBytes[1],
                            yuvBytes[2],
                            previewWidth,
                            previewHeight,
                            yRowStride,
                            uvRowStride,
                            uvPixelStride,
                            rgbBytes);

            postInferenceCallback =
                    () -> {
                        image.close();
                        isProcessingFrame = false;
                    };
            processImage();
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();
        if(!bt.isBluetoothEnabled()) {
            bt.enable();
        } else {
            if(!bt.isServiceAvailable()) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
            }
        }
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }
        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
        bt.stopService();
        camera2Fragment.closeCamera();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) ||
                    shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
                Toast.makeText(CameraActivity.this,
                        "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
        }
    }

    private String chooseCamera() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
//                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
//                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
//                    continue;
//                }
                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (useFacing != null &&
                        facing != null &&
                        !facing.equals(useFacing)
                ) {
                    continue;
                }

                return cameraId;
            }
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Not allowed to access camera");
        }

        return null;
    }

    protected void setFragment() {
        String cameraId = chooseCamera();

        camera2Fragment =
                CameraConnectionFragment.newInstance(
                        (size, rotation) -> {
                            previewHeight = size.getHeight();
                            previewWidth = size.getWidth();
                            CameraActivity.this.onPreviewSizeChosen(size, rotation);
                        },
                        this,
                        getLayoutId(),
                        getDesiredPreviewFrameSize());

        camera2Fragment.setCamera(cameraId);

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, camera2Fragment)
                .commit();
    }

    protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public boolean isDebug() {
        return debug;
    }


    public void requestRender() {
        final OverlayView overlay = findViewById(R.id.debug_overlay);
        if (overlay != null) {
            overlay.postInvalidate();
        }
    }

    public void addCallback(final OverlayView.DrawCallback callback) {
        final OverlayView overlay = findViewById(R.id.debug_overlay);
        if (overlay != null) {
            overlay.addCallback(callback);
        }
    }

    public void onSetDebug(final boolean debug) {}

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            debug = !debug;
            requestRender();
            onSetDebug(debug);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }


    protected abstract void processImage();

    protected abstract Bitmap getPhotoImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);
    protected abstract int getLayoutId();
    protected abstract Size getDesiredPreviewFrameSize();
}
