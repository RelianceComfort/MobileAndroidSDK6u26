package com.metrix.architecture.ui.widget;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.DateParam;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.AddLabor;
import com.metrix.metrixmobile.BuildConfig;
import com.metrix.metrixmobile.CalendarException;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.CalendarDayData;
import com.metrix.metrixmobile.global.DataItem;
import com.metrix.metrixmobile.system.MetrixActivity;

/**
 * Overview of one day.
 * <p>
 * Activities that contain this fragment must implement the
 * {@link DayOverviewFragment} interface to handle
 * interaction events. Use the {@link DayOverviewFragment#newInstance} factory
 * method to create an instance of this fragment.
 *
 */
public class DayOverviewFragment extends Fragment implements DayOverviewClickListener, View.OnClickListener //, LoaderCallbacks<Cursor>, DayDiscardListener
{

	private static final String TAG = DayOverviewFragment.class.getSimpleName();
	private static final String ERROR_DIALOG_SHOWN = "error_dialog_shown";

	private TextView lblDate;
	private DayOverviewRecyclerViewAdapter adapter;
	private TextView sumJobHoursView;
	private TextView sumJobHoursLabel;
	private TextView expectedJobHoursView;
	private TextView expectedJobHoursLabel;
	private TextView weekDayHeader;
	private TextView monthYearHeader;
	private ImageView stateIcon;
	//	private DiaryDay diaryDay;
	private boolean errorDialogShown;
	private Button mAddButton, mAddExceptionButton;
	private View mLayout;
	private int mSelectedPosition;
	private RecyclerView recyclerView;
	//private DiscardDayTask discardDayTask;

	// Fragment initialization data
	private DateParam dateParam;

	//private DayOverviewFragmentInterface dayOverviewInterface;

	/**
	 * Use this factory method to create a new instance of this fragment.
	 *
	 * @return A new instance of fragment DayOverviewFragment.
	 */
	public static DayOverviewFragment newInstance(DateParam dateParam) {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "newInstance(" + dateParam.getIsoDateString() + ")");
		DayOverviewFragment fragment = new DayOverviewFragment();
		Bundle args = new Bundle();
		args.putParcelable(DateParam.DATE_PARAM, dateParam);
		fragment.setArguments(args);
		return fragment;
	}

	public DayOverviewFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		if (getArguments() != null) {
			dateParam = getArguments().getParcelable(DateParam.DATE_PARAM);
		} else if (savedInstanceState != null
				&& savedInstanceState.containsKey(DateParam.DATE_PARAM)) {
			dateParam = savedInstanceState.getParcelable(DateParam.DATE_PARAM);
		} else {
			dateParam = new DateParam();
		}
		//diaryDay = new DiaryDay();
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(ERROR_DIALOG_SHOWN)) {
			errorDialogShown = savedInstanceState
					.getBoolean(ERROR_DIALOG_SHOWN);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onResume() {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "onResume()");
		super.onResume();
		setHeaders();
		setSkins();

//		final DiaryDayOverview templateObject = new DiaryDayOverview();
		String theDay = MetrixDateTimeHelper.formatDate(dateParam.getYear(), dateParam.getMonth(), dateParam.getDate());
		final CalendarDayData templateObject = new CalendarDayData(theDay);

		adapter = new DayOverviewRecyclerViewAdapter(getActivity(), templateObject, this);
		templateObject.createCalendarItem();
		recyclerView.setAdapter(adapter);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			stateIcon.setImageAlpha(40);
		} else {
			// noinspection deprecation
			stateIcon.setAlpha(60);
		}

		this.setSummary();

//		getLoaderManager().restartLoader(DayOverviewAdapter.LOADER_ID, null,
//				adapter);
	}

	@Override
	public void onPause() {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "onPause()");
		super.onPause();
