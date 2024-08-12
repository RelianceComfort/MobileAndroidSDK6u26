package com.metrix.metrixmobile.system;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixBarcodeAssistant;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixLookupFormat;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixLookupFilterDef;
import com.metrix.architecture.metadata.MetrixOrderByDef;
import com.metrix.architecture.slidingmenu.MetrixSlidingMenuManager;
import com.metrix.architecture.superclasses.MetrixFormat;
import com.metrix.architecture.ui.widget.SimpleRecyclerViewAdapter;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActionView;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAutoCompleteHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.OnCreateMetrixActionViewListner;
import com.metrix.architecture.utilities.OnMetrixActionViewItemClickListner;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.R;

import java.text.DateFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by RaWiLK on 6/21/2016.
 */
public class LookupBase extends MetrixActivity implements View.OnClickListener, SimpleRecyclerViewAdapter.OnItemClickListener, TextWatcher, OnMetrixActionViewItemClickListner, OnCreateMetrixActionViewListner {
    protected MetrixLookupDef mLookupDef;
    protected ViewGroup mParentLayout;
    protected RecyclerView recyclerView;
    protected SimpleRecyclerViewAdapter simpleAdapter;
    private String[] mFrom;
    private int[] mTo;
    protected TextView mTitle;
    protected AutoCompleteTextView mSearchCriteria;
    private FloatingActionButton mAdd;
    private MetrixActionView mMetrixActionView;
    public boolean mIsFromDesigner = false;
    protected boolean mShouldShowChkbox;
    protected List<HashMap<String, String>> mTable;
    private Handler mHandler;
    private BottomOffsetDecoration mBottomOffset;
    private List<FloatingActionButton> mFABList;

    /*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        toggleOrientationLock(true);

        // if we are hiding the options menu, treat as if we are from the Designer
        if (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("NoOptionsMenu")
                && this.getIntent().getExtras().getBoolean("NoOptionsMenu")) {
            mIsFromDesigner = true;
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.metrix.metrixmobile.MetrixActivity#onStart()
     */
    public void onStart() {
        resourceStrings.add(new ResourceValueObject(R.id.search_criteria, "Search", true));

        mLayout = (ViewGroup) findViewById(R.id.table_layout);
        mParentLayout = (ViewGroup) MetrixPublicCache.instance.getItem("lookupParentLayout");
        mAdd = (FloatingActionButton) findViewById(R.id.add);

        super.onStart();

        mLookupDef = (MetrixLookupDef) MetrixPublicCache.instance.getItem("lookupDef");
        if (mLookupDef == null) {
            LogManager.getInstance().info("No MetrixLookupDef provided at Lookup.onCreate.");
        } else if (mLookupDef.performInitialSearch) {
            performQuery();
        }

        mTitle = (TextView) findViewById(R.id.action_bar_title);
        if (mTitle != null) {
            if (mLookupDef != null && !MetrixStringHelper.isNullOrEmpty(mLookupDef.title)) {
                mTitle.setText(mLookupDef.title);
            } else {
                mTitle.setText(AndroidResourceHelper.createLookupTitle("LookupHelpAn", "Item"));
            }
        }

        wireupAutoCompleteTextViewForBarcodeScanning(mLayout);

        this.helpText = AndroidResourceHelper.getMessage("ScreenDescriptionLookup");
        try {
            MetrixAutoCompleteHelper.populateAutoCompleteTextView(Lookup.class.getName() + "-" + MetrixControlAssistant.getValue(mTitle), mSearchCriteria, this);
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }

        try {
            if (mLookupDef != null && (!MetrixStringHelper.isNullOrEmpty(mLookupDef.initialSearchCriteria)) &&
                    (MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue(mSearchCriteria)))) {
                MetrixControlAssistant.setValue(mSearchCriteria, mLookupDef.initialSearchCriteria);
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }

        // Reset Lookup to use default coloring/icons, if from Designer
        if (mIsFromDesigner) {
            int basicWhiteColor = Color.parseColor("#FFFFFF");
            MetrixSkinManager.setFirstGradientBackground(mSupportActionBar, 0, true);
            View actionBar = this.findViewById(R.id.action_bar);
            if (actionBar != null)
                MetrixSkinManager.setFirstGradientBackground(actionBar, 0, true);
            mTitle.setTextColor(basicWhiteColor);
            ViewGroup mTitleParent = (ViewGroup)mTitle.getParent();
            if(mTitleParent != null)
                MetrixSkinManager.setFirstGradientBackground(mTitleParent, 0, true);
            MetrixSkinManager.setFirstGradientBackground(findViewById(R.id.row_count_bar), 0, true);
            TextView rowCountText = (TextView)findViewById(R.id.row_count);
            if (rowCountText != null)
                rowCountText.setTextColor(basicWhiteColor);
//            MetrixActionBarManager.getInstance().setActionBarDefaultIcon(R.drawable.ifs_logo, getMetrixActionBar(), 24, 24);
            if (mDrawerLinearLayout != null && mDrawerLayout != null) {
                if (mSupportActionBar != null) {
                    mDrawerToggle = MetrixSlidingMenuManager.getInstance().setUpSlidingDrawer(this, mSupportActionBar, mDrawerLinearLayout, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close, "#FFFFFF", R.drawable.ellipsis_vertical);
                }
            }
            float scale = getResources().getDisplayMetrics().density;
            float btnCornerRadius = 4f * scale + 0.5f;
            setSkinBasedColorsOnRelevantControls(mLayout, "#360065", "#8427E2", "#4169e1", mLayout, true);
            setSkinBasedColorsOnButtons(((ViewGroup)findViewById(android.R.id.content)).getChildAt(0), "873E8D", "360065", "#FFFFFF", btnCornerRadius, this);
        }
    }

