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

public class MetrixDesignerWorkflowActivity extends MetrixDesignerActivity implements OnItemClickListener {
	private ListView mListView;
	private AlertDialog mDeleteWorkflowDialog;
	private HashMap<String, String> mSelectedItemToDelete;
	private static String mNoDescString;
	private Button mAddWorkflow;
	private WorkflowListAdapter mWorkflowAdapter;
	private MetrixDesignerResourceData mWorkflowResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mWorkflowResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerWorkflowActivityResourceData");

		setContentView(mWorkflowResourceData.LayoutResourceID);

		mListView = (ListView) findViewById(mWorkflowResourceData.ListViewResourceID);
		mListView.setOnItemClickListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mWorkflowResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mAddWorkflow = (Button) findViewById(mWorkflowResourceData.getExtraResourceID("R.id.add_workflow"));
		mAddWorkflow.setEnabled(mAllowChanges);
		mAddWorkflow.setOnClickListener(this);

		mNoDescString = AndroidResourceHelper.getMessage("NoDescriptionFound");

		TextView mWrkFlw = (TextView) findViewById(mWorkflowResourceData.getExtraResourceID("R.id.workflows"));
		TextView mScrInfo = (TextView) findViewById(mWorkflowResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_workflow"));

		AndroidResourceHelper.setResourceValues(mWrkFlw, "Workflows");
		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesWf");
		AndroidResourceHelper.setResourceValues(mAddWorkflow, "AddWorkflow");

		populateList();

		if (mAllowChanges) {
			mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
					Object item = mWorkflowAdapter.getItem(position);
					@SuppressWarnings("unchecked")
					HashMap<String, String> selectedItem = (HashMap<String, String>) item;

					boolean isBaseline = Boolean.valueOf(selectedItem.get("mm_workflow.is_baseline"));
					if (isBaseline || !mAllowChanges) {
						return false;
					} else {
						setSelectedItemToDelete(selectedItem);
						mDeleteWorkflowDialog = new AlertDialog.Builder(MetrixDesignerWorkflowActivity.this).create();
						mDeleteWorkflowDialog.setMessage(AndroidResourceHelper.getMessage("WorkflowDeleteConfirm"));
						mDeleteWorkflowDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), deleteWorkflowListener);
						mDeleteWorkflowDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), deleteWorkflowListener);
						mDeleteWorkflowDialog.show();
						return true;
					}
				}
			});
		}
	}

	private void populateList() {
		String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");

		StringBuilder query = new StringBuilder();
		query.append("select mm_workflow.metrix_row_id, mm_workflow.workflow_id, replace(mm_workflow.name, '~', ' '),");
		query.append(" mm_workflow.description, mm_workflow.created_revision_id from mm_workflow");
		query.append(" where mm_workflow.revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
		query.append(" and mm_workflow.design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
		query.append(" order by mm_workflow.name asc");

		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_workflow.metrix_row_id", cursor.getString(0));
				row.put("mm_workflow.workflow_id", cursor.getString(1));
				row.put("mm_workflow.name", cursor.getString(2));

				String rawText = cursor.getString(3);
				if (MetrixStringHelper.isNullOrEmpty(rawText)) {
					rawText = mNoDescString;
				} else if (rawText.length() > 60) {
					rawText = rawText.substring(0, 59) + "...";
				}
				row.put("mm_workflow.description", rawText);

				String rawCreatedRevisionID = cursor.getString(4);
				boolean isBaseline = true;
				boolean addedThisRevision = false;
				if (!MetrixStringHelper.isNullOrEmpty(rawCreatedRevisionID)) {
					isBaseline = false;
					if (MetrixStringHelper.valueIsEqual(rawCreatedRevisionID, currentRevisionID))
						addedThisRevision = true;
				}
				row.put("mm_workflow.is_baseline", String.valueOf(isBaseline));
				row.put("mm_workflow.added_this_revision", String.valueOf(addedThisRevision));

				table.add(row);
				cursor.moveToNext();
			}

			mWorkflowAdapter = new WorkflowListAdapter(this, table, mWorkflowResourceData.ListViewItemResourceID, mWorkflowResourceData.ExtraResourceIDs);
			mListView.setAdapter(mWorkflowAdapter);
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
		if (viewId == mWorkflowResourceData.getExtraResourceID("R.id.add_workflow")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerWorkflowAddActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = mWorkflowAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;

		String workflowId = selectedItem.get("mm_workflow.workflow_id");
		String workflowName = selectedItem.get("mm_workflow.name");
		MetrixCurrentKeysHelper.setKeyValue("mm_workflow", "workflow_id", workflowId);
		MetrixCurrentKeysHelper.setKeyValue("mm_workflow", "name", workflowName);

		Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerWorkflowScreenEnablingActivity.class);
		intent.putExtra("headingText", mHeadingText);
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	DialogInterface.OnClickListener deleteWorkflowListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					if (deleteSelectedWorkflowAndChildren()) {
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerWorkflowActivity.this, MetrixDesignerWorkflowActivity.class);
						intent.putExtra("headingText", mHeadingText);
						MetrixActivityHelper.startNewActivityAndFinish(MetrixDesignerWorkflowActivity.this, intent);
					}
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private boolean deleteSelectedWorkflowAndChildren() {
		boolean returnValue = true;
		try {
			boolean success = true;
			String currMetrixRowID = mSelectedItemToDelete.get("mm_workflow.metrix_row_id");
			String currWorkflowID = mSelectedItemToDelete.get("mm_workflow.workflow_id");

			// generate a deletion transaction for mm_workflow ONLY
			MetrixUpdateMessage message = new MetrixUpdateMessage("mm_workflow", MetrixUpdateMessageTransactionType.Delete, "workflow_id", currWorkflowID);

			MetrixDatabaseManager.begintransaction();

			// delete all child records of all kinds in DB (all 3 tables), without doing a message transaction

			// WORKFLOW SCREEN TABLES
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_workflow_screen_log where metrix_row_id in (select metrix_row_id from mm_workflow_screen where workflow_id = %s))", currWorkflowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_workflow_screen_log", String.format("metrix_row_id in (select metrix_row_id from mm_workflow_screen where workflow_id = %s)", currWorkflowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_workflow_screen", String.format("workflow_id = %s", currWorkflowID));

			// WORKFLOW TABLES
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_workflow_log where metrix_row_id = %s)", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_workflow_log", String.format("metrix_row_id = %s", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_workflow", String.format("workflow_id = %s", currWorkflowID));

			// if all of the above worked, send the message.
			if (success)
				success = message.save();
			else
				LogManager.getInstance().info("Failed to delete the mm_workflow with workflow_id = " + currWorkflowID);

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

	public static class WorkflowListAdapter extends DynamicListAdapter {
		static ViewHolder holder;
		private Bitmap mNewAddedIcon, mOldAddedIcon, mBlankIcon;

		public WorkflowListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
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
				holder.mName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_workflow__name"));
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_workflow__metrix_row_id"));
				holder.mWorkflowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_workflow__workflow_id"));
				holder.mDescription = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_workflow__description"));
				holder.mIsBaseline = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_workflow__is_baseline"));
				holder.mAddedIcon = (ImageView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.is_added_icon"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mName.setText(dataRow.get("mm_workflow.name"));
			holder.mMetrixRowID.setText(dataRow.get("mm_workflow.metrix_row_id"));
			holder.mWorkflowID.setText(dataRow.get("mm_workflow.workflow_id"));

			String currentDescText = dataRow.get("mm_workflow.description");
			holder.mDescription.setText(currentDescText);
			if (MetrixStringHelper.valueIsEqual(currentDescText, mNoDescString)) {
				holder.mDescription.setVisibility(View.GONE);
			} else {
				holder.mDescription.setVisibility(View.VISIBLE);
			}

			boolean isBaseline = Boolean.valueOf(dataRow.get("mm_workflow.is_baseline"));
			holder.mIsBaseline.setText(String.valueOf(isBaseline));

			boolean isAddedThisRev = Boolean.valueOf(dataRow.get("mm_workflow.added_this_revision"));

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
			TextView mName;
			TextView mMetrixRowID;
			TextView mWorkflowID;
			TextView mDescription;
			TextView mIsBaseline;
			ImageView mAddedIcon;
		}
	}
}

