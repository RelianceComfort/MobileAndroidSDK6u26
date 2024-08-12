package com.metrix.architecture.slidingmenu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

/****
 * Added for implementing MetrixSlisingMenu
 * @link("https://developer.android.com/design/patterns/navigation-drawer.html")
 * @author rawilk
 *
 */
public class MetrixSlidingMenu extends ListView{

	private OnMetrixSlidingMenuItemClickListner mOnMetrixSlidingMenuItemClickListner;
	
	public MetrixSlidingMenu(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		this.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(mOnMetrixSlidingMenuItemClickListner != null){
					mOnMetrixSlidingMenuItemClickListner.OnMetrixSlidingMenuItemClick(parent, view, position, id);
				}
				
			}
		});
	}
	
	public void setOnMetrixSlidingMenuItemClickListner(OnMetrixSlidingMenuItemClickListner onMetrixSlidingMenuItemClickListner){
		this.mOnMetrixSlidingMenuItemClickListner = onMetrixSlidingMenuItemClickListner;
	}
}
