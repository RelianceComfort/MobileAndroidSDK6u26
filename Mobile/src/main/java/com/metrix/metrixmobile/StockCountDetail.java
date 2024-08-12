package com.metrix.metrixmobile;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.ui.widget.SimpleRecyclerViewAdapter;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.StockCountData;
import com.metrix.architecture.utilities.StockCountHelper;
import com.metrix.metrixmobile.global.MobileGlobal;
import com.metrix.metrixmobile.system.BottomOffsetDecoration;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import static com.metrix.architecture.utilities.StockCountHelper.allPartsCount;
import static com.metrix.architecture.utilities.StockCountHelper.btnAdd;
import static com.metrix.architecture.utilities.StockCountHelper.btnConfirm;
import static com.metrix.architecture.utilities.StockCountHelper.btnDelete;
import static com.metrix.architecture.utilities.StockCountHelper.btnReset;
import static com.metrix.architecture.utilities.StockCountHelper.btnViewList;
import static com.metrix.architecture.utilities.StockCountHelper.chkBoxUsable;
import static com.metrix.architecture.utilities.StockCountHelper.ebxLotId;
import static com.metrix.architecture.utilities.StockCountHelper.ebxLotUnusable;
import static com.metrix.architecture.utilities.StockCountHelper.ebxLotUsable;
import static com.metrix.architecture.utilities.StockCountHelper.ebxSerialId;
import static com.metrix.architecture.utilities.StockCountHelper.ebxUnuseableActual;
import static com.metrix.architecture.utilities.StockCountHelper.ebxUseableActual;
import static com.metrix.architecture.utilities.StockCountHelper.filter;
import static com.metrix.architecture.utilities.StockCountHelper.recyclerViewSerialLots;
import static com.metrix.architecture.utilities.StockCountHelper.next;
import static com.metrix.architecture.utilities.StockCountHelper.partData;
import static com.metrix.architecture.utilities.StockCountHelper.previous;
import static com.metrix.architecture.utilities.StockCountHelper.runId;
import static com.metrix.architecture.utilities.StockCountHelper.selectedIndex;
import static com.metrix.architecture.utilities.StockCountHelper.serialAndLotData;
import static com.metrix.architecture.utilities.StockCountHelper.toRemove;
import static com.metrix.architecture.utilities.StockCountHelper.txtBinId;
import static com.metrix.architecture.utilities.StockCountHelper.txtLocation;
import static com.metrix.architecture.utilities.StockCountHelper.txtLotId;
import static com.metrix.architecture.utilities.StockCountHelper.txtLotUnusable;
import static com.metrix.architecture.utilities.StockCountHelper.txtLotUsable;
import static com.metrix.architecture.utilities.StockCountHelper.txtNavBar;
import static com.metrix.architecture.utilities.StockCountHelper.txtPartId;
import static com.metrix.architecture.utilities.StockCountHelper.txtSerialId;
import static com.metrix.architecture.utilities.StockCountHelper.txtUsable;
//import static com.metrix.architecture.utilities.StockCountHelper.txtViewList;
import static com.metrix.architecture.utilities.StockCountHelper.unconfirmedCount;


/**
 * Created by hesplk on 4/8/2016.
 */
