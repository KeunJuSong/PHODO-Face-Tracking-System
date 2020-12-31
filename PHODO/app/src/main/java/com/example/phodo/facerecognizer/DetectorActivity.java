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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.camera2.CameraAccessException;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.phodo.MainActivity;
import com.example.phodo.R;
import com.example.phodo.facerecognizer.env.BorderedText;
import com.example.phodo.facerecognizer.env.FileUtils;
import com.example.phodo.facerecognizer.env.ImageUtils;
import com.example.phodo.facerecognizer.env.Logger;
import com.example.phodo.facerecognizer.ml.BlazeFace;
import com.example.phodo.facerecognizer.tracking.MultiBoxTracker;
import com.google.android.material.snackbar.Snackbar;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.speech_to_text.v1.websocket.RecognizeCallback;
import com.kyleduo.switchbutton.SwitchButton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import app.akexorcist.bluetotohspp.library.BluetoothState;

/**
* An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
* objects.
*/
//TODO: 1.카메라 전면일 때 tracking box 오른쪽으로 치우쳐짐.(현재 코드적으로 fit하게 돌아가지 않음) => 현재 갤럭시s8에서만 올바르게 tracking box가 그려질 것으로 예상.
//      3. 전반적으로 직접 모델링한 것과 돌려보며 좌표값 연산 수정 작업.(Face Detect part와 Face Customizing part 좌표값 거의 동일.)
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener, SingleChoiceDialogFragment.SingleChoiceListener {
    private static final Logger LOGGER = new Logger();

    private static final int CROP_HEIGHT = BlazeFace.INPUT_SIZE_HEIGHT;
    private static final int CROP_WIDTH = BlazeFace.INPUT_SIZE_WIDTH;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;

    private Integer sensorOrientation;

    String face_info = "";
    String distanceMode = "";

    private Recognizer recognizer;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    // here the preview image is drawn in portrait way
    private Bitmap portraitBmp = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private byte[] luminanceCopy;

    private BorderedText borderedText;

    private Snackbar initSnackbar;
    private Snackbar trainSnackbar;

    private ImageButton button;
    private ImageButton listButton;
//    private FloatingActionButton button;

    public ImageButton compositionModeSelect;
    public ImageButton center_compositionModeSelect;
    public ImageButton left_compositionModeSelect;
    public ImageButton right_compositionModeSelect;

    public ToggleButton handsfree;
    private int handsFreeCaptureCnt = 1;

    private View left_optimal;
    private View right_optimal;
    private View center_optimal;

    /********** Speech To Text Variables Define **********/
    private SpeechToText speechService;
    private TextView returnedText;
    private MicrophoneInputStream capture;
    private MicrophoneHelper microphoneHelper;
    private String LOG_TAG = "VoiceRecognitionActivity";
    private static final String[] cheese = {"치즈 ",      //해당 단어를 인식하여 사진 캡쳐를 수행
            "사진 ",
            "스마일 ",
            "김치 ",
            "캡처 ",
            "캡쳐 "};
    private static final String[] keywords = {
            "치즈 ",
            "사진 ",
            "스마일 ",
            "김치 ",
            "캡처 ",
            "캡쳐 ",
            "중앙 ",
            "왼쪽 ",
            "오른쪽 ",
            "일반 "
    };

    private boolean initialized = false;
    private boolean training = false;

    // Device Screen Size info
    private int Device_height;
    private int Device_width;

    private int fin_X;
    private int fin_Y;
    private int faces_numb;
    private float faceProb;
    int min_idx_fh = 0;
    int max_idx_fh = 0;
    int[] fh = new int[5];
    int autoZoomcnt = 0;
    private float autoZoomVal = 0;

    private int compositionMode = 0;
    private boolean safeToTakePicture = true;
    private boolean listening = false;

    private boolean isRailTrackingMode = false;

    String mode = ""; // 전역변수로 선언

    private List<Pair<Float, String>> currFaceInfo = new LinkedList<Pair<Float, String>>();
    private List<Recognizer.Recognition> currFaceList = new LinkedList<Recognizer.Recognition>();

    private String choosedFace = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 상태바 제거
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 타이틀바 제거.
        // https://omty.tistory.com/12
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        Device_width = dm.widthPixels;
        Device_height = dm.heightPixels;

        compositionModeSelect = findViewById(R.id.modeSelect_tf);
        center_compositionModeSelect = findViewById(R.id.center_modeSelect_tf);
        left_compositionModeSelect = findViewById(R.id.left_modeSelect_tf);
        right_compositionModeSelect = findViewById(R.id.right_modeSelect_tf);

        center_optimal = findViewById(R.id.center_optimal);
        left_optimal = findViewById(R.id.left_optimal);
        right_optimal = findViewById(R.id.right_optimal);

        FrameLayout container = findViewById(R.id.container);
        initSnackbar = Snackbar.make(
                container, getString(R.string.initializing), Snackbar.LENGTH_INDEFINITE);
        trainSnackbar = Snackbar.make(
                container, getString(R.string.training), Snackbar.LENGTH_INDEFINITE);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edittext, null);
        EditText editText = dialogView.findViewById(R.id.edit_text);
        AlertDialog editDialog = new AlertDialog.Builder(DetectorActivity.this)
                .setTitle(R.string.enter_name)
                .setView(dialogView)
                .setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> {
                    int idx = recognizer.addPerson(editText.getText().toString());
                    performFileSearch(idx - 1);
                })
                .create();

        button = findViewById(R.id.add_button);
        button.setOnClickListener(view ->
                new AlertDialog.Builder(DetectorActivity.this)
                        .setTitle(getString(R.string.select_name))
                        .setItems(recognizer.getClassNames(), (dialogInterface, i) -> {
                            if (i == 0) {
                                editDialog.show();
                            } else {
                                performFileSearch(i - 1);
                            }
                        })
                        .show());

        //TODO: 현재 얼굴 정보 가져오는 기능
        listButton = findViewById(R.id.list_currface);
        listButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!recognizer.getResults().isEmpty()){
                    AlertDialog.Builder builder = new AlertDialog.Builder(DetectorActivity.this);
                    builder.setTitle("Customized Faces");
                    List<String> listFaces = new ArrayList<String>();
                    for(int i = 0; i < recognizer.getResults().size(); i++) {
                        Recognizer.Recognition severalFace = recognizer.getResults().get(i);
                        listFaces.add(severalFace.getTitle());
                    }
                    // 중복 제거.
                    HashSet<String> filter = new HashSet<String>(listFaces);
                    List<String> afListFaces = new ArrayList<String>(filter);
                    if(afListFaces.size() != 0){
                        // 취소 항목 추가
                        afListFaces.add("Cancel");
                    }
                    final CharSequence[] items = afListFaces.toArray(new CharSequence[afListFaces.size()]);
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Dialog dialog_ = (Dialog) dialog;
                            // 선택하지 않고 '취소'를 누른 경우
                            if (which == afListFaces.size()-1) {
                                Toast.makeText(dialog_.getContext(), "Choose cancel", Toast.LENGTH_SHORT).show();
//                                choosedFace = null;
                            } else {
                                //취소가 아닌 경우
                                Toast.makeText(dialog_.getContext(), "Choose " + afListFaces.get(which) + " Face!!", Toast.LENGTH_SHORT).show();
                                choosedFace = afListFaces.get(which);
                            }
                        }
                    });

                    builder.setCancelable(false);
                    AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    Toast.makeText(DetectorActivity.this ,"No Customized Face!!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Rail/Phodo Tracking Mode.
        // 참조 사이트: https://webnautes.tistory.com/1375
        SwitchButton switchButton = findViewById(R.id.phodo_mode_switch);
        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 스위치 버튼이 체크되었는지 검사
                if (isChecked){
                    Toast.makeText(DetectorActivity.this,"Rail_Tracking Mode!", Toast.LENGTH_SHORT).show();
                    isRailTrackingMode = true;
                    mode = "RT";
                    compositionModeSelect.setEnabled(false);
                }else{
                    Toast.makeText(DetectorActivity.this,"PHODO_Tracking Mode!", Toast.LENGTH_SHORT).show();
                    isRailTrackingMode = false;
                    compositionModeSelect.setEnabled(true);
                    if (compositionMode == 0) {
                        mode = "N";
                    } else if (compositionMode == 1) {
                        mode = "C";
                    } else if (compositionMode == 2) {
                        mode = "L";
                    } else if (compositionMode == 3) {
                        mode = "R";
                    }
                }
            }
        });

        /********** STT Service Defines **********/
        microphoneHelper = new MicrophoneHelper(this);
        safeToTakePicture = true;
        speechService = initSpeechToTextService();

        handsfree = findViewById(R.id.handsFree_tf);
        returnedText = findViewById(R.id.returnedtext_tf);
        ToggleButton autoZoomButton = findViewById(R.id.autozoom);

        autoZoomButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                autoZoomFlag = isChecked;
            }
        });

        handsfree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveLayout.setVisibility(View.INVISIBLE);
                mainTopLayout.setVisibility(View.VISIBLE);

                if (!listening) {
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void run() {
                            handsfree.setBackground(getDrawable(R.drawable.mic_on));     //토글버튼이 활성화됬을 때 버튼 모양 설정
                        }
                    });
                    capture = microphoneHelper.getInputStream(true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                speechService.recognizeUsingWebSocket(getRecognizeOptions(capture), new MicrophoneRecognizeDelegate());
                            } catch (Exception e) {
                                showError(e);
                            }
                        }
                    }).start();
                    listening = true;       //STT 기능을 활성화시키는 코드
                } else {
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void run() {
                            handsfree.setBackground(getDrawable(R.drawable.mic_off));             //토글버튼이 비활성화됬을 때 버튼 모양 설정
                            returnedText.setText("");
                        }
                    });
                    microphoneHelper.closeInputStream();
                    listening = false;      //STT 기능을 비활성화시키는 코드
                }
            }
        });

        /* 어플 모드 선택 (일반 모드, 중앙 구도 모드, 3분할 왼쪽 모드, 3분할 오른쪽 모드) */
        compositionModeSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment singleChoiceDialog = new SingleChoiceDialogFragment();
                singleChoiceDialog.setCancelable(false);
                singleChoiceDialog.show(getSupportFragmentManager(), "Single Choice Dialog");
            }
        });

        center_compositionModeSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment singleChoiceDialog = new SingleChoiceDialogFragment();
                singleChoiceDialog.setCancelable(false);
                singleChoiceDialog.show(getSupportFragmentManager(), "Single Choice Dialog");
            }
        });

        left_compositionModeSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment singleChoiceDialog = new SingleChoiceDialogFragment();
                singleChoiceDialog.setCancelable(false);
                singleChoiceDialog.show(getSupportFragmentManager(), "Single Choice Dialog");
            }
        });

        right_compositionModeSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment singleChoiceDialog = new SingleChoiceDialogFragment();
                singleChoiceDialog.setCancelable(false);
                singleChoiceDialog.show(getSupportFragmentManager(), "Single Choice Dialog");
            }
        });

