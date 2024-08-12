package com.metrix.metrixmobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.FilterSortItem;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixFilterSortManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.metrixmobile.system.MetrixActivity;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ReceivingList extends MetrixActivity implements MetrixRecyclerViewListener {
	private RecyclerView recyclerView;
	private MetadataRecyclerViewAdapter mAdapter;
	private TextView mFilterName, mSortName;
	private ImageView mSortIcon;
	private String mLastFilterName, mLastSortName, mLastSortOrder;
	private int mScreenId, mFirstGradientTextColor;
	private boolean mIsSortAscending;
	private ArrayList<String> mSortOptions;
	private ArrayList<String> mFilterOptions;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.receiving_list);
		mScreenId = MetrixScreenManager.getScreenId(this);

		String firstGradientTextColor = MetrixSkinManager.getFirstGradientTextColor();
		if (MetrixStringHelper.isNullOrEmpty(firstGradientTextColor))
			firstGradientTextColor = "#FFFFFF";
		mFirstGradientTextColor = Color.parseColor(firstGradientTextColor);

		recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
	}

	public void onStart() {
		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		View filterBar = findViewById(R.id.filter_bar);
        MetrixSkinManager.setFirstGradientBackground(filterBar,0);
		mFilterName = (TextView) findViewById(R.id.filter_bar__filter_name);
		mSortName = (TextView) findViewById(R.id.filter_bar__sort_name);
		mSortIcon = (ImageView) findViewById(R.id.filter_bar__sort_icon);

		mLastFilterName = SettingsHelper.getStringSetting(this, MetrixFilterSortManager.getSelectedFilterItemSettingName("ReceivingList"));
		if (MetrixStringHelper.isNullOrEmpty(mLastFilterName))
			mLastFilterName = MetrixFilterSortManager.getDefaultFilterItemLabel(mScreenId);

		mLastSortName = SettingsHelper.getStringSetting(this, MetrixFilterSortManager.getSelectedSortItemSettingName("ReceivingList"));
		if (MetrixStringHelper.isNullOrEmpty(mLastSortName))
			mLastSortName = MetrixFilterSortManager.getDefaultSortItemLabel(mScreenId);

		mLastSortOrder = SettingsHelper.getStringSetting(this, MetrixFilterSortManager.getSortOrderSettingName("ReceivingList"));
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
		if (!hasFilters && !hasSorts)
			filterBar.setVisibility(View.GONE);		// hide the filter/sort bar entirely
		else {
			filterBar.setVisibility(View.VISIBLE);
			if (hasFilters)
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
	}

	private void populateList() {
		StringBuilder query =  new StringBuilder();
		String shipQuery = MetrixListScreenManager.generateListQuery(this, "receiving", "");
		query.append(shipQuery);

		String whereClause = " where receiving.inventory_adjusted = 'N' and (receiving.receiving_type = 'SHIP' or receiving.receiving_type = 'PO')";
		if (!MetrixStringHelper.isNullOrEmpty(mLastFilterName)) {
			FilterSortItem filterItem = MetrixFilterSortManager.getFilterSortItemByLabel(mLastFilterName, MetrixFilterSortManager.getFilterItems(mScreenId));
			if (filterItem != null && !MetrixStringHelper.isNullOrEmpty(filterItem.Content)) {
				String actualContent = MetrixFilterSortManager.resolveFilterSortContent(this, filterItem.Content);
				if (filterItem.FullFilter)
					whereClause = " where " + actualContent;
				else
					whereClause = whereClause + " and " + actualContent;
			}
		}
		query.append(whereClause);

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
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);
			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);
					table.add(row);
					cursor.moveToNext();
				}
				table = MetrixListScreenManager.performScriptListPopulation(this, table);
			} else {
				MetrixUIHelper.showSnackbar(ReceivingList.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("NoDataForSelectedFilter"));
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		if (mAdapter == null) {
			mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
					R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, "receiving.metrix_row_id", this);
			recyclerView.setAdapter(mAdapter);
		} else {
			mAdapter.updateData(table);
		}

		setFilterBar(table.size());
	}

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
					mLastFilterName = mFilterOptions.get(pos);
					populateList();
					SettingsHelper.saveStringSetting(ReceivingList.this, MetrixFilterSortManager.getSelectedFilterItemSettingName("ReceivingList"), mLastFilterName, true);
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
					SettingsHelper.saveStringSetting(ReceivingList.this, MetrixFilterSortManager.getSelectedSortItemSettingName("ReceivingList"), mLastSortName, true);
					SettingsHelper.saveStringSetting(ReceivingList.this, MetrixFilterSortManager.getSortOrderSettingName("ReceivingList"), mLastSortOrder, true);
				}
			});

			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		if(scriptEventConsumesListTap(this, view, mScreenId)) return;

		String receivingId = listItemData.get("receiving.receiving_id");
		MetrixCurrentKeysHelper.setKeyValue("receiving", "receiving_id", receivingId);

		String receivingType = listItemData.get("receiving.receiving_type");
		Class nextClass = ReceivingUnitList.class;
		if (MetrixStringHelper.valueIsEqual(receivingType, "PO"))
			nextClass = ReceivingDetailList.class;

		Intent intent = MetrixActivityHelper.createActivityIntent(this, nextClass);
		intent.putExtra("receiving_id", receivingId);

		MetrixActivityHelper.startNewActivity(this, intent);
	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

	}
}
