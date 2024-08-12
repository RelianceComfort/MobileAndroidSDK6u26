package com.metrix.architecture.utilities;

import android.app.Activity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by hesplk on 10/12/2016.
 */
public class StockCountHelper {
    public enum PART_TYPE {
        SERIALED,
        LOTIDENTIFIED,
        SERANDLOTIDENTIFIED,
        NONE
    }

//    public static TextView txtViewList;
    public static EditText ebxUseableActual;
    public static EditText ebxUnuseableActual;
    public static CheckBox chkBoxUsable;
    public static TextView txtNavBar;

    public static TextView txtSerialId;
    public static TextView txtLotUsable;
    public static TextView txtLotUnusable;
    public static TextView txtLotId;
    public static TextView txtUsable;
    public static ImageView next;
    public static ImageView previous;

    public static FloatingActionButton btnAdd;
    public static FloatingActionButton btnConfirm;
    public static FloatingActionButton btnDelete;
    public static FloatingActionButton btnReset;
    public static Button btnViewList;

    public static TextView txtPartId;
    public static TextView txtLocation;
    public static TextView txtBinId;
    public static EditText ebxSerialId;
    public static EditText ebxLotId;
    public static EditText ebxLotUsable;
    public static EditText ebxLotUnusable;

    public static RecyclerView recyclerViewSerialLots;

    public static String serialed;
    public static String lotIdentified;
    public static String runId;
    public static int selectedIndex = 0;
    public static int unconfirmedCount = 0;
    public static int allPartsCount = 0;
    public static String usableCount = "0";
    public static String unusableCount = "0";
    public static String filter;

    public static List<HashMap<String, String>> toRemove;
    public static List<HashMap<String, String>> partData;
    public static List<HashMap<String, String>> serialAndLotData;

    public static List<HashMap<String, String>> mapData(ArrayList<Hashtable<String, String>> values) {
        List<HashMap<String, String>> listToAdd = new ArrayList<HashMap<String, String>>();
        String[] mStockKeys = new String[]{"stock_count.part_id", "part.internal_descriptn", "stock_count.location", "stock_count.bin_id", "stock_count.qty_on_hand_usebl", "stock_count.qty_on_hand_un_us", "part.serialed", "part.lot_identified", "stock_count.confirmed", "stock_count.place_id", "stock_count.metrix_row_id", "stock_count.cnt_on_hand_usebl", "stock_count.cnt_on_hand_un_us", "stock_count.condition_code"};

        if(values != null && values.size() > 0) {
            for (int count = 0; count <= values.size() - 1; count++) {
                HashMap<String, String> row = new HashMap<String, String>();
                row.put(mStockKeys[0], values.get(count).get("part_id"));
                row.put(mStockKeys[1], values.get(count).get("internal_descriptn"));
                row.put(mStockKeys[2], values.get(count).get("location"));
                row.put(mStockKeys[3], values.get(count).get("bin_id"));
                row.put(mStockKeys[4], values.get(count).get("qty_on_hand_usebl"));
                row.put(mStockKeys[5], values.get(count).get("qty_on_hand_un_us"));
                row.put(mStockKeys[6], values.get(count).get("serialed"));
                row.put(mStockKeys[7], values.get(count).get("lot_identified"));
                row.put(mStockKeys[8], values.get(count).get("confirmed"));
                row.put(mStockKeys[9], values.get(count).get("place_id"));
                row.put(mStockKeys[10], values.get(count).get("metrix_row_id"));
                row.put(mStockKeys[11], values.get(count).get("cnt_on_hand_usebl"));
                row.put(mStockKeys[12], values.get(count).get("cnt_on_hand_un_us"));
                row.put(mStockKeys[13], values.get(count).get("condition_code"));
                listToAdd.add(row);
            }
        }
        return listToAdd;
    }

