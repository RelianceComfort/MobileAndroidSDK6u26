package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

public class MetrixDesignerSkinImagesActivity extends MetrixDesignerActivity {
	private HashMap<String, String> mOriginalData;
	private Button mSmallIconSelect, mSmallIconClear, mLargeIconSelect, mLargeIconClear, mSave, mFinish;
	private TextView mSmallIconImageID, mLargeIconImageID;
	private ImageView mSmallIconPreview, mLargeIconPreview;
	private MetrixDesignerResourceData mSkinImagesResourceData;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);    	
    	
    	mSkinImagesResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerSkinImagesActivityResourceData");
        
        setContentView(mSkinImagesResourceData.LayoutResourceID);
        
        populateScreen();
    }
    
	@Override
	public void onStart() {
		super.onStart();
		
		helpText = mSkinImagesResourceData.HelpTextString;
		
		mHeadingText = MetrixCurrentKeysHelper.getKeyValue("mm_skin", "name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}
		
		mSmallIconSelect = (Button) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.small_icon_select"));
		mSmallIconClear = (Button) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.small_icon_clear"));
		mLargeIconSelect = (Button) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.large_icon_select"));
		mLargeIconClear = (Button) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.large_icon_clear"));
		mSave = (Button) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.save"));
		mFinish = (Button) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.finish"));
		
		mSmallIconSelect.setOnClickListener(this);
		mSmallIconClear.setOnClickListener(this);
		mLargeIconSelect.setOnClickListener(this);
		mLargeIconClear.setOnClickListener(this);
		mSave.setOnClickListener(this);
		mFinish.setOnClickListener(this);

		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mFinish, "Finish");
		AndroidResourceHelper.setResourceValues(mLargeIconClear, "Clear");
		AndroidResourceHelper.setResourceValues(mLargeIconSelect, "Select");
		AndroidResourceHelper.setResourceValues(mSmallIconClear, "Clear");
		AndroidResourceHelper.setResourceValues(mSmallIconSelect, "Select");
	}
	
	private void populateScreen() {
		mSmallIconImageID = (TextView) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.mm_skin__icon_small_image_id"));
		mLargeIconImageID = (TextView) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.mm_skin__icon_large_image_id"));
		mSmallIconPreview = (ImageView) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.icon_small_image_preview"));
		mLargeIconPreview = (ImageView) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.icon_large_image_preview"));
	
		mSmallIconImageID.addTextChangedListener(new ImageTextWatcher(mSmallIconPreview, mSkinImagesResourceData.getExtraResourceID("R.drawable.no_image24x24")));
		mLargeIconImageID.addTextChangedListener(new ImageTextWatcher(mLargeIconPreview, mSkinImagesResourceData.getExtraResourceID("R.drawable.no_image80x80")));

		TextView mImg = (TextView) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.images"));
		TextView mScrInfo = (TextView) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_skin_images"));
		TextView mSmlIcon = (TextView) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.small_icon"));
		TextView mLrgIcon = (TextView) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.large_icon"));

		AndroidResourceHelper.setResourceValues(mImg, "Images");
		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesSkinImages");
		AndroidResourceHelper.setResourceValues(mSmlIcon, "SmallIcon");
		AndroidResourceHelper.setResourceValues(mLrgIcon, "LargeIcon");

		String currentSkinID = MetrixCurrentKeysHelper.getKeyValue("mm_skin", "skin_id");
		StringBuilder query = new StringBuilder();
		query.append("select distinct mm_skin.metrix_row_id, mm_skin.icon_small_image_id, mm_skin.icon_large_image_id");
		query.append(String.format(" from mm_skin where skin_id = %s", currentSkinID));
		
		MetrixCursor cursor = null;
		mOriginalData = new HashMap<String, String>();
		
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);
	
			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}
			
			while (cursor.isAfterLast() == false) {
				String smallIconImageIDString = cursor.getString(1);
				String largeIconImageIDString = cursor.getString(2);
				
				mOriginalData.put("mm_skin.metrix_row_id", cursor.getString(0));
				mOriginalData.put("mm_skin.icon_small_image_id", smallIconImageIDString);
				mOriginalData.put("mm_skin.icon_large_image_id", largeIconImageIDString);

				mSmallIconImageID.setText(smallIconImageIDString);
				mLargeIconImageID.setText(largeIconImageIDString);
				break;
			}	
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		super.onClick(v);
		
		int viewId = v.getId();
		if (viewId == mSkinImagesResourceData.getExtraResourceID("R.id.small_icon_select")
			|| viewId == mSkinImagesResourceData.getExtraResourceID("R.id.large_icon_select")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.architecture.ui.widget", "ImagePicker");
			LinearLayout parentLayout = (LinearLayout) findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.table_layout"));
				
			String columnName = "";
			if (viewId == mSkinImagesResourceData.getExtraResourceID("R.id.small_icon_select")) {columnName = "icon_small_image_id";}
			if (viewId == mSkinImagesResourceData.getExtraResourceID("R.id.large_icon_select")) {columnName = "icon_large_image_id";}

			if (!MetrixStringHelper.isNullOrEmpty(columnName)) {
				TextView tvImageID = (TextView) parentLayout.findViewById(mSkinImagesResourceData.getExtraResourceID("R.id.mm_skin__" + columnName));				
				MetrixPublicCache.instance.addItem("imagePickerTextView", tvImageID);
				MetrixPublicCache.instance.addItem("imagePickerParentLayout", parentLayout);
				startActivityForResult(intent, 8181);
			}
		} else if (viewId == mSkinImagesResourceData.getExtraResourceID("R.id.small_icon_clear")) {
			mSmallIconImageID.setText("");
		} else if (viewId == mSkinImagesResourceData.getExtraResourceID("R.id.large_icon_clear")) {
			mLargeIconImageID.setText("");
		} else if (viewId == mSkinImagesResourceData.getExtraResourceID("R.id.save")) {
			if (processAndSaveChanges()) {				
				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerSkinImagesActivity.class);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mSkinImagesResourceData.getExtraResourceID("R.id.finish")) {
			if (processAndSaveChanges()) {				
				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerSkinActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				MetrixActivityHelper.startNewActivity(this, intent);
			}
		}
	}
	
	private boolean processAndSaveChanges() {	
		try {
			String smallIconImageIDString = mSmallIconImageID.getText().toString();
			String largeIconImageIDString = mLargeIconImageID.getText().toString();
			
			boolean generateUpdateMsg = false;			
			ArrayList<MetrixSqlData> skinToUpdate = new ArrayList<MetrixSqlData>();
			String metrixRowID = mOriginalData.get("mm_skin.metrix_row_id");
			MetrixSqlData data = new MetrixSqlData("mm_skin", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowID);
			data.dataFields.add(new DataField("metrix_row_id", metrixRowID));
			data.dataFields.add(new DataField("skin_id", MetrixCurrentKeysHelper.getKeyValue("mm_skin", "skin_id")));
			
			if (!MetrixStringHelper.valueIsEqual(smallIconImageIDString, mOriginalData.get("mm_skin.icon_small_image_id"))) {
				data.dataFields.add(new DataField("icon_small_image_id", smallIconImageIDString));
				generateUpdateMsg = true;
			}
			if (!MetrixStringHelper.valueIsEqual(largeIconImageIDString, mOriginalData.get("mm_skin.icon_large_image_id"))) {
				data.dataFields.add(new DataField("icon_large_image_id", largeIconImageIDString));
				generateUpdateMsg = true;
			}
			
			if (generateUpdateMsg) {
				skinToUpdate.add(data);
				
				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(skinToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("SkinImagesChange"), this);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			Toast.makeText(this, AndroidResourceHelper.getMessage("SaveFailedExThrown"), Toast.LENGTH_LONG).show();
			return false;
		}
		
		return true;
	}
}