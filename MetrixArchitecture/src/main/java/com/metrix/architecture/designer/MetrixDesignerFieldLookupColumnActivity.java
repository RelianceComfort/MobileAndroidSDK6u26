package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

public class MetrixDesignerFieldLookupColumnActivity extends MetrixDesignerActivity implements OnItemClickListener {
	private TextView mEmphasis;
	private Button mAddColumn, mColumnOrder;
	private String mScreenName, mFieldName, mTableName;
	private ListView mListView;
	private AlertDialog mDeleteLookupColumnDialog;
	private HashMap<String, String> mSelectedItemToDelete;
	private LookupColumnListAdapter mLookupColumnAdapter;
	private MetrixDesignerResourceData mLookupColumnResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLookupColumnResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupColumnActivityResourceData");

		setContentView(mLookupColumnResourceData.LayoutResourceID);

		mListView = (ListView) findViewById(mLookupColumnResourceData.ListViewResourceID);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mLookupColumnResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		mTableName = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup_table", "table_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mLookupColumnResourceData.getExtraResourceID("R.id.zzmd_field_lookup_column_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupCol", mTableName, mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mAddColumn = (Button) findViewById(mLookupColumnResourceData.getExtraResourceID("R.id.add_column"));
		mAddColumn.setEnabled(mAllowChanges);
		mAddColumn.setOnClickListener(this);

		mColumnOrder = (Button) findViewById(mLookupColumnResourceData.getExtraResourceID("R.id.column_order"));
		mColumnOrder.setOnClickListener(this);

		TextView lkupColumn = (TextView) findViewById(mLookupColumnResourceData.getExtraResourceID("R.id.lookup_columns"));


		AndroidResourceHelper.setResourceValues(lkupColumn, "LookupColumns");
		AndroidResourceHelper.setResourceValues(mAddColumn, "AddColumn");
		AndroidResourceHelper.setResourceValues(mColumnOrder, "ColumnOrder");


		populateList();

		mListView.setOnItemClickListener(this);
		if (mAllowChanges) {
			mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
					Object item = mLookupColumnAdapter.getItem(position);
					@SuppressWarnings("unchecked")
					HashMap<String, String> selectedItem = (HashMap<String, String>) item;
					if (!mAllowChanges) {
						return false;
					} else {
						setSelectedItemToDelete(selectedItem);
						mDeleteLookupColumnDialog = new AlertDialog.Builder(MetrixDesignerFieldLookupColumnActivity.this).create();
						mDeleteLookupColumnDialog.setMessage(AndroidResourceHelper.getMessage("LookupColumnDeleteConfirm"));
						mDeleteLookupColumnDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), deleteLookupColumnListener);
						mDeleteLookupColumnDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), deleteLookupColumnListener);
						mDeleteLookupColumnDialog.show();
						return true;
					}
				}
			});
		}
	}

	private void populateList() {
		String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
		String currentLkupTableID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup_table", "lkup_table_id");

		StringBuilder query = new StringBuilder();
		query.append("select distinct mm_field_lkup_column.metrix_row_id, mm_field_lkup_column.lkup_column_id, mm_field_lkup_column.column_name, mm_field_lkup_column.created_revision_id");
		query.append(" from mm_field_lkup_column where mm_field_lkup_column.lkup_table_id = " + currentLkupTableID);
		query.append(" order by mm_field_lkup_column.column_name asc");

		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_field_lkup_column.metrix_row_id", cursor.getString(0));
				row.put("mm_field_lkup_column.lkup_column_id", cursor.getString(1));
				row.put("mm_field_lkup_column.column_name", cursor.getString(2));

				String rawCreatedRevisionID = cursor.getString(3);
				boolean isBaseline = true;
				boolean addedThisRevision = false;
				if (!MetrixStringHelper.isNullOrEmpty(rawCreatedRevisionID)) {
					isBaseline = false;
					if (MetrixStringHelper.valueIsEqual(rawCreatedRevisionID, currentRevisionID))
						addedThisRevision = true;
				}
				row.put("mm_field_lkup_column.is_baseline", String.valueOf(isBaseline));
				row.put("mm_field_lkup_column.added_this_revision", String.valueOf(addedThisRevision));

				table.add(row);
				cursor.moveToNext();
			}

			mLookupColumnAdapter = new LookupColumnListAdapter(this, table, mLookupColumnResourceData.ListViewItemResourceID, mLookupColumnResourceData.ExtraResourceIDs);
			mListView.setAdapter(mLookupColumnAdapter);
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
		if (viewId == mLookupColumnResourceData.getExtraResourceID("R.id.add_column")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupColumnAddActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		} else if (viewId == mLookupColumnResourceData.getExtraResourceID("R.id.column_order")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupColumnOrderActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = mLookupColumnAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;

		String lkupColumnID = selectedItem.get("mm_field_lkup_column.lkup_column_id");
		MetrixCurrentKeysHelper.setKeyValue("mm_field_lkup_column", "lkup_column_id", lkupColumnID);

		Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupColumnPropActivity.class);
		intent.putExtra("headingText", mHeadingText);
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	DialogInterface.OnClickListener deleteLookupColumnListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					if (deleteSelectedLookupColumnAndChildren()) {
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFieldLookupColumnActivity.this, MetrixDesignerFieldLookupColumnActivity.class);
						intent.putExtra("headingText", mHeadingText);
						MetrixActivityHelper.startNewActivityAndFinish(MetrixDesignerFieldLookupColumnActivity.this, intent);
					}
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private boolean deleteSelectedLookupColumnAndChildren() {
		boolean returnValue = true;
		try {
			boolean success = true;
			String currMetrixRowID = mSelectedItemToDelete.get("mm_field_lkup_column.metrix_row_id");
			String currLkupColumnID = mSelectedItemToDelete.get("mm_field_lkup_column.lkup_column_id");

			// generate a deletion transaction for mm_field_lkup_column ONLY
			MetrixUpdateMessage message = new MetrixUpdateMessage("mm_field_lkup_column", MetrixUpdateMessageTransactionType.Delete, "lkup_column_id", currLkupColumnID);

			MetrixDatabaseManager.begintransaction();

			// delete all child records in DB (all 3 tables), without doing a message transaction
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_column_log where metrix_row_id = %s)", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_column_log", String.format("metrix_row_id = %s", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_column", String.format("lkup_column_id = %s", currLkupColumnID));

			if (success)
				success = message.save();
			else
				LogManager.getInstance().info("Failed to delete the mm_field_lkup_column with lkup_column_id = " + currLkupColumnID);

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

	public static class LookupColumnListAdapter extends DynamicListAdapter {
		static ViewHolder holder;
		private Bitmap mNewAddedIcon, mOldAddedIcon, mBlankIcon;

		public LookupColumnListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
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
				holder.mColumnName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_column__column_name"));
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_column__metrix_row_id"));
				holder.mLkupColumnID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_column__lkup_column_id"));
				holder.mIsBaseline = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_column__is_baseline"));
				holder.mAddedIcon = (ImageView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.is_added_icon"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mColumnName.setText(dataRow.get("mm_field_lkup_column.column_name"));
			holder.mMetrixRowID.setText(dataRow.get("mm_field_lkup_column.metrix_row_id"));
			holder.mLkupColumnID.setText(dataRow.get("mm_field_lkup_column.lkup_column_id"));

			boolean isBaseline = Boolean.valueOf(dataRow.get("mm_field_lkup_column.is_baseline"));
			holder.mIsBaseline.setText(String.valueOf(isBaseline));

			boolean isAddedThisRev = Boolean.valueOf(dataRow.get("mm_field_lkup_column.added_this_revision"));

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
			TextView mColumnName;
			TextView mMetrixRowID;
			TextView mLkupColumnID;
			TextView mIsBaseline;
			ImageView mAddedIcon;
		}
	}
}

