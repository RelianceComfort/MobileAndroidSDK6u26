package com.metrix.metrixmobile;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCameraHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.PhotoCameraPreview;
import com.metrix.metrixmobile.system.MetrixActivity;

public class MetrixPhotoCaptureActivity extends MetrixActivity implements SensorEventListener  {

	private Camera mCamera;
	private PhotoCameraPreview mPreview;
	private static String mPhotoFilePath;
	private Activity mActivity;
	private ZoomControls mZoomControls;
	private ImageView mImageViewFocusedIndicator;
	private static Class<?> postPhotoVideoCaptureActivty;
	
	private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float mPreviousX, mPreviousY, mPreviousZ;
    
    private static boolean isFocused;
    private static boolean isCaptured;   
    private static ProgressDialog aProgressDialog;

	/** Called when the activity is first created. */
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.metrix_photo_capture);

		try {
			
			mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
			mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			if (mAccelerometer == null){
				MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout,AndroidResourceHelper.getMessage("NoProximitySensorFound"));
			}

			mActivity = this;
			postPhotoVideoCaptureActivty = Class.forName("com.metrix.metrixmobile.DebriefTaskAttachmentAdd");
			
			Bundle extras = getIntent().getExtras();
			if (extras != null){
				mPhotoFilePath = extras.getString("PhotoPath");
			}

			mZoomControls = (ZoomControls) findViewById(R.id.zoomControls);
			mImageViewFocusedIndicator = (ImageView) findViewById(R.id.imageViewCameraFocued);
			
			if(mCamera == null)
				mCamera = MetrixCameraHelper.getCameraInstance();

			mPreview = new PhotoCameraPreview(getApplicationContext(), mZoomControls, mImageViewFocusedIndicator, mCamera, this);
			FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
			preview.addView(mPreview);

			Button captureButton = (Button) findViewById(R.id.buttonPhotoCapture);
			captureButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
						Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
							@Override
							public void onAutoFocus(boolean success, Camera camera) {
								
								if (success) {
									try {
										aProgressDialog = ProgressDialog.show(mActivity, AndroidResourceHelper.getMessage("Wait"), AndroidResourceHelper.getMessage("Processing"));
										mCamera.takePicture(mShutterCallback, null, mPicture);
									} catch (Exception e) {
										LogManager.getInstance().error(e);
									}
								}
							}
						};
						mCamera.autoFocus(autoFocusCallback);
				}
			});
			
			mPreview.setOnTouchListener(new OnTouchListener() {
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					
					Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
		                @Override
		                public void onAutoFocus(boolean success, Camera camera) {

		                    if(success){
		                    	try {
									MetrixCameraHelper.playFocusedSoundWithIndicator(getApplicationContext(), mImageViewFocusedIndicator);
								} catch (Exception e) {
									LogManager.getInstance().error(e);
								}
		                    }
		  
		                }
		            };
		            
	            	mCamera.autoFocus(autoFocusCallback);
					return false;
				}
			});

		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private PictureCallback mPicture = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			
			try {
				isCaptured = true;
				FileSaveTask fileSaveTask = new FileSaveTask(data);
				fileSaveTask.execute();
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
			finally{
				camera.release();
			}
		}
	};
	
	private android.hardware.Camera.ShutterCallback mShutterCallback = new ShutterCallback(){

		@Override
		public void onShutter() {
			try {
				MetrixCameraHelper.playShutterClickSound(getApplicationContext());
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		}
		
	};
	
	private class FileSaveTask extends AsyncTask<Void, Void, String> {
		
		File aImageFile;
		byte[] aPictureData;
		
		
		public FileSaveTask(byte[] pictureData){
			aPictureData = pictureData;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			try {
				aImageFile = MetrixCameraHelper.getOutputMediaFile(MetrixCameraHelper.getMediaTypeImage(), mPhotoFilePath);
			} catch (Exception e) {
				if(aProgressDialog != null && aProgressDialog.isShowing())
					aProgressDialog.dismiss();
				LogManager.getInstance().error(e);
			}

		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				return MetrixCameraHelper.onBeforeSave(mActivity, aPictureData, aImageFile);
			} catch (Exception e) {
				if(aProgressDialog != null && aProgressDialog.isShowing())
					aProgressDialog.dismiss();
				LogManager.getInstance().error(e);
			}
			return null;
		}
	
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			try {
							
				if(!MetrixStringHelper.isNullOrEmpty(result)){
					if (result.compareToIgnoreCase("completed") == 0) {						
						Uri	mMediaUri = MetrixCameraHelper.getOutputMediaFileUri(mActivity, MetrixCameraHelper.getMediaTypeImage(), mPhotoFilePath);
						if(mMediaUri != null){
							Intent intent = MetrixActivityHelper.createActivityIntent(mActivity, postPhotoVideoCaptureActivty);
							intent.putExtra("ImageUri", mMediaUri);
							intent.putExtra("FromCamera", true);
							MetrixActivityHelper.startNewActivityAndFinish(mActivity, intent);
						}
					}
				}
				else
					MetrixUIHelper.showSnackbar(MetrixPhotoCaptureActivity.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("GeneralErrorOccurred"));
			
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
			finally{
				if(aProgressDialog != null && aProgressDialog.isShowing())
					aProgressDialog.dismiss();
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	@Override
	protected void onPause() {
		
		try{
			super.onPause();
			mSensorManager.unregisterListener(this);
			if (mCamera != null) {
		        
		        mCamera.release();
		        mCamera = null;
		    }
		}
		catch(Exception e){
			LogManager.getInstance(this).error(e);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {	
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		try{
		
			if(!isCaptured){
					
				if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
					
					float currentX = (float) Math.abs((Math.round(event.values[0] * 100.0 ) / 100.0));        
			        float currentXDifference = mPreviousX - currentX;
			        currentXDifference = Math.abs(currentXDifference);
			        
			        float currentY = (float) Math.abs((Math.round(event.values[1] * 100.0 ) / 100.0));        
			        float currentYDifference = mPreviousY - currentY;
			        currentYDifference = Math.abs(currentYDifference);
			        
			        float currentZ = (float) Math.abs((Math.round(event.values[2] * 100.0 ) / 100.0));        
			        float currentZDifference = mPreviousZ - currentZ;
			        currentZDifference = Math.abs(currentZDifference);
			        
			        if(mPreviousX != 0){
			        	
			        	if(((currentXDifference > 0.1) && (currentXDifference < 0.5)) && ((currentYDifference > 0.1) && (currentYDifference < 0.5)) && ((currentZDifference > 0.1) && (currentZDifference < 0.5))){
			        			     
			        		isFocused = false;
			        		
			        		Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
				                @Override
				                public void onAutoFocus(boolean success, Camera camera) {
				                	try {
					                    if(success){
					                    	if(!isFocused){
												MetrixCameraHelper.playFocusedSoundWithIndicator(getApplicationContext(), mImageViewFocusedIndicator);
						                    	isFocused = success;
					                    	}
					                    }
				                	} catch (Exception e) {
										LogManager.getInstance().error(e);
									}
				                }
				            };
				            
				            if(mCamera != null)
				            	mCamera.autoFocus(autoFocusCallback);
			        	}
			        }     	
			        mPreviousX = (float) Math.abs((Math.round( currentX * 100.0 ) / 100.0));
			        mPreviousY = (float) Math.abs((Math.round( currentY * 100.0 ) / 100.0));
			        mPreviousZ = (float) Math.abs((Math.round( currentZ * 100.0 ) / 100.0));
				  }
			}
		}
		catch(Exception e){
			LogManager.getInstance(this).error(e);
		}
	}
}
