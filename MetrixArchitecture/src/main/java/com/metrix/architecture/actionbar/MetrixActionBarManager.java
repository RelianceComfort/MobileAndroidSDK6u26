package com.metrix.architecture.actionbar;

import java.lang.reflect.Field;

import android.content.Context;
import android.graphics.Color;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;

import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

/**
 * Added for supporting MetrixActionBar related functions
 * 05/09/2014
 * @author rawilk
 *
 */
public class MetrixActionBarManager {

	public static final int ActionHelpId = 456;

	private static MetrixActionBarManager mInstance = null;
	public static synchronized MetrixActionBarManager getInstance(){
		if(mInstance == null){
			mInstance = new MetrixActionBarManager();
		}
		return mInstance;
	}

	private MetrixActionBarManager(){
	}

	/***
	 * Setup ActionBar for default screens, designer screens.
	 * Specify if you want to set the skin color for ActionBar.
	 * @param actionBarActivity
	 * @param layoutId
	 * @param setColor
	 * @return ActionBar
	 */
	public ActionBar setupActionBar(AppCompatActivity actionBarActivity, int layoutId, boolean setColor){

		ActionBar supportActionBar = actionBarActivity.getSupportActionBar();

		if(supportActionBar != null){

			LayoutInflater inflater = (LayoutInflater) supportActionBar.getThemedContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View customActionBarView = inflater.inflate(layoutId, null);

			supportActionBar.setDisplayShowTitleEnabled(false);
			supportActionBar.setDisplayShowCustomEnabled(true);
			supportActionBar.setDisplayUseLogoEnabled(false);
			supportActionBar.setDisplayShowHomeEnabled(false);

			supportActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
			supportActionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
					ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.MATCH_PARENT, Gravity.LEFT));

			if(setColor){
				MetrixSkinManager.setFirstGradientBackground(supportActionBar, 0);
				MetrixSkinManager.setFirstGradientBackground(customActionBarView, 0);
			}
		}

		return supportActionBar;
	}

	/***
	 * Setup ActionBar title with desired color,etc
	 * @param actionBarActivity
	 * @param titleViewId
	 * @param title
	 * @param firstGradientColor
	 * @return ActionBar
	 */
	public TextView setupActionBarTitle(AppCompatActivity actionBarActivity, int titleViewId, String title, String firstGradientColor){

		TextView actionBarTitle = (TextView) actionBarActivity.findViewById(titleViewId);
		if (actionBarTitle != null) {
			actionBarTitle.setText(title);
			if (!MetrixStringHelper.isNullOrEmpty(firstGradientColor))
				actionBarTitle.setTextColor(Color.parseColor(firstGradientColor));
		}
		return actionBarTitle;
	}

	/***
	 * Disable Android device physical "MENU" button if exists, otherwise ActionBar -> Overflow icon won't appear
	 * @link("http://developer.android.com/design/patterns/actionbar.html")
	 * @param actionBarActivity
	 */
	public void disableMenuButton(AppCompatActivity actionBarActivity){

		try {
			ViewConfiguration config = ViewConfiguration.get(actionBarActivity);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if(menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception e) {
			LogManager.getInstance(actionBarActivity).error(e);
		}
	}

	public boolean setActionBarDefaultIcon(int imageID, ActionBar actionBar, int requiredHeightDP, int requiredWidthDP) {
		try {
			MetrixAttachmentHelper.applyActionBarDefaultIconWithDPScale(imageID, actionBar, requiredHeightDP, requiredWidthDP);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return false;
	}

	public boolean setActionBarCustomizedIcon(String imageID, ActionBar actionBar, int requiredHeightDP, int requiredWidthDP) {
		try {
			MetrixAttachmentHelper.applyActionBarIconWithDPScale(imageID, actionBar, requiredHeightDP, requiredWidthDP);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return false;
	}
}
