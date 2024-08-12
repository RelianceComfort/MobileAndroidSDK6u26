package com.metrix.metrixmobile;

import android.database.SQLException;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by RaWiLK on 5/19/2016.
 */
public class ServicePartsList extends MetrixActivity {
    private MetadataRecyclerViewAdapter mAdapter;
    private List<HashMap<String, String>> table;
    private RecyclerView recyclerView;
    private FloatingActionButton mSaveButton, mCancelButton;
    private String mPartLineCode;
    private String mStatus;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.service_parts_list);

        if(MetrixCurrentKeysHelper.keyExists("part_need", "part_line_code"))
            mPartLineCode = MetrixCurrentKeysHelper.getKeyValue("part_need", "part_line_code");
        if(MetrixCurrentKeysHelper.keyExists("part_need", "status"))
            mStatus = MetrixCurrentKeysHelper.getKeyValue("part_need", "status");

        recyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
    }

    public void onStart() {
        mLayout = (ViewGroup) findViewById(R.id.table_layout);
        super.onStart();
        populateList();
        setListeners();
    }

    /**
     * Define the listeners for this activity.
     */
    protected void setListeners() {
        mSaveButton = (FloatingActionButton) findViewById(R.id.custom_save);
        mSaveButton.setOnClickListener(this);
        mCancelButton = (FloatingActionButton) findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (scriptEventConsumesClick(this, v))
            return;

        switch (v.getId()) {
            case R.id.custom_save:
                saveAsPartNeeds();
                break;
            case R.id.cancel:
                MetrixPublicCache.instance.addItem("backButtonPress_Occurred", true);
                finish();
                break;
            default:
                super.onClick(v);
        }
    }

    private void saveAsPartNeeds() {
        try {
            List<HashMap<String, String>> partsList = mAdapter.getListData();

            if (partsList != null && partsList.size() > 0) {

                for (int i = 0; i < partsList.size(); i++) {
                    HashMap<String, String> thePart = partsList.get(i);
                    if (thePart.containsKey("checkboxState")) {

                        String chkState = thePart.get("checkboxState");
                        if (!MetrixStringHelper.isNullOrEmpty(chkState) && MetrixStringHelper.valueIsEqual(chkState, "Y")) {

                            MetrixSqlData partNeedsRow = new MetrixSqlData("part_need", MetrixTransactionTypes.INSERT);
                            ArrayList<MetrixSqlData> partNeedData = new ArrayList<MetrixSqlData>();

                            String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
                            partNeedsRow.dataFields.add(new DataField("task_id", taskId));
                            partNeedsRow.dataFields.add(new DataField("request_id", MetrixDatabaseManager.getFieldStringValue("task", "request_id", "task_id=" + taskId)));

                            String personId = User.getUser().personId;
                            String placeId = MetrixDatabaseManager.getFieldStringValue("person_place", "place_id", "place_relationship = 'FOR_STOCK' and person_id = '" + personId + "'");
                            String location = MetrixDatabaseManager.getFieldStringValue("person_place", "location", "place_relationship = 'FOR_STOCK' and person_id = '" + personId + "'");

                            String stockingPlace = MetrixDatabaseManager.getFieldStringValue("place_xref", "related_place_id", "place_relationship = 'FOR_STOCK' and place_id = '" + placeId + "'");
                            String stockingLocation = MetrixDatabaseManager.getFieldStringValue("place_xref", "related_location", "place_relationship = 'FOR_STOCK' and place_id = '" + placeId + "'");

                            partNeedsRow.dataFields.add(new DataField("promised_dt", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_FORMAT)));
                            partNeedsRow.dataFields.add(new DataField("place_id_from", stockingPlace));
                            partNeedsRow.dataFields.add(new DataField("location_from", stockingLocation));
                            partNeedsRow.dataFields.add(new DataField("place_id_to", placeId));
                            partNeedsRow.dataFields.add(new DataField("location_to", location));

                            partNeedsRow.dataFields.add(new DataField("part_line_code", mPartLineCode));
                            partNeedsRow.dataFields.add(new DataField("part_id", thePart.get("part.part_id")));

                            String qty = thePart.get("original_model_structure.quantity");
                            if (MetrixStringHelper.isNullOrEmpty(qty)) qty = "1";
                            partNeedsRow.dataFields.add(new DataField("quantity", qty));

                            partNeedsRow.dataFields.add(new DataField("status", mStatus));

                            int sequenceKey = MetrixDatabaseManager.generatePrimaryKey("part_need");
                            partNeedsRow.dataFields.add(new DataField("sequence", sequenceKey));

                            partNeedData.add(partNeedsRow);

                            MetrixTransaction transactionInfo = new MetrixTransaction();
                            MetrixUpdateManager.update(partNeedData, true, transactionInfo, AndroidResourceHelper.getMessage("AddPartNeed"), null, false);
                            MetrixPublicCache.instance.addItem("backButtonPress_Occurred", true);
                            finish();
                        }
                    }
                }
            }
        } catch (Exception e) {
            MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("ServiceBOMSaveErrMes"));
            LogManager.getInstance(this).error(e);
        }
    }

    private void populateList() {
        try {
            String taskRequestUnitId = MetrixDatabaseManager.getFieldStringValue("task", "request_unit_id", "task_id = " + MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));

            String requestUnitQuery = String.format("select request_unit.metrix_row_id, request_unit.model_id, request_unit.part_id, request_unit.lot_id" +
                    " from request_unit where request_unit.request_unit_id = '%s'", taskRequestUnitId);
            String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
            if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
                requestUnitQuery = requestUnitQuery + " limit " + maxRows;
            }

            ArrayList<Hashtable<String, String>> requestUnits = MetrixDatabaseManager.getFieldStringValuesList(requestUnitQuery);

            if (requestUnits != null && requestUnits.size() > 0) {
                Hashtable<String, String> theRequestUnit = requestUnits.get(0);
                if (theRequestUnit != null) {
                    double minPartScore = 0;

                    String theParentModelId = "";
                    if (theRequestUnit.containsKey("model_id") && !MetrixStringHelper.isNullOrEmpty(theRequestUnit.get("model_id")))
                        theParentModelId = theRequestUnit.get("model_id");

                    String theParentPartId = "";
                    if (theRequestUnit.containsKey("part_id") && !MetrixStringHelper.isNullOrEmpty(theRequestUnit.get("part_id")))
                        theParentPartId = theRequestUnit.get("part_id");

                    if (!MetrixStringHelper.isNullOrEmpty(theParentModelId) || !MetrixStringHelper.isNullOrEmpty(theParentPartId)) {
                        boolean modelConstraintAdded = false;

                        StringBuilder modelStructureWhereQueryBuilder = new StringBuilder();
                        modelStructureWhereQueryBuilder.append("where (");

                        if (!MetrixStringHelper.isNullOrEmpty(theParentModelId)) {
                            modelStructureWhereQueryBuilder.append(String.format("model_structure.parent_model_id = '%s' ", theParentModelId));
                            modelConstraintAdded = true;
                        }

                        if (!MetrixStringHelper.isNullOrEmpty(theParentPartId)) {
                            if (modelConstraintAdded)
                                modelStructureWhereQueryBuilder.append("or ");

                            modelStructureWhereQueryBuilder.append(String.format("model_structure.parent_part_id = '%s' ", theParentPartId));
                        }
                        modelStructureWhereQueryBuilder.append(") ");

                        modelStructureWhereQueryBuilder.append("and model_structure.part_id is not null ");
                        modelStructureWhereQueryBuilder.append(String.format("and model_structure.serviceable = '%s' ", "Y"));
                        if (minPartScore > 0)
                            modelStructureWhereQueryBuilder.append(String.format("and model_structure.part_score > %d", minPartScore));

                        String query = MetrixListScreenManager.generateListQuery(this, "model_structure", modelStructureWhereQueryBuilder.toString());
                        query = query + " order by model_structure.part_score desc";

                        MetrixCursor cursor = null;
                        table = new ArrayList<HashMap<String, String>>();

                        try {

                            cursor = MetrixDatabaseManager.rawQueryMC(query, null);
                            if (cursor != null && cursor.moveToFirst()) {
                                final String serviceBOMListQtyText = AndroidResourceHelper.getMessage("ServiceBOMListQty");
                                final String serviceBOMListPartScoreText = AndroidResourceHelper.getMessage("ServiceBOMListPartScore");

                                while (cursor.isAfterLast() == false) {
                                    HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);

                                    String qty = row.get("model_structure.quantity");
                                    row.put("model_structure.quantity", AndroidResourceHelper.formatMessage(serviceBOMListQtyText, "ServiceBOMListQty", qty));
                                    row.put("original_" + "model_structure.quantity", qty);

                                    String partScore = row.get("model_structure.part_score");
                                    if (MetrixStringHelper.isNullOrEmpty(partScore)) partScore = "0";
                                    row.put("model_structure.part_score", AndroidResourceHelper.formatMessage(serviceBOMListPartScoreText, "ServiceBOMListPartScore", partScore));
                                    row.put("original_" + "model_structure.part_score", partScore);

                                    table.add(row);
                                    cursor.moveToNext();
                                }

                                table = MetrixListScreenManager.performScriptListPopulation(this, table);
                            }

                        } catch (SQLException ex) {
                            LogManager.getInstance(this).error(ex);
                            throw ex;
                        } finally {
                            if (cursor != null) {
                                cursor.close();
                            }
                        }

                        if (mAdapter == null) {
                            mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_checkbox,
                                    R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold,  R.id.list_checkbox, "model_structure.metrix_row_id", 0, R.id.sliver, null, "model_structure.metrix_row_id", null);
                            recyclerView.setAdapter(mAdapter);
                        } else {
                            mAdapter.updateData(table);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LogManager.getInstance(this).error(ex);
        }
    }
}
