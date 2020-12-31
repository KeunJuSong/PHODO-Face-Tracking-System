package com.example.phodo;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
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
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.FileCallback;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.Preview;
import com.otaliastudios.cameraview.filter.Filters;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import husaynhakeem.io.facedetector.FaceBounds;
import husaynhakeem.io.facedetector.FaceBoundsOverlay;
import husaynhakeem.io.facedetector.FaceDetector;
import husaynhakeem.io.facedetector.LensFacing;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SingleChoiceDialogFragment.SingleChoiceListener {
    private final static CameraLogger LOG = CameraLogger.create("PHODO_demo");
    private static String TAG = "MainActivity";
    private static String KEY_LENS_FACING = "key-lens-facing";

    private CameraView cameraView;
    private FaceBoundsOverlay faceBoundsOverlay;
    private long mCaptureTime; // Only used for Photo

    // Device Screen Size info
    private int Device_height = 0;
    private int Device_width = 0;

    //Bluetooth
    private BluetoothSPP bt;

    private CoordinatorLayout mainframe;
    private FrameLayout mainTopLayout;
    private FrameLayout flashLayout;
    private FrameLayout saveLayout;
    private FrameLayout timerLayout;

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

    public String faces_coord_info = "";

    private int handsFreeCaptureCnt = 1;
    private int faces_numb;
    private int fin_X;
    int min_idx_fh = 0;
    int max_idx_fh = 0;
    private int compositionMode = 0;

    private int setTimerMode = 0;
    private int captureFlag = 0;

    private boolean isBackgroundPictureTaken = false;

    int[] fh = new int[5];

    int autoZoomcnt = 0;
    private float autoZoomVal = 0;

    /* Variables for Auto Zoom Feature */
    protected boolean autoZoomFlag = false;
    float Zoom;

    protected SeekBar seekBar;  // Zoom
    protected SeekBar expSeekBar;   // Exposure

    private boolean listening = false;
    private boolean compositionModeFlag_fh = true;
    private boolean compositionModeFlag_fd = true;
    private ToggleButton handsfree;
    /********** Speech To Text Variables Define **********/

    private boolean safeToTakePicture = true;
    private boolean isRailTrackingMode = false;
    String mode = ""; // 전역변수로 선언

    // Composition Mode Select Button
//    private Button compositionModeSelect;
    private ImageButton compositionModeSelect;

    private ImageButton center_compositionModeSelect;
    private ImageButton left_compositionModeSelect;
    private ImageButton right_compositionModeSelect;

    private int mCurrentFilter = 0;
    private final Filters[] mAllFilters = Filters.values();

    //face flag(Empty or not)
    private boolean isFaceEmpty;

    private ImageButton flashBtn;
    private ImageButton flashAutoState;
    private ImageButton flashOffState;

    private ImageButton timerBtn;
    private ImageButton timer2StateBtn;
    private ImageButton timer5StateBtn;
    private ImageButton timer10StateBtn;

    // Camera Timer
    private CountDownTimer countDownTimer;
    private TextView timerInfoText;
    private boolean isTimerOn = false;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 상태바 제거
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        // 타이틀바 제거.
        // https://omty.tistory.com/12
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();


        cameraView = findViewById(R.id.cameraView);
        faceBoundsOverlay = findViewById(R.id.faceBoundsOverlay);

        mainframe = findViewById(R.id.mainframe);
        mainTopLayout = findViewById(R.id.mainTopLayout);
        saveLayout = findViewById(R.id.saveLayout);
        timerLayout = findViewById(R.id.timerLayout);
        flashLayout = findViewById(R.id.flashLayout);

        left_optimal = findViewById(R.id.left_optimal);
        right_optimal = findViewById(R.id.right_optimal);
        center_optimal = findViewById(R.id.center_optimal);

        cameraView.setLifecycleOwner(this);
        cameraView.addCameraListener(new Listener());
//        cameraSwitchBtn = findViewById(R.id.toggleCameraButton);

        /** ==============================================================================
         * Bluetooth Code
         * =============================================================================*/
        bt = new BluetoothSPP(this);

        if(!bt.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
            finish();
        }

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(getApplicationContext()
                        , "Connected to " + name + "\n" + address
                        , Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeviceDisconnected() {
                Toast.makeText(getApplicationContext()
                        , "Connection lost", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeviceConnectionFailed() {
                Toast.makeText(getApplicationContext()
                        , "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        });

        /** 단말기 display information send via bluetooth */
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        Device_width = dm.widthPixels;
        Device_height = dm.heightPixels;

        Facing lensFacing = (Facing) (savedInstanceState != null ? savedInstanceState.getSerializable(KEY_LENS_FACING) : null);
        if (lensFacing == null) {
            lensFacing = Facing.BACK;
        }

        setupCamera(lensFacing); // 얼굴 좌표값 조정 및 출력하는 method.

        // Rail/Phodo Tracking Mode.
        // 참조 사이트: https://webnautes.tistory.com/1375
        SwitchButton switchButton = findViewById(R.id.phodo_mode_switch);
        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 스위치 버튼이 체크되었는지 검사
                if (isChecked){
                    Toast.makeText(MainActivity.this,"Rail_Tracking Mode!", Toast.LENGTH_SHORT).show();
                    isRailTrackingMode = true;
                    mode = "RT";
                    compositionModeSelect.setEnabled(false);
                }else{
                    Toast.makeText(MainActivity.this,"PHODO_Tracking Mode!", Toast.LENGTH_SHORT).show();
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

//        findViewById(R.id.edit).setOnClickListener(this);

        findViewById(R.id.capturePicture).setOnClickListener(this);
//        findViewById(R.id.capturePictureSnapshot).setOnClickListener(this);
//        findViewById(R.id.captureVideo).setOnClickListener(this);
        findViewById(R.id.captureVideoSnapshot).setOnClickListener(this);
        findViewById(R.id.toggleCameraButton).setOnClickListener(this);
        findViewById(R.id.blue_btn).setOnClickListener(this);
        findViewById(R.id.saveBtn).setOnClickListener(this);

        flashBtn = findViewById(R.id.flashBtn);
        flashBtn.setOnClickListener(this);

        flashAutoState = findViewById(R.id.flashAutoState);
        flashAutoState.setOnClickListener(this);

        flashOffState = findViewById(R.id.flashOffState);
        flashOffState.setOnClickListener(this);

        timerBtn = findViewById(R.id.timerBtn);
        timerBtn.setOnClickListener(this);

        timer2StateBtn = findViewById(R.id.timer2StateBtn);
        timer2StateBtn.setOnClickListener(this);

        timer5StateBtn = findViewById(R.id.timer5StateBtn);
        timer5StateBtn.setOnClickListener(this);

        timer10StateBtn = findViewById(R.id.timer10StateBtn);
        timer10StateBtn.setOnClickListener(this);

//        findViewById(R.id.timerBtn).setOnClickListener(this);
//        findViewById(R.id.flashBtn).setOnClickListener(this);

        findViewById(R.id.flashAutoBtn).setOnClickListener(this);
        findViewById(R.id.flashOnBtn).setOnClickListener(this);
        findViewById(R.id.flashOffBtn).setOnClickListener(this);

        findViewById(R.id.timer5Btn).setOnClickListener(this);
        findViewById(R.id.timer10Btn).setOnClickListener(this);
        findViewById(R.id.timer2Btn).setOnClickListener(this);
        findViewById(R.id.timerDisableBtn).setOnClickListener(this);

        findViewById(R.id.album).setOnClickListener(this);

        /** 명도를 조절할 수 있는 오른쪽 사이드 바 */
        expSeekBar = findViewById(R.id.exposure);
        expSeekBar.setProgress(10);
        expSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d("expSeekBar 값:", Integer.toString(progress));
                switch (progress){
                    case 0:
                        cameraView.setExposureCorrection(-2.0f);
                        break;
                    case 1:
                        cameraView.setExposureCorrection(-1.8f);
                        break;
                    case 2:
                        cameraView.setExposureCorrection(-1.6f);
                        break;
                    case 3:
                        cameraView.setExposureCorrection(-1.4f);
                        break;
                    case 4:
                        cameraView.setExposureCorrection(-1.2f);
                        break;
                    case 5:
                        cameraView.setExposureCorrection(-1.0f);
                        break;
                    case 6:
                        cameraView.setExposureCorrection(-0.8f);
                        break;
                    case 7:
                        cameraView.setExposureCorrection(-0.6f);
                        break;
                    case 8:
                        cameraView.setExposureCorrection(-0.4f);
                        break;
                    case 9:
                        cameraView.setExposureCorrection(-0.2f);
                        break;
                    case 10:
                        cameraView.setExposureCorrection(0f);
                        break;
                    case 11:
                        cameraView.setExposureCorrection(0.2f);
                        break;
                    case 12:
                        cameraView.setExposureCorrection(0.4f);
                        break;
                    case 13:
                        cameraView.setExposureCorrection(0.8f);
                        break;
                    case 14:
                        cameraView.setExposureCorrection(1.0f);
                        break;
                    case 15:
                        cameraView.setExposureCorrection(1.2f);
                        break;
                    case 16:
                        cameraView.setExposureCorrection(1.4f);
                        break;
                    case 17:
                        cameraView.setExposureCorrection(1.6f);
                        break;
                    case 18:
                        cameraView.setExposureCorrection(1.8f);
                        break;
                    case 19:
                        cameraView.setExposureCorrection(1.85f);
                        break;
                    case 20:
                        cameraView.setExposureCorrection(2.0f);
                        break;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        /** 줌을 조절할 수 있는 하단 바 */
        seekBar = findViewById(R.id.zoom);
        seekBar.setMax(5);
        seekBar.setMin(0);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(!autoZoomFlag) {
                    Log.d("SeekBar 값:", Integer.toString(progress));
                    float current_Zoom = 0;
                    switch (progress){
                        case 0:
                            cameraView.setZoom((float) 0);
                            current_Zoom = (float) 0;
                            break;
                        case 1:
                            cameraView.setZoom((float) 0.2);
                            current_Zoom = (float) 0.2;
                            break;
                        case 2:
                            cameraView.setZoom((float) 0.4);
                            current_Zoom = (float) 0.4;
                            break;
                        case 3:
                            cameraView.setZoom((float) 0.6);
                            current_Zoom = (float) 0.6;
                            break;
                        case 4:
                            cameraView.setZoom((float) 0.8);
                            current_Zoom = (float) 0.8;
                            break;
                        case 5:
                            cameraView.setZoom((float) 1);
                            current_Zoom = (float) 1;
                            break;
                        default:
                            break;
                    }
                    Zoom = current_Zoom;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        /********** STT Service Defines **********/
        microphoneHelper = new MicrophoneHelper(this);
        safeToTakePicture = true;
        speechService = initSpeechToTextService();

        handsfree = findViewById(R.id.handsFree_tf);
        returnedText = findViewById(R.id.returnedtext_tf);
        timerInfoText = findViewById(R.id.timerInfoText);

        compositionModeSelect = findViewById(R.id.modeSelect_tf);
        center_compositionModeSelect = findViewById(R.id.center_modeSelect_tf);
        left_compositionModeSelect = findViewById(R.id.left_modeSelect_tf);
        right_compositionModeSelect = findViewById(R.id.right_modeSelect_tf);

        ToggleButton autoZoomButton = findViewById(R.id.autozoom);

        autoZoomButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                autoZoomFlag = isChecked;
                saveLayout.setVisibility(View.INVISIBLE);
                mainTopLayout.setVisibility(View.VISIBLE);
            }
        });

        handsfree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        /********** STT Service Defines **********/

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

        //TODO: mainFrame에서 최적구도 on 됬을 때 GUI 반응 자연스러운지 민용님과 논의.
//        Drawable on = ContextCompat.getDrawable(this, R.drawable.button);
//        Drawable off = ContextCompat.getDrawable(this, R.drawable.list_item_background);
//        Drawable activated = ContextCompat.getDrawable(this, R.drawable.list_item_background_square);
//        Drawable transParent = ContextCompat.getDrawable(this, R.color.transparent);

        /*  Waiting for 2 seconds and then automatically capture image for every 5 seconds
            when optimal composition mode is activated.
        */
        //TODO: Rename Threads
        //TODO: 1. 최적구도가 ON 됬을 때  액티비티 전환 말고 해당 엑티비티 유지하면서 5초마다 사진이 찍히면서 저장 되게끔.
        //TODO: 2. 앨범에서 카메라 이미지 눌렀을때 해당 이미지 보여주는 기능 구현. 아래 참고
        /** https://webnautes.tistory.com/1302 ==> 참고 => 엑티비티 전환으로 구현. (backpressed 버튼 눌렀을 때 다시 카메라 엑티비티로 전환) */
        final Runnable runnable = new Runnable() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void run() {
                final View handsFreeCapture = findViewById(R.id.capturePicture);
                if (faces_numb >= 1 && compositionModeFlag_fh && compositionModeFlag_fd) {
                    if (compositionMode == 1) {
                        if (fin_X > 450 && fin_X < 550) {
//                            compositionModeSelect.setBackground(on);
//                            mainframe.setBackground(activated);
                            if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
                                isBackgroundPictureTaken = true;
                                handsFreeCapture.performClick();
                                safeToTakePicture = false;
                            }
                        } else {
//                            compositionModeSelect.setBackground(off);
//                            mainframe.setBackground(transParent);
                        }
                    } else if (compositionMode == 2) {
                        // Reverse fin_X value when camera is facing FRONT/BACK.
                        if(cameraView.getFacing() == Facing.FRONT) {
                            if (fin_X > 616 && fin_X < 717) {
//                                compositionModeSelect.setBackground(on);
//                                mainframe.setBackground(activated);
                                if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
                                    isBackgroundPictureTaken = true;
                                    handsFreeCapture.performClick();
                                    safeToTakePicture = false;
                                }
                            } else {
//                                compositionModeSelect.setBackground(off);
//                                mainframe.setBackground(transParent);
                            }
                        } else if(cameraView.getFacing() == Facing.BACK){
                            if (fin_X > 283 && fin_X < 384) {
//                                compositionModeSelect.setBackground(on);
//                                mainframe.setBackground(activated);
                                if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
                                    isBackgroundPictureTaken = true;
                                    handsFreeCapture.performClick();
                                    safeToTakePicture = false;
                                }
                            } else {
//                                compositionModeSelect.setBackground(off);
//                                mainframe.setBackground(transParent);
                            }
                        }
                    } else if (compositionMode == 3) {
                        // Reverse fin_X value when camera is facing FRONT/BACK.
                        if(cameraView.getFacing() == Facing.FRONT) {
                            if (fin_X > 283 && fin_X < 384) {
//                                compositionModeSelect.setBackground(on);
//                                mainframe.setBackground(activated);
                                if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
                                    isBackgroundPictureTaken = true;
                                    handsFreeCapture.performClick();
                                    safeToTakePicture = false;
                                }
                            } else {
//                                compositionModeSelect.setBackground(off);
//                                mainframe.setBackground(transParent);
                            }
                        } else if(cameraView.getFacing() == Facing.BACK) {
                            if (fin_X > 616 && fin_X < 717) {
//                                compositionModeSelect.setBackground(on);
//                                mainframe.setBackground(activated);
                                if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
                                    isBackgroundPictureTaken = true;
                                    handsFreeCapture.performClick();
                                    safeToTakePicture = false;
                                }
                            } else {
//                                compositionModeSelect.setBackground(off);
//                                mainframe.setBackground(transParent);
                            }
                        }
                    }
                    else if (compositionMode == 0) {
//                        compositionModeSelect.setBackground(off);
//                        mainframe.setBackground(transParent);
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
                    runOnUiThread(runnable);
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

                //set new Flag that find face is Empty.
                if (!isFaceEmpty) {
                    if (autoZoomFlag) {
                        seekBar.setEnabled(false);

                        ZoomInVal = (float) (fh[max_idx_fh] - 500) / 10;
                        ZoomOutVal = (float) (fh[max_idx_fh] - 550) / 10;
                        if (fh[max_idx_fh] > 550) {
                            autoZoomVal -= (int) ZoomOutVal;
                        } else if (fh[max_idx_fh] < 500) {
                            autoZoomVal -= (int) ZoomInVal;
                        }
                        Log.d(this.getClass().getName(), "얼굴크기: " + fh[max_idx_fh]);

                        Log.d(this.getClass().getName(), "오토줌: " + autoZoomVal);

                        if (autoZoomVal > 60) autoZoomVal = 60;
                        if (autoZoomVal < 0) autoZoomVal = 0;

                        // Check the Cameraview's current Engine => Going to set Camera1 engine by Default.
                        if(cameraView.getEngine() == Engine.CAMERA1) {
                            // CameraView lib에서 SetZoomInternal 정의!
                            // Camera Parameters로 제어 (Same action with yyc's branch.)
                            cameraView.SetZoomInternal((int) autoZoomVal);
                        }
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

    private void message(@NonNull String content, boolean important) {
        if (important) {
            LOG.w(content);
            Toast.makeText(this, content, Toast.LENGTH_LONG).show();
        } else {
            LOG.i(content);
            Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
        }
    }

    private class Listener extends CameraListener {
        @Override
        public void onCameraOpened(@NonNull CameraOptions options) {
//            ViewGroup group = (ViewGroup) controlpanel.getChildAt(0);
//            for (int i = 0; i < group.getChildCount(); i++) {
//                OptionView view = (OptionView) group.getChildAt(i);
//                view.onCameraOpened(cameraView, options);
//            }
        }

        @Override
        public void onCameraError(@NonNull CameraException exception) {
            super.onCameraError(exception);
            message("Got CameraException #" + exception.getReason(), true);
        }

        @Override
        public void onPictureTaken(@NonNull PictureResult result) {
            super.onPictureTaken(result);
            //TODO: 최적구도가 진행중 => 액티비티 전환 X, 5초마다 찍힐 시 사진 Save.
            if (cameraView.isTakingVideo()) {
                message("Captured while taking video. Size=" + result.getSize(), false);
                return;
            }
            // This can happen if picture was taken with a gesture.
            long callbackTime = System.currentTimeMillis();
            if (mCaptureTime == 0) mCaptureTime = callbackTime - 300;
            LOG.w("onPictureTaken called! Launching activity. Delay:", callbackTime - mCaptureTime);
            PicturePreviewActivity.setPictureResult(result);
            Intent intent = new Intent(MainActivity.this, PicturePreviewActivity.class);
            intent.putExtra("delay", callbackTime - mCaptureTime);
            if(isBackgroundPictureTaken) {
                String extension;
                switch (result.getFormat()) {
                    case JPEG: extension = "jpg"; break;
                    case DNG: extension = "dng"; break;
                    default: throw new RuntimeException("Unknown format.");
                }
                File file = new File(getFilesDir(), "picture." + extension);
                CameraUtils.writeToFile(result.getData(), file, new FileCallback() {
                    @Override
                    public void onFileReady(@Nullable File file) {
                        if(file != null) {
                            //Saving Photo!!
                            Toast.makeText(MainActivity.this,
                                    "Save Photo.",
                                    Toast.LENGTH_SHORT).show();
                            saveFile(file);
                        }else {
                            Toast.makeText(MainActivity.this,
                                    "Error while writing file.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                isBackgroundPictureTaken = false;
            } else {
                startActivity(intent);
            }
            mCaptureTime = 0;
            LOG.w("onPictureTaken called! Launched activity.");
        }

        @Override
        public void onVideoTaken(@NonNull VideoResult result) {
            super.onVideoTaken(result);
            LOG.w("onVideoTaken called! Launching activity.");
            VideoPreviewActivity.setVideoResult(result);
            Intent intent = new Intent(MainActivity.this, VideoPreviewActivity.class);
            startActivity(intent);
            LOG.w("onVideoTaken called! Launched activity.");
        }

        @Override
        public void onVideoRecordingStart() {
            super.onVideoRecordingStart();
            LOG.w("onVideoRecordingStart!");
        }

        @Override
        public void onVideoRecordingEnd() {
            super.onVideoRecordingEnd();
            message("Video taken. Processing...", false);
            LOG.w("onVideoRecordingEnd!");
        }

        @Override
        public void onExposureCorrectionChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
            super.onExposureCorrectionChanged(newValue, bounds, fingers);
            message("Exposure correction:" + newValue, false);
        }

        @Override
        public void onZoomChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
            super.onZoomChanged(newValue, bounds, fingers);
            message("Zoom:" + newValue, false);
//            seekBar.setProgress((int)(5*newValue));
        }
    }


    @SuppressLint("ResourceType")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
//            case R.id.edit:
//                edit();
//                break;
            case R.id.capturePicture:
                if(setTimerMode > 0){
                    if(setTimerMode == 1){
                        isTimerOn = true;
                        countDownTimer = new CountDownTimer(2000,1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                // Show Text 2sec Timer info!
                                timerInfoText.setText(String.format(Locale.getDefault(), "%d", millisUntilFinished / 1000L));
                            }
                            @Override
                            public void onFinish() {
                                timerInfoText.setText("");
                                capturePicture();
                            }
                        };
                        countDownTimer.start();
                    } else if(setTimerMode == 2){
                        isTimerOn = true;
                        countDownTimer = new CountDownTimer(5000,1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                // Show Text 5sec Timer info!
                                timerInfoText.setText(String.format(Locale.getDefault(), "%d", millisUntilFinished / 1000L));
                            }

                            @Override
                            public void onFinish() {
                                timerInfoText.setText("");
                                capturePicture();
                            }
                        };
                        countDownTimer.start();
                    } else if(setTimerMode == 3){
                        isTimerOn = true;
                        countDownTimer = new CountDownTimer(10000,1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                // Show Text 10sec Timer info!
                                timerInfoText.setText(String.format(Locale.getDefault(), "%d", millisUntilFinished / 1000L));
                            }

                            @Override
                            public void onFinish() {
                                timerInfoText.setText("");
                                capturePicture();
                            }
                        };
                        countDownTimer.start();
                    }
                }else {
                    isTimerOn = false;
                    capturePicture();
                }
                break;
//            case R.id.capturePictureSnapshot:
//                capturePictureSnapshot();
//                break;
//            case R.id.captureVideo:
//                captureVideo();
//                break;
            case R.id.captureVideoSnapshot:
                captureVideoSnapshot();
                break;
            case R.id.toggleCameraButton:
                final View sttBtn = findViewById(R.id.handsFree_tf);
                final View autoZoomBtn = findViewById(R.id.autozoom);
                if (listening) {
                    sttBtn.performClick();
                }
                if (autoZoomFlag){
                    autoZoomBtn.performClick();
                }
                toggleCamera();
                break;
            case R.id.blue_btn:
                saveLayout.setVisibility(View.INVISIBLE);
                mainTopLayout.setVisibility(View.VISIBLE);
                bluetoothStart();
                break;
            case R.id.saveBtn:
                showSaveBtn();
                break;
            case R.id.flashBtn:
            case R.id.flashAutoState:
            case R.id.flashOffState:
                showFlashLayout();
                break;
            case R.id.timerBtn:
            case R.id.timer2StateBtn:
            case R.id.timer5StateBtn:
            case R.id.timer10StateBtn:
                showTimerLayout();
                break;
            case R.id.flashAutoBtn:
                if(flashLayout.getVisibility() == View.VISIBLE){
                    flashAutoState.setVisibility(View.VISIBLE);
                    flashOffState.setVisibility(View.INVISIBLE);
                    flashBtn.setVisibility(View.INVISIBLE);

                    cameraView.setFlash(Flash.AUTO);
                    flashLayout.setVisibility(View.INVISIBLE);
                    mainTopLayout.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.flashOnBtn:
                if(flashLayout.getVisibility() == View.VISIBLE){
                    flashBtn.setVisibility(View.VISIBLE);
                    flashAutoState.setVisibility(View.INVISIBLE);
                    flashOffState.setVisibility(View.INVISIBLE);

                    cameraView.setFlash(Flash.ON);
                    flashLayout.setVisibility(View.INVISIBLE);
                    mainTopLayout.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.flashOffBtn:
                if(flashLayout.getVisibility() == View.VISIBLE){
                    flashOffState.setVisibility(View.VISIBLE);
                    flashAutoState.setVisibility(View.INVISIBLE);
                    flashBtn.setVisibility(View.INVISIBLE);

                    cameraView.setFlash(Flash.OFF);
                    flashLayout.setVisibility(View.INVISIBLE);
                    mainTopLayout.setVisibility(View.VISIBLE);
                }
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

//    @Override
//    public void onBackPressed() {
////        BottomSheetBehavior b = BottomSheetBehavior.from(controlpanel);
////        if (b.getState() != BottomSheetBehavior.STATE_HIDDEN) {
////            b.setState(BottomSheetBehavior.STATE_HIDDEN);
////            return;
////        }
//        //CountDownTimer close
//        countDownTimer.cancel();
//        if(saveLayout.getVisibility() == View.VISIBLE){
//            saveLayout.setVisibility(View.INVISIBLE);
//            mainTopLayout.setVisibility(View.VISIBLE);
//        }
//        super.onBackPressed();
//    }

//    private void edit() {
//        BottomSheetBehavior b = BottomSheetBehavior.from(controlpanel);
//        b.setState(BottomSheetBehavior.STATE_COLLAPSED);
//    }

    private void capturePicture() {
        if (cameraView.getMode() == Mode.VIDEO) {
            message("Can't take HQ pictures while in VIDEO mode.", false);
            return;
        }
        if (cameraView.isTakingPicture()) return;
        mCaptureTime = System.currentTimeMillis();
        message("Capturing picture...", false);
        cameraView.takePicture();
    }

//    private void capturePictureSnapshot() {
//        if (cameraView.isTakingPicture()) return;
//        if (cameraView.getPreview() != Preview.GL_SURFACE) {
//            message("Picture snapshots are only allowed with the GL_SURFACE preview.", true);
//            return;
//        }
//        mCaptureTime = System.currentTimeMillis();
//        message("Capturing picture snapshot...", false);
//        cameraView.takePictureSnapshot();
//    }

    /**
     * Maybe This will not work while face detecting!
     */
//    private void captureVideo() {
//        if (cameraView.getMode() == Mode.PICTURE) {
//            message("Can't record HQ videos while in PICTURE mode.", false);
//            return;
//        }
//        if (cameraView.isTakingPicture() || cameraView.isTakingVideo()) return;
//        message("Recording for 5 seconds...", true);
//        cameraView.takeVideo(new File(getFilesDir(), "video.mp4"), 5000);
//    }

    /**
     * This will work while face detecting!
     */
    private void captureVideoSnapshot() {
        if (cameraView.isTakingVideo()) {
            message("Already taking video.", false);
            return;
        }
        if (cameraView.getPreview() != Preview.GL_SURFACE) {
            message("Video snapshots are only allowed with the GL_SURFACE preview.", true);
            return;
        }
        message("Recording snapshot for 5 seconds...", true);

        cameraView.takeVideoSnapshot(new File(getFilesDir(), "video.mp4"), 5000);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(KEY_LENS_FACING, cameraView.getFacing());
        super.onSaveInstanceState(outState);
    }


    private void setupCamera(Facing lensFacing) {
        FaceDetector faceDetector = new FaceDetector(faceBoundsOverlay);
        cameraView.setFacing(lensFacing);
        cameraView.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {
//                FaceDetector faceDetectProcessor = faceDetector;

//                byte[] faceData = frame.getData();
//                int faceRotation = frame.getRotation();
//                Size faceSize = frame.getSize();
//                int faceWidth = faceSize.getWidth();
//                Size faceSize2 = frame.getSize();
//                android.util.Size f_faceSize = new android.util.Size(faceWidth, faceSize2.getHeight());
//                int faceFormat = frame.getFormat();
                CameraView camerafacing = (CameraView) MainActivity.this.findViewById(R.id.cameraView);
//
//                faceDetector.process(new husaynhakeem.io.facedetector.Frame(faceData, faceRotation,
//                        f_faceSize , faceFormat, camerafacing.getFacing() == Facing.BACK ? LensFacing.BACK : LensFacing.FRONT));
                faceDetector.process(new husaynhakeem.io.facedetector.Frame(frame.getData(), frame.getRotation(),
                        new Size(frame.getSize().getWidth(), frame.getSize().getHeight()), frame.getFormat(),
                        camerafacing.getFacing() == Facing.BACK ? LensFacing.BACK : LensFacing.FRONT));

                isFaceEmpty = faceDetector.getFaceBoundInfo().isEmpty();
                // Variables definition for send average coordinates of faces
                //TODO: 좌표 계산 결과가 다름. => 연산 logic 수정
                // TODO 완료.
                faces_numb = faceDetector.getFaceBoundInfo().size();
                int[] X_f = new int[5];
                int[] Y_f = new int[5];
                RectF[] face_rectF = new RectF[5];
                int sum_x = 0, sum_y = 0;
                int min_idx_fd = 0;
                int max_idx_fd = 0;
                String face_info = "";
                String distanceMode = "";

                if(faces_numb >= 6){
                    Toast.makeText(getApplicationContext()
                            , "Too Many Faces...!!! ", Toast.LENGTH_SHORT).show();
                }else {
                    for (int i = 0; i < faces_numb; ++i) {
                        /** Get the mapping overall face coordinate with Overlay
                         * Check from {@link FaceDetector} */
                        FaceBounds EachFace = faceDetector.getFaceBoundInfo().get(i);
                        face_rectF[i] = new RectF(EachFace.getBox());

                        // Width => set range 0 ~ 1000
                        // Height => set range 0 ~ 2000
                        X_f[i] = (int) ((face_rectF[i].centerX() / Device_width) * 1000);
                        Y_f[i] = (int) ((face_rectF[i].centerY() / Device_height) * 1000);

                        sum_x += X_f[i];
                        sum_y += Y_f[i];

                        fh[i] = faceDetector.getRawFace().get(i).getBoundingBox().height();
                        Log.d("Fh 값:", Integer.toString(fh[0]));
                    }

                    if (compositionMode == 0 && !isRailTrackingMode) {
                        mode = "N";
                    } else if (compositionMode == 1 && !isRailTrackingMode) {
                        mode = "C";
                    } else if (compositionMode == 2 && !isRailTrackingMode) {
                        mode = "L";
                    } else if (compositionMode == 3 && !isRailTrackingMode) {
                        mode = "R";
                    }

                    for (int i = 0; i < faceDetector.getFaceBoundInfo().size(); i++) {
                        if (fh[i] >= fh[max_idx_fh]) {
                            max_idx_fh = i;
                        }
                        if (fh[i] >= fh[min_idx_fh]) {
                            min_idx_fh = i;
                        }
                    }

                    for (int i = 0; i < faceDetector.getFaceBoundInfo().size(); i++) {
                        if (X_f[i] + Y_f[i] >= X_f[max_idx_fd] + Y_f[max_idx_fd]) {
                            max_idx_fd = i;
                        }
                        if (X_f[i] + Y_f[i] <= X_f[min_idx_fd] + Y_f[min_idx_fd]) {
                            min_idx_fd = i;
                        }
                    }

                    if (fh[max_idx_fh] > 80) {
                        distanceMode = "N";
                    } else if (fh[max_idx_fh] > 45) {
                        distanceMode = "M1";
                    } else if (fh[max_idx_fh] > 30) {
                        distanceMode = "M2";
                    } else {
                        distanceMode = "M3";
                    }

                    fin_X = sum_x / faces_numb;
                    int fin_Y = sum_y / faces_numb;

                    if (cameraView.getFacing() == Facing.FRONT) {
                        fin_X = 1000 - fin_X;
                    }

                    face_info = mode + "!" + fin_X + "/" + fin_Y;
                    face_info += ":" + distanceMode;
                }

                faces_coord_info = face_info;

                Log.d("블루투스 전송 데이터", faces_coord_info);

                // send data via Bluetooth.
                if (bt.isServiceAvailable()) {
                    bt.send(faces_coord_info, true);
                } else {
                    bt.setupService();
                    bt.startService(BluetoothState.DEVICE_OTHER);
                    bt.send(faces_coord_info, true);
                }
            }
        });
    }

//    @Override
//    public <T> boolean onValueChanged(@NonNull Option<T> option, @NonNull T value, @NonNull String name) {
//        if ((option instanceof Option.Width || option instanceof Option.Height)) {
//            Preview preview = cameraView.getPreview();
//            boolean wrapContent = (Integer) value == ViewGroup.LayoutParams.WRAP_CONTENT;
//            if (preview == Preview.SURFACE && !wrapContent) {
//                message("The SurfaceView preview does not support width or height changes. " +
//                        "The view will act as WRAP_CONTENT by default.", true);
//                return false;
//            }
//        }
//        option.set(cameraView, value);
//        BottomSheetBehavior b = BottomSheetBehavior.from(controlpanel);
//        b.setState(BottomSheetBehavior.STATE_HIDDEN);
//        message("Changed " + option.getName() + " to " + name, false);
//        return true;
//    }

    private void toggleCamera() {
        if (cameraView.isTakingPicture() || cameraView.isTakingVideo()) return;
        switch (cameraView.toggleFacing()) {
            case BACK:
                message("Switched to back camera!", false);
                break;

            case FRONT:
                message("Switched to front camera!", false);
                break;
        }
    }

    private void showSaveBtn() {
        mainTopLayout.setVisibility(View.INVISIBLE);
        saveLayout.setVisibility(View.VISIBLE);
    }

    private void showFlashLayout() {
        mainTopLayout.setVisibility(View.INVISIBLE);
        flashLayout.setVisibility(View.VISIBLE);
    }

    private void showTimerLayout() {
        mainTopLayout.setVisibility(View.INVISIBLE);
        timerLayout.setVisibility(View.VISIBLE);
    }

    //region Permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean valid = true;
        for (int grantResult : grantResults) {
            valid = valid && grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (valid && !cameraView.isOpened()) {
            cameraView.open();
        }
    }
    //endregion

    //  최적 구도 Dialog 창에서 모드 선택시 실행되는 메소드
    @Override
    public void onPositiveButtonClicked(String[] list, int position) {
        if (position == 0) {
//            compositionModeSelect.setText("N");
            compositionModeSelect.setVisibility(View.VISIBLE);
            center_compositionModeSelect.setVisibility(View.INVISIBLE);
            left_compositionModeSelect.setVisibility(View.INVISIBLE);
            right_compositionModeSelect.setVisibility(View.INVISIBLE);

            compositionMode = 0;

            center_optimal.setVisibility(View.INVISIBLE);
            left_optimal.setVisibility(View.INVISIBLE);
            right_optimal.setVisibility(View.INVISIBLE);
        } else if (position == 1) {
//            compositionModeSelect.setText("C");
            center_compositionModeSelect.setVisibility(View.VISIBLE);
            compositionModeSelect.setVisibility(View.INVISIBLE);
            left_compositionModeSelect.setVisibility(View.INVISIBLE);
            right_compositionModeSelect.setVisibility(View.INVISIBLE);

            compositionMode = 1;

            center_optimal.setVisibility(View.VISIBLE);
            left_optimal.setVisibility(View.INVISIBLE);
            right_optimal.setVisibility(View.INVISIBLE);

        } else if (position == 2) {
//            compositionModeSelect.setText("L");
            left_compositionModeSelect.setVisibility(View.VISIBLE);
            center_compositionModeSelect.setVisibility(View.INVISIBLE);
            compositionModeSelect.setVisibility(View.INVISIBLE);
            right_compositionModeSelect.setVisibility(View.INVISIBLE);

            compositionMode = 2;

            left_optimal.setVisibility(View.VISIBLE);
            right_optimal.setVisibility(View.INVISIBLE);
            center_optimal.setVisibility(View.INVISIBLE);
        } else {
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

    //  STT로 모드 활성화/비활성화 하는데 필요한 메소드
    private void changeModeByVoice(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (text.contains("중앙 ")) {        //인식한 단어와 미리 설정한 캡쳐 수행 단어를 비교하여 같으면 캡쳐 기능 수행.
                    Toast.makeText(MainActivity.this, "최적 구도 - 중앙 모드",
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
                    Toast.makeText(MainActivity.this, "최적 구도 - 3분할 왼쪽 모드",
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
                    Toast.makeText(MainActivity.this, "최적 구도 - 3분할 오른쪽 모드",
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
                    Toast.makeText(MainActivity.this, "일반 모드",
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
                    Toast.makeText(MainActivity.this, "자동 줌 모드 ON",
                            Toast.LENGTH_SHORT).show();
                    final View autoZoomBtn = findViewById(R.id.autozoom);
                    if (!autoZoomFlag) {        //플래그 검사 및 카메라 캡쳐 기능
                        autoZoomBtn.performClick();
                    }
                }else if (text.contains("줌 오프 ") || text.contains("좀 오프 ") || text.contains("즘 오프 ") || text.contains("준 오프 ") || text.contains("중 오프 ")) {        //인식한 단어와 미리 설정한 캡쳐 수행 단어를 비교하여 같으면 캡쳐 기능 수행.
                    Toast.makeText(MainActivity.this, "자동 줌 모드 OFF",
                            Toast.LENGTH_SHORT).show();
                    final View autoZoomBtn = findViewById(R.id.autozoom);
                    if (autoZoomFlag) {        //플래그 검사 및 카메라 캡쳐 기능
                        autoZoomBtn.performClick();
                    }
                }
                //TODO: AutoZoom merge with CameraView lib (진행 예정.)
                /** merge TODO */
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
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(MainActivity.this, "Take Photo",
                                Toast.LENGTH_SHORT).show();
                        if (safeToTakePicture) {        //플래그 검사 및 카메라 캡쳐 기능
                            handsFreeCapture.performClick();
                            safeToTakePicture = false;
                        }
                    }
                }
            }
        });
    }

    private void bluetoothStart() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        mBuilder.setTitle("Bluetooth 기능 활성화")
                .setMessage("Bluetooth를 활성화 하시겠습니까?")
                .setCancelable(false)
                .setPositiveButton("On", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(bt.getServiceState() != BluetoothState.STATE_CONNECTED){
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

    @Override
    protected void onDestroy() {
        final View sttBtn = findViewById(R.id.handsFree_tf);
        final View autoZoomBtn = findViewById(R.id.autozoom);
        try {
            if (listening){
                sttBtn.performClick();
            }
            if (autoZoomFlag){
                autoZoomBtn.performClick();
            }
            safeToTakePicture = false;
        } catch(Exception e) { }

        bt.stopService();
        super.onDestroy();
    }

    // Bluetooth Permission Method.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bt.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth is enabled!!"
                        , Toast.LENGTH_SHORT).show();
            } else {
                // Do something if user doesn't choose any device (Pressed back)
                Toast.makeText(getApplicationContext()
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // When MainActivity is Start.
    @Override
    protected void onStart() {
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

    private void getAlbum() {
        Intent intent = new Intent(MainActivity.this, AlbumPreviewActivity.class);
        startActivity(intent);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                capturePicture();
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

    // Save Picture When Optimal Composition is On.
    private void saveFile(File pictureFile) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "Phodo_image");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/*");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        ContentResolver contentResolver = getContentResolver();
        Uri item = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            ParcelFileDescriptor pdf = contentResolver.openFileDescriptor(item, "w", null);

            if (pdf == null) {
                Log.d("asdf", "null");
            } else {
                byte[] data = FileUtils.readFileToByteArray(pictureFile);
                FileOutputStream fos = new FileOutputStream(pdf.getFileDescriptor());
                fos.write(data);
                fos.close();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    contentResolver.update(item, values, null, null);
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}