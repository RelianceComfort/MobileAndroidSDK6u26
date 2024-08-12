package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class MetrixDesignerWorkflowMenuEnablingActivity extends MetrixDesignerActivity {
	private List<HashMap<String, String>> mOriginalData;
	private ListView mListView;
	private TextView mTitle;
	private Button mSave, mNext;
	private String mWorkflowName;
	private WorkflowMenuCheckListAdapter mWorkflowChkAdapter;
	private int mMaxJumpOrder = 0;
	private MetrixDesignerResourceData mWorkflowMEResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mWorkflowMEResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerWorkflowMenuEnablingActivityResourceData");

		setContentView(mWorkflowMEResourceData.LayoutResourceID);

		mListView = (ListView) findViewById(mWorkflowMEResourceData.ListViewResourceID);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mWorkflowMEResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mWorkflowName = MetrixCurrentKeysHelper.getKeyValue("mm_workflow", "name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mTitle = (TextView) findViewById(mWorkflowMEResourceData.getExtraResourceID("R.id.zzmd_workflow_menu_enabling_title"));
		String fullTitle = AndroidResourceHelper.getMessage("Menu1Args", mWorkflowName);
		mTitle.setText(fullTitle);

		mSave = (Button) findViewById(mWorkflowMEResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);

		mNext = (Button) findViewById(mWorkflowMEResourceData.getExtraResourceID("R.id.next"));
		mNext.setOnClickListener(this);

		TextView mScrInfo = (TextView) findViewById(mWorkflowMEResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_workflow_menu_enabling"));

		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesWfMenuEnable");
		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mNext, "Next");

		populateList();
	}

	private void populateList() {
		StringBuilder query = new StringBuilder();
		query.append("select mm_workflow_screen.metrix_row_id, mm_workflow_screen.screen_id, mm_screen.screen_name, mm_workflow_screen.jump_order from mm_workflow_screen");
		query.append(" join mm_screen on mm_workflow_screen.screen_id = mm_screen.screen_id");
		query.append(" where mm_workflow_screen.workflow_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_workflow", "workflow_id"));
//		query.append(" and mm_screen.workflow_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_workflow", "workflow_id"));
		query.append(" and mm_workflow_screen.jump_order is not null");
		query.append(" order by mm_screen.screen_name asc");

		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		mOriginalData = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;
		mMaxJumpOrder = 0;

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				String metrixRowID = cursor.getString(0);
				String screenID = cursor.getString(1);
				String screenName = cursor.getString(2);
				String thisJumpOrder = cursor.getString(3);
				int thisJumpOrderNum = Integer.valueOf(thisJumpOrder);

				// populate row for listview's table
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_workflow_screen.metrix_row_id", metrixRowID);
				row.put("mm_workflow_screen.screen_id", screenID);
				row.put("mm_screen.screen_name", screenName);
				row.put("mm_workflow_screen.jump_order", thisJumpOrder);
				if (thisJumpOrderNum > 0) {
					row.put("checkboxState", "Y");
				} else {
					row.put("checkboxState", "N");
				}
				table.add(row);

				// also, populate mOriginalData with its very own row
				HashMap<String, String> origRow = new HashMap<String, String>();
				origRow.put("mm_workflow_screen.metrix_row_id", metrixRowID);
				origRow.put("mm_workflow_screen.screen_id", screenID);
				origRow.put("mm_screen.screen_name", screenName);
				origRow.put("mm_workflow_screen.jump_order", thisJumpOrder);
				if (thisJumpOrderNum > 0) {
					origRow.put("checkboxState", "Y");
				} else {
					origRow.put("checkboxState", "N");
				}
				mOriginalData.add(origRow);

				// track the highest order number found in results (used when saving changes)
				if (thisJumpOrderNum > mMaxJumpOrder) {
					mMaxJumpOrder = thisJumpOrderNum;
				}

				cursor.moveToNext();
			}

			mWorkflowChkAdapter = new WorkflowMenuCheckListAdapter(this, table, mWorkflowMEResourceData.ListViewItemResourceID, mWorkflowMEResourceData.ExtraResourceIDs, mAllowChanges);
			mListView.setAdapter(mWorkflowChkAdapter);
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
		if (viewId == mWorkflowMEResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				processAndSaveChanges();

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerWorkflowMenuEnablingActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mWorkflowMEResourceData.getExtraResourceID("R.id.next")) {
			if (mAllowChanges) {
				processAndSaveChanges();
			}
			// allow pass through, even if changes aren't allowed
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerWorkflowMenuOrderActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private void processAndSaveChanges() {
		/*
		Compare checkboxState on original data to updated data list from listview
		If Y->Y, no action*
		If N->N, no action
		If Y->N, change prop_val in data list to -1
		If N->Y, change prop_val to ++max*
		(Make sure any changes you make to mListData have a parallel change to mIdMap)
		*AS YOU DO THIS, generate a TreeMap<integer, integer> (prop_val, pv_id) for positive prop_vals (prop_val as INT is KEY to SORT)

		AFTER THIS, use another counter to replace prop_val in data list with sequential value, leaning on TreeMap sort

		Then, compare to original data to determine which ones need to be saved (where prop_val in data list is different from original)
		and save transactions identified
		*/
		TreeMap<Integer, Integer> positiveOrderSet = new TreeMap<Integer, Integer>();

		for (HashMap<String, String> origRow : mOriginalData) {
			String origScreenID = origRow.get("mm_workflow_screen.screen_id");
			String origCheckboxState = origRow.get("checkboxState");
			String chgCheckboxState;
			HashMap<String, String> chgRow = null;
			int idMapIndex = -1;

			for (HashMap<String, String> listRow : mWorkflowChkAdapter.mListData) {
				String listScreenID = listRow.get("mm_workflow_screen.screen_id");
				if (MetrixStringHelper.valueIsEqual(origScreenID, listScreenID)) {
					idMapIndex = mWorkflowChkAdapter.mIdMap.get(listRow);
					chgRow = listRow;
					break;
				}
			}

			if (idMapIndex > -1) {
				chgCheckboxState = chgRow.get("checkboxState");
				if (!MetrixStringHelper.valueIsEqual(origCheckboxState, chgCheckboxState)) {
					if (MetrixStringHelper.valueIsEqual(chgCheckboxState, "Y")) {
						mMaxJumpOrder++;
						chgRow.put("mm_workflow_screen.jump_order", String.valueOf(mMaxJumpOrder));
						mWorkflowChkAdapter.mIdMap.put(chgRow, idMapIndex);
					} else {
						int negativeOne = -1;
						chgRow.put("mm_workflow_screen.jump_order", String.valueOf(negativeOne));
						mWorkflowChkAdapter.mIdMap.put(chgRow, idMapIndex);
					}
				}

				if (MetrixStringHelper.valueIsEqual(chgCheckboxState, "Y")) {
					// add to TreeMap, which auto-sorts by jump_order property value
					int currValue = Integer.valueOf(chgRow.get("mm_workflow_screen.jump_order"));
					while (positiveOrderSet.containsKey(currValue)) {
						currValue++;
					}
					positiveOrderSet.put(currValue, Integer.valueOf(origScreenID));
				}
			}
		}

		// at this point, we have prop_vals that are roughly sequential but not incremental ... use TreeMap to enforce incremental order
		int counter = 1;
		for (Integer sequence : positiveOrderSet.keySet()) {
			HashMap<String, String> chgRow2 = null;
			int idMapIndex2 = -1;

			String currScreenID = String.valueOf(positiveOrderSet.get(sequence));
			for (HashMap<String, String> listRow : mWorkflowChkAdapter.mListData) {
				String listScreenID = listRow.get("mm_workflow_screen.screen_id");
				if (MetrixStringHelper.valueIsEqual(currScreenID, listScreenID)) {
					idMapIndex2 = mWorkflowChkAdapter.mIdMap.get(listRow);
					chgRow2 = listRow;
					break;
				}
			}

			if (idMapIndex2 > -1) {
				chgRow2.put("mm_workflow_screen.jump_order", String.valueOf(counter));
				mWorkflowChkAdapter.mIdMap.put(chgRow2, idMapIndex2);
				counter++;
			}
		}

		// now, we compare modified list with original list, to see what changes need to be saved (-1s included)
		ArrayList<MetrixSqlData> screensToUpdate = new ArrayList<MetrixSqlData>();
		for (HashMap<String, String> origRow : mOriginalData) {
			String origScreenID = origRow.get("mm_workflow_screen.screen_id");
			String origOrder = origRow.get("mm_workflow_screen.jump_order");

			for (HashMap<String, String> listRow : mWorkflowChkAdapter.mListData) {
				String listScreenID = listRow.get("mm_workflow_screen.screen_id");
				if (MetrixStringHelper.valueIsEqual(origScreenID, listScreenID)) {
					String listOrder = listRow.get("mm_workflow_screen.jump_order");
					if (!MetrixStringHelper.valueIsEqual(origOrder, listOrder)) {
						// we have something to save
						String listMetrixRowID = listRow.get("mm_workflow_screen.metrix_row_id");
						String origWorkflowID = MetrixCurrentKeysHelper.getKeyValue("mm_workflow", "workflow_id");

						MetrixSqlData data = new MetrixSqlData("mm_workflow_screen", MetrixTransactionTypes.UPDATE, "metrix_row_id="+ listMetrixRowID);
						data.dataFields.add(new DataField("metrix_row_id", listMetrixRowID));
						data.dataFields.add(new DataField("workflow_id", origWorkflowID));
						data.dataFields.add(new DataField("screen_id", origScreenID));
						data.dataFields.add(new DataField("jump_order", listOrder));
						screensToUpdate.add(data);
					}
					break;
				}
			}
		}

		if (screensToUpdate.size() > 0) {
			MetrixTransaction transactionInfo = new MetrixTransaction();
			MetrixUpdateManager.update(screensToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("ScreenWFMenuEnable"), this);
		}
	}

	public static class WorkflowMenuCheckListAdapter extends DynamicListAdapter {
		static ViewHolder holder;
		private final String checkBoxText;

		public WorkflowMenuCheckListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs, boolean allowChanges) {
			super(context, table, listViewItemResourceID, lviElemResIDs, allowChanges);
			this.checkBoxText = AndroidResourceHelper.getMessage("Cb");
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
				holder.mBox = (CheckBox) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.checkboxState"));
				holder.mBox.setEnabled(mAllowChanges);
				holder.mBox.setText(checkBoxText);

				if (mAllowChanges) {
					holder.mBox.setOnClickListener(new View.OnClickListener() {
						public void onClick(View checkboxView) {
							int getPosition = (Integer) checkboxView.getTag();
							HashMap<String, String> dataRow = mListData.get(getPosition);
							int idMapPosition = mIdMap.get(dataRow);

							String checkState = "";
							if (((CheckBox)checkboxView).isChecked()) {
								checkState = "Y";
							} else {
								checkState = "N";
							}

							dataRow.put("checkboxState", checkState);
							mIdMap.put(dataRow, idMapPosition);
						}
					});
				}

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			holder.mBox.setTag(position);

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mMetrixRowID.setText(dataRow.get("mm_workflow_screen.metrix_row_id"));
			holder.mScreenID.setText(dataRow.get("mm_workflow_screen.screen_id"));
			holder.mScreenName.setText(dataRow.get("mm_screen.screen_name"));
			holder.mJumpOrder.setText(dataRow.get("mm_workflow_screen.jump_order"));

			String chkState = dataRow.get("checkboxState");
			if (chkState.compareToIgnoreCase("Y") == 0)
				holder.mBox.setChecked(true);
			else
				holder.mBox.setChecked(false);

			return vi;
		}

		static class ViewHolder {
			TextView mMetrixRowID;
			TextView mScreenID;
			TextView mScreenName;
			TextView mJumpOrder;
			CheckBox mBox;
		}
	}
}
