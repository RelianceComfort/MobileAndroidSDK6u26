package com.metrix.metrixmobile.system;

import android.app.Activity;
import android.os.Bundle;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.metadata.MetrixTableStructure;
import com.metrix.architecture.ui.widget.CustomSpinner;
import com.metrix.architecture.ui.widget.SimpleRecyclerViewAdapter;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @author rawilk
 *
 */
public class Query extends MetrixActivity implements View.OnClickListener,
		OnItemSelectedListener, TextWatcher {

	private Button mTableSearchButton, mTableQuerySearchButton, mImportButton,
			mExportButton;
	private SimpleRecyclerViewAdapter mSimpleAdapter;
	private String[] mFrom;
	private int[] mTo;
	private RecyclerView recyclerView;
	private CustomSpinner mSpinnerSearch;
	private HashMap<String, MetrixTableStructure> hashMapTableDetails;
	private EditText criteria;
	private boolean disableSpinnerInitialSelection, spinnerManuallySelected;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.query);
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		mLayout.requestFocus();

		recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
		criteria = (EditText) findViewById(R.id.search_criteria);
		criteria.clearFocus();

		if (criteria != null)
			criteria.append("select");

		mSpinnerSearch = (CustomSpinner) findViewById(R.id.querySpinnerSearch);
		hashMapTableDetails = MobileApplication.getTableDefinitionsFromCache();

		populateTableSpinner(mSpinnerSearch, hashMapTableDetails);
		spinnerManuallySelected = disableSpinnerInitialSelection = true;

		float scale = getResources().getDisplayMetrics().density;
		float btnCornerRadius = 4f * scale + 0.5f;
		setSkinBasedColorsOnRelevantControls(mLayout, "#360065", "#8427E2", "#4169e1", mLayout, true);
		setSkinBasedColorsOnButtons(((ViewGroup) findViewById(android.R.id.content)).getChildAt(0), "873E8D", "360065", "#FFFFFF", btnCornerRadius, this);
	}


	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.queryTextViewSearch, "QueryDisplay"));
		resourceStrings.add(new ResourceValueObject(R.id.searchTable, "Search"));
		resourceStrings.add(new ResourceValueObject(R.id.textView1, "QuickSearch"));
		resourceStrings.add(new ResourceValueObject(R.id.search, "Search"));
		resourceStrings.add(new ResourceValueObject(R.id.dbImport, "DBImport"));
		resourceStrings.add(new ResourceValueObject(R.id.dbExport, "DBExport"));

		super.onStart();
	}
	/**
	 * load all table names to Spinner
	 *
	 * @param spinner
	 * @param hashMap
	 */
	private void populateTableSpinner(Spinner spinner,
									  HashMap<String, MetrixTableStructure> hashMap) {
		ArrayList<String> arrayListTableNames = new ArrayList<String>(
				hashMapTableDetails.size());
		arrayListTableNames.add("");

		for (String tableName : hashMapTableDetails.keySet()) {
			arrayListTableNames.add(tableName);
		}
		// sorting the list alphabetically...
		Collections.sort(arrayListTableNames);
		MetrixControlAssistant.populateSpinnerFromList(this, spinner,
				arrayListTableNames);
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		mTableSearchButton = (Button) findViewById(R.id.searchTable);
		mTableSearchButton.setOnClickListener(this);

		mTableQuerySearchButton = (Button) findViewById(R.id.search);
		mTableQuerySearchButton.setOnClickListener(this);

		mImportButton = (Button) findViewById(R.id.dbImport);
		boolean isDemoBuild = MetrixApplicationAssistant.getMetaBooleanValue(this, "DemoBuild");
		if (!isDemoBuild) {
			mImportButton.setVisibility(View.GONE);
		}
		else
			mImportButton.setOnClickListener(this);

		mExportButton = (Button) findViewById(R.id.dbExport);
		mExportButton.setOnClickListener(this);

		mSpinnerSearch.setOnItemSelectedListener(this);
		criteria.addTextChangedListener(this);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.searchTable:
				clearAnyPreviousResult(recyclerView);
				performTablePKQuery();
				break;
			case R.id.search:
				clearAnyPreviousResult(recyclerView);
				performDetailQuery();
				break;
			case R.id.dbImport:
				boolean isDemoBuild = MetrixApplicationAssistant.getMetaBooleanValue(this, "DemoBuild");

				if (isDemoBuild)
					MetrixDatabaseManager.performDatabaseImport(R.raw.metrix_demo,
							"metrix_demo_import.db", this.getApplicationContext(), true,
							com.metrix.metrixmobile.R.array.system_tables,
							com.metrix.metrixmobile.R.array.business_tables);
				else
					MetrixUIHelper.showSnackbar(this, AndroidResourceHelper.getMessage("ImportFunctionOnlyForDemo"));
				break;
			case R.id.dbExport:
				String personId = "";
				int sequence = 0;
				try {
					personId = User.getUser().personId;
					sequence = SettingsHelper.getDeviceSequence(this);

					if(!MetrixStringHelper.isNullOrEmpty(personId) && sequence !=0)
						MetrixDatabaseManager.performDatabaseExport(
								personId+"__"+sequence+"__export.db", this.getApplicationContext(), true,
								com.metrix.metrixmobile.R.array.system_tables,
								com.metrix.metrixmobile.R.array.business_tables);
					else
						MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("DBExportError"));
				}catch(Exception ex){
					LogManager.getInstance().error(ex);
				}

				break;
			default:
				super.onClick(v);
		}
	}

	/**
	 * load only PK values for the selected table
	 */
	private void performTablePKQuery() {

		String query = null;
		String spinnerSelectedValue = MetrixControlAssistant.getValue(
				R.id.querySpinnerSearch, mLayout);
		if (spinnerSelectedValue == null || spinnerSelectedValue == "") {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout,
					AndroidResourceHelper.getMessage("QueryPleaseSelectTable"));
		} else {
			try {
				query = getSearchQuery(spinnerSelectedValue);
				loadingResultData(query);
			} catch (Exception e) {
				MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, e.getMessage());
			}
		}
	}

	/**
	 * load data for the user entered query
	 */
	private void performDetailQuery() {

		String query = null;

		query = MetrixControlAssistant.getValue(R.id.search_criteria, mLayout);
		loadingResultData(query);
	}

	/**
	 * loading ListView with data
	 *
	 * @param query
	 */
	private void loadingResultData(String query) {
		if (MetrixStringHelper.isNullOrEmpty(query)) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout,
					AndroidResourceHelper.getMessage("QueryProvideSqlStatement"));
			return;
		}

		if (!query.startsWith("select") || !query.contains("from")) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout,
					AndroidResourceHelper.getMessage("QueryProvideSqlStatement"));
			return;
		}

		String columnBlock = "";
		String[] columns = {};
		try {
			columnBlock = query.substring(7, query.indexOf("from"));
			columns = columnBlock.trim().split(",");
		}
		catch(Exception ex){
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("InvalidSQL"));
			return;
		}

		mFrom = new String[columns.length];
		int[] hiddenViews;
		for (int i = 0; i < columns.length; i++) {
			mFrom[i] = columns[i];
		}

		if (mFrom.length == 1) {
			mTo = new int[] { R.id.itemdescription };
			hiddenViews = new int[] { R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5,
					R.id.item6, R.id.item7, R.id.item8, R.id.item9, R.id.item10 };
		} else if (mFrom.length == 2) {
			mTo = new int[] { R.id.itemdescription, R.id.item1 };
			hiddenViews = new int[] { R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6,
					R.id.item7, R.id.item8, R.id.item9, R.id.item10 };
		} else if (mFrom.length == 3) {
			mTo = new int[] { R.id.itemdescription, R.id.item1, R.id.item2 };
			hiddenViews = new int[] { R.id.item3, R.id.item4, R.id.item5, R.id.item6, R.id.item7,
					R.id.item8, R.id.item9, R.id.item10 };
		} else if (mFrom.length == 4) {
			mTo = new int[] { R.id.itemdescription, R.id.item1, R.id.item2,
					R.id.item3 };
			hiddenViews = new int[] { R.id.item4, R.id.item5, R.id.item6, R.id.item7, R.id.item8,
					R.id.item9, R.id.item10 };
		} else {
			mTo = new int[] { R.id.itemdescription, R.id.item1, R.id.item2,
					R.id.item3, R.id.item4 };
			hiddenViews = new int[] { R.id.item5, R.id.item6, R.id.item7, R.id.item8, R.id.item9,
					R.id.item10 };
		}

		MetrixCursor cursor = null;
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);

			if (cursor == null || !cursor.moveToFirst()) {
				MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout,
						AndroidResourceHelper.getMessage("QueryNoRowsFound"));
				return;
			}

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();

				for (int i = 0; i < mFrom.length; i++) {
					row.put(mFrom[i], cursor.getString(i));
				}

				table.add(row);
				cursor.moveToNext();
			}
		} catch (Exception e) {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		// fill in the grid_item layout
		mSimpleAdapter = new SimpleRecyclerViewAdapter(table,
				R.layout.lookup_item_query, mFrom, mTo, hiddenViews, null);
		recyclerView.setAdapter(mSimpleAdapter);
	}

	/**
	 *
	 * @param spinnerSelectedValue
	 * @return generated query string with only PK's of the selected table
	 * @throws Exception
	 */
	private String getSearchQuery(String spinnerSelectedValue) throws Exception {

		String formattedColumnNames = null;
		Set<String> primaryKeys = null;
		int count = 1;

		MetrixTableStructure metrixTableStructure = hashMapTableDetails
				.get(spinnerSelectedValue);
		if (metrixTableStructure.mMetrixPrimaryKeys.keyInfo == null) {
			throw new Exception(
					AndroidResourceHelper.getMessage("QueryNoPrimaryKeysFormat",
					spinnerSelectedValue));
		}
		primaryKeys = metrixTableStructure.mMetrixPrimaryKeys.keyInfo.keySet();

		if (primaryKeys != null && primaryKeys.size() > 0) {

			StringBuilder sbQueryBuilder = new StringBuilder();
			if (primaryKeys.size() == 1)
				sbQueryBuilder.append(primaryKeys.iterator().next());
			else {
				for (String primaryKey : primaryKeys) {
					sbQueryBuilder.append(primaryKey);
					if (count == primaryKeys.size())
						break;
					sbQueryBuilder.append(",");
					count++;
				}
			}
			formattedColumnNames = sbQueryBuilder.toString();
		}


		return String.format("select %1$s from %2$s", formattedColumnNames, spinnerSelectedValue);
	}

	/**
	 * clear any previous result in ListView
	 *
	 * @param recyclerView
	 */
	private void clearAnyPreviousResult(RecyclerView recyclerView) {
		if (recyclerView != null)
			recyclerView.setAdapter(null);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
							   long id) {

		if (disableSpinnerInitialSelection) {
			disableSpinnerInitialSelection = false;
		} else {
			if (spinnerManuallySelected) {
				criteria.removeTextChangedListener(this);
				criteria.setText("select");
				criteria.addTextChangedListener(this);
				ExplicitlyHideKeyBoard();
				mLayout.requestFocus();

			} else
				criteria.requestFocus();

			spinnerManuallySelected = true;
		}

	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {}

	@Override
	public void afterTextChanged(Editable s) {

		spinnerManuallySelected = false;
		// stop re-setting the mSpinnerSearch if it's already set to 0th
		// position
		if (mSpinnerSearch.getSelectedItemPosition() == 0)
			spinnerManuallySelected = true;
		else
			mSpinnerSearch.setSelection(0);

	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	private void ExplicitlyHideKeyBoard() {
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (inputMethodManager == null)
			return;
		inputMethodManager.toggleSoftInput(
				InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
	}

}