    public static View getLabel(String tableName, String columnName, MetrixFormDef mFormDef, ViewGroup mLayout) {
        int labelId = 0;
        boolean foundControl = false;
        for (MetrixTableDef tableDef : mFormDef.tables) {
            if (tableDef.tableName.compareToIgnoreCase(tableName) == 0) {
                for (MetrixColumnDef columnDef : tableDef.columns) {
                    if (columnDef.columnName.compareToIgnoreCase(columnName) == 0) {
                        labelId = columnDef.labelId;
                        foundControl = true;
                        break;
                    }
                }
            }
            if (foundControl) break;
        }

        View label = MetrixControlAssistant.getControl(labelId, mLayout);
        return label;
    }

    public static void updatePart(List<HashMap<String, String>> serialAndLotData, Activity activity) {
        MetrixSqlData partSqlData = new MetrixSqlData("stock_count", MetrixTransactionTypes.UPDATE);

        if(partData != null && partData.size() > selectedIndex) {
            partSqlData.dataFields.add(new DataField("metrix_row_id", partData.get(selectedIndex).get("stock_count.metrix_row_id")));
            partSqlData.dataFields.add(new DataField("place_id", partData.get(selectedIndex).get("stock_count.place_id")));
            partSqlData.dataFields.add(new DataField("location", partData.get(selectedIndex).get("stock_count.location")));
            partSqlData.dataFields.add(new DataField("bin_id", partData.get(selectedIndex).get("stock_count.bin_id")));
            partSqlData.dataFields.add(new DataField("part_id", partData.get(selectedIndex).get("stock_count.part_id")));
            partSqlData.dataFields.add(new DataField("condition_code", partData.get(selectedIndex).get("stock_count.condition_code")));
        }
        //No Serial or Lot Id
        if (StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.NONE) {
	        if(ebxUseableActual != null && ebxUseableActual.getText() != null)
		        usableCount = MetrixStringHelper.isNullOrEmpty(ebxUseableActual.getText().toString()) ? "0" : ebxUseableActual.getText().toString();
	        if(ebxUnuseableActual != null && ebxUnuseableActual.getText() != null)
		        unusableCount = MetrixStringHelper.isNullOrEmpty(ebxUnuseableActual.getText().toString()) ? "0" : ebxUnuseableActual.getText().toString();
        }

        String countedDate = MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true);

        partSqlData.dataFields.add(new DataField("cnt_on_hand_usebl", usableCount));
        partSqlData.dataFields.add(new DataField("cnt_on_hand_un_us", unusableCount));
        partSqlData.dataFields.add(new DataField("count_dttm", countedDate));
        partSqlData.dataFields.add(new DataField("counted_by", User.getUser().personId));
        partSqlData.dataFields.add(new DataField("confirmed", "Y"));

        if(partData != null && partData.size() > selectedIndex)
            partSqlData.filter = "metrix_row_id = " + partData.get(selectedIndex).get("stock_count.metrix_row_id");

        ArrayList<MetrixSqlData> stockCountTransaction = new ArrayList<MetrixSqlData>();
        stockCountTransaction.add(partSqlData);

