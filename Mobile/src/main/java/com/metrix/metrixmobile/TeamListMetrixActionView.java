package com.metrix.metrixmobile;

import android.view.Menu;

import com.metrix.architecture.utilities.AndroidResourceHelper;

public class TeamListMetrixActionView {
	
	public static final String TEAMMAP = AndroidResourceHelper.getMessage("ShowMapNoEllipsis");

	public static final int TEAM = 0;

	public static void onCreateMetrixActionView(Menu menu) {
		menu.clear();
		menu.add(0, TeamListMetrixActionView.TEAM, 0, TeamListMetrixActionView.TEAMMAP);		
	}
}
