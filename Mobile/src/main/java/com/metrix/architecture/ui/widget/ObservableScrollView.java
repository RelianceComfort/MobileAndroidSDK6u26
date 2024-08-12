package com.metrix.architecture.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class ObservableScrollView extends ScrollView {

	private boolean mTouchDown = false;
	
	public interface ScrollViewListener {
        void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy);
    }
	
    private ScrollViewListener scrollViewListener = null;

    public ObservableScrollView(Context context) {
        super(context);
    }

    public ObservableScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ObservableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setScrollViewListener(ScrollViewListener scrollViewListener) {
        this.scrollViewListener = scrollViewListener;
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);
        if(scrollViewListener != null && mTouchDown) {
            scrollViewListener.onScrollChanged(this, x, y, oldx, oldy);
        }
    }
        
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

    	if (ev.getAction() == MotionEvent.ACTION_DOWN) {
    		mTouchDown = true;
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
        	mTouchDown = false;
		}

    	return super.onInterceptTouchEvent(ev);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
    	
    	if (ev.getAction() == MotionEvent.ACTION_DOWN) {
    		mTouchDown = true;
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
			mTouchDown = false;
		}
    	
    	return super.onTouchEvent(ev);
    }
}