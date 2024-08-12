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
import com.metrix.architecture.utilities.MetrixStringHelper;


public class MetrixDesignerWorkflowScreenOrderActivity extends MetrixDesignerActivity {
	private DynamicListView mListView;
	private TextView mTitle;
	private Button mSave, mFinish;
	private String mWorkflowName;
	private WorkflowScreenOrderAdapter mWorkflowOrderAdapter;
	private MetrixDesignerResourceData mWorkflowSOResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mWorkflowSOResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerWorkflowScreenOrderActivityResourceData");

		setContentView(mWorkflowSOResourceData.LayoutResourceID);

		mListView = (DynamicListView) findViewById(mWorkflowSOResourceData.ListViewResourceID);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mWorkflowSOResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mWorkflowName = MetrixCurrentKeysHelper.getKeyValue("mm_workflow", "name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mTitle = (TextView) findViewById(mWorkflowSOResourceData.getExtraResourceID("R.id.zzmd_workflow_screen_order_title"));
		String fullTitle = AndroidResourceHelper.getMessage("ScreenOrder1Args", mWorkflowName);
		mTitle.setText(fullTitle);

		mListView.setLongClickable(mAllowChanges);

		mSave = (Button) findViewById(mWorkflowSOResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);

		mFinish = (Button) findViewById(mWorkflowSOResourceData.getExtraResourceID("R.id.finish"));
		mFinish.setOnClickListener(this);

		TextView mScrInfo = (TextView) findViewById(mWorkflowSOResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_workflow_screen_order"));

		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesWfScnOrd");
		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mFinish, "Finish");

		populateList();
	}

	private void populateList() {
		StringBuilder query = new StringBuilder();
		query.append("select mm_workflow_screen.metrix_row_id, mm_workflow_screen.screen_id, mm_screen.screen_name, mm_workflow_screen.step_order, mm_screen.force_order from mm_workflow_screen");
		query.append(" join mm_screen on mm_workflow_screen.screen_id = mm_screen.screen_id");
		query.append(" where mm_workflow_screen.workflow_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_workflow", "workflow_id"));
//		query.append(" and mm_screen.workflow_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_workflow", "workflow_id"));
		query.append(" and mm_workflow_screen.step_order is not null");
		query.append(" and mm_workflow_screen.step_order > 0");
		query.append(" order by mm_workflow_screen.step_order asc");

		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			final String firstScreenIndicatorText = AndroidResourceHelper.getMessage("FirstScreenIndicator");
			final String lastScreenIndicatorText = AndroidResourceHelper.getMessage("LastScreenIndicator");

			while (cursor.isAfterLast() == false) {

				String thisForceOrder = cursor.getString(4);
				String screenName = cursor.getString(2);

				if(MetrixStringHelper.valueIsEqual(thisForceOrder,"FIRST"))
				{
					screenName = screenName + firstScreenIndicatorText;
				}
				else if(MetrixStringHelper.valueIsEqual(thisForceOrder,"LAST"))
				{
					screenName = screenName + lastScreenIndicatorText;
				}

				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_workflow_screen.metrix_row_id", cursor.getString(0));
				row.put("mm_workflow_screen.screen_id", cursor.getString(1));
				row.put("mm_screen.screen_name", screenName);
				row.put("mm_workflow_screen.step_order", cursor.getString(3));
				row.put("mm_screen.force_order", thisForceOrder);
				table.add(row);

				cursor.moveToNext();
			}

			mWorkflowOrderAdapter = new WorkflowScreenOrderAdapter(this, table, mWorkflowSOResourceData.ListViewItemResourceID, mWorkflowSOResourceData.ExtraResourceIDs, mAllowChanges);
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
		if (viewId == mWorkflowSOResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {

				//String message = mWorkflowSOResourceData.getExtraResourceString("AndroidResourceHelper.getMessage("ScreenOrderingTip"));
				//Toast.makeText(this.getApplicationContext(),message, Toast.LENGTH_LONG).show();
				processAndSaveChanges();

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerWorkflowScreenOrderActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mWorkflowSOResourceData.getExtraResourceID("R.id.finish")) {
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
			ArrayList<HashMap<String, String>> resortedWFData = new ArrayList<HashMap<String, String>>();
			HashMap<String, String> firstWFItem = new HashMap<String, String>();
			HashMap<String, String> lastWFItem = new HashMap<String, String>();

			for (HashMap<String, String> row : mListView.mListData) {
				String sequenceItem = row.get("mm_screen.force_order");

				if(!MetrixStringHelper.isNullOrEmpty(sequenceItem)) {
					if(sequenceItem.compareToIgnoreCase("FIRST")==0) {
						firstWFItem = row;
					}
					else if(sequenceItem.compareToIgnoreCase("LAST")==0) {
						lastWFItem = row;
					}
					else {
						resortedWFData.add(row);
					}
				}
				else {
					resortedWFData.add(row);
				}
			}

			// Insert the firstItem at the start of the array
			if(!firstWFItem.isEmpty()){
				resortedWFData.add(0, firstWFItem);
			}
			// Append the lastItem at the end of the array
			if(!lastWFItem.isEmpty()){
				resortedWFData.add(lastWFItem);
			}

			int i = 1;
			for (HashMap<String, String> row : resortedWFData) {
				String oldPosition = row.get("mm_workflow_screen.step_order");
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
					data.dataFields.add(new DataField("step_order", newPosition));
					screensToUpdate.add(data);
				}

				i++;
			}

			if (screensToUpdate.size() > 0) {
				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(screensToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("ScreenWFOrder"), this);
			}
		}
	}

	public static class WorkflowScreenOrderAdapter extends DynamicListAdapter {
		static ViewHolder holder;

		public WorkflowScreenOrderAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs, boolean allowChanges) {
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
				holder.mStepOrder = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_workflow_screen__step_order"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mMetrixRowID.setText(dataRow.get("mm_workflow_screen.metrix_row_id"));
			holder.mScreenID.setText(dataRow.get("mm_workflow_screen.screen_id"));
			holder.mScreenName.setText(dataRow.get("mm_screen.screen_name"));
			holder.mStepOrder.setText(dataRow.get("mm_workflow_screen.step_order"));

			return vi;
		}

		static class ViewHolder {
			TextView mMetrixRowID;
			TextView mScreenID;
			TextView mScreenName;
			TextView mStepOrder;
		}
	}
}

