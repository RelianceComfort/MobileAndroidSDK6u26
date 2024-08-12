package com.metrix.metrixmobile;

import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
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
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.system.DebriefListViewActivity;
import com.metrix.metrixmobile.system.MetadataDebriefActivity;
import com.metrix.metrixmobile.system.MetrixPooledTaskAssignmentManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class DebriefTaskTextList extends DebriefListViewActivity implements
		MetadataRecyclerViewAdapter.MetrixRecyclerViewClickListener {
	private RecyclerView recyclerView;
	private CommentRecyclerViewAdapter mCommentAdapter;
	private String[] mFrom;
	private int mSelectedPosition;
	private Context mContext;
	private ImageButton mImageButtonAddComment;
	private LinearLayout mLayoutAddComment;

	private String mMaxRows = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mContext = this;
		super.onCreate(savedInstanceState);
		if (shouldRunTabletSpecificUIMode)
			setContentView(R.layout.tb_land_debrief_task_text_list);
		else
			setContentView(R.layout.debrief_task_text_list);
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);

		setListeners();
	}

	public void onStart() {
		super.onStart();
		setupActionBar();
		populateList();
		
		setNavigation();
	}

	protected void setListeners() {
		if (!taskIsComplete()) {
			mImageButtonAddComment = (ImageButton) findViewById(R.id.comment_imagebtn_add);
			mImageButtonAddComment.setOnClickListener(this);
			mLayoutAddComment = (LinearLayout) findViewById(R.id.comment_layout_add);
			mLayoutAddComment.setOnClickListener(this);

			if (MetrixPooledTaskAssignmentManager.instance().isPooledTask(MetrixCurrentKeysHelper.getKeyValue("task", "task_id"))) {
				mImageButtonAddComment.setEnabled(false);
				mLayoutAddComment.setEnabled(false);
			}
		}
	}

	@Override
	protected void handleLocalMessage(Global.ActivityType activityType, final String message)
	{
		if (activityType == Global.ActivityType.Download && MetrixStringHelper.valueIsEqual(message, "TASK_TEXT"))
		{
			populateList();
		}
		super.handleLocalMessage(activityType, message);
	}

	private void populateList() {
		StringBuilder query = new StringBuilder();
		query.append("select task_text.metrix_row_id, task_text.text_sequence, text_line_code.description, task_text.text, person.first_name, person.last_name, task_text.created_dttm, person.image_id");
		query.append(" from task_text");
		query.append(" left outer join person on task_text.created_by = person.person_id ");
		query.append(" left outer join text_line_code on task_text.text_line_code = text_line_code.text_line_code where task_text.task_id = "
				+ MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));
		query.append(" order by task_text.created_dttm desc");
		
		if (MetrixStringHelper.isNullOrEmpty(mMaxRows)){
			mMaxRows = MetrixDatabaseManager.getAppParam("MAX_ROWS");
		}
		if (!MetrixStringHelper.isNullOrEmpty(mMaxRows)) {
			query.append(" limit " + mMaxRows);
		}
		
		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}

		mFrom = new String[] { "task_text.metrix_row_id", "task_text.text_sequence", "text_line_code.description", "task_text.text", "person.full_name", "task_text.created_dttm", "person.image_id"};

		List<HashMap<String, String>> table = new ArrayList<>();
		while (cursor.isAfterLast() == false) {
			HashMap<String, String> row = new HashMap<>(7);

			row.put(mFrom[0], cursor.getString(0));
			row.put(mFrom[1], cursor.getString(1));
			row.put(mFrom[2], cursor.getString(2));
			row.put(mFrom[3], cursor.getString(3));
			
			String firstName = cursor.getString(4);
			String lastName = cursor.getString(5);
			
			String fullName;
			if((firstName == null) && (lastName == null))
				fullName = "";
			else
				fullName = String.format("%s %s", firstName, lastName);
			
			row.put(mFrom[4], fullName);
			row.put(mFrom[5], cursor.getString(6));
			row.put(mFrom[6], cursor.getString(7));

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
		case R.id.comment_layout_add:
		case R.id.comment_imagebtn_add:
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetadataDebriefActivity.class);
			intent.putExtra("ScreenID", MetrixScreenManager.getScreenId("DebriefTaskText"));
			intent.putExtra("NavigatedFromLinkedScreen", true);
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		}
	}

	@Override
	public void onItemClick(int position, @NonNull HashMap<String, String> item, @NonNull View view) {
		if(taskIsComplete() || scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;
		mSelectedPosition = position;

		if (MetrixPooledTaskAssignmentManager.instance().isPooledTask(MetrixCurrentKeysHelper.getKeyValue("task", "task_id"))) {
			try {
				HashMap<String, String> selectedItem = mCommentAdapter.getData().get(mSelectedPosition);
				if (selectedItem == null) return;

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetadataDebriefActivity.class, MetrixTransactionTypes.UPDATE,
						"metrix_row_id", selectedItem.get("task_text.metrix_row_id"));
				intent.putExtra("ScreenID", MetrixScreenManager.getScreenId("DebriefTaskText"));
				intent.putExtra("NavigatedFromLinkedScreen", true);
				MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
			return;
		}

		final OnClickListener modifyListener = (dialog, which) -> {
			try {
				HashMap<String, String> selectedItem =  mCommentAdapter.getData().get(mSelectedPosition);
				if (selectedItem == null) return;

				Intent intent = MetrixActivityHelper.createActivityIntent(DebriefTaskTextList.this, MetadataDebriefActivity.class, MetrixTransactionTypes.UPDATE,
						"metrix_row_id", selectedItem.get("task_text.metrix_row_id"));
				intent.putExtra("ScreenID", MetrixScreenManager.getScreenId("DebriefTaskText"));
				intent.putExtra("NavigatedFromLinkedScreen", true);
				MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		final OnClickListener deleteListener = (data, which) -> {
			try {
				HashMap<String, String> selectedItem =  mCommentAdapter.getData().get(mSelectedPosition);
				if(selectedItem == null) return;

				String metrixRowId = selectedItem.get("task_text.metrix_row_id");
				String taskId = MetrixDatabaseManager.getFieldStringValue("task_text", "task_id",
						"metrix_row_id=" + metrixRowId);
				String textSequence = MetrixDatabaseManager.getFieldStringValue("task_text", "text_sequence",
						"metrix_row_id=" + metrixRowId);

				Hashtable<String, String> primaryKeys = new Hashtable<String, String>();
				primaryKeys.put("task_id", taskId);
				primaryKeys.put("text_sequence", textSequence);

				MetrixUpdateManager.delete(DebriefTaskTextList.this, "task_text", metrixRowId, primaryKeys, AndroidResourceHelper.getMessage("Text"), MetrixTransaction.getTransaction("task", "task_id"));
				mCommentAdapter.removeItemAtPosition(mSelectedPosition);

				refreshDebriefNavigationList();

			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		MetrixDialogAssistant.showEditOrDeleteDialog(AndroidResourceHelper.getMessage("NoteLCase"), modifyListener, deleteListener, this);
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
			final HashMap<String, String> item = data.get(position);
			if (item == null) return;

			final String metrixRowId = item.get("task_text.metrix_row_id");
			final String sequence = item.get("task_text.text_sequence");
			final String lineCodeDescription = item.get("text_line_code.description");
			final String taskText = item.get("task_text.text");
			final String tastTextCreated = item.get("person.full_name");
			final String tastTextCreatedOn = item.get("task_text.created_dttm");
			final String imageId = item.get("person.image_id");

			try {
				if (!MetrixStringHelper.isNullOrEmpty(imageId)) {
					MetrixAttachmentHelper.applyImageWithNoScale(imageId, holder.imageViewPersonPic);
				} else {
					Glide.with(holder.itemView.getContext())
							.load(R.drawable.comments_profile)
							.into(holder.imageViewPersonPic);
				}
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}

			holder.textViewMetrixRowId.setText(metrixRowId);
			holder.textViewSequence.setText(sequence);
			holder.textViewLineCodeDescription.setText(lineCodeDescription);
			holder.textViewTaskText.setText(taskText);
			holder.textViewTastTextCreatedBy.setText(tastTextCreated);

			final String customPeriod = MobileUIHelper.jodaPeriod(tastTextCreatedOn);
			holder.textViewTastTextCreatedOn.setText(customPeriod);
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
			private final ImageView imageViewPersonPic;
			private final TextView textViewMetrixRowId;
			private final TextView textViewSequence;
			private final TextView textViewLineCodeDescription;
			private final TextView textViewTaskText;
			private final TextView textViewTastTextCreatedBy;
			private final TextView textViewTastTextCreatedOn;

			public CommentVH(View itemView) {
				super(itemView);
				itemView.setTag(MetadataRecyclerViewAdapter.SCRIPT_EXECUTABLE);
				imageViewPersonPic = itemView.findViewById(R.id.imageView_task_text_person);
				textViewMetrixRowId = itemView.findViewById(R.id.textView_metrix_row_id);
				textViewSequence = itemView.findViewById(R.id.textView_sequence);
				textViewLineCodeDescription = itemView.findViewById(R.id.textView_description);
				textViewTaskText = itemView.findViewById(R.id.textView_text);
				textViewTastTextCreatedBy = itemView.findViewById(R.id.textView_created_by);
				textViewTastTextCreatedOn = itemView.findViewById(R.id.textView_created_on);

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
	
	//Tablet UI Optimization
	@Override
	public boolean isTabletSpecificLandscapeUIRequired()
	{
		return true;
	}
	//End Tablet UI Optimization
}
