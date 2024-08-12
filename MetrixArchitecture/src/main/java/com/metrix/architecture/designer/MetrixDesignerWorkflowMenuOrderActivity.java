package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

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

public class MetrixDesignerWorkflowMenuOrderActivity extends MetrixDesignerActivity {
	private DynamicListView mListView;
	private TextView mTitle;
	private Button mSave, mFinish;
	private String mWorkflowName;
	private WorkflowMenuOrderAdapter mWorkflowOrderAdapter;
	private MetrixDesignerResourceData mWorkflowMOResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mWorkflowMOResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerWorkflowMenuOrderActivityResourceData");

		setContentView(mWorkflowMOResourceData.LayoutResourceID);

		mListView = (DynamicListView) findViewById(mWorkflowMOResourceData.ListViewResourceID);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mWorkflowMOResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mWorkflowName = MetrixCurrentKeysHelper.getKeyValue("mm_workflow", "name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mTitle = (TextView) findViewById(mWorkflowMOResourceData.getExtraResourceID("R.id.zzmd_workflow_menu_order_title"));
		String fullTitle = AndroidResourceHelper.getMessage("MenuOrder1Args", mWorkflowName);
		mTitle.setText(fullTitle);

		mListView.setLongClickable(mAllowChanges);

		mSave = (Button) findViewById(mWorkflowMOResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);

		mFinish = (Button) findViewById(mWorkflowMOResourceData.getExtraResourceID("R.id.finish"));
		mFinish.setOnClickListener(this);

		TextView mScrInfo = (TextView) findViewById(mWorkflowMOResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_workflow_menu_order"));

		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesWfMenuOrd");
		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mFinish, "Finish");

		populateList();
	}

	private void populateList() {
		StringBuilder query = new StringBuilder();
		query.append("select mm_workflow_screen.metrix_row_id, mm_workflow_screen.screen_id, mm_screen.screen_name, mm_workflow_screen.jump_order from mm_workflow_screen");
		query.append(" join mm_screen on mm_workflow_screen.screen_id = mm_screen.screen_id");
		query.append(" where mm_workflow_screen.workflow_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_workflow", "workflow_id"));
//		query.append(" and mm_screen.workflow_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_workflow", "workflow_id"));
		query.append(" and mm_workflow_screen.jump_order is not null");
		query.append(" and mm_workflow_screen.jump_order > 0");
		query.append(" order by mm_workflow_screen.jump_order asc");

		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_workflow_screen.metrix_row_id", cursor.getString(0));
				row.put("mm_workflow_screen.screen_id", cursor.getString(1));
				row.put("mm_screen.screen_name", cursor.getString(2));
				row.put("mm_workflow_screen.jump_order", cursor.getString(3));
				table.add(row);

				cursor.moveToNext();
			}

			mWorkflowOrderAdapter = new WorkflowMenuOrderAdapter(this, table, mWorkflowMOResourceData.ListViewItemResourceID, mWorkflowMOResourceData.ExtraResourceIDs, mAllowChanges);
			mListView.setListWithEnabling(table, mAllowChanges);
			mListView.setAdapter(mWorkflowOrderAdapter);
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
		if (viewId == mWorkflowMOResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				processAndSaveChanges();

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerWorkflowMenuOrderActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mWorkflowMOResourceData.getExtraResourceID("R.id.finish")) {
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
			ArrayList<MetrixSqlData> screensToUpdate = new ArrayList<MetrixSqlData>();
			int i = 1;
			for (HashMap<String, String> row : mListView.mListData) {
				String oldPosition = row.get("mm_workflow_screen.jump_order");
				String newPosition = String.valueOf(i);

				if (!oldPosition.equals(newPosition)) {
					// then we need to do an update
					String listMetrixRowID = row.get("mm_workflow_screen.metrix_row_id");
					String listScreenID = row.get("mm_workflow_screen.screen_id");
					String origWorkflowID = MetrixCurrentKeysHelper.getKeyValue("mm_workflow", "workflow_id");

					MetrixSqlData data = new MetrixSqlData("mm_workflow_screen", MetrixTransactionTypes.UPDATE, "metrix_row_id="+ listMetrixRowID);
					data.dataFields.add(new DataField("metrix_row_id", listMetrixRowID));
					data.dataFields.add(new DataField("workflow_id", origWorkflowID));
					data.dataFields.add(new DataField("screen_id", listScreenID));
					data.dataFields.add(new DataField("jump_order", newPosition));
					screensToUpdate.add(data);
				}

				i++;
			}

			if (screensToUpdate.size() > 0) {
				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(screensToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("ScreenWFMenuOrder"), this);
			}
		}
	}

	public static class WorkflowMenuOrderAdapter extends DynamicListAdapter {
		static ViewHolder holder;

		public WorkflowMenuOrderAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs, boolean allowChanges) {
			super(context, table, listViewItemResourceID, lviElemResIDs, allowChanges);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);

				holder = new ViewHolder();
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_workflow_screen__metrix_row_id"));
				holder.mScreenID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_workflow_screen__screen_id"));
				holder.mScreenName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen__screen_name"));
				holder.mJumpOrder = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_workflow_screen__jump_order"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mMetrixRowID.setText(dataRow.get("mm_workflow_screen.metrix_row_id"));
			holder.mScreenID.setText(dataRow.get("mm_workflow_screen.screen_id"));
			holder.mScreenName.setText(dataRow.get("mm_screen.screen_name"));
			holder.mJumpOrder.setText(dataRow.get("mm_workflow_screen.jump_order"));

			return vi;
		}

		static class ViewHolder {
			TextView mMetrixRowID;
			TextView mScreenID;
			TextView mScreenName;
			TextView mJumpOrder;
		}
	}
}

