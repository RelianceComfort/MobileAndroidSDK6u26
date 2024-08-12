package com.metrix.metrixmobile;

import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.metrix.architecture.ui.widget.CalendarPagerFragment;
import com.metrix.metrixmobile.system.MetrixActivity;

public class TimeReporting extends MetrixActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_month_overview);

		FragmentManager fm = getSupportFragmentManager();
		if (fm.findFragmentById(R.id.monthOverviewFragment) == null) {
			final CalendarPagerFragment pagerFragment = CalendarPagerFragment.newInstance();
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(R.id.monthOverviewFragment, pagerFragment);
			ft.commit();
		}
	}
}