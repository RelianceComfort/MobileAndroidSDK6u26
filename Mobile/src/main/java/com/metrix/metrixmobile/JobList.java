package com.metrix.metrixmobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.FilterSortItem;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixFilterSortManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.Global.ActivityType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActionView;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixLibraryHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixRoleHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.global.MobileGlobal;
import com.metrix.metrixmobile.system.MetrixActivity;
import com.metrix.metrixmobile.system.MetrixPooledTaskAssignmentManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@SuppressLint("DefaultLocale")
public class JobList extends MetrixActivity implements View.OnClickListener, TextWatcher, MetrixRecyclerViewListener {
	private RecyclerView recyclerView;
	private MetadataRecyclerViewAdapter mAdapter;
	private TextView mFilterName, mSortName;
	private ImageView mSortIcon;
	private String mLastFilterName, mLastSortName, mLastSortOrder;
	private int mScreenId, mFirstGradientTextColor;
	private boolean mIsSortAscending;
	private static int listViewPosition = -1;
	private ArrayList<String> mSortOptions;
	private ArrayList<String> mFilterOptions;
	private static String receivedFilter;
	private int filteredResults = 0;

	private static final int INITIAL_LOCATION_PERMISSION_REQUEST = 1221;
	private static final int TASK_SEARCH_LOCATION_PERMISSION_REQUEST = 1222;
	private int incompletePermissionRequest = 0;
	private boolean isGoneToSettingsForInitialLocation = false;
	private boolean shouldShowTeamTaskSearch = false;
	private boolean screenShouldRefreshPTA = false;
	private boolean isPooledTask;

