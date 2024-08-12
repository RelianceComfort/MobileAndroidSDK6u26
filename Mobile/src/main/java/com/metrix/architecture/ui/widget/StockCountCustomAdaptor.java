package com.metrix.architecture.ui.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.metrix.architecture.designer.MetrixSkinManager;

import java.util.List;
import java.util.Map;

/**
 * Created by hesplk on 4/25/2016.
 */
public class StockCountCustomAdaptor extends CustomSimpleAdapter {
    int[] hiddenList;
    public StockCountCustomAdaptor(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to, int[] hiddenList) {
        super(context, data, resource, from, to);
        this.context = context;
        this.list = data;
        this.resource = resource;
        this.from = from;
        this.to = to;
        this.skinBasedSecondaryColor = MetrixSkinManager.getSecondaryColor();
        this.skinBasedHyperlinkColor = MetrixSkinManager.getHyperlinkColor();
        this.hiddenList = hiddenList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        //Hiding unnecessary fields as required
        if (view != null && view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View v = vg.getChildAt(i);
                if (v != null && v instanceof TextView) {
                    TextView tv = (TextView) v;
                    if(existInHiddenList(tv.getId())){
                        tv.setVisibility(View.GONE);
                    }
                }
                if (v != null && v instanceof CheckBox) {
                    CheckBox cb = (CheckBox) v;
                    cb.setTag(position);
                }
            }
        }

        super.getView(position,convertView,parent);
        return view;
    }

    private boolean existInHiddenList(int viewId)
    {
        for(int count=0;count<=hiddenList.length-1;count++){
            if(hiddenList[count] == viewId)
            {
                return true;
            }
        }
        return false;
    }
}
