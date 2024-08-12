package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.metadata.MetrixUpdateMessage;
import com.metrix.architecture.metadata.MetrixUpdateMessage.MetrixUpdateMessageTransactionType;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

public class MetrixDesignerFieldLookupTableActivity extends MetrixDesignerActivity implements OnItemClickListener {
	private TextView mEmphasis;
	private Button mAddTable;
	private String mScreenName, mFieldName;
	private ListView mListView;
	private AlertDialog mDeleteLookupTableDialog;
	private HashMap<String, String> mSelectedItemToDelete;
	private LookupTableListAdapter mLookupTableAdapter;
	private MetrixDesignerResourceData mLookupTableResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLookupTableResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupTableActivityResourceData");

		setContentView(mLookupTableResourceData.LayoutResourceID);

		mListView = (ListView) findViewById(mLookupTableResourceData.ListViewResourceID);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mLookupTableResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mLookupTableResourceData.getExtraResourceID("R.id.zzmd_field_lookup_table_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupTbl", mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mAddTable = (Button) findViewById(mLookupTableResourceData.getExtraResourceID("R.id.add_table"));
		mAddTable.setEnabled(mAllowChanges);
		mAddTable.setOnClickListener(this);

		TextView mLkupTables = (TextView) findViewById(mLookupTableResourceData.getExtraResourceID("R.id.lookup_tables"));

		AndroidResourceHelper.setResourceValues(mLkupTables, "LookupTables");
		AndroidResourceHelper.setResourceValues(mAddTable, "AddTable");

		populateList();

		mListView.setOnItemClickListener(this);
		if (mAllowChanges) {
			mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
					Object item = mLookupTableAdapter.getItem(position);
					@SuppressWarnings("unchecked")
					HashMap<String, String> selectedItem = (HashMap<String, String>) item;
					if (!mAllowChanges) {
						return false;
					} else {
						setSelectedItemToDelete(selectedItem);
						mDeleteLookupTableDialog = new AlertDialog.Builder(MetrixDesignerFieldLookupTableActivity.this).create();
						mDeleteLookupTableDialog.setMessage(AndroidResourceHelper.getMessage("LookupTableDeleteConfirm"));
						mDeleteLookupTableDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), deleteLookupTableListener);
						mDeleteLookupTableDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), deleteLookupTableListener);
						mDeleteLookupTableDialog.show();
						return true;
					}
				}
			});
		}
	}

	private void populateList() {
		String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
		String currentLkupID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id");

		StringBuilder query = new StringBuilder();
		query.append("select distinct mm_field_lkup_table.metrix_row_id, mm_field_lkup_table.lkup_table_id, mm_field_lkup_table.table_name, mm_field_lkup_table.created_revision_id");
		query.append(" from mm_field_lkup_table where mm_field_lkup_table.lkup_id = " + currentLkupID);
		query.append(" order by mm_field_lkup_table.table_name asc");

		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_field_lkup_table.metrix_row_id", cursor.getString(0));
				row.put("mm_field_lkup_table.lkup_table_id", cursor.getString(1));
				row.put("mm_field_lkup_table.table_name", cursor.getString(2));

				String rawCreatedRevisionID = cursor.getString(3);
				boolean isBaseline = true;
				boolean addedThisRevision = false;
				if (!MetrixStringHelper.isNullOrEmpty(rawCreatedRevisionID)) {
					isBaseline = false;
					if (MetrixStringHelper.valueIsEqual(rawCreatedRevisionID, currentRevisionID))
						addedThisRevision = true;
				}
				row.put("mm_field_lkup_table.is_baseline", String.valueOf(isBaseline));
				row.put("mm_field_lkup_table.added_this_revision", String.valueOf(addedThisRevision));

				table.add(row);
				cursor.moveToNext();
			}

			mLookupTableAdapter = new LookupTableListAdapter(this, table, mLookupTableResourceData.ListViewItemResourceID, mLookupTableResourceData.ExtraResourceIDs);
			mListView.setAdapter(mLookupTableAdapter);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mLookupTableResourceData.getExtraResourceID("R.id.add_table")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupTableAddActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = mLookupTableAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;

		String lkupTableID = selectedItem.get("mm_field_lkup_table.lkup_table_id");
		String tableName = selectedItem.get("mm_field_lkup_table.table_name");
		MetrixCurrentKeysHelper.setKeyValue("mm_field_lkup_table", "lkup_table_id", lkupTableID);
		MetrixCurrentKeysHelper.setKeyValue("mm_field_lkup_table", "table_name", tableName);

		Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupTablePropActivity.class);
		intent.putExtra("headingText", mHeadingText);
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	DialogInterface.OnClickListener deleteLookupTableListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					if (deleteSelectedLookupTableAndChildren()) {
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFieldLookupTableActivity.this, MetrixDesignerFieldLookupTableActivity.class);
						intent.putExtra("headingText", mHeadingText);
						MetrixActivityHelper.startNewActivityAndFinish(MetrixDesignerFieldLookupTableActivity.this, intent);
					}
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private boolean deleteSelectedLookupTableAndChildren() {
		boolean returnValue = true;
		try {
			boolean success = true;
			String currMetrixRowID = mSelectedItemToDelete.get("mm_field_lkup_table.metrix_row_id");
			String currLkupTableID = mSelectedItemToDelete.get("mm_field_lkup_table.lkup_table_id");

			// generate a deletion transaction for mm_field_lkup_table ONLY
			MetrixUpdateMessage message = new MetrixUpdateMessage("mm_field_lkup_table", MetrixUpdateMessageTransactionType.Delete, "lkup_table_id", currLkupTableID);

			MetrixDatabaseManager.begintransaction();

			// delete all child records in DB (all 3 tables), without doing a message transaction
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_column_log where metrix_row_id in (select metrix_row_id from mm_field_lkup_column where lkup_table_id = %s))", currLkupTableID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_column_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup_column where lkup_table_id = %s)", currLkupTableID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_column", String.format("lkup_table_id = %s", currLkupTableID));

			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_table_log where metrix_row_id = %s)", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_table_log", String.format("metrix_row_id = %s", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_table", String.format("lkup_table_id = %s", currLkupTableID));

			if (success)
				success = message.save();
			else
				LogManager.getInstance().info("Failed to delete the mm_field_lkup_table with lkup_table_id = " + currLkupTableID);

			if (success)
				MetrixDatabaseManager.setTransactionSuccessful();

			returnValue = success;
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
			returnValue = false;
		} finally {
			MetrixDatabaseManager.endTransaction();
		}

		return returnValue;
	}

	private void setSelectedItemToDelete(HashMap<String, String> item) {
		mSelectedItemToDelete = item;
	}

	public static class LookupTableListAdapter extends DynamicListAdapter {
		static ViewHolder holder;
		private Bitmap mNewAddedIcon, mOldAddedIcon, mBlankIcon;

		public LookupTableListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
			super(context, table, listViewItemResourceID, lviElemResIDs);

			mNewAddedIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.circle_blue16x16"));
			mOldAddedIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.circle_gray16x16"));
			mBlankIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.transparent"));
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);
				holder = new ViewHolder();
				holder.mTableName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_table__table_name"));
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_table__metrix_row_id"));
				holder.mLkupTableID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_table__lkup_table_id"));
				holder.mIsBaseline = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_table__is_baseline"));
				holder.mAddedIcon = (ImageView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.is_added_icon"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mTableName.setText(dataRow.get("mm_field_lkup_table.table_name"));
			holder.mMetrixRowID.setText(dataRow.get("mm_field_lkup_table.metrix_row_id"));
			holder.mLkupTableID.setText(dataRow.get("mm_field_lkup_table.lkup_table_id"));

			boolean isBaseline = Boolean.valueOf(dataRow.get("mm_field_lkup_table.is_baseline"));
			holder.mIsBaseline.setText(String.valueOf(isBaseline));

			boolean isAddedThisRev = Boolean.valueOf(dataRow.get("mm_field_lkup_table.added_this_revision"));

			// determine which added state icon to use
			if (isBaseline) {
				holder.mAddedIcon.setImageBitmap(mBlankIcon);
			} else if (isAddedThisRev) {
				holder.mAddedIcon.setImageBitmap(mNewAddedIcon);
			} else {
				holder.mAddedIcon.setImageBitmap(mOldAddedIcon);
			}

			return vi;
		}

		static class ViewHolder {
			TextView mTableName;
			TextView mMetrixRowID;
			TextView mLkupTableID;
			TextView mIsBaseline;
			ImageView mAddedIcon;
		}
	}
}
