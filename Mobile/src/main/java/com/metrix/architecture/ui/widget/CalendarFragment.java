package com.metrix.architecture.ui.widget;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.metadata.DateParam;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.BuildConfig;
import com.metrix.metrixmobile.DayOverviewActivity;
import com.metrix.metrixmobile.R;

public class CalendarFragment extends Fragment implements TimeReportingCalendarAdapter.CalendarListener {
    private static final String TAG = CalendarFragment.class.getSimpleName();

    // Fragment initialization data
    private DateParam dateParam;

    // Fragment UI Elements
    private TextView calendarMonthHeader;
    private TextView[] calendarWeekDayHeaders = new TextView[7];
    private ImageView leftArrow, rightArrow;
    private RecyclerView rvCalendarDays;
    private TimeReportingCalendarAdapter adapter;

    /**
     * Use this factory method to create a new instance of this fragment using
     * the provided parameters.
     *
     * @return A new instance of fragment CalendarFragment.
     */
    public static CalendarFragment newInstance(DateParam dateParam) {
        if (BuildConfig.DEBUG) Log.d(TAG, "newInstance(): " + dateParam.getIsoDateString());
        CalendarFragment fragment = new CalendarFragment();
        Bundle args = new Bundle();
        args.putParcelable(DateParam.DATE_PARAM, dateParam);
        fragment.setArguments(args);
        return fragment;
    }

