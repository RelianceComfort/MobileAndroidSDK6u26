package com.metrix.metrixmobile;

import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by royaus on 4/4/2016.
 */
public class ShipmentUnit extends MetrixActivity implements View.OnFocusChangeListener {
    private FloatingActionButton mAddButton, mListButton;
    private EditText mSerialId, mQuantity, mLotId;

    private String mShipmentId = "";
    private String mPlaceIdShipdTo = "";

    private List<FloatingActionButton> mFABList;
    private List<FloatingActionButton> mFABsToShow;

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shipment_unit);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onStart()
     */
    public void onStart() {
        super.onStart();

        mShipmentId = MetrixCurrentKeysHelper.getKeyValue("shipment","shipment_id");
        mPlaceIdShipdTo = MetrixCurrentKeysHelper.getKeyValue("shipment_detail", "place_id_shipd_to");

        mLayout = (ViewGroup) findViewById(R.id.table_layout);


        TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
        if (actionBarTitle != null) {
            actionBarTitle.setText(AndroidResourceHelper.getMessage("ShipActionBarTitle2Args", mShipmentId, mPlaceIdShipdTo));
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

        mAddButton = (FloatingActionButton) findViewById(R.id.add);
        mListButton = (FloatingActionButton) findViewById(R.id.list);

        mAddButton.setOnClickListener(this);
        mListButton.setOnClickListener(this);

        mFABList.add(mAddButton);
        mFABList.add(mListButton);

        if (this.mActivityDef != null && this.mActivityDef.TransactionType == MetrixTransactionTypes.UPDATE) {
            this.displaySaveButtonOnAddNextBar();
        }

        fabRunnable = this::showFABs;

        NestedScrollView scrollView = findViewById(R.id.scroll_view);
        mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(),generateOffsetForFABs(mFABList));
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

    protected void displaySaveButtonOnAddNextBar() {
        FloatingActionButton addButton = (FloatingActionButton) findViewById(R.id.add);
        FloatingActionButton nextButton = (FloatingActionButton) findViewById(R.id.list);
        FloatingActionButton updateButton = (FloatingActionButton) findViewById(R.id.update);

        if (addButton != null) {
            MetrixControlAssistant.setButtonVisibility(addButton, View.GONE);
        }

        if (nextButton != null) {
            MetrixControlAssistant.setButtonVisibility(nextButton, View.GONE);
        }

        MetrixControlAssistant.setButtonVisibility(updateButton, View.VISIBLE);
        updateButton.setOnClickListener(this);
        mFABList.add(updateButton);
    }

    /**
     * Set the default values for views for this activity.
     */
    protected void defaultValues() {
        if (this.mActivityDef == null || this.mActivityDef.TransactionType == MetrixTransactionTypes.INSERT) {
            try {
                String partId = MetrixCurrentKeysHelper.getKeyValue("shipment_detail", "item_id");
                String serializedPart = MetrixDatabaseManager.getFieldStringValue("part", "serialed", "part_id='" + partId + "'");
                String lotIdentified = MetrixDatabaseManager.getFieldStringValue("part", "lot_identified", "part_id='" + partId + "'");


                mSerialId = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "shipment_unit", "serial_id");
                mLotId = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "shipment_unit", "lot_id");
                mQuantity = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "shipment_unit", "quantity");

                MetrixColumnDef lotIdColDef = mFormDef.getColumnDef("shipment_unit", "lot_id");
                MetrixColumnDef serialIdColDef = mFormDef.getColumnDef("shipment_unit", "serial_id");

                mQuantity.setEnabled(true);
                if (serializedPart.compareToIgnoreCase("Y") == 0)
                {
                    if (serialIdColDef != null) {
                        mSerialId.setVisibility(View.VISIBLE);
                        if (serialIdColDef.labelId != null)
                            MetrixControlAssistant.setVisibility(serialIdColDef.labelId, mLayout, View.VISIBLE);
                    }
                    MetrixControlAssistant.setValue(mFormDef, mLayout, "shipment_unit", "quantity", "1");
                    mQuantity.setEnabled(false);
                } else {
                    MetrixControlAssistant.setValue(mFormDef, mLayout, "shipment_unit", "serial_id", "");
                    if (serialIdColDef != null) {
                        mSerialId.setVisibility(View.GONE);
                        if (serialIdColDef.labelId != null)
                            MetrixControlAssistant.setVisibility(serialIdColDef.labelId, mLayout, View.GONE);
                    }
                }

                if (lotIdentified.compareToIgnoreCase("Y") == 0)
                {
                    if (lotIdColDef != null) {
                        mLotId.setVisibility(View.VISIBLE);
                        if (lotIdColDef.labelId != null)
                            MetrixControlAssistant.setVisibility(lotIdColDef.labelId, mLayout, View.VISIBLE);
                    }
                }
                else
                {
                    MetrixControlAssistant.setValue(mFormDef, mLayout, "shipment_unit", "lot_id", "");
                    if (lotIdColDef != null) {
                        mLotId.setVisibility(View.GONE);
                        if (lotIdColDef.labelId != null)
                            MetrixControlAssistant.setVisibility(lotIdColDef.labelId, mLayout, View.GONE);
                    }
                }

                MetrixControlAssistant.setValue(mFormDef, mLayout, "shipment_unit", "part_id", MetrixCurrentKeysHelper.getKeyValue("shipment_detail", "item_id"));
                MetrixControlAssistant.setValue(mFormDef, mLayout, "shipment_unit", "shipment_sequence", MetrixCurrentKeysHelper.getKeyValue("shipment_detail", "sequence"));
                MetrixControlAssistant.setValue(mFormDef, mLayout, "shipment_unit", "shipment_id", MetrixCurrentKeysHelper.getKeyValue("shipment", "shipment_id"));

            } catch (Exception e) {
                LogManager.getInstance(this).error(e);
            }
        }
        else
        {
            try {
                String partId = MetrixCurrentKeysHelper.getKeyValue("shipment_detail", "item_id");
                String serializedPart = MetrixDatabaseManager.getFieldStringValue("part", "serialed", "part_id='" + partId + "'");
                String lotIdentified = MetrixDatabaseManager.getFieldStringValue("part", "lot_identified", "part_id='" + partId + "'");


                mSerialId = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "shipment_unit", "serial_id");
                mLotId = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "shipment_unit", "lot_id");
                mQuantity = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "shipment_unit", "quantity");

                MetrixColumnDef lotIdColDef = mFormDef.getColumnDef("shipment_unit", "lot_id");
                MetrixColumnDef serialIdColDef = mFormDef.getColumnDef("shipment_unit", "serial_id");

                mQuantity.setEnabled(true);
                if (serializedPart.compareToIgnoreCase("Y") == 0)
                {
                    if (serialIdColDef != null) {
                        mSerialId.setVisibility(View.VISIBLE);
                        if (serialIdColDef.labelId != null)
                            MetrixControlAssistant.setVisibility(serialIdColDef.labelId, mLayout, View.VISIBLE);
                    }
                    mQuantity.setEnabled(false);
                } else {
                    if (serialIdColDef != null) {
                        mSerialId.setVisibility(View.GONE);
                        if (serialIdColDef.labelId != null)
                            MetrixControlAssistant.setVisibility(serialIdColDef.labelId, mLayout, View.GONE);
                    }
                }

                if (lotIdentified.compareToIgnoreCase("Y") == 0)
                {
                    if (lotIdColDef != null) {
                        mLotId.setVisibility(View.VISIBLE);
                        if (lotIdColDef.labelId != null)
                            MetrixControlAssistant.setVisibility(lotIdColDef.labelId, mLayout, View.VISIBLE);
                    }
                }
                else
                {
                    if (lotIdColDef != null) {
                        mLotId.setVisibility(View.GONE);
                        if (lotIdColDef.labelId != null)
                            MetrixControlAssistant.setVisibility(lotIdColDef.labelId, mLayout, View.GONE);
                    }
                }

            } catch (Exception e) {
                LogManager.getInstance(this).error(e);
            }
        }
    }

    /**
     * This method is responsible for setting up the meta data which the
     * architecture uses for data binding and validation.
     */
    protected void defineForm() {

        MetrixTableDef shipmentUnitDef = null;
        if (this.mActivityDef != null)
        {
            shipmentUnitDef = new MetrixTableDef("shipment_unit", this.mActivityDef.TransactionType);
            if (this.mActivityDef.Keys != null)
                shipmentUnitDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, String.valueOf(this.mActivityDef.Keys.get("metrix_row_id")), double.class));
        }
        else
            shipmentUnitDef = new MetrixTableDef("shipment_unit", MetrixTransactionTypes.INSERT);

        this.mFormDef = new MetrixFormDef(shipmentUnitDef);
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
            case R.id.add:
                MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
                MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, AndroidResourceHelper.getMessage("ShipmentUnit"));
            case R.id.list:
                mCurrentActivity.finish();
            case R.id.update:
                if (anyOnStartValuesChanged()) {
                    transactionInfo = new MetrixTransaction();

                    MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, AndroidResourceHelper.getMessage("ShipmentUnit"));
                }
                finish();
                break;
            default:
                super.onClick(v);
        }
    }


}

