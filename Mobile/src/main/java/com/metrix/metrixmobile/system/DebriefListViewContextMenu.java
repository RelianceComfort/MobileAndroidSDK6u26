package com.metrix.metrixmobile.system;

import android.app.Activity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;

import com.metrix.architecture.utilities.AndroidResourceHelper;

public class DebriefListViewContextMenu {

	/**
	 * Populate the context menu.
	 * 
	 * @param menu
	 *            the menu to enhance.
	 * @param v
	 *            the view.
	 * @param menuInfo
	 *            the title of the menu option.
	 */
	public static void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.setHeaderTitle(AndroidResourceHelper.getMessage("SelectEllipsis"));
		menu.clear();
	}

	/**
	 * Handle the selection of a menu item.
	 * 
	 * @param item
	 *            the selected item.
	 * @return the class for the activity to start.
	 */
	public static void onContextItemSelected(Activity activity, MenuItem item) {
	}
}
