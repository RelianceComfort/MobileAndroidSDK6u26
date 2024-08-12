package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;

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

public class MetrixDesignerGlobalMenuEnablingActivity extends MetrixDesignerActivity {
	private List<HashMap<String, String>> mOriginalData;
	private ListView mListView;
	private Button mAdd, mSave, mNext;
	private GlobalMenuCheckListAdapter mGlobalChkAdapter;
	private int mMaxOrder = 0;
	private AlertDialog mGlobalMenuPropDialog;
	private HashMap<String, String> mSelectedItemForProp;
	private MetrixDesignerResourceData mGlobalMEResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGlobalMEResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerGlobalMenuEnablingActivityResourceData");

		setContentView(mGlobalMEResourceData.LayoutResourceID);

		mListView = (ListView) findViewById(mGlobalMEResourceData.ListViewResourceID);
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
				Object item = mGlobalChkAdapter.getItem(position);
				@SuppressWarnings("unchecked")
				HashMap<String, String> selectedItem = (HashMap<String, String>) item;

				setSelectedItemForProp(selectedItem);
				mGlobalMenuPropDialog = new AlertDialog.Builder(MetrixDesignerGlobalMenuEnablingActivity.this).create();
				mGlobalMenuPropDialog.setMessage(AndroidResourceHelper.getMessage("GlobalMenuPropConfirm"));
				mGlobalMenuPropDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), globalMenuPropListener);
				mGlobalMenuPropDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), globalMenuPropListener);
				mGlobalMenuPropDialog.show();
				return true;
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mGlobalMEResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mAdd = (Button) findViewById(mGlobalMEResourceData.getExtraResourceID("R.id.add"));
		mAdd.setText(AndroidResourceHelper.getMessage("Add"));
		mAdd.setEnabled(mAllowChanges);
		mAdd.setOnClickListener(this);

		mSave = (Button) findViewById(mGlobalMEResourceData.getExtraResourceID("R.id.save"));
		mSave.setText(AndroidResourceHelper.getMessage("Save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);

		mNext = (Button) findViewById(mGlobalMEResourceData.getExtraResourceID("R.id.next"));
		mNext.setOnClickListener(this);

		TextView mGlblMenu = (TextView) findViewById(mGlobalMEResourceData.getExtraResourceID("R.id.global_menu"));
		TextView mScrInfo = (TextView) findViewById(mGlobalMEResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_global_menu_enabling"));

		AndroidResourceHelper.setResourceValues(mGlblMenu, "GlobalMenu");
		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesGblMenuEnable");
		AndroidResourceHelper.setResourceValues(mNext, "Next");

		populateList();
	}

	private void populateList() {
		StringBuilder query = new StringBuilder();
		query.append("select mm_menu_item.metrix_row_id, mm_menu_item.item_id, mm_menu_item.item_name, mm_menu_item.display_order from mm_menu_item");
		query.append(" where mm_menu_item.revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
		query.append(" and mm_menu_item.design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
		query.append(" order by mm_menu_item.item_name asc");

		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		mOriginalData = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;
		mMaxOrder = 0;

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				String metrixRowID = cursor.getString(0);
				String itemID = cursor.getString(1);
				String itemName = cursor.getString(2);
				String thisOrder = cursor.getString(3);
				int thisOrderNum = Integer.valueOf(thisOrder);

				// populate row for listview's table
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_menu_item.metrix_row_id", metrixRowID);
				row.put("mm_menu_item.item_id", itemID);
				row.put("mm_menu_item.item_name", itemName);
				row.put("mm_menu_item.display_order", thisOrder);
				if (thisOrderNum > 0) {
					row.put("checkboxState", "Y");
				} else {
					row.put("checkboxState", "N");
				}
				table.add(row);

				// also, populate mOriginalData with its very own row
				HashMap<String, String> origRow = new HashMap<String, String>();
				origRow.put("mm_menu_item.metrix_row_id", metrixRowID);
				origRow.put("mm_menu_item.item_id", itemID);
				origRow.put("mm_menu_item.item_name", itemName);
				origRow.put("mm_menu_item.display_order", thisOrder);
				if (thisOrderNum > 0) {
					origRow.put("checkboxState", "Y");
				} else {
					origRow.put("checkboxState", "N");
				}
				mOriginalData.add(origRow);

				// track the highest order number found in results (used when saving changes)
				if (thisOrderNum > mMaxOrder) {
					mMaxOrder = thisOrderNum;
				}

				cursor.moveToNext();
			}

			mGlobalChkAdapter = new GlobalMenuCheckListAdapter(this, table, mGlobalMEResourceData.ListViewItemResourceID, mGlobalMEResourceData.ExtraResourceIDs, mAllowChanges);
			mListView.setAdapter(mGlobalChkAdapter);
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
		if (viewId == mGlobalMEResourceData.getExtraResourceID("R.id.add")) {
			if (mAllowChanges) {
				processAndSaveChanges();

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerGlobalMenuAddActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivity(this, intent);
			}
		} else if (viewId == mGlobalMEResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				processAndSaveChanges();

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerGlobalMenuEnablingActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mGlobalMEResourceData.getExtraResourceID("R.id.next")) {
			if (mAllowChanges) {
				processAndSaveChanges();
			}
			// allow pass through, even if changes aren't allowed
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerGlobalMenuOrderActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	DialogInterface.OnClickListener globalMenuPropListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					startGlobalMenuPropActivity();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

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
			String origItemID = origRow.get("mm_menu_item.item_id");
			String origCheckboxState = origRow.get("checkboxState");
			String chgCheckboxState;
			HashMap<String, String> chgRow = null;
			int idMapIndex = -1;

			for (HashMap<String, String> listRow : mGlobalChkAdapter.mListData) {
				String listItemID = listRow.get("mm_menu_item.item_id");
				if (MetrixStringHelper.valueIsEqual(origItemID, listItemID)) {
					idMapIndex = mGlobalChkAdapter.mIdMap.get(listRow);
					chgRow = listRow;
					break;
				}
			}

			if (idMapIndex > -1) {
				chgCheckboxState = chgRow.get("checkboxState");
				if (!MetrixStringHelper.valueIsEqual(origCheckboxState, chgCheckboxState)) {
					if (MetrixStringHelper.valueIsEqual(chgCheckboxState, "Y")) {
						mMaxOrder++;
						chgRow.put("mm_menu_item.display_order", String.valueOf(mMaxOrder));
						mGlobalChkAdapter.mIdMap.put(chgRow, idMapIndex);
					} else {
						int negativeOne = -1;
						chgRow.put("mm_menu_item.display_order", String.valueOf(negativeOne));
						mGlobalChkAdapter.mIdMap.put(chgRow, idMapIndex);
					}
				}

				if (MetrixStringHelper.valueIsEqual(chgCheckboxState, "Y")) {
					// add to TreeMap, which auto-sorts by display_order property value
					int currValue = Integer.valueOf(chgRow.get("mm_menu_item.display_order"));
					while (positiveOrderSet.containsKey(currValue)) {
						currValue++;
					}
					positiveOrderSet.put(currValue, Integer.valueOf(origItemID));
				}
			}
		}

		// at this point, we have values that are roughly sequential but not incremental ... use TreeMap to enforce incremental order
		int counter = 1;
		for (Integer sequence : positiveOrderSet.keySet()) {
			HashMap<String, String> chgRow2 = null;
			int idMapIndex2 = -1;

			String currItemID = String.valueOf(positiveOrderSet.get(sequence));
			for (HashMap<String, String> listRow : mGlobalChkAdapter.mListData) {
				String listItemID = listRow.get("mm_menu_item.item_id");
				if (MetrixStringHelper.valueIsEqual(currItemID, listItemID)) {
					idMapIndex2 = mGlobalChkAdapter.mIdMap.get(listRow);
					chgRow2 = listRow;
					break;
				}
			}

			if (idMapIndex2 > -1) {
				chgRow2.put("mm_menu_item.display_order", String.valueOf(counter));
				mGlobalChkAdapter.mIdMap.put(chgRow2, idMapIndex2);
				counter++;
			}
		}

		// now, we compare modified list with original list, to see what changes need to be saved (-1s included)
		ArrayList<MetrixSqlData> itemsToUpdate = new ArrayList<MetrixSqlData>();
		for (HashMap<String, String> origRow : mOriginalData) {
			String origItemID = origRow.get("mm_menu_item.item_id");
			String origOrder = origRow.get("mm_menu_item.display_order");

			for (HashMap<String, String> listRow : mGlobalChkAdapter.mListData) {
				String listItemID = listRow.get("mm_menu_item.item_id");
				if (MetrixStringHelper.valueIsEqual(origItemID, listItemID)) {
					String listOrder = listRow.get("mm_menu_item.display_order");
					if (!MetrixStringHelper.valueIsEqual(origOrder, listOrder)) {
						// we have something to save
						String listMetrixRowID = listRow.get("mm_menu_item.metrix_row_id");

						MetrixSqlData data = new MetrixSqlData("mm_menu_item", MetrixTransactionTypes.UPDATE, "metrix_row_id="+ listMetrixRowID);
						data.dataFields.add(new DataField("metrix_row_id", listMetrixRowID));
						data.dataFields.add(new DataField("item_id", origItemID));
						data.dataFields.add(new DataField("display_order", listOrder));
						itemsToUpdate.add(data);
					}
					break;
				}
			}
		}

		if (itemsToUpdate.size() > 0) {
			MetrixTransaction transactionInfo = new MetrixTransaction();
			MetrixUpdateManager.update(itemsToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("MenuItemEnable"), this);
		}
	}

	private void setSelectedItemForProp(HashMap<String, String> item) {
		mSelectedItemForProp = item;
	}

	private void startGlobalMenuPropActivity() {
		String itemID = mSelectedItemForProp.get("mm_menu_item.item_id");
		String itemName = mSelectedItemForProp.get("mm_menu_item.item_name");

		MetrixCurrentKeysHelper.setKeyValue("mm_menu_item", "item_id", itemID);
		MetrixCurrentKeysHelper.setKeyValue("mm_menu_item", "item_name", itemName);
		Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerGlobalMenuPropActivity.class);
		intent.putExtra("headingText", mHeadingText);
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	public static class GlobalMenuCheckListAdapter extends DynamicListAdapter {
		static ViewHolder holder;
		private final String checkBoxText;

		public GlobalMenuCheckListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs, boolean allowChanges) {
			super(context, table, listViewItemResourceID, lviElemResIDs, allowChanges);
			this.checkBoxText = AndroidResourceHelper.getMessage("Cb");
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
			holder.mMetrixRowID.setText(dataRow.get("mm_menu_item.metrix_row_id"));
			holder.mItemID.setText(dataRow.get("mm_menu_item.item_id"));
			holder.mItemName.setText(dataRow.get("mm_menu_item.item_name"));
			holder.mOrder.setText(dataRow.get("mm_menu_item.display_order"));

			String chkState = dataRow.get("checkboxState");
			if (chkState.compareToIgnoreCase("Y") == 0)
				holder.mBox.setChecked(true);
			else
				holder.mBox.setChecked(false);

			return vi;
		}

		static class ViewHolder {
			TextView mMetrixRowID;
			TextView mItemID;
			TextView mItemName;
			TextView mOrder;
			CheckBox mBox;
		}
	}
}
