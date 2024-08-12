package com.metrix.metrixmobile;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.GenericParcelable;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.system.BottomOffsetDecoration;
import com.metrix.metrixmobile.system.LookupBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by RaWiLK on 6/21/2016.
 */
public class DebriefPartUsageLookup extends LookupBase {

    private FloatingActionButton mCustomAdd, mCustomProceed;
    private boolean mPartOnly, mPartWithSerial, mPartWithLot, mPartWithSerialAndLot;
    private boolean mIsControlledPart;
    private BottomOffsetDecoration mBottomOffset;
    private List<FloatingActionButton> mFABList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debrief_part_usage_lookup);
        setInfoBasedOnPartCrieteria();
        mCustomAdd = (FloatingActionButton) findViewById(R.id.custom_add);
        mCustomProceed = (FloatingActionButton) findViewById(R.id.custom_proceed);
        recyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
    }

    public void onStart() {
        resourceStrings.add(new ResourceValueObject(R.id.search_criteria, "Search", true));
        resourceStrings.add(new ResourceValueObject(R.id.row_count, "RowCount"));
        super.onStart();
        showHidePartRelatedCustomButtons();
    }

    @Override
    public void onSimpleRvItemClick(int position, Object item, View itemView) {
        if(mPartOnly) {
            @SuppressWarnings("unchecked")
            final HashMap<String, String> selectedItem = (HashMap<String, String>) item;
            String selectedPartId = selectedItem.get("stock_bin.part_id");
            String serializedPart = MetrixDatabaseManager.getFieldStringValue("part", "serialed", "part_id='" + selectedPartId + "'");
            String lotIdentified = MetrixDatabaseManager.getFieldStringValue("part", "lot_identified", "part_id='" + selectedPartId + "'");
            String controlledPart = MetrixDatabaseManager.getFieldStringValue("part", "controlled_part", "part_id='" + selectedPartId + "'");
            if (controlledPart.toLowerCase().contains("y")) mIsControlledPart = true;

            if ((!MetrixStringHelper.isNullOrEmpty(serializedPart) && MetrixStringHelper.valueIsEqual(serializedPart, "N"))
                    && (!MetrixStringHelper.isNullOrEmpty(lotIdentified) && MetrixStringHelper.valueIsEqual(lotIdentified, "N"))
                    && !mIsControlledPart) {
                final int pos = position;
                final Object obj = item;
                final View v = itemView;

                final String qtyOnHand = selectedItem.get("stock_bin.qty_on_hand");
                PartUsedQtyEntry partUsedQtyEntry = new PartUsedQtyEntry(this, selectedPartId, qtyOnHand);
                partUsedQtyEntry.initDialog();
                partUsedQtyEntry.setOnAlertDialogPositiveButtonClickListner((view, qty) -> handleDPUPartSelection(qty, pos, obj, v));
            }
            else{
                super.onSimpleRvItemClick(position, item, itemView);
            }
        }
    }

    protected void setListeners() {
        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        if(mCustomAdd != null) {
            mCustomAdd.setOnClickListener(this);
            mFABList.add(mCustomAdd);
        }

        if(mCustomProceed != null) {
            mCustomProceed.setOnClickListener(this);
            mFABList.add(mCustomProceed);
        }

        super.setListeners();

        mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));

        fabRunnable = this::showFABs;

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.custom_add:
                List<HashMap<String, String>> selectedItems = new ArrayList<HashMap<String, String>>();

                for (int x = 0; x < mTable.size(); x++) {
                    HashMap<String, String> item = mTable.get(x);
                    String chkBoxState = item.get("checkboxState");
                    if (!MetrixStringHelper.isNullOrEmpty(chkBoxState) && MetrixStringHelper.valueIsEqual(chkBoxState, "Y"))
                        selectedItems.add(item);
                }

                if (selectedItems.size() == 0)
                {
                    MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("PrtUsdPlsSelectAtLeastOneItem"));
                    return;
                }

                Intent intent = getIntent();
                if(mPartWithSerial)
                    intent.putExtra("SerialList", new GenericParcelable<List<HashMap<String, String>>>(selectedItems));
                else if(mPartWithSerialAndLot)
                    intent.putExtra("SerialLotList", new GenericParcelable<List<HashMap<String, String>>>(selectedItems));
                setResult(RESULT_OK, intent);
                saveSearchFilter();
                this.onBackPressed();
                break;
            case R.id.custom_proceed:
                selectedItems = new ArrayList<HashMap<String, String>>();
                for(int x = 0; x < mTable.size(); x++)
                {
                    HashMap<String, String> item = mTable.get(x);
                    String chkBoxState = item.get("checkboxState");
                    if(!MetrixStringHelper.isNullOrEmpty(chkBoxState) && MetrixStringHelper.valueIsEqual(chkBoxState, "Y"))
                        selectedItems.add(item);
                }

                if (selectedItems.size() == 0)
                {
                    MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("PrtUsdPlsSelectAtLeastOneItem"));
                    return;
                }

                PartUsedLotQtyEntry partUsedLotQtyEntry = new PartUsedLotQtyEntry(mCurrentActivity, selectedItems);
                partUsedLotQtyEntry.initDialog();
                partUsedLotQtyEntry.setOnPartUsedLotAlertDialogPositiveButtonClickListner(new PartUsedLotQtyEntry.OnPartUsedLotAlertDialogPositiveButtonClickListner() {
                    @Override
                    public void OnPartUsedLotAlertDialogPositiveButtonClick(View view, List<HashMap<String, String>> list) {
                        Intent intent = getIntent();
                        intent.putExtra("LotListWithQty", new GenericParcelable<List<HashMap<String, String>>>(list));
                        setResult(RESULT_OK, intent);
                        saveSearchFilter();
                        mCurrentActivity.onBackPressed();
                    }
                });
                break;
            default:
                super.onClick(v);
        }
    }

    private void setInfoBasedOnPartCrieteria(){
        if (this.getIntent().getExtras() != null)
        {
            Bundle extras = this.getIntent().getExtras();
            if(extras.containsKey("displayPartsOnly") && extras.getBoolean("displayPartsOnly")) {
                mShouldShowChkbox = false;
                mPartOnly = true;
            }
            if(extras.containsKey("displaySerials") && extras.getBoolean("displaySerials")) {
                mShouldShowChkbox = true;
                mPartWithSerial = true;
            }
            if(extras.containsKey("displayLots") && extras.getBoolean("displayLots")) {
                mShouldShowChkbox = true;
                mPartWithLot = true;
            }
            if(extras.containsKey("displayLotSerials") && extras.getBoolean("displayLotSerials")) {
                mShouldShowChkbox = true;
                mPartWithSerialAndLot = true;
            }
        }
    }

    private void showHidePartRelatedCustomButtons(){
        if(mPartWithSerial || mPartWithSerialAndLot) {
            if (mCustomAdd != null) {
                MetrixControlAssistant.setButtonVisibility(mCustomAdd, View.VISIBLE);//mCustomAdd.setVisibility(View.VISIBLE);
            }
        }

        if(mPartWithLot) {
            if (mCustomProceed != null) {
                MetrixControlAssistant.setButtonVisibility(mCustomProceed, View.VISIBLE);//mCustomProceed.setVisibility(View.VISIBLE);
            }
        }
    }

    private void handleDPUPartSelection(String qty, int position, Object item, View view) {
        MetrixPublicCache.instance.addItem("PartOnlyUsedQty", qty);
        super.onSimpleRvItemClick(position, item, view);
    }
}