//		if (discardDayTask != null) {
//			discardDayTask.detachDisacrdListener();
//		}
//		discardDayTask = null;
	}

	private void setHeaders() {
		DateFormatSymbols symbols = new DateFormatSymbols();
		monthYearHeader.setText(symbols.getMonths()[dateParam.getMonth()] + " "
				+ dateParam.getYear());

		Calendar cal = Calendar.getInstance();
		// noinspection MagicConstant
		cal.set(dateParam.getYear(), dateParam.getMonth(), dateParam.getDate());
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

		weekDayHeader.setText(symbols.getWeekdays()[dayOfWeek]);
		lblDate.setText(Integer.toString(dateParam.getDate()));
	}

	private void setSkins() {
		TextView dayBottom = (TextView) mLayout.findViewById(R.id.dayOverviewFragmentMonthHeader);
		TextView dayTop = (TextView) mLayout.findViewById(R.id.dayOverviewFragmentWeekDayHeader);
		ImageView arrowLeft = (ImageView) mLayout.findViewById(R.id.leftArrow); //getActivity().getResources().getDrawable(R.drawable.arrow_left);
		ImageView arrowRight = (ImageView) mLayout.findViewById(R.id.rightArrow); //getActivity().getResources().getDrawable(R.drawable.arrow_right);

		String backgroundColor = MetrixSkinManager.getSecondaryColor();
		String textColor = MetrixSkinManager.getSecondGradientTextColor();
		if (!MetrixStringHelper.isNullOrEmpty(backgroundColor)) {
			dayTop.setBackgroundColor(Color.parseColor(backgroundColor));
			dayBottom.setBackgroundColor(Color.parseColor(backgroundColor));
		}

		if (!MetrixStringHelper.isNullOrEmpty(textColor)) {
			dayTop.setTextColor(Color.parseColor(textColor));
			dayBottom.setTextColor(Color.parseColor(textColor));
		}

		if (android.os.Build.VERSION.SDK_INT <= 19) {    // KitKat or earlier
			if (!MetrixStringHelper.isNullOrEmpty(backgroundColor)) {
				arrowLeft.setColorFilter(MetrixSkinManager.getColorFilter(backgroundColor));
				arrowRight.setColorFilter(MetrixSkinManager.getColorFilter(backgroundColor));
			} else {
				arrowLeft.setColorFilter(MetrixSkinManager.getColorFilter("#8427E2"));
				arrowRight.setColorFilter(MetrixSkinManager.getColorFilter("#8427E2"));
			}
		} else {
			if (!MetrixStringHelper.isNullOrEmpty(backgroundColor)) {
				arrowLeft.setColorFilter(MetrixSkinManager.getColorFilter(backgroundColor));
				arrowRight.setColorFilter(MetrixSkinManager.getColorFilter(backgroundColor));
//				arrowLeft.setColorFilter(Color.parseColor(backgroundColor), Mode.MULTIPLY);
//	        	arrowRight.setColorFilter(Color.parseColor(backgroundColor), Mode.MULTIPLY);
			} else {
				arrowLeft.setColorFilter(MetrixSkinManager.getColorFilter("#8427E2"));
				arrowRight.setColorFilter(MetrixSkinManager.getColorFilter("#8427E2"));
//	        	arrowLeft.setColorFilter(Color.parseColor("#8427E2"), Mode.MULTIPLY);
//	        	arrowRight.setColorFilter(Color.parseColor("#8427E2"), Mode.MULTIPLY);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "onCreateView()");
		// Inflate the layout for this fragment
		View layout = inflater.inflate(R.layout.fragment_day_overview,
				container, false);
		try {
			AndroidResourceHelper.setResourceValues(layout.findViewById(R.id.addF), "AddTime", false);
			AndroidResourceHelper.setResourceValues(layout.findViewById(R.id.addException), "AddException", false);
			AndroidResourceHelper.setResourceValues(layout.findViewById(R.id.dayOverviewFragmentSumJobHoursHeader), "Entered", false);
			AndroidResourceHelper.setResourceValues(layout.findViewById(R.id.dayOverviewFragmentExpectedJobHoursHeader), "Expected", false);
		} catch (Exception e) {
		}

		mLayout = layout;
		recyclerView = layout
				.findViewById(R.id.dayOverviewFragmentTimeList);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, 0);

//		final DiaryDayOverview templateObject = new DiaryDayOverview();
		String theDay = MetrixDateTimeHelper.formatDate(dateParam.getYear(), dateParam.getMonth(), dateParam.getDate());
		final CalendarDayData templateObject = new CalendarDayData(theDay);

		adapter = new DayOverviewRecyclerViewAdapter(getActivity(), templateObject, this);
		templateObject.createCalendarItem();
		recyclerView.setAdapter(adapter);


		FrameLayout emptyList = (FrameLayout) layout
				.findViewById(R.id.dayOverviewFragmentTimeListContainer);
		emptyList.setOnClickListener(this);

		sumJobHoursView = (TextView) layout
				.findViewById(R.id.dayOverviewFragmentSumJobHours);
		expectedJobHoursView = (TextView) layout
				.findViewById(R.id.dayOverviewFragmentExpectedJobHours);
		sumJobHoursLabel = (TextView) layout
				.findViewById(R.id.dayOverviewFragmentSumJobHoursHeader);
		expectedJobHoursLabel = (TextView) layout
				.findViewById(R.id.dayOverviewFragmentExpectedJobHoursHeader);

//		getLoaderManager().initLoader(SUMMARY_LOADER_ID, null, this);

		lblDate = (TextView) layout
				.findViewById(R.id.dayOverviewFragmentDateText);
		weekDayHeader = (TextView) layout
				.findViewById(R.id.dayOverviewFragmentWeekDayHeader);
		monthYearHeader = (TextView) layout
				.findViewById(R.id.dayOverviewFragmentMonthHeader);

		stateIcon = (ImageView) layout
				.findViewById(R.id.dayOverviewFragmentStateIcon);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			stateIcon.setImageAlpha(40);
		} else {
			// noinspection deprecation
			stateIcon.setAlpha(60);
		}

		mAddButton = (Button) layout.findViewById(R.id.addF);
		mAddButton.setOnClickListener(this);
		mAddExceptionButton = (Button) layout.findViewById(R.id.addException);
		mAddExceptionButton.setOnClickListener(this);
      
		String buttonBackgroundColor = MetrixSkinManager.getPrimaryColor();
		MetrixActivity.setMaterialDesignForButtons(mAddButton,buttonBackgroundColor, this.getActivity());
		MetrixActivity.setMaterialDesignForButtons(mAddExceptionButton,buttonBackgroundColor, this.getActivity());      

		this.setSummary();
		return layout;
	}

	@Override
	public void onAttach(Activity activity) {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "onAttach()");
		super.onAttach(activity);