    public CalendarFragment() {
    } // Required empty public constructor

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            dateParam = getArguments().getParcelable(DateParam.DATE_PARAM);
        } else {
            dateParam = new DateParam();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_calendar, container, false);

        rvCalendarDays = layout.findViewById(R.id.rvCalendarDays);
        rvCalendarDays.setLayoutManager(new GridLayoutManager(getContext(), 7));
        rvCalendarDays.addItemDecoration(new CalendarDividerItemDecoration(getContext(), 7));

        calendarMonthHeader = (TextView) layout.findViewById(R.id.calendarMonthHeader);
        calendarWeekDayHeaders[0] = (TextView) layout.findViewById(R.id.calendarWeekDay1);
        calendarWeekDayHeaders[1] = (TextView) layout.findViewById(R.id.calendarWeekDay2);
        calendarWeekDayHeaders[2] = (TextView) layout.findViewById(R.id.calendarWeekDay3);
        calendarWeekDayHeaders[3] = (TextView) layout.findViewById(R.id.calendarWeekDay4);
        calendarWeekDayHeaders[4] = (TextView) layout.findViewById(R.id.calendarWeekDay5);
        calendarWeekDayHeaders[5] = (TextView) layout.findViewById(R.id.calendarWeekDay6);
        calendarWeekDayHeaders[6] = (TextView) layout.findViewById(R.id.calendarWeekDay7);

        leftArrow = (ImageView) layout.findViewById(R.id.leftArrow);
        rightArrow = (ImageView) layout.findViewById(R.id.rightArrow);

        Calendar today = Calendar.getInstance();
        if (dateParam.getMonth() == today.get(Calendar.MONTH) + 1) {
            rightArrow.setVisibility(View.INVISIBLE);
        } else if (dateParam.getMonth() == today.get(Calendar.MONTH) - 1) {
            leftArrow.setVisibility(View.INVISIBLE);
        }

        setHeaders();

        AndroidResourceHelper.setResourceValues(layout.findViewById(R.id.label_confirmed231dd8a9), "LabelConfirmed");
        AndroidResourceHelper.setResourceValues(layout.findViewById(R.id.label_calendar_exception0b18079f), "LabelCalendarException");
        AndroidResourceHelper.setResourceValues(layout.findViewById(R.id.label_full_day_reportc38740d5), "LabelFullDayReport");
        AndroidResourceHelper.setResourceValues(layout.findViewById(R.id.label_partial_day_report8b13870f), "LabelPartialDayReport");
        AndroidResourceHelper.setResourceValues(layout.findViewById(R.id.lbl_confirmed), "LabelConfirmed");
        AndroidResourceHelper.setResourceValues(layout.findViewById(R.id.lbl_full_day_report), "LabelFullDayReport");
        AndroidResourceHelper.setResourceValues(layout.findViewById(R.id.lbl_partial_day_report), "LabelPartialDayReport");

        View headerDevider = layout.findViewById(R.id.calendarMonthHeaderDivider);

        String backgroundColor = MetrixSkinManager.getSecondaryColor();
        String textColor = MetrixSkinManager.getSecondGradientTextColor();
        String primaryColor = MetrixSkinManager.getPrimaryColor();

        if (!MetrixStringHelper.isNullOrEmpty(backgroundColor)) {
            calendarWeekDayHeaders[0].setBackgroundColor(Color.parseColor(backgroundColor));
            calendarWeekDayHeaders[1].setBackgroundColor(Color.parseColor(backgroundColor));
            calendarWeekDayHeaders[2].setBackgroundColor(Color.parseColor(backgroundColor));
            calendarWeekDayHeaders[3].setBackgroundColor(Color.parseColor(backgroundColor));
            calendarWeekDayHeaders[4].setBackgroundColor(Color.parseColor(backgroundColor));
            calendarWeekDayHeaders[5].setBackgroundColor(Color.parseColor(backgroundColor));
            calendarWeekDayHeaders[6].setBackgroundColor(Color.parseColor(backgroundColor));
        }

        if (!MetrixStringHelper.isNullOrEmpty(textColor)) {
            calendarWeekDayHeaders[0].setTextColor(Color.parseColor(textColor));
            calendarWeekDayHeaders[1].setTextColor(Color.parseColor(textColor));
            calendarWeekDayHeaders[2].setTextColor(Color.parseColor(textColor));
            calendarWeekDayHeaders[3].setTextColor(Color.parseColor(textColor));
            calendarWeekDayHeaders[4].setTextColor(Color.parseColor(textColor));
            calendarWeekDayHeaders[5].setTextColor(Color.parseColor(textColor));
            calendarWeekDayHeaders[6].setTextColor(Color.parseColor(textColor));
        }

        if (!MetrixStringHelper.isNullOrEmpty(backgroundColor)) {
            leftArrow.setColorFilter(MetrixSkinManager.getColorFilter(backgroundColor));
            rightArrow.setColorFilter(MetrixSkinManager.getColorFilter(backgroundColor));
        } else {
            leftArrow.setColorFilter(MetrixSkinManager.getColorFilter("#8427E2"));
            rightArrow.setColorFilter(MetrixSkinManager.getColorFilter("#8427E2"));
        }

        if (!MetrixStringHelper.isNullOrEmpty(primaryColor)) {
            calendarMonthHeader.setTextColor(Color.parseColor(primaryColor));
            headerDevider.setBackgroundColor(Color.parseColor(primaryColor));
        }

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();

        adapter = new TimeReportingCalendarAdapter(dateParam.getYear(), dateParam.getMonth(), this);
        rvCalendarDays.setAdapter(adapter);
     }

    @Override
    public void onPause() {
        super.onPause();
        if (adapter != null)
            adapter.disposeObservables();
    }

    private void setHeaders() {
        DateFormatSymbols symbols = new DateFormatSymbols();

        calendarMonthHeader.setText(symbols.getMonths()[dateParam.getMonth()] + " " + dateParam.getYear());

        Calendar cal = Calendar.getInstance();
        cal.set(dateParam.getYear(), dateParam.getMonth(), 1);

        int firstDayOfWeek = cal.getFirstDayOfWeek();

        // Weekdays is a 1-based array. The 0 element is empty. A Simple modulo
        // won't work
        String[] weekdays = symbols.getShortWeekdays();
        for (int i = 0; i < 7; i++) {
            int dayOfWeek = firstDayOfWeek + i;
            if (dayOfWeek > 7) {
                dayOfWeek -= 7;
            }
            calendarWeekDayHeaders[i].setText(weekdays[dayOfWeek]);
        }
    }

    @Override
    public void onDayClicked(TimeReportingCalendarAdapter.CalendarDate date, int position) {
        // FIXME: Check if new activity needed or if we have a multi-pane layout.
        Intent intent = new Intent(getActivity(), DayOverviewActivity.class);
        intent.putExtra(DateParam.DATE_PARAM, new DateParam(date.yearValue, date.monthValue, date.dateValue));
        startActivity(intent);
    }

    public static class CalendarDividerItemDecoration extends RecyclerView.ItemDecoration {
        private static final int[] ATTRS = new int[]{16843284};
        private Drawable divider;
        private final int span;
        private final Rect bounds = new Rect();

        public CalendarDividerItemDecoration(Context context, int span) {
            TypedArray a = context.obtainStyledAttributes(ATTRS);
            this.divider = a.getDrawable(0);
            this.span = span;
            a.recycle();
        }

        public void setDrawable(@NonNull Drawable drawable) {
            this.divider = drawable;
        }

        public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            if (parent.getLayoutManager() != null && this.divider != null) {
                canvas.save();
                int top;
                int bottom;
                int childCount = parent.getChildCount();

                for (int i = 0; i < childCount; ++i) {
                    View child = parent.getChildAt(i);
                    parent.getLayoutManager().getDecoratedBoundsWithMargins(child, this.bounds);
                    int right = this.bounds.right + Math.round(child.getTranslationX());
                    int left = right - this.divider.getIntrinsicWidth();
                    this.divider.setBounds(left, bounds.top, right, bounds.bottom);
                    this.divider.draw(canvas);

                    if (i == 0 || i % span == 0) {
                        this.divider.setBounds(0, bounds.top, this.divider.getIntrinsicWidth(), bounds.bottom);
                        this.divider.draw(canvas);
                    }

                    bottom = this.bounds.bottom + Math.round(child.getTranslationY());
                    top = bottom - this.divider.getIntrinsicHeight();
                    this.divider.setBounds(bounds.left, top, bounds.right, bottom);
                    this.divider.draw(canvas);
                }

                canvas.restore();
            }
        }

        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            if (this.divider == null)
                outRect.set(0, 0, 0, 0);
            else
                outRect.set(this.divider.getIntrinsicHeight(), this.divider.getIntrinsicHeight(), this.divider.getIntrinsicHeight(), this.divider.getIntrinsicHeight());
        }
    }
}
