package com.metrix.metrixmobile;

import android.view.Menu;

import com.metrix.architecture.utilities.AndroidResourceHelper;

public class StockListMetrixActionView {

	public static final String ADDPART = AndroidResourceHelper.getMessage("AddPart");
	public static final String CREATESHIPMENT = AndroidResourceHelper.getMessage("AddShipment");
	public static final String REMOVEPART = AndroidResourceHelper.getMessage("RemovePart");
	public static final String SWAPPART = AndroidResourceHelper.getMessage("SwapPart");
	public static final String SEARCHPART = AndroidResourceHelper.getMessage("FindPart");
	public static final String ADDPURCHASE = AndroidResourceHelper.getMessage("AddPurchaseOrder");
	
	public static void onCreateMetrixActionView(Menu menu) {
		menu.clear();
		menu.add(0, Menu.NONE, 0, StockListMetrixActionView.ADDPART);
		menu.add(0, Menu.NONE, 0, StockListMetrixActionView.REMOVEPART);
		menu.add(0, Menu.NONE, 0, StockListMetrixActionView.SWAPPART);
		menu.add(0, Menu.NONE, 0, StockListMetrixActionView.SEARCHPART);
		menu.add(0, Menu.NONE, 0, StockListMetrixActionView.CREATESHIPMENT);
	}

}