    private OnCreateMetrixActionViewListner onCreateMetrixActionViewListner;
    protected void registerForMetrixActionView(final View view, View anchorView){
        mMetrixActionView = new MetrixActionView(this, anchorView);
        mMetrixActionView.setOnMetrixActionViewItemClickListner(this);

        if (view instanceof AutoCompleteTextView){
            view.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    if(onCreateMetrixActionViewListner != null){
                        onCreateMetrixActionViewListner.OnCreateMetrixActionView(view);
                    }
                    return true;
                }
            });
        }
    }

    protected void setOnCreateMetrixActionViewListner(OnCreateMetrixActionViewListner onCreateMetrixActionViewListner){
        this.onCreateMetrixActionViewListner = onCreateMetrixActionViewListner;
    }

    protected MetrixActionView getMetrixActionView() {
        return mMetrixActionView;
    }

    @Override
    public boolean onMetrixActionViewItemClick(MenuItem menuItem) {
        if (menuItem.getTitle().toString().contains(AndroidResourceHelper.getMessage("ScanBarcode"))) {
            MetrixBarcodeAssistant.scanBarcode(mLayout.findViewById(menuItem.getItemId()));
        }
        return true;
    }

    @Override
    public boolean OnCreateMetrixActionView(View view, Integer... position) {
        Menu menu = mMetrixActionView.getMenu();
        if (view instanceof AutoCompleteTextView) {
            menu.clear();
            menu.add(0, view.getId(), 0, AndroidResourceHelper.getMessage("ScanBarcode"));
            MetrixPublicCache.instance.addItem("METRIX_VIEW_DISPLAYING_CONTEXT_MENU", view.getId());
        }
        if (menu.hasVisibleItems()) {
            mMetrixActionView.show();
            return true;
        }
        return false;
    }

    private void wireupAutoCompleteTextViewForBarcodeScanning(ViewGroup viewGroup) {
        if (viewGroup == null) {
            return;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
            return;
        }

        String barcodingEnabled = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_BARCODE_SCANNING'");
        if (!MetrixStringHelper.isNullOrEmpty(barcodingEnabled) && barcodingEnabled.compareToIgnoreCase("Y") == 0) {
            int childCount = viewGroup.getChildCount();

            for (int i = 0; i < childCount; i++) {
                View view = viewGroup.getChildAt(i);

                if (view instanceof AutoCompleteTextView) {
                    registerForMetrixActionView(view, getMetrixActionBar().getCustomView());
                } else if (view instanceof ViewGroup) {
                    wireupAutoCompleteTextViewForBarcodeScanning((ViewGroup) view);
                }
            }
        }
    }

    /**
     * Define the listeners for this activity.
     */
    protected void setListeners() {
        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        mSearchCriteria = (AutoCompleteTextView) findViewById(R.id.search_criteria);
        mSearchCriteria.addTextChangedListener(this);

        mAdd.setOnClickListener(this);
        mFABList.add(mAdd);

        mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));

        fabRunnable = this::showFABs;

        if(recyclerView != null) {
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    MetrixActivityHelper.hideKeyboard(LookupBase.this);
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dy > 0 || dy < 0) {
                        fabHandler.removeCallbacks(fabRunnable);
                        hideFABs(mFABList);
                        fabHandler.postDelayed(fabRunnable, fabDelay);
                    }
                }
            });
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search:
                performQuery();
                break;
            case R.id.add:
                try {
                    MetrixAutoCompleteHelper.saveAutoCompleteFilter(Lookup.class.getName() + "-" + MetrixControlAssistant.getValue(mTitle), mSearchCriteria);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

                String value = mSearchCriteria.getText().toString();
                try {
                    MetrixControlAssistant.setValue(mLookupDef.columnNames.get(0).controlId, mParentLayout, value);
                } catch (Exception e) {
                    LogManager.getInstance(this).error(e);
                }


                setResult(RESULT_OK, getIntent());
                this.onBackPressed();
                break;
            default:
                super.onClick(v);
        }
    }

    /**
     * perform query and show data on recyclerView
     */
    private void performQuery() {
        if (mLookupDef == null) {
            LogManager.getInstance().info("No MetrixLookupDef provided at Lookup.performQuery.");
        } else {
            StringBuilder sql = new StringBuilder();
            sql.append("select ");

            mFrom = new String[mLookupDef.columnNames.size()];
            for (int i = 0; i < mLookupDef.columnNames.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(mLookupDef.columnNames.get(i).columnName);
                mFrom[i] = mLookupDef.columnNames.get(i).columnName;
            }

            sql.append(" from ");
            for (int i = 0; i < mLookupDef.tableNames.size(); i++) {
                if (MetrixStringHelper.isNullOrEmpty(mLookupDef.tableNames.get(i).parentTableName)) {
                    if (i > 0) {
                        sql.append(", ");
                    }
                    sql.append(mLookupDef.tableNames.get(i).tableName);
                } else {
                    sql.append(" left join ");
                    sql.append(mLookupDef.tableNames.get(i).tableName);
                    sql.append(" on ");
                    for (int j = 0; j < mLookupDef.tableNames.get(i).parentKeyColumns.size(); j++) {
                        if (j > 0)
                            sql.append("and ");

                        sql.append(mLookupDef.tableNames.get(i).parentKeyColumns.get(j));
                        sql.append("=");
                        sql.append(mLookupDef.tableNames.get(i).childKeyColumns.get(j));
                        sql.append(" ");
                    }
                }
            }

            boolean addedWhere = false;

            String searchCriteria = MetrixControlAssistant.getValue(R.id.search_criteria, mLayout);
            List<String> parameters = new ArrayList<String>();

            if (!MetrixStringHelper.isNullOrEmpty(searchCriteria)) {
                sql.append(" where (");
                addedWhere = true;
                if (searchCriteria.contains(" ")) {
                    searchCriteria = searchCriteria.replace(" ", "%");
                }
                searchCriteria = "%" + searchCriteria + "%";

                for (int i = 0; i < mLookupDef.columnNames.size(); i++) {
                    if (i > 0) {
                        sql.append(" or ");
                    }
                    sql.append(mLookupDef.columnNames.get(i).columnName);
                    sql.append(" LIKE ?");

                    parameters.add(searchCriteria);
                }
                sql.append(")");
            }

            if (mLookupDef.filters != null && mLookupDef.filters.size() > 0) {
                if (addedWhere) {
                    sql.append(" and (");
                } else {
                    sql.append(" where ");
                }

                for (int i = 0; i < mLookupDef.filters.size(); i++) {
                    MetrixLookupFilterDef thisFilter = mLookupDef.filters.get(i);
                    if (i > 0) {
                        if (MetrixStringHelper.isNullOrEmpty(thisFilter.logicalOperator)) {
                            sql.append(" and ");
                        } else {
                            sql.append(" " + thisFilter.logicalOperator + " ");
                        }
                    }

                    if (thisFilter.leftParens > 0) {
                        for (int a = 1; a <= thisFilter.leftParens; a++) {
                            sql.append("(");
                        }
                    }

                    sql.append(thisFilter.leftOperand);
                    sql.append(" " + thisFilter.operator + " ");

                    if (!MetrixStringHelper.isNullOrEmpty(thisFilter.rightOperand)) {
                        if (thisFilter.noQuotes)
                            sql.append(thisFilter.rightOperand);
                        else
                            sql.append(String.format("'%s'", thisFilter.rightOperand));
                    }

                    if (thisFilter.rightParens > 0) {
                        for (int b = 1; b <= thisFilter.rightParens; b++) {
                            sql.append(")");
                        }
                    }
                }

                if (addedWhere) {
                    sql.append(")");
                }
            }

            if (mLookupDef.orderBys.size() == 0) {
                sql.append(" order by " + mLookupDef.columnNames.get(0).columnName + " collate nocase");
            } else if (mLookupDef.orderBys.size() == 1) {
                sql.append(" order by " + mLookupDef.orderBys.get(0).columnName + " collate nocase");
                if (!MetrixStringHelper.isNullOrEmpty(mLookupDef.orderBys.get(0).sortOrder)) {
                    sql.append(" " + mLookupDef.orderBys.get(0).sortOrder);
                }
            } else {
                sql.append(" order by " + mLookupDef.orderBys.get(0).columnName + " collate nocase");
                if (!MetrixStringHelper.isNullOrEmpty(mLookupDef.orderBys.get(0).sortOrder)) {
                    sql.append(" " + mLookupDef.orderBys.get(0).sortOrder);
                }

                for (int i = 1; i < mLookupDef.orderBys.size(); i++) {
                    sql.append(", " + mLookupDef.orderBys.get(i).columnName);
                    if (!MetrixStringHelper.isNullOrEmpty(mLookupDef.orderBys.get(i).sortOrder)) {
                        sql.append(" " + mLookupDef.orderBys.get(i).sortOrder);
                    }
                }
            }

            // if we have order by clauses, use the first visible column from the order by set to determine header column
            // otherwise, use existing default logic for determining header column
            String headerColumnForAdapter = mFrom[0];
            if (mLookupDef.orderBys.size() > 0) {
                ArrayList<String> fromColumns = new ArrayList<String>();
                for (int j = 0; j < mFrom.length; j++) {
                    fromColumns.add(mFrom[j]);
                }

                for (MetrixOrderByDef orderby : mLookupDef.orderBys) {
                    if (fromColumns.contains(orderby.columnName)) {
                        MetrixLookupColumnDef thisColDef = null;
                        for (MetrixLookupColumnDef colDef : mLookupDef.columnNames) {
                            if (MetrixStringHelper.valueIsEqual(colDef.columnName, orderby.columnName)) {
                                thisColDef = colDef;
                                break;
                            }
                        }

                        if (thisColDef != null && !thisColDef.alwaysHide) {
                            headerColumnForAdapter = orderby.columnName;
                            break;
                        }
                    }
                }
            }

            String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");

            if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
                sql.append(" limit " + maxRows);
            }

            if (mLookupDef.columnNames.size() == 1) {
                mTo = new int[] { R.id.itemdescription };
            } else if (mLookupDef.columnNames.size() == 2) {
                mTo = new int[] { R.id.itemdescription, R.id.item1 };
            } else if (mLookupDef.columnNames.size() == 3) {
                mTo = new int[] { R.id.itemdescription, R.id.item1, R.id.item2 };
            } else if (mLookupDef.columnNames.size() == 4) {
                mTo = new int[] { R.id.itemdescription, R.id.item1, R.id.item2, R.id.item3 };
            } else if (mLookupDef.columnNames.size() == 5) {
                mTo = new int[] { R.id.itemdescription, R.id.item1, R.id.item2, R.id.item3, R.id.item4 };
            } else if (mLookupDef.columnNames.size() == 6) {
                mTo = new int[] { R.id.itemdescription, R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5 };
            } else if (mLookupDef.columnNames.size() == 7) {
                mTo = new int[] { R.id.itemdescription, R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6 };
            } else if (mLookupDef.columnNames.size() == 8) {
                mTo = new int[] { R.id.itemdescription, R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6, R.id.item7 };
            } else if (mLookupDef.columnNames.size() == 9) {
                mTo = new int[] { R.id.itemdescription, R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6, R.id.item7, R.id.item8 };
            } else if (mLookupDef.columnNames.size() == 10) {
                mTo = new int[] { R.id.itemdescription, R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6, R.id.item7, R.id.item8, R.id.item9 };
            } else if (mLookupDef.columnNames.size() == 11) {
                mTo = new int[] { R.id.itemdescription, R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6, R.id.item7, R.id.item8, R.id.item9, R.id.item10 };
            }

            MetrixCursor cursor = null;
            mTable = new ArrayList<HashMap<String, String>>();

            int rowCount = 0;

            try {
                cursor = MetrixDatabaseManager.rawQueryMC(sql.toString(), parameters.toArray(new String[parameters.size()]));

                if (cursor != null && cursor.moveToFirst()) {
                    MetrixControlAssistant.setButtonVisibility(mAdd, View.GONE);
                    rowCount = cursor.getCount();

                    while (cursor.isAfterLast() == false) {
                        HashMap<String, String> row = new HashMap<String, String>();

                        for (int i = 0; i < mFrom.length; i++) {
                            Format formatter = mLookupDef.columnNames.get(i).formatter;
                            if (formatter != null) {
                                if (formatter instanceof DateFormat) {
                                    row.put(mFrom[i], MetrixDateTimeHelper.convertDateTimeFromDBToUI(cursor.getString(i), formatter));
                                } else if (formatter instanceof NumberFormat) {
                                    row.put(mFrom[i], MetrixFloatHelper.convertNumericFromDBToUI(cursor.getString(i), formatter));
                                } else if (formatter instanceof MetrixFormat) {
                                    MetrixFormat metrixFormatter = (MetrixFormat) (formatter);
                                    row.put(mFrom[i], metrixFormatter.format(cursor.getString(i), this));
                                } else {
                                    row.put(mFrom[i], cursor.getString(i));
                                }
                            } else {
                                row.put(mFrom[i], cursor.getString(i));
                            }
                        }

                        mTable.add(row);
                        cursor.moveToNext();
                    }
                } else {
                    if (!MetrixStringHelper.isNullOrEmpty(searchCriteria) && mLookupDef.populatesMultipleViews() && mLookupDef.allowValueNotInList) {
                        MetrixControlAssistant.setButtonVisibility(mAdd, View.VISIBLE);
                    }
                }

            } finally {
                this.displayRowCount(rowCount);

                if (cursor != null) {
                    cursor.close();
                }
            }

            int layoutId = R.layout.lookup_item;

            if (mLookupDef.format == MetrixLookupFormat.OneColumnThreeRows) {
                layoutId = R.layout.lookup_item_one_column_three_rows;
            } else if (mLookupDef.format == MetrixLookupFormat.OneColumnTwoRows) {
                layoutId = R.layout.lookup_item_one_column_two_rows;
            } else if (mLookupDef.format == MetrixLookupFormat.TwoColumnsFourRows) {
                layoutId = R.layout.lookup_item_two_columns_four_rows;
            } else if (mLookupDef.format == MetrixLookupFormat.TwoColumnsThreeRows) {
                layoutId = R.layout.lookup_item_two_columns_three_rows;
            } else if (mLookupDef.format == MetrixLookupFormat.TwoColumnsFourRowsPlusOne) {
                layoutId = R.layout.lookup_item_two_columns_four_rows_plus_one;
            }

            // fill in the grid_item layout
            if (simpleAdapter == null) {
                final String uniqueIDKey = mLookupDef.columnNames.size() > 0 ? mLookupDef.columnNames.get(0).columnName : null;
                simpleAdapter = new LookupRecyclerViewAdapter(this, mTable, layoutId, mFrom, mTo, new int[]{}, uniqueIDKey, headerColumnForAdapter, mShouldShowChkbox);
                simpleAdapter.setClickListener(this);
                if (recyclerView != null) {
                    recyclerView.setAdapter(simpleAdapter);
                    recyclerView.addItemDecoration(mBottomOffset);
                }
            } else {
                simpleAdapter.updateData(mTable);
            }
        }
    }

    private void displayRowCount(int rowCount) {
        try {
            TextView rowCountView = (TextView) findViewById(R.id.row_count);
            if (rowCountView != null) {
                rowCountView.setText(AndroidResourceHelper.getMessage("RecordsFound1Arg", rowCount));
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(() -> {
            if (!LookupBase.this.isDestroyed())
                performQuery();
        }, 500);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void onSimpleRvItemClick(int position, Object item, View view) {
        HashMap<String, String> selectedItem = (HashMap<String, String>) item;
        boolean lookupSet = true;

        saveSearchFilter();

        for (MetrixLookupColumnDef columnDef : mLookupDef.columnNames) {
            if (columnDef.controlId != 0) {
                try {
                    String value = selectedItem.get(columnDef.columnName);
                    MetrixControlAssistant.setValue(columnDef.controlId, mParentLayout, value);
                } catch (Exception e) {
                    lookupSet = false;
                    LogManager.getInstance(this).error(e);
                }
            }
        }

        if (lookupSet)
            setResult(RESULT_OK, getIntent());
        else
            setResult(RESULT_CANCELED, getIntent());
        this.onBackPressed();
    }

    public static class LookupRecyclerViewAdapter extends SimpleRecyclerViewAdapter {
        private final MetrixLookupDef lookupDef;
        private final String checkBoxText;
        private final boolean shouldShowCheckBox;

        public LookupRecyclerViewAdapter(Activity activity, @NonNull List<HashMap<String, String>> data,
                                         int resource, String[] from, int[] to, int[] hiddenViews,
                                         String uniqueIDKey, String headerColumn, boolean shouldShowCheckBox) {
            super(data, resource, from, to, hiddenViews, uniqueIDKey, headerColumn);
            lookupDef = (MetrixLookupDef) MetrixPublicCache.instance.getItem("lookupDef");
            resolveColorsToUse(activity);
            this.checkBoxText = AndroidResourceHelper.getMessage("Cb");
            this.shouldShowCheckBox = shouldShowCheckBox;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            if (holder instanceof SimpleViewHolder) {
                final SimpleViewHolder viewHolder = (SimpleViewHolder) holder;
                hideUnusedViews(viewHolder);
                showHideChkBox(viewHolder, shouldShowCheckBox, position);
            }
        }

        private void hideUnusedViews(SimpleViewHolder viewHolder) {
            hideUnusedView(viewHolder.views.get(R.id.itemdescription), 1);
            hideUnusedView(viewHolder.views.get(R.id.item1), 0);
            hideUnusedView(viewHolder.views.get(R.id.item2), 2);
            hideUnusedView(viewHolder.views.get(R.id.item3), 3);
            hideUnusedView(viewHolder.views.get(R.id.item4), 4);
            hideUnusedView(viewHolder.views.get(R.id.item5), 5);
            hideUnusedView(viewHolder.views.get(R.id.item6), 6);
            hideUnusedView(viewHolder.views.get(R.id.item7), 7);
            hideUnusedView(viewHolder.views.get(R.id.item8), 8);
            hideUnusedView(viewHolder.views.get(R.id.item9), 9);
            hideUnusedView(viewHolder.views.get(R.id.item10), 10);
        }

        private void hideUnusedView(View view, int count) {
            if (view != null) {
                try {
                    Boolean alwaysHide = false;
                    if (lookupDef.columnNames.size() > count) {
                        alwaysHide = lookupDef.columnNames.get(count).alwaysHide;
                    }

                    if (alwaysHide || MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue((TextView) view))) {
                        view.setVisibility(View.GONE);
                    } else {
                        view.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    LogManager.getInstance().error(e);
                }
            }
        }

        private void showHideChkBox(SimpleViewHolder viewHolder, boolean show, final int position) {
            final CheckBox cBox = (CheckBox) viewHolder.views.get(R.id.checkboxState);

            if(cBox != null) {
                cBox.setText(checkBoxText);
                final HashMap<String, String> dataRow = (HashMap<String, String>) data.get(position);
                if(dataRow != null) {
                    final String currentChkState = dataRow.get("checkboxState");
                    if (!MetrixStringHelper.isNullOrEmpty(currentChkState) && MetrixStringHelper.valueIsEqual(currentChkState, "Y"))
                        cBox.setChecked(true);
                    else
                        cBox.setChecked(false);
                }

                if(show)
                    cBox.setVisibility(View.VISIBLE);
                else
                    cBox.setVisibility(View.GONE);

                cBox.setOnClickListener((v) -> {
                    if (dataRow == null) return;
                    String checkState;
                    if (cBox.isChecked())
                        checkState = "Y";
                    else
                        checkState = "N";
                    dataRow.put("checkboxState", checkState);
                });
            }
        }
    }

    public class SectionedLookupAdapter extends SectionedSimpleAdapter {

        private MetrixLookupDef mLookupDef;
        private final String checkBoxText;

        public SectionedLookupAdapter(Context context, Activity activity, List<HashMap<String, String>> data, int resource, String[] from, int[] to, String headerColumn) {
            super(context, data, resource, from, to, headerColumn);
            mLookupDef = (MetrixLookupDef) MetrixPublicCache.instance.getItem("lookupDef");
            this.sourceActivity = activity;
            this.resolveColorsToUse();
            this.checkBoxText = AndroidResourceHelper.getMessage("Cb");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            int type = getItemViewType(position);

            switch (type) {
                case ITEM:
                    View view = super.getView(position, convertView, parent);
                    hideUnusedViews(view);
                    showHideChkBox(view, mShouldShowChkbox, position);
                    return view;
                case SEPARATOR:
                    view = super.getView(position, convertView, parent);
                    hideUnusedViews(view);
                    showHideChkBox(view, mShouldShowChkbox, position);
                    return view;
            }

            return convertView;
        }

        private void hideUnusedViews(View view) {
            hideUnusedView(view.findViewById(R.id.itemdescription), 1);
            hideUnusedView(view.findViewById(R.id.item1), 0);
            hideUnusedView(view.findViewById(R.id.item2), 2);
            hideUnusedView(view.findViewById(R.id.item3), 3);
            hideUnusedView(view.findViewById(R.id.item4), 4);
            hideUnusedView(view.findViewById(R.id.item5), 5);
            hideUnusedView(view.findViewById(R.id.item6), 6);
            hideUnusedView(view.findViewById(R.id.item7), 7);
            hideUnusedView(view.findViewById(R.id.item8), 8);
            hideUnusedView(view.findViewById(R.id.item9), 9);
            hideUnusedView(view.findViewById(R.id.item10), 10);
        }

        private void hideUnusedView(View view, int count) {
            if (view != null) {
                try {
                    Boolean alwaysHide = false;
                    if (mLookupDef.columnNames.size() > count) {
                        alwaysHide = mLookupDef.columnNames.get(count).alwaysHide;
                    }

                    if (alwaysHide || MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue((TextView) view))) {
                        view.setVisibility(View.GONE);
                    } else {
                        view.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    LogManager.getInstance().error(e);
                }
            }
        }

        private void showHideChkBox(View view, boolean show, final int position)
        {
            final CheckBox cBox = (CheckBox) view.findViewById(R.id.checkboxState);

            if(cBox != null) {
                cBox.setText(checkBoxText);
                final HashMap<String, String> dataRow = mTable.get(position);
                if(dataRow != null) {
                    String currentChkState = dataRow.get("checkboxState");
                    if (!MetrixStringHelper.isNullOrEmpty(currentChkState) && MetrixStringHelper.valueIsEqual(currentChkState, "Y"))
                        cBox.setChecked(true);
                    else
                        cBox.setChecked(false);
                }

                if(show)
                    cBox.setVisibility(View.VISIBLE);
                else
                    cBox.setVisibility(View.GONE);

                cBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String checkState;
                        if (cBox.isChecked())
                            checkState = "Y";
                        else
                            checkState = "N";
                        dataRow.put("checkboxState", checkState);
                    }
                });
            }
        }
    }

    protected void saveSearchFilter() {
        try {
            MetrixAutoCompleteHelper.saveAutoCompleteFilter(Lookup.class.getName() + "-" + MetrixControlAssistant.getValue(mTitle), mSearchCriteria);
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }
    }
}