        MetrixTransaction transactionInfo = new MetrixTransaction();
        MetrixUpdateManager.update(stockCountTransaction, true, transactionInfo, AndroidResourceHelper.getMessage("StockCount"), null);
        updateSerialsAndLots(getPartType(), serialAndLotData, activity);
        StockCountHelper.setUnconfirmedCount();
    }

    public static void updateSerialsAndLots(PART_TYPE partType, List<HashMap<String, String>> serialAndLotData, Activity activity) {
        String placeId = "";
        String location = "";
        String binId = "";
        String partId = "";
        String conditionCode = "";
        if(partData != null && partData.size() > selectedIndex) {
            placeId = partData.get(selectedIndex).get("stock_count.place_id");
            location = partData.get(selectedIndex).get("stock_count.location");
            binId = partData.get(selectedIndex).get("stock_count.bin_id");
            partId = partData.get(selectedIndex).get("stock_count.part_id");
            conditionCode = partData.get(selectedIndex).get("stock_count.condition_code");
        }
        if(serialAndLotData != null) {
            //Serialed
            if (partType == PART_TYPE.SERIALED || partType == PART_TYPE.SERANDLOTIDENTIFIED) {
                for (int count = 0; count <= serialAndLotData.size() - 1; count++) {
                    MetrixSqlData serialData;

                    //Confirmed items are only able to be deleted
                    if (MetrixStringHelper.isNullOrEmpty(serialAndLotData.get(count).get("stock_count_ser.metrix_row_id"))) {
                        serialData = new MetrixSqlData("stock_count_ser", MetrixTransactionTypes.INSERT);

                        serialData.dataFields.add(new DataField("place_id", placeId));
                        serialData.dataFields.add(new DataField("location", location));
                        serialData.dataFields.add(new DataField("bin_id", binId));
                        serialData.dataFields.add(new DataField("part_id", partId));
                        serialData.dataFields.add(new DataField("condition_code", conditionCode));

                        serialData.dataFields.add(new DataField("serial_id", serialAndLotData.get(count).get("stock_count_ser.serial_id")));
                        serialData.dataFields.add(new DataField("usable", serialAndLotData.get(count).get("stock_count_ser.usable")));
                        //Serialed & Lot identified
                        if (partType == PART_TYPE.SERANDLOTIDENTIFIED) {
                            serialData.dataFields.add(new DataField("lot_id", serialAndLotData.get(count).get("stock_count_ser.lot_id")));
                        }

                        ArrayList<MetrixSqlData> stockSerialTransaction = new ArrayList<MetrixSqlData>();
                        stockSerialTransaction.add(serialData);

                        MetrixTransaction transactionInfo = new MetrixTransaction();

                        MetrixUpdateManager.update(stockSerialTransaction, true, transactionInfo, AndroidResourceHelper.getMessage("StockCountSerial"), null);
                    }
                }
            }
            //Lot identified only
            else if (partType == PART_TYPE.LOTIDENTIFIED) {
                for (int count = 0; count <= serialAndLotData.size() - 1; count++) {
                    MetrixSqlData lotData;

                    if (MetrixStringHelper.isNullOrEmpty(serialAndLotData.get(count).get("stock_count_lot.metrix_row_id"))) {
                        lotData = new MetrixSqlData("stock_count_lot", MetrixTransactionTypes.INSERT);

                        lotData.dataFields.add(new DataField("place_id", placeId));
                        lotData.dataFields.add(new DataField("location", location));
                        lotData.dataFields.add(new DataField("bin_id", binId));
                        lotData.dataFields.add(new DataField("part_id", partId));
                        lotData.dataFields.add(new DataField("condition_code", conditionCode));

                        lotData.dataFields.add(new DataField("cnt_on_hand_usebl", serialAndLotData.get(count).get("stock_count_lot.cnt_on_hand_usebl")));
                        lotData.dataFields.add(new DataField("cnt_on_hand_un_us", serialAndLotData.get(count).get("stock_count_lot.cnt_on_hand_un_us")));
                        lotData.dataFields.add(new DataField("lot_id", serialAndLotData.get(count).get("stock_count_lot.lot_id")));

                        ArrayList<MetrixSqlData> stockLotTransaction = new ArrayList<MetrixSqlData>();
                        stockLotTransaction.add(lotData);

                        MetrixTransaction transactionInfo = new MetrixTransaction();

                        MetrixUpdateManager.update(stockLotTransaction, true, transactionInfo, AndroidResourceHelper.getMessage("StockCountLot"), null);
                    }
                }
            }
        }
        //Check if the user has any items to be deleted from the DB
        deleteSerialsOrLotsFromDb(activity, partType);
    }

    public static void deleteSerialsOrLotsFromDb(Activity activity, PART_TYPE partType) {

        if (toRemove != null && toRemove.size() > 0) {
            if (partType == PART_TYPE.SERIALED || partType == PART_TYPE.SERANDLOTIDENTIFIED) {
                for (int count = 0; count <= toRemove.size() - 1; count++) {
                    String metrixRowId = toRemove.get(count).get("stock_count_ser.metrix_row_id");
                    //Update the client Db only if the item has been written to the Db. Otherwise, removing from the display list is sufficient.
                    if (metrixRowId != null) {
                        ArrayList<Hashtable<String, String>> existingData = MetrixDatabaseManager.getFieldStringValuesList(String.format("select place_id,location,bin_id,part_id, condition_code from stock_count_ser where metrix_row_id = %s", metrixRowId));
                        String placeId = existingData.get(0).get("place_id");
                        String location = existingData.get(0).get("location");
                        String binId = existingData.get(0).get("bin_id");
                        String partId = existingData.get(0).get("part_id");
                        String conditionCode = existingData.get(0).get("condition_code");
                        //Update the client Db only if the serial has been confirmed. Otherwise, only remove from the display list.
                        Hashtable<String, String> thePrimaryKeys = new Hashtable<String, String>();
                        thePrimaryKeys.put("place_id", placeId);
                        thePrimaryKeys.put("location", location);
                        thePrimaryKeys.put("bin_id", binId);
                        thePrimaryKeys.put("part_id", partId);
                        thePrimaryKeys.put("condition_code", conditionCode);
                        thePrimaryKeys.put("serial_id", toRemove.get(count).get("stock_count_ser.serial_id"));

                        MetrixTransaction transactionInfo = new MetrixTransaction();
                        boolean serialTest = MetrixUpdateManager.delete(activity, "stock_count_ser", metrixRowId, thePrimaryKeys, AndroidResourceHelper.getMessage("StockCountDelete"), transactionInfo);
                    }
                }
            }
            //Lot identified only
            else if (partType == PART_TYPE.LOTIDENTIFIED) {
                for (int count = 0; count <= toRemove.size() - 1; count++) {
                    String metrixRowId = toRemove.get(count).get("stock_count_lot.metrix_row_id");
                    //Update the client Db only if the item has been written to the Db. Otherwise, removing from the display list is sufficient.
                    if (metrixRowId != null) {
                        ArrayList<Hashtable<String, String>> existingData = MetrixDatabaseManager.getFieldStringValuesList(String.format("select place_id,location,bin_id,part_id, condition_code  from stock_count_lot where metrix_row_id = %s", metrixRowId));

                        String placeId = existingData.get(0).get("place_id");
                        String location = existingData.get(0).get("location");
                        String binId = existingData.get(0).get("bin_id");
                        String partId = existingData.get(0).get("part_id");
                        String conditionCode = existingData.get(0).get("condition_code");

                        Hashtable<String, String> thePrimaryKeys = new Hashtable<String, String>();
                        thePrimaryKeys.put("place_id", placeId);
                        thePrimaryKeys.put("location", location);
                        thePrimaryKeys.put("bin_id", binId);
                        thePrimaryKeys.put("part_id", partId);
                        thePrimaryKeys.put("condition_code", conditionCode);
                        thePrimaryKeys.put("lot_id", toRemove.get(count).get("stock_count_lot.lot_id"));

                        MetrixTransaction transactionInfo = new MetrixTransaction();
                        MetrixUpdateManager.delete(activity, "stock_count_lot", metrixRowId, thePrimaryKeys, AndroidResourceHelper.getMessage("StockCountDelete"), transactionInfo);
                    }
                }
            }
        }
        toRemove = new ArrayList<HashMap<String, String>>();
    }

    public static void updateCount(List<HashMap<String, String>> serialAndLotData) {
        if (StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.SERIALED || StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.SERANDLOTIDENTIFIED) {
	        int uTemp = 0;
	        int unTemp = 0;

	        int usableCnt = MetrixStringHelper.isNullOrEmpty(usableCount) ? 0 : Integer.valueOf(usableCount);
	        int unUsableCnt = MetrixStringHelper.isNullOrEmpty(unusableCount) ? 0 : Integer.valueOf(unusableCount);
	        if(serialAndLotData != null && serialAndLotData.size() > 0) {
                for (int count = 0; count <= serialAndLotData.size() - 1; count++) {
                    String usable = serialAndLotData.get(count).get("stock_count_ser.usable");
                    if (MetrixStringHelper.valueIsEqual(usable, "Y")) {
	                    uTemp = uTemp + 1;
                    } else if (MetrixStringHelper.valueIsEqual(usable, "N")) {
	                    unTemp = unTemp + 1;
                    }
                }
		        usableCnt = usableCnt + uTemp;
		        unUsableCnt = unUsableCnt + unTemp;

		        usableCount = String.valueOf(usableCnt);
		        unusableCount = String.valueOf(unUsableCnt);
            }
        }
        //Lot identified only
        else if (StockCountHelper.getPartType() == StockCountHelper.PART_TYPE.LOTIDENTIFIED && serialAndLotData.size() > 0) {
            int uTemp = 0;
            int unTemp = 0;
            if(serialAndLotData != null && serialAndLotData.size() > 0) {
                for (int count = 0; count <= serialAndLotData.size() - 1; count++) {
                    String individualUsable = serialAndLotData.get(count).get("stock_count_lot.cnt_on_hand_usebl");
                    String individualUnusable = serialAndLotData.get(count).get("stock_count_lot.cnt_on_hand_un_us");

                    uTemp = uTemp + Integer.parseInt(individualUsable);
                    unTemp = unTemp + Integer.parseInt(individualUnusable);
                }
            }
            usableCount = String.valueOf(uTemp);
            unusableCount = String.valueOf(unTemp);
        } else {
            if(partData != null && partData.size() > selectedIndex) {
	            if(StockCountHelper.getPartType() == PART_TYPE.NONE){
		            usableCount = (!MetrixStringHelper.isNullOrEmpty(partData.get(selectedIndex).get("stock_count.cnt_on_hand_usebl")) ? partData.get(selectedIndex).get("stock_count.cnt_on_hand_usebl") : "");
		            unusableCount = (!MetrixStringHelper.isNullOrEmpty(partData.get(selectedIndex).get("stock_count.cnt_on_hand_un_us")) ? partData.get(selectedIndex).get("stock_count.cnt_on_hand_un_us") : "");
	            }
				else {
		            usableCount = (!MetrixStringHelper.isNullOrEmpty(partData.get(selectedIndex).get("stock_count.cnt_on_hand_usebl")) ? partData.get(selectedIndex).get("stock_count.cnt_on_hand_usebl") : "0");
		            unusableCount = (!MetrixStringHelper.isNullOrEmpty(partData.get(selectedIndex).get("stock_count.cnt_on_hand_un_us")) ? partData.get(selectedIndex).get("stock_count.cnt_on_hand_un_us") : "0");
	            }
            }
        }
        ebxUseableActual.setText(usableCount);
        ebxUnuseableActual.setText(unusableCount);
    }

    public static boolean exist(String value, String key, List<HashMap<String, String>> serialAndLotData) {
        boolean found = false;
        for (int count = 0; count <= serialAndLotData.size() - 1; count++) {
            String serialId = serialAndLotData.get(count).get(key);
            if (MetrixStringHelper.valueIsEqual(serialId, value)) {
                found = true;
                break;
            }
        }
        return found;
    }

    public static String getUsabilityStatus() {
        if (chkBoxUsable.isChecked()) {
            return "Y";
        } else {
            return "N";
        }
    }

    public static void setUnconfirmedCount() {
        String whereClause = String.format("run_id = '%s' and confirmed is null", runId);
        unconfirmedCount = MetrixDatabaseManager.getCount("stock_count", whereClause);
        btnViewList.setText(AndroidResourceHelper.getMessage("List1Arg", unconfirmedCount));
    }

    public static void setNavigatorText() {
        txtNavBar.setText(AndroidResourceHelper.getMessage("RecordOf2Args", (selectedIndex + 1), allPartsCount));
    }

    public static void setFormData() {
        toRemove = new ArrayList<HashMap<String, String>>();
        if(partData != null && partData.size() > selectedIndex) {
            txtPartId.setText(partData.get(selectedIndex).get("stock_count.part_id"));
            txtLocation.setText(partData.get(selectedIndex).get("stock_count.location"));
            txtBinId.setText(partData.get(selectedIndex).get("stock_count.bin_id"));

            serialed = partData.get(selectedIndex).get("part.serialed");
            lotIdentified = partData.get(selectedIndex).get("part.lot_identified");
        }

        ebxUnuseableActual.setText(String.valueOf(0));
        ebxUseableActual.setText(String.valueOf(0));

        btnViewList.setText(AndroidResourceHelper.getMessage("List1Arg", unconfirmedCount));
        populateControls();
    }

    private static void populateControls() {
        PART_TYPE partType = getPartType();
        //Serialed or Serialed & Lot identified
        if (partType == PART_TYPE.SERIALED || partType == PART_TYPE.SERANDLOTIDENTIFIED) {
            ebxSerialId.setVisibility(View.VISIBLE);
            txtSerialId.setVisibility(View.VISIBLE);
            chkBoxUsable.setVisibility(View.VISIBLE);
	        //KEST-2139 #I)
	        chkBoxUsable.setChecked(true);
            txtUsable.setVisibility(View.VISIBLE);
            ebxUseableActual.setEnabled(false);
            ebxUnuseableActual.setEnabled(false);

            //Serialed & Lot identified
            if (partType == PART_TYPE.SERANDLOTIDENTIFIED) {
                ebxLotId.setVisibility(View.VISIBLE);
                txtLotUsable.setVisibility(View.VISIBLE);
                txtLotId.setVisibility(View.VISIBLE);
                txtLotUnusable.setVisibility(View.VISIBLE);
            } else {
                ebxLotId.setVisibility(View.GONE);
                txtLotId.setVisibility(View.GONE);
            }

            txtLotUsable.setVisibility(View.GONE);
            txtLotUnusable.setVisibility(View.GONE);
            ebxLotUsable.setVisibility(View.GONE);
            ebxLotUnusable.setVisibility(View.GONE);
            btnReset.setVisibility(View.GONE);
            btnAdd.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
        }
        //Non serialed.
        //Lot identified.
        else if (partType == PART_TYPE.LOTIDENTIFIED) {
            ebxLotId.setVisibility(View.VISIBLE);
            txtLotId.setVisibility(View.VISIBLE);
            txtLotUsable.setVisibility(View.VISIBLE);
            txtLotUnusable.setVisibility(View.VISIBLE);
            ebxLotUsable.setVisibility(View.VISIBLE);
            ebxLotUnusable.setVisibility(View.VISIBLE);
            ebxUseableActual.setEnabled(false);
            ebxUnuseableActual.setEnabled(false);

            ebxSerialId.setVisibility(View.GONE);

            txtSerialId.setVisibility(View.GONE);

            chkBoxUsable.setVisibility(View.GONE);
            txtUsable.setVisibility(View.GONE);
            btnReset.setVisibility(View.GONE);
            btnAdd.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
        }
        //Non Serialed.
        //Non Lot identified.
        else {
            ebxLotId.setVisibility(View.GONE);
            txtLotId.setVisibility(View.GONE);
            txtLotUsable.setVisibility(View.GONE);
            txtLotUnusable.setVisibility(View.GONE);
            ebxLotUsable.setVisibility(View.GONE);
            ebxLotUnusable.setVisibility(View.GONE);
            ebxUseableActual.setEnabled(true);
            ebxUnuseableActual.setEnabled(true);

            ebxSerialId.setVisibility(View.GONE);
            txtSerialId.setVisibility(View.GONE);

            chkBoxUsable.setVisibility(View.GONE);
            txtUsable.setVisibility(View.GONE);
            btnReset.setVisibility(View.VISIBLE);
            btnAdd.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }
    }

    public static void reset() {
        if(ebxSerialId != null) ebxSerialId.setText("");
        if(ebxLotId != null) ebxLotId.setText("");

	    if(getPartType() == PART_TYPE.NONE) {
		    if(partData != null && partData.size() > selectedIndex) {
			    usableCount = (!MetrixStringHelper.isNullOrEmpty(partData.get(selectedIndex).get("stock_count.cnt_on_hand_usebl")) ? partData.get(selectedIndex).get("stock_count.cnt_on_hand_usebl") : "");
			    unusableCount = (!MetrixStringHelper.isNullOrEmpty(partData.get(selectedIndex).get("stock_count.cnt_on_hand_un_us")) ? partData.get(selectedIndex).get("stock_count.cnt_on_hand_un_us") : "");
		    }
	    }
	    else {
			usableCount = "0";
		    unusableCount = "0";
	    }

	    if (ebxUseableActual != null) ebxUseableActual.setText(String.valueOf(usableCount));
	    if (ebxUnuseableActual != null) ebxUnuseableActual.setText(String.valueOf(unusableCount));
        if(ebxLotUsable != null) ebxLotUsable.setText("");
        if(ebxLotUnusable != null) ebxLotUnusable.setText("");

		//KEST-2139 #I)
        if(chkBoxUsable != null) chkBoxUsable.setChecked(true);
        //if(toRemove != null) toRemove.clear();
    }

    public static PART_TYPE getPartType() {
        if (MetrixStringHelper.valueIsEqual(serialed, "Y") && MetrixStringHelper.valueIsEqual(lotIdentified, "Y")) {
            return PART_TYPE.SERANDLOTIDENTIFIED;
        } else if (MetrixStringHelper.valueIsEqual(serialed, "N") && MetrixStringHelper.valueIsEqual(lotIdentified, "Y")) {
            return PART_TYPE.LOTIDENTIFIED;
        } else if (MetrixStringHelper.valueIsEqual(serialed, "Y") && MetrixStringHelper.valueIsEqual(lotIdentified, "N")) {
            return PART_TYPE.SERIALED;
        } else {
            return PART_TYPE.NONE;
        }
    }

    //Raw SQL query is used to stop triggering a sync.
    public static void UpdateFlag(List<HashMap<String, String>> data) {
        for (int count = 0; count <= data.size() - 1; count++) {
            MetrixDatabaseManager.executeSql(String.format("update stock_count set posted = 'Y' where metrix_row_id = %s", data.get(count).get("stock_count.metrix_row_id")));
        }
    }

    public static boolean performStkCount() {
        try {
            String performName = MobileApplication.getAppParam("STOCK_COUNT_ACTION");
            if (MetrixStringHelper.isNullOrEmpty(performName)) {
                performName = "perform_post_stock_counts";
            }

            Hashtable<String, String> params = new Hashtable<>();
            params.put("run_id", runId);

            if ("perform_post_stock_counts".equalsIgnoreCase(performName)) {
                params.put("delete_all", String.valueOf(true));
                params.put("gl_acct", "");
                params.put("mrl_update_period", "");
            }
            MetrixPerformMessage perform = new MetrixPerformMessage(performName, params);
            perform.save();

        } catch (Exception ex) {
            LogManager.getInstance().error(ex);
            return false;
        }
        return true;
    }

    public static String getPartListQuery(Activity activity, String rId, String fltr) {
        String whereClause = String.format("where run_id = '%s' %s ", rId, fltr);
        StringBuilder query = new StringBuilder();
        query.append(MetrixListScreenManager.generateListQuery(activity, "stock_count", whereClause));

	    String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
	    if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
		    query.append(" limit ");
		    query.append(maxRows);
	    }

        return query.toString();
    }
}
