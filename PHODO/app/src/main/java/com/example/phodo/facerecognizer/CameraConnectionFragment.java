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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import com.example.phodo.R;
import com.example.phodo.facerecognizer.env.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;


@SuppressLint("ValidFragment")
public class CameraConnectionFragment extends Fragment {
    private static final Logger LOGGER = new Logger();

    /**
     * The camera preview size will be chosen to be the smallest frame by pixel size capable of
     * containing a DESIRED_SIZE x DESIRED_SIZE square.
     */
    private static final int MINIMUM_PREVIEW_SIZE = 320;

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    private int mState = STATE_PREVIEW;
    private boolean mFlashSupported;

    /** Conversion from screen rotation to JPEG orientation. */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /** A {@link Semaphore} to prevent the app from exiting before closing the camera. */
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);
    /** A {@link OnImageAvailableListener} to receive frames as they are available. */
    private final OnImageAvailableListener imageListener;
    /** The input size in pixels desired by TensorFlow (width and height of a square bitmap). */
    private final Size inputSize;
    /** The layout identifier to inflate for this Fragment. */
    private final int layout;

    private final ConnectionCallback cameraConnectionCallback;
    protected final CameraCaptureSession.CaptureCallback captureCallback =
            new CameraCaptureSession.CaptureCallback() {

                private void process(CaptureResult result) {
                    switch (mState) {
                        case STATE_PREVIEW: {
                            // We have nothing to do when the camera preview is working normally.
                            break;
                        }
                        case STATE_WAITING_LOCK: {
                            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                            if (afState == null) {
                                captureStillPicture();
                            } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                                // CONTROL_AE_STATE can be null on some devices
                                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                                if (aeState == null ||
                                        aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                                    mState = STATE_PICTURE_TAKEN;
                                    captureStillPicture();
                                } else {
                                    runPrecaptureSequence();
                                }
                            }
                            break;
                        }
                        case STATE_WAITING_PRECAPTURE: {
                            // CONTROL_AE_STATE can be null on some devices
                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                            if (aeState == null ||
                                    aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                                    aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                                mState = STATE_WAITING_NON_PRECAPTURE;
                            }
                            break;
                        }
                        case STATE_WAITING_NON_PRECAPTURE: {
                            // CONTROL_AE_STATE can be null on some devices
                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                            if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                                mState = STATE_PICTURE_TAKEN;
                                captureStillPicture();
                            }
                            break;
                        }
                    }
                }
                @Override
                public void onCaptureProgressed(
                        final CameraCaptureSession session,
                        final CaptureRequest request,
                        final CaptureResult partialResult) {}

                @Override
                public void onCaptureCompleted(
                        final CameraCaptureSession session,
                        final CaptureRequest request,
                        final TotalCaptureResult result) {}
            };
    /** ID of the current {@link CameraDevice}. */
    private String cameraId;
    /** An {@link AutoFitTextureView} for camera preview. */
    private AutoFitTextureView textureView;
    /** A {@link CameraCaptureSession } for camera preview. */
    protected CameraCaptureSession captureSession;
    /** A reference to the opened {@link CameraDevice}. */
    private CameraDevice cameraDevice;
    /** The rotation in degrees of the camera sensor from the display. */
    private Integer sensorOrientation;
    /** The {@link Size} of camera preview. */
    private Size previewSize;
    /** An additional thread for running tasks that shouldn't block the UI. */
    private HandlerThread backgroundThread;
    /** A {@link Handler} for running tasks in the background. */
    protected Handler backgroundHandler;
    /** An {@link ImageReader} that handles preview frame capture. */
    private ImageReader previewReader;
    /** {@link CaptureRequest.Builder} for the camera preview */
    protected CaptureRequest.Builder previewRequestBuilder;
    /** {@link CaptureRequest} generated by {@link #previewRequestBuilder} */
    private CaptureRequest previewRequest;
    /** {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state. */
    protected CameraCharacteristics cameraCharacteristics;
    protected Zoom zoom;
    private File mFile;
    private Button captureBtn;
    private final CameraDevice.StateCallback stateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(final CameraDevice cd) {
                    // This method is called when the camera is opened.  We start camera preview here.
                    cameraOpenCloseLock.release();
                    cameraDevice = cd;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(final CameraDevice cd) {
                    cameraOpenCloseLock.release();
                    cd.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(final CameraDevice cd, final int error) {
                    cameraOpenCloseLock.release();
                    cd.close();
                    cameraDevice = null;
                    final Activity activity = getActivity();
                    if (null != activity) {
                        activity.finish();
                    }
                }
            };
    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a {@link
     * TextureView}.
     */
    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(
                        final SurfaceTexture texture, final int width, final int height) {
                    openCamera(width, height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(
                        final SurfaceTexture texture, final int width, final int height) {
                    configureTransform(width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(final SurfaceTexture texture) {}
            };

    CameraConnectionFragment(
            final ConnectionCallback connectionCallback,
            final OnImageAvailableListener imageListener,
            final int layout,
            final Size inputSize) {
        this.cameraConnectionCallback = connectionCallback;
        this.imageListener = imageListener;
        this.layout = layout;
        this.inputSize = inputSize;
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the minimum of both, or an exact match if possible.
     *
     * @param choices The list of sizes that the camera supports for the intended output class
     * @param width The minimum desired width
     * @param height The minimum desired height
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    protected static Size chooseOptimalSize(final Size[] choices, final int width, final int height) {
        final int minSize = Math.max(Math.min(width, height), MINIMUM_PREVIEW_SIZE);
        final Size desiredSize = new Size(width, height);

        // Collect the supported resolutions that are at least as big as the preview Surface
        boolean exactSizeFound = false;
        final List<Size> bigEnough = new ArrayList<Size>();
        final List<Size> tooSmall = new ArrayList<Size>();
        for (final Size option : choices) {
            if (option.equals(desiredSize)) {
                // Set the size but don't return yet so that remaining sizes will still be logged.
                exactSizeFound = true;
            }

            if (option.getHeight() >= minSize && option.getWidth() >= minSize) {
                bigEnough.add(option);
            } else {
                tooSmall.add(option);
            }
        }

        LOGGER.i("Desired size: " + desiredSize + ", min size: " + minSize + "x" + minSize);
        LOGGER.i("Valid preview sizes: [" + TextUtils.join(", ", bigEnough) + "]");
        LOGGER.i("Rejected preview sizes: [" + TextUtils.join(", ", tooSmall) + "]");

        if (exactSizeFound) {
            LOGGER.i("Exact size match found.");
            return desiredSize;
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            final Size chosenSize = Collections.min(bigEnough, new CompareSizesByArea());
            LOGGER.i("Chosen size: " + chosenSize.getWidth() + "x" + chosenSize.getHeight());
            return chosenSize;
        } else {
            LOGGER.e("Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static CameraConnectionFragment newInstance(
            final ConnectionCallback callback,
            final OnImageAvailableListener imageListener,
            final int layout,
            final Size inputSize) {
        return new CameraConnectionFragment(callback, imageListener, layout, inputSize);
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private final OnImageAvailableListener mOnImageAvailableListener
            = new OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            backgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
        }

    };

    @Override
    public View onCreateView(
            final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.camera_connection_fragment_tracking,
                container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        textureView =   (AutoFitTextureView) view.findViewById(R.id.texture);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFile = new File(Environment.getExternalStorageDirectory(), "/DCIM/"+"photo"+currentDateFormat()+".jpg");
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeCamera();
    }

    public void setCamera(String cameraId) {
        this.cameraId = cameraId;
    }

    private String currentDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HH_mm_ss");
        String currentTime = dateFormat.format(new Date());
        return currentTime;
    }

    /**
     * Initiate a still image capture.
     */
    public void takePicture() {
        LOGGER.i("Capture Image");
        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            captureSession.capture(previewRequestBuilder.build(), captureCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /** Sets up member variables related to camera. */
    private void setUpCameraOutputs() {
        final Activity activity = getActivity();
        final CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraCharacteristics= manager.getCameraCharacteristics(cameraId);

            final StreamConfigurationMap map =
                    cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            zoom = new Zoom(cameraCharacteristics);

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            previewSize =
                    chooseOptimalSize(
                            map.getOutputSizes(SurfaceTexture.class),
                            inputSize.getWidth(),
                            inputSize.getHeight());

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            final int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
            } else {
                textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
            }
        } catch (final CameraAccessException e) {
            LOGGER.e(e, "Exception!");
        } catch (final NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.tfe_od_camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            throw new IllegalStateException(getString(R.string.tfe_od_camera_error));
        }

        cameraConnectionCallback.onPreviewSizeChosen(previewSize, sensorOrientation);
    }

    /** Opens the camera specified by {@link CameraConnectionFragment#cameraId}. */
    private void openCamera(final int width, final int height) {
        setUpCameraOutputs();
        configureTransform(width, height);
        final Activity activity = getActivity();
        final CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (final CameraAccessException e) {
            LOGGER.e(e, "Exception!");
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /** Closes the current {@link CameraDevice}. */
    public void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            if (null != captureSession) {
                captureSession.close();
                captureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != previewReader) {
                previewReader.close();
                previewReader = null;
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    /** Starts a background thread and its {@link Handler}. */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("ImageListener");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    /** Stops the background thread and its {@link Handler}. */
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }
    }

//  private final CameraCaptureSession.CaptureCallback captureCallback =
//          new CameraCaptureSession.CaptureCallback() {
//            @Override
//            public void onCaptureProgressed(
//                    final CameraCaptureSession session,
//                    final CaptureRequest request,
//                    final CaptureResult partialResult) {}
//
//            @Override
//            public void onCaptureCompleted(
//                    final CameraCaptureSession session,
//                    final CaptureRequest request,
//                    final TotalCaptureResult result) {}
//          };

    /** Creates a new {@link CameraCaptureSession} for camera preview. */
    private void createCameraPreviewSession() {
        try {
            final SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            // This is the output Surface we need to start preview.
            final Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            LOGGER.i("Opening camera preview: " + previewSize.getWidth() + "x" + previewSize.getHeight());

            // Create the reader for the preview frames.
            previewReader =
                    ImageReader.newInstance(
                            previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 2);

            previewReader.setOnImageAvailableListener(imageListener, backgroundHandler);
            previewRequestBuilder.addTarget(previewReader.getSurface());

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice.createCaptureSession(
                    Arrays.asList(surface, previewReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(final CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == cameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            captureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                // Finally, we start displaying the camera preview.
                                previewRequest = previewRequestBuilder.build();
                                captureSession.setRepeatingRequest(
                                        previewRequest, captureCallback, backgroundHandler);
                            } catch (final CameraAccessException e) {
                                LOGGER.e(e, "Exception!");
                            }
                        }

                        @Override
                        public void onConfigureFailed(final CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    },
                    null);
        } catch (final CameraAccessException e) {
            LOGGER.e(e, "Exception!");
        }
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`. This method should be
     * called after the camera preview size is determined in setUpCameraOutputs and also the size of
     * `mTextureView` is fixed.
     *
     * @param viewWidth The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(final int viewWidth, final int viewHeight) {
        final Activity activity = getActivity();
        if (null == textureView || null == previewSize || null == activity) {
            return;
        }
        final int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        final Matrix matrix = new Matrix();
        final RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        final RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        final float centerX = viewRect.centerX();
        final float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            final float scale =
                    Math.max(
                            (float) viewHeight / previewSize.getHeight(),
                            (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    /**
     * Callback for Activities to use to initialize their data once the selected preview size is
     * known.
     */
    public interface ConnectionCallback {
        void onPreviewSizeChosen(Size size, int cameraRotation);
    }

    /** Compares two {@code Size}s based on their areas. */
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(final Size lhs, final Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /** Shows an error message dialog. */
    public static class ErrorDialog extends DialogFragment {
        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(final String message) {
            final ErrorDialog dialog = new ErrorDialog();
            final Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(
                            android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialogInterface, final int i) {
                                    activity.finish();
                                }
                            })
                    .create();
        }
    }

    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            captureSession.capture(previewRequestBuilder.build(), captureCallback,
                    backgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            captureSession.setRepeatingRequest(previewRequest, captureCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public static final class Zoom
    {
        private static final float DEFAULT_ZOOM_FACTOR = 1.0f;

        @NonNull
        private final Rect mCropRegion = new Rect();

        public final float maxZoom;

        @Nullable
        private final Rect mSensorSize;

        public final boolean hasSupport;

        public Zoom(@NonNull final CameraCharacteristics characteristics)
        {
            this.mSensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            if (this.mSensorSize == null)
            {
                this.maxZoom = Zoom.DEFAULT_ZOOM_FACTOR;
                this.hasSupport = false;
                return;
            }

            final Float value = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);

            this.maxZoom = ((value == null) || (value < Zoom.DEFAULT_ZOOM_FACTOR))
                    ? Zoom.DEFAULT_ZOOM_FACTOR
                    : value;

            this.hasSupport = (Float.compare(this.maxZoom, Zoom.DEFAULT_ZOOM_FACTOR) > 0);
        }

        public void setZoom(@NonNull final CaptureRequest.Builder builder, final float zoom)
        {
            if (!this.hasSupport)
            {
                return;
            }

            final float newZoom = MathUtils.clamp(zoom, Zoom.DEFAULT_ZOOM_FACTOR, this.maxZoom);

            final int centerX = this.mSensorSize.width() / 2;
            final int centerY = this.mSensorSize.height() / 2;
            final int deltaX  = (int)((0.5f * this.mSensorSize.width()) / newZoom);
            final int deltaY  = (int)((0.5f * this.mSensorSize.height()) / newZoom);

            this.mCropRegion.set(centerX - deltaX,
                    centerY - deltaY,
                    centerX + deltaX,
                    centerY + deltaY);

            builder.set(CaptureRequest.SCALER_CROP_REGION, this.mCropRegion);
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            captureSession.capture(previewRequestBuilder.build(), captureCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     */
    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == cameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(previewReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    showToast("Saved: " + mFile);
                    Log.d(TAG, mFile.toString());
                    unlockFocus();
                }
            };

            captureSession.stopRepeating();
            captureSession.abortCaptures();
            captureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}

//@SuppressLint("ValidFragment")
//public class CameraConnectionFragment extends Fragment {
//    private static final Logger LOGGER = new Logger();
//
//    /**
//     * The camera preview size will be chosen to be the smallest frame by pixel size capable of
//     * containing a DESIRED_SIZE x DESIRED_SIZE square.
//     */
//    private static final int MINIMUM_PREVIEW_SIZE = 320;
//
//    /**
//     * Camera state: Showing camera preview.
//     */
//    private static final int STATE_PREVIEW = 0;
//    /**
//     * Camera state: Waiting for the focus to be locked.
//     */
//    private static final int STATE_WAITING_LOCK = 1;
//
//    /**
//     * Camera state: Waiting for the exposure to be precapture state.
//     */
//    private static final int STATE_WAITING_PRECAPTURE = 2;
//
//    /**
//     * Camera state: Waiting for the exposure state to be something other than precapture.
//     */
//    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
//
//    /**
//     * Camera state: Picture was taken.
//     */
//    private static final int STATE_PICTURE_TAKEN = 4;
//
//    private int mState = STATE_PREVIEW;
//    /**
//     * Conversion from screen rotation to JPEG orientation.
//     */
//    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
//    private static final String FRAGMENT_DIALOG = "dialog";
//
//    static {
//        ORIENTATIONS.append(Surface.ROTATION_0, 90);
//        ORIENTATIONS.append(Surface.ROTATION_90, 0);
//        ORIENTATIONS.append(Surface.ROTATION_180, 270);
//        ORIENTATIONS.append(Surface.ROTATION_270, 180);
//    }
//
//    /**
//     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
//     */
//    private final Semaphore cameraOpenCloseLock = new Semaphore(1);
//    /**
//     * A {@link OnImageAvailableListener} to receive frames as they are available.
//     */
//    private final OnImageAvailableListener imageListener;
//    /** The input size in pixels desired by TensorFlow (width and height of a square bitmap). */
//    private final Size inputSize;
//    /**
//     * The layout identifier to inflate for this Fragment.
//     */
//    private final int layout;
//
//
//    /**
//     * {@link android.view.TextureView.SurfaceTextureListener} handles several lifecycle events on a
//     * {@link TextureView}.
//     */
//    private final TextureView.SurfaceTextureListener surfaceTextureListener =
//            new TextureView.SurfaceTextureListener() {
//                @Override
//                public void onSurfaceTextureAvailable(
//                        final SurfaceTexture texture, final int width, final int height) {
//                    openCamera(width, height);
//                }
//
//                @Override
//                public void onSurfaceTextureSizeChanged(
//                        final SurfaceTexture texture, final int width, final int height) {
//                    configureTransform(width, height);
//                }
//
//                @Override
//                public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
//                    return true;
//                }
//
//                @Override
//                public void onSurfaceTextureUpdated(final SurfaceTexture texture) {}
//            };
//
//    /**
//     * Callback for Activities to use to initialize their data once the
//     * selected preview size is known.
//     */
//    public interface ConnectionCallback {
//        void onPreviewSizeChosen(Size size, int cameraRotation);
//    }
//
//    /**
//     * ID of the current {@link CameraDevice}.
//     */
//    private String cameraId;
//
//    /**
//     * An {@link AutoFitTextureView} for camera preview.
//     */
//    private AutoFitTextureView textureView;
//
//    /**
//     * A {@link CameraCaptureSession } for camera preview.
//     */
//    protected CameraCaptureSession captureSession;
//
//    /**
//     * A reference to the opened {@link CameraDevice}.
//     */
//    private CameraDevice cameraDevice;
//
//    /**
//     * The rotation in degrees of the camera sensor from the display.
//     */
//    private Integer sensorOrientation;
//
//    /**
//     * The {@link android.util.Size} of camera preview.
//     */
//    private Size previewSize;
//
//    /**
//     * {@link android.hardware.camera2.CameraDevice.StateCallback}
//     * is called when {@link CameraDevice} changes its state.
//     */
//    private final CameraDevice.StateCallback stateCallback =
//            new CameraDevice.StateCallback() {
//                @Override
//                public void onOpened(final CameraDevice cd) {
//                    // This method is called when the camera is opened.  We start camera preview here.
//                    cameraOpenCloseLock.release();
//                    cameraDevice = cd;
//                    createCameraPreviewSession();
//                }
//
//                @Override
//                public void onDisconnected(final CameraDevice cd) {
//                    cameraOpenCloseLock.release();
//                    cd.close();
//                    cameraDevice = null;
//                }
//
//                @Override
//                public void onError(final CameraDevice cd, final int error) {
//                    cameraOpenCloseLock.release();
//                    cd.close();
//                    cameraDevice = null;
//                    final Activity activity = getActivity();
//                    if (null != activity) {
//                        activity.finish();
//                    }
//                }
//            };
//
//    /**
//     * An additional thread for running tasks that shouldn't block the UI.
//     */
//    private HandlerThread backgroundThread;
//
//    /**
//     * A {@link Handler} for running tasks in the background.
//     */
//    protected Handler backgroundHandler;
//
//    /**
//     * An {@link ImageReader} that handles preview frame capture.
//     */
//    private ImageReader previewReader;
//
//    /**
//     * {@link android.hardware.camera2.CaptureRequest.Builder} for the camera preview
//     */
//    protected CaptureRequest.Builder previewRequestBuilder;
//
//    /**
//     * {@link CaptureRequest} generated by {@link #previewRequestBuilder}
//     */
//    private CaptureRequest previewRequest;
//
//    protected CameraCharacteristics cameraCharacteristics;
//    protected Zoom zoom;
//    private File mFile;
//
//
//    private final ConnectionCallback cameraConnectionCallback;
//
//    private CameraConnectionFragment(
//            final ConnectionCallback connectionCallback,
//            final OnImageAvailableListener imageListener,
//            final int layout,
//            final Size inputSize) {
//        this.cameraConnectionCallback = connectionCallback;
//        this.imageListener = imageListener;
//        this.layout = layout;
//        this.inputSize = inputSize;
//    }
//
//    /**
//     * Shows a {@link Toast} on the UI thread.
//     *
//     * @param text The message to show
//     */
//    private void showToast(final String text) {
//        final Activity activity = getActivity();
//        if (activity != null) {
//            activity.runOnUiThread(
//                    () -> Toast.makeText(activity, text, Toast.LENGTH_SHORT).show());
//        }
//    }
//
//    /**
//     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
//     * width and height are at least as large as the minimum of both, or an exact match if possible.
//     *
//     * @param choices The list of sizes that the camera supports for the intended output class
//     * @param width The minimum desired width
//     * @param height The minimum desired height
//     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
//     */
//    protected static Size chooseOptimalSize(final Size[] choices, final int width, final int height) {
//        final int minSize = Math.max(Math.min(width, height), MINIMUM_PREVIEW_SIZE);
//        final Size desiredSize = new Size(width, height);
//
//        // Collect the supported resolutions that are at least as big as the preview Surface
//        boolean exactSizeFound = false;
//        final List<Size> bigEnough = new ArrayList<Size>();
//        final List<Size> tooSmall = new ArrayList<Size>();
//        for (final Size option : choices) {
//            if (option.equals(desiredSize)) {
//                // Set the size but don't return yet so that remaining sizes will still be logged.
//                exactSizeFound = true;
//            }
//
//            if (option.getHeight() >= minSize && option.getWidth() >= minSize) {
//                bigEnough.add(option);
//            } else {
//                tooSmall.add(option);
//            }
//        }
//
//        LOGGER.i("Desired size: " + desiredSize + ", min size: " + minSize + "x" + minSize);
//        LOGGER.i("Valid preview sizes: [" + TextUtils.join(", ", bigEnough) + "]");
//        LOGGER.i("Rejected preview sizes: [" + TextUtils.join(", ", tooSmall) + "]");
//
//        if (exactSizeFound) {
//            LOGGER.i("Exact size match found.");
//            return desiredSize;
//        }
//
//        // Pick the smallest of those, assuming we found any
//        if (bigEnough.size() > 0) {
//            final Size chosenSize = Collections.min(bigEnough, new CompareSizesByArea());
//            LOGGER.i("Chosen size: " + chosenSize.getWidth() + "x" + chosenSize.getHeight());
//            return chosenSize;
//        } else {
//            LOGGER.e("Couldn't find any suitable preview size");
//            return choices[0];
//        }
//    }
//
//    public static CameraConnectionFragment newInstance(
//            final ConnectionCallback callback,
//            final OnImageAvailableListener imageListener,
//            final int layout,
//            final Size inputSize) {
//        return new CameraConnectionFragment(callback, imageListener, layout, inputSize);
//    }
//
//    @Override
//    public View onCreateView(
//            final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
//        return inflater.inflate(layout, container, false);
//    }
//
//    @Override
//    public void onViewCreated(final View view, final Bundle savedInstanceState) {
//        textureView = view.findViewById(R.id.texture);
//    }
//
//    @Override
//    public void onActivityCreated(final Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        startBackgroundThread();
//
//        // When the screen is turned off and turned back on, the SurfaceTexture is already
//        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
//        // a camera and start preview from here (otherwise, we wait until the surface is ready in
//        // the SurfaceTextureListener).
//        if (textureView.isAvailable()) {
//            openCamera(textureView.getWidth(), textureView.getHeight());
//        }
//        textureView.setSurfaceTextureListener(surfaceTextureListener);
//    }
//
//    @Override
//    public void onPause() {
//        textureView.setSurfaceTextureListener(null);
//        closeCamera();
//        stopBackgroundThread();
//        super.onPause();
//    }
//
//    public void setCamera(String cameraId) {
//        this.cameraId = cameraId;
//    }
//
//    /**
//     * Lock the focus as the first step for a still image capture.
//     */
//    private void lockFocus() {
//        try {
//            // This is how to tell the camera to lock focus.
//            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
//                    CameraMetadata.CONTROL_AF_TRIGGER_START);
//            // Tell #mCaptureCallback to wait for the lock.
//            mState = STATE_WAITING_LOCK;
//            captureSession.capture(previewRequestBuilder.build(), captureCallback,
//                    backgroundHandler);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Sets up member variables related to camera.
//     */
//    private void setUpCameraOutputs() {
//        final Activity activity = getActivity();
//        final CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
//        try {
//            cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
//
//            final StreamConfigurationMap map =
//                    cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//
//            // For still image captures, we use the largest available size.
//            final Size largest =
//                    Collections.max(
//                            Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)),
//                            new CompareSizesByArea());
//
//            sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
//
//            zoom = new Zoom(cameraCharacteristics);
//
//            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
//            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
//            // garbage capture data.
//            previewSize =
//                    chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
//                            inputSize.getWidth(),
//                            inputSize.getHeight());
//
//            // We fit the aspect ratio of TextureView to the size of preview we picked.
//            final int orientation = getResources().getConfiguration().orientation;
//            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
//            } else {
//                textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
//            }
//        } catch (final CameraAccessException e) {
//            LOGGER.e(e, "Exception!");
//        } catch (final NullPointerException e) {
//            // Currently an NPE is thrown when the Camera2API is used but not supported on the
//            // device this code runs.
//            // TODO(andrewharp): abstract ErrorDialog/RuntimeException handling out into new method and
//            // reuse throughout app.
//            ErrorDialog.newInstance(getString(R.string.camera_error))
//                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
//            throw new RuntimeException(getString(R.string.camera_error));
//        }
//
//        cameraConnectionCallback.onPreviewSizeChosen(previewSize, sensorOrientation);
//    }
//
//    /**
//     * Opens the camera specified by {@link CameraConnectionFragment#cameraId}.
//     */
//    private void openCamera(final int width, final int height) {
//        setUpCameraOutputs();
//        configureTransform(width, height);
//        final Activity activity = getActivity();
//        final CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
//        try {
//            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
//                throw new RuntimeException("Time out waiting to lock camera opening.");
//            }
//            manager.openCamera(cameraId, stateCallback, backgroundHandler);
//        } catch (final CameraAccessException e) {
//            LOGGER.e(e, "Exception!");
//        } catch (final InterruptedException e) {
//            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
//        }
//    }
//
//    /**
//     * Closes the current {@link CameraDevice}.
//     */
//    private void closeCamera() {
//        try {
//            cameraOpenCloseLock.acquire();
//            if (null != captureSession) {
//                captureSession.close();
//                captureSession = null;
//            }
//            if (null != cameraDevice) {
//                cameraDevice.close();
//                cameraDevice = null;
//            }
//            if (null != previewReader) {
//                previewReader.close();
//                previewReader = null;
//            }
//        } catch (final InterruptedException e) {
//            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
//        } finally {
//            cameraOpenCloseLock.release();
//        }
//    }
//
//    /**
//     * Starts a background thread and its {@link Handler}.
//     */
//    private void startBackgroundThread() {
//        backgroundThread = new HandlerThread("ImageListener");
//        backgroundThread.start();
//        backgroundHandler = new Handler(backgroundThread.getLooper());
//    }
//
//    /**
//     * Stops the background thread and its {@link Handler}.
//     */
//    private void stopBackgroundThread() {
//        backgroundThread.quitSafely();
//        try {
//            backgroundThread.join();
//            backgroundThread = null;
//            backgroundHandler = null;
//        } catch (final InterruptedException e) {
//            LOGGER.e(e, "Exception!");
//        }
//    }
//
//    protected final CameraCaptureSession.CaptureCallback captureCallback =
//            new CameraCaptureSession.CaptureCallback() {
//
//                private void process(CaptureResult result) {
//                    switch (mState) {
//                        case STATE_PREVIEW: {
//                            // We have nothing to do when the camera preview is working normally.
//                            break;
//                        }
//                        case STATE_WAITING_LOCK: {
//                            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
//                            if (afState == null) {
//                                captureStillPicture();
//                            } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
//                                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
//                                // CONTROL_AE_STATE can be null on some devices
//                                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
//                                if (aeState == null ||
//                                        aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
//                                    mState = STATE_PICTURE_TAKEN;
//                                    captureStillPicture();
//                                } else {
//                                    runPrecaptureSequence();
//                                }
//                            }
//                            break;
//                        }
//                        case STATE_WAITING_PRECAPTURE: {
//                            // CONTROL_AE_STATE can be null on some devices
//                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
//                            if (aeState == null ||
//                                    aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
//                                    aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
//                                mState = STATE_WAITING_NON_PRECAPTURE;
//                            }
//                            break;
//                        }
//                        case STATE_WAITING_NON_PRECAPTURE: {
//                            // CONTROL_AE_STATE can be null on some devices
//                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
//                            if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
//                                mState = STATE_PICTURE_TAKEN;
//                                captureStillPicture();
//                            }
//                            break;
//                        }
//                    }
//                }
//                @Override
//                public void onCaptureProgressed(
//                        final CameraCaptureSession session,
//                        final CaptureRequest request,
//                        final CaptureResult partialResult) {}
//
//                @Override
//                public void onCaptureCompleted(
//                        final CameraCaptureSession session,
//                        final CaptureRequest request,
//                        final TotalCaptureResult result) {}
//            };
//    /**
//     * Creates a new {@link CameraCaptureSession} for camera preview.
//     */
//    private void createCameraPreviewSession() {
//        try {
//            final SurfaceTexture texture = textureView.getSurfaceTexture();
//            assert texture != null;
//
//            // We configure the size of default buffer to be the size of camera preview we want.
//            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
//
//            // This is the output Surface we need to start preview.
//            final Surface surface = new Surface(texture);
//
//            // We set up a CaptureRequest.Builder with the output Surface.
//            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//            previewRequestBuilder.addTarget(surface);
//
//            LOGGER.i("Opening camera preview: " + previewSize.getWidth() + "x" + previewSize.getHeight());
//
//            // Create the reader for the preview frames.
//            previewReader =
//                    ImageReader.newInstance(
//                            previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 2);
//
//            previewReader.setOnImageAvailableListener(imageListener, backgroundHandler);
//            previewRequestBuilder.addTarget(previewReader.getSurface());
//
//            // Here, we create a CameraCaptureSession for camera preview.
//            cameraDevice.createCaptureSession(
//                    Arrays.asList(surface, previewReader.getSurface()),
//                    new CameraCaptureSession.StateCallback() {
//
//                        @Override
//                        public void onConfigured(final CameraCaptureSession cameraCaptureSession) {
//                            // The camera is already closed
//                            if (null == cameraDevice) {
//                                return;
//                            }
//
//                            // When the session is ready, we start displaying the preview.
//                            captureSession = cameraCaptureSession;
//                            try {
//                                // Auto focus should be continuous for camera preview.
//                                previewRequestBuilder.set(
//                                        CaptureRequest.CONTROL_AF_MODE,
//                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                                // Flash is automatically enabled when necessary.
//                                previewRequestBuilder.set(
//                                        CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//
//                                // Finally, we start displaying the camera preview.
//                                previewRequest = previewRequestBuilder.build();
//                                captureSession.setRepeatingRequest(
//                                        previewRequest, captureCallback, backgroundHandler);
//                            } catch (final CameraAccessException e) {
//                                LOGGER.e(e, "Exception!");
//                            }
//                        }
//
//                        @Override
//                        public void onConfigureFailed(final CameraCaptureSession cameraCaptureSession) {
//                            showToast("Failed");
//                        }
//                    },
//                    null);
//        } catch (final CameraAccessException e) {
//            LOGGER.e(e, "Exception!");
//        }
//    }
//
//    /**
//     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
//     * This method should be called after the camera preview size is determined in
//     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
//     *
//     * @param viewWidth  The width of `mTextureView`
//     * @param viewHeight The height of `mTextureView`
//     */
//    private void configureTransform(final int viewWidth, final int viewHeight) {
//        final Activity activity = getActivity();
//        if (null == textureView || null == previewSize || null == activity) {
//            return;
//        }
//        final int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
//        final Matrix matrix = new Matrix();
//        final RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
//        final RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
//        final float centerX = viewRect.centerX();
//        final float centerY = viewRect.centerY();
//        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
//            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
//            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
//            final float scale =
//                    Math.max(
//                            (float) viewHeight / previewSize.getHeight(),
//                            (float) viewWidth / previewSize.getWidth());
//            matrix.postScale(scale, scale, centerX, centerY);
//            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
//        } else if (Surface.ROTATION_180 == rotation) {
//            matrix.postRotate(180, centerX, centerY);
//        }
//        textureView.setTransform(matrix);
//    }
//
//    /**
//     * Compares two {@code Size}s based on their areas.
//     */
//    static class CompareSizesByArea implements Comparator<Size> {
//        @Override
//        public int compare(final Size lhs, final Size rhs) {
//            // We cast here to ensure the multiplications won't overflow
//            return Long.signum(
//                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
//        }
//    }
//
//    /**
//     * Shows an error message dialog.
//     */
//    public static class ErrorDialog extends DialogFragment {
//        private static final String ARG_MESSAGE = "message";
//
//        public static ErrorDialog newInstance(final String message) {
//            final ErrorDialog dialog = new ErrorDialog();
//            final Bundle args = new Bundle();
//            args.putString(ARG_MESSAGE, message);
//            dialog.setArguments(args);
//            return dialog;
//        }
//
//        @Override
//        public Dialog onCreateDialog(final Bundle savedInstanceState) {
//            final Activity activity = getActivity();
//            return new AlertDialog.Builder(activity)
//                    .setMessage(getArguments().getString(ARG_MESSAGE))
//                    .setPositiveButton(
//                            android.R.string.ok,
//                            (dialogInterface, i) -> activity.finish())
//                    .create();
//        }
//    }
//
//    private void unlockFocus() {
//        try {
//            // Reset the auto-focus trigger
//            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
//                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
//            captureSession.capture(previewRequestBuilder.build(), captureCallback,
//                    backgroundHandler);
//            // After this, the camera will go back to the normal state of preview.
//            mState = STATE_PREVIEW;
//            captureSession.setRepeatingRequest(previewRequest, captureCallback,
//                    backgroundHandler);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static final class Zoom
//    {
//        private static final float DEFAULT_ZOOM_FACTOR = 1.0f;
//
//        @NonNull
//        private final Rect mCropRegion = new Rect();
//
//        public final float maxZoom;
//
//        @Nullable
//        private final Rect mSensorSize;
//
//        public final boolean hasSupport;
//
//        public Zoom(@NonNull final CameraCharacteristics characteristics)
//        {
//            this.mSensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
//
//            if (this.mSensorSize == null)
//            {
//                this.maxZoom = Zoom.DEFAULT_ZOOM_FACTOR;
//                this.hasSupport = false;
//                return;
//            }
//
//            final Float value = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
//
//            this.maxZoom = ((value == null) || (value < Zoom.DEFAULT_ZOOM_FACTOR))
//                    ? Zoom.DEFAULT_ZOOM_FACTOR
//                    : value;
//
//            this.hasSupport = (Float.compare(this.maxZoom, Zoom.DEFAULT_ZOOM_FACTOR) > 0);
//        }
//
//        public void setZoom(@NonNull final CaptureRequest.Builder builder, final float zoom)
//        {
//            if (!this.hasSupport)
//            {
//                return;
//            }
//
//            final float newZoom = MathUtils.clamp(zoom, Zoom.DEFAULT_ZOOM_FACTOR, this.maxZoom);
//
//            final int centerX = this.mSensorSize.width() / 2;
//            final int centerY = this.mSensorSize.height() / 2;
//            final int deltaX  = (int)((0.5f * this.mSensorSize.width()) / newZoom);
//            final int deltaY  = (int)((0.5f * this.mSensorSize.height()) / newZoom);
//
//            this.mCropRegion.set(centerX - deltaX,
//                    centerY - deltaY,
//                    centerX + deltaX,
//                    centerY + deltaY);
//
//            builder.set(CaptureRequest.SCALER_CROP_REGION, this.mCropRegion);
//        }
//    }
//
//    /**
//     * Run the precapture sequence for capturing a still image. This method should be called when
//     */
//    private void runPrecaptureSequence() {
//        try {
//            // This is how to tell the camera to trigger.
//            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
//                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
//            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
//            mState = STATE_WAITING_PRECAPTURE;
//            captureSession.capture(previewRequestBuilder.build(), captureCallback,
//                    backgroundHandler);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Capture a still picture. This method should be called when we get a response in
//     */
//    private void captureStillPicture() {
//        try {
//            final Activity activity = getActivity();
//            if (null == activity || null == cameraDevice) {
//                return;
//            }
//            // This is the CaptureRequest.Builder that we use to take a picture.
//            final CaptureRequest.Builder captureBuilder =
//                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            captureBuilder.addTarget(previewReader.getSurface());
//
//            // Use the same AE and AF modes as the preview.
//            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//
//            // Orientation
//            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
//            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
//
//            CameraCaptureSession.CaptureCallback CaptureCallback
//                    = new CameraCaptureSession.CaptureCallback() {
//
//                @Override
//                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
//                                               @NonNull CaptureRequest request,
//                                               @NonNull TotalCaptureResult result) {
//                    showToast("Saved: " + mFile);
//                    Log.d(TAG, mFile.toString());
//                    unlockFocus();
//                }
//            };
//
//            captureSession.stopRepeating();
//            captureSession.abortCaptures();
//            captureSession.capture(captureBuilder.build(), CaptureCallback, null);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//}
