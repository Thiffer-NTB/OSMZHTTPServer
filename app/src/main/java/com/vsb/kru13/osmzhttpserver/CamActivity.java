package com.vsb.kru13.osmzhttpserver;


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.TimerTask;

public class CamActivity extends Activity {

    private static byte[] imageData;
    private Camera mCamera;
    private FrameLayout frame;
    private CameraPreview preview;
    private boolean isSafe;
    private static boolean isStreaming;

    public static byte[] getImageData(){
        return imageData;
    }

    public static boolean isStreaming() {
        return isStreaming;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam);
        frame = (FrameLayout)findViewById(R.id.preview);
        isSafe = true;
        if(isReady(this)){
            Toast.makeText(this,"You can use camera", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this,"Camera is not prepared", Toast.LENGTH_LONG).show();
        }
        mCamera = Camera.open();
        preview = new CameraPreview(this, mCamera, isSafe);

        frame.addView(preview);
        Button startStream = (Button)findViewById(R.id.stream);
        Button stopStream = (Button)findViewById(R.id.streamst);
        Button startcamera = (Button)findViewById(R.id.camera);
        Button stopCamera = (Button)findViewById(R.id.stcamera);

        startcamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer.run();
            }
        });

        stopCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.release();
            }
        });

        startStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isStreaming = true;
                timerTask.run();
            }
        });

        stopStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isStreaming = false;
                mCamera.release();
            }
        });


    }
    //TimerTask pro /camera/snapshot - reloading fotky
    TimerTask timer = new TimerTask() {
        @Override
        public void run() {
            if (isSafe) {
                mCamera.takePicture(null, null, mPictureCallback);
                (new Handler()).postDelayed(this, 3000);
                isSafe = false;
            }
        }
    };
    //TimerTask pro streaming /camera/stream
    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            if(preview.getSafe()){
                mCamera.setPreviewCallback(previewCallback);
            }
        }
    };

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            imageData = data;
        }
    };


    public static byte[] convertToImg(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();

        YuvImage img = new YuvImage(data, parameters.getPreviewFormat(), size.width, size.height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        img.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, out);
        return out.toByteArray();
    }

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            imageData = convertToImg(data, camera);
            try {
                mCamera.startPreview();
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            isSafe = true;
        }
    };

    private boolean isReady(Context context) {
        return (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA));
    }
}