//		try {
//			dayOverviewInterface = (DayOverviewFragmentInterface) activity;
//		} catch (ClassCastException e) {
//			throw new ClassCastException(activity.toString()
//					+ " must implement DayOverviewInterface");
//		}
	}

	@Override
	public void onDetach() {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "onDetach()");
		super.onDetach();
//		dayOverviewInterface = null;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(ERROR_DIALOG_SHOWN, errorDialogShown);
		outState.putParcelable(DateParam.DATE_PARAM, dateParam);
	}

//	@Override
//	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//		if (BuildConfig.DEBUG)
//			Log.d(TAG, "onCreateOptionsMenu(): " + dateParam.getIsoDateString());
//		inflater.inflate(R.menu.day_overview_fragment, menu);
//		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
//			rootMenu = menu;
//		}
//
//		boolean projectsEnabled = PropertyUtils.get(getActivity(), BasicDataCheckService.PREF_PROJECT_ENABLED, false);
//		boolean workOrdersEnabled = PropertyUtils.get(getActivity(), BasicDataCheckService.PREF_WORK_ORDER_ENABLED, false);
//		menu.findItem(R.id.action_project).setEnabled(projectsEnabled).setVisible(projectsEnabled);
//		menu.findItem(R.id.action_work_order).setEnabled(workOrdersEnabled).setVisible(workOrdersEnabled);
//	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (BuildConfig.DEBUG)
			Log.d(TAG,
					"onOptionsItemSelected(): " + dateParam.getIsoDateString());
