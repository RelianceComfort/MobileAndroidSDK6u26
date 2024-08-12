package com.metrix.architecture.ui.widget;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;

import com.metrix.architecture.ui.widget.MetrixCarouselAdapter.OnMetrixCarouselAdapterItemLongClickListner;

public class MetrixCarouselView extends ViewPager{

	private MetrixCarouselAdapter metrixCarouselPagerAdapter;
	private OnMetrixCarouselViewItemLongClickListner mMetrixCarouselViewItemLongClickListner;
	private OnMetrixCarouselViewItemClickListner mMetrixCarouselViewItemClickListner;
	
	public MetrixCarouselView(Context context) {
		super(context);
	}
	
	public MetrixCarouselView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		this.setOnPageChangeListener(new OnPageChangeListener(){

			@Override
			public void onPageScrollStateChanged(int arg0) {
				
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {

			}

			@Override
			public void onPageSelected(int selectedPosition) {	
				
			}
			
		});
		
	}

	public void setDatasource(Context context, List<HashMap<String, String>> dataList) {

		metrixCarouselPagerAdapter = new MetrixCarouselAdapter(context, dataList);
		this.setAdapter(metrixCarouselPagerAdapter);
		
		//if(metrixCarouselPagerAdapter == null) return;
		metrixCarouselPagerAdapter.setMetrixCarouselAdapterItemLongClickListner(new OnMetrixCarouselAdapterItemLongClickListner() {
			
			@Override
			public void onMetrixCarouselAdapterItemLongClick(int position) {
				if(mMetrixCarouselViewItemLongClickListner != null){
					mMetrixCarouselViewItemLongClickListner.onMetrixCarouselViewItemLongClick(position);
				}
				
			}
		});
		metrixCarouselPagerAdapter.setMetrixCarouselViewItemClickListner(new OnMetrixCarouselViewItemClickListner() {
			@Override
			public void onMetrixCarouselViewItemClick(int position) {
				if(mMetrixCarouselViewItemClickListner != null){
					mMetrixCarouselViewItemClickListner.onMetrixCarouselViewItemClick(position);
				}
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<String, String> getItem(int position) {
		return (HashMap<String, String>) metrixCarouselPagerAdapter.getItem(position);
	}
	
	public void removeItem(HashMap<String, String> deletedItem) {
		metrixCarouselPagerAdapter.remove(deletedItem);
		metrixCarouselPagerAdapter.notifyDataSetChanged();
	}
	
	public interface OnMetrixCarouselViewItemLongClickListner {
		public void onMetrixCarouselViewItemLongClick(int position);
	}

	public interface OnMetrixCarouselViewItemClickListner {
		public void onMetrixCarouselViewItemClick(int position);
	}

	public void setMetrixCarouselViewItemClickListner(OnMetrixCarouselViewItemClickListner viewItemLongClickListne) {
		mMetrixCarouselViewItemClickListner = viewItemLongClickListne;
	}

    public void setMetrixCarouselViewItemLongClickListner(OnMetrixCarouselViewItemLongClickListner viewItemClickListner) {
    	mMetrixCarouselViewItemLongClickListner = viewItemClickListner;
	}

}
