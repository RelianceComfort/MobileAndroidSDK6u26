package com.metrix.metrixmobile;

import android.view.Menu;

import com.metrix.architecture.utilities.AndroidResourceHelper;

public class PurchaseOrderListMetrixActionView {
	public static final String ADDPURCHASE = AndroidResourceHelper.getMessage("AddPurchaseOrder");
	
	public static void onCreateMetrixActionView(Menu menu) {
		menu.clear();
		menu.add(0, Menu.NONE, 0, PurchaseOrderListMetrixActionView.ADDPURCHASE);
	}

}
