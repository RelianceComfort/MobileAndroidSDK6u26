package com.metrix.metrixmobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.User;
import com.metrix.architecture.utilities.Global.ActivityType;
import com.metrix.metrixmobile.system.BottomOffsetDecoration;
import com.metrix.metrixmobile.system.DebriefListViewActivity;

public class CalendarExceptionList extends DebriefListViewActivity implements View.OnClickListener, MetrixRecyclerViewListener {
	private RecyclerView recyclerView;
	private FloatingActionButton mSaveButton;
	private MetadataRecyclerViewAdapter mAdapter;
	private int mSelectedPosition;
	private BottomOffsetDecoration mBottomOffset;
	private List<FloatingActionButton> mFABList;
	private List<FloatingActionButton> mFABsToShow;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calendar_exception_list);
		recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		mParentActivity = CalendarException.class;

		setListeners();
	}

	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.add, "Add"));
		super.onStart();
		setupActionBar();
		populateList();
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		if (mFABList == null)
			mFABList = new ArrayList<FloatingActionButton>();
		else
			mFABList.clear();

		mSaveButton = (FloatingActionButton) findViewById(R.id.add);
		mSaveButton.setOnClickListener(this);
		mFABList.add(mSaveButton);

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

	/**
	 * Populate the job list with the tasks assigned to the user.
	 */
	private void populateList() {
		String today = MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_FORMAT);
		String todayDB = MetrixDateTimeHelper.convertDateTimeFromUIToDB(today);
		String query = MetrixListScreenManager.generateListQuery(this, "person_cal_except", String.format("person_cal_except.person_id = '%1s' and person_cal_except.start_dttm >='%2s' order by person_cal_except.start_dttm", User.getUser().personId, todayDB));
		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);

		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}
		
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		while (cursor.isAfterLast() == false) {
			HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);
			table.add(row);
			cursor.moveToNext();
		}
		cursor.close();

		if (mAdapter == null) {
			mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
					R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, "person_cal_except.metrix_row_id", this);
			recyclerView.addItemDecoration(mBottomOffset);
			recyclerView.setAdapter(mAdapter);
		} else {
			mAdapter.updateData(table);
		}
	}

	@Override
	protected void bindService() {
		bindService(new Intent(CalendarExceptionList.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	@Override
	protected void unbindService() {
		if (mIsBound) {
			try {
				if (service != null) {
					service.removeListener(listener);
					unbindService(mConnection);
				}
			} catch (Exception ex) {
				LogManager.getInstance().error(ex);
			} finally {
				mIsBound = false;
			}
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			// mBoundService = ((MetrixIntentService.LocalBinder) binder)
			// .getService();

			try {
				service = (IPostMonitor) binder;
				service.registerListener(listener);
			} catch (Throwable t) {
				LogManager.getInstance().error(t);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			// mBoundService = null;
			service = null;
		}
	};

	protected com.metrix.architecture.services.IPostListener listener = new com.metrix.architecture.services.IPostListener() {
		public void newSyncStatus(final ActivityType activityType, final String message) {
			runOnUiThread(new Runnable() {
				public void run() {
					if (activityType == ActivityType.Download) {
						//if (message.contains("update_person_cal_except_result")) {
							populateList();
						//}
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.add:
			Intent intent = new Intent(this, CalendarException.class);
			String theDate = MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT);
			intent.putExtra("CalendarDay", theDate);						

			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		default:
			super.onClick(v);
		}
	}

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		if (taskIsComplete()) return;

		mSelectedPosition = position;
		if(scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;

		final OnClickListener modifyListener = (dialog, which) -> {
			try {
				HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);
				Intent intent = MetrixActivityHelper.createActivityIntent(CalendarExceptionList.this, CalendarException.class, MetrixTransactionTypes.UPDATE,
						"metrix_row_id", selectedItem.get("person_cal_except.metrix_row_id"));
				MetrixActivityHelper.startNewActivity(CalendarExceptionList.this, intent);
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		final OnClickListener deleteListener = (dialog, which) -> {
			try {
				HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);
				String metrixRowId = selectedItem.get("person_cal_except.metrix_row_id");
				String exceptionId = MetrixDatabaseManager.getFieldStringValue("person_cal_except", "exception_id",
						"metrix_row_id=" + metrixRowId);
				MetrixUpdateManager.delete(CalendarExceptionList.this, "person_cal_except", metrixRowId, "exception_id", exceptionId, AndroidResourceHelper.getMessage("CalendarException"), new MetrixTransaction());
				mAdapter.getListData().remove(mSelectedPosition);
				mAdapter.notifyItemRemoved(mSelectedPosition);
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		MetrixDialogAssistant.showEditOrDeleteDialog(AndroidResourceHelper.getMessage("CalendarException"), modifyListener, deleteListener, this);
	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

	}
}
