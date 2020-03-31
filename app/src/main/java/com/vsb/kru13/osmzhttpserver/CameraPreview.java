package com.vsb.kru13.osmzhttpserver;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private boolean isSafe = false;


    public CameraPreview(Context context, Camera camera, Boolean safe){
        super(context);
        mCamera = camera;
        isSafe = safe;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public boolean getSafe(){
        return isSafe;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Camera.Parameters param = mCamera.getParameters();
        if(this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
            param.set("orientation", "portrait");
            mCamera.setDisplayOrientation(90);
            param.setRotation(90);
        }else{
            param.set("orientation", "landscape");
            mCamera.setDisplayOrientation(0);
            param.setRotation(0);
        }
        mCamera.setParameters(param);
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("CamPreview", "Error setting camera preview: " + e.getMessage());
        }


    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(width, height);
        mCamera.startPreview();
        isSafe = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera = null;
    }

}