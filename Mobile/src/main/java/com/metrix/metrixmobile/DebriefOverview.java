package com.metrix.metrixmobile;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.metrix.architecture.actionbar.MetrixActionBarManager;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixRelationOperands;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixFieldManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixFormManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixRelationDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.superclasses.MetrixImplicitSaveSwipeActivity;
import com.metrix.architecture.ui.widget.MetrixQuickLinksBar;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper.ISO8601;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.global.MetrixImportantInformation;
import com.metrix.metrixmobile.global.MetrixTimeClockAssistant;
import com.metrix.metrixmobile.global.MetrixTimeClockEvents;
import com.metrix.metrixmobile.global.MobileGlobal;
import com.metrix.metrixmobile.policies.MeterReadingsPolicy;
import com.metrix.metrixmobile.system.DebriefActivity;
import com.metrix.metrixmobile.system.SyncServiceMonitor;

import org.joda.time.DateTime;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

public class DebriefOverview extends DebriefActivity implements View.OnClickListener, OnChronometerTickListener, MetrixImplicitSaveSwipeActivity {
	private FloatingActionButton mSaveButton, mNextButton;
	private ImageView mRemoveProduct, mAddProduct, mTimeClockStart, mTimeClockPause, mTimeClockStop, mMapProduct;
	private LinearLayout mTimeClock;
	private Chronometer mTimeClockChronometer;
	private long mElapsedTime = 0;
	private Spinner mTimeClockSequence, mTaskStatus;
	private Boolean backPressed = false;
	private MetrixQuickLinksBar metrixQuickLinksBar = null;
	private List<FloatingActionButton> mFABList;
	private List<FloatingActionButton> mFABsToShow;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(shouldRunTabletSpecificUIMode)
			setContentView(R.layout.tb_land_debrief_overview);
		else
			setContentView(R.layout.debrief_overview);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		backPressed = false;
		String backCached = (String) MetrixPublicCache.instance.getItem("BackbuttonPressed");
		if (!MetrixStringHelper.isNullOrEmpty(backCached) && backCached.equalsIgnoreCase("Y"))
			backPressed = true;

		Bundle extras = getIntent().getExtras();
		String taskId = extras.getString("TASK_ID");

		if (!MetrixStringHelper.isNullOrEmpty(taskId)) {
			// this block of code indicates that the DebriefOverview screen was opened
			// from a notification issued by the OS
			MetrixCurrentKeysHelper.setKeyValue("task", "task_id", taskId);

			String taskType = MetrixDatabaseManager.getFieldStringValue("task", "task_type", String.format("task_id = %s", taskId));
			String workflowName = MetrixWorkflowManager.getDebriefWorkflowNameForTaskType(taskType);
			MetrixWorkflowManager.setCurrentWorkflowName(mCurrentActivity, workflowName);
		}

		taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");

		if(!MetrixStringHelper.isNullOrEmpty(taskId))
			setTaskCacheValues(taskId);