	private EditText mSearchCriteria;
	private Handler mHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.job_list);
		mScreenId = MetrixScreenManager.getScreenId(this);

		String firstGradientTextColor = MetrixSkinManager.getFirstGradientTextColor();
		if (MetrixStringHelper.isNullOrEmpty(firstGradientTextColor))
			firstGradientTextColor = "#FFFFFF";
		mFirstGradientTextColor = Color.parseColor(firstGradientTextColor);

        mHandler = new Handler();
		recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);

		final boolean askedLocationPermission = SettingsHelper.getBooleanSetting(this,
				SettingsHelper.ASKED_INIT_LOCATION_PERMISSION);

		//Handling Location ASK_EVERY_TIME Permission
		boolean requestLocationPermission = false;
		if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
				ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			MobileGlobal.jobListFilterEngaged = false;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.search_criteria, "Search", true));
		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);

		//adding barcode scanning to search field.
		String barcodingEnabled = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_BARCODE_SCANNING'");
		if (!MetrixStringHelper.isNullOrEmpty(barcodingEnabled) && barcodingEnabled.compareToIgnoreCase("Y") == 0) {
			registerForMetrixActionView(findViewById(R.id.search_criteria), getMetrixActionBar().getCustomView());
		}

		View filterBar = findViewById(R.id.filter_bar);
		MetrixSkinManager.setFirstGradientBackground(filterBar, 0);
		mFilterName = (TextView) findViewById(R.id.filter_bar__filter_name);
		mSortName = (TextView) findViewById(R.id.filter_bar__sort_name);
		mSortIcon = (ImageView) findViewById(R.id.filter_bar__sort_icon);

		boolean handlingFilterViaIntent = false;
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			receivedFilter = extras.getString("filter");
			if (!MobileGlobal.jobListFilterEngaged && !MetrixStringHelper.isNullOrEmpty(receivedFilter)) {
				if (receivedFilter.compareToIgnoreCase(AndroidResourceHelper.getMessage("New")) == 0) {
					mLastFilterName =  MetrixFilterSortManager.getFilterLabelByItemName("New", mScreenId);
				} else if (receivedFilter.compareToIgnoreCase( AndroidResourceHelper.getMessage("Overdue")) == 0) {
					mLastFilterName =  MetrixFilterSortManager.getFilterLabelByItemName("Overdue", mScreenId);
				} else {
					// This will be a signal to use receivedFilter directly as the filter to use
					mLastFilterName = AndroidResourceHelper.getMessage("OtherJobs");
				}

				handlingFilterViaIntent = true;
			}
		}

		if (!handlingFilterViaIntent) {
			mLastFilterName = SettingsHelper.getStringSetting(this, MetrixFilterSortManager.getSelectedFilterItemSettingName("JobList"));
			if (MetrixStringHelper.isNullOrEmpty(mLastFilterName))
				mLastFilterName = MetrixFilterSortManager.getDefaultFilterItemLabel(mScreenId);
		}

		mLastSortName = SettingsHelper.getStringSetting(this, MetrixFilterSortManager.getSelectedSortItemSettingName("JobList"));
		if (MetrixStringHelper.isNullOrEmpty(mLastSortName))
			mLastSortName = MetrixFilterSortManager.getDefaultSortItemLabel(mScreenId);

		mLastSortOrder = SettingsHelper.getStringSetting(this, MetrixFilterSortManager.getSortOrderSettingName("JobList"));
		if (MetrixStringHelper.isNullOrEmpty(mLastSortOrder))
			mLastSortOrder = " asc";
		mIsSortAscending = MetrixStringHelper.valueIsEqual(mLastSortOrder, " asc");

		mFilterName.setTextColor(mFirstGradientTextColor);
		mSortName.setTextColor(mFirstGradientTextColor);

		mFilterName.setOnClickListener(this);
		mSortName.setOnClickListener(this);

		// Only show filter/sort if there are items in metadata
		boolean hasFilters = MetrixFilterSortManager.hasFilterItems(mScreenId);
		boolean hasSorts = MetrixFilterSortManager.hasSortItems(mScreenId);
		if (!hasFilters && !hasSorts && !handlingFilterViaIntent)
			filterBar.setVisibility(View.GONE);		// hide the filter/sort bar entirely
		else {
			filterBar.setVisibility(View.VISIBLE);
			if (hasFilters || handlingFilterViaIntent)
				mFilterName.setVisibility(View.VISIBLE);
			else
				mFilterName.setVisibility(View.GONE);

			if (hasSorts) {
				mSortName.setVisibility(View.VISIBLE);
				mSortIcon.setVisibility(View.VISIBLE);
			} else {
				mSortName.setVisibility(View.GONE);
				mSortIcon.setVisibility(View.GONE);
			}
		}

		populateList();
		setJobSearchPanel();
		registerForMetrixActionView(mLayout, getMetrixActionBar().getCustomView());
	}

	@Override
	protected void bindService() {
		bindService(new Intent(JobList.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			// mBoundService = ((MetrixIntentService.LocalBinder) binder)
			// .getService();

			try {
				service = (IPostMonitor) binder;
				service.registerListener(listener);
			} catch (Throwable t) {
				LogManager.getInstance().error(t);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			// mBoundService = null;
			service = null;
		}
	};

	protected com.metrix.architecture.services.IPostListener listener = new com.metrix.architecture.services.IPostListener() {
		public void newSyncStatus(final ActivityType activityType, final String message) {
			runOnUiThread(new Runnable() {
				public void run() {
					if (activityType == ActivityType.Download) {
						if (message.compareTo("TASK") == 0) {
							populateList();
						}
						else if(!MetrixStringHelper.isNullOrEmpty(message) && message.contains("{\"task_hierarchy_select_result\":")){
							if(screenShouldRefreshPTA){
								mUIHelper.dismissLoadingDialog();
							}
						}
						else if (MetrixStringHelper.valueIsEqual(message, "{\"END_PTAS\":null}")) {
							MobileApplication.stopSync(mCurrentActivity);
							MobileApplication.startSync(mCurrentActivity);
							screenShouldRefreshPTA = true;

						} else if (MetrixStringHelper.valueIsEqual(message, "{\"END_PTATAA\":null}")) {
							MobileApplication.stopSync(mCurrentActivity);
							MobileApplication.startSync(mCurrentActivity);
							mUIHelper.dismissLoadingDialog();
							MetrixUIHelper.showSnackbar(JobList.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("PTaskAssignmentAlreadyAssigned"));

						} else if (MetrixStringHelper.valueIsEqual(message, "{\"END_PTDNE\":null}")) {
							MobileApplication.stopSync(mCurrentActivity);
							MobileApplication.startSync(mCurrentActivity);
							mUIHelper.dismissLoadingDialog();

						} else if (MetrixStringHelper.valueIsEqual(message, "{\"END_PTA\":null}")) {
							MobileApplication.stopSync(mCurrentActivity);
							MobileApplication.startSync(mCurrentActivity);
							if(!screenShouldRefreshPTA)
								mUIHelper.dismissLoadingDialog();
						}
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void setFilterBar(int rowCount) {
		try
		{
			if (MetrixStringHelper.isNullOrEmpty(mLastFilterName))
				mFilterName.setText(String.format("%1$s (%2$s)", AndroidResourceHelper.getMessage("NoFilter"), String.valueOf(rowCount)));
			else
				mFilterName.setText(String.format("%1$s (%2$s)", mLastFilterName, String.valueOf(rowCount)));

			if (MetrixStringHelper.isNullOrEmpty(mLastSortName))
				mSortName.setText(AndroidResourceHelper.getMessage("NoSort"));
			else
				mSortName.setText(mLastSortName);
		} catch (Exception e) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, e.getMessage());
		}
	}

	@SuppressLint("SimpleDateFormat")
	private boolean populateList() {
		String whereClause = String.format("where (task.person_id = '%1$s' or exists (select task_id from task_resource where task_resource.task_id = task.task_id and task_resource.person_id = '%1$s' and task_resource.assigned_resource = 'Y')) and task.task_status in (select task_status from task_status where status = 'OP' and task_status <> '%2$s')", User.getUser().personId, MobileApplication.getAppParam("REJECTED_TASK_STATUS"));
		String constraintFilter = MetrixDatabaseManager.getAppParam("ADDITIONAL_JOB_LIST_CONSTRAINTS");
		if (!MetrixStringHelper.isNullOrEmpty(constraintFilter)){
			whereClause = whereClause + " and ("+constraintFilter+")";
		}

		if (!MetrixStringHelper.isNullOrEmpty(mLastFilterName)) {
			if (MetrixStringHelper.valueIsEqual(mLastFilterName, AndroidResourceHelper.getMessage("OtherJobs"))) {
				// push receivedFilter onto where clause
				whereClause = whereClause + " and " + receivedFilter;
			} else {
				FilterSortItem filterItem = MetrixFilterSortManager.getFilterSortItemByLabel(mLastFilterName, MetrixFilterSortManager.getFilterItems(mScreenId));
				if (filterItem != null && !MetrixStringHelper.isNullOrEmpty(filterItem.Content)) {
					String actualContent = MetrixFilterSortManager.resolveFilterSortContent(this, filterItem.Content);
					if (filterItem.FullFilter)
						whereClause = "where " + actualContent;
					else
						whereClause = whereClause + " and " + actualContent;
				}
			}
		}

		String searchCriteria = MetrixControlAssistant.getValue(R.id.search_criteria, mLayout);
		StringBuilder query = new StringBuilder();
		query.append(MetrixListScreenManager.generateListQuery(this, "task", whereClause, searchCriteria));

		if (!MetrixStringHelper.isNullOrEmpty(mLastSortName)) {
			FilterSortItem sortItem = MetrixFilterSortManager.getFilterSortItemByLabel(mLastSortName, MetrixFilterSortManager.getSortItems(mScreenId));
			if (sortItem != null && !MetrixStringHelper.isNullOrEmpty(sortItem.Content)) {
				query.append(" order by ");
				query.append(MetrixFilterSortManager.resolveFilterSortContent(this, sortItem.Content));
				query.append(mLastSortOrder);

				if (mIsSortAscending) {
					mSortIcon.setImageDrawable(getDrawable(R.drawable.asc_order));
					mSortIcon.setColorFilter(mFirstGradientTextColor);
				} else {
					mSortIcon.setImageDrawable(getDrawable(R.drawable.desc_order));
					mSortIcon.setColorFilter(mFirstGradientTextColor);
				}
			}
		}

		String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
		if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
			query.append(" limit " + maxRows);
		}

		MetrixCursor cursor = null;
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		int count = 0;

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor != null && cursor.moveToFirst()) {

				while (cursor.isAfterLast() == false) {
					count = count + 1;
					HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);

					String address = row.get("address.address");
					if (MetrixStringHelper.isNullOrEmpty(address)) {
						row.put("custom.full_address", "");
					} else {
						String city = row.get("address.city");
						row.put("custom.full_address", address + ", " + city);
					}

					String plannedStart = row.get("task.plan_start_dttm");
					String plannedEnd = row.get("task.plan_end_dttm");
					String formattedTimeValue = "";

					SimpleDateFormat simpleFormat = new SimpleDateFormat();
					DateFormat dateFormat = MetrixDateTimeHelper.getDateTimeFormat(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS);
					simpleFormat = (SimpleDateFormat) dateFormat;
					String formatString = simpleFormat.toPattern();

					if (formatString.contains(" ")) {
						formatString = formatString.replace(":ss", "").replace(".ss", "");
						simpleFormat.applyLocalizedPattern(formatString);
					}

					String formattedStartDate = "";
					String formattedStartTime = "";
					Date startDate = null;
					if(!MetrixStringHelper.isNullOrEmpty(plannedStart)) {
						startDate = simpleFormat.parse(plannedStart);
						String formattedStartDateTime = simpleFormat.format(startDate);
						String[] formattedStartPieces = formattedStartDateTime.split(" ");

						if (formattedStartPieces != null) {
							if (formattedStartPieces.length > 0) formattedStartDate = formattedStartPieces[0];

							if (formattedStartPieces.length == 3)
								formattedStartTime = formattedStartPieces[1] + formattedStartPieces[2];
							else if (formattedStartPieces.length == 4){
								formattedStartDate = formattedStartPieces[0] + formattedStartPieces[1] + formattedStartPieces[2];
								formattedStartTime = formattedStartPieces[3];
							}
							else if (formattedStartPieces.length > 1)
								formattedStartTime = formattedStartPieces[1];
						}
					}

					String formattedEndTime = "";
					Date endDate = null;
					if(!MetrixStringHelper.isNullOrEmpty(plannedEnd)) {
						endDate = simpleFormat.parse(plannedEnd);
						String formattedEndDateTime = simpleFormat.format(endDate);
						String[] formattedEndPieces = formattedEndDateTime.split(" ");
						if(formattedEndPieces != null) {
							if (formattedEndPieces.length == 3)
								formattedEndTime = formattedEndPieces[1] + formattedEndPieces[2];
							else if (formattedEndPieces.length == 4){
								formattedEndTime = formattedEndPieces[3];
							}
							else if (formattedEndPieces.length > 1)
								formattedEndTime = formattedEndPieces[1];
						}
					}

					formattedTimeValue = formattedStartTime.toLowerCase() + " - " + formattedEndTime.toLowerCase();

					row.put("custom.formatted_time", String.format("%1$s  %2$s", formattedStartDate, formattedTimeValue));

					table.add(row);
					cursor.moveToNext();
				}

				table = MetrixListScreenManager.performScriptListPopulation(this, table);
			} else {
				MetrixUIHelper.showSnackbar(JobList.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("NoDataForSelectedFilter"));
			}
			filteredResults = count;
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		if (count > 0) {
			recyclerView.setVisibility(View.VISIBLE);
		} else {
			recyclerView.setVisibility(View.GONE);
			registerForMetrixActionView(mLayout, getMetrixActionBar().getCustomView());
		}

		if (mAdapter == null) {
			mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
					R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0,
					null, 0, R.id.sliver, null, "task.metrix_row_id", this);
			recyclerView.setAdapter(mAdapter);
		} else {
			mAdapter.updateData(table);
		}

		setFilterBar(count);

		return true;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.filter_bar__filter_name:
				displayFilterOptions();
				break;
			case R.id.filter_bar__sort_name:
				displaySortOptions();
				break;
			default:
				super.onClick(v);
		}
	}

	private void displayFilterOptions() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (MetrixStringHelper.isNullOrEmpty(mLastFilterName))
			builder.setTitle(AndroidResourceHelper.getMessage("FilterTitle"));
		else
			builder.setTitle(AndroidResourceHelper.getMessage("FilterTitle1Arg", mLastFilterName));

		ArrayList<FilterSortItem> filterList = MetrixFilterSortManager.getFilterItems(mScreenId);
		if (filterList != null && !filterList.isEmpty()) {
			mFilterOptions = new ArrayList<String>();
			for (FilterSortItem fsi : filterList) {
				mFilterOptions.add(fsi.Label);
			}
			mFilterOptions.remove(mLastFilterName);

			CharSequence[] items = mFilterOptions.toArray(new CharSequence[mFilterOptions.size()]);
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int pos) {
					MobileGlobal.jobListFilterEngaged = true;
					mLastFilterName = mFilterOptions.get(pos);
					populateList();
					SettingsHelper.saveStringSetting(JobList.this, MetrixFilterSortManager.getSelectedFilterItemSettingName("JobList"), mLastFilterName, true);
				}
			});

			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	private void displaySortOptions() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (MetrixStringHelper.isNullOrEmpty(mLastSortName))
			builder.setTitle(AndroidResourceHelper.getMessage("SortTitle"));
		else
			builder.setTitle(AndroidResourceHelper.getMessage("SortTitle1Arg", mLastSortName));

		ArrayList<FilterSortItem> sortList = MetrixFilterSortManager.getSortItems(mScreenId);
		if (sortList != null && !sortList.isEmpty()) {
			mSortOptions = new ArrayList<String>();
			for (FilterSortItem fsi : sortList) {
				mSortOptions.add(fsi.Label);
			}

			CharSequence[] items = mSortOptions.toArray(new CharSequence[mSortOptions.size()]);
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int pos) {
					String newSortName = mSortOptions.get(pos);
					if (MetrixStringHelper.valueIsEqual(mLastSortName, newSortName))
						mIsSortAscending = !mIsSortAscending;	// if we choose same sort again, invert ASC/DESC
					else
						mIsSortAscending = true;	// when we choose a different sort, always start with ASC

					mLastSortName = newSortName;
					mLastSortOrder = mIsSortAscending ? " asc" : " desc";
					populateList();
					SettingsHelper.saveStringSetting(JobList.this, MetrixFilterSortManager.getSelectedSortItemSettingName("JobList"), mLastSortName, true);
					SettingsHelper.saveStringSetting(JobList.this, MetrixFilterSortManager.getSortOrderSettingName("JobList"), mLastSortOrder, true);
				}
			});

			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	private boolean isTaskMapObjectExist() {
		boolean objectExist = false;
		if(filteredResults >0)
		{
			objectExist =true;
		}
		return objectExist;
	}

	@Override
	public boolean onMetrixActionViewItemClick(MenuItem menuItem) {
		String open_message_id = AndroidResourceHelper.getMessageFromScript("Open","CODE");
		switch (menuItem.getItemId())
		{
			case JobListMetrixActionView.SCHEDULEJOBMENUITEM:
				MetrixWorkflowManager.advanceWorkflow(MetrixWorkflowManager.SCHEDULE_WORKFLOW, JobList.this);
				break;
			case JobListMetrixActionView.SHOWMAPJOBMENUITEM:
				if (isTaskMapObjectExist()) {
					Intent intent = new Intent(this, TaskMap.class);
					intent.putExtra("Filter", mLastFilterName);
					if (MetrixStringHelper.valueIsEqual(mLastFilterName, AndroidResourceHelper.getMessage("OtherJobs")))
						intent.putExtra("LiteralFilter", receivedFilter);
					MetrixActivityHelper.startNewActivity(this, intent);
				}
				break;
			case JobListMetrixActionView.SHOWTEAMMAPJOBMENUITEM:
				if (isTaskMapObjectExist()) {
					if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
							ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
						TeamTaskSearchEntry searchTeamTask = new TeamTaskSearchEntry(this, mLastFilterName);
						searchTeamTask.initDialog();
					} else {
						ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
								TASK_SEARCH_LOCATION_PERMISSION_REQUEST);
					}
				}
				break;
			case JobListMetrixActionView.JOBACCEPTMENUITEM:
				HashMap<String, String> selectedItem = mAdapter.getListData().get(listViewPosition);

				String taskId = selectedItem.get("task.task_id");
				String taskStatus = selectedItem.get("task.task_status");

				if (taskStatus.compareToIgnoreCase(open_message_id) != 0) {
					MetrixUIHelper.showErrorDialogOnGuiThread(mCurrentActivity, AndroidResourceHelper.getMessage("YouCanOnlyAcceptJobInOpenStatus"));
					return true;
				}

				String currTaskPersonId = MetrixDatabaseManager.getFieldStringValue("task", "person_id", String.format("task_id = '%s'", taskId));
				if (MetrixStringHelper.isNullOrEmpty(currTaskPersonId))
					isPooledTask = true;

				if (isPooledTask) {
					String acceptedStatus = MetrixDatabaseManager.getAppParam("ACCEPTED_TASK_STATUS");
					if (MetrixStringHelper.isNullOrEmpty(acceptedStatus))
						acceptedStatus = "ACCEPTED";
					MetrixPooledTaskAssignmentManager.instance().doTeamTaskAccept(this, taskId, acceptedStatus, null, false);
					return true;
				}

				MetrixSqlData contactData = new MetrixSqlData("task", MetrixTransactionTypes.UPDATE, "task_id=" + taskId);
			{
				String rowId = MetrixDatabaseManager.getFieldStringValue("task", "metrix_row_id", "task_id=" + taskId);
				String acceptedStatus = MetrixDatabaseManager.getAppParam("ACCEPTED_TASK_STATUS");
				if (MetrixStringHelper.isNullOrEmpty(acceptedStatus))
					acceptedStatus = "ACCEPTED";

				DataField tId = new DataField("task_id", taskId);
				DataField rId = new DataField("metrix_row_id", rowId);
				DataField tStatus = new DataField("task_status", acceptedStatus);
				DataField tStatusAsOf = new DataField("status_as_of", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true));

				contactData.dataFields.add(tId);
				contactData.dataFields.add(rId);
				contactData.dataFields.add(tStatus);
				contactData.dataFields.add(tStatusAsOf);

				String value = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='GPS_LOCATION_TASK_STATUS_UPDATE'");
				boolean updateTask = MetrixRoleHelper.isGPSFunctionEnabled("GPS_TASK");

				if (!MetrixStringHelper.isNullOrEmpty(value) && value.compareToIgnoreCase("Y") == 0) {
					if (updateTask) {
						Location currentLocation = MetrixLocationAssistant.getCurrentLocation(this);

						if (currentLocation != null) {
							try {
								DataField gLat = new DataField("geocode_lat", MetrixFloatHelper.convertNumericFromUIToDB(Double.toString(currentLocation.getLatitude())));
								DataField gLong = new DataField("geocode_long", MetrixFloatHelper.convertNumericFromUIToDB(Double.toString(currentLocation.getLongitude())));

								contactData.dataFields.add(gLat);
								contactData.dataFields.add(gLong);
							} catch (Exception e) {
								LogManager.getInstance(this).error(e);
							}
						}
					}
				}
			}

			ArrayList<MetrixSqlData> contactTran = new ArrayList<MetrixSqlData>();
			contactTran.add(contactData);

			MetrixTransaction transactionInfo = new MetrixTransaction();
			MetrixUpdateManager.update(contactTran, true, transactionInfo, AndroidResourceHelper.getMessage("TaskLCase"), mCurrentActivity);
			populateList();
			break;
			case JobListMetrixActionView.JOBREJECTMENUITEM:
				HashMap<String, String> theItem = mAdapter.getListData().get(listViewPosition);

				taskId = theItem.get("task.task_id");
				taskStatus = theItem.get("task.task_status");

				if (taskStatus.compareToIgnoreCase(open_message_id) != 0) {
					MetrixUIHelper.showErrorDialogOnGuiThread(mCurrentActivity, AndroidResourceHelper.getMessage("YouCanOnlyRejectJobInOpenStatus"));
					return true;
				}

				MetrixSqlData taskData = new MetrixSqlData("task", MetrixTransactionTypes.UPDATE, "task_id=" + taskId);
			{
				String rowId = MetrixDatabaseManager.getFieldStringValue("task", "metrix_row_id", "task_id="+taskId);
				String rejectedStatus = MetrixDatabaseManager.getAppParam("REJECTED_TASK_STATUS");

				if(MetrixStringHelper.isNullOrEmpty(rejectedStatus))
					rejectedStatus = "REJECTED";

				DataField tId = new DataField("task_id", taskId);
				DataField rId = new DataField("metrix_row_id", rowId);
				DataField tStatus = new DataField("task_status", rejectedStatus);
				DataField tStatusAsOf = new DataField("status_as_of", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true));

				taskData.dataFields.add(tId);
				taskData.dataFields.add(rId);
				taskData.dataFields.add(tStatus);
				taskData.dataFields.add(tStatusAsOf);

				String value = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='GPS_LOCATION_TASK_STATUS_UPDATE'");
				boolean updateTask = MetrixRoleHelper.isGPSFunctionEnabled("GPS_TASK");

				if (!MetrixStringHelper.isNullOrEmpty(value) && value.compareToIgnoreCase("Y") == 0) {
					if(updateTask) {
						Location currentLocation = MetrixLocationAssistant.getCurrentLocation(this);

						if (currentLocation != null) {
							try {
								DataField gLat = new DataField("geocode_lat", Double.toString(currentLocation.getLatitude()));
								DataField gLong = new DataField("geocode_long", Double.toString(currentLocation.getLongitude()));

								taskData.dataFields.add(gLat);
								taskData.dataFields.add(gLong);
							} catch (Exception e) {
								LogManager.getInstance(this).error(e);
							}
						}
					}
				}
			}

			contactTran = new ArrayList<MetrixSqlData>();
			contactTran.add(taskData);

			transactionInfo = new MetrixTransaction();
			MetrixUpdateManager.update(contactTran, true, transactionInfo, AndroidResourceHelper.getMessage("TaskLCase"), mCurrentActivity);
			populateList();
			break;
		}
		return super.onMetrixActionViewItemClick(menuItem);
	}

	@Override
	public boolean OnCreateMetrixActionView(View view, Integer... position) {

		MetrixActionView metrixActionView = getMetrixActionView();
		Menu menu = metrixActionView.getMenu();

		//we set the rest of the options only if it's not the search field
		if (!(view instanceof EditText)) {
			menu.removeGroup(0);

			if (menu.findItem(JobListMetrixActionView.SCHEDULEJOBMENUITEM) == null)
				menu.add(5, JobListMetrixActionView.SCHEDULEJOBMENUITEM, 0,
						JobListMetrixActionView.SCHEDULEJOB);

			if (mAdapter.getListData() != null && mAdapter.getListData().size() > 0) {
				if (MetrixLibraryHelper.googleMapsIsInstalled(view.getContext())) {
					if (mLastFilterName.compareToIgnoreCase(MetrixFilterSortManager.getFilterLabelByItemName("Team Tasks", mScreenId)) == 0) {
						if (menu.findItem(JobListMetrixActionView.SHOWTEAMMAPJOBMENUITEM) == null)
							menu.add(5, JobListMetrixActionView.SHOWTEAMMAPJOBMENUITEM, 0, JobListMetrixActionView.SHOWTEAMMAP);
						menu.removeGroup(2);
					} else if (menu.findItem(JobListMetrixActionView.SHOWMAPJOBMENUITEM) == null)
						menu.add(5, JobListMetrixActionView.SHOWMAPJOBMENUITEM, 0, JobListMetrixActionView.SHOWMAP);
				}
			}

			if (mAdapter.getListData() != null && mAdapter.getListData().size() > 0) {
				if (position.length > 0)
					listViewPosition = position[0];
				else
					listViewPosition = 0;
				HashMap<String, String> selectedItem = mAdapter.getListData().get(listViewPosition);

				String taskId = MetrixStringHelper.getString(selectedItem.get("task.task_id"));
				String placeName = MetrixStringHelper.getString(selectedItem.get("place.name"));
				String taskStatus = MetrixStringHelper.getString(selectedItem.get("task.task_status"));

				//LCS #134463
				if (!MetrixStringHelper.isNullOrEmpty(placeName)) {
					if (placeName.length() > 14) {
						placeName = placeName.substring(0, 14);
					}
				} else
					placeName = "";

				String allowAcceptJob = MetrixDatabaseManager.getAppParam("ALLOW_ACCEPT_OR_REJECT_TASKS");

				if (menu.findItem(JobListMetrixActionView.JOBACCEPTMENUITEM) != null)
					menu.removeItem(JobListMetrixActionView.JOBACCEPTMENUITEM);

				if (menu.findItem(JobListMetrixActionView.JOBREJECTMENUITEM) != null)
					menu.removeItem(JobListMetrixActionView.JOBREJECTMENUITEM);

				String open_message_id = AndroidResourceHelper.getMessageFromScript("Open","CODE");
				if (!MetrixStringHelper.isNullOrEmpty(allowAcceptJob) && allowAcceptJob.compareToIgnoreCase("Y") == 0 && taskStatus.compareToIgnoreCase(open_message_id) == 0) {
					if(!MetrixStringHelper.isNullOrEmpty(placeName)){
						if (!MetrixStringHelper.isNullOrEmpty(placeName)) {
							menu.add(1, JobListMetrixActionView.JOBACCEPTMENUITEM, 0, JobListMetrixActionView.JOBACCEPT + " (" + taskId + " - " + placeName + ")");
							menu.add(2, JobListMetrixActionView.JOBREJECTMENUITEM, 0, JobListMetrixActionView.JOBREJECT + " (" + taskId + " - " + placeName + ")");
						}
					} else {
						menu.add(1,	JobListMetrixActionView.JOBACCEPTMENUITEM, 0, JobListMetrixActionView.JOBACCEPT+" ("+taskId+")");
						menu.add(2,	JobListMetrixActionView.JOBREJECTMENUITEM, 0, JobListMetrixActionView.JOBREJECT+" ("+taskId+")");
					}
				}
			}
		}
		return super.OnCreateMetrixActionView(view);
	}

	@Override
	public void afterTextChanged(Editable s) {
		mHandler.removeCallbacksAndMessages(null);
		mHandler.postDelayed(() -> {
			if (!JobList.this.isDestroyed())
				this.populateList();
		}, 500);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	boolean showJobSearchPanel()
	{
		boolean status = false;
		String showJobSearchPanelParam = MetrixDatabaseManager.getAppParam("DISPLAY_JOBLIST_SEARCH");
		if (!MetrixStringHelper.isNullOrEmpty(showJobSearchPanelParam) && MetrixStringHelper.valueIsEqual(showJobSearchPanelParam.toLowerCase(), "y"))
			status = true;
		return status;
	}

	private void setJobSearchPanel() {
		if(showJobSearchPanel()) {
			TableLayout jobSearchPanel = (TableLayout) findViewById(R.id.job_search_panel);
			if(jobSearchPanel != null)
				jobSearchPanel.setVisibility(View.VISIBLE);

			mSearchCriteria = (EditText) findViewById(R.id.search_criteria);
			mSearchCriteria.addTextChangedListener(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (isGoneToSettingsForInitialLocation) {
			isGoneToSettingsForInitialLocation = false;
			MetrixLocationAssistant.startLocationManager(this);
		}

		if (shouldShowTeamTaskSearch) {
			shouldShowTeamTaskSearch = false;
			TeamTaskSearchEntry searchTeamTask = new TeamTaskSearchEntry(this, mLastFilterName);
			searchTeamTask.initDialog();
		}

		if (incompletePermissionRequest == INITIAL_LOCATION_PERMISSION_REQUEST || incompletePermissionRequest == TASK_SEARCH_LOCATION_PERMISSION_REQUEST) {
			final int requestCode = incompletePermissionRequest;
			incompletePermissionRequest = 0;
			DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (ActivityCompat.shouldShowRequestPermissionRationale(JobList.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
						// can request permission again
						ActivityCompat.requestPermissions(JobList.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
					} else {
						// user need to go to app settings to enable it
						try {
							if (requestCode == INITIAL_LOCATION_PERMISSION_REQUEST)
								isGoneToSettingsForInitialLocation = true;
							Intent intent = new Intent();
							intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
							Uri uri = Uri.fromParts("package", getPackageName(), null);
							intent.setData(uri);
							startActivity(intent);
						} catch (ActivityNotFoundException ex) {
							// This is extremely rare
							if (requestCode == INITIAL_LOCATION_PERMISSION_REQUEST)
								isGoneToSettingsForInitialLocation = false;
							MetrixUIHelper.showSnackbar(JobList.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("EnablePermManuallyAndRetry"));
						}
					}
				}
			};

			final String message = (requestCode == INITIAL_LOCATION_PERMISSION_REQUEST) ?
					AndroidResourceHelper.getMessage("LocationPermFirstLaunchExpl") :
					AndroidResourceHelper.getMessage("LocationPermGenericExpl");

			MetrixDialogAssistant.showAlertDialog(
					AndroidResourceHelper.getMessage("PermissionRequired"),
					message,
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
		if (requestCode == INITIAL_LOCATION_PERMISSION_REQUEST || requestCode == TASK_SEARCH_LOCATION_PERMISSION_REQUEST) {
			boolean locationPermissionGranted = false;
			if (requestCode == INITIAL_LOCATION_PERMISSION_REQUEST) {
				// If this is the initial request, specifically check for fine location so we can nag the user about precise location at startup
				for (int i = 0; i < permissions.length; i++) {
					if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
						locationPermissionGranted = true;
						break;
					}
				}
			} else {
				// If this is is for task mapping then any type of location is fine
				for (int grantResult : grantResults) {
					if (grantResult == PackageManager.PERMISSION_GRANTED) {
						locationPermissionGranted = true;
						break;
					}
				}
			}

			if (locationPermissionGranted) {
				if (requestCode == INITIAL_LOCATION_PERMISSION_REQUEST)
					MetrixLocationAssistant.startLocationManager(this);
				else
					shouldShowTeamTaskSearch = true;
			} else {
				// Showing dialog at this Activity lifecycle can lead to app crash as the view is not guaranteed to
				// be visible to the user. So we set the incompletePermissionRequest value and handle it inside
				// onResume activity life cycle
				incompletePermissionRequest = requestCode;
			}
		}
	}

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		if(scriptEventConsumesListTap(JobList.this, view, MetrixScreenManager.getScreenId(JobList.this))) return;
		listViewPosition = position;

		String taskId = MetrixStringHelper.getString(listItemData.get("task.task_id")).replace(AndroidResourceHelper.getMessage("Job"), "");
		MetrixCurrentKeysHelper.setKeyValue("task", "task_id", taskId);

		String placeIdCust = MetrixStringHelper.getString(listItemData.get("task.place_id_cust"));
		String customerName = MetrixStringHelper.getString(listItemData.get("place.name"));

		MetrixPublicCache.instance.addItem("CurrentTaskId", taskId);
		MetrixPublicCache.instance.addItem("CurrentCustomerName", customerName);
		MetrixPublicCache.instance.addItem("CurrentTaskPlaceIdCust", placeIdCust);

		String taskType = MetrixDatabaseManager.getFieldStringValue("task", "task_type", String.format("task_id = %s", taskId));
		String workflowName = MetrixWorkflowManager.getDebriefWorkflowNameForTaskType(taskType);
		MetrixWorkflowManager.advanceWorkflow(workflowName, JobList.this);
	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		listViewPosition = position;
		if(onCreateMetrixActionViewListner != null)
			onCreateMetrixActionViewListner.OnCreateMetrixActionView(view, position);
	}
}

