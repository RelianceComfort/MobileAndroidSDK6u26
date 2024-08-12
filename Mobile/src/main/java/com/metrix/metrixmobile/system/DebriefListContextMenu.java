package com.metrix.metrixmobile.system;

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;

import com.metrix.architecture.utilities.AndroidResourceHelper;

public class DebriefListContextMenu {

	private static final String ACTIONS = AndroidResourceHelper.getMessage("ActionsTitle");
	public static final String DELETE = AndroidResourceHelper.getMessage("Delete");
	public static final String EDIT = AndroidResourceHelper.getMessage("Edit");

	public static void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.setHeaderTitle(DebriefListContextMenu.ACTIONS);
		menu.clear();
		menu.add(0, v.getId(), 0, DebriefListContextMenu.DELETE);
	}

	public static void onContextItemSelected(MenuItem item) {
	}
}
