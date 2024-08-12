package com.metrix.metrixmobile;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.ui.widget.MobileUIHelper;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.system.MetrixActivity;

public class DebriefNonPartUsageList extends MetrixActivity implements MetadataRecyclerViewAdapter.MetrixRecyclerViewClickListener {

	private RecyclerView recyclerView;
	private CommentRecyclerViewAdapter mCommentAdapter;
	private String[] mFrom;
	private int mSelectedPosition;
	private Context mContext;
	private Button mImageButtonAddComment;
	// private String dayFilter;
	private String calendarDay;
	private String lineCode;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		mContext = this;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debrief_non_part_usage_list);
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		//mParentActivity = DayOverviewActivity.class;

		calendarDay = getIntent().getStringExtra("CalendarDay");
		lineCode = getIntent().getStringExtra("LineCode");
		recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
		setListeners();
	}

	public void onStart() {

		resourceStrings.add(new ResourceValueObject(R.id.comment_imagebtn_add, "Add"));
		super.onStart();
		//setupActionBar();
		populateList();
		
		//setNavigation();
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
			mImageButtonAddComment = (Button) findViewById(R.id.comment_imagebtn_add);
			mImageButtonAddComment.setOnClickListener(this);
	}

	/**
	 * Populate the job list with the tasks assigned to the user.
	 */
	private void populateList() {
		String tempFilter = new String();
		
		StringBuilder query = new StringBuilder();
		query.append("select non_part_usage.metrix_row_id, non_part_usage.npu_id, line_code.description, non_part_usage.line_code, person.first_name, person.last_name, non_part_usage.work_dt, person.image_id, non_part_usage.quantity, non_part_usage.task_id, non_part_usage.created_dttm");
		query.append(" from non_part_usage");
		query.append(" left outer join person on non_part_usage.created_by = person.person_id ");
		query.append(" left outer join line_code on non_part_usage.line_code = line_code.line_code");
		
		if(!MetrixStringHelper.isNullOrEmpty(calendarDay)) {
			Calendar theDay = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, calendarDay);
			int year = theDay.get(Calendar.YEAR);
			int month = theDay.get(Calendar.MONTH); // Note: zero based!
			int day = theDay.get(Calendar.DAY_OF_MONTH);		
			
			String dayStart = MetrixDateTimeHelper.formatDate(MetrixDateTimeHelper.DATE_FORMAT, year, month, day, 0, 0, 0);
			dayStart = MetrixDateTimeHelper.convertDateTimeFromUIToDB(dayStart, MetrixDateTimeHelper.DATE_FORMAT);

			String dayEnd = MetrixDateTimeHelper.formatDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, year, month, day, 23, 59, 59); 
			dayEnd = MetrixDateTimeHelper.convertDateTimeFromUIToDB(dayEnd, MetrixDateTimeHelper.DATE_TIME_FORMAT);	
		
			tempFilter = " where non_part_usage.work_dt >= '" + dayStart
				+ "' and non_part_usage.work_dt<='"+dayEnd+"'";
			query.append(tempFilter);
		}
		if(!MetrixStringHelper.isNullOrEmpty(lineCode)){
			if(MetrixStringHelper.isNullOrEmpty(tempFilter))
				tempFilter = " where line_code.line_code = '"+lineCode+"'";
			else
				tempFilter = " and line_code.line_code = '"+lineCode+"'";
			
			query.append(tempFilter);
		}
		
