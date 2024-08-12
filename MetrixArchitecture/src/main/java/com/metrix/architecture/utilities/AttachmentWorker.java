package com.metrix.architecture.utilities;

import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import androidx.collection.LruCache;
import android.widget.ImageView;

public class AttachmentWorker extends AsyncTask<Integer, Void, Integer> {
	
	private Context mContext;
	private String mFilePath;
	private ImageView mImageView;
	private LruCache<String, Bitmap> mMemoryCache;
	private String mKey;
	private int mRequiredHeight;
	private int mRequiredWidth;
	private HashMap<String, Object> mResourceCache;
	private Bitmap imageBitmap;
	
	public AttachmentWorker(Context context, String filePath, ImageView imageView, int requiredHeight, int requiredWidth, String key, LruCache<String, Bitmap> memoryCache, HashMap<String, Object> resourceCache){
		mContext = context;
		mFilePath = filePath;
		mImageView = imageView;
		mRequiredHeight = requiredHeight;
		mRequiredWidth = requiredWidth;
		mKey = key;
		mMemoryCache = memoryCache;	
		mResourceCache = resourceCache;
	}

	@Override
	protected Integer doInBackground(Integer... arg0) {

		int status = 1;

		try {

			imageBitmap = MetrixAttachmentHelper.loadBitmap(mContext, mFilePath, mRequiredHeight, mRequiredWidth, mResourceCache);

		} catch (Exception e) {
			status = -1;
			LogManager.getInstance().error(e);
		}

		if (status == 1 && imageBitmap != null) {
			MetrixAttachmentHelper.addBitmapToMemoryCache(mKey, imageBitmap,
					mMemoryCache);
		}

		return status;
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		if (result == 1)
			mImageView.setImageBitmap(imageBitmap);
		else
			mImageView.setImageBitmap(null);
		super.onPostExecute(result);
	}
	
    
}