package com.metrix.metrixmobile.survey;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

/**
 * Created by royaus on 7/11/2016.
 */
public class SurveyQuestionView extends LinearLayout
{
    public SurveyQuestionView(Context context) {
        super(context);
    }

    public SurveyQuestionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void setContentView(int layoutResID) {
        LayoutInflater li = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        li.inflate(layoutResID, this, true);
    }
}
