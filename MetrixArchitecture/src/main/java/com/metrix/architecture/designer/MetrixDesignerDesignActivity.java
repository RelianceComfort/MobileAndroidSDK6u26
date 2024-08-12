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
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class MetrixDesignerDesignActivity extends MetrixDesignerActivity implements OnItemClickListener, OnItemLongClickListener {
	private ListView mListView;
	private AlertDialog mGetDesignAlert;
	private Button mAddDesignButton;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private DesignListAdapter mDesignAdapter;
	private MetrixDesignerResourceData mDesignResourceData;
	private static String mSelectedDesignSetID, mSelectedDesignID;
	private List<ResourceValueObject> resourceStrings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDesignResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerDesignActivityResourceData");

		setContentView(mDesignResourceData.LayoutResourceID);

		mListView = (ListView) findViewById(mDesignResourceData.ListViewResourceID);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mDesignResourceData.HelpTextString;

		mAddDesignButton = (Button) findViewById(mDesignResourceData.getExtraResourceID("R.id.add_design"));
		mAddDesignButton.setOnClickListener(this);
		AndroidResourceHelper.setResourceValues(mAddDesignButton, "AddDesign");

		populateList();

		if (this.getIntent().getExtras().containsKey("targetDesignerActivity") && !mProcessedTargetIntent) {
			String designSetId = (String) this.getIntent().getExtras().get("targetDesignerDesignSetID");
			String designId = (String) this.getIntent().getExtras().get("targetDesignerDesignID");

			MetrixCurrentKeysHelper.setKeyValue("mm_design", "design_id", designId);
			MetrixCurrentKeysHelper.setKeyValue("mm_design_set", "design_set_id", designSetId);
			String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + designId);

			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerRevisionActivity.class);
			intent.putExtra("headingText", designName);
			intent.putExtra("targetDesignerActivity", (String) this.getIntent().getExtras().get("targetDesignerActivity"));
			intent.putExtra("targetDesignerRevisionID", (String) this.getIntent().getExtras().get("targetDesignerRevisionID"));
			if (this.getIntent().getExtras().containsKey("targetDesignerScreenID")) {
				intent.putExtra("targetDesignerScreenID", (String) this.getIntent().getExtras().get("targetDesignerScreenID"));
				intent.putExtra("targetDesignerScreenName", (String) this.getIntent().getExtras().get("targetDesignerScreenName"));
			}

			mProcessedTargetIntent = true;

			MetrixActivityHelper.startNewActivity(this, intent);
		}

		resourceStrings = new ArrayList<ResourceValueObject>();
		resourceStrings.add(new ResourceValueObject(mDesignResourceData.getExtraResourceID("R.id.designs"), "Designs"));
		resourceStrings.add(new ResourceValueObject(mDesignResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_design"), "ScnDescMxDesDes"));
		resourceStrings.add(new ResourceValueObject(mDesignResourceData.getExtraResourceID("R.id.add_design"), "AddDesign"));
		try {
			AndroidResourceHelper.setResourceValues(this, resourceStrings);
		} catch (Exception e) {
		}
	}

	@Override
	protected void bindService() {
		bindService(new Intent(MetrixDesignerDesignActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
					if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMDN\":null}")) {
						MetrixCurrentKeysHelper.setKeyValue("mm_design", "design_set_id", mSelectedDesignSetID);
						MetrixCurrentKeysHelper.setKeyValue("mm_design", "design_id", mSelectedDesignID);

						MobileApplication.stopSync(mCurrentActivity);
						MobileApplication.startSync(mCurrentActivity);
						mUIHelper.dismissLoadingDialog();
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerDesignActivity.this, MetrixDesignerRevisionActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						// re-getting this data, as the intent has the bad habit of using previous extras
						String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + mSelectedDesignID);
						intent.putExtra("headingText", designName);
						MetrixActivityHelper.startNewActivity(MetrixDesignerDesignActivity.this, intent);
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void populateList() {
		String query = "select mm_design.design_id, mm_design.design_set_id, mm_design.name, mm_design.parent_design_id from mm_design order by mm_design.name asc";
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			final String noneText = AndroidResourceHelper.getMessage("None");
			final String parentDesignText = AndroidResourceHelper.getMessage("ParentDesign");

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_design.design_id", cursor.getString(0));
				row.put("mm_design.design_set_id", cursor.getString(1));
				row.put("mm_design.name", cursor.getString(2));

				String parentDesignId = cursor.getString(3);
				String parentDesignName = "";
				if (!MetrixStringHelper.isNullOrEmpty(parentDesignId)) {
					parentDesignName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "mm_design.design_id = " + parentDesignId);
				}

				if (MetrixStringHelper.isNullOrEmpty(parentDesignName)) {
					parentDesignName = "(" + noneText + ")";
				}
				row.put("mm_design.parent_name", parentDesignText + ": " + parentDesignName);

				table.add(row);
				cursor.moveToNext();
			}

			mDesignAdapter = new DesignListAdapter(this, table, mDesignResourceData.ListViewItemResourceID, mDesignResourceData.ExtraResourceIDs);
			mListView.setAdapter(mDesignAdapter);
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
		if (viewId == mDesignResourceData.getExtraResourceID("R.id.add_design")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerDesignAddActivity.class);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = mDesignAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;

		String designId = selectedItem.get("mm_design.design_id");
		String designSetId = selectedItem.get("mm_design.design_set_id");
		String designName = selectedItem.get("mm_design.name");
		MetrixCurrentKeysHelper.setKeyValue("mm_design", "design_id", designId);
		MetrixCurrentKeysHelper.setKeyValue("mm_design_set", "design_set_id", designSetId);

		Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerRevisionActivity.class);
		intent.putExtra("headingText", designName);
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
		Object item = mDesignAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;
		String designSetId = selectedItem.get("mm_design.design_set_id");
		String designId = selectedItem.get("mm_design.design_id");
		mSelectedDesignSetID = designSetId;
		mSelectedDesignID = designId;
		mGetDesignAlert = new AlertDialog.Builder(MetrixDesignerDesignActivity.this).create();
		mGetDesignAlert.setMessage(AndroidResourceHelper.getMessage("RefreshDesignConfirm"));
		mGetDesignAlert.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), getDesignListener);
		mGetDesignAlert.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), getDesignListener);
		mGetDesignAlert.show();

		return true;
	}

	DialogInterface.OnClickListener getDesignListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // OK
					// wire up OK button to call perform_get_mobile_design
					if (SettingsHelper.getSyncPause(mCurrentActivity))
					{
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
						if (syncPauseAlertDialog != null) {
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
									startGetDesignListener();
								}
							});
						}
					} else
						startGetDesignListener();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // Cancel (do nothing)
					break;
			}
		}
	};

	private void startGetDesignListener() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				clearDesignMetadata(mSelectedDesignSetID, mSelectedDesignID);

				if (doDesignGet(mCurrentActivity) == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					MetrixDesignerDesignActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();

							if (mGetDesignAlert != null) {
								mGetDesignAlert.dismiss();
							}
						}
					});
					return;
				}

				if (mGetDesignAlert != null) {
					mGetDesignAlert.dismiss();
				}

				// start waiting dialog on-screen
				mUIHelper = new MetrixUIHelper(mCurrentActivity);
				mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("GetDesignInProgress"));
			}
		});

		thread.start();
	}

	public static boolean doDesignGet(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		try {
			Hashtable<String, String> params = new Hashtable<String, String>();
			int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

			params.put("design_set_id", mSelectedDesignSetID);
			params.put("design_id", mSelectedDesignID);
			params.put("device_sequence", String.valueOf(device_id));

			MetrixPerformMessage performGMD = new MetrixPerformMessage("perform_get_mobile_design", params);
			performGMD.save();
		}
		catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return true;
	}

	public static class DesignListAdapter extends DynamicListAdapter {
		static ViewHolder holder;

		public DesignListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
			super(context, table, listViewItemResourceID, lviElemResIDs);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);
				holder = new ViewHolder();
				holder.mDesignID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_design__design_id"));
				holder.mDesignSetID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_design__design_set_id"));
				holder.mName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_design__name"));
				holder.mParentName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_design__parent_name"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mDesignID.setText(dataRow.get("mm_design.design_id"));
			holder.mDesignSetID.setText(dataRow.get("mm_design.design_set_id"));
			holder.mName.setText(dataRow.get("mm_design.name"));
			holder.mParentName.setText(dataRow.get("mm_design.parent_name"));

			return vi;
		}

		static class ViewHolder {
			TextView mDesignID;
			TextView mDesignSetID;
			TextView mName;
			TextView mParentName;
		}
	}
}