		resourceStrings.add(new ResourceValueObject(R.id.overview_heading_1, "Details"));
		resourceStrings.add(new ResourceValueObject(R.id.Contact84006c7a, "Contact"));
		resourceStrings.add(new ResourceValueObject(R.id.Product1b5451c0, "Product"));
		resourceStrings.add(new ResourceValueObject(R.id.contract_heading, "Contract"));
		resourceStrings.add(new ResourceValueObject(R.id.warranty_heading, "Warranty"));
		resourceStrings.add(new ResourceValueObject(R.id.important_information_heading, "ImportantInformation"));
		//We need to do this after seeing if we have a task_id being passed in.
		//If we don't set the MetrixCurrentKeysHelper for task_id before this, the defineForm method will have the wrong task_id
		super.onStart();

		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		setImportantInformation(MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id"));

		this.setTimeClockVisibility();

		setRegionVisibility();

		metrixQuickLinksBar = (MetrixQuickLinksBar) findViewById(R.id.quick_links_bar);
		if (metrixQuickLinksBar != null) {
			metrixQuickLinksBar.invalidate();
		}

		MetrixControlAssistant.setOnItemSelectedListener(mTaskStatus, new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (MetrixControlAssistant.spinnerIsReady(mTaskStatus)) {
					if (backPressed)
						setNavigation(null);
					else
						setNavigation(mTaskStatus);
				}
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		if (backPressed)
			setNavigation(null);
		else
			setNavigation(mTaskStatus);
	}

	private void setTaskCacheValues(String taskid)
	{
		//Get the task's customer data
		ArrayList<Hashtable<String, String>> theData;
		String theRawQuery = String.format("select address.state_prov from address, task where task.task_id = %s and task.address_id = address.address_id", taskid);
		theData = MetrixDatabaseManager.getFieldStringValuesList(theRawQuery);
		Hashtable<String, String> dict;
		if (theData != null && theData.size() > 0){
			dict = theData.get(0);
			if (dict != null){
				String theStateProvValue = dict.get("state_prov");
				if (!MetrixStringHelper.isNullOrEmpty(theStateProvValue))
				{
					MetrixPublicCache.instance.addItem("CurrentCustomerStateProv", theStateProvValue);
				}
			}
		}

		theRawQuery = String.format("select place.global_name from place, task where task.task_id = %s and task.place_id_cust = place.place_id", taskid);
		theData = MetrixDatabaseManager.getFieldStringValuesList(theRawQuery);
		if (theData != null && theData.size() > 0){
			dict = theData.get(0);
			if (dict != null){
				String theGlobalNameValue = dict.get("global_name");
				if (!MetrixStringHelper.isNullOrEmpty(theGlobalNameValue))
				{
					MetrixPublicCache.instance.addItem("CurrentCustomerGlobalName", theGlobalNameValue);
				}
				else
				{
					MetrixPublicCache.instance.addItem("CurrentCustomerGlobalName", "");
				}
			}
		}

		theRawQuery = String.format("select place_id_cust, request_id from task where task_id = %s", taskid);
		theData = MetrixDatabaseManager.getFieldStringValuesList(theRawQuery);
		if (theData != null && theData.size() > 0){
			dict = theData.get(0);
			if (dict != null){
				String theRequestIdValue = dict.get("request_id");
				if (!MetrixStringHelper.isNullOrEmpty(theRequestIdValue))
				{
					MetrixPublicCache.instance.addItem("CurrentRequestId", theRequestIdValue);
				}
				String thePlaceIdValue = dict.get("place_id_cust");
				if (!MetrixStringHelper.isNullOrEmpty(thePlaceIdValue))
				{
					MetrixPublicCache.instance.addItem("CurrentPlaceId", thePlaceIdValue);
				}
			}
		}
	}

	private void setTimeClockVisibility() {
		int count = MetrixDatabaseManager.getCount("time_clock", "table_name = 'TASK' and active = 'Y' and column_name is null");

		if (count == 0) {
			mTimeClock.setVisibility(View.GONE);
		} else {
			try {
				MetrixControlAssistant.populateSpinnerFromQuery(this, mTimeClockSequence,
						"select description, clock_id from time_clock where table_name = 'TASK' and active = 'Y' and column_name is null order by sequence",
						false);
				mTimeClock.setVisibility(View.VISIBLE);

				MetrixTimeClockEvents currentStatus = MetrixTimeClockAssistant.getTimeClockStatus(this,
						MetrixControlAssistant.getValue(this.mTimeClockSequence), MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));

				if (currentStatus != MetrixTimeClockEvents.stop) {
					MetrixTimeClockAssistant.updateTimeClockStatus(this, MetrixControlAssistant.getValue(this.mTimeClockSequence), currentStatus,
							this.mTimeClock, false);
					long elapsedTimeInMilliseconds = MetrixTimeClockAssistant.getCurrentElapsedTime(this,
							MetrixControlAssistant.getValue(this.mTimeClockSequence), MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));
					if (elapsedTimeInMilliseconds == 0) {
						this.mElapsedTime = 0;
						this.mTimeClockChronometer.setText("00:00:00");
					} else {
						int seconds = (int) (elapsedTimeInMilliseconds / 1000) % 60;
						int minutes = (int) ((elapsedTimeInMilliseconds / (1000 * 60)) % 60);
						int hours = (int) ((elapsedTimeInMilliseconds / (1000 * 60 * 60)) % 24);
						this.mTimeClockChronometer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
						this.mTimeClockChronometer.setOnChronometerTickListener(this);
						this.mElapsedTime = elapsedTimeInMilliseconds;
						if (currentStatus == MetrixTimeClockEvents.start) {
							this.mTimeClockChronometer.start();
						}
					}
				} else {
					this.mElapsedTime = 0;
					this.mTimeClockChronometer.setText("00:00:00");
				}
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		try {
			mSaveButton = (FloatingActionButton) findViewById(R.id.update);
			mNextButton = (FloatingActionButton) findViewById(R.id.next);
			mRemoveProduct = (ImageView) findViewById(R.id.remove_product);
			mAddProduct = (ImageView) findViewById(R.id.add_product);
			mTimeClock = (LinearLayout) findViewById(R.id.time_clock);
			mTimeClockChronometer = (Chronometer) findViewById(R.id.time_clock_chronometer);
			mTimeClockSequence = (Spinner) findViewById(R.id.time_clock__sequence);
			mTimeClockStart = (ImageView) findViewById(R.id.time_clock_start_button);
			mTimeClockPause = (ImageView) findViewById(R.id.time_clock_pause_button);
			mTimeClockStop = (ImageView) findViewById(R.id.time_clock_stop_button);
			mMapProduct = (ImageView) findViewById(R.id.map_product);

			mTaskStatus = (Spinner)MetrixControlAssistant.getControl(mFormDef,  mLayout, "task", "task_status");

			mSaveButton.setOnClickListener(this);
			mSaveButton.setContentDescription("btnUpdate");
			mNextButton.setOnClickListener(this);
			mNextButton.setContentDescription("btnNext");
			mRemoveProduct.setOnClickListener(this);
			mNextButton.setContentDescription("imageRemove_product");
			mAddProduct.setOnClickListener(this);
			mAddProduct.setContentDescription("imageAdd_product");
			mTimeClockStart.setOnClickListener(this);
			mTimeClockPause.setOnClickListener(this);
			mTimeClockStop.setOnClickListener(this);
			mMapProduct.setOnClickListener(this);

			if (mFABList == null)
				mFABList = new ArrayList<FloatingActionButton>();
			else
				mFABList.clear();

			mFABList.add(mNextButton);

			final FloatingActionButton addButton = (FloatingActionButton) findViewById(R.id.save);
			if (addButton != null)
				MetrixControlAssistant.setButtonVisibility(addButton, View.GONE);
			if (mSaveButton != null && !mHandlingErrors) {
				MetrixControlAssistant.setButtonVisibility(mSaveButton, View.VISIBLE);
				mFABList.add(mSaveButton);
			}

			fabRunnable = this::showFABs;

			NestedScrollView scrollView = findViewById(R.id.scroll_view);
			mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), generateOffsetForFABs(mFABList));
			scrollView.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
				if ((scrollY > oldScrollY) || (scrollY < oldScrollY)) {
					fabHandler.removeCallbacks(fabRunnable);
					if(mFABsToShow != null)
						mFABsToShow.clear();
					else
						mFABsToShow = new ArrayList<>();

					hideFABs(mFABList);
					fabHandler.postDelayed(fabRunnable, fabDelay);
				}
			});

		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}

		if (!taskProductHasGPS()) {
			mMapProduct.setVisibility(View.GONE);
		} else {
			mMapProduct.setVisibility(View.VISIBLE);
		}

		setFullAddress();
	}

	private void setRegionVisibility() {
		// if any regions have no visible fields, auto-hide the region and its heading
		// these will be visible by default, so we only need to worry about hiding
		String activityName = this.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);

		String currentRegion = "1-Details";
		if (!MetrixFieldManager.regionContainsVisibleFields(screenId, currentRegion)) {
			View ovHeading1 = findViewById(R.id.overview_heading_1);
			View ovRegion1 = findViewById(R.id.overview_region_1);
			if (ovHeading1 != null) { ovHeading1.setVisibility(View.GONE); }
			if (ovRegion1 != null) { ovRegion1.setVisibility(View.GONE); }
		}

		currentRegion = "2-Contact";
		if (!MetrixFieldManager.regionContainsVisibleFields(screenId, currentRegion)) {
			View ovHeading2 = findViewById(R.id.overview_heading_2);
			View ovRegion2 = findViewById(R.id.overview_region_2);
			if (ovHeading2 != null) { ovHeading2.setVisibility(View.GONE); }
			if (ovRegion2 != null) { ovRegion2.setVisibility(View.GONE); }
		}

		currentRegion = "3-Product";
		if (!MetrixFieldManager.regionContainsVisibleFields(screenId, currentRegion)) {
			View ovHeading3 = findViewById(R.id.overview_heading_3);
			View ovRegion3 = findViewById(R.id.overview_region_3);
			if (ovHeading3 != null) { ovHeading3.setVisibility(View.GONE); }
			if (ovRegion3 != null) { ovRegion3.setVisibility(View.GONE); }
		}

		currentRegion = "4-Contract";
		if (!MetrixFieldManager.regionContainsVisibleFields(screenId, currentRegion)) {
			View ovHeading4 = findViewById(R.id.overview_heading_4);
			View ovRegion4 = findViewById(R.id.overview_region_4);
			if (ovHeading4 != null) { ovHeading4.setVisibility(View.GONE); }
			if (ovRegion4 != null) { ovRegion4.setVisibility(View.GONE); }
		}

		currentRegion = "5-Warranty";
		if (!MetrixFieldManager.regionContainsVisibleFields(screenId, currentRegion)) {
			View ovHeading5 = findViewById(R.id.overview_heading_5);
			View ovRegion5 = findViewById(R.id.overview_region_5);
			if (ovHeading5 != null) { ovHeading5.setVisibility(View.GONE); }
			if (ovRegion5 != null) { ovRegion5.setVisibility(View.GONE); }
		}
	}

	private void setNavigation(View statusControl) {
		boolean statusFound = false;
		String allowedStatusParam = MetrixDatabaseManager.getAppParam("DEBRIEF_ALLOWED_STATUSES");
		if (!MetrixStringHelper.isNullOrEmpty(allowedStatusParam)) {
			String[] allowedStatuses = allowedStatusParam.split(",");
			try {
				String currentStatus = "";
				if (statusControl == null) {
					String taskId = MetrixFormManager.getOriginalValue("task.task_id");
					currentStatus = MetrixDatabaseManager.getFieldStringValue("task", "task_status", "task_id = " + taskId);
				} else
					currentStatus = MetrixControlAssistant.getValue(statusControl);

				if (allowedStatuses != null) {
					for (String allowedStatus : allowedStatuses) {
						if (allowedStatus.trim().compareToIgnoreCase(currentStatus) == 0) {
							statusFound = true;
							break;
						}
					}
				}
			} catch (Exception ex) {}
		} else
			statusFound = true;

		if (statusFound == false) {
			mNextButton.setEnabled(false);
			mDisableContextMenu = true;
			MetrixActionBarManager.getInstance().disableMenuButton(this);
			setTabletUILeftMenuVisible(false);
		} else {
			mDisableContextMenu = false;
			setTabletUILeftMenuVisible(true);
		}
	}

	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
		ArrayList<MetrixTableDef> tableDefs = new ArrayList<MetrixTableDef>();

		MetrixTableDef taskDef = new MetrixTableDef("task", MetrixTransactionTypes.UPDATE);
		taskDef.constraints.add(new MetrixConstraintDef("task_id", MetrixConstraintOperands.EQUALS, MetrixCurrentKeysHelper.getKeyValue("task", "task_id"), double.class));

		MetrixRelationDef taskStatusRelationDef = new MetrixRelationDef("task", "task_status", "task_status", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef taskStatusDef = new MetrixTableDef("task_status", MetrixTransactionTypes.SELECT, taskStatusRelationDef);

		MetrixRelationDef taskContactRelationDef = new MetrixRelationDef("task", "task_id", "task_id", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef taskContactDef = new MetrixTableDef("task_contact", MetrixTransactionTypes.SELECT, taskContactRelationDef);

		MetrixRelationDef addressRelationDef = new MetrixRelationDef("task", "address_id", "address_id", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef addressDef = new MetrixTableDef("address", MetrixTransactionTypes.SELECT, addressRelationDef);

		MetrixRelationDef placeRelationDef = new MetrixRelationDef("task", "place_id_cust", "place_id", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef placeDef = new MetrixTableDef("place", MetrixTransactionTypes.SELECT, placeRelationDef);

		MetrixRelationDef reqUnitRelationDef = new MetrixRelationDef("task", "request_unit_id", "request_unit_id", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef reqUnitDef = new MetrixTableDef("request_unit", MetrixTransactionTypes.SELECT, reqUnitRelationDef);

		MetrixRelationDef contractRelationDef = new MetrixRelationDef("request_unit", "contract_id", "contract_id", MetrixRelationOperands.LEFT_OUTER);
		//LCS #134175
		MetrixRelationDef contractRelationDef2 = new MetrixRelationDef("request_unit", "contract_version", "contract_version", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef contractDef = new MetrixTableDef("contract", MetrixTransactionTypes.SELECT, contractRelationDef);
		contractDef.relations.add(contractRelationDef2);

		MetrixRelationDef modelRelationDef = new MetrixRelationDef("request_unit", "model_id", "model_id", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef modelDef = new MetrixTableDef("model", MetrixTransactionTypes.SELECT, modelRelationDef);

		MetrixRelationDef contrTypeRelationDef = new MetrixRelationDef("contract", "contr_type", "contr_type", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef contrTypeDef = new MetrixTableDef("contr_type", MetrixTransactionTypes.SELECT, contrTypeRelationDef);

		MetrixRelationDef warrantyRelationDef = new MetrixRelationDef("request_unit", "wty_cov_id", "wty_cov_id", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef warrantyTableDef = new MetrixTableDef("warranty_coverage", MetrixTransactionTypes.SELECT, warrantyRelationDef);

		MetrixRelationDef wtyCovTypeRelationDef = new MetrixRelationDef("warranty_coverage", "wty_cov_type", "wty_cov_type", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef wtyCovTypeDef = new MetrixTableDef("wty_cov_type", MetrixTransactionTypes.SELECT, wtyCovTypeRelationDef);

		MetrixRelationDef productRelationDef = new MetrixRelationDef("task", "product_id", "product_id", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef productTableDef = new MetrixTableDef("product", MetrixTransactionTypes.SELECT, productRelationDef);

		MetrixRelationDef partRelationDef = new MetrixRelationDef("request_unit", "part_id", "part_id", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef partTableDef = new MetrixTableDef("part", MetrixTransactionTypes.SELECT, partRelationDef);

		tableDefs.add(taskDef);
		tableDefs.add(taskStatusDef);
		tableDefs.add(taskContactDef);
		tableDefs.add(addressDef);
		tableDefs.add(placeDef);
		tableDefs.add(reqUnitDef);
		tableDefs.add(modelDef);
		tableDefs.add(contractDef);
		tableDefs.add(contrTypeDef);
		tableDefs.add(warrantyTableDef);
		tableDefs.add(wtyCovTypeDef);
		tableDefs.add(productTableDef);
		tableDefs.add(partTableDef);

		this.mFormDef = new MetrixFormDef(tableDefs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		if (scriptEventConsumesClick(this, v))
			return;

		Intent intent = null;
		switch (v.getId()) {
			case R.id.update:
				if (MetrixControlAssistant.valueChanged(mFormDef.getId("task", "task_status"), mFormDef, mLayout)) {
					handleStatusChange();
				}

				if (MetrixControlAssistant.valueChanged(mFormDef.getId("task", "task_type"), mFormDef, mLayout)) {
					handleTaskTypeChange();
				}

				MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
				MetrixSaveResult result = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, false, AndroidResourceHelper.getMessage("Task"));
				if (result == MetrixSaveResult.SUCCESSFUL) {
					CalculatePlanEnd();
					reloadActivity();
				}
				break;
			case R.id.next:
				if (anyOnStartValuesChanged() && (!taskIsComplete())) {
					transactionInfo = MetrixTransaction.getTransaction("task", "task_id");

					if (MetrixControlAssistant.valueChanged(mFormDef.getId("task", "task_status"), mFormDef, mLayout)) {
						handleStatusChange();
					}

					if (MetrixControlAssistant.valueChanged(mFormDef.getId("task", "task_type"), mFormDef, mLayout)) {
						handleTaskTypeChange();
					}

					MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, AndroidResourceHelper.getMessage("Task"), true);
				} else {
					MetrixWorkflowManager.advanceWorkflow(this);
				}
				break;
			case R.id.correct_error:
				if (MetrixControlAssistant.valueChanged(mFormDef.getId("task", "task_status"), mFormDef, mLayout)) {
					handleStatusChange();
				}

				transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
				MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, SyncServiceMonitor.class, true, AndroidResourceHelper.getMessage("Task"));
				break;
			case R.id.add_product:
				//Adding of multiple request units for the same task is not allowed.
				String taskRequestUnitId = MetrixDatabaseManager.getFieldStringValue("task", "request_unit_id", "task_id = " + MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));
				if (!MetrixStringHelper.isNullOrEmpty(taskRequestUnitId)) {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("MultipleRequestUnitsDisallowed"));
					return;
				}

				if (anyOnStartValuesChanged()) {
					transactionInfo = MetrixTransaction.getTransaction("task", "task_id");

					if (MetrixControlAssistant.valueChanged(mFormDef.getId("task", "task_status"), mFormDef, mLayout)) {
						handleStatusChange();
					}

					MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, false, AndroidResourceHelper.getMessage("Task"));
				}

				int druScreenId = MetrixScreenManager.getScreenId("DebriefRequestUnit");
				intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "MetadataDebriefActivity");
				intent.putExtra("ScreenID", druScreenId);
				MetrixActivityHelper.startNewActivity(this, intent);
				break;
			case R.id.remove_product:
				OnClickListener yesListener = new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							MetrixControlAssistant.setValue(mFormDef, mLayout, "task", "request_unit_id", "");
							MetrixControlAssistant.setValue(mFormDef, mLayout, "task", "product_id", "");
							MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
							MetrixUpdateManager.update(DebriefOverview.this, mLayout, mFormDef, transactionInfo, false, DebriefOverview.class, true, AndroidResourceHelper.getMessage("Task"));
						} catch (Exception e) {
							LogManager.getInstance().error(e);
						}
					}
				};
				MetrixDialogAssistant.showConfirmDeleteDialog(AndroidResourceHelper.getMessage("ProductLCase"), yesListener, null, this);
				break;
			case R.id.time_clock_start_button:
				try {
					MetrixTimeClockEvents currentStatus = MetrixTimeClockAssistant.getTimeClockStatus(this,
							MetrixControlAssistant.getValue(this.mTimeClockSequence), MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));

					if (currentStatus == MetrixTimeClockEvents.stop) {
						MetrixTimeClockAssistant.updateTimeClockStatus(this, MetrixControlAssistant.getValue(this.mTimeClockSequence), MetrixTimeClockEvents.start,
								this.mTimeClock, true);
					} else {
						MetrixTimeClockAssistant.updateTimeClockStatus(this, MetrixControlAssistant.getValue(this.mTimeClockSequence),
								MetrixTimeClockEvents.resume, this.mTimeClock, true);
					}

					long elapsedTimeInMilliseconds = MetrixTimeClockAssistant.getCurrentElapsedTime(this, MetrixControlAssistant.getValue(this.mTimeClockSequence),
							MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));
					if (elapsedTimeInMilliseconds == 0) {
						this.mTimeClockChronometer.setText("00:00:00");
						this.mElapsedTime = 0;
					} else {
						int seconds = (int) (elapsedTimeInMilliseconds / 1000) % 60;
						int minutes = (int) ((elapsedTimeInMilliseconds / (1000 * 60)) % 60);
						int hours = (int) ((elapsedTimeInMilliseconds / (1000 * 60 * 60)) % 24);
						this.mTimeClockChronometer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
						this.mElapsedTime = elapsedTimeInMilliseconds;
					}
					this.mTimeClockChronometer.start();
					this.mTimeClockChronometer.setOnChronometerTickListener(this);
				} catch (Exception e) {
					LogManager.getInstance(this).error(e);
				}
				break;
			case R.id.time_clock_pause_button:
				try {
					MetrixTimeClockAssistant.updateTimeClockStatus(this, MetrixControlAssistant.getValue(this.mTimeClockSequence), MetrixTimeClockEvents.pause,
							this.mTimeClock, true);
					this.mTimeClockChronometer.stop();
				} catch (Exception e) {
					LogManager.getInstance(this).error(e);
				}
				break;
			case R.id.time_clock_stop_button:
				try {
					MetrixTimeClockAssistant.updateTimeClockStatus(this, MetrixControlAssistant.getValue(this.mTimeClockSequence), MetrixTimeClockEvents.stop,
							this.mTimeClock, true);
					this.mTimeClockChronometer.setText("00:00:00");
					this.mTimeClockChronometer.stop();
				} catch (Exception e) {
					LogManager.getInstance(this).error(e);
				}
				break;
			case R.id.map_product:
				intent = MetrixActivityHelper.createActivityIntent(this, ProductMap.class);
				intent.putExtra("product_id", MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "product_id"));
				MetrixActivityHelper.startNewActivity(this, intent);
				break;

			default:
				super.onClick(v);
		}
	}

	private void CalculatePlanEnd()
	{
		try
		{
			EditText planStartControl = (EditText)MetrixControlAssistant.getControl(mFormDef, mLayout, "task", "plan_start_dttm");
			String Date = MetrixControlAssistant.getValue(planStartControl);

			EditText durationControl = (EditText)MetrixControlAssistant.getControl(mFormDef, mLayout, "task", "plan_task_dur_min");
			String duration = MetrixControlAssistant.getValue(durationControl);
			DateFormat formatter = MetrixDateTimeHelper.getDateTimeFormat(MetrixDateTimeHelper.DATE_TIME_FORMAT);
			Date endDate = formatter.parse(Date);
			DateTime finalEndDate = new DateTime(endDate).plusMinutes(Integer.valueOf(duration));;
			//Raw query is used to stop seperate sync.
			String query = String.format("update task set plan_end_dttm = '%s' where metrix_row_id = %s", MetrixDateTimeHelper.convertDateTimeFromDateToDB(finalEndDate.toDate()).toString(), MetrixDatabaseManager.getFieldStringValue(String.format("select metrix_row_id from task where task_id = '%s'", MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id"))));
			MetrixDatabaseManager.executeSql(query);
		}
		catch (Exception ex)
		{
			LogManager.getInstance().error(ex);
		}
	}

	@Override
	public void onChronometerTick(Chronometer arg0) {
		this.mElapsedTime = this.mElapsedTime + 1000;

		long seconds = (long) (this.mElapsedTime / 1000) % 60;
		long minutes = (long) ((this.mElapsedTime / (1000 * 60)) % 60);
		long hours = (long) ((this.mElapsedTime / (1000 * 60 * 60)) % 24);

		this.mTimeClockChronometer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
	}

	private boolean taskProductHasGPS() {
		String prodLat = MetrixControlAssistant.getValue(mFormDef, mLayout, "product", "geocode_lat");
		String prodLong = MetrixControlAssistant.getValue(mFormDef, mLayout, "product", "geocode_long");

		// not valid GPS if one or more coordinates missing
		if (MetrixStringHelper.isNullOrEmpty(prodLat) || MetrixStringHelper.isNullOrEmpty(prodLong))
			return false;

		return true;
	}

	private void handleStatusChange() {
		try {
			MetrixControlAssistant.setValue(mFormDef, mLayout, "task", "status_as_of", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS));
		} catch (Exception e2) {
			e2.printStackTrace();
		}

		String currentStatus = MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_status");

		// setup task_status based on the role/function settings for the person
		MetrixLocationAssistant.setGeocodeForTaskStatus(mCurrentActivity, mFormDef, mLayout);
		MetrixTimeClockAssistant.updateAutomatedTimeClock(this, currentStatus);
	}

	private void handleTaskTypeChange() {
		String taskType = MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_type");
		String workflowName = MetrixWorkflowManager.getDebriefWorkflowNameForTaskType(taskType);
		MetrixWorkflowManager.setCurrentWorkflowName(mCurrentActivity, workflowName);
	}

	protected void openMapWithFullAddress(Activity activity) {
		String fullAddress = MetrixControlAssistant.getValue(mFormDef, mLayout, "custom", "full_address");
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + fullAddress));
		MetrixActivityHelper.startNewActivity(activity, intent);
	}

	private void setFullAddress() {
		try {
			String address = MetrixControlAssistant.getValue(mFormDef, mLayout, "address", "address")==null? "":MetrixControlAssistant.getValue(mFormDef, mLayout, "address", "address");
			String city = MetrixControlAssistant.getValue(mFormDef, mLayout, "address", "city")==null? "":MetrixControlAssistant.getValue(mFormDef, mLayout, "address", "city");
			String state = MetrixControlAssistant.getValue(mFormDef, mLayout, "address", "state_prov")== null? "": MetrixControlAssistant.getValue(mFormDef, mLayout, "address", "state_prov");
			String zip = MetrixControlAssistant.getValue(mFormDef, mLayout, "address", "zippost")==null?"":MetrixControlAssistant.getValue(mFormDef, mLayout, "address", "zippost");

			if ((!MetrixStringHelper.isNullOrEmpty(address)) || (!MetrixStringHelper.isNullOrEmpty(city)) || (!MetrixStringHelper.isNullOrEmpty(state)) || (!MetrixStringHelper.isNullOrEmpty(zip))) {
				MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "full_address", address + " " + city + " " + state + " " + zip);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private android.view.View.OnClickListener onClickListenerWtyRenew = new android.view.View.OnClickListener() {
		@Override
		// warranty won't act as a hyperlink
		public void onClick(View v) {
			/*Intent intent = MetrixActivityHelper.createActivityIntent(DebriefOverview.this, DebriefOpportunity.class);
			intent.putExtra("opportunity_name", "Warranty Renewal");
			MetrixActivityHelper.startNewActivity(DebriefOverview.this, intent);*/
		}
	};

	private void setImportantInformation(String taskId) {
		MetrixImportantInformation.reset(mLayout, this);
		final StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("select count(*) from time_commit ");
		queryBuilder.append("join task on time_commit.task_id = task.task_id ");
		queryBuilder.append("where time_commit.actual_dttm is null and time_commit.task_id = '");
		queryBuilder.append(MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id"));
		queryBuilder.append("' and task.person_id = '");
		queryBuilder.append(User.getUser().personId);
		queryBuilder.append("'");

		String commitmentsCountStr = MetrixDatabaseManager.getFieldStringValue(queryBuilder.toString());
		int commitmentsCount = 0;
		if (!MetrixStringHelper.isNullOrEmpty(commitmentsCountStr))
			commitmentsCount = Integer.parseInt(commitmentsCountStr);

		if (commitmentsCount > 0) {
			android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = MetrixActivityHelper.createActivityIntent(DebriefOverview.this, CommitmentList.class);
					intent.putExtra("task_id", MetrixControlAssistant.getValue(mFormDef, mLayout, "task", "task_id"));
					MetrixActivityHelper.startNewActivity(DebriefOverview.this, intent);
				}
			};

			MetrixImportantInformation.add(mLayout, AndroidResourceHelper.getMessage("ThereAreCommitmentsForThisJob"), AndroidResourceHelper.getMessage("Commitments"), onClickListener);
		}

		if (!MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue(mFormDef, mLayout, "warranty_coverage", "end_dt"))) {
			android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MetrixControlAssistant.setFocus(mFormDef.getId("warranty_coverage", "end_dt"), mLayout);
				}
			};

			try {
				String mWarrantyEndDate = MetrixControlAssistant.getValue(mFormDef, mLayout, "warranty_coverage", "end_dt");
				mWarrantyEndDate = MetrixDateTimeHelper.adjustDate(mWarrantyEndDate, MetrixDateTimeHelper.DATE_FORMAT, Calendar.DAY_OF_MONTH, 1);
				Date endDate = MetrixDateTimeHelper.convertDateTimeFromUIToDate(mWarrantyEndDate);
				Date todayDate = MetrixDateTimeHelper.convertDateTimeFromUIToDate(MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_FORMAT));
				long dayDifference = MetrixDateTimeHelper.daysBetween(todayDate, endDate);
				String reminderDay = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='NOTIFY_DAYS_IF_WARRANTY_EXPIRES'");

				if (MetrixStringHelper.isInteger(reminderDay)) {
					if (dayDifference >= 0 && dayDifference < Integer.parseInt(reminderDay)) {
						if (dayDifference > 1) {
							MetrixImportantInformation.add(mLayout, AndroidResourceHelper.getMessage("JobWarrantyExpiresInDays1Args", dayDifference), "", onClickListenerWtyRenew);
						} else if (dayDifference == 1) {
							MetrixImportantInformation.add(mLayout, (AndroidResourceHelper.getMessage("JobWarrantyExpiresInDay")), "", onClickListenerWtyRenew);
						} else {
							MetrixImportantInformation.add(mLayout, (AndroidResourceHelper.getMessage("JobWarrantyExpired")), "", onClickListenerWtyRenew);
						}
					} else {
						MetrixImportantInformation.add(mLayout, (AndroidResourceHelper.getMessage("JobWarrantyCovered")), "", onClickListener);
					}
				} else if (dayDifference == 0) {
					MetrixImportantInformation.add(mLayout, (AndroidResourceHelper.getMessage("JobWarrantyExpired")), "", onClickListenerWtyRenew);
				} else {
					MetrixImportantInformation.add(mLayout, (AndroidResourceHelper.getMessage("JobWarrantyCovered")), "", onClickListener);
				}
			} catch (NumberFormatException e) {
				LogManager.getInstance(this).error(e);
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}

		if (!MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue(mFormDef, mLayout, "request_unit", "contract_id"))) {
			android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MetrixControlAssistant.setFocus(mFormDef.getId("contract", "end_dt"), mLayout);
				}
			};

			String reminderDay = MetrixDatabaseManager.getFieldStringValue("Metrix_app_params", "param_value", "param_name='NOTIFY_DAYS_IF_CONTRACT_EXPIRES'");

			if(MetrixStringHelper.isInteger(reminderDay)){
				String mContractEndDate = MetrixControlAssistant.getValue(mFormDef, mLayout, "contract", "end_dt");
				Date endDate = MetrixDateTimeHelper.convertDateTimeFromUIToDate(mContractEndDate);
				Date todayDate = MetrixDateTimeHelper.convertDateTimeFromUIToDate(MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_FORMAT));

				long dayDifference = MetrixDateTimeHelper.daysBetween(todayDate, endDate);

				if(dayDifference >=0 && dayDifference < Integer.parseInt(reminderDay)){
					android.view.View.OnClickListener onClickListener2 = new android.view.View.OnClickListener() {
						@Override
						public void onClick(View v) {
							int screenId = MetrixScreenManager.getScreenId("DebriefOpportunity");
							Intent intent = MetrixActivityHelper.createActivityIntent(DebriefOverview.this, "com.metrix.metrixmobile.system", "MetadataDebriefActivity");
							intent.putExtra("ScreenID", screenId);
							MetrixActivityHelper.startNewActivity(DebriefOverview.this, intent);
						}
					};

					if (dayDifference > 0) {
						MetrixImportantInformation.add(mLayout, AndroidResourceHelper.getMessage("JobContractWillExpire1Arg", dayDifference), AndroidResourceHelper.getMessage("ContractLowercase"), onClickListener2);
					} else {
						MetrixImportantInformation.add(mLayout, AndroidResourceHelper.getMessage("JobContractExpired"),AndroidResourceHelper.getMessage("ContractLowercase"), onClickListener2);
					}
				}
				else {
					MetrixImportantInformation.add(mLayout, AndroidResourceHelper.getMessage("JobIsUnderContract"), AndroidResourceHelper.getMessage("ContractLowercase"), onClickListener);
				}
			}
			else {
				MetrixImportantInformation.add(mLayout, AndroidResourceHelper.getMessage("JobIsUnderContract"), AndroidResourceHelper.getMessage("ContractLowercase"), onClickListener);
			}
		}

		String notifyDaysForPMs = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='NOTIFY_DAYS_IF_PMS_EXIST'");
		String notifyDaysForECOs = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='NOTIFY_DAYS_IF_ECOS_EXIST'");
		String notifyDaysForOtherTasks = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value",
				"param_name='NOTIFY_DAYS_IF_OTHER_TASKS_EXIST'");

		final String placeId = MetrixDatabaseManager.getFieldStringValue("task", "place_id_cust",
				"task_id = " + MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));

		String filter = "task.person_id = '" + User.getUser().personId + "' and task.task_status in (select task_status from task_status where status = 'OP' and task_status <> '" + MobileApplication.getAppParam("REJECTED_TASK_STATUS") + "') and task.place_id_cust = '" + placeId
				+ "' and task.task_id != " + taskId;

		int count = 0;

		if (!MetrixStringHelper.isNullOrEmpty(notifyDaysForPMs)) {
			final String enhancedFilter = filter + " and request.origin = 'AGEN' and task.plan_start_dttm < '"
					+ MetrixDateTimeHelper.getRelativeDate(MetrixDateTimeHelper.DATE_FORMAT, Integer.valueOf(notifyDaysForPMs), true, ISO8601.Yes) + "'"
					+ " and task.place_id_cust='"+placeId+"'";
			count = MetrixDatabaseManager.getCount(new String[] { "task left outer join request on task.request_id = request.request_id" }, enhancedFilter);
			if (count > 0) {
				android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MobileGlobal.jobListFilterEngaged = false;
						Intent intent = MetrixActivityHelper.createActivityIntent(DebriefOverview.this, JobList.class); //, Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.putExtra("filter", enhancedFilter);
						MetrixActivityHelper.startNewActivity(DebriefOverview.this, intent);
					}
				};
				MetrixImportantInformation.add(mLayout, AndroidResourceHelper.getMessage("CustomerHasOpenPM"), AndroidResourceHelper.getMessage("PMs"), onClickListener);
			}
		}

		if (!MetrixStringHelper.isNullOrEmpty(notifyDaysForECOs)) {
			final String enhancedFilter = filter + " and request.origin = 'ECO' and task.plan_start_dttm < '"
					+ MetrixDateTimeHelper.getRelativeDate(MetrixDateTimeHelper.DATE_FORMAT, Integer.valueOf(notifyDaysForECOs), true, ISO8601.Yes) + "'"
					+ " and task.place_id_cust='"+placeId+"'";;
			count = MetrixDatabaseManager.getCount(new String[] { "task left outer join request on task.request_id = request.request_id" }, enhancedFilter);
			if (count > 0) {
				android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MobileGlobal.jobListFilterEngaged = false;
						Intent intent = MetrixActivityHelper.createActivityIntent(DebriefOverview.this, JobList.class); //, Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.putExtra("filter", enhancedFilter);
						MetrixActivityHelper.startNewActivity(DebriefOverview.this, intent);
					}
				};
				MetrixImportantInformation.add(mLayout, AndroidResourceHelper.getMessage("CustomerHasOpenECO"), AndroidResourceHelper.getMessage("ECOs"), onClickListener);
			}
		}

		if (!MetrixStringHelper.isNullOrEmpty(notifyDaysForOtherTasks)) {
			final String enhancedFilter = filter + " and request.origin != 'AGEN' and request.origin != 'ECO' and task.plan_start_dttm < '"
					+ MetrixDateTimeHelper.getRelativeDate(MetrixDateTimeHelper.DATE_FORMAT, Integer.valueOf(notifyDaysForOtherTasks), true, ISO8601.Yes) + "'"
					+ " and task.place_id_cust='"+placeId+"'";;
			count = MetrixDatabaseManager.getCount(new String[] { "task left outer join request on task.request_id = request.request_id" }, enhancedFilter);
			if (count > 0) {
				android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MobileGlobal.jobListFilterEngaged = false;
						Intent intent = MetrixActivityHelper.createActivityIntent(DebriefOverview.this, JobList.class); //, Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.putExtra("filter", enhancedFilter);
						MetrixActivityHelper.startNewActivity(DebriefOverview.this, intent);
					}
				};
				MetrixImportantInformation.add(mLayout, AndroidResourceHelper.getMessage("OtherTaskForCustomer"), AndroidResourceHelper.getMessage("TasksLowercase"), onClickListener);
			}
		}

		ArrayList<Hashtable<String, String>> dataRows = MetrixDatabaseManager
				.getFieldStringValuesList("select part_need.status, part_need.request_id, part_need.sequence from part_need where part_need.request_id = (select request_id from task where task_id="
						+ taskId + ")");
		String status = MetrixStringHelper.getValueFromSqlResults(dataRows, "status", 0);
		String requestId = MetrixStringHelper.getValueFromSqlResults(dataRows, "request_id", 0);
		String sequence = MetrixStringHelper.getValueFromSqlResults(dataRows, "sequence", 0);

		if (MetrixStringHelper.isNullOrEmpty(status) == false && status.compareToIgnoreCase("S") != 0) {
			MetrixImportantInformation.add(mLayout, AndroidResourceHelper.getMessage("JobsPartNotShippedYet"));
		} else if (MetrixStringHelper.isNullOrEmpty(status) == false && status.compareToIgnoreCase("S") == 0) {
			dataRows = MetrixDatabaseManager
					.getFieldStringValuesList("select shipment.shipment_id, shipment.inventory_adjusted, shipment.in_transit from part_need, shipment where part_need.request_id="
							+ requestId + " and sequence=" + sequence + " and part_need.shipment_id = shipment.shipment_id");
			String shipmentAdjusted = MetrixStringHelper.getValueFromSqlResults(dataRows, "inventory_adjusted", 0);
			String inTransit = MetrixStringHelper.getValueFromSqlResults(dataRows, "in_transit", 0);

			if (shipmentAdjusted.compareToIgnoreCase("n") == 0)
				MetrixImportantInformation.add(mLayout, AndroidResourceHelper.getMessage("JobsPartNotShippedYet"));
			else if (shipmentAdjusted.compareToIgnoreCase("y") == 0 && inTransit.compareToIgnoreCase("y") == 0) {
				dataRows = MetrixDatabaseManager
						.getFieldStringValuesList("select receiving.inventory_adjusted from receiving, shipment where receiving.shipment_id = shipment.shipment_id");
				String inventory_adjusted = MetrixStringHelper.getValueFromSqlResults(dataRows, "inventory_adjusted", 0);

				if (inventory_adjusted.compareToIgnoreCase("n") == 0) {
					android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = MetrixActivityHelper.createActivityIntent(DebriefOverview.this, ReceivingList.class);
							MetrixActivityHelper.startNewActivity(DebriefOverview.this, intent);
						}
					};

					MetrixImportantInformation.add(mLayout, AndroidResourceHelper.getMessage("JobsPartNeedToBeReceived"),
							AndroidResourceHelper.getMessage("ReceivedLowercase"), onClickListener);
				}
			}
		}

		String meterReadingsMessage = MeterReadingsPolicy.generateErrorForOutstandingReadings(taskId);

		if (!MetrixStringHelper.isNullOrEmpty(meterReadingsMessage)) {
			MetrixImportantInformation.add(mLayout, meterReadingsMessage);
		}
	}

	@Override
	public boolean implicitSwipeSave() {
		if (anyOnStartValuesChanged()) {
			if (MetrixControlAssistant.valueChanged(mFormDef.getId("task", "task_status"), mFormDef, mLayout)) {
				handleStatusChange();
			}

			MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
			MetrixSaveResult result = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, false, AndroidResourceHelper.getMessage("Task"));

			if (result == MetrixSaveResult.SUCCESSFUL) {
				return true;
			} else {
				return false;
			}
		}

		return true;
	}

	//Tablet UI Optimization
	@Override
	public boolean isTabletSpecificLandscapeUIRequired()
	{
		return true;
	}
	//End Tablet UI Optimization
}


