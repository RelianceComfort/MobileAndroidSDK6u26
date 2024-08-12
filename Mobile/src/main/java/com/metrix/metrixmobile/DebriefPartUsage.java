package com.metrix.metrixmobile;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixFieldLookupManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixLookupFilterDef;
import com.metrix.architecture.metadata.MetrixLookupTableDef;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.global.MobileGlobal;
import com.metrix.metrixmobile.system.DebriefActivity;
import com.metrix.metrixmobile.system.Lookup;
import com.metrix.metrixmobile.system.MetadataDebriefActivity;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

public class DebriefPartUsage extends DebriefActivity implements View.OnClickListener, OnFocusChangeListener, View.OnTouchListener, TextWatcher {
	private FloatingActionButton mNextButton;
	private EditText mPartId, mPuId, mPlaceIdFrom, mLocation, mLotId;
	private TextView mSelectedSerial;

	private List<FloatingActionButton> mFABList;
	private List<FloatingActionButton> mFABsToShow;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debrief_part_usage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		super.onStart();

		mLayout = (ViewGroup) findViewById(R.id.table_layout);
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		if (mFABList == null)
			mFABList = new ArrayList<FloatingActionButton>();
		else
			mFABList.clear();

		mNextButton = (FloatingActionButton) findViewById(R.id.next);
		mPuId = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "part_usage", "pu_id");
		mPartId = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "part_usage", "part_id");
		mPlaceIdFrom = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "part_usage", "place_id_from");
		mLocation = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "part_usage", "location");
		mSelectedSerial = (TextView) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "selected_serial");
		mLotId = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "lot_id");

		if (!MetrixFieldLookupManager.fieldHasLookup(MetrixControlAssistant.getFieldId(mFormDef, "part_usage", "part_id"))) {
			mPartId.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.magnifying_glass, 0);
			mPartId.setOnTouchListener(this);
		}

		if (!MetrixFieldLookupManager.fieldHasLookup(MetrixControlAssistant.getFieldId(mFormDef, "part_usage", "place_id_from"))) {
			mPlaceIdFrom.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.magnifying_glass, 0);
			mPlaceIdFrom.setOnTouchListener(this);
		}

		if (!MetrixFieldLookupManager.fieldHasLookup(MetrixControlAssistant.getFieldId(mFormDef, "part_usage", "location"))) {
			mLocation.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.magnifying_glass, 0);
			mLocation.setOnTouchListener(this);
		}

		if (!MetrixFieldLookupManager.fieldHasLookup(MetrixControlAssistant.getFieldId(mFormDef, "custom", "lot_id"))) {
			mLotId.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.magnifying_glass, 0);
			mLotId.setOnTouchListener(this);
		}

		MetrixColumnDef thisColDef = mFormDef.getColumnDef("custom", "lot_id");
		if (thisColDef != null) {
			mLotId.setVisibility(View.GONE);
			if (thisColDef.labelId != null)
				MetrixControlAssistant.setVisibility(thisColDef.labelId, mLayout, View.GONE);
		}

		mNextButton.setOnClickListener(this);

		mFABList.add(mNextButton);
		fabRunnable = this::showFABs;

		float mOffset = generateOffsetForFABs(mFABList);
		// The amount subtracted it equal to the button height plus the top and bottom margins of the button
		mOffset -= getResources().getDimension((R.dimen.button_height)) + (2*getResources().getDimension((R.dimen.md_margin)));

		NestedScrollView scrollView = findViewById(R.id.scroll_view);
		mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(),(int)mOffset);
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

		mSelectedSerial.removeTextChangedListener(this);
		mSelectedSerial.addTextChangedListener(this);

		mPartId.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				String partId = MetrixControlAssistant.getValue(mPartId.getId(), mLayout);
				String lotIdentified = MetrixDatabaseManager.getFieldStringValue("part", "lot_identified", "part_id='" + partId + "'");
				String serializedPart = MetrixDatabaseManager.getFieldStringValue("part", "serialed", "part_id='" + partId + "'");
				EditText quantity = (EditText)MetrixControlAssistant.getControl(mFormDef,mLayout,"part_usage","quantity");
				quantity.setEnabled(true);

				MetrixColumnDef thisColDef = mFormDef.getColumnDef("custom", "lot_id");
				if (thisColDef != null)
				{
					if (lotIdentified.compareToIgnoreCase("y") == 0 && serializedPart.compareToIgnoreCase("n") == 0)
					{
						mLotId.setVisibility(View.VISIBLE);
						if (thisColDef.labelId != null)
							MetrixControlAssistant.setVisibility(thisColDef.labelId, mLayout, View.VISIBLE);
					}else if(serializedPart.compareToIgnoreCase("y") == 0){
						//default quantity to 1
						MetrixControlAssistant.setValue(mFormDef,mLayout,"part_usage","quanity","1");
						//disable field
						quantity.setEnabled(false);
					}else {
						MetrixControlAssistant.setValue(mFormDef, mLayout, "custom", "lot_id", "");
						mLotId.setVisibility(View.GONE);
						if (thisColDef.labelId != null)
							MetrixControlAssistant.setVisibility(thisColDef.labelId, mLayout, View.GONE);
					}
				}
			}

			@Override
			public void afterTextChanged(Editable s) {}
		});

	}

	/**
	 * Set the default values for views for this activity.
	 */
	protected void defaultValues() {
		try {
			MetrixControlAssistant.setValue(mFormDef, mLayout, "part_usage", "task_id", MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));
			MetrixControlAssistant.setValue(mFormDef, mLayout, "part_usage", "work_dt", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_FORMAT));

			String[] columnsToGet = new String[] { "place_id", "location" };
			Hashtable<String, String> results = MetrixDatabaseManager.getFieldStringValues("person_place", columnsToGet, "place_relationship='FOR_STOCK'");

			MetrixControlAssistant.setValue(mFormDef, mLayout, "part_usage", "place_id_from", results.get("place_id"));
			MetrixControlAssistant.setValue(mFormDef, mLayout, "part_usage", "location", results.get("location"));

		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
		MetrixTableDef partUsageDef = new MetrixTableDef("part_usage", MetrixTransactionTypes.INSERT);
		this.mFormDef = new MetrixFormDef(partUsageDef);
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

		switch (v.getId()) {
			case R.id.next:
				if (anyOnStartValuesChanged() && (!taskIsComplete())) {
					try {
						String partId = MetrixControlAssistant.getValue(mPartId);
						String serialized = MetrixDatabaseManager.getFieldStringValue("part", "serialed", "part_id='" + partId + "'");
						String lotIdentified = MetrixDatabaseManager.getFieldStringValue("part", "lot_identified", "part_id='" + partId + "'");

						if (serialized.compareToIgnoreCase("Y") == 0) {
							MetrixLookupDef lookupDef = new MetrixLookupDef("stock_serial_id", true);
							lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_serial_id.serial_id", mSelectedSerial.getId()));
							lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_serial_id.part_id"));
							lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_serial_id.lot_id", mLotId.getId()));
							lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA","SerialId");
							lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.part_id", "=", partId));
							lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.usable", "=", "Y"));
							lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.in_transit", "!=", "Y"));
							lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.place_id", "=", MetrixControlAssistant.getValue(mPlaceIdFrom)));
							lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.location", "=", MetrixControlAssistant.getValue(mLocation)));

							Intent intent = MetrixActivityHelper.createActivityIntent(this, Lookup.class);
							MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
							MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);
							startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);
						} else if (lotIdentified.compareToIgnoreCase("y") == 0 && MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue(mLotId))) {
							MetrixLookupDef lookupDef = new MetrixLookupDef();
							lookupDef.tableNames.add(new MetrixLookupTableDef("stock_lot_table"));
							lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_lot_table.part_id"));
							lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_lot_table.lot_id", mLotId.getId()));
							lookupDef.filters.add(new MetrixLookupFilterDef("stock_lot_table.part_id", "=", MetrixControlAssistant.getValue(mPartId.getId(), mLayout)));
							lookupDef.initialSearchCriteria = MetrixControlAssistant.getValue(v.getId(), mLayout);

							lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA", "LotId");

							Intent intent = MetrixActivityHelper.createActivityIntent(this, Lookup.class);
							MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
							MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);
							startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);
						} else {
							savePartUsage();
						}
					} catch (Exception ex) {
						LogManager.getInstance().error(ex);
					} finally {
						MetrixUpdateManager.resumeSync();
					}
				} else {
					MetrixWorkflowManager.advanceWorkflow(this);
				}
			default:
				super.onClick(v);
		}
	}

	private void savePartUsage() {
		String partId = "";
		try {
			partId = MetrixControlAssistant.getValue(mPartId);
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}

		try {
			MetrixUpdateManager.pauseSync();

			String controlled = MetrixDatabaseManager.getFieldStringValue("part", "controlled_part", "part_id='" + partId + "'");
			if (controlled.compareToIgnoreCase("Y") == 0) {
				MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
				simplePriceIfEnabled();

				MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, AndroidResourceHelper.getMessage("PartsUsed"));

				savePartUsageSerialIfNeeded();
				savePartUsageLotIfNeeded();

				MetrixCurrentKeysHelper.setKeyValue("part_usage", "pu_id", mPuId.getText().toString());

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetadataDebriefActivity.class);
				intent.putExtra("ScreenID", MetrixScreenManager.getScreenId("DebriefPartDisposition"));
				intent.putExtra("NavigatedFromLinkedScreen", true);
				MetrixActivityHelper.startNewActivity(this, intent);
			} else {
				MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
				simplePriceIfEnabled();
				MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, AndroidResourceHelper.getMessage("PartsUsed"));

				savePartUsageSerialIfNeeded();
				savePartUsageLotIfNeeded();

				Intent nextIntent = MetrixActivityHelper.createActivityIntent(this, DebriefPartUsageType.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
				MetrixActivityHelper.startNewActivity(this, nextIntent);
			}
		} finally {
			MetrixUpdateManager.resumeSync();
		}
	}

	private void savePartUsageLotIfNeeded() {
		String lotId = "";
		try {
			lotId = MetrixControlAssistant.getValue(mLotId);
			String enteredQty = MetrixControlAssistant.getValue(mFormDef, mLayout, "part_usage", "quantity");

			if (!MetrixStringHelper.isNullOrEmpty(lotId)) {
				ArrayList<MetrixSqlData> lotsToInsert = new ArrayList<MetrixSqlData>();
				MetrixSqlData pulot = new MetrixSqlData("part_usage_lot", MetrixTransactionTypes.INSERT, "");
				pulot.dataFields.add(new DataField("pu_id", mPuId.getText().toString()));
				pulot.dataFields.add(new DataField("lot_id", lotId));
				pulot.dataFields.add(new DataField("quantity", enteredQty));
				lotsToInsert.add(pulot);
				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(lotsToInsert, true, transactionInfo, AndroidResourceHelper.getMessage("PartUsageLot"), this);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private void savePartUsageSerialIfNeeded() {
		String serialId = "";
		try {
			serialId = MetrixControlAssistant.getValue(mSelectedSerial);

			if (!MetrixStringHelper.isNullOrEmpty(serialId)) {
				ArrayList<MetrixSqlData> serialsToInsert = new ArrayList<MetrixSqlData>();
				MetrixSqlData serial = new MetrixSqlData("part_usage_serial", MetrixTransactionTypes.INSERT, "");
				serial.dataFields.add(new DataField("pu_id", MetrixControlAssistant.getValue(mPuId)));
				serial.dataFields.add(new DataField("serial_id", serialId));
				serialsToInsert.add(serial);
				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(serialsToInsert, true, transactionInfo, AndroidResourceHelper.getMessage("PartsUsed"), this);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private void simplePriceIfEnabled() {
		try {
			String simplePricingEnabled = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_MOBILE_SIMPLE_PART_PRICING'");
			String manualBillPrice = MetrixControlAssistant.getValue(mFormDef, mLayout, "part_usage", "bill_price");

			if (simplePricingEnabled.compareToIgnoreCase("y") == 0 && (MetrixStringHelper.isNullOrEmpty(manualBillPrice))) {
				String partId = MetrixControlAssistant.getValue(mPartId);

				StringBuilder query = new StringBuilder();
				query.append("column_1 = '");
				query.append(partId);
				query.append("' and effective_dt < '");
				query.append(MetrixDateTimeHelper.getRelativeDate(MetrixDateTimeHelper.DATE_FORMAT, 0));
				query.append("' and currency = '");
				query.append(User.getUser().getCurrencyToUse());
				query.append("' order by effective_dt DESC limit 1");

				String listPrice = MetrixDatabaseManager.getFieldStringValue("std_part_price_view", "list_price", query.toString());
				MetrixControlAssistant.setValue(mFormDef, mLayout, "part_usage", "unadj_list_price", listPrice);
				MetrixControlAssistant.setValue(mFormDef, mLayout, "part_usage", "bill_price", listPrice);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		final int DRAWABLE_LEFT = 0;
		final int DRAWABLE_TOP = 1;
		final int DRAWABLE_RIGHT = 2;
		final int DRAWABLE_BOTTOM = 3;

		if (event.getAction() == MotionEvent.ACTION_UP) {
			if (v.getId() == mPartId.getId()) {
				if (event.getRawX() >= (mPartId.getRight() - mPartId.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
					MetrixLookupDef lookupDef = new MetrixLookupDef("part");
					lookupDef.columnNames.add(new MetrixLookupColumnDef("part.internal_descriptn"));
					lookupDef.columnNames.add(new MetrixLookupColumnDef("part.part_id", mPartId.getId()));
					lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA", "Part");
					lookupDef.initialSearchCriteria = MetrixControlAssistant.getValue(v.getId(), mLayout);

					Intent intent = MetrixActivityHelper.createActivityIntent(this, Lookup.class);
					MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
					MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);
					startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);
				}
			} else if (v.getId() == mPlaceIdFrom.getId()) {
				if (event.getRawX() >= (mPlaceIdFrom.getRight() - mPlaceIdFrom.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
					MetrixLookupDef lookupDef = new MetrixLookupDef();
					lookupDef.tableNames.add(new MetrixLookupTableDef("place"));
					lookupDef.tableNames.add(new MetrixLookupTableDef("location", "place", "place.place_id", "location.place_id", "="));
					lookupDef.columnNames.add(new MetrixLookupColumnDef("place.name"));
					lookupDef.columnNames.add(new MetrixLookupColumnDef("place.place_id", mPlaceIdFrom.getId()));
					lookupDef.columnNames.add(new MetrixLookupColumnDef("location.location", mLocation.getId()));
					lookupDef.columnNames.add(new MetrixLookupColumnDef("location.description"));
					lookupDef.filters.add(new MetrixLookupFilterDef("place.stock_parts", "=", "Y"));
					lookupDef.filters.add(new MetrixLookupFilterDef("place.whos_place", "<>", "CUST"));
					lookupDef.initialSearchCriteria = MetrixControlAssistant.getValue(v.getId(), mLayout);

					lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA", "PlaceFrom");

					Intent intent = MetrixActivityHelper.createActivityIntent(this, Lookup.class);
					MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
					MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);
					startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);
				}
			} else if (v.getId() == mLocation.getId()) {
				if (event.getRawX() >= (mLocation.getRight() - mLocation.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
					MetrixLookupDef lookupDef = new MetrixLookupDef();
					lookupDef.tableNames.add(new MetrixLookupTableDef("place"));
					lookupDef.tableNames.add(new MetrixLookupTableDef("location", "place", "place.place_id", "location.place_id", "="));
					lookupDef.columnNames.add(new MetrixLookupColumnDef("place.name"));
					lookupDef.columnNames.add(new MetrixLookupColumnDef("place.place_id", mPlaceIdFrom.getId()));
					lookupDef.columnNames.add(new MetrixLookupColumnDef("location.location", mLocation.getId()));
					lookupDef.columnNames.add(new MetrixLookupColumnDef("location.description"));
					lookupDef.filters.add(new MetrixLookupFilterDef("place.stock_parts", "=", "Y"));
					lookupDef.filters.add(new MetrixLookupFilterDef("place.whos_place", "<>", "CUST"));
					lookupDef.initialSearchCriteria = MetrixControlAssistant.getValue(v.getId(), mLayout);

					lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA", "LocationFrom");

					Intent intent = MetrixActivityHelper.createActivityIntent(this, Lookup.class);
					MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
					MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);
					startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);
				}
			} else if (v.getId() == mLotId.getId()) {
				if (event.getRawX() >= (mLotId.getRight() - mLotId.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
					MetrixLookupDef lookupDef = new MetrixLookupDef();
					lookupDef.tableNames.add(new MetrixLookupTableDef("stock_lot_table"));
					lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_lot_table.part_id"));
					lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_lot_table.lot_id", mLotId.getId()));
					lookupDef.filters.add(new MetrixLookupFilterDef("stock_lot_table.part_id", "=", MetrixControlAssistant.getValue(mPartId.getId(), mLayout)));
					lookupDef.initialSearchCriteria = MetrixControlAssistant.getValue(v.getId(), mLayout);

					lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA", "LotId");

					Intent intent = MetrixActivityHelper.createActivityIntent(this, Lookup.class);
					MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
					MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);
					startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);
				}
			}
		}
		return false;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		// only attempt to save if the Serial ID has been populated
		if (!MetrixStringHelper.isNullOrEmpty(s.toString()))
			this.savePartUsage();
	}

	@Override
	public void afterTextChanged(Editable s)
	{

	}
}

