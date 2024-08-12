package com.metrix.architecture.ui.widget;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.collection.LruCache;
import android.util.AttributeSet;
import android.view.Display;
import android.view.WindowManager;
import android.widget.GridView;

import com.metrix.architecture.utilities.LogManager;

public class MetrixGalleryView extends GridView {

	private MetrixGalleryAdapter mGalleryAdapter;
	private Context mContext;
	private int mOrientation;
	private int previousOrientation;
	private int mNumberOfColumnsInPortraitMode = 3;
	private int mNumberOfColumnsInHorizontalMode = 6;	
	private int mNumberOfRowsInPortraitMode = 4;
	private int mNumberOfRowsInHorizontalMode = 2;
	private boolean mTabletLayoutRequired;
		
	protected int getVerticalColumnCount() {
		return mNumberOfColumnsInPortraitMode;
	}

	protected void setVerticalColumnCount(int mNumberOfColumnsInPortraitMode) {
		this.mNumberOfColumnsInPortraitMode = mNumberOfColumnsInPortraitMode;
	}

	protected int getHorizontalColumnCount() {
		return mNumberOfColumnsInHorizontalMode;
	}

	protected void setHorizontalColumnCount(int mNumberOfColumnsInHorizontalMode) {
		this.mNumberOfColumnsInHorizontalMode = mNumberOfColumnsInHorizontalMode;
	}

	public MetrixGalleryView(Context context) {
		super(context);
		mContext = context;
	}

	public MetrixGalleryView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	public MetrixGalleryView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}
	
	//Tablet UI Optimization
	public void setDatasource(Context context, String uniqueColumn, String attachmentNameColumn, List<HashMap<String, String>> dataList, LruCache<String, Bitmap> memoryCache, boolean tabletLayoutRequired) {
		mTabletLayoutRequired = tabletLayoutRequired;
		mContext = context;
		mGalleryAdapter = new MetrixGalleryAdapter(context, uniqueColumn, attachmentNameColumn, dataList, memoryCache);
		this.setAdapter(mGalleryAdapter);
		//reseting the orientation
		previousOrientation = 0;
		
	}
	//End Tablet UI Optimization
	
	public void setDatasource(Context context, String uniqueColumn, String attachmentNameColumn, List<HashMap<String, String>> dataList, LruCache<String, Bitmap> memoryCache) {
		mContext = context;
		mGalleryAdapter = new MetrixGalleryAdapter(context, uniqueColumn, attachmentNameColumn, dataList, memoryCache);
		this.setAdapter(mGalleryAdapter);
		//reseting the orientation
		previousOrientation = 0;
	}
	
	public void removeItem(HashMap<String, String> deletedItem) {
		mGalleryAdapter.remove(deletedItem);
		mGalleryAdapter.notifyDataSetChanged();
	}
	
	public void clearAllImages() {
		if (mGalleryAdapter != null) {
			mGalleryAdapter.removeAll();
			mGalleryAdapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void setSelection(int position) {
		boolean sameSelected = position == getSelectedItemPosition();
		super.setSelection(position);
		if (sameSelected) {
			final OnItemSelectedListener onItemSelectedListener = getOnItemSelectedListener();
			if (onItemSelectedListener != null)
				onItemSelectedListener.onItemSelected(this, getSelectedView(), position, getSelectedItemId());
		}
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<String, String> getItem(int position) {
		return (HashMap<String, String>) mGalleryAdapter.getItem(position);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		try{
			int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
			
			mOrientation = mContext.getResources().getConfiguration().orientation;
			if(previousOrientation != mOrientation){
				
				WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
				Display display = wm.getDefaultDisplay();
			
				if (mOrientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
					if(mGalleryAdapter != null){
						mGalleryAdapter.setImageWidth(parentWidth/mNumberOfColumnsInPortraitMode);
						mGalleryAdapter.setImageHeight(display.getHeight()/mNumberOfRowsInPortraitMode);
						this.setNumColumns(mNumberOfColumnsInPortraitMode);
					}
				}
				if (mOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {		
					if(mGalleryAdapter != null){
						if(mTabletLayoutRequired)
							mGalleryAdapter.setImageWidth(((display.getWidth()/10)*8)/mNumberOfColumnsInHorizontalMode);
						else
							mGalleryAdapter.setImageWidth(display.getWidth()/mNumberOfColumnsInHorizontalMode);
						mGalleryAdapter.setImageHeight(display.getHeight()/mNumberOfRowsInHorizontalMode);
						this.setNumColumns(mNumberOfColumnsInHorizontalMode);
					}
				}
			}
			
			previousOrientation = mOrientation;
			
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
		catch(Exception e){
			LogManager.getInstance().error(e);
		}
	}


}