//        Drawable on = ContextCompat.getDrawable(this, R.drawable.button);
//        Drawable off = ContextCompat.getDrawable(this, R.drawable.list_item_background);
//        Drawable activated = ContextCompat.getDrawable(this, R.drawable.list_item_background_square);
//        Drawable transParent = ContextCompat.getDrawable(this, R.color.transparent);

        final Runnable runCompositionMode = new Runnable() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void run() {
                final View handsFreeCapture = findViewById(R.id.capturePicture);
                if (faces_numb >= 1) {
                    if (compositionMode == 1) {
                        //CENTER
                        if (fin_X > 670 && fin_X < 770) {
                            if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
                                handsFreeCapture.performClick();
                                safeToTakePicture = false;
                            }
                        }
//                        if (fin_X > 450 && fin_X < 550) {
////                            compositionModeSelect.setBackground(on);
////                            container.setBackground(activated);
//                            if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
//                                handsFreeCapture.performClick();
//                                safeToTakePicture = false;
//                            }
//                        } else {
////                            compositionModeSelect.setBackground(off);
////                            container.setBackground(transParent);
//                        }
                    } else if (compositionMode == 2) {
                        //LEFT
                        if (fin_X > 900 && fin_X < 1000) {
                            if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
                                handsFreeCapture.performClick();
                                safeToTakePicture = false;
                            }
                        }
//                        if (fin_X > 283 && fin_X < 384) {
////                            compositionModeSelect.setBackground(on);
////                            container.setBackground(activated);
//                            if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
//                                handsFreeCapture.performClick();
//                                safeToTakePicture = false;
//                            }
//                        } else {
////                            compositionModeSelect.setBackground(off);
////                            container.setBackground(transParent);
//                        }
                    } else if (compositionMode == 3) {
                        //RIGHT
                        if (fin_X > 330 && fin_X < 430) {
                            if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
                                handsFreeCapture.performClick();
                                safeToTakePicture = false;
                            }
                        }