//		switch (item.getItemId()) {
//		case R.id.action_project:
//			dayOverviewInterface.addProjectTime(dateParam);
//			return true;
//		case R.id.action_work_order:
//			dayOverviewInterface.addWorkOrderTime(dateParam);
//			return true;
//		case R.id.action_wage_code:
//			dayOverviewInterface.addWageCodeTime(dateParam);
//			return true;
//		case R.id.action_copy_previous_day:
//			if (adapter != null && !adapter.isEmpty()) {
//				ConfirmOverwriteDayDialog fragment = ConfirmOverwriteDayDialog
//						.newInstance(dateParam);
//				fragment.show(getChildFragmentManager().beginTransaction(),
//						"confirmOverwriteDay");
//			} else {
//				CopyPreviousDayTask.Param param = new CopyPreviousDayTask.Param();
//				param.context = getActivity().getApplicationContext();
//				param.copyTo = dateParam;
//				new CopyPreviousDayTask().execute(param);
//			}
//			return true;
//		case R.id.action_add_time:
//			// Workaround for the following bug:
//			// https://code.google.com/p/android/issues/detail?id=59679
//			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
//				if (!menuExpanded) {
//					menuExpanded = true;
//					// This call is recursive so the flag above is meant to only
//					// call this once
//					rootMenu.performIdentifierAction(R.id.action_add_time, 0);
//					menuExpanded = false;
//					return true;
//				}
//			}
//		}
		return super.onOptionsItemSelected(item);
	}

