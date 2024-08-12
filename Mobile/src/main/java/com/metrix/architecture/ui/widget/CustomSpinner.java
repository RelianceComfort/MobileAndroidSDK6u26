package com.metrix.architecture.ui.widget;

import android.content.Context;
import androidx.appcompat.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;

public class CustomSpinner extends AppCompatSpinner {

	public CustomSpinner(Context context) {
		super(context);
	}

	public CustomSpinner(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}

	public CustomSpinner(Context context, AttributeSet attributeSet,
			int defaultStyle) {
		super(context, attributeSet, defaultStyle);
	}

	@Override
	public void setSelection(int position, boolean animate) {
		boolean isSameItemSelected = position == getSelectedItemPosition();
		super.setSelection(position, animate);
		if (isSameItemSelected) {
			ExplicitCallingOfOnItemSelectListner(this, getSelectedView(),
					position, getSelectedItemId());
		}
	}

	@Override
	public void setSelection(int position) {
		boolean isSameItemSelected = position == getSelectedItemPosition();
		super.setSelection(position);
		if (isSameItemSelected) {
			ExplicitCallingOfOnItemSelectListner(this, getSelectedView(),
					position, getSelectedItemId());
		}
	}

	private void ExplicitCallingOfOnItemSelectListner(
			AdapterView<?> adapterView, View selectedView, int position,
			long selectedItemId) {
		getOnItemSelectedListener().onItemSelected(adapterView, selectedView,
				position, selectedItemId);
	}

}
