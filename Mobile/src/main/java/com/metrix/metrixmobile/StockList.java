package com.metrix.metrixmobile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataHeadingsRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixActionView;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAutoCompleteHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.system.MetrixActivity;

public class StockList extends MetrixActivity implements MetrixRecyclerViewListener, TextWatcher {
	private static final int STOCK_SEARCH_LOCATION_PERMISSION_REQUEST = 1224;
	private RecyclerView recyclerView;
	private MetadataHeadingsRecyclerViewAdapter mAdapter;
	private static int mSelectedPosition = -1;
	private int incompletePermissionRequest = 0;
	private boolean shouldShowStockSearch = false;
	private AutoCompleteTextView mSearchCriteria;
	private Handler mHandler;
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stock_list);
		mHandler = new Handler();
		recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.search_criteria, "Search", true));
		resourceStrings.add(new ResourceValueObject(R.id.row_count, "RowCount"));
		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);

		populateList();
		setListeners();

		MetrixAutoCompleteHelper.populateAutoCompleteTextView(StockList.class.getName(), mSearchCriteria, this);
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		mSearchCriteria = (AutoCompleteTextView) findViewById(R.id.search_criteria);
		mSearchCriteria.addTextChangedListener(this);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				MetrixActivityHelper.hideKeyboard(StockList.this);
			}

			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
			}
		});

		initMetrixActionView(getMetrixActionBar().getCustomView());
	}

	@Override
	public boolean OnCreateMetrixActionView(View view, Integer... position) {
		if(position.length > 0)
			mSelectedPosition = position[0];
		
		MetrixActionView metrixActionView = getMetrixActionView();
		Menu menu = metrixActionView.getMenu();
		StockListMetrixActionView.onCreateMetrixActionView(menu);
		return super.OnCreateMetrixActionView(view);
	}
	
	@Override
	public boolean onMetrixActionViewItemClick(MenuItem menuItem) {
		
		if ((menuItem.getTitle().toString().compareToIgnoreCase(StockListMetrixActionView.REMOVEPART) == 0) || (menuItem.getTitle().toString().compareToIgnoreCase(StockListMetrixActionView.SWAPPART) == 0)) {
			Object listItem = mAdapter.getListData().get(mSelectedPosition);
			@SuppressWarnings("unchecked")
			HashMap<String, String> selectedItem = (HashMap<String, String>) listItem;

			String partId = selectedItem.get("stock_bin.part_id");

			Intent intent = MetrixActivityHelper.createActivityIntent(this, StockAdjustment.class);
			intent.putExtra("action", menuItem.getTitle().toString());
			intent.putExtra("part_id", partId);
			MetrixActivityHelper.startNewActivity(this, intent);
		} else if ((menuItem.getTitle().toString().compareToIgnoreCase(StockListMetrixActionView.ADDPART) == 0)) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, StockAdjustment.class);
			intent.putExtra("action", menuItem.getTitle().toString());
			intent.putExtra("part_id", "");
			MetrixActivityHelper.startNewActivity(this, intent);
		} else if (menuItem.getTitle().toString().compareToIgnoreCase(StockListMetrixActionView.SEARCHPART) == 0) {
			if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
					ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				StockSearchEntry searchPart = new StockSearchEntry(this);
				searchPart.initDialog();
			} else {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
						STOCK_SEARCH_LOCATION_PERMISSION_REQUEST);
			}
		}
		else if (menuItem.getTitle().toString().compareToIgnoreCase(StockListMetrixActionView.CREATESHIPMENT) == 0) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, ShipmentCreate.class);
			Object listItem = mAdapter.getListData().get(mSelectedPosition);
			HashMap<String, String> selectedItem = (HashMap<String, String>) listItem;
			String partId = selectedItem.get("stock_bin.part_id");
			String usable = selectedItem.get("stock_bin.usable");
			MetrixCurrentKeysHelper.setKeyValue("stock_bin", "part_id", partId);
			MetrixCurrentKeysHelper.setKeyValue("stock_bin", "usable", usable);
			MetrixActivityHelper.startNewActivity(this, intent);
		}

		return super.onMetrixActionViewItemClick(menuItem);
	}

	/**
	 * Populate the job list with the tasks assigned to the user.
	 */
	private void populateList() {
		String searchCriteria = MetrixControlAssistant.getValue(R.id.search_criteria, mLayout);
		String whereClause = " where stock_bin.qty_on_hand > 0";
		String query = MetrixListScreenManager.generateListQuery(this, "stock_bin", whereClause, searchCriteria);

		String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
		if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
			query = query + " limit " + maxRows;
		}

		MetrixCursor cursor = null;
		int rowCount = 0;
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);

			if (cursor != null && cursor.moveToFirst()) {

				rowCount = cursor.getCount();

				while (cursor.isAfterLast() == false) {
					HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);
					
					table.add(row);
					cursor.moveToNext();
				}

				table = MetrixListScreenManager.performScriptListPopulation(this, table);
			}
		} finally {
			MetrixUIHelper.displayListRowCount(this, (TextView) findViewById(R.id.row_count), rowCount);
			if (cursor != null) {
				cursor.close();
			}
		}

		if (mAdapter == null) {
			mAdapter = new MetadataHeadingsRecyclerViewAdapter(this, table, R.layout.list_item_basic,
					R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold,
					MetrixListScreenManager.getFirstDisplayedFieldName(this), R.layout.list_item_seperator,
					R.id.list_item_seperator__header, R.id.sliver, "stock_bin.metrix_row_id", this);
			recyclerView.setAdapter(mAdapter);
		} else {
			mAdapter.updateData(table);
		}
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		mHandler.removeCallbacksAndMessages(null);
		mHandler.postDelayed(() -> {
			if (!StockList.this.isDestroyed())
				this.populateList();
		}, 500);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

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
					if (ActivityCompat.shouldShowRequestPermissionRationale(StockList.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
						// can request permission again
						ActivityCompat.requestPermissions(StockList.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
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
							MetrixUIHelper.showSnackbar(StockList.this, R.id.coordinator_layout, AndroidResourceHelper
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

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		if(scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;
		MetrixAutoCompleteHelper.saveAutoCompleteFilter(StockList.class.getName(), mSearchCriteria);

		String partId = listItemData.get("stock_bin.part_id");
		String placeId = listItemData.get("stock_bin.place_id");
		String location = listItemData.get("stock_bin.location");
		String usable = listItemData.get("stock_bin.usable");

		MetrixCurrentKeysHelper.setKeyValue("stock_bin", "part_id", partId);
		MetrixCurrentKeysHelper.setKeyValue("stock_bin", "place_id", placeId);
		MetrixCurrentKeysHelper.setKeyValue("stock_bin", "location", location);
		MetrixCurrentKeysHelper.setKeyValue("stock_bin", "usable", usable);

		Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "MetadataTabActivity");
		intent.putExtra("ScreenID", MetrixScreenManager.getScreenId("StockTabs"));
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		mSelectedPosition = position;
		if (onCreateMetrixActionViewListner != null)
			onCreateMetrixActionViewListner.OnCreateMetrixActionView(view, position);
	}
}
