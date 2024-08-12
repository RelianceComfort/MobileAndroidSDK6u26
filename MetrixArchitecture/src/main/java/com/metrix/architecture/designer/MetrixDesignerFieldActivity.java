package com.metrix.architecture.designer;

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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MetrixDesignerFieldActivity extends MetrixDesignerActivity implements OnItemClickListener {
	private TextView mTitle;
	private Button mAddField, mFieldOrder;
	private String mScreenName;
	private static String mNoDescString;
	private ListView mListView;
	private AlertDialog mDeleteFieldDialog;
	private HashMap<String, String> mSelectedItemToDelete;
	private FieldListAdapter mFieldAdapter;
	private MetrixDesignerResourceData mFieldResourceData;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFieldResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldActivityResourceData");

        setContentView(mFieldResourceData.LayoutResourceID);

        mListView = (ListView) findViewById(mFieldResourceData.ListViewResourceID);
    }

    @Override
    public void onStart() {
		super.onStart();

		helpText = mFieldResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mTitle = (TextView) findViewById(mFieldResourceData.getExtraResourceID("R.id.zzmd_field_title"));
        String fullTitle = AndroidResourceHelper.getMessage("Fields1Args", mScreenName);
        mTitle.setText(fullTitle);

		mAddField = (Button) findViewById(mFieldResourceData.getExtraResourceID("R.id.add_field"));
		mAddField.setEnabled(mAllowChanges);
        mAddField.setOnClickListener(this);
        AndroidResourceHelper.setResourceValues(mAddField, "AddField");

        mFieldOrder = (Button) findViewById(mFieldResourceData.getExtraResourceID("R.id.field_order"));
        mFieldOrder.setOnClickListener(this);
        AndroidResourceHelper.setResourceValues(mFieldOrder, "FieldOrder");

        TextView mScrInfo = (TextView) findViewById(mFieldResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_field"));
        AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesFld");

        mNoDescString = AndroidResourceHelper.getMessage("NoDescriptionFound");

		populateList();

		if (this.getIntent().getExtras().containsKey("targetDesignerActivity") && !mProcessedTargetIntent) {
			// if we get here, we have to go to MetrixDesignerFieldOrderActivity		
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldOrderActivity.class);
            intent.putExtra("headingText", mHeadingText);

			mProcessedTargetIntent = true;

			MetrixActivityHelper.startNewActivity(this, intent);
		}

        mListView.setOnItemClickListener(this);
        if (mAllowChanges) {
            mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
					Object item = mFieldAdapter.getItem(position);
					@SuppressWarnings("unchecked")
					HashMap<String, String> selectedItem = (HashMap<String, String>) item;

					boolean isBaseline = Boolean.valueOf(selectedItem.get("mm_field.is_baseline"));
					if (isBaseline || !mAllowChanges) {
						return false;
					} else {
						if (thisFieldIsALookupLinkedFieldForAnotherField(selectedItem.get("mm_field.field_id"))) {
                            Toast.makeText(MetrixDesignerFieldActivity.this, AndroidResourceHelper.getMessage("CannotDeleteFieldLinkedLookup"), Toast.LENGTH_LONG).show();
                            return false;
						}

						setSelectedItemToDelete(selectedItem);
						mDeleteFieldDialog = new AlertDialog.Builder(MetrixDesignerFieldActivity.this).create();
                        mDeleteFieldDialog.setMessage(AndroidResourceHelper.getMessage("FieldDeleteConfirm"));
                        mDeleteFieldDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), deleteFieldListener);
                        mDeleteFieldDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), deleteFieldListener);
                        mDeleteFieldDialog.show();
                        return true;
                    }
                }
			});
        }
    }

	private void populateList() {
		String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
        String currentScreenID = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id");

		// now, with tracking data in place, do the actual list selection
		StringBuilder query = new StringBuilder();
		query.append("select distinct mm_field.metrix_row_id, mm_field.field_id, mm_field.table_name, mm_field.column_name, mm_field.description, mm_field.created_revision_id, mm_field.modified_revision_id, mm_field.control_type");
		query.append(" from mm_field where mm_field.screen_id = " + currentScreenID);
		query.append(" order by mm_field.table_name, mm_field.column_name asc");

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
				String fieldID = cursor.getString(1);
				row.put("mm_field.field_id", fieldID);

				String tableName = cursor.getString(2);
				String colName = cursor.getString(3);
				row.put("mm_field.field_name", tableName + "." + colName);

                String rawText = cursor.getString(4);
				if (MetrixStringHelper.isNullOrEmpty(rawText)) {
					rawText = mNoDescString;
				} else if (rawText.length() > 40) {
					rawText = rawText.substring(0, 39) + "...";
				}
                row.put("mm_field.description", rawText);

				String rawCreatedRevisionID = cursor.getString(5);
				boolean isBaseline = true;
				boolean addedThisRevision = false;
				if (!MetrixStringHelper.isNullOrEmpty(rawCreatedRevisionID)) {
                    isBaseline = false;
                    if (MetrixStringHelper.valueIsEqual(rawCreatedRevisionID, currentRevisionID))
						addedThisRevision = true;
                }
                row.put("mm_field.is_baseline", String.valueOf(isBaseline));
				row.put("mm_field.added_this_revision", String.valueOf(addedThisRevision));

				String rawModifiedRevisionID = cursor.getString(6);
				boolean editedPrevRevision = false;
				boolean editedThisRevision = false;
				if (!MetrixStringHelper.isNullOrEmpty(rawModifiedRevisionID)) {
					if (MetrixStringHelper.valueIsEqual(rawModifiedRevisionID, currentRevisionID))
						editedThisRevision = true;
					else
						editedPrevRevision = true;
                }
                row.put("mm_field.edited_prev_revision", String.valueOf(editedPrevRevision));
                row.put("mm_field.edited_this_revision", String.valueOf(editedThisRevision));

				int lkupCount = MetrixDatabaseManager.getCount("mm_field_lkup", "field_id = " + fieldID);
				row.put("mm_field.has_lookup", String.valueOf((lkupCount > 0)));

				String controlType = cursor.getString(7);
				row.put("mm_field.control_type", controlType);
				row.put("mm_field.is_button_field", String.valueOf(!MetrixStringHelper.isNullOrEmpty(controlType) &&
                        MetrixStringHelper.valueIsEqual(controlType.toUpperCase(), "BUTTON")));

				table.add(row);
				cursor.moveToNext();
			}

            mFieldAdapter = new FieldListAdapter(this, table, mFieldResourceData.ListViewItemResourceID, mFieldResourceData.ExtraResourceIDs);
			mListView.setAdapter(mFieldAdapter);
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
        if (viewId == mFieldResourceData.getExtraResourceID("R.id.add_field")) {
            Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldAddActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		} else if (viewId == mFieldResourceData.getExtraResourceID("R.id.field_order")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldOrderActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = mFieldAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;

        String fieldId = selectedItem.get("mm_field.field_id");
		String fieldName = selectedItem.get("mm_field.field_name");
		MetrixCurrentKeysHelper.setKeyValue("mm_field", "field_id", fieldId);
		MetrixCurrentKeysHelper.setKeyValue("mm_field", "field_name", fieldName);
		String controlType = selectedItem.get("mm_field.control_type");
		MetrixCurrentKeysHelper.setKeyValue("mm_field", "control_type", controlType);

        Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldPropActivity.class);
		intent.putExtra("headingText", mHeadingText);
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	private boolean thisFieldIsALookupLinkedFieldForAnotherField(String fieldID) {
		int linkCount = MetrixDatabaseManager.getCount("mm_field_lkup_column", String.format("linked_field_id = %1$s and lkup_table_id not in (select lkup_table_id from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id = %2$s))", fieldID, fieldID));
		return linkCount > 0;
	}

    DialogInterface.OnClickListener deleteFieldListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
                case DialogInterface.BUTTON_POSITIVE:    // Yes
                    if (deleteSelectedFieldAndChildren()) {
                        Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFieldActivity.this, MetrixDesignerFieldActivity.class);
                        intent.putExtra("headingText", mHeadingText);
                        MetrixActivityHelper.startNewActivityAndFinish(MetrixDesignerFieldActivity.this, intent);
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
                    break;
            }
		}
	};

    private boolean deleteSelectedFieldAndChildren() {
		boolean returnValue = true;
		try {
			boolean success = true;
			String currMetrixRowID = mSelectedItemToDelete.get("mm_field.metrix_row_id");
			String currFieldID = mSelectedItemToDelete.get("mm_field.field_id");

            // generate a deletion transaction for mm_field ONLY
			MetrixUpdateMessage message = new MetrixUpdateMessage("mm_field", MetrixUpdateMessageTransactionType.Delete, "field_id", currFieldID);

			MetrixDatabaseManager.begintransaction();

            // delete all child records in DB, without doing a message transaction
            if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_column_log where metrix_row_id in (select metrix_row_id from mm_field_lkup_column where lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id = %s))))", currFieldID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_column_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup_column where lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id = %s)))", currFieldID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_column", String.format("lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id = %s))", currFieldID));

            if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_table_log where metrix_row_id in (select metrix_row_id from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id = %s)))", currFieldID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_table_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id = %s))", currFieldID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_table", String.format("lkup_id in (select lkup_id from mm_field_lkup where field_id = %s)", currFieldID));

            if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_filter_log where metrix_row_id in (select metrix_row_id from mm_field_lkup_filter where lkup_id in (select lkup_id from mm_field_lkup where field_id = %s)))", currFieldID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_filter_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup_filter where lkup_id in (select lkup_id from mm_field_lkup where field_id = %s))", currFieldID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_filter", String.format("lkup_id in (select lkup_id from mm_field_lkup where field_id = %s)", currFieldID));

            if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_orderby_log where metrix_row_id in (select metrix_row_id from mm_field_lkup_orderby where lkup_id in (select lkup_id from mm_field_lkup where field_id = %s)))", currFieldID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_orderby_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup_orderby where lkup_id in (select lkup_id from mm_field_lkup where field_id = %s))", currFieldID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_orderby", String.format("lkup_id in (select lkup_id from mm_field_lkup where field_id = %s)", currFieldID));

            if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_log where metrix_row_id in (select metrix_row_id from mm_field_lkup where field_id = %s))", currFieldID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup where field_id = %s)", currFieldID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup", String.format("field_id = %s", currFieldID));

            if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_log where metrix_row_id = %s)", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_log", String.format("metrix_row_id = %s", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field", String.format("field_id = %s", currFieldID));

            if (success)
				success = message.save();
			else
                LogManager.getInstance().info("Failed to delete the mm_field with field_id = " + currFieldID);

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

    public static class FieldListAdapter extends DynamicListAdapter {
		static ViewHolder holder;
		private Bitmap mNewAddedIcon, mOldAddedIcon, mNewEditedIcon, mOldEditedIcon, mBlankIcon;

		public FieldListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
			super(context, table, listViewItemResourceID, lviElemResIDs);

            mNewAddedIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.circle_blue16x16"));
			mOldAddedIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.circle_gray16x16"));
			mNewEditedIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.pencil_purple16x16"));
			mOldEditedIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.pencil_gray16x16"));
			mBlankIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.transparent"));
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);
				holder = new ViewHolder();
				holder.mName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field__field_name"));
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field__metrix_row_id"));
				holder.mFieldID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field__field_id"));
				holder.mDescription = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field__description"));
				holder.mIsBaseline = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field__is_baseline"));
				holder.mAddedIcon = (ImageView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.is_added_icon"));
				holder.mEditedIcon = (ImageView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.is_edited_icon"));
				holder.mHasLookupIcon = (ImageView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.has_lookup_icon"));
				holder.mIsButtonFieldIcon = (ImageView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.is_button_field_icon"));

                vi.setTag(holder);
            } else {
				holder = (ViewHolder) vi.getTag();
			}

            HashMap<String, String> dataRow = mListData.get(position);
            holder.mName.setText(dataRow.get("mm_field.field_name"));
			holder.mMetrixRowID.setText(dataRow.get("mm_field.metrix_row_id"));
			holder.mFieldID.setText(dataRow.get("mm_field.field_id"));

            String currentDescText = dataRow.get("mm_field.description");
			holder.mDescription.setText(currentDescText);
			if (MetrixStringHelper.valueIsEqual(currentDescText, mNoDescString)) {
				holder.mDescription.setVisibility(View.GONE);
			} else {
				holder.mDescription.setVisibility(View.VISIBLE);
			}

            boolean isBaseline = Boolean.valueOf(dataRow.get("mm_field.is_baseline"));
			holder.mIsBaseline.setText(String.valueOf(isBaseline));

            boolean isAddedThisRev = Boolean.valueOf(dataRow.get("mm_field.added_this_revision"));
			boolean isEditedThisRev = Boolean.valueOf(dataRow.get("mm_field.edited_this_revision"));
			boolean isEditedPrevRev = Boolean.valueOf(dataRow.get("mm_field.edited_prev_revision"));
			boolean hasLookup = Boolean.valueOf(dataRow.get("mm_field.has_lookup"));
			boolean isButtonField = Boolean.valueOf(dataRow.get("mm_field.is_button_field"));

            // determine which added state icon to use
			if (isBaseline) {
				holder.mAddedIcon.setImageBitmap(mBlankIcon);
			} else if (isAddedThisRev) {
				holder.mAddedIcon.setImageBitmap(mNewAddedIcon);
			} else {
				holder.mAddedIcon.setImageBitmap(mOldAddedIcon);
			}

            // determine which edited state icon to use
			if (isEditedThisRev) {
				holder.mEditedIcon.setImageBitmap(mNewEditedIcon);
			} else if (isEditedPrevRev) {
				holder.mEditedIcon.setImageBitmap(mOldEditedIcon);
			} else {
				holder.mEditedIcon.setImageBitmap(mBlankIcon);
			}

            // determine whether to show has lookup icon
			if (hasLookup)
				holder.mHasLookupIcon.setVisibility(View.VISIBLE);
			else
				holder.mHasLookupIcon.setVisibility(View.GONE);

			// determine whether to show is button icon
			if (isButtonField)
				holder.mIsButtonFieldIcon.setVisibility(View.VISIBLE);
			else
				holder.mIsButtonFieldIcon.setVisibility(View.GONE);

            return vi;
		}

		static class ViewHolder {
			TextView mName;
			TextView mMetrixRowID;
			TextView mFieldID;
			TextView mDescription;
			TextView mIsBaseline;
			ImageView mAddedIcon;
			ImageView mEditedIcon;
			ImageView mHasLookupIcon;
			ImageView mIsButtonFieldIcon;
		}
	}
}