//	@Override
//	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//		int year = dateParam.getYear();
//		int month = dateParam.getMonth();
//		int day = dateParam.getDate();
//		
//		Object item = adapter.getItem(position);
//		@SuppressWarnings("unchecked")
//		DataItem selectedItem = (DataItem) item;
//		String lineCode = selectedItem.LineCode;
//		
//		String theDate = MetrixDateTimeHelper.formatDate(year, month, day);
//		Intent intent = new Intent(getActivity(), DebriefNonPartUsageList.class);
//		intent.putExtra("CalendarDay", theDate);
//		intent.putExtra("PreviousActivity", "DayOverviewActivity");		
//		if(!MetrixStringHelper.isNullOrEmpty(lineCode))
//			intent.putExtra("LineCode", lineCode);
//
//		MetrixActivityHelper.startNewActivity(getActivity(), intent);
//		
////		if (dayOverviewInterface != null) {
////			DiaryDayOverview item = adapter.getItem(position);
////
////			if (item.codeType.getValue() == DiaryDayOverview.TYPE_PROJECT
////					&& PropertyUtils.get(getActivity(), BasicDataCheckService.PREF_PROJECT_ENABLED, false)) {
////				// Edit project time
////				ArrayList<ProjectTimeParam> paramList = new ArrayList<ProjectTimeParam>();
////				paramList.add(new ProjectTimeParam(item));
////				for (int i = 0; i < adapter.getCount(); i++) {
////					DiaryDayOverview ddo = adapter.getItem(i);
////					if (ddo.codeType.getValue() == DiaryDayOverview.TYPE_PROJECT
////							&& i != position) {
////						paramList.add(new ProjectTimeParam(ddo));
////					}
////				}
////				dayOverviewInterface.setProjectTime(paramList
////						.toArray(new ProjectTimeParam[paramList.size()]));
////			} else if (item.codeType.getValue() == DiaryDayOverview.TYPE_WORK_ORDER
////					&& PropertyUtils.get(getActivity(), BasicDataCheckService.PREF_WORK_ORDER_ENABLED, false)) {
////				// Edit work order time
////				ArrayList<WorkOrderTimeParam> paramList = new ArrayList<WorkOrderTimeParam>();
////				paramList.add(new WorkOrderTimeParam(item));
////				for (int i = 0; i < adapter.getCount(); i++) {
////					DiaryDayOverview ddo = adapter.getItem(i);
////					if (ddo.codeType.getValue() == DiaryDayOverview.TYPE_WORK_ORDER
////							&& i != position) {
////						paramList.add(new WorkOrderTimeParam(ddo));
////					}
////				}
////				dayOverviewInterface.setWorkOrderTime(paramList
////						.toArray(new WorkOrderTimeParam[paramList.size()]));
////			} else if (item.codeType.getValue() == DiaryDayOverview.TYPE_WAGE_CODE) {
////				// Edit wage code time
////				dayOverviewInterface
////						.setWageCodeTime(new WageCodeTimeParam(item));
////			}
////		}
//	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.addF:
				Intent intent = MetrixActivityHelper.createActivityIntent(getActivity(), AddLabor.class);
				intent.putExtra("PlannedWorkDate", MetrixDateTimeHelper.convertDateTimeFromDBToUI(dateParam.getIsoDateString()));
				MetrixActivityHelper.startNewActivity(getActivity(), intent);
				break;
			case R.id.addException:
				intent = MetrixActivityHelper.createActivityIntent(getActivity(), CalendarException.class);
				intent.putExtra("PreviousActivity", "DayOverviewActivity");
				intent.putExtra("CalendarDay", MetrixDateTimeHelper.convertDateTimeFromDBToUI(dateParam.getIsoDateString()));
				MetrixActivityHelper.startNewActivity(getActivity(), intent);
				break;
			default:
				break;
		}
	}

	private void showErrorMessage(String errorMessage) {
		if (!errorDialogShown) {
			errorDialogShown = true;
			FragmentTransaction ft = getChildFragmentManager()
					.beginTransaction();
			ErrorDialog errorDialog = ErrorDialog.newInstance(errorMessage,
					dateParam);
			errorDialog.show(ft, "errorDialog");
		}
	}

	@Override
	public void onDayOverviewClicked(int position, DataItem item) {
		mSelectedPosition = position;

		final OnClickListener modifyListener = (dialog, which) -> {
			try {
				DataItem selectedItem = adapter.mListData.get(mSelectedPosition);

				String taskId = selectedItem.TaskId;

				if(MetrixStringHelper.isNullOrEmpty(taskId) == false && taskId.equalsIgnoreCase("CalendarException")){
					Intent intent = MetrixActivityHelper.createActivityIntent(getActivity(), CalendarException.class, MetrixTransactionTypes.UPDATE,
							"metrix_row_id", selectedItem.metrixRowId);
					intent.putExtra("CalendarDay", MetrixDateTimeHelper.convertDateTimeFromDBToUI(dateParam.getIsoDateString()));
					MetrixActivityHelper.startNewActivity(getActivity(), intent);
				}
				else {
					Intent intent = MetrixActivityHelper.createActivityIntent(getActivity(), AddLabor.class, MetrixTransactionTypes.UPDATE,
							"metrix_row_id", selectedItem.metrixRowId);
					MetrixActivityHelper.startNewActivity(getActivity(), intent);
				}
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		final OnClickListener deleteListener = (dialog, which) -> {
			try {
				DataItem selectedItem = adapter.mListData.get(mSelectedPosition);

				String metrixRowId = selectedItem.metrixRowId;
				String taskId = selectedItem.TaskId;

				if(MetrixStringHelper.isNullOrEmpty(taskId) == false && taskId.equalsIgnoreCase("CalendarException")){
					String exceptionId = MetrixDatabaseManager.getFieldStringValue("person_cal_except", "exception_id",
							"metrix_row_id=" + metrixRowId);

					MetrixUpdateManager.delete(getActivity(), "person_cal_except", metrixRowId, "exception_id", exceptionId, AndroidResourceHelper.getMessage("CalendarException"), new MetrixTransaction());
					adapter.mListData.remove(mSelectedPosition);
					adapter.notifyItemRemoved(mSelectedPosition);
				}
				else {
					String npuId = MetrixDatabaseManager.getFieldStringValue("non_part_usage", "npu_id",
							"metrix_row_id=" + metrixRowId);

					MetrixUpdateManager.delete(getActivity(), "non_part_usage", metrixRowId, "npu_id", npuId, AndroidResourceHelper.getMessage("Labor"), new MetrixTransaction());
					adapter.mListData.remove(mSelectedPosition);
					adapter.notifyItemRemoved(mSelectedPosition);
				}
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		MetrixDialogAssistant.showEditOrDeleteDialog(AndroidResourceHelper.getMessage("TimeRptItemLCase"), modifyListener, deleteListener, getActivity());
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated to
	 * the activity and potentially other fragments contained in that activity.
	 */
//	public interface DayOverviewFragmentInterface {
//		public void addProjectTime(DateParam dateParam);
//
//		public void addWageCodeTime(DateParam dateParam);
//
//		public void addWorkOrderTime(DateParam dateParam);
//
//		public void addRecentTime(DateParam dateParam);
//
//		public void setProjectTime(ProjectTimeParam[] projectTime);
//
//		public void setWageCodeTime(WageCodeTimeParam wageCodeTime);
//
//		public void setWorkOrderTime(WorkOrderTimeParam[] workOrderTime);
//	}


		static class DayOverviewRecyclerViewAdapter extends RecyclerView.Adapter<DayOverviewRecyclerViewAdapter.DailyOverviewVH> {

		public CalendarDayData mDayData;
		public List<DataItem> mListData;
		private final Context context;
		private final DayOverviewClickListener listener;

		public DayOverviewRecyclerViewAdapter(Context context, CalendarDayData templateObject, DayOverviewClickListener listener) {
			this.mDayData = templateObject;
			this.mListData = templateObject.WorkItemList;
			this.context = context;
			this.listener = listener;
		}

		@NonNull
		@Override
		public DailyOverviewVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			final View view = LayoutInflater.from(context).inflate(R.layout.listitem_day_overview, parent, false);
			return new DailyOverviewVH(view);
		}

		@Override
		public void onBindViewHolder(@NonNull DailyOverviewVH holder, int position) {
			final DataItem dataObject = this.mDayData.WorkItemList.get(position);

			String backgroundColor = MetrixSkinManager.getSecondaryColor();
			String hourColor = MetrixSkinManager.getPrimaryColor();

			holder.hours.setText(""+MetrixFloatHelper.round(dataObject.Duration, 2));
			if(!MetrixStringHelper.isNullOrEmpty(hourColor))
				holder.hours.setTextColor(Color.parseColor(hourColor));

			holder.taskId.setText(dataObject.TaskId);
			holder.metrixRowId.setText(dataObject.metrixRowId);
			holder.lineCode.setText(dataObject.Description);
			holder.reportCode.setText(dataObject.CreatedDateTime);
			if(!MetrixStringHelper.isNullOrEmpty(backgroundColor))
				holder.reportCode.setTextColor(Color.parseColor(backgroundColor));
			if(!MetrixStringHelper.isNullOrEmpty(hourColor))
				holder.divider.setColorFilter(MetrixSkinManager.getColorFilter(hourColor));

			if(dataObject.TaskId != null && dataObject.TaskId.equalsIgnoreCase("CalendarException")) {
				Resources resource = context.getResources();
				holder.layout.setBackgroundColor(resource.getColor(R.color.IFSLightGray));
				holder.taskId.setVisibility(View.INVISIBLE);
			}
		}

		@Override
		public int getItemCount() {
			return (mListData == null) ? 0 : mListData.size();
		}

		class DailyOverviewVH extends RecyclerView.ViewHolder {
			private final TextView hours;
			private final TextView taskId;
			private final TextView metrixRowId;
			private final TextView lineCode;
			private final TextView reportCode;
			private final ImageView divider;
			protected final LinearLayout layout;

				public DailyOverviewVH(View itemView) {
					super(itemView);
					hours = itemView.findViewById(R.id.hoursView);
					taskId = itemView.findViewById(R.id.reportItemTaskId);
					metrixRowId = itemView.findViewById(R.id.nonPartUsage_metrix_row_id);
					lineCode =  itemView.findViewById(R.id.reportItemHeaderTitle);
					reportCode = itemView.findViewById(R.id.reportItemHeaderReportCode);
					divider = itemView.findViewById(R.id.item_divider);
					layout = itemView.findViewById(R.id.listLayout);

					itemView.setOnClickListener((v) -> {
						if (listener != null) {
							final int position = getAdapterPosition();
							if (position != RecyclerView.NO_POSITION)
								listener.onDayOverviewClicked(position, mListData.get(position));
						}
					});
				}
			}
	}

//	@Override
//	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
//		if (id == SUMMARY_LOADER_ID) {
//			Query<DiaryDay> query = QueryBuilder.selectAllFrom(diaryDay)
//					.where(diaryDay.dayDate).is(dateParam.toJavaDate())
//					.getQuery();
//			return LoaderHelper.query(getActivity(), query);
//		}
//		return null;
//	}

//	@Override
//	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
//		if (cursor != null && cursor.moveToFirst()) {
//			diaryDay.valuesFromCursor(cursor);
//
//			if (diaryDay.errorMessage.getValue() != null) {
//				showErrorMessage(diaryDay.errorMessage.getValue());
//			}
//
//			double jobHours = diaryDay.jobHours.getValue();
//			double remainingHours = -1.0;
////			if (diaryDay.scheduledHours.getValue() != null) {
////				remainingHours = Math.max(
////						0,
////						diaryDay.scheduledHours.getValue()
////								+ diaryDay.wageHours.getValue()
////								- diaryDay.jobHours.getValue());
////				remainingJobHoursView.setText(DefaultDecimalFormat
////						.getInstance().format(remainingHours));
////
////			} else {
////				remainingJobHoursView.setVisibility(View.GONE);
////				remainingJobHoursLabel.setVisibility(View.GONE);
////			}
//
//			if (jobHours > 0) {
////				sumJobHoursView.setText(DefaultDecimalFormat.getInstance()
////						.format(jobHours));
//				sumJobHoursView.setVisibility(View.VISIBLE);
//				sumJobHoursLabel.setVisibility(View.VISIBLE);
//			} else {
//				sumJobHoursView.setVisibility(View.GONE);
//				sumJobHoursLabel.setVisibility(View.GONE);
//			}
//
////			if (diaryDay.isConfirmed.getValue()) {
////				stateIcon.setImageLevel(3);
////			} else 
//			if (remainingHours == 0) {
//				stateIcon.setImageLevel(2);
//			} else if (jobHours > 0) {
//				stateIcon.setImageLevel(1);
//			} else {
//				stateIcon.setImageLevel(0);
//			}
//		} else { // No schedule or summary information available for this day
//			stateIcon.setImageLevel(0);
//			sumJobHoursLabel.setVisibility(View.GONE);
//			sumJobHoursView.setVisibility(View.GONE);
//			remainingJobHoursView.setVisibility(View.GONE);
//			remainingJobHoursLabel.setVisibility(View.GONE);
//		}
//	}

	private void setSummary() {
		int year = this.dateParam.getYear();
		int month = this.dateParam.getMonth();
		int day = this.dateParam.getDate();
		String dateValue = MetrixDateTimeHelper.formatDate(year, month, day);

		CalendarDayData dayData = new CalendarDayData(dateValue);
		dayData.calculateJobHours("");
		double wageHours = dayData.getExpectedWorkHour();
		boolean hasException = dayData.hasExceptionHour();
		double jobHours = dayData.JobHours;

		if (wageHours > 0) {
			double remainingHours = wageHours - jobHours;

			//		if (diaryDay.scheduledHours.getValue() != null) {
			//			remainingHours = Math.max(
			//					0,
			//					diaryDay.scheduledHours.getValue()
			//							+ diaryDay.wageHours.getValue()
			//							- diaryDay.jobHours.getValue());
			//			remainingJobHoursView.setText(DefaultDecimalFormat
			//					.getInstance().format(remainingHours));
			//
			//		} else {
			//			remainingJobHoursView.setVisibility(View.GONE);
			//			remainingJobHoursLabel.setVisibility(View.GONE);
			//		}

			if (jobHours > 0) {
				sumJobHoursView.setText(""+jobHours);
				sumJobHoursView.setVisibility(View.VISIBLE);
				sumJobHoursLabel.setVisibility(View.VISIBLE);
			} else {
				sumJobHoursView.setVisibility(View.GONE);
				sumJobHoursLabel.setVisibility(View.GONE);
			}

			if(wageHours > 0) {
				expectedJobHoursView.setText(""+wageHours);
				expectedJobHoursView.setVisibility(View.VISIBLE);
				expectedJobHoursLabel.setVisibility(View.VISIBLE);
			}
			else {
				expectedJobHoursView.setVisibility(View.GONE);
				expectedJobHoursLabel.setVisibility(View.GONE);
			}

			if (jobHours == 0) {
				if(hasException)
					stateIcon.setImageLevel(5);
				else
					stateIcon.setImageLevel(2);
			} else if (jobHours > 0 && remainingHours <= 0) {
				if(hasException)
					stateIcon.setImageLevel(6);
				else
					stateIcon.setImageLevel(3);
			} else if(jobHours > 0 && remainingHours>0){
				if(hasException)
					stateIcon.setImageLevel(4);
				else
					stateIcon.setImageLevel(1);
			} else {
				if(hasException)
					stateIcon.setImageLevel(4);
				else
					stateIcon.setImageLevel(0);
			}
		} else { // No schedule or summary information available for this day
			if(hasException) {
				stateIcon.setImageLevel(6);
			}

			if(jobHours == 0) {
				sumJobHoursLabel.setVisibility(View.GONE);
				sumJobHoursView.setVisibility(View.GONE);
			}
			else {
				sumJobHoursView.setText(""+jobHours);
				sumJobHoursView.setVisibility(View.VISIBLE);
				sumJobHoursLabel.setVisibility(View.VISIBLE);
			}

			expectedJobHoursView.setText(""+wageHours);
			expectedJobHoursView.setVisibility(View.VISIBLE);
			expectedJobHoursLabel.setVisibility(View.VISIBLE);
		}
	}

//	@Override
//	public void onLoaderReset(Loader<Cursor> loader) {
//	}

	public static class ErrorDialog extends DialogFragment implements
			DialogInterface.OnClickListener {
		private static final String ERROR_MESSAGE = "error_message";

		private String errorMessage;
		private DateParam dateParam;

		public static ErrorDialog newInstance(String errorMessage,
											  DateParam dateParam) {
			Bundle b = new Bundle();
			b.putString(ERROR_MESSAGE, errorMessage);
			b.putParcelable(DateParam.DATE_PARAM, dateParam);

			ErrorDialog errorDialog = new ErrorDialog();
			errorDialog.setArguments(b);
			return errorDialog;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			if (getArguments() != null) {
				errorMessage = getArguments().getString(ERROR_MESSAGE);
				dateParam = getArguments().getParcelable(DateParam.DATE_PARAM);
			}
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new AlertDialog.Builder(getActivity())
					.setTitle(AndroidResourceHelper.getMessage("TitleDialogErrorSyncDay"))
					.setMessage(errorMessage)
					.setNeutralButton(AndroidResourceHelper.getMessage("Ok"), this)
					.setPositiveButton(AndroidResourceHelper.getMessage("DiscardChanges"), this).create();
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
//				DiscardDayTask.DiscardParam discardParam = new DiscardDayTask.DiscardParam();
//				discardParam.context = getActivity();
//				discardParam.dateParam = dateParam;
//				discardParam.dayDiscardlistener = (DayDiscardListener) getParentFragment();
//				new DiscardDayTask().execute(discardParam);
			}
		}
	}

	public static class ConfirmOverwriteDayDialog extends DialogFragment
			implements DialogInterface.OnClickListener {
		private DateParam copyToDate;

		public static ConfirmOverwriteDayDialog newInstance(DateParam copyToDate) {
			Bundle b = new Bundle();
			b.putParcelable(DateParam.DATE_PARAM, copyToDate);

			ConfirmOverwriteDayDialog confirmOverwriteDayDialog = new ConfirmOverwriteDayDialog();
			confirmOverwriteDayDialog.setArguments(b);
			return confirmOverwriteDayDialog;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			if (getArguments() != null) {
				copyToDate = getArguments().getParcelable(DateParam.DATE_PARAM);
			}
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new AlertDialog.Builder(getActivity())
					.setTitle(AndroidResourceHelper.getMessage("ConfirmCopyPreviousDialogHeader"))
					.setMessage(AndroidResourceHelper.getMessage("ConfirmCopyPreviousMessage"))
					.setPositiveButton(AndroidResourceHelper.getMessage("Yes"), this)
					.setNegativeButton(AndroidResourceHelper.getMessage("No"), this).create();
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
//				CopyPreviousDayTask.Param param = new CopyPreviousDayTask.Param();
//				param.context = getActivity().getApplicationContext();
//				param.copyTo = copyToDate;
//				new CopyPreviousDayTask().execute(param);
			}
		}
	}

//	@Override
//	public void onDayDiscard() {
//		new Handler(getActivity().getMainLooper()).post(new Runnable() {
//
//			@Override
//			public void run() {
//				getLoaderManager().restartLoader(DayOverviewAdapter.LOADER_ID, null, adapter);
//			}
//		});
//		discardDayTask.detachDisacrdListener();
//		discardDayTask = null;
//	}
//
//	@Override
//	public void onDiscardListenerAttach(DiscardDayTask task) {
//		discardDayTask = task;
//	}
}

