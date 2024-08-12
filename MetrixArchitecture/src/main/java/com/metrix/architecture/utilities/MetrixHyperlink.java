package com.metrix.architecture.utilities;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.metrix.architecture.designer.MetrixSkinManager;

import java.util.HashMap;

/**
 * Created by RaWiLK on 12/15/2016.
 */

public class MetrixHyperlink extends TextView {

    private String mStrDefaultColor = MetrixSkinManager.getHyperlinkColor();
    private int mDefaultColor = Color.parseColor(!MetrixStringHelper.isNullOrEmpty(mStrDefaultColor) ? mStrDefaultColor : MetrixSkinManager.DEFAULT_HYPERLINK_COLOR);

    private String mStrPressedColor = MetrixSkinManager.getSecondaryColor();
    private int mPressedColor = Color.parseColor(!MetrixStringHelper.isNullOrEmpty(mStrPressedColor) ? mStrPressedColor : MetrixSkinManager.DEFAULT_SECONDARY_COLOR);

    //region #Constructors

    public MetrixHyperlink(Context context) {
        super(context);

        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE)
                    setTextColor(mPressedColor);
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_OUTSIDE
                        || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    setTextColor(mDefaultColor);
                }
                return false;
            }
        });
    }

    public MetrixHyperlink(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCustomAttrValues(context, attrs);

        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE)
                    setTextColor(mPressedColor);
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_OUTSIDE
                        || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    setTextColor(mDefaultColor);
                }
                return false;
            }
        });
    }

    public MetrixHyperlink(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setCustomAttrValues(context, attrs);

        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE)
                    setTextColor(mPressedColor);
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_OUTSIDE
                        || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    setTextColor(mDefaultColor);
                }
                return false;
            }
        });
    }

    //endregion

    //region #Getters, Setters

    public int getDefaultColor() {
        return mDefaultColor;
    }

    public void setDefaultColor(int defaultColor) {
        this.mDefaultColor = defaultColor;
    }

    public int getPressedColor() {
        return mPressedColor;
    }

    public void setPressedColor(int pressedColor) {
        this.mPressedColor = pressedColor;
    }

    //endregion

    //region #Methods

    public void setLinkText(String value) {
        if (value == null) value = "";

        SpannableString content = new SpannableString(value);
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        setText(content);

        setTextColor(mDefaultColor);
    }

    public void setLinkText(String value, int start, int end) throws Exception {
        if (value == null) value = "";

        if (end > value.length())
            throw new Exception(AndroidResourceHelper.getMessage("EndValIsGreaterThan"));

        SpannableString content = new SpannableString(value);
        content.setSpan(new UnderlineSpan(), start, end, 0);
        setText(content);

        setTextColor(mDefaultColor);
    }

    public String getLinkText() {
        return this.getText().toString();
    }

    private void setCustomAttrValues(Context context, AttributeSet attrs) {
        HashMap<String, Object> attrbs = (HashMap<String, Object>) MetrixPublicCache.instance.getItem("MetrixHyperlinkAttributeData");
        if (attrbs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, (int[]) attrbs.get("R.styleable.MetrixHyperlinkAttr"));
            if (typedArray != null) {

                //region #setLinkText
                String s = typedArray.getString((Integer) attrbs.get("R.styleable.MetrixHyperlinkAttr_linkText"));
                if (!MetrixStringHelper.isNullOrEmpty(s))
                    setLinkText(s);
                //endregion

            }
        }
    }

    //endregion
}
