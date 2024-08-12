package com.metrix.metrixmobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixLookupFilterDef;
import com.metrix.architecture.metadata.MetrixLookupTableDef;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.GenericParcelable;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper.ISO8601;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.global.MobileGlobal;
import com.metrix.metrixmobile.system.BottomOffsetDecoration;
import com.metrix.metrixmobile.system.DebriefActivity;
import com.metrix.metrixmobile.system.Lookup;
import com.metrix.metrixmobile.system.MetadataDebriefActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DebriefPartUsageType extends DebriefActivity implements View.OnClickListener, MetrixRecyclerViewListener {
	private static final int STOCK_SEARCH_LOCATION_PERMISSION_REQUEST = 1223;
	private FloatingActionButton mNextButton;
	private Button mViewPreviousEntriesButton;
	private EditText mTruckPartSelected, mTruckPartSelectedSerial, mTruckPartSelectedPlace, mTruckPartSelectedLocation, mPartNeedSequence, mLotId;
	private RecyclerView recyclerView, recyclerViewDebriefPartUsageTypes;
	private MetadataRecyclerViewAdapter mAdapter;
	private int mSelectedPosition;
	private int incompletePermissionRequest = 0;
	private boolean shouldShowStockSearch = false;
	private boolean mClickedPartFromStock;
	private BottomOffsetDecoration mBottomOffset;
	private List<FloatingActionButton> mFABList;
	private List<FloatingActionButton> mFABsToShow;
	private int mOffset = 0;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(shouldRunTabletSpecificUIMode)
			setContentView(R.layout.tb_land_debrief_part_usage_type);
		else
			setContentView(R.layout.debrief_part_usage_type);

		recyclerViewDebriefPartUsageTypes = findViewById(R.id.recyclerview_debrief_parts_usage_types);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerViewDebriefPartUsageTypes, 0);
		recyclerViewDebriefPartUsageTypes.setNestedScrollingEnabled(false);

		recyclerView = findViewById(R.id.recyclerView);
		if (recyclerView != null)
			MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
	}

	public void onStart() {
		super.onStart();

		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		getCounts();
		populateList();
	}

	private void getCounts() {
		int count = MetrixDatabaseManager.getCount("part_usage", "task_id=" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));
		try {
			MetrixControlAssistant.setValue(R.id.view_previous_entries, (ViewGroup) findViewById(R.id.view_previous_entries_bar), AndroidResourceHelper.getMessage("List1Arg", String.valueOf(count)));
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private int getCountOfPartNeeds() {
		String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");

		int partNeedsCount = MetrixDatabaseManager.getCount("part_need", "task_id = " + taskId + " and parts_used != 'Y' and line_code_type = 'PT'");

		return partNeedsCount;
	}

	private void recordPartNeedsBeingUsed() {
		String partNeedSequence = "";
		try {
			partNeedSequence = MetrixControlAssistant.getValue(mPartNeedSequence);
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}

		if (MetrixStringHelper.isNullOrEmpty(partNeedSequence)) {
			return;
		}

		String requestId = MetrixDatabaseManager.getFieldStringValue("task", "request_id", "task_id=" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));

		ArrayList<MetrixSqlData> partNeedsToUpdate = new ArrayList<MetrixSqlData>();
		String rowId = MetrixDatabaseManager.getFieldStringValue("part_need", "metrix_row_id", "request_id='" + requestId + "' and sequence=" + partNeedSequence);

		String partNeedStatus = MetrixDatabaseManager.getFieldStringValue("part_need", "status", "metrix_row_id = " + rowId);
		if (partNeedStatus.compareToIgnoreCase("S") != 0) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("PartNotShippedForNeed"));
			return;
		} else if (partNeedStatus.compareToIgnoreCase("S") == 0) {
			String shipmentId = MetrixDatabaseManager.getFieldStringValue("part_need", "shipment_id", "metrix_row_id = " + rowId);
			String inTransit = MetrixDatabaseManager.getFieldStringValue("shipment", "in_transit", "shipment_id = " + shipmentId);

			if (!MetrixStringHelper.isNullOrEmpty(inTransit) && inTransit.compareToIgnoreCase("Y") == 0) {
				String receivingStatus = MetrixDatabaseManager.getFieldStringValue("receiving", "inventory_adjusted", "shipment_id = " + shipmentId);

				if (!MetrixStringHelper.isNullOrEmpty(receivingStatus) && receivingStatus.compareToIgnoreCase("N") == 0) {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("PartNotShippedForNeed"));
					return;
				}
			}
		}

		MetrixSqlData data = new MetrixSqlData("part_need", MetrixTransactionTypes.UPDATE, "metrix_row_id=" + rowId);
		data.dataFields.add(new DataField("metrix_row_id", rowId));
		data.dataFields.add(new DataField("request_id", requestId));
		data.dataFields.add(new DataField("sequence", partNeedSequence));
		data.dataFields.add(new DataField("parts_used", "Y"));
		partNeedsToUpdate.add(data);

		MetrixTransaction transactionInfo = new MetrixTransaction();
		MetrixUpdateManager.update(partNeedsToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("PartNeeds"), this);
	}

	protected void setListeners() {
		if (mFABList == null)
			mFABList = new ArrayList<FloatingActionButton>();
		else
			mFABList.clear();

		mNextButton = (FloatingActionButton) findViewById(R.id.next);
		mViewPreviousEntriesButton = (Button) findViewById(R.id.view_previous_entries);
		mTruckPartSelected = (EditText) findViewById(R.id.truck_part_selected);
		mLotId = (EditText) findViewById(R.id.lot_id);
		mTruckPartSelectedSerial = (EditText) findViewById(R.id.truck_part_selected_serial);
		mTruckPartSelectedPlace = (EditText) findViewById(R.id.truck_part_selected_place);
		mTruckPartSelectedLocation = (EditText) findViewById(R.id.truck_part_selected_location);
		mPartNeedSequence = (EditText) findViewById(R.id.part_need__sequence);

		mViewPreviousEntriesButton.setOnClickListener(this);
		mNextButton.setOnClickListener(this);

		mFABList.add(mNextButton);

//		mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));

		mOffset = generateOffsetForFABs(mFABList);
		// The amount subtracted it equal to the button height plus the top and bottom margins of the button
		mOffset -= getResources().getDimension((R.dimen.button_height)) + (2*getResources().getDimension((R.dimen.md_margin)));

		fabRunnable = this::showFABs;

		NestedScrollView scrollView = findViewById(R.id.scroll_view);
		mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), mOffset);
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
	}

	@SuppressLint("DefaultLocale")
	private void recordTruckStockBeingUsed() {
		String partId = "";
		String placeId = "";
		String location = "";
		String lotId = "";
		boolean isControlledPart = false;

		try {
			partId = MetrixControlAssistant.getValue(mTruckPartSelected);
			placeId = MetrixControlAssistant.getValue(mTruckPartSelectedPlace);
			location = MetrixControlAssistant.getValue(mTruckPartSelectedLocation);
			lotId = MetrixControlAssistant.getValue(mLotId);
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}

		if (MetrixStringHelper.isNullOrEmpty(partId)) {
			return;
		}

		String serializedPart = MetrixDatabaseManager.getFieldStringValue("part", "serialed", "part_id='" + partId + "'");
		String lotIdentified = MetrixDatabaseManager.getFieldStringValue("part", "lot_identified", "part_id='" + partId + "'");
		String controlledPart = MetrixDatabaseManager.getFieldStringValue("part", "controlled_part", "part_id='" + partId + "'");
		if (controlledPart.toLowerCase().contains("y")) isControlledPart = true;

		if (serializedPart.compareToIgnoreCase("Y") == 0) {
			MetrixLookupDef lookupDef = new MetrixLookupDef("stock_serial_id", true);
			lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_serial_id.serial_id", R.id.truck_part_selected_serial));
			lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_serial_id.part_id"));
			lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_serial_id.lot_id", R.id.lot_id));
			lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA", "SerialId");
			lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.part_id", "=", partId));
			lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.usable", "=", "Y"));
			lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.in_transit", "!=", "Y"));
			lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.place_id", "=", placeId));
			lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.location", "=", location));

			Class<?> correctActivity = Lookup.class;
			if (mClickedPartFromStock && !isControlledPart)
				correctActivity = DebriefPartUsageLookup.class;

			Intent intent = MetrixActivityHelper.createActivityIntent(this, correctActivity);
			MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
			MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);

			if(mClickedPartFromStock && !isControlledPart) {
				if (serializedPart.compareToIgnoreCase("Y") == 0 && lotIdentified.compareToIgnoreCase("Y") == 0)
					intent.putExtra("displayLotSerials", true);
				else
					intent.putExtra("displaySerials", true);
			}
			startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);
		} else if (lotIdentified.compareToIgnoreCase("Y") == 0 && MetrixStringHelper.isNullOrEmpty(lotId)) {
			// only look up lots that match the selected part, place, and location in stock_lot_table
			MetrixLookupDef lookupDef = new MetrixLookupDef("stock_lot_table", true);
			lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_lot_table.part_id"));
			lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_lot_table.qty_on_hand"));
			lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_lot_table.lot_id", R.id.lot_id));
			lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA", "LotId");
			lookupDef.filters.add(new MetrixLookupFilterDef("stock_lot_table.part_id", "=", partId));
			lookupDef.filters.add(new MetrixLookupFilterDef("stock_lot_table.place_id", "=", placeId));
			lookupDef.filters.add(new MetrixLookupFilterDef("stock_lot_table.location", "=", location));

			Class<?> correctActivity = Lookup.class;
			if(mClickedPartFromStock && !isControlledPart)
				correctActivity = DebriefPartUsageLookup.class;

			Intent intent = MetrixActivityHelper.createActivityIntent(this, correctActivity);
			MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
			MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);

			if(mClickedPartFromStock && !isControlledPart)
				intent.putExtra("displayLots", true);
			startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);
		} else {
			savePartOnlyUsage(partId, placeId, location);
		}
	}

	private void savePartUsageLotIfNeeded(String puId) {
		try {
			String lotId = MetrixControlAssistant.getValue(mLotId);

			if (!MetrixStringHelper.isNullOrEmpty(lotId)) {
				ArrayList<MetrixSqlData> lotsToInsert = new ArrayList<MetrixSqlData>();
				MetrixSqlData pulot = new MetrixSqlData("part_usage_lot", MetrixTransactionTypes.INSERT, "");
				pulot.dataFields.add(new DataField("pu_id", puId));
				pulot.dataFields.add(new DataField("lot_id", lotId));
				pulot.dataFields.add(new DataField("quantity", "1"));
				lotsToInsert.add(pulot);
				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(lotsToInsert, true, transactionInfo, AndroidResourceHelper.getMessage("PartUsageLot"), this);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	@SuppressLint("DefaultLocale")
	private void recordTruckStockSerialBeingUsed() {
		String partId = "";
		String serialId = "";
		String placeId = "";
		String location = "";
		String lotId = "";

		try {
			partId = MetrixControlAssistant.getValue(mTruckPartSelected);
			serialId = MetrixControlAssistant.getValue(mTruckPartSelectedSerial);
			placeId = MetrixControlAssistant.getValue(mTruckPartSelectedPlace);
			location = MetrixControlAssistant.getValue(mTruckPartSelectedLocation);
			lotId = MetrixControlAssistant.getValue(mLotId);
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}

		if (MetrixStringHelper.isNullOrEmpty(partId) || MetrixStringHelper.isNullOrEmpty(serialId)) {
			return;
		}

		ArrayList<MetrixSqlData> partUsagesToInsert = new ArrayList<MetrixSqlData>();

		int puId = MetrixDatabaseManager.generatePrimaryKey("part_usage");

		MetrixSqlData data = new MetrixSqlData("part_usage", MetrixTransactionTypes.INSERT, "");
		data.dataFields.add(new DataField("pu_id", puId));
		data.dataFields.add(new DataField("task_id", MetrixCurrentKeysHelper.getKeyValue("task", "task_id")));
		data.dataFields.add(new DataField("part_id", partId));
		data.dataFields.add(new DataField("quantity", "1"));
		data.dataFields.add(new DataField("work_dt", MetrixDateTimeHelper.getCurrentDate(User.getUser().serverDateFormat)));
		data.dataFields.add(new DataField("part_line_code", "PB"));
		data.dataFields.add(new DataField("place_id_from", placeId));
		data.dataFields.add(new DataField("location", location));

		simplePriceIfEnabled(partId, data);
		partUsagesToInsert.add(data);

		MetrixSqlData serial = new MetrixSqlData("part_usage_serial", MetrixTransactionTypes.INSERT, "");
		serial.dataFields.add(new DataField("pu_id", puId));
		serial.dataFields.add(new DataField("serial_id", serialId));

		partUsagesToInsert.add(serial);

		try {
			MetrixUpdateManager.pauseSync();
			MetrixTransaction transactionInfo = new MetrixTransaction();
			boolean successful = MetrixUpdateManager.update(partUsagesToInsert, true, transactionInfo, AndroidResourceHelper.getMessage("PartsUsed"), this);

			if (successful) {
				savePartUsageLotIfNeeded(Integer.toString(puId));

				String controlledPart = MetrixDatabaseManager.getFieldStringValue("part", "controlled_part", "part_id='" + partId + "'");

				if (controlledPart.toLowerCase().contains("y")) {
					MetrixCurrentKeysHelper.setKeyValue("part_usage", "pu_id", Integer.toString(puId));

					Intent intent = MetrixActivityHelper.createActivityIntent(this, MetadataDebriefActivity.class);
					intent.putExtra("ScreenID", MetrixScreenManager.getScreenId("DebriefPartDisposition"));
					intent.putExtra("NavigatedFromLinkedScreen", true);
					MetrixActivityHelper.startNewActivity(this, intent);
				} else {
					Intent intent = MetrixActivityHelper.createActivityIntent(this, DebriefPartUsageType.class);
					MetrixActivityHelper.startNewActivityAndFinish(this, intent);
				}
			}

			MetrixControlAssistant.setValue(mTruckPartSelected, "");
			MetrixControlAssistant.setValue(mTruckPartSelectedSerial, "");
			MetrixControlAssistant.setValue(mTruckPartSelectedPlace, "");
			MetrixControlAssistant.setValue(mTruckPartSelectedLocation, "");
			MetrixControlAssistant.setValue(mLotId, "");
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		} finally {
			MetrixUpdateManager.resumeSync();
		}
	}

	private void simplePriceIfEnabled(String partId, MetrixSqlData data) {
		String simplePricingEnabled = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_MOBILE_SIMPLE_PART_PRICING'");
		if (simplePricingEnabled.compareToIgnoreCase("y") == 0) {
			StringBuilder query = new StringBuilder();
			query.append("column_1 = '");
			query.append(partId);
			query.append("' and effective_dt < '");
			query.append(MetrixDateTimeHelper.getRelativeDate(MetrixDateTimeHelper.DATE_FORMAT, 0, true, ISO8601.Yes));
			query.append("' and currency = '");
			query.append(User.getUser().getCurrencyToUse());
			query.append("' order by effective_dt DESC limit 1");

			String listPrice = MetrixDatabaseManager.getFieldStringValue("std_part_price_view", "list_price", query.toString());

			try {
				data.dataFields.add(new DataField("unadj_list_price", listPrice));
				data.dataFields.add(new DataField("bill_price", listPrice));
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	@Override
	public void onClick(View v) {
		if (scriptEventConsumesClick(this, v))
			return;

		switch (v.getId()) {
			case R.id.next:
				MetrixWorkflowManager.advanceWorkflow(this);
				break;
			case R.id.view_previous_entries:
				Intent intent = MetrixActivityHelper.createActivityIntent(this, DebriefPartUsageList.class);
				MetrixActivityHelper.startNewActivity(this, intent);
				break;
			default:
				super.onClick(v);
		}
	}

	private void populateList() {
		List<HashMap<String, Object>> table = new ArrayList<HashMap<String, Object>>();

		// Parts from my stock
		int stockCount = MetrixDatabaseManager.getCount("stock_bin", "qty_on_hand > 0 and usable='Y'");
		HashMap<String, Object> usageTypePartsFromMyStock = new HashMap<String, Object>();
		usageTypePartsFromMyStock.put("icon", R.drawable.debrief_warehouse_alt);
		usageTypePartsFromMyStock.put("description", "DUTPartsFromMyStock");
		usageTypePartsFromMyStock.put("count", stockCount);
		//disable/enable button depending on taskIsComplete (enable if false)
		usageTypePartsFromMyStock.put("enable", !taskIsComplete());
		table.add(usageTypePartsFromMyStock);

		// Parts ordered
		int partNeedsCount = getCountOfPartNeeds();
		//set visibility depending on the number of parts need(partNeedsCount > 0)
		if(partNeedsCount > 0){
			HashMap<String, Object> usageTypePartsOrdered = new HashMap<String, Object>();
			usageTypePartsOrdered.put("icon", R.drawable.debrief_warehouse_alt);
			usageTypePartsOrdered.put("description", "DUTPartsOrdered");
			usageTypePartsOrdered.put("count", partNeedsCount);
			//disable/enable button depending on taskIsComplete (enable if false)
			usageTypePartsOrdered.put("enable", !taskIsComplete());
			table.add(usageTypePartsOrdered);
		}

		// Miscellaneous parts
		HashMap<String, Object> usageTypePartsMisc = new HashMap<String, Object>();
		usageTypePartsMisc.put("icon", R.drawable.debrief_parts);
		usageTypePartsMisc.put("description", "DUTPartsMisc");
		usageTypePartsMisc.put("count", null);
		//disable/enable button depending on taskIsComplete (enable if false)
		usageTypePartsMisc.put("enable", !taskIsComplete());
		table.add(usageTypePartsMisc);

		// Find parts
		HashMap<String, Object> usageTypePartsFind = new HashMap<String, Object>();
		usageTypePartsFind.put("icon", R.drawable.debrief_parts_search);
		usageTypePartsFind.put("description", "DUTPartsFind");
		usageTypePartsFind.put("count", null);
		//not disable it for any condition
		usageTypePartsFind.put("enable", true);
		table.add(usageTypePartsFind);

		final PartUsageTypeAdapter partUsageTypeAdapter = new PartUsageTypeAdapter(this, table, getPUTClickListener());
		recyclerViewDebriefPartUsageTypes.setAdapter(partUsageTypeAdapter);
	}

	private PartUsageTypeAdapter.PUTClickListener getPUTClickListener() {
		return (position, selectedItem, view) -> {
			try {
				// Clear out invisible tracking fields, as we are attempting a new part transaction using the DPUT options
				MetrixControlAssistant.setValue(mTruckPartSelected, "");
				MetrixControlAssistant.setValue(mTruckPartSelectedSerial, "");
				MetrixControlAssistant.setValue(mTruckPartSelectedPlace, "");
				MetrixControlAssistant.setValue(mTruckPartSelectedLocation, "");
				MetrixControlAssistant.setValue(mLotId, "");
				MetrixControlAssistant.setValue(mPartNeedSequence, "");
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}

			String debriefItemUsageType = (String) selectedItem.get("description");
			MetrixLookupDef lookupDef;
			Intent intent;
			if (MetrixStringHelper.valueIsEqual(debriefItemUsageType, "DUTPartsOrdered")) {
				mClickedPartFromStock = false;

				lookupDef = new MetrixLookupDef("part_need", true);
				lookupDef.tableNames.add(new MetrixLookupTableDef("part", "part_need", "part_need.part_id", "part.part_id", "="));

				lookupDef.columnNames.add(new MetrixLookupColumnDef("part_need.part_id"));
				lookupDef.columnNames.add(new MetrixLookupColumnDef("part.internal_descriptn"));
				lookupDef.columnNames.add(new MetrixLookupColumnDef("part_need.place_id_from"));
				lookupDef.columnNames.add(new MetrixLookupColumnDef("part_need.location_from"));
				lookupDef.columnNames.add(new MetrixLookupColumnDef("part_need.sequence", R.id.part_need__sequence));
				lookupDef.filters.add(new MetrixLookupFilterDef("part_need.task_id", "=", MetrixCurrentKeysHelper.getKeyValue("task", "task_id")));
				lookupDef.filters.add(new MetrixLookupFilterDef("part_need.parts_used", "!=", "Y"));
				lookupDef.filters.add(new MetrixLookupFilterDef("part_need.line_code_type", "=", "PT"));
				lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA", "PartNeed");

				intent = MetrixActivityHelper.createActivityIntent(this, Lookup.class);
				MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
				MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);
				startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);
			} else if (MetrixStringHelper.valueIsEqual(debriefItemUsageType, "DUTPartsFromMyStock")) {
				mClickedPartFromStock = true;
				lookupDef = new MetrixLookupDef("stock_bin", true);
				lookupDef.tableNames.add(new MetrixLookupTableDef("part", "stock_bin", "stock_bin.part_id", "part.part_id", "="));

				lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_bin.part_id", R.id.truck_part_selected));
				lookupDef.columnNames.add(new MetrixLookupColumnDef("part.internal_descriptn"));
				lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_bin.place_id", R.id.truck_part_selected_place));
				lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_bin.location", R.id.truck_part_selected_location));
				lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_bin.qty_on_hand"));
				lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA", "Part");
				lookupDef.filters.add(new MetrixLookupFilterDef("stock_bin.usable", "=", "Y"));
				lookupDef.filters.add(new MetrixLookupFilterDef("stock_bin.qty_on_hand", ">", "0"));

				intent = MetrixActivityHelper.createActivityIntent(this, DebriefPartUsageLookup.class);
				MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
				MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);
				intent.putExtra("displayPartsOnly", true);
				startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);
			} else if (MetrixStringHelper.valueIsEqual(debriefItemUsageType, "DUTPartsFind")) {
				mClickedPartFromStock = false;
				if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
						ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					StockSearchEntry searchPart = new StockSearchEntry(this);
					searchPart.initDialog();
				} else {
					ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
							STOCK_SEARCH_LOCATION_PERMISSION_REQUEST);
				}
			} else if (MetrixStringHelper.valueIsEqual(debriefItemUsageType, "DUTPartsMisc")) {
				mClickedPartFromStock = false;
				intent = MetrixActivityHelper.createActivityIntent(this, DebriefPartUsage.class);
				MetrixActivityHelper.startNewActivity(this, intent);
			}
		};
	}

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		//Tablet UI Optimization
		if (taskIsComplete() || scriptEventConsumesListTap(this, view, linkedScreenIdInTabletUIMode)) return;

		mSelectedPosition = position;
		OnClickListener yesListener = (dialog, which) -> {
			try {
				HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);
				String metrixRowId = selectedItem.get("part_usage.metrix_row_id");
				String puId = selectedItem.get("part_usage.pu_id");

				MetrixUpdateManager.delete(DebriefPartUsageType.this, "part_usage", metrixRowId, "pu_id", puId, AndroidResourceHelper.getMessage("PartsUsed"), MetrixTransaction.getTransaction("task", "task_id"));

				Intent intent = MetrixActivityHelper.createActivityIntent(mCurrentActivity, DebriefPartUsageType.class);
				MetrixActivityHelper.startNewActivityAndFinish(mCurrentActivity, intent);
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		MetrixDialogAssistant.showConfirmDeleteDialog(AndroidResourceHelper.getMessage("PartUsageLCase"), yesListener, null, DebriefPartUsageType.this);
		//End Tablet UI Optimization
	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

	}

	static class PartUsageTypeAdapter extends RecyclerView.Adapter<PartUsageTypeAdapter.PUTViewHolder> {
		private final LayoutInflater mInflater;
		public List<HashMap<String, Object>> mListData;
		private final PUTClickListener listener;

		PartUsageTypeAdapter(Context context, List<HashMap<String, Object>> dataList, PUTClickListener listener) {
			mInflater = LayoutInflater.from(context);
			mListData = dataList;
			this.listener = listener;
		}

		@NonNull
		@Override
		public PUTViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			final View view = mInflater.inflate(R.layout.debrief_part_usage_type_item, parent, false);
			return new PUTViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull PUTViewHolder holder, int position) {
			final HashMap<String, Object> item = mListData.get(position);

			Integer imageResource = (Integer) item.get("icon");
			if (imageResource != null) {
				Glide.with(holder.itemView.getContext())
						.load(imageResource)
						.into(holder.partsUsageTypeIcon);
			}

			// blend icon if item is disabled
			Boolean isEnabled = (Boolean) item.get("enable");
			if ((isEnabled != null) && (!isEnabled)) {
				holder.partsUsageTypeIcon.setColorFilter(0x99D0D0D0, Mode.SRC_ATOP);
				holder.itemView.setBackgroundColor(0x99D0D0D0);
				holder.partsUsageDescription.setTextColor(0xff888888);
				holder.itemView.setClickable(false);
			}

			Integer count = (Integer) item.get("count");
			if (count == null) {
				holder.partsUsageDescription.setText(AndroidResourceHelper.getMessage((String) item.get("description")));
			} else {
				holder.partsUsageDescription.setText(String.format("%s (%d)", AndroidResourceHelper.getMessage((String) item.get("description")), count));
			}
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemCount() {
			return mListData.size();
		}

		class PUTViewHolder extends RecyclerView.ViewHolder {
			private final TextView partsUsageDescription;
			private final ImageView partsUsageTypeIcon;

			PUTViewHolder(View view) {
				super(view);
				partsUsageDescription = itemView.findViewById(R.id.debrief_part_usage_type_description);
				partsUsageTypeIcon = itemView.findViewById(R.id.debrief_part_usage_type_icon);

				view.setOnClickListener((v) -> {
					final int position = getAdapterPosition();
					if (position != RecyclerView.NO_POSITION && listener != null)
						listener.onPUTClicked(position, mListData.get(position), itemView);
				});
			}
		}

		interface PUTClickListener {
			void onPUTClicked(int position, @NonNull HashMap<String, Object> listItemData, @NonNull View view);
		}
	}

	//Tablet UI Optimization
	@Override
	public boolean isTabletSpecificLandscapeUIRequired()
	{
		return true;
	}

	protected void renderTabletSpecificLayoutControls() {
		super.renderTabletSpecificLayoutControls();
		populatePreviousItemList();
		this.listActivityFullNameInTabletUIMode = String.format("%s.%s", "com.metrix.metrixmobile", "DebriefPartUsageList");
	}

	private void populatePreviousItemList() {
		int listScreenId = MetrixScreenManager.getScreenId("DebriefPartUsageList");
		String query = MetrixListScreenManager.generateListQuery("part_usage", String.format("part_usage.task_id = %s", MetrixCurrentKeysHelper.getKeyValue("task", "task_id")), listScreenId);
		MetrixCursor cursor = null;
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(cursor, listScreenId);
				table.add(row);
				cursor.moveToNext();
			}

			table = MetrixListScreenManager.performScriptListPopulation(this, listScreenId, "DebriefPartUsageList", table);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		if (mAdapter == null) {
			mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
					R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, listScreenId, "part_usage.metrix_row_id", this);
			recyclerView.setAdapter(mAdapter);
		} else {
			mAdapter.updateData(table);
		}
	}
	//End Tablet UI Optimization

	//Added by RaWiLK on 6/20/2016.
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
		try {
			if (resultCode == RESULT_OK) {
				Bundle extras = resultIntent.getExtras();
				if (extras != null) {
					GenericParcelable<List<HashMap<String, String>>> parcelableList;
					if (extras.containsKey("SerialList")) {
						parcelableList = extras.getParcelable("SerialList");
						if (parcelableList != null) {
							List<HashMap<String, String>> serialList = parcelableList.getValue();
							if (serialList != null && serialList.size() > 0) {
								int qty = serialList.size();
								HashMap<String, String> firstItem = serialList.get(0);
								String partId = firstItem.get("stock_serial_id.part_id");
								String lotId = firstItem.get("stock_serial_id.lot_id");

								try {
									ArrayList<MetrixSqlData> partUsagesToInsert = new ArrayList<MetrixSqlData>();
									int puId = setDPUDetail(partUsagesToInsert, partId, String.valueOf(qty));

									MetrixUpdateManager.pauseSync();

									setCustomSerialList(serialList, partUsagesToInsert, puId);
									saveDPUResult(partId, lotId, partUsagesToInsert, puId);

									MetrixControlAssistant.setValue(mTruckPartSelected, "");
									MetrixControlAssistant.setValue(mTruckPartSelectedSerial, "");
									MetrixControlAssistant.setValue(mTruckPartSelectedPlace, "");
									MetrixControlAssistant.setValue(mTruckPartSelectedLocation, "");
									MetrixControlAssistant.setValue(mLotId, "");
								} catch (Exception e) {
									LogManager.getInstance(this).error(e);
								} finally {
									MetrixUpdateManager.resumeSync();
								}
							}
						}
					} else if (extras.containsKey("LotListWithQty")) {
						parcelableList = extras.getParcelable("LotListWithQty");
						if (parcelableList != null) {
							List<HashMap<String, String>> lotListWithQty = parcelableList.getValue();
							if (lotListWithQty != null && lotListWithQty.size() > 0) {
								long qty = 0;
								for (int y = 0; y < lotListWithQty.size(); y++) {
									HashMap<String, String> selectedItem = lotListWithQty.get(y);
									String strMinQty = selectedItem.get("MinQty");
									if (!MetrixStringHelper.isNullOrEmpty(strMinQty)) {
										long minQty = Long.parseLong(strMinQty);
										qty = qty + minQty;
									}
								}

								HashMap<String, String> firstItem = lotListWithQty.get(0);
								String partId = firstItem.get("stock_lot_table.part_id");

								try {
									ArrayList<MetrixSqlData> partUsagesToInsert = new ArrayList<MetrixSqlData>();
									int puId = setDPUDetail(partUsagesToInsert, partId, String.valueOf(qty));

									MetrixUpdateManager.pauseSync();

									saveDPUResult(partId, lotListWithQty, partUsagesToInsert, puId);

									MetrixControlAssistant.setValue(mTruckPartSelected, "");
									MetrixControlAssistant.setValue(mTruckPartSelectedPlace, "");
									MetrixControlAssistant.setValue(mTruckPartSelectedLocation, "");
									MetrixControlAssistant.setValue(mLotId, "");
								} catch (Exception e) {
									LogManager.getInstance(mCurrentActivity).error(e);
								} finally {
									MetrixUpdateManager.resumeSync();
								}
							}
						}
					} else if (extras.containsKey("SerialLotList")) {
						parcelableList = extras.getParcelable("SerialLotList");
						if (parcelableList != null) {
							List<HashMap<String, String>> serialLotList = parcelableList.getValue();
							if (serialLotList != null && serialLotList.size() > 0) {
								int qty = serialLotList.size();
								HashMap<String, String> firstItem = serialLotList.get(0);
								String partId = firstItem.get("stock_serial_id.part_id");

								try {
									MetrixUpdateManager.pauseSync();

									ArrayList<MetrixSqlData> partUsagesToInsert = new ArrayList<MetrixSqlData>();
									int puId = setDPUDetail(partUsagesToInsert, partId, String.valueOf(qty));
									setCustomSerialLotList(serialLotList, partUsagesToInsert, puId);

									MetrixTransaction transactionInfo = new MetrixTransaction();
									boolean successful = MetrixUpdateManager.update(partUsagesToInsert, true, transactionInfo, AndroidResourceHelper.getMessage("PartsUsed"), this);

									if (successful) {
										HashMap<String, Long> lotSerialMap = new HashMap<String, Long>();
										for (int i = 0; i < serialLotList.size(); i++) {
											HashMap<String, String> item = serialLotList.get(i);
											String lotId = item.get("stock_serial_id.lot_id");

											if (lotSerialMap.containsKey(lotId)) {
												long currentQty = lotSerialMap.get(lotId);
												lotSerialMap.put(lotId, currentQty + 1);
											} else {
												lotSerialMap.put(lotId, 1L);
											}
										}

										ArrayList<MetrixSqlData> lotsToInsert = new ArrayList<MetrixSqlData>();
										Iterator<String> lotIdSet = lotSerialMap.keySet().iterator();

										while (lotIdSet.hasNext()) {
											String lotId = lotIdSet.next();
											if (!MetrixStringHelper.isNullOrEmpty(lotId)) {
												MetrixSqlData pulot = new MetrixSqlData("part_usage_lot", MetrixTransactionTypes.INSERT, "");
												pulot.dataFields.add(new DataField("pu_id", puId));
												pulot.dataFields.add(new DataField("lot_id", lotId));

												long lotQty = lotSerialMap.get(lotId);
												pulot.dataFields.add(new DataField("quantity", lotQty));
												lotsToInsert.add(pulot);
											}
										}

										transactionInfo = new MetrixTransaction();
										MetrixUpdateManager.update(lotsToInsert, true, transactionInfo, AndroidResourceHelper.getMessage("PartUsageLot"), mCurrentActivity);
									}

									MetrixControlAssistant.setValue(mTruckPartSelected, "");
									MetrixControlAssistant.setValue(mTruckPartSelectedSerial, "");
									MetrixControlAssistant.setValue(mTruckPartSelectedPlace, "");
									MetrixControlAssistant.setValue(mTruckPartSelectedLocation, "");
									MetrixControlAssistant.setValue(mLotId, "");
								} catch (Exception e) {
									LogManager.getInstance(mCurrentActivity).error(e);
								} finally {
									MetrixUpdateManager.resumeSync();
								}
							}
						}
					} else {
						String partId = "";
						String serialId = "";
						String partNeedSequence = "";

						try {
							partId = MetrixControlAssistant.getValue(mTruckPartSelected);
							serialId = MetrixControlAssistant.getValue(mTruckPartSelectedSerial);
							partNeedSequence = MetrixControlAssistant.getValue(mPartNeedSequence);
						} catch (Exception e) {
							LogManager.getInstance(this).error(e);
						}

						if (!MetrixStringHelper.isNullOrEmpty(partId)) {
							if (MetrixStringHelper.isNullOrEmpty(serialId)) {
								this.recordTruckStockBeingUsed();
							} else {
								this.recordTruckStockSerialBeingUsed();
							}
						} else if (!MetrixStringHelper.isNullOrEmpty(partNeedSequence)) {
							this.recordPartNeedsBeingUsed();
						}
					}
				}
			}
			super.onActivityResult(requestCode, resultCode, resultIntent);

		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private void setCustomSerialList(List<HashMap<String, String>> serialList, ArrayList<MetrixSqlData> partUsagesToInsert, int puId) {
		for (int i = 0; i < serialList.size(); i++) {
			MetrixSqlData serial = new MetrixSqlData("part_usage_serial", MetrixTransactionTypes.INSERT, "");

			HashMap<String, String> item = serialList.get(i);
			String serialId = item.get("stock_serial_id.serial_id");

			serial.dataFields.add(new DataField("pu_id", puId));
			serial.dataFields.add(new DataField("serial_id", serialId));

			partUsagesToInsert.add(serial);
		}
	}

	private void setCustomSerialLotList(List<HashMap<String, String>> serialLotList, ArrayList<MetrixSqlData> partUsagesToInsert, int puId) {
		for (int i = 0; i < serialLotList.size(); i++) {
			MetrixSqlData serialLot = new MetrixSqlData("part_usage_serial", MetrixTransactionTypes.INSERT, "");

			HashMap<String, String> item = serialLotList.get(i);
			String serialId = item.get("stock_serial_id.serial_id");
			String lotId = item.get("stock_serial_id.lot_id");

			serialLot.dataFields.add(new DataField("pu_id", puId));
			serialLot.dataFields.add(new DataField("serial_id", serialId));
			serialLot.dataFields.add(new DataField("lot_id", lotId));

			partUsagesToInsert.add(serialLot);
		}
	}

	private int setDPUDetail(ArrayList<MetrixSqlData> partUsagesToInsert, String partId, String qty) {
		try {
			int puId = MetrixDatabaseManager.generatePrimaryKey("part_usage");

			MetrixSqlData data = new MetrixSqlData("part_usage", MetrixTransactionTypes.INSERT, "");
			data.dataFields.add(new DataField("pu_id", puId));
			data.dataFields.add(new DataField("task_id", MetrixCurrentKeysHelper.getKeyValue("task", "task_id")));
			data.dataFields.add(new DataField("part_id", partId));
			data.dataFields.add(new DataField("quantity", qty));
			data.dataFields.add(new DataField("work_dt", MetrixDateTimeHelper.getCurrentDate(User.getUser().serverDateFormat)));
			data.dataFields.add(new DataField("part_line_code", "PB"));
			data.dataFields.add(new DataField("place_id_from", MetrixControlAssistant.getValue(mTruckPartSelectedPlace)));
			data.dataFields.add(new DataField("location", MetrixControlAssistant.getValue(mTruckPartSelectedLocation)));

			simplePriceIfEnabled(partId, data);
			partUsagesToInsert.add(data);

			return puId;
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
		return -1;
	}

	private void savePartUsageLotIfNeeded(String lotId, String puId) {
		try {
			if (!MetrixStringHelper.isNullOrEmpty(lotId)) {
				ArrayList<MetrixSqlData> lotsToInsert = new ArrayList<MetrixSqlData>();
				MetrixSqlData pulot = new MetrixSqlData("part_usage_lot", MetrixTransactionTypes.INSERT, "");
				pulot.dataFields.add(new DataField("pu_id", puId));
				pulot.dataFields.add(new DataField("lot_id", lotId));
				pulot.dataFields.add(new DataField("quantity", "1"));
				lotsToInsert.add(pulot);
				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(lotsToInsert, true, transactionInfo, AndroidResourceHelper.getMessage("PartUsageLot"), this);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private void saveCustomLotIfNeeded(List<HashMap<String, String>> lotListWithQty, int puId){
		try {
			ArrayList<MetrixSqlData> lotsToInsert = new ArrayList<MetrixSqlData>();

			for (int y = 0; y < lotListWithQty.size(); y++) {
				HashMap<String, String> selectedItem = lotListWithQty.get(y);

				String lotId = selectedItem.get("stock_lot_table.lot_id");
				String lotQty = selectedItem.get("MinQty");

				if (!MetrixStringHelper.isNullOrEmpty(lotId)) {
					MetrixSqlData pulot = new MetrixSqlData("part_usage_lot", MetrixTransactionTypes.INSERT, "");
					pulot.dataFields.add(new DataField("pu_id", puId));
					pulot.dataFields.add(new DataField("lot_id", lotId));
					if (!MetrixStringHelper.isNullOrEmpty(lotQty))
						pulot.dataFields.add(new DataField("quantity", lotQty));
					lotsToInsert.add(pulot);
				}
			}
			MetrixTransaction transactionInfo = new MetrixTransaction();
			MetrixUpdateManager.update(lotsToInsert, true, transactionInfo, AndroidResourceHelper.getMessage("PartUsageLot"), mCurrentActivity);

		} catch (Exception e) {
			LogManager.getInstance(mCurrentActivity).error(e);
		}
	}

	private void savePartOnlyUsage(String partId, String placeId, String location) {

		ArrayList<MetrixSqlData> partUsagesToInsert = new ArrayList<MetrixSqlData>();
		MetrixSqlData data = new MetrixSqlData("part_usage", MetrixTransactionTypes.INSERT, "");
		int tempPuId = MetrixDatabaseManager.generatePrimaryKey("part_usage");
		data.dataFields.add(new DataField("pu_id", tempPuId));
		data.dataFields.add(new DataField("task_id", MetrixCurrentKeysHelper.getKeyValue("task", "task_id")));
		data.dataFields.add(new DataField("part_id", partId));

		String qty = "1";
		if (MetrixPublicCache.instance.containsKey("PartOnlyUsedQty")) {
			qty = (String) MetrixPublicCache.instance.getItem("PartOnlyUsedQty");
			MetrixPublicCache.instance.removeItem("PartUsedQty");
		}
		if (MetrixStringHelper.isNullOrEmpty(qty)) qty = "1";
		data.dataFields.add(new DataField("quantity", qty));

		data.dataFields.add(new DataField("work_dt", MetrixDateTimeHelper.getCurrentDate(User.getUser().serverDateFormat)));
		data.dataFields.add(new DataField("part_line_code", "PB"));
		data.dataFields.add(new DataField("place_id_from", placeId));
		data.dataFields.add(new DataField("location", location));

		simplePriceIfEnabled(partId, data);
		partUsagesToInsert.add(data);

		try {
			MetrixUpdateManager.pauseSync();
			MetrixTransaction transactionInfo = new MetrixTransaction();
			boolean successful = MetrixUpdateManager.update(partUsagesToInsert, true, transactionInfo, AndroidResourceHelper.getMessage("PartsUsed"), this);

			if (successful) {

				savePartUsageLotIfNeeded(Integer.toString(tempPuId));

				String controlledPart = MetrixDatabaseManager.getFieldStringValue("part", "controlled_part", "part_id='" + partId + "'");

				if (controlledPart.toLowerCase().contains("y")) {
					MetrixCurrentKeysHelper.setKeyValue("part_usage", "pu_id", Integer.toString(tempPuId));

					Intent intent = MetrixActivityHelper.createActivityIntent(this, MetadataDebriefActivity.class);
					intent.putExtra("ScreenID", MetrixScreenManager.getScreenId("DebriefPartDisposition"));
					intent.putExtra("NavigatedFromLinkedScreen", true);
					MetrixActivityHelper.startNewActivity(this, intent);
				} else {
					Intent intent = MetrixActivityHelper.createActivityIntent(this, DebriefPartUsageType.class);
					MetrixActivityHelper.startNewActivityAndFinish(this, intent);
				}
			}

			MetrixControlAssistant.setValue(mTruckPartSelected, "");
			MetrixControlAssistant.setValue(mTruckPartSelectedPlace, "");
			MetrixControlAssistant.setValue(mTruckPartSelectedLocation, "");
			MetrixControlAssistant.setValue(mLotId, "");
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		} finally {
			MetrixUpdateManager.resumeSync();
		}
	}

	private void saveDPUResult(String partId, List<HashMap<String, String>> lotListWithQty, ArrayList<MetrixSqlData> partUsagesToInsert, int puId) {
		MetrixTransaction transactionInfo = new MetrixTransaction();
		boolean successful = MetrixUpdateManager.update(partUsagesToInsert, true, transactionInfo, AndroidResourceHelper.getMessage("PartsUsed"), this);

		if (successful) {

			saveCustomLotIfNeeded(lotListWithQty, puId);

			String controlledPart = MetrixDatabaseManager.getFieldStringValue("part", "controlled_part", "part_id='" + partId + "'");

			if (controlledPart.toLowerCase().contains("y")) {
				MetrixCurrentKeysHelper.setKeyValue("part_usage", "pu_id", Integer.toString(puId));

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetadataDebriefActivity.class);
				intent.putExtra("ScreenID", MetrixScreenManager.getScreenId("DebriefPartDisposition"));
				intent.putExtra("NavigatedFromLinkedScreen", true);
				MetrixActivityHelper.startNewActivity(this, intent);
			} else {
				Intent intent = MetrixActivityHelper.createActivityIntent(this, DebriefPartUsageType.class);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		}
	}

	private void saveDPUResult(String partId, String lotId, ArrayList<MetrixSqlData> partUsagesToInsert, int puId) {
		MetrixTransaction transactionInfo = new MetrixTransaction();
		boolean successful = MetrixUpdateManager.update(partUsagesToInsert, true, transactionInfo, AndroidResourceHelper.getMessage("PartsUsed"), this);

		if (successful) {

			savePartUsageLotIfNeeded(lotId, Integer.toString(puId));

			String controlledPart = MetrixDatabaseManager.getFieldStringValue("part", "controlled_part", "part_id='" + partId + "'");

			if (controlledPart.toLowerCase().contains("y")) {
				MetrixCurrentKeysHelper.setKeyValue("part_usage", "pu_id", Integer.toString(puId));

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetadataDebriefActivity.class);
				intent.putExtra("ScreenID", MetrixScreenManager.getScreenId("DebriefPartDisposition"));
				intent.putExtra("NavigatedFromLinkedScreen", true);
				MetrixActivityHelper.startNewActivity(this, intent);
			} else {
				Intent intent = MetrixActivityHelper.createActivityIntent(this, DebriefPartUsageType.class);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		}
	}
	//End Added by RaWiLK on 6/20/2016.

	@Override
	protected void onResume() {
		super.onResume();
		if (shouldShowStockSearch) {
			shouldShowStockSearch = false;
			StockSearchEntry searchPart = new StockSearchEntry(this);
			searchPart.initDialog();
		}

		if (incompletePermissionRequest == STOCK_SEARCH_LOCATION_PERMISSION_REQUEST) {
			final int requestCode = incompletePermissionRequest;
			incompletePermissionRequest = 0;
			DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (ActivityCompat.shouldShowRequestPermissionRationale(DebriefPartUsageType.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
						// can request permission again
						ActivityCompat.requestPermissions(DebriefPartUsageType.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
					} else {
						// user need to go to app settings to enable it
						try {
							Intent intent = new Intent();
							intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
							Uri uri = Uri.fromParts("package", getPackageName(), null);
							intent.setData(uri);
							startActivity(intent);
						} catch (ActivityNotFoundException ex) {
							// This is extremely rare
							MetrixUIHelper.showSnackbar(DebriefPartUsageType.this, R.id.coordinator_layout, AndroidResourceHelper
									.getMessage("EnablePermManuallyAndRetry"));
						}
					}
				}
			};

			MetrixDialogAssistant.showAlertDialog(
					AndroidResourceHelper.getMessage("PermissionRequired"),
					AndroidResourceHelper.getMessage("LocationPermGenericExpl"),
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
		if (requestCode == STOCK_SEARCH_LOCATION_PERMISSION_REQUEST) {
			boolean locationPermissionGranted = false;
			for (int grantResult : grantResults) {
				if (grantResult == PackageManager.PERMISSION_GRANTED) {
					locationPermissionGranted = true;
					break;
				}
			}
			if (locationPermissionGranted) {
				shouldShowStockSearch = true;
			} else {
				// Showing dialog at this Activity lifecycle can lead to app crash as the view is not guaranteed to
				// be visible to the user. So we set the incompletePermissionRequest value and handle it inside
				// onResume activity life cycle
				incompletePermissionRequest = requestCode;
			}
		}
	}
}