//		query.append(" where non_part_usage.task_id = "
//				+ MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));		
		query.append(" order by non_part_usage.modified_dttm desc");
		
		String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");		
		if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
			query.append(" limit " + maxRows);
		}
		
		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}

		mFrom = new String[] { "non_part_usage.metrix_row_id", "non_part_usage.npu_id", "line_code.description", "non_part_usage.line_code", "person.full_name", "non_part_usage.work_dt", "person.image_id", "non_part_usage.quantity", "non_part_usage.task_id", "non_part_usage.created_dttm"};

		List<HashMap<String, String>> table = new ArrayList<>();
		while (cursor.isAfterLast() == false) {
			HashMap<String, String> row = new HashMap<>();

			row.put(mFrom[0], cursor.getString(0));
			row.put(mFrom[1], cursor.getString(1));
			row.put(mFrom[2], cursor.getString(2));
			row.put(mFrom[3], cursor.getString(3));
			
			String firstName = cursor.getString(4);
			String lastName = cursor.getString(5);
			
			String fullName = null;
			if((firstName == null) && (lastName == null))
				fullName = "";
			else
				fullName = String.format("%s %s", firstName, lastName);
			
			row.put(mFrom[4], fullName);
			row.put(mFrom[5], MetrixDateTimeHelper.convertDateTimeFromDBToUI(cursor.getString(6), MetrixDateTimeHelper.DATE_FORMAT));
			row.put(mFrom[6], cursor.getString(7));
			row.put(mFrom[7], cursor.getString(8));
			row.put(mFrom[8], cursor.getString(9));
			row.put(mFrom[9], cursor.getString(10));

			table.add(row);
			cursor.moveToNext();
		}
		cursor.close();

		// fill in the grid_item layout
		mCommentAdapter = new CommentRecyclerViewAdapter(table, this);
		recyclerView.setAdapter(mCommentAdapter);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.comment_imagebtn_add:
			Intent intent = MetrixActivityHelper.createActivityIntent(this, AddLabor.class);	
			MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			break;
		}
	}

	@Override
	public void onItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		if(scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;
		mSelectedPosition = position;

		final OnClickListener modifyListener = (data, which) -> {
			try {
				HashMap<String, String> selectedItem = mCommentAdapter.getData().get(mSelectedPosition);
				if (selectedItem == null) return;

				Intent intent = MetrixActivityHelper.createActivityIntent(DebriefNonPartUsageList.this, AddLabor.class, MetrixTransactionTypes.UPDATE,
						"metrix_row_id", selectedItem.get("non_part_usage.metrix_row_id"));
				MetrixActivityHelper.startNewActivityAndFinish((Activity) mContext, intent);
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		final OnClickListener deleteListener = (data, which) -> {
			try {
				HashMap<String, String> selectedItem = mCommentAdapter.getData().get(mSelectedPosition);
				if(selectedItem == null) return;

				String metrixRowId = selectedItem.get("non_part_usage.metrix_row_id");
				String taskId = MetrixDatabaseManager.getFieldStringValue("non_part_usage", "task_id",
						"metrix_row_id=" + metrixRowId);
				String textSequence = MetrixDatabaseManager.getFieldStringValue("non_part_usage", "npu_id",
						"metrix_row_id=" + metrixRowId);

				Hashtable<String, String> primaryKeys = new Hashtable<String, String>();
				primaryKeys.put("task_id", taskId);
				primaryKeys.put("npu_id", textSequence);

				MetrixUpdateManager.delete(DebriefNonPartUsageList.this, "non_part_usage", metrixRowId, primaryKeys, AndroidResourceHelper.getMessage("Labor"), MetrixTransaction.getTransaction("task", "task_id"));
				mCommentAdapter.removeItemAtPosition(position);
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		MetrixDialogAssistant.showEditOrDeleteDialog(AndroidResourceHelper.getMessage("Labor"), modifyListener, deleteListener, this);
	}

	static class CommentRecyclerViewAdapter extends RecyclerView.Adapter<CommentRecyclerViewAdapter.CommentVH> {
		private final List<HashMap<String, String>> data;
		private final MetadataRecyclerViewAdapter.MetrixRecyclerViewClickListener listener;

		CommentRecyclerViewAdapter(List<HashMap<String, String>> data, MetadataRecyclerViewAdapter.MetrixRecyclerViewClickListener listener) {
			this.data = data;
			this.listener = listener;
		}

		@NonNull
		@Override
		public CommentVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			final View view = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.debrief_task_text_list_item, parent, false);
			return new CommentVH(view);
		}

		@Override
		public void onBindViewHolder(@NonNull CommentVH holder, int position) {
			HashMap<String, String> item = data.get(position);
			if (item == null) return;

			final String metrixRowId = item.get("non_part_usage.metrix_row_id");
			final String sequence = item.get("non_part_usage.npu_id");
			final String lineCodeDescription = item.get("line_code.description");
			final String NPUQuantity = item.get("non_part_usage.quantity");
			final String NPUCreatedOn =  item.get("non_part_usage.created_dttm");
			final String NPUWorkDt = item.get("non_part_usage.work_dt");
			final String NPUTaskId = item.get("non_part_usage.task_id");

			holder.textViewMetrixRowId.setText(metrixRowId);
			holder.textViewSequence.setText(sequence);
			holder.textViewLineCodeDescription.setText(lineCodeDescription);
			holder.textViewNPUDuration.setText(NPUQuantity);
			holder.textViewNPUTaskId.setText(NPUTaskId);
			final String customPeriod = MobileUIHelper.jodaPeriod(NPUCreatedOn);
			holder.textViewNPUTimeOffset.setText(customPeriod);
			holder.textViewNPUWorkDt.setText(NPUWorkDt);
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		public void removeItemAtPosition(int position) {
			if (position < data.size()) {
				data.remove(position);
				notifyItemRemoved(position);
			}
		}

		public List<HashMap<String, String>> getData() {
			return data;
		}

		class CommentVH extends RecyclerView.ViewHolder {
			private final TextView textViewMetrixRowId;
			private final TextView textViewSequence;
			private final TextView textViewLineCodeDescription;
			private final TextView textViewNPUDuration;
			private final TextView textViewNPUWorkDt;
			private final TextView textViewNPUTaskId;
			private final TextView textViewNPUTimeOffset;

			public CommentVH(View itemView) {
				super(itemView);
				itemView.setTag(MetadataRecyclerViewAdapter.SCRIPT_EXECUTABLE);
				textViewMetrixRowId = itemView.findViewById(R.id.nonPartUsage_metrix_row_id);
				textViewSequence = itemView.findViewById(R.id.nonPartUsage_npu_id);
				textViewLineCodeDescription = itemView.findViewById(R.id.nonPartUsage_description);
				textViewNPUWorkDt = itemView.findViewById(R.id.nonPartUsage_work_dt);
				textViewNPUDuration = itemView.findViewById(R.id.nonPartUsage_quantity);
				textViewNPUTimeOffset = itemView.findViewById(R.id.nonPartUsage_created_on);
				textViewNPUTaskId = itemView.findViewById(R.id.nonPartUsage_task_id);

				itemView.setOnClickListener((v) -> {
					if (listener != null) {
						final int position = getAdapterPosition();
						if (position != RecyclerView.NO_POSITION)
							listener.onItemClick(position, data.get(position), v);
					}
				});
			}
		}
	}
}
