package com.example.phodo;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.FileCallback;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.size.AspectRatio;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class PicturePreviewActivity extends AppCompatActivity {

    private static PictureResult picture;

    public static void setPictureResult(@Nullable PictureResult pictureResult) {
        picture = pictureResult;
    }

    String[] permission_list = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);
        final PictureResult result = picture;
        if (result == null) {
            finish();
            return;
        }

        final ImageView imageView = findViewById(R.id.image);
        final MessageView captureResolution = findViewById(R.id.nativeCaptureResolution);
        final MessageView captureLatency = findViewById(R.id.captureLatency);
        final MessageView exifRotation = findViewById(R.id.exifRotation);

        final long delay = getIntent().getLongExtra("delay", 0);
        AspectRatio ratio = AspectRatio.of(result.getSize());
        captureLatency.setTitleAndMessage("Approx. latency", delay + " milliseconds");
        captureResolution.setTitleAndMessage("Resolution", result.getSize() + " (" + ratio + ")");
        exifRotation.setTitleAndMessage("EXIF rotation", result.getRotation() + "");
        try {
            result.toBitmap(1000, 1000, new BitmapCallback() {
                @Override
                public void onBitmapReady(Bitmap bitmap) {
                    imageView.setImageBitmap(bitmap);
                }
            });
        } catch (UnsupportedOperationException e) {
            imageView.setImageDrawable(new ColorDrawable(Color.GREEN));
            Toast.makeText(this, "Can't preview this format: " + picture.getFormat(),
                    Toast.LENGTH_LONG).show();
        }

        if (result.isSnapshot()) {
            // Log the real size for debugging reason.
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(result.getData(), 0, result.getData().length, options);
            if (result.getRotation() % 180 != 0) {
                Log.e("PicturePreview", "The picture full size is " + result.getSize().getHeight() + "x" + result.getSize().getWidth());
            } else {
                Log.e("PicturePreview", "The picture full size is " + result.getSize().getWidth() + "x" + result.getSize().getHeight());
            }
        }

        checkPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            setPictureResult(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.share, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.share) {
            Toast.makeText(this, "Sharing...", Toast.LENGTH_SHORT).show();
            String extension;
            switch (picture.getFormat()) {
                case JPEG: extension = "jpg"; break;
                case DNG: extension = "dng"; break;
                default: throw new RuntimeException("Unknown format.");
            }
            File file = new File(getFilesDir(), "picture." + extension);
            CameraUtils.writeToFile(picture.getData(), file, new FileCallback() {
                @Override
                public void onFileReady(@Nullable File file) {
                    if (file != null) {
                        Context context = PicturePreviewActivity.this;
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("image/*");
                        Uri uri = FileProvider.getUriForFile(context,
                                context.getPackageName() + ".provider",
                                file);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } else {
                        Toast.makeText(PicturePreviewActivity.this,
                                "Error while writing file.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return true;
        } else if(item.getItemId() == R.id.save) {
            Toast.makeText(this, "Saving Photo...", Toast.LENGTH_SHORT).show();
            String extension;
            switch (picture.getFormat()) {
                case JPEG: extension = "jpg"; break;
                case DNG: extension = "dng"; break;
                default: throw new RuntimeException("Unknown format.");
            }
            File file = new File(getFilesDir(), "picture." + extension);
            CameraUtils.writeToFile(picture.getData(), file, new FileCallback() {
                @Override
                public void onFileReady(@Nullable File file) {
                    if(file != null) {
                        //Saving Photo!!
                       saveFile(file);
                    }else {
                        Toast.makeText(PicturePreviewActivity.this,
                                "Error while writing file.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

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

    public void checkPermission(){
        //현재 안드로이드 버전이 6.0미만이면 메서드를 종료한다.
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;

        for(String permission : permission_list){
            //권한 허용 여부를 확인한다.
            int chk = checkCallingOrSelfPermission(permission);

            if(chk == PackageManager.PERMISSION_DENIED){
                //권한 허용을여부를 확인하는 창을 띄운다
                requestPermissions(permission_list,0);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==0)
        {
            for(int i=0; i<grantResults.length; i++)
            {
                //허용됬다면
                if(grantResults[i]==PackageManager.PERMISSION_GRANTED){
                }
                else {
                    Toast.makeText(getApplicationContext(),"앱 권한 설정하세요",Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }
}
