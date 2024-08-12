package com.metrix.architecture.slidingmenu;

import android.view.View;
import android.widget.AdapterView;

/****
 * MetrixSlidingMenuItemClick implementation
 * @author rawilk
 *
 */
public interface OnMetrixSlidingMenuItemClickListner{
	public void OnMetrixSlidingMenuItemClick(AdapterView<?> parent, View view, int position, long id);
}
