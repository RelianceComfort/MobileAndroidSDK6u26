package com.metrix.metrixmobile.global;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;

import android.app.Activity;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MetrixImportantInformation {	
	public static void reset(ViewGroup layout, Activity activity) {
		LinearLayout fullLayout = (LinearLayout) MetrixControlAssistant.getControl(R.id.important_information_bar, layout);
		if (fullLayout != null) {
			float scale = activity.getResources().getDisplayMetrics().density;
			float radius = 7f * scale + 0.5f;
			MetrixSkinManager.setSecondGradientBackground(fullLayout, radius, 1);
			fullLayout.setVisibility(View.GONE);
			
			String secondGradientTextColor = MetrixSkinManager.getSecondGradientTextColor();
			if (!MetrixStringHelper.isNullOrEmpty(secondGradientTextColor)) {
				TextView iiHeading = (TextView) MetrixControlAssistant.getControl(R.id.important_information_heading, layout);
				iiHeading.setTextColor(Color.parseColor(secondGradientTextColor));
			}
			
			MetrixImportantInformation.resetView(R.id.important_information, layout);
			MetrixImportantInformation.resetView(R.id.important_information2, layout);
			MetrixImportantInformation.resetView(R.id.important_information3, layout);
			MetrixImportantInformation.resetView(R.id.important_information4, layout);
			MetrixImportantInformation.resetView(R.id.important_information5, layout);
			MetrixImportantInformation.resetView(R.id.important_information6, layout);
			MetrixImportantInformation.resetView(R.id.important_information7, layout);
			MetrixImportantInformation.resetView(R.id.important_information8, layout);
		}
	}
	
	private static void resetView(int viewId, ViewGroup layout) {
		try {
			View view = MetrixControlAssistant.getControl(viewId, layout);
			MetrixControlAssistant.setValue(view, "");
			view.setVisibility(View.GONE);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}
	
	public static void add(ViewGroup layout, String value, String hyperlinkValue, OnClickListener onClickListener) {
		if ((layout == null) || (MetrixStringHelper.isNullOrEmpty(value))) {
			return;
		}
	
		if (MetrixControlAssistant.getControl(R.id.important_information_bar, layout) == null) {
			return;
		}

		value = "\u2022 "+value; // value = "• " + value;
		SpannableString spannableString = new SpannableString(value);
		if (!MetrixStringHelper.isNullOrEmpty(hyperlinkValue) && value.contains(hyperlinkValue)) {
			spannableString.setSpan(new UnderlineSpan(), value.indexOf(hyperlinkValue), value.indexOf(hyperlinkValue) + hyperlinkValue.length(), 0);
		}
		
		try
		{
			int viewId = MetrixImportantInformation.nextAvailableView(layout);	
			TextView view = (TextView)MetrixControlAssistant.getControl(viewId, layout);
			view.setText(spannableString);
			
			String hyperlinkAppearanceColor = "#000000";
			String secondGradientTextColor = MetrixSkinManager.getSecondGradientTextColor();
			if (!MetrixStringHelper.isNullOrEmpty(secondGradientTextColor)) {
				view.setTextColor(Color.parseColor(secondGradientTextColor));
				hyperlinkAppearanceColor = secondGradientTextColor;
			}
			
			view.setVisibility(View.VISIBLE);
			if (onClickListener != null) {
				view.setOnClickListener(onClickListener);
				if(!MetrixStringHelper.isNullOrEmpty(hyperlinkValue))		// warranty won't act as a hyperlink
					MetrixControlAssistant.giveTextViewHyperlinkApperance(viewId, layout, hyperlinkAppearanceColor, R.color.transparent, false);
			}
			MetrixControlAssistant.getControl(R.id.important_information_bar, layout).setVisibility(View.VISIBLE);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}
	
	public static void add(ViewGroup layout, String value) {
		if ((layout == null) || (MetrixStringHelper.isNullOrEmpty(value))) {
			return;
		}
	
		if (MetrixControlAssistant.getControl(R.id.important_information_bar, layout) == null) {
			return;
		}

		value = "\u2022 "+value; // value = "• " + value;
		int viewId = MetrixImportantInformation.nextAvailableView(layout);
		
		try {
			TextView tv = (TextView) MetrixControlAssistant.getControl(viewId, layout);
			MetrixControlAssistant.setValue(tv, value);
			String secondGradientTextColor = MetrixSkinManager.getSecondGradientTextColor();
			if (!MetrixStringHelper.isNullOrEmpty(secondGradientTextColor)) {
				tv.setTextColor(Color.parseColor(secondGradientTextColor));
			}			
			tv.setVisibility(View.VISIBLE);
			MetrixControlAssistant.getControl(R.id.important_information_bar, layout).setVisibility(View.VISIBLE);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}
	
	private static int nextAvailableView(ViewGroup layout) {
		if (MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue(R.id.important_information, layout))) {
			return R.id.important_information;
		}
		if (MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue(R.id.important_information2, layout))) {
			return R.id.important_information2;
		}
		if (MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue(R.id.important_information3, layout))) {
			return R.id.important_information3;
		}
		if (MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue(R.id.important_information4, layout))) {
			return R.id.important_information4;
		}
		if (MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue(R.id.important_information5, layout))) {
			return R.id.important_information5;
		}
		if (MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue(R.id.important_information6, layout))) {
			return R.id.important_information6;
		}
		if (MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue(R.id.important_information7, layout))) {
			return R.id.important_information7;
		}
		if (MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue(R.id.important_information8, layout))) {
			return R.id.important_information8;
		}
		return -1;
	}
}
