package com.metrix.metrixmobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixHomeMenuManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global.ActivityType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper.ISO8601;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.global.MetrixImportantInformation;
import com.metrix.metrixmobile.global.MetrixWorkStatusAssistant;
import com.metrix.metrixmobile.global.MobileGlobal;
import com.metrix.metrixmobile.system.DemoWelcome;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class Home extends MetrixActivity implements View.OnClickListener {

	private static final int INITIAL_LOCATION_PERMISSION_REQUEST = 1221;
	private int incompletePermissionRequest = 0;
	private boolean isGoneToSettingsForInitialLocation = false;
	private boolean hasEvenItemCount = false;
	TextView mInformation;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		hasEvenItemCount = MetrixHomeMenuManager.hasEvenItemCount();
		if (hasEvenItemCount)
			setContentView(R.layout.home_even);
		else
			setContentView(R.layout.home_odd);

		MetrixPublicCache.instance.addItem("person_id", User.getUser().personId);

		final boolean askedLocationPermission = SettingsHelper.getBooleanSetting(this,
				SettingsHelper.ASKED_INIT_LOCATION_PERMISSION);

		//Handling Location ASK_EVERY_TIME Permission
		boolean requestLocationPermission = false;
		if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
			if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
				requestLocationPermission = true;
			}
		}

		if (!askedLocationPermission || requestLocationPermission ) {
			// This is the first time user has initialised the app. Ask for location permission.
			SettingsHelper.saveBooleanSetting(this, SettingsHelper.ASKED_INIT_LOCATION_PERMISSION, true);
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
					INITIAL_LOCATION_PERMISSION_REQUEST);
		}

		MetrixLocationAssistant.startLocationManager(this);

	}

	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.important_information_heading, "ImportantInformation"));
		super.onStart();

		mLayout = (ViewGroup) findViewById(R.id.home_background);

		enhanceButtonText();
		addImportantInformation();

		if (MetrixApplicationAssistant.getMetaBooleanValue(this, "DemoBuild")) {
			if (!MetrixStringHelper.isNullOrEmpty(SettingsHelper.getStringSetting(this, "DemoBuildFirstRun"))) {
				SettingsHelper.saveStringSetting(this, "DemoBuildFirstRun", "", true);
				Intent intent = MetrixActivityHelper.createActivityIntent(this, DemoWelcome.class);
				MetrixActivityHelper.startNewActivity(this, intent);
			}
		}
	}

	public void onStop() {
		clearOutButtonImages();

		super.onStop();
	}

	//utility method of workStatus
	private void countStringEnhancement(TextView countTextView, String workStatus) {
		countTextView.setTextSize(8);
		countTextView.setLines(2);
		String[] statusArray = workStatus.split(" ");
		if (statusArray.length == 1) {
			countTextView.setText(statusArray[0]);
		} else if (statusArray.length == 2) {
			countTextView.setText(statusArray[0]
					+ System.getProperty("line.separator") + statusArray[1]);
		} else {
			countTextView.setText(workStatus);
		}
	}

	//rendering the details of each home menu item
	private void setDetails(HashMap<String, String> homeMenuItem, HashMap<String, Object> resourceMap, int homeItemIndex) {
		String key = homeMenuItem.get("item_name");
		String displayText = homeMenuItem.get("label");
		String imageID = homeMenuItem.get("image_id");
		String countScript = homeMenuItem.get("count_script");
		TextView headingTextView = (TextView) resourceMap.get("HeadingTextView");
		headingTextView.setText(displayText);

		TextView countTextView = (TextView) resourceMap.get("CountTextView");
		float scale = getResources().getDisplayMetrics().density;
		float radius = 10f * scale + 0.5f;
		MetrixSkinManager.setFirstGradientColorsForTextView(countTextView, radius);
		countTextView.setVisibility(View.VISIBLE);

		ImageButton imageButton = (ImageButton) resourceMap.get("ImageButton");
		// Since Glide won't work on views with tags, We'll have to delegate the image button's click functionality to its parent
		imageButton.setClickable(false);
		View parent = ((View)imageButton.getParent());
		parent.setClickable(true);
		parent.setFocusable(true);
		parent.setOnClickListener(this);
		parent.setTag(homeItemIndex);

		// if we don't use the use_mm_home_item.image_id, we'll use this imageResourceID
		int imageResourceID = getDefaultImageResourceID(key);

		// use count_script to determine countTextView content and visibility
		boolean countDisplayed = false;
		if (!MetrixStringHelper.isNullOrEmpty(countScript)) {
			ClientScriptDef countScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(countScript);
			if (countScriptDef != null) {
				Object countObj = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(this), countScriptDef);
				if (countObj != null) {
					String countString = "";
					if (countObj instanceof String)
						countString = String.valueOf(countObj);
					else if (countObj instanceof Double) {
						Double countDbl = (Double) countObj;
						countString = String.valueOf(countDbl.intValue());
					}

					if (!MetrixStringHelper.isNullOrEmpty(countString) && !MetrixStringHelper.valueIsEqual(countString, "0")) {
						countStringEnhancement(countTextView, countString);
						countDisplayed = true;
					}
				}
			}
		}
		if (!countDisplayed)
			countTextView.setVisibility(View.INVISIBLE);

		boolean didUseImageID = false;
		if (!MetrixStringHelper.isNullOrEmpty(imageID)) {
			didUseImageID = applyImageWithGlide(imageID, imageButton);
		}

		if (!didUseImageID) {
			try {
				Glide.with(this)
						.load(imageResourceID)
						.apply(RequestOptions.centerCropTransform())
						.into(imageButton);
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		}
	}

	private boolean applyImageWithGlide(String imageID, ImageView view) {
		try {
			String fullPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + imageID;
			File imageFile = new File(fullPath);
			if (imageFile.exists()) {
				Glide.with(this)
						.load(imageFile)
						.apply(RequestOptions.centerCropTransform())
						.into(view);
				return true;
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return false;
	}

	private int getDefaultImageResourceID(String key) {
		int imageResourceID = R.drawable.home_admin;		// this is the default if nothing is called out below

		if (key.compareToIgnoreCase("About") == 0)
			imageResourceID = R.drawable.home_about;
		else if (key.compareToIgnoreCase("Customers") == 0)
			imageResourceID = R.drawable.home_customers;
		else if (key.compareToIgnoreCase("Escalations") == 0)
			imageResourceID = R.drawable.home_escalations;
		else if (key.compareToIgnoreCase("Jobs") == 0)
			imageResourceID = R.drawable.home_jobs;
		else if (key.compareToIgnoreCase("Profile") == 0)
			imageResourceID = R.drawable.home_profile;
		else if (key.compareToIgnoreCase("Query") == 0)
			imageResourceID = R.drawable.home_query;
		else if ((key.compareToIgnoreCase("Receiving") == 0) || (key.compareToIgnoreCase("Shipping") == 0) || (key.compareToIgnoreCase("Purchase Order") == 0))
			imageResourceID = R.drawable.home_receiving;
		else if (key.compareToIgnoreCase("Close App") == 0)
			imageResourceID = R.drawable.home_close_app;
		else if ((key.compareToIgnoreCase("Stock") == 0) || (key.compareToIgnoreCase("Stock Count") == 0))
			imageResourceID = R.drawable.home_stock;
		else if (key.compareToIgnoreCase("Sync") == 0)
			imageResourceID = R.drawable.home_sync;
		else if (key.compareToIgnoreCase("Team") == 0)
			imageResourceID = R.drawable.home_team;
		else if ((key.compareToIgnoreCase("Work Status") == 0) || (key.compareToIgnoreCase("Time Reporting") == 0))
			imageResourceID = R.drawable.home_work_status;

		return imageResourceID;
	}

	//keeping the resources of home layout
	@SuppressLint("UseSparseArrays")
	private HashMap<Integer, HashMap<String, Object>> getHomeItemResources() {
		HashMap<Integer, HashMap<String, Object>> ResourceHashMap = new HashMap<Integer, HashMap<String, Object>>();

		HashMap<String, Object> PriorityFirst = new HashMap<String, Object>();
		PriorityFirst.put("HeadingTextView", (TextView) findViewById(R.id.home_priority_first_heading));
		PriorityFirst.put("CountTextView", (TextView) findViewById(R.id.home_priority_first_count));
		PriorityFirst.put("ImageButton", (ImageButton) findViewById(R.id.home_priority_first_button));
		ResourceHashMap.put(1, PriorityFirst);

		HashMap<String, Object> PrioritySecond = new HashMap<String, Object>();
		PrioritySecond.put("HeadingTextView", (TextView) findViewById(R.id.home_priority_second_heading));
		PrioritySecond.put("CountTextView", (TextView) findViewById(R.id.home_priority_second_count));
		PrioritySecond.put("ImageButton", (ImageButton) findViewById(R.id.home_priority_second_button));
		ResourceHashMap.put(2, PrioritySecond);

		HashMap<String, Object> PriorityThird = new HashMap<String, Object>();
		PriorityThird.put("HeadingTextView", (TextView) findViewById(R.id.home_priority_third_heading));
		PriorityThird.put("CountTextView", (TextView) findViewById(R.id.home_priority_third_count));
		PriorityThird.put("ImageButton", (ImageButton) findViewById(R.id.home_priority_third_button));
		ResourceHashMap.put(3, PriorityThird);

		HashMap<String, Object> PriorityFourth = new HashMap<String, Object>();
		PriorityFourth.put("HeadingTextView", (TextView) findViewById(R.id.home_priority_fourth_heading));
		PriorityFourth.put("CountTextView", (TextView) findViewById(R.id.home_priority_fourth_count));
		PriorityFourth.put("ImageButton", (ImageButton) findViewById(R.id.home_priority_fourth_button));
		ResourceHashMap.put(4, PriorityFourth);

		HashMap<String, Object> PriorityFifth = new HashMap<String, Object>();
		PriorityFifth.put("HeadingTextView", (TextView) findViewById(R.id.home_priority_fifth_heading));
		PriorityFifth.put("CountTextView", (TextView) findViewById(R.id.home_priority_fifth_count));
		PriorityFifth.put("ImageButton", (ImageButton) findViewById(R.id.home_priority_fifth_button));
		ResourceHashMap.put(5, PriorityFifth);

		HashMap<String, Object> PrioritySixth = new HashMap<String, Object>();
		PrioritySixth.put("HeadingTextView", (TextView) findViewById(R.id.home_priority_sixth_heading));
		PrioritySixth.put("CountTextView", (TextView) findViewById(R.id.home_priority_sixth_count));
		PrioritySixth.put("ImageButton", (ImageButton) findViewById(R.id.home_priority_sixth_button));
		ResourceHashMap.put(6, PrioritySixth);

		HashMap<String, Object> PrioritySeventh = new HashMap<String, Object>();
		PrioritySeventh.put("HeadingTextView", (TextView) findViewById(R.id.home_priority_seventh_heading));
		PrioritySeventh.put("CountTextView", (TextView) findViewById(R.id.home_priority_seventh_count));
		PrioritySeventh.put("ImageButton", (ImageButton) findViewById(R.id.home_priority_seventh_button));
		ResourceHashMap.put(7, PrioritySeventh);

		if (hasEvenItemCount) {
			// Item 8 only exists in EVEN case
			HashMap<String, Object> PriorityEighth = new HashMap<String, Object>();
			PriorityEighth.put("HeadingTextView", (TextView) findViewById(R.id.home_priority_eighth_heading));
			PriorityEighth.put("CountTextView", (TextView) findViewById(R.id.home_priority_eighth_count));
			PriorityEighth.put("ImageButton", (ImageButton) findViewById(R.id.home_priority_eighth_button));
			ResourceHashMap.put(8, PriorityEighth);
		}

		return ResourceHashMap;
	}

	//rendering the home-menu-screen
	private void enhanceButtonText() {
		Map<Integer, HashMap<String, String>> homeMenuItems = MetrixHomeMenuManager.getHomeMenuItems();
		HashMap<Integer, HashMap<String, Object>> resourceList = getHomeItemResources();

		if ((homeMenuItems == null) || (homeMenuItems.size() == 0)) {
			MetrixUIHelper.showSnackbar(this, AndroidResourceHelper.getMessage("HomeMenuItemsRenderingError"));
			return;
		}

		for (int i = 1; i <= homeMenuItems.size(); i++) {
			HashMap<String, String> homeMenuItem = homeMenuItems.get(i);
			HashMap<String, Object> resourceMap = resourceList.get(i);
			setDetails(homeMenuItem, resourceMap, i);
		}

		// Make any items for which we don't have metadata GONE, knowing that max item is 8 in the EVEN case, 7 in ODD
		int itemCount = homeMenuItems.size();
		if (itemCount < 7) {
			// We need to hide at least one pair of items
			// For ODD, these will be TableRows ... For EVEN, these will be LinearLayouts
			if (hasEvenItemCount) {
				if (itemCount < 5) {
					View row56 = findViewById(R.id.home_item_5and6);
					row56.setVisibility(View.GONE);
				}
				View row78 = findViewById(R.id.home_item_7and8);
				row78.setVisibility(View.GONE);
			} else {
				if (itemCount < 4) {
					View row45 = findViewById(R.id.home_item_4and5);
					row45.setVisibility(View.GONE);
				}
				View row67 = findViewById(R.id.home_item_6and7);
				row67.setVisibility(View.GONE);
			}
		}
	}

	//free up memory being used by item images when activity is not being displayed (regenerated at onStart)
	private void clearOutButtonImages() {
		HashMap<Integer, HashMap<String, Object>> resourceList = getHomeItemResources();
		for (int i = 1; i <= resourceList.size(); i++) {
			HashMap<String, Object> resourceMap = resourceList.get(i);
			ImageButton imageButton = (ImageButton) resourceMap.get("ImageButton");
			imageButton.setImageResource(0);
			imageButton.setImageBitmap(null);
		}
	}

	private void addImportantInformation() {
		mLayout = (ViewGroup) findViewById(R.id.home_background);
		MetrixImportantInformation.reset(mLayout, this);
		String personId = User.getUser().personId;

		// when changing the font size
		// get 30% of the screen height as the maximum height for the important information area
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		int maxHeightForImportantInfoArea = (int) (displayMetrics.heightPixels * 0.3);

		// when changing the font size
		// assign maximum height for the scrollView in important information area
		ScrollView scrollView = (ScrollView) findViewById(R.id.importantInfoScrollView);
		ConstraintLayout constraintLayout = findViewById(R.id.home_background);
		ConstraintSet constraintSet = new ConstraintSet();
		constraintSet.clone(constraintLayout);
		constraintSet.constrainMaxHeight(scrollView.getId(), maxHeightForImportantInfoArea);
		constraintLayout.setConstraintSet(constraintSet);

		// New Jobs
		String extraFilter = MetrixDatabaseManager.getAppParam("ADDITIONAL_JOB_LIST_CONSTRAINTS");
		String filter = "task_status = 'OPEN' and request_id is not null and person_id='" + personId + "'";
		if(!MetrixStringHelper.isNullOrEmpty(extraFilter)){
			filter += " and ("+extraFilter+")";
		}
		int jobsCount = MetrixDatabaseManager.getCount("task", filter);
		if (jobsCount > 0) {
			String message = "";
			if (jobsCount == 1)
				message = AndroidResourceHelper.getMessage("AssignedANewJob");
			else
				message = AndroidResourceHelper.getMessage("AssignedNewJobs1Arg", jobsCount);

			android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = MetrixActivityHelper.createActivityIntent(Home.this, JobList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.putExtra("filter", AndroidResourceHelper.getMessage("New"));
					MetrixActivityHelper.startNewActivity(Home.this, intent);
				}
			};
			MetrixImportantInformation.add(mLayout, message, AndroidResourceHelper.getMessage("NewLCase"), onClickListener);
		}

		// Commitments
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("select count(*) from time_commit ");
		queryBuilder.append("join task on time_commit.task_id = task.task_id ");
		queryBuilder.append("where time_commit.actual_dttm is null and commit_dttm < '"
								+ MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true) + "'");
		queryBuilder.append(" and task.person_id = '");
		queryBuilder.append(User.getUser().personId);
		queryBuilder.append("'");
		String commitmentsCountStr = MetrixDatabaseManager.getFieldStringValue(queryBuilder.toString());
		if(!MetrixStringHelper.isNullOrEmpty(commitmentsCountStr)) {
			int commitmentsCount = 0;
			if (!MetrixStringHelper.isNullOrEmpty(commitmentsCountStr))
				commitmentsCount = Integer.parseInt(commitmentsCountStr);

			if (commitmentsCount > 0) {
				String message = "";
				String hyperlink = "";
				if (commitmentsCount == 1) {
					message = AndroidResourceHelper.getMessage("AnOverdueCommitment");
					hyperlink = AndroidResourceHelper.getMessage("CommitmentLCase");
				} else {
					message = AndroidResourceHelper.getMessage("OverdueCommitments1Arg", commitmentsCount);
					hyperlink = AndroidResourceHelper.getMessage("CommitmentsLCase");
				}
				android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = MetrixActivityHelper.createActivityIntent(Home.this, CommitmentList.class);
						intent.putExtra("filter", AndroidResourceHelper.getMessage("Overdue"));
						MetrixActivityHelper.startNewActivity(Home.this, intent);
					}
				};
				MetrixImportantInformation.add(mLayout, message, hyperlink, onClickListener);
			}
		}
		// Escalations
		int escalationsCount = MetrixDatabaseManager.getCount("escalation", "(status is null or status != 'CLOSED') and escalation.table_name = 'TASK' and person_id = '" + personId + "'");
		if (escalationsCount > 0) {
			String message = "";
			if (escalationsCount == 1)
				message = AndroidResourceHelper.getMessage("AnEscalation");
			else
				message = AndroidResourceHelper.getMessage("Escalations1Arg", escalationsCount);

			android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = MetrixActivityHelper.createActivityIntent(Home.this, EscalationList.class);
					MetrixActivityHelper.startNewActivity(Home.this, intent);
				}
			};
			MetrixImportantInformation.add(mLayout, message, AndroidResourceHelper.getMessage("AssignedLCase"), onClickListener);
		}

		// Overdue Jobs
		filter = "task_status in (select task_status from task_status where status = 'OP' and task_status <> '" + MobileApplication.getAppParam("REJECTED_TASK_STATUS") + "') and plan_start_dttm < '"
				+ MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS,true)
				+ "' and request_id is not null and person_id='"
				+ personId + "'";
		if (!MetrixStringHelper.isNullOrEmpty(extraFilter)) {
			filter += " and ("+extraFilter+")";
		}
		int overdueCount = MetrixDatabaseManager.getCount("task", filter);
		if (overdueCount > 0) {
			String message = "";
			if (overdueCount == 1)
				message = AndroidResourceHelper.getMessage("AnOverdueJob");
			else
				message = AndroidResourceHelper.getMessage("JobsOverdue1Arg", overdueCount);

			android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = MetrixActivityHelper.createActivityIntent(Home.this, JobList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.putExtra("filter", AndroidResourceHelper.getMessage("Overdue"));
					MetrixActivityHelper.startNewActivity(Home.this, intent);
				}
			};
			MetrixImportantInformation.add(mLayout, message, AndroidResourceHelper.getMessage("OverdueLCase"), onClickListener);
		}

		// Expiring Password
		String value = MetrixDatabaseManager.getFieldStringValue("person", "password_expire_dt", "person_id = '" + personId + "'");
		Calendar expireDate = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, value, ISO8601.Yes);
		Calendar fifteenDays = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, MetrixDateTimeHelper.getRelativeDate(MetrixDateTimeHelper.DATE_FORMAT, 15));
		if (expireDate.before(fifteenDays)) {
			String message = AndroidResourceHelper.getMessage("PasswordExpiringSoon1Arg", MetrixDateTimeHelper.convertDateTimeFromDBToUI(value, MetrixDateTimeHelper.DATE_FORMAT));
			MetrixImportantInformation.add(mLayout, message);
		}

		// Stock Counts
		ArrayList<Hashtable<String, String>> stockCountList = MetrixDatabaseManager.getFieldStringValuesList("select distinct run_id from stock_count where posted is null and (submitted is null or submitted = 'N')");
		if (stockCountList != null) {
			if (stockCountList.size() > 0) {
				String message = "";
				if (stockCountList.size() == 1)
					message = AndroidResourceHelper.getMessage("ANewStockCount");
				else
					message = AndroidResourceHelper.getMessage("NewStockCounts1Arg", stockCountList.size());

				android.view.View.OnClickListener onClickListener = new android.view.View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MetrixPublicCache.instance.addItem("ToBarcode", "N");
						Intent intent = MetrixActivityHelper.createActivityIntent(Home.this, StockCount.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
						MetrixActivityHelper.startNewActivity(Home.this, intent);
					}
				};
				MetrixImportantInformation.add(mLayout, message, AndroidResourceHelper.getMessage("NewLCase"), onClickListener);
			}
		}
	}

	@Override
	public void onClick(View v) {
		Intent intent = null;
		//using the tag to identify the button
		Map<Integer, HashMap<String, String>> homeMenuItems = MetrixHomeMenuManager.getHomeMenuItems();
		Object tagObj = v.getTag();

		if(tagObj == null) {
			super.onClick(v);
			return;
		}

		int tagNumber = (Integer) tagObj;
		HashMap<String, String> homeMenuItem = homeMenuItems.get(tagNumber);
		String itemName = homeMenuItem.get("item_name");
		String screenIdString = homeMenuItem.get("screen_id");
		String tapEvent = homeMenuItem.get("tap_event");

		if (!MetrixStringHelper.isNullOrEmpty(itemName)) {
			if (itemName.compareToIgnoreCase("Close App") == 0) {
				android.os.Process.killProcess(android.os.Process.myPid());
			} else if (itemName.compareToIgnoreCase("Work Status") == 0) {
				MetrixWorkStatusAssistant statusAssistant = new MetrixWorkStatusAssistant();
				statusAssistant.displayStatusDialog(this, this, true);
			} else if (!MetrixStringHelper.isNullOrEmpty(screenIdString)) {
				int screenId = Integer.valueOf(screenIdString);
				String screenName = MetrixScreenManager.getScreenName(screenId);
				if (MetrixApplicationAssistant.screenNameHasClassInCode(screenName)) {
					intent = MetrixActivityHelper.createActivityIntent(this, screenName);
					MetrixActivityHelper.startNewActivity(this, intent);
				} else {
					// Right now, we only support codeless tab parents and codeless non-workflow screens.
					String screenType = MetrixScreenManager.getScreenType(screenId);
					if (MetrixStringHelper.valueIsEqual(screenType, "TAB_PARENT")) {
						intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "MetadataTabActivity");
						intent.putExtra("ScreenID", screenId);
						MetrixActivityHelper.startNewActivity(this, intent);
					} else if (MetrixStringHelper.valueIsEqual(screenType, "STANDARD")) {
						intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "MetadataStandardActivity");
						intent.putExtra("ScreenID", screenId);
						MetrixActivityHelper.startNewActivity(this, intent);
					} else if (screenType.toLowerCase().contains("list")) {
						intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "MetadataListActivity");
						intent.putExtra("ScreenID", screenId);
						MetrixActivityHelper.startNewActivity(this, intent);
					} else
						LogManager.getInstance().error(String.format("The %1$s screen (screen_id=%2$s) cannot be used as a codeless screen from the Home menu.", screenName, screenIdString));
				}
			} else if (!MetrixStringHelper.isNullOrEmpty(tapEvent)) {
				ClientScriptDef tapEventScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(tapEvent);
				if (tapEventScriptDef != null)
					MetrixClientScriptManager.executeScript(new WeakReference<Activity>(this), tapEventScriptDef);
			} else {
				LogManager.getInstance().error(String.format("Cannot handle click event for Home menu item named %s.", itemName));
			}
		} else {
			super.onClick(v);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case MobileGlobal.UPDATE_WORK_STATUS_RESULT:
				if (resultCode == RESULT_OK) {
					this.enhanceButtonText();
				}
				break;
		}
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
		public void newSyncStatus(final ActivityType activityType,
								  final String message) {
			runOnUiThread(new Runnable() {
				public void run() {
					if (activityType == ActivityType.Download) {
						if (message.compareTo("TASK") == 0||message.compareTo("RECEIVING")==0) {
							enhanceButtonText();
							addImportantInformation();
						}
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		if (isGoneToSettingsForInitialLocation) {
			isGoneToSettingsForInitialLocation = false;
			MetrixLocationAssistant.startLocationManager(this);
		}

		if (incompletePermissionRequest == INITIAL_LOCATION_PERMISSION_REQUEST) {
			final int requestCode = incompletePermissionRequest;
			incompletePermissionRequest = 0;
			DialogInterface.OnClickListener listener = (dialog, which) -> {
				if (ActivityCompat.shouldShowRequestPermissionRationale(Home.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
					// can request permission again
					ActivityCompat.requestPermissions(Home.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
				} else {
					// user need to go to app settings to enable it
					try {
						isGoneToSettingsForInitialLocation = true;
						Intent intent = new Intent();
						intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
						Uri uri = Uri.fromParts("package", getPackageName(), null);
						intent.setData(uri);
						startActivity(intent);
					} catch (ActivityNotFoundException ex) {
						// This is extremely rare
						isGoneToSettingsForInitialLocation = false;
						MetrixUIHelper.showSnackbar(Home.this, R.id.coordinator_layout, AndroidResourceHelper
								.getMessage("EnablePermManuallyAndRetry"));
					}
				}
			};

			MetrixDialogAssistant.showAlertDialog(
					AndroidResourceHelper.getMessage("PermissionRequired"),
					AndroidResourceHelper.getMessage("LocationPermFirstLaunchExpl"),
					AndroidResourceHelper.getMessage("Yes"),
					listener,
					AndroidResourceHelper.getMessage("No"),
					null,
					this
			);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == INITIAL_LOCATION_PERMISSION_REQUEST) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// User granted location permission. Start location manager.
				MetrixLocationAssistant.startLocationManager(this);
			} else {
				// Showing dialog at this Activity lifecycle can lead to app crash as the view is not guaranteed to
				// be visible to the user. So we set the incompletePermissionRequest value and handle it inside
				// onResume activity life cycle
				incompletePermissionRequest = requestCode;
			}
		}
	}

}