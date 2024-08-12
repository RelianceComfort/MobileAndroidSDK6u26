package com.metrix.metrixmobile;

import java.util.Calendar;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.View;

import com.metrix.architecture.metadata.DateParam;
import com.metrix.architecture.ui.widget.DayOverviewFragment;
import com.metrix.metrixmobile.system.MetrixActivity;
//import com.ifsworld.appbase.IfsActivity;
//import com.ifsworld.timereporting.fragments.DayOverviewFragment;
//import com.ifsworld.timereporting.fragments.DayOverviewFragment.DayOverviewFragmentInterface;
//import com.ifsworld.timereporting.utils.DateParam;
//import com.ifsworld.timereporting.utils.ProjectTimeParam;
//import com.ifsworld.timereporting.utils.WageCodeTimeParam;
//import com.ifsworld.timereporting.utils.WorkOrderTimeParam;

public class DayOverviewActivity extends MetrixActivity implements View.OnClickListener// implements DayOverviewFragmentInterface 
{	
	private DateParam mDateParam; 
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_day_overview);
		
		Intent i = getIntent();

		mDateParam = i.getParcelableExtra(DateParam.DATE_PARAM);
		
		if(mDateParam == null)
			mDateParam = new DateParam();

		ViewPager viewPager = (ViewPager) findViewById(R.id.dayOverviewViewPager);
		viewPager.setOffscreenPageLimit(1);
		Calendar cal = Calendar.getInstance();
		cal.set(mDateParam.getYear(), mDateParam.getMonth(), mDateParam.getDate());
		viewPager.setAdapter(new DayPagerAdapter(getSupportFragmentManager(), cal));
		viewPager.setCurrentItem(DayPagerAdapter.START_OFFSET_DAYS);	
		//getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

//	@Override 
//	public void onStart() {
//		super.onStart();				
//	}	
/*
	private void showNewProject(DateParam dateParam) {
//		Intent intent = new Intent(this, ProjectActivity.class);
//		intent.putExtra(DateParam.DATE_PARAM, dateParam);
//		startActivity(intent);
	}
	
	private void showSetProjectTime(Bundle data) {
//		Intent intent = new Intent(this, ProjectActivity.class);
//		intent.putExtras(data);
//		startActivity(intent);
	}
	
	private void showNewWageCode(DateParam dateParam) {
//		Intent intent = new Intent(this, WageCodeActivity.class);
//		intent.putExtra(DateParam.DATE_PARAM, dateParam);
//		startActivity(intent);
	}

	private void showSetWageCodeTime(Bundle data) {
//		Intent intent = new Intent(this, WageCodeActivity.class);
//		intent.putExtras(data);
//		startActivity(intent);
	}
	
	private void showNewWorkOrder(DateParam dateParam) {
//		Intent intent = new Intent(this, WorkOrderActivity.class);
//		intent.putExtra(DateParam.DATE_PARAM, dateParam);
//		startActivity(intent);
	}
	
	private void showSetWorkOrderTime(Bundle data) {
//		Intent intent = new Intent(this, WorkOrderActivity.class);
//		intent.putExtras(data);
//		startActivity(intent);
	}

	private void showSetRecentTime(DateParam dateParam) {
//		Intent intent = new Intent(this, SetRecentTimeActivity.class);
//		intent.putExtra(DateParam.DATE_PARAM, dateParam);
//		startActivity(intent);
	}
*/
	
	static class DayPagerAdapter extends FragmentStatePagerAdapter {
		private static final String TAG = DayPagerAdapter.class.getSimpleName();
		
		private Calendar cal;
		static final int START_OFFSET_DAYS = 2000;
		
		public DayPagerAdapter(FragmentManager fm, Calendar start) {
			super(fm);
			if (BuildConfig.DEBUG) Log.d(TAG, "Constructor DayPagerAdapter()");
			cal = Calendar.getInstance();
			//noinspection MagicConstant
			cal.set(start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH)); // Defensive, don't need hours, minutes and so forth.
			cal.setLenient(false); //No, I don't want dates as 34th February.
		}

		@Override
		public int getCount() {
			return 4000;
		}
		
		@Override
		public Fragment getItem(int daysFromStart) {
			if (BuildConfig.DEBUG) Log.d(TAG, "getItem(): daysFromStart = " + daysFromStart);
			Calendar tmpCal = (Calendar)cal.clone();
			tmpCal.add(Calendar.DAY_OF_MONTH, daysFromStart - START_OFFSET_DAYS);
			DateParam dateParam = new DateParam(tmpCal.get(Calendar.YEAR), tmpCal.get(Calendar.MONTH), tmpCal.get(Calendar.DAY_OF_MONTH));
			return DayOverviewFragment.newInstance(dateParam);
		}		
	}
	
//	@Override
//	public void addProjectTime(DateParam dateParam) {
//		showNewProject(dateParam); // Perhaps save the dates first...
//	}
//	
//	@Override
//	public void addWageCodeTime(DateParam dateParam) {
//		showNewWageCode(dateParam);
//	}
//
//	@Override
//	public void addWorkOrderTime(DateParam dateParam) {
//		showNewWorkOrder(dateParam);
//	}
//
//	@Override
//	public void addRecentTime(DateParam dateParam) {
//		showSetRecentTime(dateParam);
//	}

//	@Override
//	public void setProjectTime(ProjectTimeParam[] projectTimeParam) {
//		Bundle b = new Bundle();
//		b.putParcelableArrayList(ProjectTimeParam.PROJECT_TIME_PARAM, new ArrayList<ProjectTimeParam>(Arrays.asList(projectTimeParam)));
//		showSetProjectTime(b);
//	}
//
//	@Override
//	public void setWageCodeTime(WageCodeTimeParam wageCodeTimeParam) {
//		Bundle b = new Bundle();
//		b.putParcelable(WageCodeTimeParam.WAGE_CODE_TIME_PARAM, wageCodeTimeParam);
//		showSetWageCodeTime(b);
//	}
//
//	@Override
//	public void setWorkOrderTime(WorkOrderTimeParam[] workOrderTimeParam) {
//		Bundle b = new Bundle();
//		b.putParcelableArrayList(WorkOrderTimeParam.WORK_ORDER_TIME_PARAM, new ArrayList<WorkOrderTimeParam>(Arrays.asList(workOrderTimeParam)));
//		showSetWorkOrderTime(b);
//	}
}