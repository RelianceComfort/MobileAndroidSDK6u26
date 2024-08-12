package com.metrix.metrixmobile.system;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.metrix.architecture.actionbar.MetrixActionBarManager;
import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixFormManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.ui.widget.DebriefNavigationRecyclerViewAdapter;
import com.metrix.architecture.ui.widget.MetrixQuickLinksBar;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global.ActivityType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.metrixmobile.DebriefMetrixActionView;
import com.metrix.metrixmobile.JobList;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.MobileGlobal;

import java.util.HashMap;
import java.util.List;

@SuppressLint("DefaultLocale")
public class DebriefActivity extends MetrixActivity {
	protected boolean mDisableContextMenu = false;
	private DebriefNavigationRecyclerViewAdapter mDebriefNavigationAdapter;
	private String[] mDebriefNavigationListFrom;
	private int[] mDebriefNavigationListTo;
	protected RecyclerView mDebriefNavigationRecyclerView;
	private boolean screenShouldRefreshPTA = false;

	private boolean pooledTaskAssignmentStarted;
	public boolean isPooledTaskAssignmentStarted() {
		return pooledTaskAssignmentStarted;
	}

	public void setPooledTaskAssignmentStarted(boolean pooledTaskAssignmentStarted) {
		this.pooledTaskAssignmentStarted = pooledTaskAssignmentStarted;
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	public void onStart() {
		super.onStart();

		boolean backPressed = false;
		String backCached = (String) MetrixPublicCache.instance.getItem("BackbuttonPressed");
		if(!MetrixStringHelper.isNullOrEmpty(backCached)&&backCached.equalsIgnoreCase("Y"))
			backPressed = true;

		if(backPressed){
			String task_id = MetrixFormManager.getOriginalValue("task.task_id");
			if(!MetrixStringHelper.isNullOrEmpty(task_id))
				MetrixCurrentKeysHelper.setKeyValue("task", "task_id", task_id);

			MetrixPublicCache.instance.addItem("BackbuttonPressed", "N");
		}

		ViewGroup layout = (ViewGroup) findViewById(R.id.table_layout);
		registerForMetrixActionView(layout, getMetrixActionBar().getCustomView());
		if(!MetrixStringHelper.isNullOrEmpty(MetrixCurrentKeysHelper.getKeyValue("task", "task_id"))){
			this.setupActionBar();
		}
		displayPreviousCount();
		refreshMetrixQuickLinksBar();
		disableAllControlsIfPooledTask();

		setPooledTaskAssignmentStarted(false);
	}

	/*
	 * (non-Javadoc) The sub class must execute this super method of the
	 * onDestroy, otherwise it may have memory leak
	 *
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		unbindService();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		MetrixPublicCache.instance.addItem("BackbuttonPressed", "Y");
		super.onBackPressed();
		return;
	}

	@SuppressLint("NewApi")
	@Override
	protected void onResume() {
		super.onResume();
		invalidateOptionsMenu();
	}

	@Override
	protected void bindService() {
		bindService(new Intent(this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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

	protected ServiceConnection mConnection = new ServiceConnection() {
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
					handleLocalMessage(activityType, message);
				}
			});
		}
	};

	protected void handleLocalMessage(ActivityType activityType, final String message) {

		boolean updateQuickLinkBar = MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "DebriefOverview")
				    || MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "DebriefTaskTextList")
				    || MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "DebriefTaskAttachment");

		if (activityType == ActivityType.Download) {
			if (message.compareTo("TASK") == 0) {
				if (MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "DebriefOverview")
						|| MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "DebriefTaskAttachment")
						|| MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "DebriefTaskAttachmentFullScreen")
						|| MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "DebriefTaskAttachmentAdd")
						|| MetrixStringHelper.valueIsEqual(codeLessScreenName, "DebriefTaskText")
						|| MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "DebriefTaskTextList")) {

					if (screenShouldRefreshPTA) {
						reloadActivity();
						screenShouldRefreshPTA = false;
						return;
					}

					if (!isPooledTaskAssignmentStarted()) {
						String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
						if (!MetrixPooledTaskAssignmentManager.instance().currentTaskExists(taskId)) {
							mUIHelper.dismissLoadingDialog();
							MetrixPooledTaskAssignmentManager.instance().dismissCurrentScreen(this, AndroidResourceHelper.getMessage("PTskAsgnmntAlrdyAsgndOrNotExsts"));
							return;
						} else {
							if (!MetrixPooledTaskAssignmentManager.instance().currentUserIsTheOwnerOfTheTask(taskId)) {
								mUIHelper.dismissLoadingDialog();
								MetrixPooledTaskAssignmentManager.instance().dismissCurrentScreen(this, AndroidResourceHelper.getMessage("PTskAsgnmntAlrdyAsgndOrNotExsts"));
								return;
							}
						}
					}
				}
			}
			else if (MetrixStringHelper.valueIsEqual(message, "{\"END_PTAS\":null}")) {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity);
				mUIHelper.dismissLoadingDialog();
				screenShouldRefreshPTA = true;

			} else if (MetrixStringHelper.valueIsEqual(message, "{\"END_PTATAA\":null}")) {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity);
				mUIHelper.dismissLoadingDialog();
				MetrixPooledTaskAssignmentManager.instance().dismissCurrentScreen(this, AndroidResourceHelper.getMessage("PTaskAssignmentAlreadyAssigned"));

			} else if (MetrixStringHelper.valueIsEqual(message, "{\"END_PTDNE\":null}")) {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity);
				mUIHelper.dismissLoadingDialog();
				MetrixPooledTaskAssignmentManager.instance().dismissCurrentScreen(this, AndroidResourceHelper.getMessage("PTaskNotExists"));

			} else if (MetrixStringHelper.valueIsEqual(message, "{\"END_PTA\":null}")) {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity);
				if(!screenShouldRefreshPTA)
					mUIHelper.dismissLoadingDialog();

				setPooledTaskAssignmentStarted(false);
			}
			if (updateQuickLinkBar){
				refreshMetrixQuickLinksBar();
			}
		} else{
			processPostListener(activityType, message);
		}
	}

	protected void displayPreviousCount() {

	}

	protected boolean taskIsComplete() {
		try {
			if(MetrixStringHelper.isNullOrEmpty(MetrixCurrentKeysHelper.getKeyValue("task", "task_id"))){
				return false;
			}

			String taskStatus = MetrixDatabaseManager.getFieldStringValue("task", "task_status",
					"task_id = " + MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));

			String status = MetrixDatabaseManager.getFieldStringValue("task_status", "status",
					"task_status = '" + taskStatus + "'");

			if ((status.compareToIgnoreCase("CA") == 0) || (status.compareToIgnoreCase("CO") == 0)
					|| (status.compareToIgnoreCase("CL") == 0)) {
				return true;
			} else {
				return false;
			}
		}
		catch(Exception ex){
			return false;
		}
	}

	public void setupActionBar() {
		TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
		if (actionBarTitle != null) {
			if (this.mHandlingErrors) {
				actionBarTitle.setText(AndroidResourceHelper.getMessage("ErrorActionBarTitle1Arg", MobileGlobal.mErrorInfo.transactionDescription));
			} else {
				String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
				if (!MetrixStringHelper.isNullOrEmpty(taskId)) {
					String placeIdCust = MetrixDatabaseManager.getFieldStringValue("task", "place_id_cust", "task_id = " + taskId);
					String name = MetrixDatabaseManager.getFieldStringValue("place", "name", "place_id = '" + placeIdCust + "'");
					actionBarTitle.setText(AndroidResourceHelper.getMessage("DebriefActionBarTitle2Args", taskId, name));
				}
			}
		}
		ImageView up = (ImageView) findViewById(R.id.up);
		AppCompatImageView overflow = (AppCompatImageView)up;
		overflow.setColorFilter(Color.parseColor("#FEFDFE"));
		MetrixSkinManager.setFirstGradientBackground(overflow, 0);
		if (up != null) {
			up.setImageResource(R.drawable.up);
			up.setOnClickListener(this);
			up.setVisibility(View.GONE);
		}
	}

	protected void setupActionBar(int textId) {
		TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
		if (actionBarTitle != null) {
			actionBarTitle.setText(textId);
		}
	}

	protected void setNavigation() {
		boolean statusFound = false;
		String allowedStatusParam = MetrixDatabaseManager.getAppParam("DEBRIEF_ALLOWED_STATUSES");
		if (!MetrixStringHelper.isNullOrEmpty(allowedStatusParam)) {
			String[] allowedStatuses = allowedStatusParam.split(",");
			try {
				String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
				if (!MetrixStringHelper.isNullOrEmpty(taskId)) {
					String currentStatus = MetrixDatabaseManager.getFieldStringValue("task", "task_status", "task_id = " + taskId);
					if (allowedStatuses != null) {
						for (String allowedStatus : allowedStatuses) {
							if (allowedStatus.trim().compareToIgnoreCase(currentStatus) == 0) {
								statusFound = true;
								break;
							}
						}
					}
				} else {
					statusFound = false;
				}
			} catch (Exception ex) {}
		} else
			statusFound = true;

		if (statusFound == false) {
			mDisableContextMenu = true;
			MetrixActionBarManager.getInstance().disableMenuButton(this);
			setTabletUILeftMenuVisible(false);
		} else {
			mDisableContextMenu = false;
			setTabletUILeftMenuVisible(true);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.correct_error:
				MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");

				MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, SyncServiceMonitor.class, true, "");
				break;
			case R.id.up:
				Intent intent = MetrixActivityHelper.createActivityIntent(this, JobList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
				MetrixActivityHelper.startNewActivity(this, intent);
				break;
			default:
				super.onClick(v);
		}
	}

	@Override
	public boolean onMetrixActionViewItemClick(MenuItem menuItem) {
		return super.onMetrixActionViewItemClick(menuItem);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if(mDisableContextMenu == false)
			DebriefMetrixActionView.onCreateMetrixActionView(this, menu);
		boolean createOption = super.onCreateOptionsMenu(menu);
		return createOption;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		DebriefMetrixActionView.onMetrixActionMenuItemSelected(this, item);
		boolean itemSelected = super.onOptionsItemSelected(item);
		return itemSelected;
	}

	protected void setTabletUILeftMenuVisible(boolean isVisible) {
		LinearLayout tabletUILeftMenu = this.findViewById(R.id.debrief_navigation_bar);
		if (tabletUILeftMenu != null) {
			tabletUILeftMenu.setVisibility(isVisible ? View.VISIBLE : View.GONE);
			NestedScrollView nsv = this.findViewById(R.id.scroll_view);
			if (nsv != null && !isVisible) {
				// Making the left-hand menu disappear leads to the scroll_view being scrolled down a bit
				// Manually forcing a scroll to the top, so that it does not cut off the quick action bar icons by default
				// This shift is visible to the end user and will make the FABs temporarily disappear.
				nsv.post(new Runnable() { public void run() { nsv.smoothScrollTo(0,0); }});
			}
		}
	}

	private void refreshMetrixQuickLinksBar(){
		MetrixQuickLinksBar metrixQuickLinksBar = (MetrixQuickLinksBar) findViewById(R.id.quick_links_bar);
		if(metrixQuickLinksBar != null){
			String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
			if(!MetrixStringHelper.isNullOrEmpty(taskId))
				metrixQuickLinksBar.refreshBadges();
		}
	}

	private void disableAllControlsIfPooledTask() {
		if(MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "DebriefOverview")
				|| MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "DebriefTaskAttachment")
				|| MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "DebriefTaskAttachmentFullScreen")
				|| MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "DebriefTaskAttachmentAdd")
				|| MetrixStringHelper.valueIsEqual(codeLessScreenName, "DebriefTaskText")
				|| MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "DebriefTaskTextList")) {

			boolean status = MetrixPooledTaskAssignmentManager.instance().isPooledTask(MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));

			if (mFormDef != null)
			{
				for (MetrixTableDef tableDef : mFormDef.tables) {
					if(tableDef == null) continue;
					for (MetrixColumnDef columnDef : tableDef.columns) {
						if(columnDef == null) continue;
						View view = MetrixControlAssistant.getControl(columnDef.id, mLayout);
						if(view == null) continue;
						if (status)
							view.setEnabled(!status);
					}
				}
			}

			FloatingActionButton saveButton = (FloatingActionButton)findViewById(R.id.save);
			if (saveButton != null) saveButton.setEnabled(!status && !taskIsComplete());

			FloatingActionButton addButton = (FloatingActionButton)findViewById(R.id.add);
			if (addButton != null) addButton.setEnabled(!status);

			FloatingActionButton updateButton = (FloatingActionButton)findViewById(R.id.update);
			if (updateButton != null) updateButton.setEnabled(!status);

		}
	}

	//Tablet UI Optimization
	protected void renderTabletSpecificLayoutControls() {
		super.renderTabletSpecificLayoutControls();
		setupDebriefNavigationList();
	}

	protected void refreshDebriefNavigationList(){
		setupDebriefNavigationList();
	}

	protected void setupDebriefNavigationList() {
		if (mDebriefNavigationRecyclerView == null) {
			mDebriefNavigationRecyclerView = findViewById(R.id.debrief_side_rv);
			MetrixListScreenManager.setupVerticalRecyclerView(mDebriefNavigationRecyclerView, R.drawable.rv_item_divider);
		}
		if (mDebriefNavigationRecyclerView != null) {

			mDebriefNavigationListTo = new int[] { R.id.sidelist_screen_name, R.id.sidelist_item_name, R.id.sidelist_item_count };
			mDebriefNavigationListFrom = new String[] { "screen_name", "label", "record_count" };

			List<HashMap<String, String>> table = MetrixWorkflowManager.getDebriefNavigationListItems(mCurrentActivity, mDebriefNavigationListFrom, mDebriefNavigationListTo);

			// fill in the grid_item layout
			if (mDebriefNavigationAdapter == null) {
				mDebriefNavigationAdapter = new DebriefNavigationRecyclerViewAdapter(mCurrentActivity,
						table, R.layout.debrief_navigation_bar_item, mDebriefNavigationListFrom, mDebriefNavigationListTo, null);
				mDebriefNavigationAdapter.setClickListener((pos, data, view) -> {
					Object item = mDebriefNavigationAdapter.getData().get(pos);
					@SuppressWarnings("unchecked")
					HashMap<String, String> selectedItem = (HashMap<String, String>) item;
					String selectedActivity = selectedItem.get("screen_name");

					if (DebriefMetrixActionView.itemNeedsComplexHandling(selectedActivity)) {
						DebriefMetrixActionView.handleComplexActionMenuItem(mCurrentActivity, selectedActivity);
					} else {
						if(MetrixApplicationAssistant.screenNameHasClassInCode(selectedActivity)){
							Intent intent = MetrixActivityHelper.createActivityIntent(mCurrentActivity, selectedActivity);
							MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
						}
						else{
							int screenId = MetrixScreenManager.getScreenId(selectedActivity);
							HashMap<String, String> screenPropertyMap = MetrixScreenManager.getScreenProperties(screenId);
							if(screenPropertyMap != null){
								String screenType = screenPropertyMap.get("screen_type");
								if(!MetrixStringHelper.isNullOrEmpty(screenType))
								{
									if(screenType.toLowerCase().contains("standard"))
									{
										Intent	intent = MetrixActivityHelper.createActivityIntent(mCurrentActivity, "com.metrix.metrixmobile.system", "MetadataDebriefActivity");
										intent.putExtra("ScreenID", screenId);
										MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
									}
									else if(screenType.toLowerCase().contains("list"))
									{
										Intent	intent = MetrixActivityHelper.createActivityIntent(mCurrentActivity, "com.metrix.metrixmobile.system", "MetadataListDebriefActivity");
										intent.putExtra("ScreenID", screenId);
										MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
									}
									else
										MetrixUIHelper.showSnackbar(mCurrentActivity, R.id.coordinator_layout, AndroidResourceHelper.getMessage("YYCSWrongScreenType", screenType));
								}
							}
						}
					}
				});
				mDebriefNavigationRecyclerView.setAdapter(mDebriefNavigationAdapter);
			} else {
				mDebriefNavigationAdapter.updateData(table);
			}
		}
	}
	//End Tablet UI Optimization
}

