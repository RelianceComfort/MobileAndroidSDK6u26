package com.metrix.metrixmobile.system;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;

/**
 * Created by royaus on 2/25/2016.
 */
public class MessageDetails extends MetrixActivity
{
    Button mContinueButton;
    TextView mText;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_details);
        getWindow().setLayout(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onStart()
	 */
    public void onStart() {
        super.onStart();

        ViewGroup labelTipView = (ViewGroup) findViewById(R.id.table_layout);
        TextView tvLabel = (TextView) labelTipView.findViewWithTag("SCREEN_LABEL");
        TextView tvTip = (TextView) labelTipView.findViewWithTag("SCREEN_TIP");
        tvLabel.setText(AndroidResourceHelper.getMessage("MessageDetails"));
        tvTip.setText(AndroidResourceHelper.getMessage("ScreenDescriptionMessageDetails"));

        String message = getIntent().getStringExtra("SYNC_MESSAGE_DETAILS");
        if (!MetrixStringHelper.isNullOrEmpty(message)){
            mText = (TextView) findViewById(R.id.messageDetails);
            mText.setText(message);
        }
    }
}

