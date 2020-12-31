package com.example.phodo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

//TODO: 구현은 완료.. 부분적인 GUI 및 크기 조정!
public class AlbumPreviewActivity extends AppCompatActivity {

    private VideoView videoView;
    private ImageView imageView;

    private static final int REQUEST_TAKE_ALBUM = 2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_album_preview);

        imageView = findViewById(R.id.image_album);
        videoView = findViewById(R.id.video_album);

        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/* video/*");
        startActivityForResult(pickIntent, REQUEST_TAKE_ALBUM);

        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playVideo();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri selectedMediaUri = data.getData();
            if (selectedMediaUri.toString().contains("image")) {
                //handle image
                //TODO: 이미지를 불러올 때 가로로 찍힌 이미지가 눕혀서 보여짐 => 돌려서 보여주기.
                imageView.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.INVISIBLE);
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedMediaUri);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (selectedMediaUri.toString().contains("video")) {
                //handle video
                videoView.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.INVISIBLE);

                MediaController controller = new MediaController(this);
                controller.setAnchorView(videoView);
                controller.setMediaPlayer(videoView);
                videoView.setMediaController(controller);
                videoView.setVideoURI(selectedMediaUri);
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        ViewGroup.LayoutParams lp = videoView.getLayoutParams();
                        float videoWidth = mp.getVideoWidth();
                        float videoHeight = mp.getVideoHeight();
                        float viewWidth = videoView.getWidth();
                        lp.height = (int) (viewWidth * (videoHeight / videoWidth));
                        videoView.setLayoutParams(lp);
                        playVideo();
                    }
                });
            }
        }
    }

    void playVideo() {
        if (!videoView.isPlaying()) {
            videoView.start();
        }
    }
}
