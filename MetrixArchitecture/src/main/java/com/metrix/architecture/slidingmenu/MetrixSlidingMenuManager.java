package com.metrix.architecture.slidingmenu;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import com.metrix.architecture.utilities.MetrixStringHelper;

/****
 * Added for supporting MetrixSlidingMenu functions
 * @author rawilk
 *
 */
public class MetrixSlidingMenuManager {

	private static MetrixSlidingMenuManager mInstance = null;
	public static synchronized MetrixSlidingMenuManager getInstance(){
		if(mInstance == null){
			mInstance = new MetrixSlidingMenuManager();
		}
		return mInstance;
	}

	private MetrixSlidingMenuManager(){
	}

	/****
	 * Setup sliding drawer for default screens, designer screens
	 * @param actionBarActivity
	 * @param supportActionBar
	 * @param drawerLinearLayout
	 * @param drawerLayout
	 * @param drawerIconId
	 * @param drawerOpenStringId
	 * @param drawerCloseStringId
	 * @return ActionBarDrawerToggle
	 */

	// EDL Oidc Redo
	public ActionBarDrawerToggle setUpSlidingDrawer(final AppCompatActivity actionBarActivity, ActionBar supportActionBar, LinearLayout drawerLinearLayout, DrawerLayout drawerLayout, int drawerIconId, int drawerOpenStringId, int drawerCloseStringId, String drawerIconColor, int overflowIconId){

		ActionBarDrawerToggle drawerToggle = null;

		if(drawerLayout != null){
			drawerToggle = new ActionBarDrawerToggle(actionBarActivity, drawerLayout,
					drawerOpenStringId, drawerCloseStringId)
					//drawerToggle = new ActionBarDrawerToggle(actionBarActivity, drawerLayout,
					//		drawerIconId, drawerOpenStringId, drawerCloseStringId)
			{

				public void onDrawerClosed(View view) {
					super.onDrawerClosed(view);
					actionBarActivity.supportInvalidateOptionsMenu();
				}

				public void onDrawerOpened(View drawerView) {
					super.onDrawerOpened(drawerView);
					actionBarActivity.supportInvalidateOptionsMenu();
				}
			};

			// Set the drawer toggle as the DrawerListener
			drawerLayout.addDrawerListener(drawerToggle);

			if(supportActionBar != null){
				supportActionBar.setDisplayHomeAsUpEnabled(true);
				supportActionBar.setHomeButtonEnabled(true);
			}

			Drawable navigationDrawer = actionBarActivity.getResources().getDrawable(drawerIconId);
			Drawable overflowIcon = actionBarActivity.getResources().getDrawable(overflowIconId);

			if (android.os.Build.VERSION.SDK_INT <= 19) {	// KitKat or earlier
				if (!MetrixStringHelper.isNullOrEmpty(drawerIconColor)){
					navigationDrawer.setColorFilter(Color.parseColor(drawerIconColor), Mode.SRC_ATOP);
					overflowIcon.setColorFilter(Color.parseColor(drawerIconColor), Mode.SRC_ATOP);
				}
				else{
					navigationDrawer.setColorFilter(Color.parseColor("#FEFDFE"), Mode.SRC_ATOP);
					overflowIcon.setColorFilter(Color.parseColor("#FEFDFE"), Mode.SRC_ATOP);
				}
			} else {
				if (!MetrixStringHelper.isNullOrEmpty(drawerIconColor)){
					navigationDrawer.setColorFilter(Color.parseColor(drawerIconColor), Mode.SRC_ATOP);
					setOverflowButtonColor(actionBarActivity, drawerIconColor);
				}
				else{
					navigationDrawer.setColorFilter(Color.parseColor("#FEFDFE"), Mode.SRC_ATOP);
					setOverflowButtonColor(actionBarActivity, "#FEFDFE");
				}
			}
          
          	drawerToggle.getDrawerArrowDrawable().setColor(Color.parseColor(!MetrixStringHelper.isNullOrEmpty(drawerIconColor) ? drawerIconColor : "#FEFDFE"));
		}
		return drawerToggle;
	}

	public static void setOverflowButtonColor(final Activity activity, final String drawerIconColor) {
		final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
		final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
		viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				final ArrayList<View> outViews = new ArrayList<View>();
				findViewsWithText(outViews, decorView, "IFSFSMAOverflowIcon");
				if (outViews.isEmpty()) {
					return;
				}

				AppCompatImageView overflow = (AppCompatImageView)outViews.get(0);
				overflow.setColorFilter(Color.parseColor(drawerIconColor));
				overflow.setBackground(null);
				removeOnGlobalLayoutListener(decorView, this);
			}
		});
	}

	public static void removeOnGlobalLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener listener) {
		v.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
	}

	public static void findViewsWithText(List<View> outViews, ViewGroup parent, String targetDescription) {
		if (parent == null || TextUtils.isEmpty(targetDescription)) {
			return;
		}
		final int count = parent.getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = parent.getChildAt(i);
			final CharSequence desc = child.getContentDescription();
			if (!TextUtils.isEmpty(desc) && targetDescription.equals(desc.toString())) {
				outViews.add(child);
			} else if (child instanceof ViewGroup && child.getVisibility() == View.VISIBLE) {
				findViewsWithText(outViews, (ViewGroup) child, targetDescription);
			}
		}
	}
}
