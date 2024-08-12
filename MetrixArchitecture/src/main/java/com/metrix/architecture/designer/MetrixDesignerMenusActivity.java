package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class MetrixDesignerMenusActivity extends MetrixDesignerActivity implements OnItemClickListener {
	private ListView mListView;
	private MenusListAdapter mMenusAdapter;
	private MetrixDesignerResourceData mMenusResourceData;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMenusResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerMenusActivityResourceData");

        setContentView(mMenusResourceData.LayoutResourceID);

        mListView = (ListView) findViewById(mMenusResourceData.ListViewResourceID);
        mListView.setOnItemClickListener(this);
    }

    @Override
    public void onStart() {
		super.onStart();

		helpText = mMenusResourceData.HelpTextString;

        mHeadingText = getIntent().getStringExtra("headingText");
        if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

        TextView mMenus = (TextView) findViewById(mMenusResourceData.getExtraResourceID("R.id.menus"));
        TextView mScrInfo = (TextView) findViewById(mMenusResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_menus"));

        AndroidResourceHelper.setResourceValues(mMenus, "Menus");
        AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesMenus");

		populateList();

        if (this.getIntent().getExtras().containsKey("targetDesignerActivity") && !mProcessedTargetIntent) {
            // only possibility now is going to Home Enabling screen
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerHomeMenuEnablingActivity.class);
			intent.putExtra("headingText", mHeadingText);

			mProcessedTargetIntent = true;

			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private void populateList() {
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();

		HashMap<String, String> row = new HashMap<String, String>();
        row.put("text", AndroidResourceHelper.getMessage("Global"));
        row.put("type", "GLOBAL");
        row.put("workflow_id", "");
		table.add(row);

		row = new HashMap<String, String>();
        row.put("text", AndroidResourceHelper.getMessage("Home"));
        row.put("type", "HOME");
        row.put("workflow_id", "");
		table.add(row);

        // also, get any workflows that have corresponding screens with a jump_order value
		StringBuilder query = new StringBuilder();
		query.append("select replace(mm_workflow.name, '~', ' '), mm_workflow.workflow_id from mm_workflow where");
		query.append(" mm_workflow.revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
		query.append(" and mm_workflow.design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
		query.append(" and mm_workflow.workflow_id in (select workflow_id from mm_workflow_screen where jump_order IS NOT NULL)");
		query.append(" order by mm_workflow.name asc");

        MetrixCursor cursor = null;

        try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

            if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			final String workflowText = AndroidResourceHelper.getMessage("Workflow");

            while (cursor.isAfterLast() == false) {
				row = new HashMap<String, String>();
                row.put("text", cursor.getString(0) + " " + workflowText);
                row.put("type", "WORKFLOW");
                row.put("workflow_id", cursor.getString(1));

                table.add(row);
				cursor.moveToNext();
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
        }

		mMenusAdapter = new MenusListAdapter(this, table, mMenusResourceData.ListViewItemResourceID, mMenusResourceData.ExtraResourceIDs);
		mListView.setAdapter(mMenusAdapter);
	}

    @Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = mMenusAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;

        String typeString = selectedItem.get("type");
        if (MetrixStringHelper.valueIsEqual(typeString, "GLOBAL")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerGlobalMenuEnablingActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
            //working on home menu enabling
        } else if (MetrixStringHelper.valueIsEqual(typeString, "HOME")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerHomeMenuEnablingActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		} else if (MetrixStringHelper.valueIsEqual(typeString, "WORKFLOW")) {
			String workflowId = selectedItem.get("workflow_id");
			String workflowName = selectedItem.get("text");
            workflowName = workflowName.replace(AndroidResourceHelper.getMessage("Workflow"), "").trim();
            MetrixCurrentKeysHelper.setKeyValue("mm_workflow", "workflow_id", workflowId);
            MetrixCurrentKeysHelper.setKeyValue("mm_workflow", "name", workflowName);

            Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerWorkflowMenuEnablingActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

    public static class MenusListAdapter extends DynamicListAdapter {
		static ViewHolder holder;

		public MenusListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
			super(context, table, listViewItemResourceID, lviElemResIDs);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);
				holder = new ViewHolder();
				holder.mText = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.zzmd_menu__text"));
				holder.mType = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.zzmd_menu__type"));
				holder.mWorkflowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.zzmd_menu__workflow_id"));

                vi.setTag(holder);
            } else {
				holder = (ViewHolder) vi.getTag();
			}

            HashMap<String, String> dataRow = mListData.get(position);
            holder.mText.setText(dataRow.get("text"));
			holder.mType.setText(dataRow.get("type"));
			holder.mWorkflowID.setText(dataRow.get("workflow_id"));

            return vi;
		}

		static class ViewHolder {
			TextView mText;
			TextView mType;
			TextView mWorkflowID;
		}
	}
}

