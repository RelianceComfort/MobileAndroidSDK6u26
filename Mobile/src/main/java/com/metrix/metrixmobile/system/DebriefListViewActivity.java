package com.metrix.metrixmobile.system;

import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.metrixmobile.JobList;
import com.metrix.metrixmobile.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.lang.ref.WeakReference;

public class DebriefListViewActivity extends DebriefActivity{

	protected Class<?> mParentActivity;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	/**
	 * @param listView
	 */
	public void registerContextMenu(ListView listView) {
		registerForContextMenu(listView);
		this.setupActionBar();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
	 * android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		DebriefListViewContextMenu.onCreateContextMenu(menu, v, menuInfo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		DebriefListViewContextMenu.onContextItemSelected(this, item);
		return true;
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.up:
			Intent intent = null;
			if (mParentActivity != null) {
				intent = MetrixActivityHelper.createActivityIntent(this, mParentActivity);
			} else {
				intent = MetrixActivityHelper.createActivityIntent(this, JobList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
			}
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		default:
			super.onClick(v);
		}
	}

}
