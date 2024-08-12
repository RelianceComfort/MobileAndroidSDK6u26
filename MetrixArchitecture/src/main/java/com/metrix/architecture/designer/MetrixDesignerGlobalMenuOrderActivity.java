package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MetrixDesignerGlobalMenuOrderActivity extends MetrixDesignerActivity {
	private DynamicListView mListView;
	private Button mSave, mFinish;
	private GlobalMenuOrderAdapter mGlobalOrderAdapter;
	private MetrixDesignerResourceData mGlobalMOResourceData;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);    	
    	
    	mGlobalMOResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerGlobalMenuOrderActivityResourceData");
        
        setContentView(mGlobalMOResourceData.LayoutResourceID);
        
        mListView = (DynamicListView) findViewById(mGlobalMOResourceData.ListViewResourceID);
    }
    
	@Override
	public void onStart() {
		super.onStart();
		
		helpText = mGlobalMOResourceData.HelpTextString;
		
		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}
        
		mListView.setLongClickable(mAllowChanges);
		
		mSave = (Button) findViewById(mGlobalMOResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);	
		
		mFinish = (Button) findViewById(mGlobalMOResourceData.getExtraResourceID("R.id.finish"));
		mFinish.setOnClickListener(this);

		TextView mGlblMnuOrdr = (TextView) findViewById(mGlobalMOResourceData.getExtraResourceID("R.id.global_menu_order"));
		TextView mScrInfo = (TextView) findViewById(mGlobalMOResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_global_menu_order"));

		AndroidResourceHelper.setResourceValues(mGlblMnuOrdr, "GlobalMenuOrder");
		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesGblMenuOrd");
		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mFinish, "Finish");

		populateList();
	}

	private void populateList() {	
		StringBuilder query = new StringBuilder();
		query.append("select mm_menu_item.metrix_row_id, mm_menu_item.item_id, mm_menu_item.item_name, mm_menu_item.display_order from mm_menu_item");
		query.append(" where mm_menu_item.revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
		query.append(" and mm_menu_item.design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
		query.append(" and mm_menu_item.display_order > 0"); 
		query.append(" order by mm_menu_item.display_order asc");
		
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;		
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);
	
			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}
			
			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_menu_item.metrix_row_id", cursor.getString(0));
				row.put("mm_menu_item.item_id", cursor.getString(1));
				row.put("mm_menu_item.item_name", cursor.getString(2));
				row.put("mm_menu_item.display_order", cursor.getString(3));
				table.add(row);
							
				cursor.moveToNext();
			}
			
			mGlobalOrderAdapter = new GlobalMenuOrderAdapter(this, table, mGlobalMOResourceData.ListViewItemResourceID, mGlobalMOResourceData.ExtraResourceIDs, mAllowChanges);
			mListView.setListWithEnabling(table, mAllowChanges);
			mListView.setAdapter(mGlobalOrderAdapter);
			mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
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
		if (viewId == mGlobalMOResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				processAndSaveChanges();
				
				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerGlobalMenuOrderActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mGlobalMOResourceData.getExtraResourceID("R.id.finish")) {
			if (mAllowChanges) {
				processAndSaveChanges();
			}			
			// allow pass through, even if changes aren't allowed
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerCategoriesActivity.class);
			intent.putExtra("headingText", mHeadingText);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}
	
	private void processAndSaveChanges() {
		if (mListView.mListData != null && mListView.mListData.size() > 0) {
			ArrayList<MetrixSqlData> itemsToUpdate = new ArrayList<MetrixSqlData>();
			int i = 1;
			for (HashMap<String, String> row : mListView.mListData) {					
				String oldPosition = row.get("mm_menu_item.display_order");
				String newPosition = String.valueOf(i);
				
				if (!oldPosition.equals(newPosition)) {
					// then we need to do an update
					String listMetrixRowID = row.get("mm_menu_item.metrix_row_id");
					String listItemID = row.get("mm_menu_item.item_id");
					
					MetrixSqlData data = new MetrixSqlData("mm_menu_item", MetrixTransactionTypes.UPDATE, "metrix_row_id="+ listMetrixRowID);
					data.dataFields.add(new DataField("metrix_row_id", listMetrixRowID));
					data.dataFields.add(new DataField("item_id", listItemID));
					data.dataFields.add(new DataField("display_order", newPosition));
					itemsToUpdate.add(data);
				}
				
				i++;
			}
			
			if (itemsToUpdate.size() > 0) {
				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(itemsToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("MenuItemOrder"), this);
			}
		}
	}
	
	public static class GlobalMenuOrderAdapter extends DynamicListAdapter {
		static ViewHolder holder;

		public GlobalMenuOrderAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs, boolean allowChanges) {
			super(context, table, listViewItemResourceID, lviElemResIDs, allowChanges);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);
				
				holder = new ViewHolder();
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_menu_item__metrix_row_id"));
				holder.mItemID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_menu_item__item_id"));
				holder.mItemName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_menu_item__item_name"));
				holder.mOrder = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_menu_item__display_order"));
					
				vi.setTag(holder);				
			} else {
				holder = (ViewHolder) vi.getTag();
			}
			
			HashMap<String, String> dataRow = mListData.get(position);		
			holder.mMetrixRowID.setText(dataRow.get("mm_menu_item.metrix_row_id"));
			holder.mItemID.setText(dataRow.get("mm_menu_item.item_id"));
			holder.mItemName.setText(dataRow.get("mm_menu_item.item_name"));
			holder.mOrder.setText(dataRow.get("mm_menu_item.display_order"));
			
			return vi;
		}
		
		static class ViewHolder {
			TextView mMetrixRowID;
			TextView mItemID;
			TextView mItemName;
			TextView mOrder;
		}
	}
}
