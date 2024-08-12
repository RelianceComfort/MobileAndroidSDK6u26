package com.metrix.metrixmobile.system;

import android.view.Menu;

import com.metrix.architecture.utilities.AndroidResourceHelper;

public class SyncMonitorMetrixActionView {

	public static final String DELETE = AndroidResourceHelper.getMessage("Delete");
	public static final String RESEND = AndroidResourceHelper.getMessage("Resend");
	public static final String CORRECT = AndroidResourceHelper.getMessage("Correct");
	
	public static void onCreateMetrixActionView(Menu menu) {
		menu.clear();
		menu.add(0, Menu.NONE, 0, SyncMonitorMetrixActionView.CORRECT);
		menu.add(0, Menu.NONE, 0, SyncMonitorMetrixActionView.RESEND);
		menu.add(0, Menu.NONE, 0, SyncMonitorMetrixActionView.DELETE);
	}

	public static void onCreateMetrixActionViewDelete(Menu menu) {
		menu.clear();
		menu.add(0, Menu.NONE, 0, SyncMonitorMetrixActionView.DELETE);
	}
}
