package com.metrix.metrixmobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.system.DebriefListViewActivity;

import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class DebriefPaymentList extends DebriefListViewActivity implements MetrixRecyclerViewListener {
	private RecyclerView recyclerView;
	private MetadataRecyclerViewAdapter mAdapter;
	private int mSelectedPosition;
	private String allowDelete;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debrief_payment_list);
		recyclerView = findViewById(R.id.recyclerView);
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		mParentActivity = DebriefPayment.class;
		allowDelete = MobileApplication.getAppParam("PAYMENT_ALLOW_DELETE");

		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
	}

	public void onStart() {
		super.onStart();
		setupActionBar();
		populateList();
	}

	/**
	 * Populate the job list with the tasks assigned to the user.
	 */
	private void populateList() {
		String query = MetrixListScreenManager.generateListQuery(this, "payment", String.format("payment.task_id = %s", MetrixCurrentKeysHelper.getKeyValue("task", "task_id")));
		
		String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");		
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

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);

				String firstName = "";
				String lastName = "";
				String contactSequence = row.get("payment.contact_sequence");
				if (!MetrixStringHelper.isNullOrEmpty(contactSequence)) {
					firstName = row.get("contact.first_name");
					lastName = row.get("contact.last_name");
				}

				String requestContactSequence = row.get("payment.req_cont_seq");
				if (!MetrixStringHelper.isNullOrEmpty(requestContactSequence)) {
					firstName = row.get("request_contact.first_name");
					lastName = row.get("request_contact.last_name");
				}

				String fullName = "";
				if (!MetrixStringHelper.isNullOrEmpty(firstName) || !MetrixStringHelper.isNullOrEmpty(lastName)) {
					if (MetrixStringHelper.isNullOrEmpty(firstName)) {
						fullName = String.format("%s", lastName);
					} else {
						fullName = String.format("%1$s %2$s", firstName, lastName);
					}
				}
				row.put("custom.full_name", fullName);

				table.add(row);
				cursor.moveToNext();
			}

			table = MetrixListScreenManager.performScriptListPopulation(this, table);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		if (mAdapter == null) {
			mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
					R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, "payment.metrix_row_id", this);
			recyclerView.setAdapter(mAdapter);
		} else {
			mAdapter.updateData(table);
		}
	}

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		if (MetrixStringHelper.isNullOrEmpty(allowDelete) || allowDelete.compareToIgnoreCase("Y") != 0
				|| taskIsComplete() || scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;

		mSelectedPosition = position;
		final OnClickListener deleteListener = (dialog, which) -> {
			try {
				@SuppressWarnings("unchecked")
				HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);

				String metrixRowId = selectedItem.get("payment.metrix_row_id");
				String paymentId = MetrixDatabaseManager.getFieldStringValue("payment", "payment_id",
						"metrix_row_id=" + metrixRowId);

				MetrixUpdateManager.delete(DebriefPaymentList.this, "payment", metrixRowId, "payment_id", paymentId, AndroidResourceHelper.getMessage("Payment"), MetrixTransaction.getTransaction("task", "task_id"));
				mAdapter.getListData().remove(mSelectedPosition);
				mAdapter.notifyItemRemoved(mSelectedPosition);
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		MetrixDialogAssistant.showConfirmDeleteDialog(AndroidResourceHelper.getMessage("PaymentLCase"), deleteListener, null, this);
	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

	}
}
