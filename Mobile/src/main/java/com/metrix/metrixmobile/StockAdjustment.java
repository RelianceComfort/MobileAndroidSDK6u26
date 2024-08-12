package com.metrix.metrixmobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.designer.MetrixFieldLookupManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.ui.widget.SimpleRecyclerViewAdapter;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixLookupFilterDef;
import com.metrix.architecture.metadata.MetrixLookupTableDef;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.global.MobileGlobal;
import com.metrix.metrixmobile.system.BottomOffsetDecoration;
import com.metrix.metrixmobile.system.Lookup;
import com.metrix.metrixmobile.system.MetrixActivity;

public class StockAdjustment extends MetrixActivity implements View.OnClickListener, View.OnTouchListener {
	private RecyclerView serialRecyclerView = null;
	private FloatingActionButton mSaveButton, mAddButton, mDeleteButton;
	private EditText mPartID, mSerialID, mQuantity, mPlaceFrom, mPlaceTo, mLocationFrom, mLocationTo, mLotID;
	private Spinner mAdjustReasonSpinner;
	private ArrayList<Map<String, Object>> mSerialIDs = new ArrayList<>();
	private String[] mFrom;
	private int[] mTo;
	private String intentParameter = "";
	private boolean mInitialized = false;
	private SimpleRecyclerViewAdapter adapter;
	private BottomOffsetDecoration mBottomOffset;
	private List<FloatingActionButton> mFABList;
	private List<FloatingActionButton> mFABsToShow;

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			try {
				mSerialIDs = (ArrayList<Map<String, Object>>) savedInstanceState.getSerializable("SerialIDsData");
			}catch(ClassCastException cce) {
				LogManager.getInstance().error(cce);
			}
		}

		setContentView(R.layout.stock_adjustment);
		serialRecyclerView = findViewById(R.id.serialList);
		MetrixListScreenManager.setupVerticalRecyclerView(serialRecyclerView, R.drawable.rv_item_divider);
		serialRecyclerView.setNestedScrollingEnabled(false);
		intentParameter = getIntent().getExtras().getString("action");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		super.onStart();
		this.processIntentParameter();

		if (mFABList == null)
			mFABList = new ArrayList<FloatingActionButton>();
		else
			mFABList.clear();

		mFABList.add(mSaveButton);

		mFABList.add(mAddButton);

		if(mSerialIDs == null || mSerialIDs.isEmpty())
			MetrixControlAssistant.setButtonVisibility(mDeleteButton, View.GONE);
		else
			MetrixControlAssistant.setButtonVisibility(mDeleteButton, View.VISIBLE);

		mFABList.add(mDeleteButton);

		bindRecyclerView();
	}

	private void processIntentParameter() {
		try {
			if (!this.mInitialized) {
				if (intentParameter != null) {
					if (intentParameter.compareToIgnoreCase(StockListMetrixActionView.ADDPART) == 0) {
						MetrixControlAssistant.setValue(mAdjustReasonSpinner, "RECEIPT");
					} else if (intentParameter.compareToIgnoreCase(StockListMetrixActionView.REMOVEPART) == 0) {
						MetrixControlAssistant.setValue(mAdjustReasonSpinner, "ISSUE");
					} else if (intentParameter.compareToIgnoreCase(StockListMetrixActionView.SWAPPART) == 0) {
						MetrixControlAssistant.setValue(mAdjustReasonSpinner, "TRANSFER");
					}
				}

				String partId = getIntent().getExtras().getString("part_id");
				if (!MetrixStringHelper.isNullOrEmpty(partId)) {
					MetrixControlAssistant.setValue(mPartID, partId);

					if (this.partIsSerialed(partId) == 1 && intentParameter.compareToIgnoreCase(StockListMetrixActionView.ADDPART) != 0) {
						MetrixColumnDef serialIdColDef = mFormDef.getColumnDef("custom", "serial_id");
						mSerialID.setVisibility(View.VISIBLE);
						MetrixControlAssistant.setVisibility(serialIdColDef.labelId, mLayout, View.VISIBLE);
					}
				}
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		} finally {
			this.mInitialized = true;
		}
	}

	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putSerializable("SerialIDsData", mSerialIDs);
	}


	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		mSaveButton = (FloatingActionButton) findViewById(R.id.confirm);
		mSaveButton.setOnClickListener(this);

		mAddButton = (FloatingActionButton) findViewById(R.id.addSerial);
		mAddButton.setOnClickListener(this);

		mDeleteButton = (FloatingActionButton) findViewById(R.id.delete);

		mDeleteButton.setOnClickListener(this);

		mPartID = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "part_id");
		mSerialID = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "serial_id");
		mQuantity = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "quantity");
		mAdjustReasonSpinner = (Spinner) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "adjustment_reason");
		mPlaceFrom = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "place_id_from");
		mPlaceTo = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "place_id_to");
		mLocationFrom = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "location_from");
		mLocationTo = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "location_to");
		mLotID = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "lot_id");

		mAdjustReasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				setAdjustmentUI();
			}

			public void onNothingSelected(AdapterView<?> parent) {}
		});

		if (!MetrixFieldLookupManager.fieldHasLookup(MetrixControlAssistant.getFieldId(mFormDef, "custom", "part_id"))) {
			mPartID.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.magnifying_glass, 0);
			mPartID.setOnTouchListener(this);
		}

		if (!MetrixFieldLookupManager.fieldHasLookup(MetrixControlAssistant.getFieldId(mFormDef, "custom", "place_id_from"))) {
			mPlaceFrom.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.magnifying_glass, 0);
			mPlaceFrom.setOnTouchListener(this);
		}

		if (!MetrixFieldLookupManager.fieldHasLookup(MetrixControlAssistant.getFieldId(mFormDef, "custom", "place_id_to"))) {
			mPlaceTo.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.magnifying_glass, 0);
			mPlaceTo.setOnTouchListener(this);
		}

		if (!MetrixFieldLookupManager.fieldHasLookup(MetrixControlAssistant.getFieldId(mFormDef, "custom", "location_from"))) {
			mLocationFrom.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.magnifying_glass, 0);
			mLocationFrom.setOnTouchListener(this);
		}

		if (!MetrixFieldLookupManager.fieldHasLookup(MetrixControlAssistant.getFieldId(mFormDef, "custom", "location_to"))) {
			mLocationTo.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.magnifying_glass, 0);
			mLocationTo.setOnTouchListener(this);
		}

		if (!MetrixFieldLookupManager.fieldHasLookup(MetrixControlAssistant.getFieldId(mFormDef, "custom", "lot_id"))) {
			mLotID.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.magnifying_glass, 0);
			mLotID.setOnTouchListener(this);
		}

		mPartID.addTextChangedListener(new PartTextWatcher());
		mPlaceFrom.addTextChangedListener(new PlaceFromTextWatcher());
		mPlaceTo.addTextChangedListener(new PlaceToTextWatcher());

		fabRunnable = this::showFABs;

		NestedScrollView scrollView = findViewById(R.id.scroll_view);
		scrollView.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
			if ((scrollY > oldScrollY) || (scrollY < oldScrollY)) {
				fabHandler.removeCallbacks(fabRunnable);
				hideFABs(mFABList);
				fabHandler.postDelayed(fabRunnable, fabDelay);
			}
		});
	}

	private void setAdjustmentUI() {
		try {
			hideControls();

			String value = MetrixControlAssistant.getValue(mAdjustReasonSpinner);
			String transRouteId = MetrixDatabaseManager.getFieldStringValue("adjustmnt_reasn", "trans_route_id", "adjustment_reason='" + value + "'");

			MetrixColumnDef placeIdFromColDef = mFormDef.getColumnDef("custom", "place_id_from");
			MetrixColumnDef locationFromColDef = mFormDef.getColumnDef("custom", "location_from");
			MetrixColumnDef placeIdToColDef = mFormDef.getColumnDef("custom", "place_id_to");
			MetrixColumnDef locationToColDef = mFormDef.getColumnDef("custom", "location_to");
			MetrixColumnDef serialIdColDef = mFormDef.getColumnDef("custom", "serial_id");
			MetrixColumnDef lotIdColDef = mFormDef.getColumnDef("custom", "lot_id");

			if (transRouteId.contains("I")) {
				mPlaceFrom.setVisibility(View.VISIBLE);
				mLocationFrom.setVisibility(View.VISIBLE);
				MetrixControlAssistant.setVisibility(placeIdFromColDef.labelId, mLayout, View.VISIBLE);
				MetrixControlAssistant.setVisibility(locationFromColDef.labelId, mLayout, View.VISIBLE);
			} else if (transRouteId.contains("R")) {
				mPlaceTo.setVisibility(View.VISIBLE);
				mLocationTo.setVisibility(View.VISIBLE);
				MetrixControlAssistant.setVisibility(placeIdToColDef.labelId, mLayout, View.VISIBLE);
				MetrixControlAssistant.setVisibility(locationToColDef.labelId, mLayout, View.VISIBLE);
			} else if (transRouteId.contains("T")) {
				mPlaceFrom.setVisibility(View.VISIBLE);
				mLocationFrom.setVisibility(View.VISIBLE);
				mPlaceTo.setVisibility(View.VISIBLE);
				mLocationTo.setVisibility(View.VISIBLE);
				MetrixControlAssistant.setVisibility(placeIdFromColDef.labelId, mLayout, View.VISIBLE);
				MetrixControlAssistant.setVisibility(locationFromColDef.labelId, mLayout, View.VISIBLE);
				MetrixControlAssistant.setVisibility(placeIdToColDef.labelId, mLayout, View.VISIBLE);
				MetrixControlAssistant.setVisibility(locationToColDef.labelId, mLayout, View.VISIBLE);
			}

			String serialized = MetrixDatabaseManager.getFieldStringValue("part", "serialed", "part_id='" + MetrixControlAssistant.getValue(mPartID) + "'");
			if (serialized.compareToIgnoreCase("Y") == 0) {
				mSerialID.setVisibility(View.VISIBLE);
				MetrixControlAssistant.setVisibility(serialIdColDef.labelId, mLayout, View.VISIBLE);
				MetrixControlAssistant.setButtonVisibility(mAddButton, View.VISIBLE);
			} else {
				mSerialID.setVisibility(View.GONE);
				MetrixControlAssistant.setVisibility(serialIdColDef.labelId, mLayout, View.GONE);
				MetrixControlAssistant.setButtonVisibility(mAddButton, View.GONE);
			}

			String lotIdentified = MetrixDatabaseManager.getFieldStringValue("part", "lot_identified", "part_id='" + MetrixControlAssistant.getValue(mPartID) + "'");
			if (lotIdentified.compareToIgnoreCase("Y") == 0) {
				mLotID.setVisibility(View.VISIBLE);
				MetrixControlAssistant.setVisibility(lotIdColDef.labelId, mLayout, View.VISIBLE);
			} else {
				mLotID.setVisibility(View.GONE);
				MetrixControlAssistant.setVisibility(lotIdColDef.labelId, mLayout, View.GONE);
			}

			// Might need to add logic for the padding here
			serialRecyclerView.removeItemDecoration(mBottomOffset);
			int offset = (int)getResources().getDimension((R.dimen.md_margin));
			if (mSerialIDs != null && !mSerialIDs.isEmpty()) {
				mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), offset);
			} else {
				mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), offset + generateOffsetForFABs(mFABList));
			}
			mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));
			serialRecyclerView.addItemDecoration(mBottomOffset);

		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	@Override
	protected void hideFABs(List<FloatingActionButton> fabList) {
		if (mFABsToShow == null)
			mFABsToShow = new ArrayList<>();
		for (FloatingActionButton fab : fabList){
			if (fab.isOrWillBeShown()) {
				mFABsToShow.add(fab);
				fab.hide();
			}
		}
	}

	@Override
	protected void showFABs() {
		for (FloatingActionButton fab : mFABsToShow){
			if (fab.isOrWillBeHidden()) {
				final Object tag = fab.getTag();
				if (!(tag instanceof String && MetrixStringHelper.valueIsEqual((String)tag, MetrixClientScriptManager.HIDDEN_BY_SCRIPT)))
					fab.show();
			}
		}
	}

	private void partChanged() {
		try {
			String serialized = MetrixDatabaseManager.getFieldStringValue("part", "serialed", "part_id='" + MetrixControlAssistant.getValue(mPartID) + "'");

			MetrixColumnDef serialIdColDef = mFormDef.getColumnDef("custom", "serial_id");
			MetrixColumnDef lotIdColDef = mFormDef.getColumnDef("custom", "lot_id");

			if (serialized.compareToIgnoreCase("Y") == 0) {
				mSerialID.setVisibility(View.VISIBLE);
				serialRecyclerView.setVisibility(View.VISIBLE);
				MetrixControlAssistant.setVisibility(serialIdColDef.labelId, mLayout, View.VISIBLE);
				MetrixControlAssistant.setButtonVisibility(mAddButton, View.VISIBLE);
			} else {
				mSerialID.setVisibility(View.GONE);
				serialRecyclerView.setVisibility(View.GONE);
				MetrixControlAssistant.setVisibility(serialIdColDef.labelId, mLayout, View.GONE);
				MetrixControlAssistant.setButtonVisibility(mAddButton, View.GONE);
			}

			String lotIdentified = MetrixDatabaseManager.getFieldStringValue("part", "lot_identified", "part_id='" + MetrixControlAssistant.getValue(mPartID) + "'");
			if (lotIdentified.compareToIgnoreCase("Y") == 0) {
				mLotID.setVisibility(View.VISIBLE);
				MetrixControlAssistant.setVisibility(lotIdColDef.labelId, mLayout, View.VISIBLE);
			} else {
				mLotID.setVisibility(View.GONE);
				MetrixControlAssistant.setVisibility(lotIdColDef.labelId, mLayout, View.GONE);
			}

		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private void placeFromChanged() {
		try {
			mLocationFrom.setText("");
			String reasonValue = MetrixControlAssistant.getValue(mAdjustReasonSpinner);
			String transRouteId = MetrixDatabaseManager.getFieldStringValue("adjustmnt_reasn", "trans_route_id", "adjustment_reason='" + reasonValue + "'");
			User user = (User) MetrixPublicCache.instance.getItem("MetrixUser");
			String currPlace = MetrixControlAssistant.getValue(mPlaceFrom);

			if (MetrixStringHelper.valueIsEqual(currPlace, user.stockFromPlace) && !transRouteId.contains("R")) {
				mSerialID.setOnTouchListener(this);

				mSerialID.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.magnifying_glass, 0);
			} else {
				mSerialID.setOnTouchListener(null);

				mSerialID.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private void placeToChanged() {
		try {
			mLocationTo.setText("");
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
		MetrixTableDef stockAdjustmentDef = new MetrixTableDef("stock_adjustment", MetrixTransactionTypes.INSERT);
		this.mFormDef = new MetrixFormDef(stockAdjustmentDef);

		mFrom = new String[] { "serial_id", "is_checked" };
		mTo = new int[] { R.id.item1, R.id.itemselected };
	}

	protected void defaultValues() {
		if(!this.mInitialized){

			mAddButton = (FloatingActionButton) findViewById(R.id.addSerial);
			mPartID = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "part_id");
			mSerialID = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "serial_id");
			mQuantity = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "quantity");
			mAdjustReasonSpinner = (Spinner) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "adjustment_reason");
			mPlaceFrom = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "place_id_from");
			mPlaceTo = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "place_id_to");
			mLocationFrom = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "location_from");
			mLocationTo = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "location_to");
			mLotID = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "lot_id");

			User user = (User) MetrixPublicCache.instance.getItem("MetrixUser");
			String reasonFilter = "";
			if (intentParameter != null) {
				if (intentParameter.compareToIgnoreCase(StockListMetrixActionView.ADDPART) == 0) {
					reasonFilter = "where trans_route_id like 'R%'";
				} else if (intentParameter.compareToIgnoreCase(StockListMetrixActionView.REMOVEPART) == 0) {
					reasonFilter = "where trans_route_id like 'I%'";
				} else if (intentParameter.compareToIgnoreCase(StockListMetrixActionView.SWAPPART) == 0) {
					reasonFilter = "where trans_route_id like 'T%'";
				}
			}
			MetrixControlAssistant.populateSpinnerFromQuery(this, mAdjustReasonSpinner, String.format("select distinct description, adjustment_reason from adjustmnt_reasn %s", reasonFilter), false);

			hideControls();

			try {
				MetrixControlAssistant.setValue(mQuantity, "1");
				MetrixControlAssistant.setValue(mPlaceFrom, user.stockFromPlace);
				placeFromChanged();
				MetrixControlAssistant.setValue(mLocationFrom, user.stockFromLocation);

				if (intentParameter.compareToIgnoreCase(StockListMetrixActionView.SWAPPART) != 0) {
					MetrixControlAssistant.setValue(mPlaceTo, user.stockFromPlace);
					MetrixControlAssistant.setValue(mLocationTo, user.stockFromLocation);
				}
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	/**
	 * Hide the controls before the selected reason to be changed
	 */
	private void hideControls() {
		mPlaceFrom.setVisibility(View.GONE);
		MetrixColumnDef placeIdFromColDef = mFormDef.getColumnDef("custom", "place_id_from");
		MetrixControlAssistant.setVisibility(placeIdFromColDef.labelId, mLayout, View.GONE);
		mPlaceTo.setVisibility(View.GONE);
		MetrixColumnDef placeIdToColDef = mFormDef.getColumnDef("custom", "place_id_to");
		MetrixControlAssistant.setVisibility(placeIdToColDef.labelId, mLayout, View.GONE);
		mLocationFrom.setVisibility(View.GONE);
		MetrixColumnDef locationFromColDef = mFormDef.getColumnDef("custom", "location_from");
		MetrixControlAssistant.setVisibility(locationFromColDef.labelId, mLayout, View.GONE);
		mLocationTo.setVisibility(View.GONE);
		MetrixColumnDef locationToColDef = mFormDef.getColumnDef("custom", "location_to");
		MetrixControlAssistant.setVisibility(locationToColDef.labelId, mLayout, View.GONE);
		mSerialID.setVisibility(View.GONE);
		MetrixColumnDef serialIdColDef = mFormDef.getColumnDef("custom", "serial_id");
		MetrixControlAssistant.setVisibility(serialIdColDef.labelId, mLayout, View.GONE);
		mLotID.setVisibility(View.GONE);
		MetrixColumnDef lotIdColDef = mFormDef.getColumnDef("custom", "lot_id");
		MetrixControlAssistant.setVisibility(lotIdColDef.labelId, mLayout, View.GONE);
		MetrixControlAssistant.setButtonVisibility(mAddButton, View.GONE);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		int viewId = v.getId();

		if (v.getId() == mSaveButton.getId())
		{
			if (mPartID.getText().toString().compareTo("") == 0) {
				MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("EnterPartID"));
				return;
			}

			if (mSerialID.getText().toString().length() > 0)
				addSerial();

			int partSerialed = this.partIsSerialed(mPartID.getText().toString());

			if (partSerialed == -1)
				MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("PartDoesNotExist"));
			else if (partSerialed == 1)
				this.PerformSerialedTransaction();
			else if (partSerialed == 0)
				this.PerformNonSerialedTransaction();
		}
		else if (v.getId() == mAddButton.getId())
		{
			addSerial();
			mQuantity.setText(String.valueOf(mSerialIDs.size()));
		}
		else if (v.getId() == mDeleteButton.getId())
		{
			deleteSerial();
			int currSerialCount = mSerialIDs.size();
			if (currSerialCount < 1) { currSerialCount = 1; }
			mQuantity.setText(String.valueOf(currSerialCount));
		}
		else if (v.getId() == mPartID.getId())
		{
			displayPartSelection();
		}
		else if (v.getId() == mLotID.getId())
		{
			findLotIdByPart();
		}
		else if (v.getId() == mPlaceFrom.getId() || v.getId() == mPlaceTo.getId() || v.getId() == mLocationFrom.getId() || v.getId() == mLocationTo.getId())
		{
			displayPlaceLocationSelection(viewId, true);
		}
		else
		{
			super.onClick(v);
		}
	}

	/**
	 * Add a serial ID to the list to be processed
	 */
	private void addSerial() {
		if (mSerialID.getText().length() <= 0) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("EnterSerialID"));
			return;
		}

		final Map<String, Object> serial = new HashMap<>(3);
		serial.put("serial_id", mSerialID.getText().toString());
		serial.put("is_checked", false);
		serial.put("internal_id", UUID.randomUUID().toString());
		mSerialIDs.add(serial);
		
		MetrixControlAssistant.setButtonVisibility(mDeleteButton, View.VISIBLE);
		
		bindRecyclerView();
		this.mSerialID.setText("");
	}

	/**
	 * Delete a serial from the list
	 */
	private void deleteSerial() {
		if (mSerialIDs.isEmpty())
			return;

		for (int i = mSerialIDs.size() - 1; i >=0; i--) {
			if ((Boolean)mSerialIDs.get(i).get("is_checked"))
				mSerialIDs.remove(i);
		}

		if (mSerialIDs.size() == 0) {
			MetrixControlAssistant.setButtonVisibility(mDeleteButton, View.GONE);
		}
		bindRecyclerView();
	}

	/**
	 * Bind the recyclerview for the stock list
	 */
	private void bindRecyclerView() {
		if (adapter == null) {
			final int layoutId = R.layout.stock_serial_item_multiple_select;
			adapter = new SimpleRecyclerViewAdapter(new ArrayList<>(mSerialIDs), layoutId, mFrom, mTo, new int[] {}, "internal_id");
			serialRecyclerView.setAdapter(adapter);
		} else {
			adapter.updateData(new ArrayList<>(mSerialIDs));
		}

		serialRecyclerView.removeItemDecoration(mBottomOffset);
		int offset = (int)getResources().getDimension((R.dimen.md_margin));
		if (mSerialIDs != null && !mSerialIDs.isEmpty()) {
			mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), offset);
		} else {
			mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), offset + generateOffsetForFABs(mFABList));
		}
		mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));
		serialRecyclerView.addItemDecoration(mBottomOffset);
	}

	/**
	 * @return 1: serialed part 0: Non serialed part -1: no such part
	 */
	private int partIsSerialed(String partId) {
		int partIdSerialed = 0;
		ArrayList<Hashtable<String, String>> partList = MetrixDatabaseManager.getFieldStringValuesList("part", new String[] { "part_id", "serialed" }, "part_id='" + partId.toUpperCase() + "'");

		if (partList == null || partList.size() <= 0) {
			return -1;
		} else {
			for (Hashtable<String, String> part : partList) {
				part.get("part_id");
				String serialed = part.get("serialed");

				if (serialed != null && (serialed.compareToIgnoreCase("Y") == 0))
					partIdSerialed = 1;
			}
		}

		return partIdSerialed;
	}

	private void PerformSerialedTransaction() {
		User user = (User) MetrixPublicCache.instance.getItem("MetrixUser");
		String placeFrom = "";
		String placeTo = "";
		String locationFrom = "";
		String locationTo = "";
		String lotTo = "";
		String lotIdentified = "";

		Hashtable<String, String> parameters = new Hashtable<String, String>();
		try {
			parameters.put("adjustment_reason", MetrixControlAssistant.getValue(mAdjustReasonSpinner));
			parameters.put("part_id", MetrixControlAssistant.getValue(mPartID));
			parameters.put("lot_id", MetrixControlAssistant.getValue(mLotID));

			String reasonValue = MetrixControlAssistant.getValue(mAdjustReasonSpinner);
			String transRouteId = MetrixDatabaseManager.getFieldStringValue("adjustmnt_reasn", "trans_route_id", "adjustment_reason='" + reasonValue + "'");
			lotTo = MetrixControlAssistant.getValue(mLotID);
			lotIdentified = MetrixDatabaseManager.getFieldStringValue(String.format("select lot_identified from part where part_id = '%s'", MetrixControlAssistant.getValue(mPartID)));

			if (transRouteId.contains("I")) {
				placeFrom = MetrixControlAssistant.getValue(mPlaceFrom);
				locationFrom = MetrixControlAssistant.getValue(mLocationFrom);
				if (MetrixStringHelper.isNullOrEmpty(placeFrom)||MetrixStringHelper.isNullOrEmpty(locationFrom)) {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("FillInFromPlaceFromLoc"));
					return;
				}

				parameters.put("place_id_from", placeFrom);
				parameters.put("location_from", locationFrom);
			} else if (transRouteId.contains("R")) {
				placeTo = MetrixControlAssistant.getValue(mPlaceTo);
				locationTo = MetrixControlAssistant.getValue(mLocationTo);

				if (MetrixStringHelper.isNullOrEmpty(placeTo)||MetrixStringHelper.isNullOrEmpty(locationTo)) {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("FillInPlaceToLocTo"));
					return;
				}

				parameters.put("place_id_to", MetrixControlAssistant.getValue(mPlaceTo));
				parameters.put("location_to", MetrixControlAssistant.getValue(mLocationTo));
			} else if (transRouteId.compareTo("") != 0) {
				placeFrom = MetrixControlAssistant.getValue(mPlaceFrom);
				locationFrom = MetrixControlAssistant.getValue(mLocationFrom);
				placeTo = MetrixControlAssistant.getValue(mPlaceTo);
				locationTo = MetrixControlAssistant.getValue(mLocationTo);

				if (MetrixStringHelper.isNullOrEmpty(placeFrom) || MetrixStringHelper.isNullOrEmpty(locationFrom)
						|| MetrixStringHelper.isNullOrEmpty(placeTo) || MetrixStringHelper.isNullOrEmpty(locationTo)) {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("FillInPlaceLocInfo"));
					return;
				}

				parameters.put("place_id_from", MetrixControlAssistant.getValue(mPlaceFrom));
				parameters.put("place_id_to", MetrixControlAssistant.getValue(mPlaceTo));
				parameters.put("location_from", MetrixControlAssistant.getValue(mLocationFrom));
				parameters.put("location_to", MetrixControlAssistant.getValue(mLocationTo));
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}

		JsonObject jsonMessage = new JsonObject();
		JsonObject sequential = new JsonObject();
		JsonObject perform_method = new JsonObject();
		JsonObject parameterList = new JsonObject();
		JsonObject nameValue = new JsonObject();

		for (String key : parameters.keySet()) {
			String value = parameters.get(key);
			nameValue.addProperty(key, value);
			parameterList.add("parameters", nameValue);
		}

		if (mSerialIDs.size() > 0) {
			final List<String> serials = new ArrayList<>(mSerialIDs.size());
			for (Map<String, Object> item : mSerialIDs) {
				serials.add((String) item.get("serial_id"));
			}
			JsonObject serialIDs = new JsonObject();
			JSONArray serialArray = new JSONArray(serials);
			JsonArray serialIdArray = new JsonArray();
			JsonParser parser = new JsonParser();

			serialIdArray = (JsonArray) parser.parse(serialArray.toString());
			serialIDs.add("serial_id", serialIdArray);
			nameValue.add("serial_ids", serialIDs);
			nameValue.addProperty("quantity", mSerialIDs.size());
			parameterList.add("parameters", nameValue);
		} else {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("AddSerialIDBeforeConfirm"));
			return;
		}
		if (MetrixStringHelper.isNullOrEmpty(lotTo) && lotIdentified.compareTo("Y")==0) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("SpecifyLotIdStockAdjustment"));
			return;
		}

		perform_method.add("perform_stock_adjustment", parameterList);
		sequential.add("sequential_dependent", perform_method);
		jsonMessage.add("perform_batch", sequential);

		String message_out_message = jsonMessage.toString();

		ArrayList<DataField> fields = new ArrayList<DataField>();
		fields.add(new DataField("person_id", user.personId));
		fields.add(new DataField("transaction_type", "update"));
		fields.add(new DataField("transaction_desc", "perform_stock_adjustment"));
		fields.add(new DataField("message", message_out_message));
		fields.add(new DataField("transaction_id", MetrixDatabaseManager.generateTransactionId("mm_message_out")));
		fields.add(new DataField("status", "READY"));
		fields.add(new DataField("created_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true)));
		fields.add(new DataField("modified_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true)));
		long ret_value = MetrixDatabaseManager.insertRow("mm_message_out", fields);

		if (ret_value > 0) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, StockList.class);
			MetrixActivityHelper.startNewActivityAndFinish(this, intent);
		} else {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("FailedSaveStockAdj"));
		}
	}

	private void PerformNonSerialedTransaction() {
		String placeFrom = "";
		String placeTo = "";
		String locationFrom = "";
		String locationTo = "";
		String lotTo = "";
		String lotIdentified = "";

		MetrixPerformMessage performMessage = new MetrixPerformMessage("perform_stock_adjustment");

		try {
			performMessage.parameters.put("adjustment_reason", MetrixControlAssistant.getValue(mAdjustReasonSpinner));
			performMessage.parameters.put("part_id", MetrixControlAssistant.getValue(mPartID));
			performMessage.parameters.put("lot_id", MetrixControlAssistant.getValue(mLotID));

			String reasonValue = MetrixControlAssistant.getValue(mAdjustReasonSpinner);
			String transRouteId = MetrixDatabaseManager.getFieldStringValue("adjustmnt_reasn", "trans_route_id", "adjustment_reason='" + reasonValue + "'");
			lotTo = MetrixControlAssistant.getValue(mLotID);
			lotIdentified = MetrixDatabaseManager.getFieldStringValue(String.format("select lot_identified from part where part_id = '%s'", MetrixControlAssistant.getValue(mPartID)));

			if (transRouteId.contains("I")) {
				placeFrom = MetrixControlAssistant.getValue(mPlaceFrom);
				locationFrom = MetrixControlAssistant.getValue(mLocationFrom);

				if (MetrixStringHelper.isNullOrEmpty(placeFrom)||MetrixStringHelper.isNullOrEmpty(locationFrom)) {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("FillInFromPlaceFromLoc"));
					return;
				}

				performMessage.parameters.put("place_id_from", placeFrom);
				performMessage.parameters.put("location_from", locationFrom);
			} else if (transRouteId.contains("R")) {
				placeTo = MetrixControlAssistant.getValue(mPlaceTo);
				locationTo = MetrixControlAssistant.getValue(mLocationTo);

				if (MetrixStringHelper.isNullOrEmpty(placeTo)||MetrixStringHelper.isNullOrEmpty(locationTo)) {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("FillInPlaceToLocTo"));
					return;
				}

				performMessage.parameters.put("place_id_to", MetrixControlAssistant.getValue(mPlaceTo));
				performMessage.parameters.put("location_to", MetrixControlAssistant.getValue(mLocationTo));
			} else if (transRouteId.compareTo("") != 0) {
				placeFrom = MetrixControlAssistant.getValue(mPlaceFrom);
				locationFrom = MetrixControlAssistant.getValue(mLocationFrom);
				placeTo = MetrixControlAssistant.getValue(mPlaceTo);
				locationTo = MetrixControlAssistant.getValue(mLocationTo);

				if (MetrixStringHelper.isNullOrEmpty(placeFrom) || MetrixStringHelper.isNullOrEmpty(locationFrom)
						|| MetrixStringHelper.isNullOrEmpty(placeTo) || MetrixStringHelper.isNullOrEmpty(locationTo)) {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("FillInPlaceLocInfo"));
					return;
				}

				performMessage.parameters.put("place_id_from", MetrixControlAssistant.getValue(mPlaceFrom));
				performMessage.parameters.put("place_id_to", MetrixControlAssistant.getValue(mPlaceTo));
				performMessage.parameters.put("location_from", MetrixControlAssistant.getValue(mLocationFrom));
				performMessage.parameters.put("location_to", MetrixControlAssistant.getValue(mLocationTo));
			}
			if (MetrixStringHelper.isNullOrEmpty(lotTo) && lotIdentified.compareTo("Y")==0) {
				MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("SpecifyLotIdStockAdjustment"));
				return;
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}

		performMessage.parameters.put("quantity", mQuantity.getText().toString());

		if (performMessage.save()) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, StockList.class);
			MetrixActivityHelper.startNewActivityAndFinish(this, intent);
		} else {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("FailedSaveStockAdj"));
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		final int DRAWABLE_LEFT = 0;
		final int DRAWABLE_TOP = 1;
		final int DRAWABLE_RIGHT = 2;
		final int DRAWABLE_BOTTOM = 3;

		if (event.getAction() == MotionEvent.ACTION_UP) {
			if (v.getId() == mPartID.getId()) {
				if (event.getRawX() >= (mPartID.getRight() - mPartID.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()))
					displayPartSelection();
			} else if (v.getId() == mSerialID.getId()) {
				if (event.getRawX() >= (mSerialID.getRight() - mSerialID.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()))
					findSerialIdByPart();
			} else if (v.getId() == mLotID.getId()) {
				if (event.getRawX() >= (mLotID.getRight() - mLotID.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()))
					findLotIdByPart();
			} else if (v.getId() == mPlaceFrom.getId()) {
				if (event.getRawX() >= (mPlaceFrom.getRight() - mPlaceFrom.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()))
					displayPlaceLocationSelection(mPlaceFrom.getId(), true);
			} else if (v.getId() == mLocationFrom.getId()) {
				if (event.getRawX() >= (mLocationFrom.getRight() - mLocationFrom.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()))
					displayPlaceLocationSelection(mLocationFrom.getId(), true);
			} else if (v.getId() == mPlaceTo.getId()) {
				if (event.getRawX() >= (mPlaceTo.getRight() - mPlaceTo.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()))
					displayPlaceLocationSelection(mPlaceTo.getId(), false);
			} else if (v.getId() == mLocationTo.getId()) {
				if (event.getRawX() >= (mLocationTo.getRight() - mLocationTo.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()))
					displayPlaceLocationSelection(mLocationTo.getId(), false);
			}
		}
		return false;
	}

	private void displayPartSelection() {

		try {
			MetrixLookupDef lookupDef = new MetrixLookupDef("part");
			lookupDef.columnNames.add(new MetrixLookupColumnDef("part.internal_descriptn"));
			lookupDef.columnNames.add(new MetrixLookupColumnDef("part.part_id", mPartID.getId()));
			lookupDef.columnNames.add(new MetrixLookupColumnDef("part.serialed"));
			lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA", "Part");
			lookupDef.initialSearchCriteria = MetrixControlAssistant.getValue(mPartID);

			Intent intent = MetrixActivityHelper.createActivityIntent(this, Lookup.class);
			MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
			MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);
			startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);

		}
		catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}

	private void displayPlaceLocationSelection(int viewId, boolean isFrom) {
		try {
			int placeId = isFrom ? mPlaceFrom.getId() : mPlaceTo.getId();
			int locationId = isFrom ? mLocationFrom.getId() : mLocationTo.getId();
			MetrixLookupDef lookupDef = new MetrixLookupDef("place");
			lookupDef.tableNames.add(new MetrixLookupTableDef("location", "place", "place.place_id", "location.place_id", "="));
			lookupDef.columnNames.add(new MetrixLookupColumnDef("place.name"));
			lookupDef.columnNames.add(new MetrixLookupColumnDef("place.place_id", placeId));
			lookupDef.columnNames.add(new MetrixLookupColumnDef("location.location", locationId));
			lookupDef.columnNames.add(new MetrixLookupColumnDef("location.description"));
			lookupDef.filters.add(new MetrixLookupFilterDef("place.stock_parts", "=", "Y"));
			if (isFrom)
				lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA", "Source");
			else
				lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA", "Destination");

			lookupDef.initialSearchCriteria = MetrixControlAssistant.getValue(viewId, mLayout);

			Intent intent = MetrixActivityHelper.createActivityIntent(this, Lookup.class);
			MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
			MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);
			startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);
		}
		catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}

	private void findLotIdByPart() 	{
		MetrixLookupDef lookupDef = new MetrixLookupDef();
		lookupDef.tableNames.add(new MetrixLookupTableDef("lot"));
		lookupDef.columnNames.add(new MetrixLookupColumnDef("lot.part_id"));
		lookupDef.columnNames.add(new MetrixLookupColumnDef("lot.lot_id", mLotID.getId()));
		lookupDef.filters.add(new MetrixLookupFilterDef("lot.part_id", "=", MetrixControlAssistant.getValue(mPartID.getId(), mLayout)));
		lookupDef.initialSearchCriteria = MetrixControlAssistant.getValue(mLotID.getId(), mLayout);

		lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA", "LotId");

		Intent intent = MetrixActivityHelper.createActivityIntent(this, Lookup.class);
		MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
		MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);
		startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);
	}

	private void findSerialIdByPart(){
		try {
			MetrixColumnDef serialIdColDef = mFormDef.getColumnDef("custom", "serial_id");
			String serialized = MetrixDatabaseManager.getFieldStringValue("part", "serialed", "part_id='" + MetrixControlAssistant.getValue(mPartID) + "'");
			if (intentParameter.compareToIgnoreCase(StockListMetrixActionView.ADDPART) != 0) {
				if (serialized.compareToIgnoreCase("Y") == 0) {
					MetrixControlAssistant.setVisibility(serialIdColDef.labelId, mLayout, View.VISIBLE);
					MetrixControlAssistant.setButtonVisibility(mAddButton, View.VISIBLE);

					String reasonValue = MetrixControlAssistant.getValue(mAdjustReasonSpinner);
					String transRouteId = MetrixDatabaseManager.getFieldStringValue("adjustmnt_reasn", "trans_route_id", "adjustment_reason='" + reasonValue + "'");

					MetrixLookupDef lookupDef = new MetrixLookupDef("stock_serial_id", true);
					lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_serial_id.serial_id", mSerialID.getId()));
					lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_serial_id.part_id"));
					lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_serial_id.lot_id"));
					lookupDef.title = AndroidResourceHelper.createLookupTitle("LookupHelpA","SerialId");
					lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.part_id", "=", MetrixControlAssistant.getValue(mPartID)));

					if (MetrixStringHelper.valueIsEqual(transRouteId, "I2") || MetrixStringHelper.valueIsEqual(transRouteId, "T3") || MetrixStringHelper.valueIsEqual(transRouteId, "T4"))
						lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.usable", "=", "N"));
					else
						lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.usable", "=", "Y"));

					Intent intent = MetrixActivityHelper.createActivityIntent(this,	Lookup.class);
					MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
					MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);
					startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);
				} else {
					mSerialID.setVisibility(View.GONE);
					MetrixControlAssistant.setVisibility(serialIdColDef.labelId, mLayout, View.GONE);
					MetrixControlAssistant.setButtonVisibility(mAddButton, View.GONE);
				}
			} else {
				if (serialized.compareToIgnoreCase("Y") == 0) {
					mSerialID.setVisibility(View.VISIBLE);
					MetrixControlAssistant.setVisibility(serialIdColDef.labelId, mLayout, View.VISIBLE);
					MetrixControlAssistant.setButtonVisibility(mAddButton, View.VISIBLE);
				} else {
					mSerialID.setVisibility(View.GONE);
					MetrixControlAssistant.setVisibility(serialIdColDef.labelId, mLayout, View.GONE);
					MetrixControlAssistant.setButtonVisibility(mAddButton, View.GONE);
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	private class PartTextWatcher implements TextWatcher {
		public PartTextWatcher() {
		}

		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before, int count) {
			StockAdjustment.this.partChanged();
		}

		public void afterTextChanged(Editable s) {
		}
	}

	private class PlaceFromTextWatcher implements TextWatcher {
		public PlaceFromTextWatcher() {
		}

		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before, int count) {
			StockAdjustment.this.placeFromChanged();
		}

		public void afterTextChanged(Editable s) {
		}
	}

	private class PlaceToTextWatcher implements TextWatcher {
		public PlaceToTextWatcher() {
		}

		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before, int count) {
			StockAdjustment.this.placeToChanged();
		}

		public void afterTextChanged(Editable s) {
		}
	}
}