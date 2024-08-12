package com.metrix.architecture.ui.widget;

import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import androidx.collection.LruCache;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;

@SuppressLint("SimpleDateFormat")
public class MetrixGalleryAdapter extends BaseAdapter {
	
	private List<HashMap<String, String>> mList;
	private Context mContext;
	private int mImageWidth;
	private int mImageHeight;
		
	private LruCache<String, Bitmap> mMemoryCache;
	
	private String mUniqueColumn;
	private String mAttachmentNameColumn;
	private MetrixAttachmentManager mMetrixAttachmentManager;
	
	static class ViewHolder {
		ImageView imageViewAttachment;
		ImageView imageViewVideoIcon;
		TextView fileName;
	}
		
	public int getImageWidth() {
		return mImageWidth;
	}

	public void setImageWidth(int imageWidth) {
		this.mImageWidth = imageWidth;
	}
	
	public int getImageHeight() {
		return mImageHeight;
	}

	public void setImageHeight(int imageHeight) {
		this.mImageHeight = imageHeight;
	}
		
	public MetrixGalleryAdapter(Context context, String uniqueColumn, String attachmentNameColumn, List<HashMap<String, String>> list, LruCache<String, Bitmap> memoryCache) {
		
		this.mContext = context;
		this.mList = list;
		this.mMemoryCache = memoryCache;
	    this.mMetrixAttachmentManager = MetrixAttachmentManager.getInstance();
	    this.mUniqueColumn = uniqueColumn;
		this.mAttachmentNameColumn = attachmentNameColumn;

	}
	
	@Override
	public int getCount() {
		if(mList == null)
			return 0;
		
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@SuppressLint({ "SimpleDateFormat", "InflateParams" })
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewHolder view;
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
        if(convertView==null)
        {
            view = new ViewHolder();
            convertView = inflater.inflate(R.layout.metrix_gallery_item, null);
            
            view.imageViewAttachment = (ImageView) convertView.findViewById(R.id.imgview_attachment);
            view.imageViewVideoIcon = (ImageView) convertView.findViewById(R.id.imageview_video_icon);
			view.fileName = (TextView) convertView.findViewById(R.id.myImageViewText);
                       
            convertView.setLayoutParams(new MetrixGalleryView.LayoutParams(mImageWidth, mImageHeight));
 
            convertView.setTag(view);
        }
        else
        {
            view = (ViewHolder) convertView.getTag();
        }
 
        HashMap<String, String> item = mList.get(position);
        if (item == null) return convertView;
        	
		String attachmentPath = mMetrixAttachmentManager.getAttachmentPath() + "/" + item.get(mAttachmentNameColumn);
		String onDemand = item.get("attachment.on_demand");
		String key = item.get(mUniqueColumn);
		
		try {
			Bitmap bitmap;

			if(!MetrixStringHelper.isNullOrEmpty(onDemand) && onDemand.equalsIgnoreCase("Y")) {
				bitmap = MetrixAttachmentHelper.drawableToBitmap(mContext.getResources().getDrawable(R.drawable.download), mImageHeight, mImageWidth);
				view.imageViewAttachment.setImageBitmap(bitmap);
			}
			else {
				bitmap = MetrixAttachmentHelper.getBitmapFromMemCache(key, mMemoryCache);
			}

			if (bitmap != null) {
				view.imageViewAttachment.setImageBitmap(bitmap);
			}
			else{
//				bitmap = MetrixAttachmentHelper.getAttachmentResourceBitmap(this.mContext, R.drawable.no_image80x80);
//				view.imageViewAttachment.setImageBitmap(bitmap);
				MetrixAttachmentHelper.showGridPreview(mContext, attachmentPath, view.imageViewAttachment,
						mImageHeight, mImageWidth, mMemoryCache, key);
			}
			
			//if it's an video file setting the video icon for identifying purpose
			MetrixAttachmentHelper.setVideoIcon(view.imageViewVideoIcon, attachmentPath, false);
			if(!MetrixStringHelper.isNullOrEmpty(onDemand) && onDemand.equalsIgnoreCase("Y")) {
				view.fileName.setText(item.get(mAttachmentNameColumn));
			}

	        return convertView;

		} catch (Exception e) {
			Toast.makeText(mContext, e.getLocalizedMessage(),
					Toast.LENGTH_SHORT).show();
		}
		return null;
        
	}
	
	public boolean remove(HashMap<String, String> item) {
		return mList.remove(item);
	}

	public void removeAll() {
		mList.clear();
		mMemoryCache.evictAll();
		mMemoryCache = null;
	}
}
