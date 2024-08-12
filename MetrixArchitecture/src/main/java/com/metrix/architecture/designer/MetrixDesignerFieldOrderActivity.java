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

public class MetrixDesignerFieldOrderActivity extends MetrixDesignerActivity {
	private DynamicListView mListView;
	private TextView mTitle;
	private Button mSave, mFinish;
	private String mScreenName;
	private FieldOrderAdapter mFieldOrderAdapter;
	private MetrixDesignerResourceData mFieldOrderResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mFieldOrderResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldOrderActivityResourceData");

		setContentView(mFieldOrderResourceData.LayoutResourceID);

		mListView = (DynamicListView) findViewById(mFieldOrderResourceData.ListViewResourceID);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mFieldOrderResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mTitle = (TextView) findViewById(mFieldOrderResourceData.getExtraResourceID("R.id.zzmd_field_order_title"));
		String fullTitle = AndroidResourceHelper.getMessage("FieldOrder1Args", mScreenName);
		mTitle.setText(fullTitle);

		mListView.setLongClickable(mAllowChanges);

		mSave = (Button) findViewById(mFieldOrderResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);

		mFinish = (Button) findViewById(mFieldOrderResourceData.getExtraResourceID("R.id.finish"));
		mFinish.setOnClickListener(this);

		TextView mFldOrdrTitl = (TextView) findViewById(mFieldOrderResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_field_order"));

		AndroidResourceHelper.setResourceValues(mFldOrdrTitl, "ScnInfoMxDesFldOrd");
		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mFinish, "Finish");

		populateList();
	}

	private void populateList() {
		StringBuilder query = new StringBuilder();
		query.append("SELECT mm_field.metrix_row_id, mm_field.field_id, mm_field.display_order, mm_field.table_name, mm_field.column_name from mm_field");
		query.append(" WHERE mm_field.screen_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id"));
		query.append(" AND mm_field.display_order > 0");
		query.append(" ORDER BY mm_field.display_order ASC");

		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_field.metrix_row_id", cursor.getString(0));
				row.put("mm_field.field_id", cursor.getString(1));
				row.put("mm_field.display_order", cursor.getString(2));

				String tableName = cursor.getString(3);
				String colName = cursor.getString(4);
				row.put("mm_field.field_name", tableName + "." + colName);
				table.add(row);

				cursor.moveToNext();
			}

			mFieldOrderAdapter = new FieldOrderAdapter(this, table, mFieldOrderResourceData.ListViewItemResourceID, mFieldOrderResourceData.ExtraResourceIDs, mAllowChanges);
			mListView.setListWithEnabling(table, mAllowChanges);
			mListView.setAdapter(mFieldOrderAdapter);
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
		if (viewId == mFieldOrderResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				processAndSaveChanges();

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldOrderActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mFieldOrderResourceData.getExtraResourceID("R.id.finish")) {
			if (mAllowChanges) {
				processAndSaveChanges();
			}
			// allow pass through, even if changes aren't allowed (popping this activity off of stack)
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldActivity.class);
			intent.putExtra("headingText", mHeadingText);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private void processAndSaveChanges() {
		if (mListView.mListData != null && mListView.mListData.size() > 0) {
			ArrayList<MetrixSqlData> fieldsToUpdate = new ArrayList<MetrixSqlData>();
			int i = 1;
			for (HashMap<String, String> row : mListView.mListData) {
				String oldPosition = row.get("mm_field.display_order");
				String newPosition = String.valueOf(i);

				if (!oldPosition.equals(newPosition)) {
					// then we need to do an update
					String listMetrixRowID = row.get("mm_field.metrix_row_id");
					String listFieldID = row.get("mm_field.field_id");

					MetrixSqlData data = new MetrixSqlData("mm_field", MetrixTransactionTypes.UPDATE, "metrix_row_id="+ listMetrixRowID);
					data.dataFields.add(new DataField("metrix_row_id", listMetrixRowID));
					data.dataFields.add(new DataField("field_id", listFieldID));
					data.dataFields.add(new DataField("display_order", newPosition));
					fieldsToUpdate.add(data);
				}

				i++;
			}

			if (fieldsToUpdate.size() > 0) {
				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(fieldsToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("Field_Order"), this);
			}
		}
	}

	public static class FieldOrderAdapter extends DynamicListAdapter {
		static ViewHolder holder;

		public FieldOrderAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs, boolean allowChanges) {
			super(context, table, listViewItemResourceID, lviElemResIDs, allowChanges);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);

				holder = new ViewHolder();
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field__metrix_row_id"));
				holder.mFieldID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field__field_id"));
				holder.mFieldName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field__field_name"));
				holder.mOrder = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field__display_order"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mMetrixRowID.setText(dataRow.get("mm_field.metrix_row_id"));
			holder.mFieldID.setText(dataRow.get("mm_field.field_id"));
			holder.mFieldName.setText(dataRow.get("mm_field.field_name"));
			holder.mOrder.setText(dataRow.get("mm_field.display_order"));

			return vi;
		}

		static class ViewHolder {
			TextView mMetrixRowID;
			TextView mFieldID;
			TextView mFieldName;
			TextView mOrder;
		}
	}
}

