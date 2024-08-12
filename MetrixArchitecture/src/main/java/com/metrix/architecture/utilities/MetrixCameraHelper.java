package com.metrix.architecture.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.Media;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.ZoomControls;

@SuppressWarnings("unchecked")
public class MetrixCameraHelper {
	   
    private static final int MEDIA_TYPE_IMAGE = 1;
	private static final int MEDIA_TYPE_VIDEO = 2;

	private static int mCurrentZoomLevel = 0;
	
	private static SoundPool mSoundPool;
	private static int shutterClickSoundId = -1;
	private static int autoFocusedSoundId = -1;
	private static AlphaAnimation mAlphaAnimation;
	private static HashMap<String, Integer> resourceCache;
	
	static {
		resourceCache = (HashMap<String, Integer>) MetrixPublicCache.instance.getItem("MetrixCameraResources");
		mSoundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
	}
	  
	/**
	 * Checking whether the phone is on portrait mode
	 * @param activity
	 * @return true/false
	 */
	public static boolean isPortrait(Activity activity) throws Exception {
		return (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
	}

	/**
	 * Get the best preview size depend upon the given height & width
	 * @param parameters
	 * @param width
	 * @param height
	 * @return Size
	 */
	public static Size getBestPreviewSize(Parameters parameters, int width, int height) throws Exception {

		Size result = null;

		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea > resultArea) {
						result = size;
					}
				}
			}
		}
		return result;

	}
	
	/**
	 * Check camera is available
	 * @param context
	 * @return true/false
	 */
	public static boolean isCameraAvailable(Context context) throws Exception {
		
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get access to Camera object
	 * @return Camera
	 */
	public static Camera getCameraInstance() throws Exception {
		
		Camera camera = Camera.open();
		return camera;
	}
	
	/**
	 * Get access to MediaRecorder object
	 * @return Camera
	 */
	public static MediaRecorder getMediaRecorderInstance() throws Exception {
		
		MediaRecorder mediaRecorder = new MediaRecorder();
		return mediaRecorder;
	}

	/**
	 * Getting the Uri for the given image/video file
	 * @param activity
	 * @param type
	 * @param filePath
	 * @return
	 */
	public static Uri getOutputMediaFileUri(Activity activity, int type, String filePath) throws Exception {

		ContentValues values = new ContentValues();
		
		if (type == MEDIA_TYPE_VIDEO) {
			
			values.put(Media.TITLE, "Video");
			values.put(Video.Media.BUCKET_ID, filePath.hashCode());
			values.put(Video.Media.MIME_TYPE, "video/mp4");
			values.put(Video.Media.DESCRIPTION, AndroidResourceHelper.getMessage("CameraVideoCapture"));

			values.put("_data", filePath);

			return activity.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
		}
		else if(type == MEDIA_TYPE_IMAGE){

			values.put(Media.TITLE, "Image");
			values.put(Images.Media.BUCKET_ID, filePath.hashCode());
			values.put(Images.Media.MIME_TYPE, "image/jpeg");
			values.put(MediaStore.Images.Media.DESCRIPTION, AndroidResourceHelper.getMessage("CameraImageCapture"));
			values.put("_data", filePath);

			return activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		}
		return null;

	}

	/**
	 * Creating a file object of given type & path
	 * @param type
	 * @param filePath
	 * @return File
	 */
	public static File getOutputMediaFile(int type, String filePath) throws Exception {
		
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE)
			mediaFile = new File(filePath);
		else if (type == MEDIA_TYPE_VIDEO)
			mediaFile = new File(filePath);
		else
			return null;
		
		return mediaFile;
	}
	
	/**
	 * Configuring the camera object
	 * @param activity
	 * @param zoomControls
	 * @param mediaRecorder
	 * @param camera
	 * @param portrait
	 * @param width
	 * @param height
	 */
	public static void configureCamera(Activity activity, ZoomControls zoomControls, MediaRecorder mediaRecorder, final Camera camera, boolean portrait, int width, int height) throws Exception {

		if(camera == null)
			return;
		
		final Parameters parameters  = camera.getParameters();		
		
		int angle;
		Display display = activity.getWindowManager().getDefaultDisplay();
		switch (display.getRotation()) {
		case Surface.ROTATION_0:
			angle = 90;
			break;
		case Surface.ROTATION_90:
			angle = 0;
			break;
		case Surface.ROTATION_180:
			angle = 270;
			break;
		case Surface.ROTATION_270:
			angle = 180;
			break;
		default:
			angle = 90;
			break;
		}

		camera.setDisplayOrientation(angle);
		if (mediaRecorder != null){
			parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			mediaRecorder.setOrientationHint(angle);	
		}else
			parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);

		Size bestPreviewSize = getBestPreviewSize(parameters, width, height);
		
		if (parameters.isZoomSupported()) {
			
			final int maxZoomLevel = parameters.getMaxZoom();
			zoomControls.setIsZoomInEnabled(true);
			zoomControls.setIsZoomOutEnabled(true);

			zoomControls.setOnZoomInClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (mCurrentZoomLevel < maxZoomLevel) {
						mCurrentZoomLevel++;
						camera.startSmoothZoom(mCurrentZoomLevel);
						parameters.setZoom(mCurrentZoomLevel);
						camera.setParameters(parameters);
					}
				}
			});

			zoomControls.setOnZoomOutClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (mCurrentZoomLevel > 0) {
						mCurrentZoomLevel--;
						camera.startSmoothZoom(mCurrentZoomLevel);
						parameters.setZoom(mCurrentZoomLevel);
						camera.setParameters(parameters);
					}
				}
			});
		} else
			zoomControls.setVisibility(View.GONE);

		parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
		camera.setParameters(parameters);

	}
	
	/**
	 * Playing shutter click sound
	 * @param context
	 */
	public static void playShutterClickSound(Context context) throws Exception {
			
		if(mSoundPool == null)
			mSoundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
		else
			if(shutterClickSoundId == -1){
				int soundResourceId = resourceCache.get("R.raw.camera_shutter_click");
				shutterClickSoundId = mSoundPool.load(context, soundResourceId, 0);
			}
		mSoundPool.play(shutterClickSoundId, 1f, 1f, 0, 0, 1);
	}
	
	/**
	 * Play a sound and display icon when focused
	 * @param context
	 * @param imageViewFoucsedIndicator
	 */
	public static void playFocusedSoundWithIndicator(Context context, final ImageView imageViewFoucsedIndicator) throws Exception {
					
		imageViewFoucsedIndicator.setVisibility(View.VISIBLE);
		
		if(mSoundPool == null)
			mSoundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
		else
			if(autoFocusedSoundId == -1){
				int soundResourceId = resourceCache.get("R.raw.camera_focused");
				autoFocusedSoundId = mSoundPool.load(context, soundResourceId, 0);
			}
		mSoundPool.play(autoFocusedSoundId, 1f, 1f, 0, 0, 1);
		
		if(mAlphaAnimation == null){
			mAlphaAnimation = new AlphaAnimation(1.00f, 0.00f);
			mAlphaAnimation.setDuration(1000);
		}
		
		mAlphaAnimation.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation){
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {	
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {	
				imageViewFoucsedIndicator.setVisibility(View.GONE);
			}
		});
		
		imageViewFoucsedIndicator.startAnimation(mAlphaAnimation);		
	}
	
	/**
	 * 
	 * @return MEDIA_TYPE_IMAGE
	 */
	public static int getMediaTypeImage() {
		return MEDIA_TYPE_IMAGE;
	}

	/**
	 * 
	 * @return MEDIA_TYPE_VIDEO
	 */
	public static int getMediaTypeVideo() {
		return MEDIA_TYPE_VIDEO;
	}
	
	/**
	 * Rotating the image before saving(since there is a problem with orientation when it comes to capture images)
	 * @param activity
	 * @param pictureData
	 * @param imageFile
	 * @return status
	 */
	public static String onBeforeSave(Activity activity, byte[] pictureData, File imageFile) {
		
		Bitmap rotateBitmap = null;
		Bitmap tempBitmap = null;
		FileOutputStream fileOutputStream = null;
		
		try {

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 2;
			options.inDither = false;
			options.inPurgeable = true;
			options.inInputShareable = true;
			options.inPreferredConfig = Bitmap.Config.ALPHA_8;

			tempBitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length, options);

			int orientation = 0;

			Display display = activity.getWindowManager().getDefaultDisplay();
			int rotation = display.getRotation();

			if (rotation == 0)
				orientation = 90;
			else if (rotation == 1)
				orientation = 0;
			
			if (orientation != 0) {
				Matrix matrix = new Matrix();
				matrix.postRotate(orientation);
				rotateBitmap = Bitmap.createBitmap(tempBitmap, 0, 0, tempBitmap.getWidth(), tempBitmap.getHeight(), matrix, true);
			} else
				rotateBitmap = Bitmap.createScaledBitmap(tempBitmap, tempBitmap.getWidth(), tempBitmap.getHeight(), true);

			fileOutputStream = new FileOutputStream(imageFile);
			rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream);

		} catch (Exception e) {
			LogManager.getInstance().error(e);	
		} finally {

			if (rotateBitmap != null) {
				rotateBitmap.recycle();
				rotateBitmap = null;
			}
			if (tempBitmap != null) {
				tempBitmap.recycle();
				tempBitmap = null;
			}
			if(fileOutputStream != null){
				try {
					if(fileOutputStream != null){
						fileOutputStream.close();
						fileOutputStream = null;
					}
				} catch (IOException e) {
					LogManager.getInstance().error(e);
				}
			}
		}
		
		return "completed";
	}
}