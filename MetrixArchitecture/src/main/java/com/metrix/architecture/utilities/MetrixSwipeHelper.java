package com.metrix.architecture.utilities;

import com.metrix.architecture.superclasses.MetrixBaseActivity;

import android.app.Activity;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * Measures a user swipe and if it is a horizontal swipe that 
 * exceeds 100 dp from right to left in width we'll advance 
 * to the next screen. If it exceeds 100 dp from left to right
 * we take the user back to the previous screen by finishing 
 * the current activity. 
 * 
 * @deprecated in 5.6.0
 */
@Deprecated
public class MetrixSwipeHelper implements OnTouchListener {

	static final int MINIMUM_DISTANCE = 100;

	private Activity mActivity;
	private float mDownXPosition, mUpXPosition;
	
	public MetrixSwipeHelper(Activity activity) {
		this.mActivity = activity;
	}

	public void onRightToLeftSwipe() {
		MetrixBaseActivity metrixBaseActivity = (MetrixBaseActivity) mActivity;
		if (metrixBaseActivity.nextActivity != null) {
			Intent intent = MetrixActivityHelper.createActivityIntent(mActivity, metrixBaseActivity.nextActivity);
			intent.putExtra("task_id", mActivity.getIntent().getStringExtra("task_id"));
			MetrixActivityHelper.startNewActivity(mActivity, intent);
		}
	}

	public void onLeftToRightSwipe() {
		mActivity.finish();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnTouchListener#onTouch(android.view.View,
	 * android.view.MotionEvent)
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			mDownXPosition = event.getX();
			return true;
		}
		case MotionEvent.ACTION_UP: {
			mUpXPosition = event.getX();

			float deltaX = mDownXPosition - mUpXPosition;

			if (Math.abs(deltaX) > MINIMUM_DISTANCE) {
				if (deltaX < 0) {
					this.onLeftToRightSwipe();
					return true;
				}
				if (deltaX > 0) {
					this.onRightToLeftSwipe();
					return true;
				}
			}
		}
		}

		return false;
	}
}
