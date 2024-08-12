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

public class MetrixDesignerHomeMenuEnablingActivity extends MetrixDesignerActivity {
	private List<HashMap<String, String>> mOriginalData;
	private ListView mListView;
	private Button mAdd, mSave, mNext;
	private HomeMenuCheckListAdapter mHomeChkAdapter;
	private int mMaxOrder = 0;
	private MetrixDesignerResourceData mHomeMEResourceData;
	private AlertDialog mHomeMenuPropDialog;
	private HashMap<String, String> mSelectedItemForProp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mHomeMEResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerHomeMenuEnablingActivityResourceData");
		setContentView(mHomeMEResourceData.LayoutResourceID);
		mListView = (ListView) findViewById(mHomeMEResourceData.ListViewResourceID);
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
				Object item = mHomeChkAdapter.getItem(position);
				@SuppressWarnings("unchecked")
				HashMap<String, String> selectedItem = (HashMap<String, String>) item;
				setSelectedItemForProp(selectedItem);
				mHomeMenuPropDialog = new AlertDialog.Builder(MetrixDesignerHomeMenuEnablingActivity.this).create();
				mHomeMenuPropDialog.setMessage(AndroidResourceHelper.getMessage("HomeMenuPropConfirm"));
				mHomeMenuPropDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), homeMenuPropListener);
				mHomeMenuPropDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), homeMenuPropListener);
				mHomeMenuPropDialog.show();
				return true;
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mHomeMEResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mAdd = (Button) findViewById(mHomeMEResourceData.getExtraResourceID("R.id.add"));
		mAdd.setText(AndroidResourceHelper.getMessage("Add"));
		mAdd.setEnabled(mAllowChanges);
		mAdd.setOnClickListener(this);

		mSave = (Button) findViewById(mHomeMEResourceData.getExtraResourceID("R.id.save"));
		mSave.setText(AndroidResourceHelper.getMessage("Save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);

		mNext = (Button) findViewById(mHomeMEResourceData.getExtraResourceID("R.id.next"));
		mNext.setOnClickListener(this);

		TextView mHomeMenu = (TextView) findViewById(mHomeMEResourceData.getExtraResourceID("R.id.home_menu"));
		TextView mScrInfo = (TextView) findViewById(mHomeMEResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_home_menu_enabling"));

		AndroidResourceHelper.setResourceValues(mHomeMenu, "HomeMenu");
		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesHomeMenuEnable");
		AndroidResourceHelper.setResourceValues(mNext, "Next");

		populateList();
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);
		int viewId = v.getId();
		Boolean saveSucceeded = false;
		if (viewId == mHomeMEResourceData.getExtraResourceID("R.id.add")) {
			if (mAllowChanges) {
				saveSucceeded = processAndSaveChanges();
				if (saveSucceeded) {
					Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerHomeMenuAddActivity.class);
					intent.putExtra("headingText", mHeadingText);
					MetrixActivityHelper.startNewActivity(this, intent);
				}
			}
		} else if (viewId == mHomeMEResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				saveSucceeded = processAndSaveChanges();
				if (saveSucceeded) {
					Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerHomeMenuEnablingActivity.class);
					intent.putExtra("headingText", mHeadingText);
					MetrixActivityHelper.startNewActivityAndFinish(this, intent);
				}
			}
		} else if (viewId == mHomeMEResourceData.getExtraResourceID("R.id.next")) {
			if (mAllowChanges)
				saveSucceeded = processAndSaveChanges();
			else
				saveSucceeded = true;	// allow pass through, even if changes aren't allowed

			if (saveSucceeded) {
				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerHomeMenuOrderActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivity(this, intent);
			}
		}
	}

	private void populateList() {
		StringBuilder query = new StringBuilder();
		query.append("select mm_home_item.metrix_row_id, mm_home_item.item_id, mm_home_item.item_name, mm_home_item.display_order from mm_home_item ");
		query.append(" where mm_home_item.revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
		query.append(" and mm_home_item.design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
		query.append(" order by mm_home_item.item_name asc");

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
				row.put("mm_home_item.metrix_row_id", metrixRowID);
				row.put("mm_home_item.item_id", itemID);
				row.put("mm_home_item.item_name", itemName);
				row.put("mm_home_item.display_order", thisOrder);

				if (thisOrderNum > 0)
					row.put("checkboxState", "Y");
				else
					row.put("checkboxState", "N");

				table.add(row);

				// also, populate mOriginalData with its very own row
				HashMap<String, String> origRow = new HashMap<String, String>();
				origRow.put("mm_home_item.metrix_row_id", metrixRowID);
				origRow.put("mm_home_item.item_id", itemID);
				origRow.put("mm_home_item.item_name", itemName);
				origRow.put("mm_home_item.display_order", thisOrder);
				if (thisOrderNum > 0)
					origRow.put("checkboxState", "Y");
				else
					origRow.put("checkboxState", "N");

				mOriginalData.add(origRow);

				// track the highest order number found in results (used when
				// saving changes)
				if (thisOrderNum > mMaxOrder)
					mMaxOrder = thisOrderNum;

				cursor.moveToNext();
			}

			mHomeChkAdapter = new HomeMenuCheckListAdapter(this, table, mHomeMEResourceData.ListViewItemResourceID, mHomeMEResourceData.ExtraResourceIDs, mAllowChanges);
			mListView.setAdapter(mHomeChkAdapter);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	DialogInterface.OnClickListener homeMenuPropListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					startHomeMenuPropActivity();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private void setSelectedItemForProp(HashMap<String, String> item) {
		mSelectedItemForProp = item;
	}

	private void startHomeMenuPropActivity() {
		String itemID = mSelectedItemForProp.get("mm_home_item.item_id");
		String itemName = mSelectedItemForProp.get("mm_home_item.item_name");

		MetrixCurrentKeysHelper.setKeyValue("mm_home_item", "item_id", itemID);
		MetrixCurrentKeysHelper.setKeyValue("mm_home_item", "item_name", itemName);
		Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerHomeMenuPropActivity.class);
		intent.putExtra("headingText", mHeadingText);
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	private Boolean processAndSaveChanges() {
		try {
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
			int enabledCount = 0;

			for (HashMap<String, String> origRow : mOriginalData) {
				String origItemID = origRow.get("mm_home_item.item_id");
				String origCheckboxState = origRow.get("checkboxState");
				String chgCheckboxState;
				HashMap<String, String> chgRow = null;
				int idMapIndex = -1;

				for (HashMap<String, String> listRow : mHomeChkAdapter.mListData) {
					String listItemID = listRow.get("mm_home_item.item_id");
					if (MetrixStringHelper.valueIsEqual(origItemID, listItemID)) {
						idMapIndex = mHomeChkAdapter.mIdMap.get(listRow);
						chgRow = listRow;
						break;
					}
				}

				if (idMapIndex > -1) {
					chgCheckboxState = chgRow.get("checkboxState");
					if (!MetrixStringHelper.valueIsEqual(origCheckboxState, chgCheckboxState)) {
						if (MetrixStringHelper.valueIsEqual(chgCheckboxState, "Y")) {
							mMaxOrder++;
							chgRow.put("mm_home_item.display_order", String.valueOf(mMaxOrder));
							mHomeChkAdapter.mIdMap.put(chgRow, idMapIndex);
						} else {
							int negativeOne = -1;
							chgRow.put("mm_home_item.display_order", String.valueOf(negativeOne));
							mHomeChkAdapter.mIdMap.put(chgRow, idMapIndex);
						}
					}

					if (MetrixStringHelper.valueIsEqual(chgCheckboxState, "Y")) {
						// add to TreeMap, which auto-sorts by SORT_ORDER property value
						int currValue = Integer.valueOf(chgRow.get("mm_home_item.display_order"));
						while (positiveOrderSet.containsKey(currValue)) {
							currValue++;
						}
						positiveOrderSet.put(currValue, Integer.valueOf(origItemID));
						enabledCount++;
					}
				}
			}

			// Only proceed with further logic if we've found at least three items currently checked
			if (enabledCount < 3) {
				Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("HomeItemEnableCountError"), Toast.LENGTH_LONG).show();
				return false;
			}

			// at this point, we have prop_vals that are roughly sequential but not
			// incremental ... use TreeMap to enforce incremental order
			int counter = 1;
			for (Integer sequence : positiveOrderSet.keySet()) {
				HashMap<String, String> chgRow2 = null;
				int idMapIndex2 = -1;

				String currItemID = String.valueOf(positiveOrderSet.get(sequence));
				for (HashMap<String, String> listRow : mHomeChkAdapter.mListData) {
					String listItemID = listRow.get("mm_home_item.item_id");
					if (MetrixStringHelper.valueIsEqual(currItemID, listItemID)) {
						idMapIndex2 = mHomeChkAdapter.mIdMap.get(listRow);
						chgRow2 = listRow;
						break;
					}
				}

				if (idMapIndex2 > -1) {
					chgRow2.put("mm_home_item.display_order", String.valueOf(counter));
					mHomeChkAdapter.mIdMap.put(chgRow2, idMapIndex2);
					counter++;
				}
			}

			// now, we compare modified list with original list, to see what changes
			// need to be saved (-1s included)
			ArrayList<MetrixSqlData> itemsToUpdate = new ArrayList<MetrixSqlData>();
			for (HashMap<String, String> origRow : mOriginalData) {
				String origItemID = origRow.get("mm_home_item.item_id");
				String origOrder = origRow.get("mm_home_item.display_order");

				for (HashMap<String, String> listRow : mHomeChkAdapter.mListData) {
					String listItemID = listRow.get("mm_home_item.item_id");
					if (MetrixStringHelper.valueIsEqual(origItemID, listItemID)) {
						String listOrder = listRow.get("mm_home_item.display_order");
						if (!MetrixStringHelper.valueIsEqual(origOrder, listOrder)) {
							// we have something to save
							String listMetrixRowID = listRow.get("mm_home_item.metrix_row_id");

							MetrixSqlData data = new MetrixSqlData("mm_home_item", MetrixTransactionTypes.UPDATE, "metrix_row_id=" + listMetrixRowID);
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
				MetrixUpdateManager.update(itemsToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("HomeItemEnable"), this);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			return false;
		}

		return true;
	}

	public static class HomeMenuCheckListAdapter extends DynamicListAdapter {
		static ViewHolder holder;
		private final String checkBoxText;

		public HomeMenuCheckListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs, boolean allowChanges) {
			super(context, table, listViewItemResourceID, lviElemResIDs, allowChanges);
			this.checkBoxText = AndroidResourceHelper.getMessage("Cb");
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);

				holder = new ViewHolder();
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_home_item__metrix_row_id"));
				holder.mItemID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_home_item__item_id"));
				holder.mItemName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_home_item__item_name"));
				holder.mOrder = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_home_item__display_order"));
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
							if (((CheckBox) checkboxView).isChecked()) {
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
			holder.mMetrixRowID.setText(dataRow.get("mm_home_item.metrix_row_id"));
			holder.mItemID.setText(dataRow.get("mm_home_item.item_id"));
			holder.mItemName.setText(dataRow.get("mm_home_item.item_name"));
			holder.mOrder.setText(dataRow.get("mm_home_item.display_order"));

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

