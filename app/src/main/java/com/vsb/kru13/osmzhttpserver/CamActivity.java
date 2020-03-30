package com.vsb.kru13.osmzhttpserver;


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.TimerTask;

public class CamActivity extends Activity {

    private static byte[] imageData;
    private Camera mCamera;
    private FrameLayout frame;
    private CameraPreview preview;
    private Button captureImage;
    private boolean isSafe;

    public static byte[] getImageData(){
        return imageData;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam);
        frame = (FrameLayout)findViewById(R.id.preview);
        captureImage = (Button)findViewById(R.id.camera);
        isSafe = true;
        if(isReady(this)){
            Toast.makeText(this,"You can use camera", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this,"Camera is not prepared", Toast.LENGTH_LONG).show();
        }
        mCamera = Camera.open();
        preview = new CameraPreview(this, mCamera, isSafe);

        captureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timerTask.run();
            }
        });

        frame.addView(preview);


    }


    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            preview.getSafe();
            if (isSafe) {
                mCamera.takePicture(null, null, mPictureCallback);
                (new Handler()).postDelayed(this, 3000);
                isSafe = false;
            }
        }
    };

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            imageData = data;
        }
    };

    private boolean isReady(Context context) {
        return (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA));
    }
}

