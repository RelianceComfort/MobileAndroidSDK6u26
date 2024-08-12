package com.metrix.metrixmobile.system;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.metrix.metrixmobile.R; 
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CameraPictures extends Activity implements SurfaceHolder.Callback, 
		OnClickListener {
	static final int PHOTO_MODE = 0;
	private static final String TAG = "Camera";
	Camera mCamera;
	boolean mPreviewRunning = false;
	private Context mContext = this;
	private Button mShutter; 
	
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		LogManager.getInstance(this).debug(TAG + " onCreate");

		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.surface_camera);
		mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);
		//mSurfaceView.setOnClickListener(this);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mShutter = (Button)findViewById(R.id.takepicture);
		mShutter.setOnClickListener(this);
		mShutter.setEnabled(false);
	}
	
	public void onStart() {
		super.onStart();
		
		LogManager.getInstance(this).info("{0} onStart()", this.getLocalClassName());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
		public void onPictureTaken(byte[] imageData, Camera c) {

			if (imageData != null) {
				String time_stamp = MetrixDateTimeHelper.getCurrentDate("yyyyMMddHHmmss");
				String fileName = getIntent().getExtras().getString("ID")+"_"+time_stamp;
				
				String filePath = StoreByteImage(mContext, imageData, 50,
						fileName);
				mCamera.startPreview();

				getIntent().putExtra("IMAGE_PATH", filePath);
				setResult(RESULT_OK, getIntent());
				finish();

			}
		}
	};
	
	Camera.AutoFocusCallback mAutofocusCallback = new Camera.AutoFocusCallback() {
		
		@Override
		public void onAutoFocus(boolean arg0, Camera arg1) {
			mShutter.setEnabled(true);
		}
	};

	protected void onResume() {
		LogManager.getInstance(this).debug(TAG + " onResume");
		super.onResume();
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	protected void onStop() {
		LogManager.getInstance(this).debug(TAG + " onStop");
		super.onStop();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		LogManager.getInstance(this).debug(TAG + " surfaceCreated");
		mCamera = Camera.open();		
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		LogManager.getInstance(this).debug(TAG + " surfaceChanged");

		// XXX stopPreview() will crash if preview is not running
		if (mPreviewRunning) {
			mCamera.stopPreview();
		}

		Camera.Parameters p = mCamera.getParameters();
		List<Size> previewSizeList = p.getSupportedPreviewSizes();
	
		Size previewSize = getOptimalPreviewSize(previewSizeList, w, h);
		
		p.setPreviewSize(previewSize.width, previewSize.height);
		//p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		mCamera.setParameters(p);		
		mCamera.autoFocus(mAutofocusCallback);
		try {
			mCamera.setPreviewDisplay(holder);
		} catch (IOException e) {
			LogManager.getInstance(this).error(e);
		}
		mCamera.startPreview();
		mPreviewRunning = true;
	}

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }	
	
	public void surfaceDestroyed(SurfaceHolder holder) {
		LogManager.getInstance(this).debug(TAG + " surfaceDestroyed");
		mCamera.stopPreview();
		mPreviewRunning = false;
		mCamera.release();
	}

	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;

	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.takepicture:
			mCamera.takePicture(null, mPictureCallback, mPictureCallback);
			mShutter.setEnabled(false);
			break;
		default:
			break;
		}		
	}
	
	public static String StoreByteImage(Context mContext, byte[] imageData,
			int quality, String expName) {

		File sdImageMainDirectory = new File(mContext.getExternalFilesDir(null).getPath());
		FileOutputStream fileOutputStream = null;
		String nameFile="";
		try {

			BitmapFactory.Options options=new BitmapFactory.Options();
			options.inSampleSize = 5;
			
			Bitmap myImage = BitmapFactory.decodeByteArray(imageData, 0,
					imageData.length,options);

			
			fileOutputStream = new FileOutputStream(
					sdImageMainDirectory.toString() +"/"+expName+".jpg");
							
			nameFile = sdImageMainDirectory.toString() +"/"+expName+".jpg";
			BufferedOutputStream bos = new BufferedOutputStream(
					fileOutputStream);

			myImage.compress(CompressFormat.JPEG, quality, bos);

			bos.flush();
			bos.close();

		} catch (FileNotFoundException e) {
			LogManager.getInstance().error(e);
		} catch (IOException e) {
			LogManager.getInstance().error(e);
		}

		return nameFile;
	}

}