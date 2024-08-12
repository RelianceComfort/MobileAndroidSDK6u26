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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
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
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;

import org.w3c.dom.Text;

import java.util.Hashtable;

public class MetrixDesignerDesignAddActivity extends MetrixDesignerActivity {
	private Button mSaveButton;
	private TextView mSrcDesignDesc, mSrcDesignDescLabel, mSrcRevisionDesc, mSrcRevisionDescLabel;
	private static EditText mName, mDesc;
	private static Spinner mSrcDesignID, mSrcRevisionID;
	private AlertDialog mAddDesignDialog;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private MetrixDesignerResourceData mDesignAddResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDesignAddResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerDesignAddActivityResourceData");

		setContentView(mDesignAddResourceData.LayoutResourceID);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mDesignAddResourceData.HelpTextString;

		mSaveButton = (Button) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.save"));
		mSaveButton.setOnClickListener(this);

		populateScreen();
	}

	@Override
	protected void bindService() {
		bindService(new Intent(MetrixDesignerDesignAddActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
					if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMD\":null}")) {
						MobileApplication.stopSync(mCurrentActivity);
						MobileApplication.startSync(mCurrentActivity);
						mUIHelper.dismissLoadingDialog();
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerDesignAddActivity.this, MetrixDesignerDesignActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						MetrixActivityHelper.startNewActivity(MetrixDesignerDesignAddActivity.this, intent);
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void populateScreen() {
		mName = (EditText) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.name"));
		mDesc = (EditText) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.description"));
		mSrcDesignID = (Spinner) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.source_design_id"));
		mSrcRevisionID = (Spinner) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.source_revision_id"));
		mSrcDesignDesc = (TextView) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.source_design_description"));
		mSrcDesignDescLabel = (TextView) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.source_design_description_label"));
		mSrcRevisionDesc = (TextView) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.source_revision_description"));
		mSrcRevisionDescLabel = (TextView) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.source_revision_description_label"));

		TextView title = (TextView) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.add_design"));
		TextView mScrInfo = (TextView) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.screen_Info_metrix_designer_design_add"));
		TextView mNameLbl = (TextView) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.name66cc700a"));
		TextView mDesign = (TextView) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.design"));
		TextView mRevision = (TextView) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.revision"));
		TextView mDescLbl = (TextView) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.description87969ca2"));
		Button mSave = (Button) findViewById(mDesignAddResourceData.getExtraResourceID("R.id.save"));

		AndroidResourceHelper.setResourceValues(title, "AddDesign");
		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnDescMxDesDesAdd");
		AndroidResourceHelper.setResourceValues(mNameLbl, "Name");
		AndroidResourceHelper.setResourceValues(mDesign, "Design");
		AndroidResourceHelper.setResourceValues(mRevision, "Revision");
		AndroidResourceHelper.setResourceValues(mDescLbl, "Description");
		AndroidResourceHelper.setResourceValues(mSave, "Save");




		// add Required hint to Name
		mName.setHint(AndroidResourceHelper.getMessage("Required"));

		// start off by hiding the design/revision descriptions (only show if we have data selected in spinners)
		mSrcDesignDesc.setVisibility(View.GONE);
		mSrcDesignDescLabel.setVisibility(View.GONE);
		mSrcRevisionDesc.setVisibility(View.GONE);
		mSrcRevisionDescLabel.setVisibility(View.GONE);

		// populate design spinner with all designs (value = design_id ... display = name)
		MetrixControlAssistant.populateSpinnerFromQuery(this, mSrcDesignID, "select name, design_id from mm_design where parent_design_id IS NULL order by name asc", true);
		mSrcDesignID.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				setDesignDescriptionBasedOnSelectedDesign();
				setRevisionSpinnerBasedOnSelectedDesign();

				try {
					String selectedDesignID = MetrixControlAssistant.getValue(mSrcDesignID);
					if (MetrixStringHelper.isNullOrEmpty(selectedDesignID)) {
						makeRevisionDescriptionDisappear();
					}
				} catch (Exception e) {
					LogManager.getInstance().error(e);
				}
			}

			public void onNothingSelected(AdapterView<?> parent) {
				setDesignDescriptionBasedOnSelectedDesign();
				setRevisionSpinnerBasedOnSelectedDesign();
				makeRevisionDescriptionDisappear();
			}
		});

		mSrcRevisionID.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				setRevisionDescriptionBasedOnSelectedRevision();
			}

			public void onNothingSelected(AdapterView<?> parent) {
				setRevisionDescriptionBasedOnSelectedRevision();
			}
		});
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mDesignAddResourceData.getExtraResourceID("R.id.save")) {
			try {
				// Make sure Name is filled in.
				String designName = MetrixControlAssistant.getValue(mName);
				if (MetrixStringHelper.isNullOrEmpty(designName)) {
					Toast.makeText(this, AndroidResourceHelper.getMessage("AddDesignNameError"), Toast.LENGTH_LONG).show();
					return;
				}

				// Make sure source design/revision are either BOTH populated or NEITHER populated.
				String sourceDesignID = "";
				String sourceRevisionID = "";
				if (mSrcDesignID.getSelectedItem() != null)
					sourceDesignID = MetrixControlAssistant.getValue(mSrcDesignID);
				if (mSrcRevisionID.getSelectedItem() != null)
					sourceRevisionID = MetrixControlAssistant.getValue(mSrcRevisionID);

				if ((MetrixStringHelper.isNullOrEmpty(sourceDesignID) || MetrixStringHelper.isNullOrEmpty(sourceRevisionID))
						&& !(MetrixStringHelper.isNullOrEmpty(sourceDesignID) && MetrixStringHelper.isNullOrEmpty(sourceRevisionID))) {
					Toast.makeText(this, AndroidResourceHelper.getMessage("AddDesignSourceError"), Toast.LENGTH_LONG).show();
					return;
				}

				mAddDesignDialog = new AlertDialog.Builder(this).create();
				mAddDesignDialog.setMessage(AndroidResourceHelper.getMessage("AddDesignConfirm"));
				mAddDesignDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), addDesignListener);
				mAddDesignDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), addDesignListener);
				mAddDesignDialog.show();
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	DialogInterface.OnClickListener addDesignListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					// wire up Yes button to call perform_generate_mobile_design
					if (SettingsHelper.getSyncPause(mCurrentActivity))
					{
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
						if (syncPauseAlertDialog != null) {
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
									startAddDesignListener();
								}
							});
						}
					} else
						startAddDesignListener();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private void startAddDesignListener() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				if (doDesignAddition(mCurrentActivity) == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					MetrixDesignerDesignAddActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();

							if (mAddDesignDialog != null) {
								mAddDesignDialog.dismiss();
							}
						}
					});
					return;
				}

				if (mAddDesignDialog != null) {
					mAddDesignDialog.dismiss();
				}

				// start waiting dialog on-screen
				mUIHelper = new MetrixUIHelper(mCurrentActivity);
				mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddDesignInProgress"));
			}
		});

		thread.start();
	}

	public static boolean doDesignAddition(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		try {
			Hashtable<String, String> params = new Hashtable<String, String>();
			int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

			String name = mName.getText().toString();
			String description = mDesc.getText().toString();

			String sourceDesignID = "";
			String sourceRevisionID = "";
			if (mSrcDesignID.getSelectedItem() != null)
				sourceDesignID = MetrixControlAssistant.getValue(mSrcDesignID);
			if (mSrcRevisionID.getSelectedItem() != null)
				sourceRevisionID = MetrixControlAssistant.getValue(mSrcRevisionID);

			params.put("name", name);
			params.put("is_child_design", "false");
			if (!MetrixStringHelper.isNullOrEmpty(description)) {
				params.put("description", description);
			}
			if (!MetrixStringHelper.isNullOrEmpty(sourceDesignID)) {
				params.put("source_design_id", sourceDesignID);
			}
			if (!MetrixStringHelper.isNullOrEmpty(sourceRevisionID)) {
				params.put("source_revision_id", sourceRevisionID);
			}
			params.put("device_sequence", String.valueOf(device_id));

			MetrixPerformMessage performGMD = new MetrixPerformMessage("perform_generate_mobile_design", params);
			performGMD.save();
		}
		catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return true;
	}

	private void setRevisionSpinnerBasedOnSelectedDesign() {
		try {
			String selectedDesignID = MetrixControlAssistant.getValue(mSrcDesignID);
			StringBuilder query = new StringBuilder();
			if (!MetrixStringHelper.isNullOrEmpty(selectedDesignID)) {
				query.append(String.format("select '%s ' || revision_number, revision_id from mm_revision ", AndroidResourceHelper.getMessage("Revision")));
				query.append(String.format("where (mm_revision.design_set_id in (select design_set_id from mm_design where design_id = %s) and mm_revision.status = 'PENDING') ", selectedDesignID));
				query.append(String.format("or (mm_revision.revision_id in (select revision_id from mm_design_revision_view where design_id = %s)) ", selectedDesignID));
				query.append("order by revision_number DESC");
			} else {
				// pass in a valid query, but use a where clause that ensures no results returned (blank out spinner)
				query.append("select revision_number, revision_id from mm_revision where 1=2");
			}
			MetrixControlAssistant.populateSpinnerFromQuery(this, mSrcRevisionID, query.toString(), true);
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private void setDesignDescriptionBasedOnSelectedDesign() {
		try {
			String selectedDesignID = MetrixControlAssistant.getValue(mSrcDesignID);
			if (!MetrixStringHelper.isNullOrEmpty(selectedDesignID)) {
				String designDesc = MetrixDatabaseManager.getFieldStringValue("mm_design", "description", "design_id = " + selectedDesignID);
				if (MetrixStringHelper.isNullOrEmpty(designDesc)) {
					designDesc = AndroidResourceHelper.getMessage("NoDescriptionFound");
				}
				mSrcDesignDesc.setVisibility(View.VISIBLE);
				mSrcDesignDescLabel.setVisibility(View.INVISIBLE);
				MetrixControlAssistant.setValue(mSrcDesignDesc, designDesc);
			} else {
				mSrcDesignDesc.setVisibility(View.GONE);
				mSrcDesignDescLabel.setVisibility(View.GONE);
				MetrixControlAssistant.setValue(mSrcDesignDesc, "");
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private void setRevisionDescriptionBasedOnSelectedRevision() {
		try {
			String selectedRevisionID = MetrixControlAssistant.getValue(mSrcRevisionID);
			if (!MetrixStringHelper.isNullOrEmpty(selectedRevisionID)) {
				String revisionDesc = MetrixDatabaseManager.getFieldStringValue("mm_revision", "description", "revision_id = " + selectedRevisionID);
				if (MetrixStringHelper.isNullOrEmpty(revisionDesc)) {
					revisionDesc = AndroidResourceHelper.getMessage("NoDescriptionFound");
				}
				mSrcRevisionDesc.setVisibility(View.VISIBLE);
				mSrcRevisionDescLabel.setVisibility(View.INVISIBLE);
				MetrixControlAssistant.setValue(mSrcRevisionDesc, revisionDesc);
			} else {
				makeRevisionDescriptionDisappear();
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private void makeRevisionDescriptionDisappear() {
		try {
			mSrcRevisionDesc.setVisibility(View.GONE);
			mSrcRevisionDescLabel.setVisibility(View.GONE);
			MetrixControlAssistant.setValue(mSrcRevisionDesc, "");
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}
}

