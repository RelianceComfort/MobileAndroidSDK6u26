package com.metrix.metrixmobile.system;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.metrix.architecture.actionbar.MetrixActionBarManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.MetrixAuthenticationAssistant;

import java.util.Hashtable;

public class ActivateDeviceEntry extends AppCompatActivity implements View.OnClickListener {
    Button activateButton;
    private ViewGroup mLayout;
    private Context mCurrentContext;
    private Boolean welcomeDisplayed = null;
    private ProgressDialog progressDialog;
    private EditText mServiceUrl;
    private TextView mEndpointUrl;
    private Drawable originalBackground;
    protected ActionBar mSupportActionBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.active_device_entry);

        mCurrentContext = this;
        mLayout = (ViewGroup) findViewById(R.id.table_layout);
        activateButton = (Button) findViewById(R.id.validateUrl) ;
        mEndpointUrl = findViewById(R.id.endpointLabel);
        activateButton.setOnClickListener(this);
        mServiceUrl = (EditText) findViewById(R.id.serviceUrl);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onStart()
     */
    public void onStart() {
        AndroidResourceHelper.setResourceValues(mServiceUrl, "ServiceUrl", true);
        AndroidResourceHelper.setResourceValues(activateButton, "Connect");
        AndroidResourceHelper.setResourceValues(mEndpointUrl, "FSMEndPointURL");

        super.onStart();

        mSupportActionBar = MetrixActionBarManager.getInstance().setupActionBar(this, R.layout.action_bar, false);
        String firstGradientText = "";
        String actionBarTitle = "Field Service Management";
        MetrixActionBarManager.getInstance().setupActionBarTitle(this, R.id.action_bar_title, actionBarTitle, firstGradientText);
//        MetrixActionBarManager.getInstance().setActionBarDefaultIcon(R.drawable.ifs_logo, getMetrixActionBar(), 24, 24);

        // Toast.makeText(this, AndroidResourceHelper.getMessage("EnterAADMobileServiceURL", this), Toast.LENGTH_LONG).show();
        LogManager.getInstance(this).info("{0} onStart()", this.getLocalClassName());
    }

    @SuppressWarnings("deprecation")
    protected void onDestroy() {
        View v = findViewById(R.id.table_layout);
        if (v != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                v.setBackgroundDrawable(null);
            } else {
                v.setBackground(null);
            }
        }

        super.onDestroy();

        if (progressDialog != null)
            this.progressDialog.dismiss();
    }

//    @Override
//    public Object onRetainNonConfigurationInstance() {
//        return welcomeDisplayed;
//    }


    protected ActionBar getMetrixActionBar() {
        return mSupportActionBar;
    }

    private void displayWelcome() {
        @SuppressWarnings("deprecation") final Object displayed = getLastNonConfigurationInstance();

        if (displayed == null) {
            Intent intent = MetrixActivityHelper.createActivityIntent(this, Welcome.class);
            MetrixActivityHelper.startNewActivity(this, intent);
        } else if ((Boolean) displayed == false) {
            Intent intent = MetrixActivityHelper.createActivityIntent(this, Welcome.class);
            MetrixActivityHelper.startNewActivity(this, intent);
        }

        welcomeDisplayed = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.validateUrl:
                MobileApplication.PerformingActivation = true;

                mServiceUrl = (EditText) findViewById(R.id.serviceUrl);
                String serviceUrl = mServiceUrl.getText().toString();
                activateButton.setEnabled(false);

                new Thread(new Runnable() {
                    public void run() {
                        queryAuthentication(serviceUrl);
                    }
                }).start();

                break;
            case R.id.cancel:
                finish();
                break;
        }
    }

    private void queryAuthentication(String serviceUrl) {
        Hashtable<String, String> authSettings = MetrixAuthenticationAssistant.getAuthenticationMethods(this,serviceUrl);
        String authMethods = authSettings.get("FSM_AUTHENTICATION_METHODS");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Stuff that updates the UI
                activateButton.setEnabled(true);
            }
        });

        Hashtable<String, String> OidcSettings = new  Hashtable<String, String>();

        SettingsHelper.saveStringSetting(this, SettingsHelper.AUTHENTICATION_METHODS, authMethods, true);
        SettingsHelper.saveServiceAddress(this, serviceUrl);

        // check FSM_AUTHENTICATION_METHODS param value enabled OIDC and FSM with invalid OIDC
        // this will prompt an error message and navigate to the ActivateDeviceStandard screen
        if (!MetrixStringHelper.isNullOrEmpty(authMethods) && ((authMethods.contains("FSM") == false && authMethods.contains("OIDC") == true))){
            OidcSettings = MetrixAuthenticationAssistant.getOidcConfiguration(this);
            if(OidcSettings == null || OidcSettings.size()<1) {
                MetrixUIHelper.showErrorDialogOnGuiThread(this, AndroidResourceHelper.getMessage("FetchOIDCSettingsFailure"));
                return;
            }
        }
        // check FSM_AUTHENTICATION_METHODS param value enabled OIDC and FSM with invalid OIDC
        // this will prompt an error message and navigate to the ActivateDeviceStandard screen
        else if (!MetrixStringHelper.isNullOrEmpty(authMethods) && ((authMethods.contains("FSM") == true && authMethods.contains("OIDC") == true))){
            OidcSettings = MetrixAuthenticationAssistant.getOidcConfiguration(this);
            if(OidcSettings == null || OidcSettings.size()<1) {
                SettingsHelper.saveStringSetting(this, "OIDC_INVALID", "True", true);
            }
            else {
                SettingsHelper.saveStringSetting(this, "OIDC_INVALID", "False", true);
            }
        }

        if(!MetrixStringHelper.isNullOrEmpty(authMethods) && authMethods.startsWith("FSM")){
            Intent intent = MetrixActivityHelper.createActivityIntent(this, "ActivateDevice");
            MetrixActivityHelper.startNewActivityAndFinish(this, intent);
        }
        else if(!MetrixStringHelper.isNullOrEmpty(authMethods) && authMethods.startsWith("OIDC")){
            Intent intent = MetrixActivityHelper.createActivityIntent(this, "ActivateDeviceOIDC");
            MetrixActivityHelper.startNewActivityAndFinish(this, intent);
        }
        else if(MetrixStringHelper.isNullOrEmpty(authMethods) || (authMethods.contains("FSM") == false && authMethods.contains("OIDC") == false)){
            MetrixUIHelper.showErrorDialogOnGuiThread(this, AndroidResourceHelper.getMessage("IncorrectURLOrNetworkIssue"));
        }
    }
}


