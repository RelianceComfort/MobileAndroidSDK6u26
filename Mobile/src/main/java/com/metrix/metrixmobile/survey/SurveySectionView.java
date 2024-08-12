package com.metrix.metrixmobile.survey;

import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

public class SurveySectionView extends LinearLayout  {

	private TextView mHeader;
	private TextView mDetail;
	private ImageView mAllowSkipAll;
	
	public SurveySectionView(Context context) {
		super(context);
		
		@SuppressWarnings("deprecation")
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		lp.setMargins(0, 0, 0, 10);
		setLayoutParams(lp);
		setFocusableInTouchMode(false);
		setFocusable(false);
		setOrientation(LinearLayout.VERTICAL);
		setPadding(6, 1, 6, 1);
		
		LayoutInflater li = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        li.inflate(R.layout.survey_section, this, true);
               
        mHeader = (TextView)findViewById(R.id.survey_section_header);
        mDetail = (TextView)findViewById(R.id.survey_section_detail);
        mAllowSkipAll = (ImageView)findViewById(R.id.skip_all);
        
        String primaryColorString = MetrixSkinManager.getPrimaryColor();
        if (!MetrixStringHelper.isNullOrEmpty(primaryColorString)) {
        	mHeader.setTextColor(Color.parseColor(primaryColorString));
        }
	}
	
	public ImageView getSkipAllButton() {
		return mAllowSkipAll;
	}
	
	public void hideSkipAllButton() {
		mAllowSkipAll.setVisibility(GONE);
	}
	
	public void showSkipAllButton() {
		mAllowSkipAll.setVisibility(VISIBLE);
	}
	
	public void setSection(SurveySection section) {
		
		String header = section.getHeader();
		boolean allSkipAll = section.getAllowSkipAll();
		if(header != null) {
			mHeader.setText(header);
			mHeader.setVisibility(VISIBLE);
		} else {
			mHeader.setVisibility(GONE);
		}
		
		String detail = section.getDetail();
		if(detail != null) {
			mDetail.setText(detail);
			mDetail.setVisibility(VISIBLE);
		} else {
			mDetail.setVisibility(GONE);
		}
		
		if (allSkipAll)
		{
			mAllowSkipAll.setVisibility(VISIBLE);
		}
		else
		{
			mAllowSkipAll.setVisibility(GONE);
		}
	}
	
}
