package com.metrix.architecture.ui.widget;

import java.util.Calendar;
import java.util.TimeZone;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.metrix.architecture.metadata.DateParam;
import com.metrix.metrixmobile.BuildConfig;
import com.metrix.metrixmobile.R;

public class CalendarPagerFragment extends Fragment {
	private static final String TAG = CalendarPagerFragment.class.getSimpleName();
	ViewPager viewPager;

	public static CalendarPagerFragment newInstance() {
		return new CalendarPagerFragment();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (BuildConfig.DEBUG) Log.d(TAG, "onCreateView()");
		View v = inflater.inflate(R.layout.fragment_month_overview, container, false);
		assert v != null;

		viewPager = v.findViewById(R.id.monthOverviewCalendarPager);
		if (viewPager.getAdapter() != null) {
			if (BuildConfig.DEBUG) Log.d(TAG, "onCreateView(): pager has adapter");
		}
		viewPager.setAdapter(new CalendarPagerAdapter(getChildFragmentManager()));
		viewPager.setOffscreenPageLimit(2);
		viewPager.setCurrentItem(1);
		return v;
	}

	static class CalendarPagerAdapter extends FragmentPagerAdapter {
		private DateParam[] monthList = new DateParam[3];

		public CalendarPagerAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
			// First get today's date in the users local timezone
			Calendar todayCal = Calendar.getInstance();
			// Then create a calendar normalized to GMT that we use to populate month list
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			cal.clear();
			//noinspection MagicConstant
			cal.set(todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH), 1);
			cal.add(Calendar.MONTH, -1);
			for (int i = 0; i < monthList.length; i++) {
				monthList[i] = new DateParam(cal.getTime());
				cal.add(Calendar.MONTH, 1);
			}
		}

		@Override
		public Fragment getItem(int position) {
			return CalendarFragment.newInstance(monthList[position]);
		}

		@Override
		public int getCount() {
			return monthList.length;
		}

		public DateParam getMonth(int position) {
			return monthList[position];
		}
	}

	public CalendarFragment getNewInstanceOfCurrentItem() {
		return (CalendarFragment)((CalendarPagerAdapter) viewPager.getAdapter()).getItem(viewPager.getCurrentItem());
	}
}