public class StockCountDetail extends MetrixActivity {
    private SimpleRecyclerViewAdapter mStockSimpleAdapter;
    private String[] mStockFrom;
    private int[] mStockTo;
    private int[] mHiddenList;
    private BottomOffsetDecoration mBottomOffset;
    private List<FloatingActionButton> mFABList;
    private List<FloatingActionButton> mFABsToShow;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stock_count_detail);
        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
        recyclerView.setNestedScrollingEnabled(false);
    }

    public void onStart() {
        super.onStart();
        if(!MetrixStringHelper.isNullOrEmpty(ebxSerialId.getText().toString()) || !MetrixStringHelper.isNullOrEmpty(ebxLotId.getText().toString())) {
            mLayout = (ViewGroup) findViewById(R.id.table_layout);
            wireupEditTextsForBarcodeScanning(mLayout);
        } else {
            runId = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("stock_count.run_id"));
            filter = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("stock_count.filter"));
            selectedIndex = Integer.parseInt(MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("stock_count.position")));
            String filterClause = "order by stock_count.page_number , stock_count.page_sequence";
            if (!MetrixStringHelper.isNullOrEmpty(filter))
                filterClause = String.format(" and stock_count.confirmed %s %s", filter, filterClause);
            String query = String.format("select stock_count.metrix_row_id, stock_count.place_id, stock_count.part_id,stock_count.confirmed, part.internal_descriptn, stock_count.location, stock_count.bin_id, stock_count.qty_on_hand_usebl, stock_count.qty_on_hand_un_us, stock_count.cnt_on_hand_usebl,stock_count.cnt_on_hand_un_us, stock_count.condition_code,part.serialed, part.lot_identified  from stock_count join part on  stock_count.part_id = part.part_id where run_id = '%s' %s", runId, filterClause);
            ArrayList<Hashtable<String, String>> values = MetrixDatabaseManager.getFieldStringValuesList(query);
            partData = StockCountHelper.mapData(values);
            if(partData != null) allPartsCount = partData.size();

            TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
            if (actionBarTitle != null) {
                if (this.mHandlingErrors) {
                    actionBarTitle.setText(AndroidResourceHelper.getMessage("ErrorActionBarTitle1Arg", MobileGlobal.mErrorInfo.transactionDescription));
                } else {
                    if (!MetrixStringHelper.isNullOrEmpty(runId)) {
                        actionBarTitle.setText(AndroidResourceHelper.getMessage("Run1Arg", runId));
                    }
                }
            }

            mLayout = (ViewGroup) findViewById(R.id.table_layout);
            wireupEditTextsForBarcodeScanning(mLayout);
            setListeners();
            StockCountHelper.setUnconfirmedCount();
            StockCountHelper.setFormData();
            getExistingSerialsAndLots();
            StockCountHelper.updateCount(serialAndLotData);
            handleNavigationButtons();
            StockCountHelper.setNavigatorText();
        }
    }

    protected void defineForm() {
        ArrayList<MetrixTableDef> tableDefs = new ArrayList<MetrixTableDef>();
        MetrixTableDef stockDef = new MetrixTableDef("stock_count", MetrixTransactionTypes.UPDATE);
        tableDefs.add(stockDef);

        mLayout = (ViewGroup) findViewById(R.id.table_layout);
        this.mFormDef = new MetrixFormDef(tableDefs);
    }

    @Override
    protected void setListeners() {
        findViews();
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                navigate("Next");
            }
        });
        previous.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                navigate("Previous");
            }
        });
        chkBoxUsable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chkBoxUsable.isChecked()) {
                    System.out.println("Checked");
                } else {
                    System.out.println("Un-Checked");
                }
            }
        });

        btnViewList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (unconfirmedCount > 0) {
                    Intent intent = MetrixActivityHelper.createActivityIntent(StockCountDetail.this, StockCountPartList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    MetrixPublicCache.instance.addItem("stock_count.run_id", runId);
                    MetrixPublicCache.instance.addItem("stock_count.filter", "is null");
                    MetrixActivityHelper.startNewActivity(StockCountDetail.this, intent);
                }
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StockCountHelper.updatePart(serialAndLotData, StockCountDetail.this);
                if (StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.NONE) {
                    //Refreshing the list to get updated counts.
                    String query = String.format("select stock_count.metrix_row_id, stock_count.place_id, stock_count.part_id,stock_count.confirmed, part.internal_descriptn, stock_count.location, stock_count.bin_id, stock_count.qty_on_hand_usebl, stock_count.qty_on_hand_un_us, stock_count.cnt_on_hand_usebl,stock_count.cnt_on_hand_un_us, stock_count.condition_code,part.serialed, part.lot_identified  from stock_count join part on  stock_count.part_id = part.part_id where run_id = '%s' order by stock_count.page_number , stock_count.page_sequence", runId);
                    ArrayList<Hashtable<String, String>> values = MetrixDatabaseManager.getFieldStringValuesList(query);
                    partData = StockCountHelper.mapData(values);
                }

                if (selectedIndex < allPartsCount - 1) {
                    navigate("Next");
                } else {
                    //No more parts, navigate back
                    finish();
                }
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.SERIALED || StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.SERANDLOTIDENTIFIED) {
                    if (StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.SERANDLOTIDENTIFIED) {
                        if (MetrixStringHelper.isNullOrEmpty(ebxSerialId.getText().toString()) && MetrixStringHelper.isNullOrEmpty(ebxLotId.getText().toString())) {
                            MetrixUIHelper.showSnackbar(StockCountDetail.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("LotSerialError"));
                            return;
                        } else if (MetrixStringHelper.isNullOrEmpty(ebxSerialId.getText().toString())) {
                            MetrixUIHelper.showSnackbar(StockCountDetail.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("SerialError"));
                            return;
                        } else if (MetrixStringHelper.isNullOrEmpty(ebxLotId.getText().toString())) {
                            MetrixUIHelper.showSnackbar(StockCountDetail.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("LotError"));
                            return;
                        }
                    }
                    if (!MetrixStringHelper.isNullOrEmpty(ebxSerialId.getText().toString())) {
                        HashMap<String, String> row = new HashMap<String, String>();
                        if (!StockCountHelper.exist(ebxSerialId.getText().toString(), "stock_count_ser.serial_id", serialAndLotData)) {
                            String usable = StockCountHelper.getUsabilityStatus();
                            mStockTo = new int[]{R.id.serial_id, R.id.lot_id, R.id.usability};
                            mHiddenList = new int[]{R.id.lot_Usable, R.id.lot_unusable};
                            mStockFrom = new String[]{"stock_count_ser.serial_id", "stock_count_ser.lot_id", "stock_count_ser.usable", "stock_count_ser.metrix_row_id"};

                            row.put(mStockFrom[0], ebxSerialId.getText().toString());//serial_id
                            row.put(mStockFrom[1], ebxLotId.getText().toString());//lot_id
                            row.put(mStockFrom[2], usable);//usable
                            serialAndLotData.add(row);
                        } else {
                            MetrixUIHelper.showSnackbar(StockCountDetail.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("StockCountDupeIdError"));
                            return;
                        }
                    } else {
                        MetrixUIHelper.showSnackbar(StockCountDetail.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("SerialError"));
                        return;
                    }
                }
                //Lot identified only
                else if (StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.LOTIDENTIFIED && !MetrixStringHelper.isNullOrEmpty(ebxLotId.getText().toString())) {
                    if (!StockCountHelper.exist(ebxLotId.getText().toString(), "stock_count_lot.lot_id", serialAndLotData)) {
                        HashMap<String, String> row = new HashMap<String, String>();
                        String uCount = "0", unCount = "0";
                        if (!MetrixStringHelper.isNullOrEmpty(ebxLotUsable.getText().toString())) {
                            uCount = ebxLotUsable.getText().toString();
                        }
                        if (!MetrixStringHelper.isNullOrEmpty(ebxLotUnusable.getText().toString())) {
                            unCount = ebxLotUnusable.getText().toString();
                        }

                        mStockTo = new int[]{R.id.lot_id, R.id.lot_Usable, R.id.lot_unusable};
                        mHiddenList = new int[]{R.id.serial_id, R.id.usability};
                        mStockFrom = new String[]{"stock_count_lot.lot_id", "stock_count_lot.cnt_on_hand_usebl", "stock_count_lot.cnt_on_hand_un_us", "stock_count_lot.metrix_row_id"};

                        row.put(mStockFrom[0], ebxLotId.getText().toString());//lot_id
                        row.put(mStockFrom[1], uCount);//cnt_on_hand_usebl
                        row.put(mStockFrom[2], unCount);//cnt_on_hand_un_us
                        serialAndLotData.add(row);
                    } else {
                        MetrixUIHelper.showSnackbar(StockCountDetail.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("StockCountDuplicateLotIdError"));
                        return;
                    }
                } else {
                    MetrixUIHelper.showSnackbar(StockCountDetail.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("LotError"));
                    return;
                }
                StockCountHelper.reset();
                StockCountHelper.updateCount(serialAndLotData);
                if (mStockSimpleAdapter == null) {
                    mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), (int)getResources().getDimension((R.dimen.md_margin)));
                    mStockSimpleAdapter = new SimpleRecyclerViewAdapter(serialAndLotData, R.layout.stock_count_serial_lot_item, mStockFrom, mStockTo, mHiddenList, null);
                    recyclerViewSerialLots.addItemDecoration(mBottomOffset);
                    recyclerViewSerialLots.setAdapter(mStockSimpleAdapter);
                } else {
                    mStockSimpleAdapter.updateData(serialAndLotData);
                }
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(serialAndLotData != null) {
                    serialAndLotData.removeAll(toRemove);
                    if (serialAndLotData.size() > 0) {
                        if (StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.LOTIDENTIFIED ||
                                StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.SERANDLOTIDENTIFIED ||
                                StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.SERIALED) {
                            if (mStockSimpleAdapter == null) {
                                mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), (int)getResources().getDimension((R.dimen.md_margin)));
                                mStockSimpleAdapter = new SimpleRecyclerViewAdapter(serialAndLotData, R.layout.stock_count_serial_lot_item, mStockFrom, mStockTo, mHiddenList, null);
                                recyclerViewSerialLots.addItemDecoration(mBottomOffset);
                                recyclerViewSerialLots.setAdapter(mStockSimpleAdapter);
                            } else {
                                mStockSimpleAdapter.updateData(serialAndLotData);
                            }
                        }
                    } else {
                        if (mStockSimpleAdapter != null)
                            mStockSimpleAdapter.updateData(new ArrayList<>());
                    }
                }
                StockCountHelper.reset();
                StockCountHelper.updateCount(serialAndLotData);
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.NONE) {
                    StockCountHelper.reset();
                }
            }
        });
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        if (listAdapter != null) {
            int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
            int totalHeight = 0;
            View view = null;
            for (int i = 0; i < listAdapter.getCount(); i++) {
                view = listAdapter.getView(i, view, listView);
                if (i == 0)
                    view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

                view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
                totalHeight += view.getMeasuredHeight();
            }

            params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));

        } else {
            params.height = 0;
        }
        listView.setLayoutParams(params);
    }

    private void navigate(String direction) {

        if (MetrixStringHelper.valueIsEqual("Next", direction)) {
            if (selectedIndex < allPartsCount - 1) {
                selectedIndex += 1;
            }
        }
        if (MetrixStringHelper.valueIsEqual("Previous", direction)) {
            if (selectedIndex >= 1) {
                selectedIndex -= 1;
            }
        }

        StockCountHelper.setNavigatorText();
        StockCountHelper.setFormData();
        if (mStockSimpleAdapter != null)
            mStockSimpleAdapter.updateData(new ArrayList<>());
        StockCountHelper.reset();
        getExistingSerialsAndLots();
        StockCountHelper.updateCount(serialAndLotData);
        handleNavigationButtons();
    }

    private void getExistingSerialsAndLots() {
        String placeId = "";
        String location = "";
        String binId = "";
        String partId = "";
        if(partData != null && partData.size() > selectedIndex) {
            placeId = partData.get(selectedIndex).get("stock_count.place_id");
            location = partData.get(selectedIndex).get("stock_count.location");
            binId = partData.get(selectedIndex).get("stock_count.bin_id");
            partId = partData.get(selectedIndex).get("stock_count.part_id");
        }
        serialAndLotData = new ArrayList<HashMap<String, String>>();

        String serialQuery = String.format("select metrix_row_id, serial_id,usable,lot_id from stock_count_ser where place_id='%s' and location ='%s' and bin_id ='%s' and part_id ='%s'", placeId, location, binId, partId);
        ArrayList<Hashtable<String, String>> existingSerials = MetrixDatabaseManager.getFieldStringValuesList(serialQuery);

        if (StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.LOTIDENTIFIED) {
            String lotsQuery = String.format("select metrix_row_id, cnt_on_hand_usebl, cnt_on_hand_un_us, lot_id from stock_count_lot where place_id='%s' and location ='%s' and bin_id ='%s' and part_id ='%s'", placeId, location, binId, partId);
            ArrayList<Hashtable<String, String>> existingLots = MetrixDatabaseManager.getFieldStringValuesList(lotsQuery);
            if (existingLots != null && existingLots.size() > 0) {
                mStockTo = new int[]{R.id.lot_id, R.id.lot_Usable, R.id.lot_unusable};
                mHiddenList = new int[]{R.id.serial_id, R.id.usability};
                mStockFrom = new String[]{"stock_count_lot.lot_id", "stock_count_lot.cnt_on_hand_usebl", "stock_count_lot.cnt_on_hand_un_us", "stock_count_lot.metrix_row_id"};

                for (int count = 0; count <= existingLots.size() - 1; count++) {
                    HashMap<String, String> row = new HashMap<String, String>();
                    row.put(mStockFrom[0], existingLots.get(count).get("lot_id"));//lot_id
                    row.put(mStockFrom[1], existingLots.get(count).get("cnt_on_hand_usebl"));//cnt_on_hand_usebl
                    row.put(mStockFrom[2], existingLots.get(count).get("cnt_on_hand_un_us"));//cnt_on_hand_un_us
                    row.put(mStockFrom[3], existingLots.get(count).get("metrix_row_id"));//metrix_row_id
                    serialAndLotData.add(row);
                }
                if (mStockSimpleAdapter == null) {
                    mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), (int)getResources().getDimension((R.dimen.md_margin)));
                    mStockSimpleAdapter = new SimpleRecyclerViewAdapter(serialAndLotData, R.layout.stock_count_serial_lot_item, mStockFrom, mStockTo, mHiddenList, null);
                    recyclerViewSerialLots.addItemDecoration(mBottomOffset);
                    recyclerViewSerialLots.setAdapter(mStockSimpleAdapter);
                } else {
                    mStockSimpleAdapter.updateData(serialAndLotData);
                }
            }
        }

        if (existingSerials != null && existingSerials.size() > 0) {
            mStockTo = new int[]{R.id.serial_id, R.id.lot_id, R.id.usability};
            mHiddenList = new int[]{R.id.lot_Usable, R.id.lot_unusable};
            mStockFrom = new String[]{"stock_count_ser.serial_id", "stock_count_ser.lot_id", "stock_count_ser.usable", "stock_count_ser.metrix_row_id"};

            for (int count = 0; count <= existingSerials.size() - 1; count++) {
                HashMap<String, String> row = new HashMap<String, String>();
                row.put(mStockFrom[0], existingSerials.get(count).get("serial_id"));//serial_id
                row.put(mStockFrom[1], existingSerials.get(count).get("lot_id"));//lot_id
                row.put(mStockFrom[2], existingSerials.get(count).get("usable"));//usable
                row.put(mStockFrom[3], existingSerials.get(count).get("metrix_row_id"));//metrix_row_id
                serialAndLotData.add(row);
            }
            if (mStockSimpleAdapter == null) {
                mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), (int)getResources().getDimension((R.dimen.md_margin)));
                mStockSimpleAdapter = new SimpleRecyclerViewAdapter(serialAndLotData, R.layout.stock_count_serial_lot_item, mStockFrom, mStockTo, mHiddenList, null);
                recyclerViewSerialLots.addItemDecoration(mBottomOffset);
                recyclerViewSerialLots.setAdapter(mStockSimpleAdapter);
            } else {
                mStockSimpleAdapter.updateData(serialAndLotData);
            }
        }
    }

    public void listViewItemSelect(View v) {
        CheckBox cb = (CheckBox) v;
        int listViewPosition = Integer.parseInt(cb.getTag().toString());
        if(serialAndLotData != null && serialAndLotData.size() > listViewPosition) {
            if (toRemove != null) {
                if (!toRemove.contains(serialAndLotData.get(listViewPosition))) {
                    toRemove.add(serialAndLotData.get(listViewPosition));
                } else {
                    toRemove.remove(serialAndLotData.get(listViewPosition));
                }
            }
        }
    }

    private void handleNavigationButtons() {
        if (selectedIndex == 0) {
            if (allPartsCount == 1) {
                previous.setVisibility(View.GONE);
                next.setVisibility(View.GONE);
            } else {
                previous.setVisibility(View.GONE);
                next.setVisibility(View.VISIBLE);
            }
        } else if (selectedIndex > 0) {
            if (selectedIndex == allPartsCount - 1) {
                previous.setVisibility(View.VISIBLE);
                next.setVisibility(View.GONE);
            } else {
                previous.setVisibility(View.VISIBLE);
                next.setVisibility(View.VISIBLE);
            }
        }
    }

    private void findViews() {
        //Nav Bar
        next = (ImageView) findViewById(R.id.btn_next);
        previous = (ImageView) findViewById(R.id.btn_previous);
        txtNavBar = (TextView) findViewById(R.id.txt_navbar);
        //Labels
        txtSerialId = (TextView) StockCountHelper.getLabel("custom", "serial_id", mFormDef, mLayout);
        txtLotId = (TextView) StockCountHelper.getLabel("custom", "lot_id", mFormDef, mLayout);
        txtPartId = (TextView) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "part_id");
        txtLocation = (TextView) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "location");
        txtBinId = (TextView) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "bin_id");
        txtUsable = (TextView) StockCountHelper.getLabel("custom", "usable", mFormDef, mLayout);

        txtLotUnusable = (TextView) StockCountHelper.getLabel("custom", "lot_usable", mFormDef, mLayout);
        txtLotUsable = (TextView) StockCountHelper.getLabel("custom", "lot_un_usable", mFormDef, mLayout);

        //Edit Boxes and Check Boxes
        ebxSerialId = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "serial_id");
        ebxLotId = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "lot_id");

        chkBoxUsable = (CheckBox) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "usable");

        ebxLotUsable = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "lot_usable");
        ebxLotUnusable = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "lot_un_usable");
        ebxUnuseableActual = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "cnt_on_hand_un_us");
        ebxUseableActual = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "cnt_on_hand_usebl");

        //Static Controls
        btnAdd = (FloatingActionButton) findViewById(R.id.btn_stock_add);
        btnConfirm = (FloatingActionButton) findViewById(R.id.btn_stock_confirm);
        btnDelete = (FloatingActionButton) findViewById(R.id.btn_stock_delete);
        btnReset = (FloatingActionButton) findViewById(R.id.btn_stock_reset);
        btnViewList = (Button) findViewById(R.id.btn_viewList);
        recyclerViewSerialLots = findViewById(R.id.recyclerView);

        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        mFABList.add(btnAdd);
        mFABList.add(btnConfirm);
        mFABList.add(btnDelete);
        mFABList.add(btnReset);

        mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));

        if(serialAndLotData == null || serialAndLotData.isEmpty())
            mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), generateOffsetForFABs(mFABList));
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable("currentSerialAndLotData", new StockCountData(serialAndLotData));
        MetrixPublicCache.instance.addItem("stock_count.position", selectedIndex);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey("currentSerialAndLotData")) {
            StockCountData stockCountData = (StockCountData)savedInstanceState.getSerializable("currentSerialAndLotData");
            if(stockCountData != null){
                List<HashMap<String, String>> list = stockCountData.getListObjects();
                serialAndLotData = list;

                if (StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.LOTIDENTIFIED) {
                    mStockTo = new int[]{R.id.lot_id, R.id.lot_Usable, R.id.lot_unusable};
                    mHiddenList = new int[]{R.id.serial_id, R.id.usability};
                    mStockFrom = new String[]{"stock_count_lot.lot_id", "stock_count_lot.cnt_on_hand_usebl", "stock_count_lot.cnt_on_hand_un_us", "stock_count_lot.metrix_row_id"};
                }
                else if(StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.SERANDLOTIDENTIFIED ||
                        StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.SERIALED){
                    mStockTo = new int[]{R.id.serial_id, R.id.lot_id, R.id.usability};
                    mHiddenList = new int[]{R.id.lot_Usable, R.id.lot_unusable};
                    mStockFrom = new String[]{"stock_count_ser.serial_id", "stock_count_ser.lot_id", "stock_count_ser.usable", "stock_count_ser.metrix_row_id"};
                }
                if (mStockSimpleAdapter == null) {
                    mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), (int)getResources().getDimension((R.dimen.md_margin)));
                    mStockSimpleAdapter = new SimpleRecyclerViewAdapter(serialAndLotData, R.layout.stock_count_serial_lot_item, mStockFrom, mStockTo, mHiddenList, null);
                    recyclerViewSerialLots.addItemDecoration(mBottomOffset);
                    recyclerViewSerialLots.setAdapter(mStockSimpleAdapter);
                } else {
                    mStockSimpleAdapter.updateData(serialAndLotData);
                }
            }
        }
    }
}

