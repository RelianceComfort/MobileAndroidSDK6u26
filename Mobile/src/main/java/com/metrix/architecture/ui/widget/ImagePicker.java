package com.metrix.architecture.ui.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.collection.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.metrixmobile.R;

public class ImagePicker extends Activity {
	private ViewGroup mParentLayout;
	private TextView mImageID;
	private MetrixGalleryView mImageGrid;
	private LruCache<String, Bitmap> mMemoryCache;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.image_picker);
		
		//OOM Exception, allocate cache size depending on the max memory available
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int cacheSize = maxMemory / 8;

		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
	        @Override
	        protected int sizeOf(String key, Bitmap bitmap) {
	            return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
	        }
	        
	    };
		
		LayoutParams params = getWindow().getAttributes();
        params.width = LayoutParams.MATCH_PARENT;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

		mImageID = (TextView) MetrixPublicCache.instance.getItem("imagePickerTextView");
		mParentLayout = (ViewGroup) MetrixPublicCache.instance.getItem("imagePickerParentLayout");
		
		mImageGrid = (MetrixGalleryView) findViewById(R.id.image_gallery);
		mImageGrid.setLongClickable(false);
		mImageGrid.setOnItemClickListener(new OnItemClickListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				boolean valueSet = true;
				try {
					HashMap<String, String> itemMap = (HashMap<String, String>) adapterView.getItemAtPosition(position);
					String imageID = itemMap.get("metrix_image_view.image_id");
					MetrixControlAssistant.setValue(mImageID.getId(), mParentLayout, imageID);
				} catch (Exception e) {
					valueSet = false;
					LogManager.getInstance().error(e);
				}
				
				if (valueSet)
					setResult(RESULT_OK, getIntent());
				else
					setResult(RESULT_CANCELED, getIntent());
				
				ImagePicker.this.finish();
			}			
		});	
	}
	
	public void onStart() {
		super.onStart();
		populateImageGrid();
	}
	
	//free up memory being used by images when activity is not being displayed (regenerated at onStart)
	public void onStop() {
		mImageGrid.clearAllImages();
		super.onStop();
	}
	
	private void populateImageGrid() {
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		StringBuilder query = new StringBuilder();
		query.append("select metrix_image_view.metrix_row_id, metrix_image_view.image_id");
		query.append(" from metrix_image_view where metrix_image_view.image_category = 'MOBILE'");
		
		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);
		
		try {
			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}
	
			String[] mFrom = new String[] { "metrix_image_view.metrix_row_id", "metrix_image_view.image_id" };
	
			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put(mFrom[0], cursor.getString(0));
				row.put(mFrom[1], cursor.getString(1));

				table.add(row);
				cursor.moveToNext();
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		
		if (table != null) {
			mImageGrid.setDatasource(getApplicationContext(), "metrix_image_view.metrix_row_id", "metrix_image_view.image_id", table, mMemoryCache);
		}
	}
}
