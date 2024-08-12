package com.metrix.metrixmobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.system.BottomOffsetDecoration;
import com.metrix.metrixmobile.system.MetrixActivity;

public class ReceivingUnitList extends MetrixActivity implements View.OnClickListener, MetrixRecyclerViewListener  {
	private RecyclerView partRecyclerView = null;
	private FloatingActionButton mProcessButton;
	private String mReceivingId = "";
	private MetadataRecyclerViewAdapter adapter;
	private BottomOffsetDecoration mBottomOffset;
	private List<FloatingActionButton> mFABList;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			try {
				// if there is a cast exception, app continues as if the adapter is null
				@SuppressWarnings("unchecked")
				List<HashMap<String, String>> table = (ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable("AdapterData");
				adapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_checkbox,
						R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, R.id.list_checkbox,
						"receiving_unit.metrix_row_id", 0, R.id.sliver, null, "receiving_unit.metrix_row_id", this);
			}catch(ClassCastException cce) {
				LogManager.getInstance().error(cce);
			}
		}
		setContentView(R.layout.receiving_unit_list);
		partRecyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(partRecyclerView, R.drawable.rv_item_divider);
	}

	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		if(adapter != null)
			savedInstanceState.putSerializable("AdapterData", (ArrayList<HashMap<String, String>>) adapter.getListData());
	}

	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.process, "Process"));
		mLayout = (ViewGroup) findViewById(R.id.table_layout);

		mReceivingId = getIntent().getExtras().getString("receiving_id");

		super.onStart();

		setReceivingActionBarTitle();

		populateList();
		setListeners();
	}

	protected void setListeners() {
		mProcessButton = (FloatingActionButton) findViewById(R.id.process);

		if (mFABList == null)
			mFABList = new ArrayList<FloatingActionButton>();
		else
			mFABList.clear();

		if (mProcessButton != null) {
			mProcessButton.setOnClickListener(this);
			mFABList.add(mProcessButton);
		}

		mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));

		fabRunnable = this::showFABs;

		partRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

	private void populateList() {
		final String whereClause = String.format("receiving.inventory_adjusted = 'N' and receiving_unit.receiving_id = %s", mReceivingId);
		String query = MetrixListScreenManager.generateListQuery(this, "receiving_unit", whereClause);

		final String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
		if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
			query = query + " limit " + maxRows;
		}

		MetrixCursor cursor = null;
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			int index = 0;

			while (cursor.isAfterLast() == false) {
				final HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);
				if(adapter == null || adapter.getListData() == null){
					row.put("checkboxState", "Y");
				} else {
					row.put("checkboxState", adapter.getListData().get(index).get("checkboxState"));
				}

				table.add(row);
				index++;
				cursor.moveToNext();
			}

			table = MetrixListScreenManager.performScriptListPopulation(this, table);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		if (adapter == null) {
			adapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_checkbox,
					R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, R.id.list_checkbox,
					"receiving_unit.metrix_row_id", 0, R.id.sliver, null, "receiving_unit.metrix_row_id", this);
			partRecyclerView.addItemDecoration(mBottomOffset);
			partRecyclerView.setAdapter(adapter);
		}

		if (partRecyclerView.getAdapter() == null) {
			partRecyclerView.addItemDecoration(mBottomOffset);
			partRecyclerView.setAdapter(adapter);
		} else
			adapter.updateData(table);
	}	

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.process:
				if (adapter != null && adapter.getListData() != null) {
					final ArrayList<Hashtable<String, String>> updateList = new ArrayList<Hashtable<String, String>>();

					for (HashMap<String, String> item : adapter.getListData()) {
						if (item != null) {
							final boolean notChecked = "N".equalsIgnoreCase(item.get("checkboxState"));
							if (notChecked) {
								final String receivingSequence = item.get("receiving_unit.receiving_sequence");
								final String sequence = item.get("receiving_unit.sequence");

								if (!MetrixStringHelper.isNullOrEmpty(mReceivingId) &&
										!MetrixStringHelper.isNullOrEmpty(receivingSequence) && !MetrixStringHelper.isNullOrEmpty(sequence)) {
									final Hashtable<String, String> parameters = new Hashtable<String, String>();
									parameters.put("receiving_id", mReceivingId);
									parameters.put("receiving_sequence", receivingSequence);
									parameters.put("sequence", sequence);

									updateList.add(parameters);
								}
							}
						}
					}

					performPostReceipt(updateList);
				}

				break;
		default:
			super.onClick(v);
		}
	}
	
	private void initDialog(final String rec_id, final String rec_seq, final String seq, final String quantity){
		AlertDialog.Builder alert = new AlertDialog.Builder(ReceivingUnitList.this);
		alert.setTitle(AndroidResourceHelper.getMessage("UpdateQuantity"));
		alert.setMessage(AndroidResourceHelper.getMessage("Quantity"));

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		input.setText(quantity);
		input.setTag(rec_id+"-"+rec_seq+"-"+seq+"-"+quantity); // hide the rec_id, rec_seq and original quantity 
													   // in tag for positive button to process
		alert.setView(input);		

		alert.setPositiveButton(AndroidResourceHelper.getMessage("Update"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				String rInfo = input.getTag().toString();

				int intValue = 0;
				if (MetrixFloatHelper.convertNumericFromUIToNumber(value) != null){
					intValue = MetrixFloatHelper.convertNumericFromUIToNumber(value).intValue();
				}
				if (intValue < 1) {
					MetrixUIHelper.showSnackbar(ReceivingUnitList.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("ReceivingUnitQtyError"));
					return;
				}

				if(rInfo.contains("-")){
					String rId = rInfo.split("-")[0];
					String rSeq = rInfo.split("-")[1];					
					String seq = rInfo.split("-")[2];
					if(MetrixStringHelper.isInteger(value))
						updateShipmentQuantity(rId, rSeq, seq, value);
				}
			}
		});

		alert.setNegativeButton(AndroidResourceHelper.getMessage("Cancel"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		alert.show();
	}
	
	private void updateShipmentQuantity(String receivingId, String receivingSequence, String sequence, String quantity){
		String row_id = MetrixDatabaseManager.getFieldStringValue("receiving_unit", "metrix_row_id", "receiving_id='"+receivingId+"' and receiving_sequence='"+receivingSequence+"'");
		MetrixSqlData receivingUnitData = new MetrixSqlData("receiving_unit", MetrixTransactionTypes.UPDATE);
		
		DataField fdRowId = new DataField("metrix_row_id", row_id);		
		DataField fdReceiving_id = new DataField("receiving_id", receivingId);
		DataField fdReceiving_sequence = new DataField("receiving_sequence", receivingSequence);
		DataField fdSequence = new DataField("sequence", sequence);		
		DataField fdQuantity= new DataField("quantity", quantity);		
				
		receivingUnitData.dataFields.add(fdRowId);		
		receivingUnitData.dataFields.add(fdReceiving_id);
		receivingUnitData.dataFields.add(fdReceiving_sequence);
		receivingUnitData.dataFields.add(fdSequence);		
		receivingUnitData.dataFields.add(fdQuantity);
		
		// this transaction is for update, so add the filter which is using metrix_row_id
		receivingUnitData.filter = "metrix_row_id="+row_id;
		
		ArrayList<MetrixSqlData> receivingTrans = new ArrayList<MetrixSqlData>();
		receivingTrans.add(receivingUnitData);

		MetrixTransaction transactionInfo = new MetrixTransaction();

		boolean successful = MetrixUpdateManager.update(receivingTrans, true, transactionInfo, AndroidResourceHelper.getMessage("ReceivingUnit"), this);
		
		if(successful){
			populateList();
		}
	}

	private void performPostReceipt(ArrayList<Hashtable<String, String>> updateList) {
		User user = (User) MetrixPublicCache.instance.getItem("MetrixUser");
		ArrayList<String> sqlDelete = new ArrayList<String>(); 
		
		JsonObject jsonMessage = new JsonObject();
		JsonObject sequential = new JsonObject();
		JsonArray sequentialElement = new JsonArray();		
		JsonObject update_method = new JsonObject();
		JsonObject table = new JsonObject();
		JsonObject nameValue = new JsonObject();		
		
		if(updateList!=null){
			for(Hashtable<String, String> item: updateList){	
				StringBuilder sql = new StringBuilder();
				sql.append("delete from receiving_unit where 1=1");
				
				for (String key : item.keySet()) {
					String value = item.get(key);
		
					nameValue.addProperty(key, value);
					table.add("receiving_unit", nameValue);
					sql.append(" and "+key+"="+value);
				}						
		
				sqlDelete.add(sql.toString());
				// add the transaction type insert/delete/update
				nameValue.addProperty("delete", "");
				table.add("receiving_unit", nameValue);			
				
				update_method.add("update_" + "receiving_unit", table);	
				sequentialElement.add(update_method);
			}
		}
		JsonObject perform_method = new JsonObject();
		JsonObject parameterList = new JsonObject();
     	nameValue = new JsonObject();
     	
     	Hashtable<String, String> parameters = new Hashtable<String, String>();
		parameters = new Hashtable<String, String>();
		parameters.put("receiving_id", mReceivingId);
		
		for (String key : parameters.keySet()) {
			String value = parameters.get(key);

			nameValue.addProperty(key, value);
			parameterList.add("parameters", nameValue);
		}

		perform_method.add("perform_post_receipt", parameterList);
		
		sequentialElement.add(perform_method);		
		sequential.add("sequential_dependent", sequentialElement);
		jsonMessage.add("perform_batch", sequential);		
				
		String message_out_message = jsonMessage.toString();

		ArrayList<DataField> fields = new ArrayList<DataField>();
		fields.add(new DataField("person_id", user.personId));
		fields.add(new DataField("transaction_type", "update"));
		fields.add(new DataField("message", message_out_message));
		fields.add(new DataField("transaction_id", MetrixDatabaseManager
				.generateTransactionId("mm_message_out")));
		fields.add(new DataField("status", "READY"));
		fields.add(new DataField("created_dttm", MetrixDateTimeHelper
				.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true)));
		fields.add(new DataField("modified_dttm", MetrixDateTimeHelper
				.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true)));
		long ret_value = MetrixDatabaseManager.insertRow("mm_message_out",
				fields);

		if (ret_value > 0) {
			if(sqlDelete.size()>0){
				for(String sqlStatement: sqlDelete){
					MetrixDatabaseManager.executeSql(sqlStatement);
				}
			}
			
			MetrixDatabaseManager.executeSql("update receiving set inventory_adjusted = 'Y' where receiving_id ="+mReceivingId);
			finish();
		} else {
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("FailedPostReceiving"));
		}
	}

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		if(scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;

		// only non-serial units can have quantity adjusted
		String serialID = listItemData.get("receiving_unit.serial_id");
		if (MetrixStringHelper.isNullOrEmpty(serialID)) {
			String receiving_id = listItemData.get("receiving_unit.receiving_id");
			String receiving_sequence = listItemData.get("receiving_unit.receiving_sequence");
			String sequence = listItemData.get("receiving_unit.sequence");
			String quantity = listItemData.get("receiving_unit.quantity");

			initDialog(receiving_id, receiving_sequence, sequence, quantity);
		}
	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

	}
}