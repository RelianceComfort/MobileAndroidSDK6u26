package com.metrix.architecture.utilities;

import android.content.Context;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;

/***
 * Added as a replacement for ContextMenu
 * @author rawilk
 *
 */
public class MetrixActionView extends PopupMenu {

	private OnMetrixActionViewItemClickListner onMetrixActionViewItemClickListner;

	public MetrixActionView(Context context, View anchor) {
		super(context, anchor);

		this.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				if(onMetrixActionViewItemClickListner != null)
					onMetrixActionViewItemClickListner.onMetrixActionViewItemClick(menuItem);
				return true;
			}
		});
	}

	public void setOnMetrixActionViewItemClickListner(OnMetrixActionViewItemClickListner onMetrixActionViewItemClickListner){
		this.onMetrixActionViewItemClickListner = onMetrixActionViewItemClickListner;
	}


//	@Override
//	public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing){
//		if(menu != null)
//			menu.clear();
//		super.onCloseMenu(menu, allMenusAreClosing);
//	}
}