//                        if (fin_X > 616 && fin_X < 717) {
////                            compositionModeSelect.setBackground(on);
////                            container.setBackground(activated);
//                            if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
//                                handsFreeCapture.performClick();
//                                safeToTakePicture = false;
//                            }
//                        } else {
////                            compositionModeSelect.setBackground(off);
////                            container.setBackground(transParent);
//                        }
                    } else if (compositionMode == 0) {
//                        compositionModeSelect.setBackground(off);
//                        container.setBackground(transParent);
                    }
                }
            }
        };

        class NewRunnable implements Runnable {
            @Override
            public void run() {
                while (true) {
                    try {
                        safeToTakePicture = true;
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // 메인 스레드에 runnable 전달.
                    runOnUiThread(runCompositionMode);
                }
            }
        }

        NewRunnable nr = new NewRunnable();
        Thread t = new Thread(nr);
        t.start();

        final Runnable autoZoom = new Runnable() {
            @Override
            public void run() {
                float ZoomInVal;
                float ZoomOutVal;

                if (!face_info.equals("E")) {
                    if (autoZoomFlag) {
                        seekBar.setEnabled(false);

                        ZoomInVal = (float) (fh[max_idx_fh] - 100) / 100;
                        ZoomOutVal = (float) (fh[max_idx_fh] - 200) / 100;
                        if (fh[max_idx_fh] > 200) {
                            autoZoomVal -= ZoomOutVal;
                        } else if (fh[max_idx_fh] < 100) {
                            autoZoomVal -= ZoomInVal;
                        }
                        Log.d(this.getClass().getName(), "얼굴크기: " + fh[max_idx_fh]);

                        Log.d(this.getClass().getName(), "오토줌: " + autoZoomVal);

                        if (autoZoomVal > 3.0) autoZoomVal = (float) 3.0;
                        if (autoZoomVal < 0) autoZoomVal = 0;

                        camera2Fragment.zoom.setZoom(camera2Fragment.previewRequestBuilder, 1 + autoZoomVal);

                        try {
                            camera2Fragment.captureSession.setRepeatingRequest(
                                    camera2Fragment.previewRequestBuilder.build(),
                                    camera2Fragment.captureCallback,
                                    camera2Fragment.backgroundHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }

//          seekBar.setProgress((int) ((autoZoomVal) / 4 * 20));
                    } else {
                        autoZoomVal = Zoom;
                        seekBar.setEnabled(true);
                    }
                }
            }
        };

        class NewRunnable2 implements Runnable {
            int initCnt = 0;

            @Override
            public void run() {
                while (true) {

                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // 메인 스레드에 runnable 전달.
                    runOnUiThread(autoZoom);
                }
            }
        }

        NewRunnable2 nr2 = new NewRunnable2();
        Thread t2 = new Thread(nr2);
        t2.start();
    }

    // 좌표값 연산 작업 => Device의 display에 따른 matrix mapping.
    private void sendChoosedFaceCoord(String faceTitle){
        if(faceTitle != null){
            for(int i = 0; i < recognizer.getResults().size(); i++){
                Recognizer.Recognition result = recognizer.getResults().get(i);
                if(faceTitle == result.getTitle()){
                    final Matrix displayFrame = new Matrix(tracker.getFrameToCanvasMatrix());
                    final RectF detectionFrameRect = result.getLocation();
                    final RectF detectionScreenRect = new RectF();
                    displayFrame.mapRect(detectionScreenRect, detectionFrameRect);

                    fin_X = (int) ((detectionScreenRect.centerX() / Device_width)*1000);
                    fin_Y = (int) ((detectionScreenRect.centerY() / Device_height)*1000);

                    faceProb = tracker.faceConfidence;

                    String X_info = Integer.toString((int) ((detectionScreenRect.centerX() / Device_width)*1000));
                    String Y_info = Integer.toString((int) ((detectionScreenRect.centerY() / Device_height)*1000));
                    Log.d("Mapped Coord/Prob/Title: ", X_info + "/" + Y_info + " " + Float.toString(faceProb) + " " + result.getTitle());
                }
            }
        }else {
            // choose faceTitle is null!!
        }
    }

    //  STT로 모드 활성화/비활성화 하는데 필요한 메소드
    private void changeModeByVoice(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (text.contains("중앙 ")) {        //인식한 단어와 미리 설정한 캡쳐 수행 단어를 비교하여 같으면 캡쳐 기능 수행.
                    Toast.makeText(DetectorActivity.this, "최적 구도 - 중앙 모드",
                            Toast.LENGTH_SHORT).show();
                    if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
//                        compositionModeSelect.setText("C");
                        center_compositionModeSelect.setVisibility(View.VISIBLE);
                        compositionModeSelect.setVisibility(View.INVISIBLE);
                        left_compositionModeSelect.setVisibility(View.INVISIBLE);
                        right_compositionModeSelect.setVisibility(View.INVISIBLE);

                        center_optimal.setVisibility(View.VISIBLE);
                        left_optimal.setVisibility(View.INVISIBLE);
                        right_optimal.setVisibility(View.INVISIBLE);

                        compositionMode = 1;
                        safeToTakePicture = false;
                    }
                } else if (text.contains("왼쪽 ")) {        //인식한 단어와 미리 설정한 캡쳐 수행 단어를 비교하여 같으면 캡쳐 기능 수행.
                    Toast.makeText(DetectorActivity.this, "최적 구도 - 3분할 왼쪽 모드",
                            Toast.LENGTH_SHORT).show();
                    if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
//                        compositionModeSelect.setText("L");
                        left_compositionModeSelect.setVisibility(View.VISIBLE);
                        center_compositionModeSelect.setVisibility(View.INVISIBLE);
                        compositionModeSelect.setVisibility(View.INVISIBLE);
                        right_compositionModeSelect.setVisibility(View.INVISIBLE);

                        left_optimal.setVisibility(View.VISIBLE);
                        right_optimal.setVisibility(View.INVISIBLE);
                        center_optimal.setVisibility(View.INVISIBLE);

                        compositionMode = 2;
                        safeToTakePicture = false;
                    }
                } else if (text.contains("오른쪽 ")) {        //인식한 단어와 미리 설정한 캡쳐 수행 단어를 비교하여 같으면 캡쳐 기능 수행.
                    Toast.makeText(DetectorActivity.this, "최적 구도 - 3분할 오른쪽 모드",
                            Toast.LENGTH_SHORT).show();
                    if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
//                        compositionModeSelect.setText("R");
                        right_compositionModeSelect.setVisibility(View.VISIBLE);
                        left_compositionModeSelect.setVisibility(View.INVISIBLE);
                        center_compositionModeSelect.setVisibility(View.INVISIBLE);
                        compositionModeSelect.setVisibility(View.INVISIBLE);

                        right_optimal.setVisibility(View.VISIBLE);
                        center_optimal.setVisibility(View.INVISIBLE);
                        left_optimal.setVisibility(View.INVISIBLE);

                        compositionMode = 3;
                        safeToTakePicture = false;
                    }
                } else if (text.contains("일반 ")) {        //인식한 단어와 미리 설정한 캡쳐 수행 단어를 비교하여 같으면 캡쳐 기능 수행.
                    Toast.makeText(DetectorActivity.this, "일반 모드",
                            Toast.LENGTH_SHORT).show();
                    if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
//                        compositionModeSelect.setText("N");
                        compositionModeSelect.setVisibility(View.VISIBLE);
                        center_compositionModeSelect.setVisibility(View.INVISIBLE);
                        left_compositionModeSelect.setVisibility(View.INVISIBLE);
                        right_compositionModeSelect.setVisibility(View.INVISIBLE);

                        center_optimal.setVisibility(View.INVISIBLE);
                        left_optimal.setVisibility(View.INVISIBLE);
                        right_optimal.setVisibility(View.INVISIBLE);

                        compositionMode = 0;
                        safeToTakePicture = false;
                    }
                }else if (text.contains("줌 온 ") || text.contains("좀 온 ") || text.contains("즘 온 ") || text.contains("준 온 ") || text.contains("중 온 ")) {        //인식한 단어와 미리 설정한 캡쳐 수행 단어를 비교하여 같으면 캡쳐 기능 수행.
                    Toast.makeText(DetectorActivity.this, "자동 줌 모드 ON",
                            Toast.LENGTH_SHORT).show();
                    final View autoZoomBtn = findViewById(R.id.autozoom);
                    if (!autoZoomFlag) {        //플래그 검사 및 카메라 캡쳐 기능
                        autoZoomBtn.performClick();
                    }
                }else if (text.contains("줌 오프 ") || text.contains("좀 오프 ") || text.contains("즘 오프 ") || text.contains("준 오프 ") || text.contains("중 오프 ")) {        //인식한 단어와 미리 설정한 캡쳐 수행 단어를 비교하여 같으면 캡쳐 기능 수행.
                    Toast.makeText(DetectorActivity.this, "자동 줌 모드 OFF",
                            Toast.LENGTH_SHORT).show();
                    final View autoZoomBtn = findViewById(R.id.autozoom);
                    if (autoZoomFlag) {        //플래그 검사 및 카메라 캡쳐 기능
                        autoZoomBtn.performClick();
                    }
                }
            }
        });
    }

    /********** STT Functions Defined **********/
    private class MicrophoneRecognizeDelegate extends BaseRecognizeCallback implements RecognizeCallback {
        @Override
        public void onTranscription(SpeechRecognitionResults speechResults) {
            System.out.println(speechResults);
            if (speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
                String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
                showMicText(text);
                changeModeByVoice(text);
                if ((handsFreeCaptureCnt % 2) == 1) {     //음성인식을 수행하면서 여러 가지 후보를 따지는데, 경우에 따라 같은 단어로 두 개의 후보가 생성되기도 함.
                    handsFreeCapture(text);             //해당 조건검사를 해주지 않으면 사진이 두 번 연속 찍히는 현상 발생. 정상적으로 한 번만 인식되는 경우도 있어 완벽한 조건검사는 아님.
                }
                handsFreeCaptureCnt++;
                if (handsFreeCaptureCnt == 3) handsFreeCaptureCnt = 1;
            }
        }

        @Override
        public void onError(Exception e) {
            try {
                // This is critical to avoid hangs
                // (see https://github.com/watson-developer-cloud/android-sdk/issues/59)
                capture.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            showError(e);
            enableMicButton();
        }

        @Override
        public void onDisconnected() {
            enableMicButton();
        }
    }

    private void enableMicButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                handsfree.setEnabled(true);
            }
        });
    }

    private void showError(final Exception e) {
        runOnUiThread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                Toast.makeText(DetectorActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                // Update the icon background
                handsfree.setBackground(getDrawable(R.drawable.mic_off));
            }
        });
    }

    private void showMicText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                returnedText.setText(text);
                safeToTakePicture = true;               //카메라 캡쳐를 소프트웨어로 구현하기 위해서는 플래그 검사를 통해 제어해줄 필요가 있음.(구글링한 정보, 없으면 오류남)
            }
        });
    }

    private SpeechToText initSpeechToTextService() {
        Authenticator authenticator = new IamAuthenticator(getString(R.string.speech_text_iam_apikey));     //IBM Cloud STT 서비스의 API key => res/values/strings.xml 참고
        SpeechToText service = new SpeechToText(authenticator);
        service.setServiceUrl(getString(R.string.speech_text_url));            //IBM Cloud STT 서비스의 URL => res/values/strings.xml 참고
        return service;
    }

    private RecognizeOptions getRecognizeOptions(InputStream captureStream) {
        return new RecognizeOptions.Builder()
                .audio(captureStream)
                .contentType(ContentType.OPUS.toString())
                .model("ko-KR_BroadbandModel")      //한국어 모델 사용
                .interimResults(true)
                .inactivityTimeout(2000)
                .keywords(Arrays.asList(keywords))
                .keywordsThreshold((float) 0.5)
                .wordAlternativesThreshold((float) 0.5)
                .build();
    }
    /********** STT Functions Defined **********/

    // 핸즈프리 캡처를 하기 위해 필요한 메소드
    private void handsFreeCapture(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View handsFreeCapture = findViewById(R.id.capturePicture);
                for (int i = 0; i < 6; i++) {
                    if (text.contains(cheese[i])) {        //인식한 단어와 미리 설정한 캡쳐 수행 단어를 비교하여 같으면 캡쳐 기능 수행.
//                        Toast.makeText(DetectorActivity.this, "Take Photo",
//                                Toast.LENGTH_SHORT).show();
                        if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
                            handsFreeCapture.performClick();
                            safeToTakePicture = false;
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        if (!initialized)
            init();
        final float textSizePx =
        TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(CROP_WIDTH, CROP_HEIGHT, Config.ARGB_8888);

        int targetW, targetH;
        if (sensorOrientation == 90 || sensorOrientation == 270) {
            targetH = previewWidth;
            targetW = previewHeight;
        }
        else {
            targetW = previewWidth;
            targetH = previewHeight;
        }
        portraitBmp = Bitmap.createBitmap(targetW, targetH, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        CROP_WIDTH, CROP_HEIGHT,
                        sensorOrientation, false);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                canvas -> {
//                    String mode = "";
                    tracker.draw(canvas, getCameraFacing());
                    if (isDebug()) {
                        tracker.drawDebug(canvas);
                    }


//                    fin_X = (int) ((tracker.getFinX() / Device_width)*3100);
//                    int fin_Y = (int) ((tracker.getFinY() / Device_height)*4000);
////                    fin_X = 1000 - fin_X;
//                    fin_Y = 1000 - fin_Y;
//                    if(fin_X > 1000) fin_X = 1000;
//                    if(fin_Y > 1000) fin_Y = 1000;
//                    if(fin_X < 0) fin_X = 0;
//                    if(fin_Y < 0) fin_Y = 0;

                    sendChoosedFaceCoord(choosedFace);

                    if(compositionMode == 0 && !isRailTrackingMode){
                        mode = "N";
                    }else if(compositionMode == 1 && !isRailTrackingMode){
                        mode = "C";
                    }else if(compositionMode == 2 && !isRailTrackingMode){
                        mode = "L";
                    }else if(compositionMode == 3 && !isRailTrackingMode){
                        mode = "R";
                    }

                    if (tracker.getFaces_numb() > 0) {
                        fh = tracker.get_fh();
                    }

                    faces_numb = tracker.getFaces_numb();

                    // TODO: face prob 임계값 조건문으로 제어
                    if (tracker.getFaces_numb() > 0 && faceProb > 0.85){
                        face_info = mode + "!" + fin_X + "/" + fin_Y;
                        face_info += ":" + tracker.getDistanceMode();
                    }else{
                        face_info = "E";
                        // Empty or Not 100% customized face!
                    }
                    LOGGER.i("Face Info : " + face_info);

                    //TODO: customized Face 선택 후 좌표값 전송 완료. (좌표도 face detecing 부분이랑 동일한 좌표값으로 연산 완료.)
                    // Discuss part: Face Info로 얼굴이 없을 때는 "E"로 전송하게 되는데,
                    // 그럼 어플에서 확률값으로 블루투스 전송 제어를 하는 것 보다
                    // Face Info에 확률값 정보까지 추가해 MCU에 다 전송하여  MCU에서 "E" 나 확률값으로 모터 제어를 하는것이 나은지?
                    if (bt.isServiceAvailable()) {
                        bt.send(face_info, true);
                    } else {
                        bt.setupService();
                        bt.startService(BluetoothState.DEVICE_OTHER);
                        bt.send(face_info, true);
                    }
                });

        addCallback(
                canvas -> {
                    if (!isDebug()) {
                        return;
                    }
                    final Bitmap copy = cropCopyBitmap;
                    if (copy == null) {
                        return;
                    }

                    final int backgroundColor = Color.argb(100, 0, 0, 0);
                    canvas.drawColor(backgroundColor);

                    final Matrix matrix = new Matrix();
                    final float scaleFactor = 2;
                    matrix.postScale(scaleFactor, scaleFactor);
                    matrix.postTranslate(
                            canvas.getWidth() - copy.getWidth() * scaleFactor,
                            canvas.getHeight() - copy.getHeight() * scaleFactor);
                    canvas.drawBitmap(copy, matrix, new Paint());

                    final Vector<String> lines = new Vector<>();
                    lines.add("Frame: " + previewWidth + "x" + previewHeight);
                    lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
                    lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
                    lines.add("Rotation: " + sensorOrientation);
                    lines.add("Inference time: " + lastProcessingTimeMs + "ms");

                    borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
                });
    }

    OverlayView trackingOverlay;

    void init() {
        runInBackground(() -> {
            runOnUiThread(()-> initSnackbar.show());
            File dir = new File(FileUtils.ROOT);

            if (!dir.isDirectory()) {
                if (dir.exists()) dir.delete();
                dir.mkdirs();

                AssetManager mgr = getAssets();
                FileUtils.copyAsset(mgr, FileUtils.DATA_FILE);
                FileUtils.copyAsset(mgr, FileUtils.MODEL_FILE);
                FileUtils.copyAsset(mgr, FileUtils.LABEL_FILE);
            }

            try {
                recognizer = Recognizer.getInstance(getAssets());
            } catch (Exception e) {
                LOGGER.e("Exception initializing classifier!", e);
                finish();
            }

            runOnUiThread(()-> initSnackbar.dismiss());
            initialized = true;
        });
    }

    //  최적 구도 Dialog 창에서 모드 선택시 실행되는 메소드
    @Override
    public void onPositiveButtonClicked(String[] list, int position) {
        if(position == 0){
            compositionModeSelect.setVisibility(View.VISIBLE);
            center_compositionModeSelect.setVisibility(View.INVISIBLE);
            left_compositionModeSelect.setVisibility(View.INVISIBLE);
            right_compositionModeSelect.setVisibility(View.INVISIBLE);

            compositionMode = 0;

            center_optimal.setVisibility(View.INVISIBLE);
            left_optimal.setVisibility(View.INVISIBLE);
            right_optimal.setVisibility(View.INVISIBLE);
        }else if(position == 1){
//            compositionModeSelect.setText("C");
            center_compositionModeSelect.setVisibility(View.VISIBLE);
            compositionModeSelect.setVisibility(View.INVISIBLE);
            left_compositionModeSelect.setVisibility(View.INVISIBLE);
            right_compositionModeSelect.setVisibility(View.INVISIBLE);

            compositionMode = 1;

            center_optimal.setVisibility(View.VISIBLE);
            left_optimal.setVisibility(View.INVISIBLE);
            right_optimal.setVisibility(View.INVISIBLE);
        }else if(position == 2){
//            compositionModeSelect.setText("L");
            left_compositionModeSelect.setVisibility(View.VISIBLE);
            center_compositionModeSelect.setVisibility(View.INVISIBLE);
            compositionModeSelect.setVisibility(View.INVISIBLE);
            right_compositionModeSelect.setVisibility(View.INVISIBLE);

            compositionMode = 2;

            left_optimal.setVisibility(View.VISIBLE);
            right_optimal.setVisibility(View.INVISIBLE);
            center_optimal.setVisibility(View.INVISIBLE);
        }else{
//            compositionModeSelect.setText("R");
            right_compositionModeSelect.setVisibility(View.VISIBLE);
            left_compositionModeSelect.setVisibility(View.INVISIBLE);
            center_compositionModeSelect.setVisibility(View.INVISIBLE);
            compositionModeSelect.setVisibility(View.INVISIBLE);

            compositionMode = 3;

            right_optimal.setVisibility(View.VISIBLE);
            left_optimal.setVisibility(View.INVISIBLE);
            center_optimal.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onNegativeButtonClicked() {

    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        byte[] originalLuminance = getLuminance();

        tracker.onFrame(
                previewWidth,
                previewHeight,
                getLuminanceStride(),
                sensorOrientation,
                originalLuminance,
                timestamp);

        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection || !initialized || training) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        if (luminanceCopy == null) {
            luminanceCopy = new byte[originalLuminance.length];
        }
        System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(
                () -> {
                    LOGGER.i("Running detection on image " + currTimestamp);
                    final long startTime = SystemClock.uptimeMillis();

                    cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                    List<Recognizer.Recognition> mappedRecognitions =
                            recognizer.recognizeImage(croppedBitmap, cropToFrameTransform);

                    lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                    tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);

                    for(int i = 0; i < recognizer.getResults().size(); i++){
                        Log.d("List", recognizer.getResults().get(i).getTitle());
                    }

                    trackingOverlay.postInvalidate();

                    requestRender();
                    computingDetection = false;
                });
    }

    @Override
    protected Bitmap getPhotoImage() {
        return rgbFrameBitmap;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (!initialized) {
            Snackbar.make(
                    getWindow().getDecorView().findViewById(R.id.container),
                    getString(R.string.try_it_later), Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }

        if (resultCode == RESULT_OK) {
            trainSnackbar.show();
            button.setEnabled(false);
            training = true;

            ClipData clipData = data.getClipData();
            ArrayList<Uri> uris = new ArrayList<>();

            if (clipData == null) {
                uris.add(data.getData());
            } else {
                for (int i = 0; i < clipData.getItemCount(); i++)
                    uris.add(clipData.getItemAt(i).getUri());
            }

            new Thread(() -> {
                try {
                    recognizer.updateData(requestCode, getContentResolver(), uris);
                } catch (Exception e) {
                    LOGGER.e(e, "Exception!");
                } finally {
                    training = false;
                }
                runOnUiThread(() -> {
                    trainSnackbar.dismiss();
                    button.setEnabled(true);
                });
            }).start();

        }
        //////////////////////////////// Bluetooth Permission Result /////////////////////////////////
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bt.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth is enabled!!"
                        , Toast.LENGTH_SHORT).show();

//                bt.setupService();
//                bt.startService(BluetoothState.DEVICE_OTHER);
//                setup();
            } else {
                // Do something if user doesn't choose any device (Pressed back)
                Toast.makeText(getApplicationContext()
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public void performFileSearch(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");

        startActivityForResult(intent, requestCode);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                takePhoto();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if(saveLayout.getVisibility() == View.VISIBLE){
                    saveLayout.setVisibility(View.INVISIBLE);
                    mainTopLayout.setVisibility(View.VISIBLE);
                } else if(isTimerOn) {
                    // CountDownTimer closed when activate timer2, 5, 10 btn.
                    countDownTimer.cancel();
                    timerInfoText.setText("");
                    setTimerMode = 0;

                    timerBtn.setVisibility(View.VISIBLE);
                    timer2StateBtn.setVisibility(View.INVISIBLE);
                    timer5StateBtn.setVisibility(View.INVISIBLE);
                    timer10StateBtn.setVisibility(View.INVISIBLE);

                    isTimerOn = false;
                } else {
                    finish();
                }
                return true;
        }
        return false;
    }

    @Override
    public synchronized void onDestroy() {
        final View sttBtn = findViewById(R.id.handsFree_tf);
        try {
            if (listening) {
                sttBtn.performClick();
            }
        }catch (Exception e){}
        super.onDestroy();
    }
}
