package com.metrix.architecture.designer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global.ActivityType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class MetrixDesignerRevisionActivity extends MetrixDesignerActivity implements OnItemClickListener, OnItemLongClickListener {
	private ListView mListView;
	private AlertDialog mAddRevisionAlert, mGetRevisionAlert;
	private Button mAddRevisionButton;
	private RevisionListAdapter mRevisionAdapter;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private MetrixDesignerResourceData mRevisionResourceData;
	private static String mNoDescString, mSelectedRevisionID;
	private static TextView mRevDesc;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mRevisionResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerRevisionActivityResourceData");

		setContentView(mRevisionResourceData.LayoutResourceID);

		mListView = (ListView) findViewById(mRevisionResourceData.ListViewResourceID);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mRevisionResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mAddRevisionButton = (Button) findViewById(mRevisionResourceData.getExtraResourceID("R.id.add_revision"));
		mAddRevisionButton.setOnClickListener(this);

		mNoDescString = AndroidResourceHelper.getMessage("NoDescriptionFound");

		TextView mRev = (TextView) findViewById(mRevisionResourceData.getExtraResourceID("R.id.revisions"));
		TextView mDesiRev = (TextView) findViewById(mRevisionResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_revision"));

		AndroidResourceHelper.setResourceValues(mRev, "Revisions");
		AndroidResourceHelper.setResourceValues(mDesiRev, "ScnInfoMxDesRev");
		AndroidResourceHelper.setResourceValues(mAddRevisionButton, "AddRevision");

		populateList();

		if (this.getIntent().getExtras().containsKey("targetDesignerActivity") && !mProcessedTargetIntent) {
			String revisionId = (String) this.getIntent().getExtras().get("targetDesignerRevisionID");
			String revisionNumber = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_number", "revision_id = " + revisionId);
			String revisionStatus = MetrixDatabaseManager.getFieldStringValue("mm_revision", "status", "revision_id = " + revisionId);
			revisionNumber = AndroidResourceHelper.getMessage("Rev1Args", revisionNumber);

			MetrixCurrentKeysHelper.setKeyValue("mm_revision", "revision_id", revisionId);
			MetrixCurrentKeysHelper.setKeyValue("mm_revision", "status", revisionStatus);
			String headingText = mHeadingText + " (" + revisionNumber + ")";

			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerCategoriesActivity.class);
			intent.putExtra("headingText", headingText);

			intent.putExtra("targetDesignerActivity", (String) this.getIntent().getExtras().get("targetDesignerActivity"));
			if (this.getIntent().getExtras().containsKey("targetDesignerScreenID")) {
				intent.putExtra("targetDesignerScreenID", (String) this.getIntent().getExtras().get("targetDesignerScreenID"));
				intent.putExtra("targetDesignerScreenName", (String) this.getIntent().getExtras().get("targetDesignerScreenName"));
			}

			mProcessedTargetIntent = true;

			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	@Override
	protected void bindService() {
		bindService(new Intent(MetrixDesignerRevisionActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	@Override
	protected void unbindService() {
		if (mIsBound) {
			try {
				if (service != null) {
					service.removeListener(listener);
					unbindService(mConnection);
				}
			} catch (Exception ex) {
				LogManager.getInstance().error(ex);
			} finally {
				mIsBound = false;
			}
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			try {
				service = (IPostMonitor) binder;
				service.registerListener(listener);
			} catch (Throwable t) {
				LogManager.getInstance().error(t);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			service = null;
		}
	};

	protected com.metrix.architecture.services.IPostListener listener = new com.metrix.architecture.services.IPostListener() {
		public void newSyncStatus(final ActivityType activityType, final String message) {
			runOnUiThread(new Runnable() {
				public void run() {
					if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMR\":null}") || MetrixStringHelper.valueIsEqual(message, "{\"END_GMRV\":null}")) {
						if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMR\":null}")) {
							// get the revision_id of the generated revision ... otherwise, if just retrieving, mSelectedRevisionID already has a good value
							mSelectedRevisionID = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_id", String.format("design_set_id = %s order by revision_id desc limit 1", MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id")));
						}
						String revisionStatus = MetrixDatabaseManager.getFieldStringValue("mm_revision", "status", "revision_id = " + mSelectedRevisionID);
						MetrixCurrentKeysHelper.setKeyValue("mm_revision", "revision_id", mSelectedRevisionID);
						MetrixCurrentKeysHelper.setKeyValue("mm_revision", "status", revisionStatus);

						MobileApplication.stopSync(mCurrentActivity);
						MobileApplication.startSync(mCurrentActivity);
						mUIHelper.dismissLoadingDialog();
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerRevisionActivity.this, MetrixDesignerCategoriesActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						// re-getting this data, as the intent has the bad habit of using previous extras
						String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
						String revNumber = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_number", "revision_id = " + mSelectedRevisionID);
						intent.putExtra("headingText", String.format("%1$s (%2$s %3$s)", designName, AndroidResourceHelper.getMessage("Rev"), revNumber));
						MetrixActivityHelper.startNewActivity(MetrixDesignerRevisionActivity.this, intent);
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void populateList() {
		// get current design_set pending revision OR any revisions to which this design is mapped in mm_design_revision_view
		String query = String.format("select mm_revision.revision_id, mm_revision.revision_number, mm_revision.status, mm_revision.description from mm_revision "
						+ "where (mm_revision.design_set_id = %1$s and mm_revision.status = 'PENDING') "
						+ "or (mm_revision.revision_id in "
						+ "(select mm_design_revision_view.revision_id from mm_design_revision_view where mm_design_revision_view.design_id = %2$s)) "
						+ "order by mm_revision.revision_number asc"
				, MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id"), MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			final String revisionText = AndroidResourceHelper.getMessage("Revision");

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_revision.revision_id", cursor.getString(0));
				row.put("mm_revision.revision_number", revisionText + " " + cursor.getString(1));
				row.put("mm_revision.status", cursor.getString(2));

				String rawText = cursor.getString(3);
				if (MetrixStringHelper.isNullOrEmpty(rawText)) {
					rawText = mNoDescString;
				} else if (rawText.length() > 40) {
					rawText = rawText.substring(0, 39) + "...";
				}
				row.put("mm_revision.description", rawText);

				table.add(row);
				cursor.moveToNext();
			}

			mRevisionAdapter = new RevisionListAdapter(this, table, mRevisionResourceData.ListViewItemResourceID, mRevisionResourceData.ExtraResourceIDs);
			mListView.setAdapter(mRevisionAdapter);
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
		if (viewId == mRevisionResourceData.getExtraResourceID("R.id.add_revision")) {
			// first, make sure there are no PENDING revisions already ... if so, WARN and STOP
			String designSetID = MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id");
			int pendingCount = MetrixDatabaseManager.getCount("mm_revision", "status = 'PENDING' and design_set_id = " + designSetID);
			if (pendingCount > 0) {
				Toast.makeText(this, AndroidResourceHelper.getMessage("PendingRevisionAlreadyExists"), Toast.LENGTH_LONG).show();
				return;
			}

			// if we get through validation, inflate (zzmd_revision_add_dialog) as an AlertDialog
			AlertDialog.Builder addRevBuilder = new AlertDialog.Builder(this);
			LayoutInflater inflater = this.getLayoutInflater();
			View inflatedView = inflater.inflate(mRevisionResourceData.getExtraResourceID("R.layout.zzmd_revision_add_dialog"), null);
			addRevBuilder.setView(inflatedView);
			mRevDesc = (TextView) inflatedView.findViewById(mRevisionResourceData.getExtraResourceID("R.id.revision_add_description"));
			final TextView dialogTitle = (TextView) inflatedView.findViewById(mRevisionResourceData.getExtraResourceID("R.id.revision_add_dialog_title"));
			final TextView dialogDescription = (TextView) inflatedView.findViewById(mRevisionResourceData.getExtraResourceID("R.id.revision_add_dialog_description"));
			AndroidResourceHelper.setResourceValues(dialogTitle, "RevisionAddDescription");
			AndroidResourceHelper.setResourceValues(dialogDescription, "RevisionAddInstructions");

			mAddRevisionAlert = addRevBuilder.create();
			mAddRevisionAlert.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("OK"), addRevisionListener);
			mAddRevisionAlert.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("Cancel"), addRevisionListener);
			mAddRevisionAlert.show();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = mRevisionAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;
		String revisionId = selectedItem.get("mm_revision.revision_id");

		if (!subordinateRecordsExistForRevision(revisionId)) {
			mSelectedRevisionID = revisionId;
			mGetRevisionAlert = new AlertDialog.Builder(MetrixDesignerRevisionActivity.this).create();
			mGetRevisionAlert.setMessage(AndroidResourceHelper.getMessage("GetRevisionConfirm"));
			mGetRevisionAlert.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), getRevisionListener);
			mGetRevisionAlert.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), getRevisionListener);
			mGetRevisionAlert.show();
		} else {
			String revisionNumber = selectedItem.get("mm_revision.revision_number");
			String revisionStatus = selectedItem.get("mm_revision.status");
			revisionNumber = revisionNumber.replace(AndroidResourceHelper.getMessage("Revision"), AndroidResourceHelper.getMessage("Rev"));
			MetrixCurrentKeysHelper.setKeyValue("mm_revision", "revision_id", revisionId);
			MetrixCurrentKeysHelper.setKeyValue("mm_revision", "status", revisionStatus);
			String headingText = mHeadingText + " (" + revisionNumber + ")";

			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerCategoriesActivity.class);
			intent.putExtra("headingText", headingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
		Object item = mRevisionAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;
		String revisionId = selectedItem.get("mm_revision.revision_id");
		mSelectedRevisionID = revisionId;
		mGetRevisionAlert = new AlertDialog.Builder(MetrixDesignerRevisionActivity.this).create();
		mGetRevisionAlert.setMessage(AndroidResourceHelper.getMessage("RefreshRevisionConfirm"));
		mGetRevisionAlert.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), getRevisionListener);
		mGetRevisionAlert.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), getRevisionListener);
		mGetRevisionAlert.show();

		return true;
	}

	private boolean subordinateRecordsExistForRevision(String revisionId) {
		String whereClause = String.format("revision_id = %s", revisionId);
		int wfCount = MetrixDatabaseManager.getCount("mm_workflow", whereClause);
		int screenCount = MetrixDatabaseManager.getCount("mm_screen", whereClause);
		int homeCount = MetrixDatabaseManager.getCount("mm_home_item", whereClause);
		int menuCount = MetrixDatabaseManager.getCount("mm_menu_item", whereClause);

		return (wfCount > 0 && screenCount > 0 && homeCount > 0 && menuCount > 0);
	}

	DialogInterface.OnClickListener addRevisionListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // OK
					// wire up OK button to call perform_generate_mobile_revision
					if (SettingsHelper.getSyncPause(mCurrentActivity))
					{
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
						if (syncPauseAlertDialog != null) {
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
									startAddRevisionListener();
								}
							});
						}
					} else
						startAddRevisionListener();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // Cancel (do nothing)
					break;
			}
		}
	};

	private void startAddRevisionListener() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				if (doRevisionAddition(mCurrentActivity) == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					MetrixDesignerRevisionActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();

							if (mAddRevisionAlert != null) {
								mAddRevisionAlert.dismiss();
							}
						}
					});
					return;
				}

				if (mAddRevisionAlert != null) {
					mAddRevisionAlert.dismiss();
				}

				// start waiting dialog on-screen
				mUIHelper = new MetrixUIHelper(mCurrentActivity);
				mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddRevisionInProgress"));
			}
		});

		thread.start();
	}

	DialogInterface.OnClickListener getRevisionListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // OK
					// wire up OK button to call perform_get_mobile_revision
					if (SettingsHelper.getSyncPause(mCurrentActivity))
					{
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
						if (syncPauseAlertDialog != null) {
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
									startGetRevisionListener();
								}
							});
						}
					} else
						startGetRevisionListener();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // Cancel (do nothing)
					break;
			}
		}
	};

	private void startGetRevisionListener() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				// we might be doing a refresh, so clear old data
				clearRevisionMetadata(mSelectedRevisionID);

				if (doRevisionGet(mCurrentActivity) == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					MetrixDesignerRevisionActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();

							if (mGetRevisionAlert != null) {
								mGetRevisionAlert.dismiss();
							}
						}
					});
					return;
				}

				if (mGetRevisionAlert != null) {
					mGetRevisionAlert.dismiss();
				}

				// start waiting dialog on-screen
				mUIHelper = new MetrixUIHelper(mCurrentActivity);
				mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("GetRevisionInProgress"));
			}
		});

		thread.start();
	}

	public static boolean doRevisionAddition(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		try {
			Hashtable<String, String> params = new Hashtable<String, String>();
			String designSetID = MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id");
			int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

			params.put("design_set_id", designSetID);
			params.put("device_sequence", String.valueOf(device_id));
			String description = mRevDesc.getText().toString();
			if (!MetrixStringHelper.isNullOrEmpty(description)) {
				params.put("description", description);
			}

			MetrixPerformMessage performGMR = new MetrixPerformMessage("perform_generate_mobile_revision", params);
			performGMR.save();
		}
		catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return true;
	}

	public static boolean doRevisionGet(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		try {
			Hashtable<String, String> params = new Hashtable<String, String>();
			int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

			params.put("revision_id", mSelectedRevisionID);
			params.put("device_sequence", String.valueOf(device_id));

			MetrixPerformMessage performGMR = new MetrixPerformMessage("perform_get_mobile_revision", params);
			performGMR.save();
		}
		catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return true;
	}

	public static class RevisionListAdapter extends DynamicListAdapter {
		static ViewHolder holder;

		public RevisionListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
			super(context, table, listViewItemResourceID, lviElemResIDs);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);
				holder = new ViewHolder();
				holder.mRevisionID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_revision__revision_id"));
				holder.mRevisionNumber = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_revision__revision_number"));
				holder.mStatus = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_revision__status"));
				holder.mDescription = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_revision__description"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mRevisionID.setText(dataRow.get("mm_revision.revision_id"));
			holder.mRevisionNumber.setText(dataRow.get("mm_revision.revision_number"));
			holder.mStatus.setText(dataRow.get("mm_revision.status"));

			String currentDescText = dataRow.get("mm_revision.description");
			holder.mDescription.setText(currentDescText);
			if (MetrixStringHelper.valueIsEqual(currentDescText, mNoDescString)) {
				holder.mDescription.setVisibility(View.GONE);
			} else {
				holder.mDescription.setVisibility(View.VISIBLE);
			}

			return vi;
		}

		static class ViewHolder {
			TextView mRevisionID;
			TextView mRevisionNumber;
			TextView mStatus;
			TextView mDescription;
		}
	}
}

