package com.metrix.architecture.attachment;

import android.content.Context;
import android.util.AttributeSet;

import androidx.viewpager.widget.ViewPager;

import java.util.HashMap;
import java.util.List;

public class FSMCarouselView extends ViewPager {
    private FSMCarouselAdapter carouselAdapter;
    private FSMAttachmentFullScreen parentActivity;

    public FSMCarouselView(Context context) {
        super(context);
        setUpPageChangeListener();
    }

    public FSMCarouselView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUpPageChangeListener();
    }

    public void setParentActivity(FSMAttachmentFullScreen parentActivity) {
        this.parentActivity = parentActivity;
    }

    public void setDataSource(Context context, List<HashMap<String, String>> dataList) {
        carouselAdapter = new FSMCarouselAdapter(context, dataList, this.parentActivity);
        this.setAdapter(carouselAdapter);
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, String> getItem(int position) {
        return (HashMap<String, String>) carouselAdapter.getItem(position);
    }

    private void setUpPageChangeListener() {
        this.clearOnPageChangeListeners();
        this.addOnPageChangeListener(new OnPageChangeListener(){
            @Override
            public void onPageScrollStateChanged(int arg0) {}
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {}

            @Override
            public void onPageSelected(int selectedPosition) {
                if (parentActivity != null) {
                    parentActivity.mSelectedPosition = selectedPosition;
                    parentActivity.updateEditButtonAppearance();
                }
            }
        });
    }
}
