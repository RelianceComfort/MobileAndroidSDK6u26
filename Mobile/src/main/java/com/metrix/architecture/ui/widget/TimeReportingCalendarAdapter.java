package com.metrix.architecture.ui.widget;

import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.CalendarDayData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TimeReportingCalendarAdapter extends RecyclerView.Adapter<TimeReportingCalendarAdapter.CalendarItemVH> {
    private final String dayHasLocalModifications;
    private final String errorSavingDataToServer;
    private final int year, month;
    private final CalendarListener listener;
    private List<CalendarDate> daysData;
    private CompositeDisposable disposables = new CompositeDisposable();

    public TimeReportingCalendarAdapter(int year, int month, CalendarListener listener) {
        dayHasLocalModifications = AndroidResourceHelper.getMessage("DayHasLocalModifications");
        errorSavingDataToServer = AndroidResourceHelper.getMessage("ErrorSavingDataToServer");
        this.year = year;
        this.month = month;
        this.listener = listener;
        this.daysData = generateDaysData();
    }

    private List<CalendarDate> generateDaysData() {
        List<CalendarDate> dateList = new ArrayList<>();

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT")); // Yes we need to normalize all dates and times to GMT, this does not affect locale
        Calendar localToday = Calendar.getInstance();
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        today.setTimeInMillis(0L);
        today.set(localToday.get(Calendar.YEAR), localToday.get(Calendar.MONTH), localToday.get(Calendar.DATE));

        cal.setTimeInMillis(0L);
        cal.set(year, month, 1);

        int firstDayOfWeek = cal.getFirstDayOfWeek();
        int firstDayOfMonth = cal.get(Calendar.DAY_OF_WEEK);

        // Pad start of grid with dates from previous month
        for (int i = 0; i < (firstDayOfMonth - firstDayOfWeek >= 0 ? firstDayOfMonth - firstDayOfWeek : 7 - firstDayOfWeek + firstDayOfMonth); i++) {
            cal.add(Calendar.DATE, -1);
            final CalendarDate date = new CalendarDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), cal.getTime());
            date.inMonth = false;
            dateList.add(0, date);
        }

        cal.set(year, month, 1);

        // Populate grid with current month
        while (cal.get(Calendar.MONTH) == month) {
            final CalendarDate date = new CalendarDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), cal.getTime());
            date.isToday = cal.compareTo(today) == 0;
            dateList.add(date);
            cal.add(Calendar.DATE, 1);
        }

        // Fill end of grid with dates from next month
        while (dateList.size() % 7 != 0) {
            CalendarDate date = new CalendarDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), cal.getTime());
            date.inMonth = false;
            dateList.add(date);
            cal.add(Calendar.DATE, 1);
        }

        return dateList;
    }

    private Observable<Pair<String, CalendarDate>> generateDayState(@NonNull CalendarDate date, @NonNull String uniqueRequestId) {
        return Observable.create((emitter) -> {
            final CalendarDayData dayData = new CalendarDayData(date);
            dayData.calculateJobHours("");
            double jobHours = dayData.JobHours;
            double wageHours = dayData.getExpectedWorkHour();
            double remainingHours = wageHours - jobHours;
            boolean hasException = dayData.hasExceptionHour();
            CalendarDayState state = new CalendarDayState();

            state.reportState = 0;
            if (wageHours > 0) {
                if (dayData.JobHours == 0) {
                    if (hasException)
                        state.reportState = 5;
                    else
                        state.reportState = 2;
                } else if (remainingHours <= 0 && remainingHours <= 0) {
                    if (hasException)
                        state.reportState = 6;
                    else
                        state.reportState = 3;
                } else if (dayData.JobHours > 0 && remainingHours > 0) {
                    if (hasException)
                        state.reportState = 4;
                    else
                        state.reportState = 1;
                } else {
                    if (hasException)
                        state.reportState = 4;
                    else
                        state.reportState = 0;
                }
            } else {
                if (hasException)
                    state.reportState = 6;
                else if (dayData.JobHours > 0)
                    state.reportState = 3;
            }

            state.hasError = false;
            state.isPendingSync = false;
            date.state = state;
            emitter.onNext(new Pair<>(uniqueRequestId, date));
            emitter.onComplete();
        });
    }

    private void updateItemForState(@NonNull CalendarItemVH vh, @NonNull CalendarDayState state) {
        vh.dateStateIcon.setImageLevel(state.reportState);
        vh.pendingChanges.setVisibility(state.isPendingSync ? View.VISIBLE : View.GONE);
        vh.hasError.setVisibility(state.hasError ? View.VISIBLE : View.GONE);
    }

    public void disposeObservables() {
        disposables.dispose();
        disposables = new CompositeDisposable();
    }

    public void refreshCalendar() {
        this.daysData = generateDaysData();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CalendarItemVH onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
        final View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.fragment_calendar_day, viewGroup, false);
        return new CalendarItemVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final CalendarItemVH vh, int pos) {
        final CalendarDate date = daysData.get(pos);
        vh.dateItemTextView.setText("" + date.dateValue);

        if (!date.inMonth) {
            vh.dateItemTextView.setTextColor(vh.itemView.getContext().getResources().getColor(R.color.IFSDarkGrey));
            vh.dateStateIcon.setImageAlpha(66);
            vh.calendarDayItem.setOnClickListener(null);
            vh.calendarDayItem.setClickable(false);
            vh.dateStateIcon.setImageLevel(0);
            vh.pendingChanges.setVisibility(View.GONE);
            vh.hasError.setVisibility(View.GONE);
        } else if (date.isToday) {
            int ifsGreen = vh.itemView.getContext().getResources().getColor(R.color.IFSGreen);
            final String secondaryColor = MetrixSkinManager.getSecondaryColor();
            if (!MetrixStringHelper.isNullOrEmpty(secondaryColor))
                ifsGreen = Color.parseColor(secondaryColor);

            vh.itemView.setBackgroundColor(Color.argb(102, Color.red(ifsGreen), Color.green(ifsGreen), Color.blue(ifsGreen)));
        } else
            vh.itemView.setBackground(null);

        if (date.inMonth) {
            vh.dateStateIcon.setImageAlpha(255);
            vh.calendarDayItem.setBackgroundResource(R.drawable.list_selector);
            vh.calendarDayItem.setClickable(true);

            if (date.state != null) {
                // State was already loaded. Use it
                updateItemForState(vh, date.state);
            } else {
                // State needs to be loaded from the database. Do it asynchronously to prevent UI stutter
                final String requestId = UUID.randomUUID().toString();
                vh.itemView.setTag(requestId);
                final Disposable subscription = generateDayState(date, requestId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((result) -> {
                            final String tag = result.first;
                            if (tag != null && tag.equals(vh.itemView.getTag()) && result.second != null)
                                updateItemForState(vh, result.second.state);
                        });
                disposables.add(subscription);
            }
        }
    }

    @Override
    public int getItemCount() {
        return daysData.size();
    }

    class CalendarItemVH extends RecyclerView.ViewHolder {
        LinearLayout calendarDayItem;
        TextView dateItemTextView;
        ImageView dateStateIcon;
        ImageView pendingChanges;
        ImageView hasError;

        CalendarItemVH(@NonNull View itemView) {
            super(itemView);
            calendarDayItem = itemView.findViewById(R.id.calendarDayItem);
            dateItemTextView = itemView.findViewById(R.id.calendarDayItemTextView);
            dateStateIcon = itemView.findViewById(R.id.calendarDayStateIcon);
            pendingChanges = itemView.findViewById(R.id.calendarDayItemModifiedIcon);
            hasError = itemView.findViewById(R.id.calendarDayItemWarningIcon);
            pendingChanges.setContentDescription(dayHasLocalModifications);
            hasError.setContentDescription(errorSavingDataToServer);

            calendarDayItem.setOnClickListener((v) -> {
                final int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION)
                    listener.onDayClicked(daysData.get(position), position);
            });
        }
    }

    public static class CalendarDate {
        public int yearValue;
        public int monthValue;
        public int dateValue;
        public Date timestamp;
        public boolean inMonth = true;
        public boolean isToday = false;
        public CalendarDayState state;

        CalendarDate(int yearValue, int monthValue, int dateValue, Date timestamp) {
            this.yearValue = yearValue;
            this.monthValue = monthValue;
            this.dateValue = dateValue;
            this.timestamp = timestamp;
        }
    }

    public static class CalendarDayState {
        public int reportState;
        public boolean hasError;
        public boolean isPendingSync;
    }

    public interface CalendarListener {
        void onDayClicked(CalendarDate date, int position);
    }
}
