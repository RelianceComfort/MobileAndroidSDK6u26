package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixDesignerResourceData;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class MetrixDesignerDesignSetActivity extends MetrixDesignerActivity implements OnItemClickListener {
	private ListView mListView;
	private DesignSetListAdapter mDesignSetAdapter;
	private MetrixDesignerResourceData mDesignSetResourceData;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);    	
    	
    	mDesignSetResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerDesignSetActivityResourceData");
        
    	setContentView(mDesignSetResourceData.LayoutResourceID);
    	
    	mListView = (ListView) findViewById(mDesignSetResourceData.ListViewResourceID);
        mListView.setOnItemClickListener(this);
    }
    
	@Override
	public void onStart() {
		super.onStart();
		
		helpText = mDesignSetResourceData.HelpTextString;

		TextView mDsgnSets = (TextView) findViewById(mDesignSetResourceData.getExtraResourceID("R.id.design_sets"));
		TextView mScrInfo = (TextView) findViewById(mDesignSetResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_design_set"));

		AndroidResourceHelper.setResourceValues(mDsgnSets, "DesignSets");
		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnDescMxDesDesSet");

		populateList();
	}

	private void populateList() {
		String query = "select mm_design_set.design_set_id, mm_design_set.name from mm_design_set order by mm_design_set.name asc";
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;
		
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);
	
			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}
			
			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_design_set.design_set_id", cursor.getString(0));
				row.put("mm_design_set.name", cursor.getString(1));
				table.add(row);
				cursor.moveToNext();
			}
			
			mDesignSetAdapter = new DesignSetListAdapter(this, table, mDesignSetResourceData.ListViewItemResourceID, mDesignSetResourceData.ExtraResourceIDs);
			mListView.setAdapter(mDesignSetAdapter);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = mDesignSetAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;
		
		String designSetId = selectedItem.get("mm_design_set.design_set_id");
		MetrixCurrentKeysHelper.setKeyValue("mm_design_set", "design_set_id", designSetId);
		
		Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerDesignActivity.class);
		MetrixActivityHelper.startNewActivity(this, intent);
	}
	
	public static class DesignSetListAdapter extends DynamicListAdapter {
		static ViewHolder holder;

		public DesignSetListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
			super(context, table, listViewItemResourceID, lviElemResIDs);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);
				holder = new ViewHolder();
				holder.mName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_design_set__name"));
				holder.mDesignSetID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_design_set__design_set_id"));
				
				vi.setTag(holder);	
			} else {
				holder = (ViewHolder) vi.getTag();
			}
			
			HashMap<String, String> dataRow = mListData.get(position);		
			holder.mName.setText(dataRow.get("mm_design_set.name"));
			holder.mDesignSetID.setText(dataRow.get("mm_design_set.design_set_id"));
			
			return vi;
		}

		static class ViewHolder {
			TextView mName;
			TextView mDesignSetID;
		}
	}
}
