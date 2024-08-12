package com.metrix.architecture.utilities;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.ZoomControls;

public class PhotoCameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private Activity mActivity;
	private ZoomControls mZoomControls;
	private ImageView mImageViewFocusedIndicator;
	
	public PhotoCameraPreview(Context context){
		super(context);
	}
	
	@SuppressWarnings("deprecation")
	public PhotoCameraPreview(Context context, ZoomControls zoomControls, ImageView imageViewFocusedIndicator, Camera camera, Activity activity) {
		super(context);
		
		mCamera = camera;
		mActivity = activity;
		mZoomControls = zoomControls;
		mImageViewFocusedIndicator = imageViewFocusedIndicator;
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
        	LogManager.getInstance().error(e);
        }

	}

	public void surfaceDestroyed(SurfaceHolder holder) {
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

		try{
			
			if (mHolder.getSurface() == null){
				return;
		    }

			mCamera.stopPreview();
			
			boolean isPortrait = MetrixCameraHelper.isPortrait(mActivity);
			MetrixCameraHelper.configureCamera(mActivity, mZoomControls, null, mCamera, isPortrait, width, height);
						
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();
			
			Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {

                    if(success){
                    	try {
							MetrixCameraHelper.playFocusedSoundWithIndicator(mActivity.getApplicationContext(), mImageViewFocusedIndicator);
						} catch (Exception e) {
							LogManager.getInstance().error(e);
						}
                    }
                }
            };
            
            mCamera.autoFocus(autoFocusCallback);

		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

}
