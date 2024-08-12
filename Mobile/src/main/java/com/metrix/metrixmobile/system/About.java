package com.metrix.metrixmobile.system;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixHyperlink;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.metrixmobile.R;

import java.util.ArrayList;
import java.util.List;

public class About extends Activity {
	private List<ResourceValueObject> resourceStrings;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		super.onStart();

		LogManager.getInstance(this).info("{0} onStart()", this.getLocalClassName());

		try {

			resourceStrings = new ArrayList<ResourceValueObject>();
			resourceStrings.add(new ResourceValueObject(R.id.copyright, "Copyright"));
			resourceStrings.add(new ResourceValueObject(R.id.support_url, "SupportUrl"));
			resourceStrings.add(new ResourceValueObject(R.id.support_email, "SupportEmail"));
			resourceStrings.add(new ResourceValueObject(R.id.support_phone, "SupportPhone"));

			try {
				AndroidResourceHelper.setResourceValues(this, resourceStrings);
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}

			MetrixControlAssistant.setValue(R.id.version_and_build_number, (ViewGroup) findViewById(R.id.table_layout),
					MetrixApplicationAssistant.getAppVersion(this));

			ImageView largeIconView = (ImageView) findViewById(R.id.up);
			if (largeIconView != null) {
				String largeIconImageID = MetrixSkinManager.getLargeIconImageID();
				if (!MetrixStringHelper.isNullOrEmpty(largeIconImageID)) {
					MetrixAttachmentHelper.applyImageWithDPScale(largeIconImageID, largeIconView, 80, 80);
				}
			}

			TextView tvDesignInfo = (TextView) findViewById(R.id.design_info);
			String designInfoString = getDesignInfo();
			if (MetrixStringHelper.isNullOrEmpty(designInfoString)) {
				tvDesignInfo.setVisibility(View.GONE);
			} else {
				tvDesignInfo.setText(designInfoString);
				tvDesignInfo.setVisibility(View.VISIBLE);
			}

			TextView tvServiceAddress = (TextView) findViewById(R.id.service_address);
			String serviceAddressString = SettingsHelper.getServiceAddress(this);
			if (MetrixStringHelper.isNullOrEmpty(serviceAddressString)) {
				tvServiceAddress.setVisibility(View.GONE);
			} else {
				tvServiceAddress.setText(serviceAddressString);
				tvServiceAddress.setVisibility(View.VISIBLE);
			}

			final MetrixHyperlink hlSupportEmail = (MetrixHyperlink) findViewById(R.id.support_email);
			final String supportEmail = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='SUPPORT_EMAIL'");
			if (!MetrixStringHelper.isNullOrEmpty(supportEmail))
				hlSupportEmail.setLinkText(supportEmail);
			hlSupportEmail.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
					emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { hlSupportEmail.getLinkText() });
					emailIntent.setType("plain/text");
					MetrixActivityHelper.startNewActivity(About.this, Intent.createChooser(emailIntent, AndroidResourceHelper.getMessage("SendEmail")));
				}
			});

			final MetrixHyperlink hlSupportPhone = (MetrixHyperlink) findViewById(R.id.support_phone);
			final String supportPhone = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='SUPPORT_PHONE'");
			if (!MetrixStringHelper.isNullOrEmpty(supportPhone))
				hlSupportPhone.setLinkText(supportPhone);
			hlSupportPhone.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
						String uri = "tel:" + hlSupportPhone.getLinkText();
						Intent intent = new Intent(Intent.ACTION_DIAL);
						intent.setData(Uri.parse(uri));
						startActivity(intent);
					}
					else
						MetrixUIHelper.showSnackbar(About.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("NoTelephonyServiceAvailable"));
				}
			});

			final MetrixHyperlink hlSupportUrl = (MetrixHyperlink) findViewById(R.id.support_url);
			final String supportUrl = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='SUPPORT_URL'");
			if (!MetrixStringHelper.isNullOrEmpty(supportUrl))
				hlSupportUrl.setLinkText(supportUrl);
			hlSupportUrl.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String validUrl = hlSupportUrl.getLinkText();
					if (!MetrixStringHelper.isNullOrEmpty(validUrl) && !validUrl.startsWith("https://") && !validUrl.startsWith("http://"))
						validUrl = "http://" + validUrl;
					Intent openUrlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(validUrl));
					startActivity(openUrlIntent);
				}
			});

			ViewGroup tableLayout = (ViewGroup) findViewById(R.id.table_layout);
			float scale = getResources().getDisplayMetrics().density;
			float btnCornerRadius = 4f * scale + 0.5f;
			MetrixActivity.setSkinBasedColorsOnRelevantControls(tableLayout, MetrixSkinManager.getPrimaryColor(), MetrixSkinManager.getSecondaryColor(),
					MetrixSkinManager.getHyperlinkColor(), tableLayout, false);
			MetrixActivity.setSkinBasedColorsOnButtons(((ViewGroup)findViewById(android.R.id.content)).getChildAt(0), MetrixSkinManager.getTopFirstGradientColor(), MetrixSkinManager.getBottomFirstGradientColor(),
					MetrixSkinManager.getFirstGradientTextColor(), btnCornerRadius, this);

		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private String getDesignInfo() {
		String returnString = "";

		// use_ tables should only have one record in each, so use blank where clause
		String designName = MetrixDatabaseManager.getFieldStringValue("use_mm_design", "name", "");
		String revNumber = MetrixDatabaseManager.getFieldStringValue("use_mm_revision", "revision_number", "");

		// only provide a non-empty return value if you find both chunks of data
		if (!MetrixStringHelper.isNullOrEmpty(designName) && !MetrixStringHelper.isNullOrEmpty(revNumber)) {
			// Should look like "{DESIGN_NAME} (Rev {REVISION_NUMBER})"
			returnString = String.format("%1$s (%2$s %3$s)", designName, AndroidResourceHelper.getMessage("Rev"), revNumber);
		}
		return returnString;
	}
}
