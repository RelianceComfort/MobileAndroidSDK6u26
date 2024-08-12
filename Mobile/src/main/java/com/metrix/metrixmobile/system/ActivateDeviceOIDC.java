package com.metrix.metrixmobile.system;

import com.metrix.architecture.actionbar.MetrixActionBarManager;
import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.oidc.AuthStateManager;
import com.metrix.metrixmobile.oidc.Configuration;
import com.metrix.metrixmobile.oidc.ConfigurationManager;
import com.metrix.metrixmobile.oidc.LogoutHandler;
import com.metrix.metrixmobile.oidc.TokenActivity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.ColorRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ClientSecretBasic;
import net.openid.appauth.RegistrationRequest;
import net.openid.appauth.RegistrationResponse;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.browser.AnyBrowserMatcher;
import net.openid.appauth.browser.BrowserMatcher;

import java.util.Collections;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.metrix.metrixmobile.global.MobileGlobal.END_SESSION_RELOGIN_CODE;
import static com.metrix.metrixmobile.global.MobileGlobal.END_SESSION_REQUEST_CODE;

public class ActivateDeviceOIDC extends AppCompatActivity implements View.OnClickListener {
    private ViewGroup mLayout;
    private Context mCurrentContext;
    private Boolean welcomeDisplayed = null;
    private static boolean singleSignOnChecked = false;
    private static ProgressDialog progressDialog;
    private boolean mLoginOnly = false;
    private ImageView mAppIcon;
    private TextView fsmButton, reenterButton;
    private Button activateOidcButton;
    private Drawable originalBackground;
    protected ActionBar mSupportActionBar;
    AlertDialog mLoginAlert;

    //////////////////// OIDC variables ///////////////////////
    private static final String TAG = "ActivateDeviceOIDC";
    private static final String EXTRA_FAILED = "failed";
    private static final int RC_AUTH = 100;
    private String mUserId;

    private AuthorizationService mAuthService;
    private AuthStateManager mAuthStateManager;
    private Configuration mConfiguration;
    private ConfigurationManager mConfigurationManager;
    private Hashtable<String, String> mLatestOidcSettings;

    private final AtomicReference<String> mClientId = new AtomicReference<>();
    private final AtomicReference<AuthorizationRequest> mAuthRequest = new AtomicReference<>();
    private final AtomicReference<CustomTabsIntent> mAuthIntent = new AtomicReference<>();
    private CountDownLatch mAuthIntentLatch = new CountDownLatch(1);
    private ExecutorService mExecutor;

    private boolean mUsePendingIntents;
    private boolean mLogoutReceiverInitialized;
    private BroadcastReceiver mLogoutReceiver;

    @NonNull
    private BrowserMatcher mBrowserMatcher = AnyBrowserMatcher.INSTANCE;
    ///////////////////////////////////////////////////////////


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activate_device_oidc);

        this.registerLogoutActivityReceiver();

        mCurrentContext = this;
        mLayout = (ViewGroup) findViewById(R.id.table_layout);
        mAppIcon = (ImageView) findViewById(R.id.app_icon);
        activateOidcButton = (Button) findViewById(R.id.oidcActivate);

        fsmButton = (TextView) findViewById(R.id.linkFSM);
        fsmButton.setMovementMethod(LinkMovementMethod.getInstance());
        fsmButton.setOnClickListener(this);

        reenterButton = (TextView) findViewById(R.id.linkEntry);
        reenterButton.setMovementMethod(LinkMovementMethod.getInstance());
        reenterButton.setOnClickListener(this);

        activateOidcButton = (Button) findViewById(R.id.oidcActivate);
        activateOidcButton.setOnClickListener(this);

        //////////////////// OIDC Contents ///////////////////////
        mExecutor = Executors.newSingleThreadExecutor();
        mAuthStateManager = AuthStateManager.getInstance(this);
        mConfigurationManager = ConfigurationManager.getInstance();
        mConfigurationManager.refreshConfiguration();
        mConfiguration = mConfigurationManager.getCurrent();

//        if (mAuthStateManager.getCurrent().isAuthorized()
//                && !mConfiguration.hasConfigurationChanged()) {
//            //Log.i(TAG, "User is already authenticated, proceeding to token activity");
//            startActivity(new Intent(this, TokenActivity.class));
//            finish();
//            return;
//        }

        if (!mConfiguration.isValid()) {
            Log.i(TAG, mConfiguration.getConfigurationError());
            return;
        }

        if (mConfiguration.hasConfigurationChanged()) {
            // discard any existing authorization state due to the change of configuration
            Log.i(TAG, "Configuration change detected, discarding old state");
            mAuthStateManager.replace(new AuthState());
            mConfiguration.acceptConfiguration();
        }

        if (getIntent().getBooleanExtra(EXTRA_FAILED, false)) {
            Log.i(TAG, "Authorization canceled");
        }

        Log.i(TAG, "Initializing");
        mExecutor.submit(this::initializeAppAuth);
        //////////////////////////////////////////////////////////
    }

    @MainThread
    private void configureBrowserSelector() {
        mBrowserMatcher = AnyBrowserMatcher.INSTANCE;

        recreateAuthorizationService();
        createAuthRequest("");
        warmUpBrowser();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onStart()
     */
    public void onStart() {
        super.onStart();
        applyTheme();

        AndroidResourceHelper.setResourceValues(activateOidcButton, "LoginFSM");
        AndroidResourceHelper.setResourceValues(fsmButton, "UseFSM");
        AndroidResourceHelper.setResourceValues(reenterButton, "ReEnterUrl");

        verifyConfig();

        mSupportActionBar = MetrixActionBarManager.getInstance().setupActionBar(this, R.layout.action_bar, false);
        String firstGradientText = "";
        String actionBarTitle = "Field Service Management";
        MetrixActionBarManager.getInstance().setupActionBarTitle(this, R.id.action_bar_title, actionBarTitle, firstGradientText);

        if (mExecutor.isShutdown()) {
            mExecutor = Executors.newSingleThreadExecutor();
        }
        // Toast.makeText(this, AndroidResourceHelper.getMessage("EnterAADMobileServiceURL", this), Toast.LENGTH_LONG).show();
        LogManager.getInstance(this).info("{0} onStart()", this.getLocalClassName());
    }

    private void verifyConfig() {
        String authMethods = SettingsHelper.getStringSetting(this, SettingsHelper.AUTHENTICATION_METHODS);
        String userId = SettingsHelper.getActivatedUser(this);
        if (!MetrixStringHelper.isNullOrEmpty(userId)) { //if (SettingsHelper.getStringSetting(this, SettingsHelper.OIDC_ENABLED).compareToIgnoreCase("ON") == 0) {
            fsmButton.setVisibility(View.GONE);
            reenterButton.setVisibility(View.GONE);
        } else if (!authMethods.toUpperCase().contains("FSM")) {
            fsmButton.setVisibility(View.GONE);
        }
    }

    private void applyTheme() {
        try {
            String userId = SettingsHelper.getActivatedUser(this);
            if (!MetrixStringHelper.isNullOrEmpty(userId)) {
                // We need active adapters cached for metadata retrieval to work.
                // If we cannot get a handle on adapters successfully, then fail with a log entry and carry on.
                MetrixDatabaseManager.createDatabaseAdapters(MobileApplication.getAppContext(), MetrixApplicationAssistant.getMetaIntValue(MobileApplication.getAppContext(), "DatabaseVersion"), com.metrix.metrixmobile.R.array.system_tables,
                        com.metrix.metrixmobile.R.array.business_tables);
                
                // Only apply theming to this screen if we already have an activated user
                String largeIconImageID = MetrixSkinManager.getLargeIconImageID();
                if (!MetrixStringHelper.isNullOrEmpty(largeIconImageID))
                    MetrixAttachmentHelper.applyImageWithDPScale(largeIconImageID, mAppIcon, 96, 96);

                String primaryColorString = MetrixSkinManager.getPrimaryColor();
                String hyperlinkColorString = MetrixSkinManager.getHyperlinkColor();
                int hyperlinkColor = Color.parseColor(hyperlinkColorString);
                MetrixActivity.setMaterialDesignForButtons(activateOidcButton, primaryColorString, this);
                fsmButton.setTextColor(hyperlinkColor);
                reenterButton.setTextColor(hyperlinkColor);
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
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

        if(mLogoutReceiverInitialized){
            unregisterReceiver(mLogoutReceiver);
        }

        if(mAuthService != null){
            mAuthService.dispose();
        }
        super.onDestroy();

        if (progressDialog != null)
            this.progressDialog.dismiss();
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

    protected ActionBar getMetrixActionBar() {
        return mSupportActionBar;
    }

    private void setVisibilities(boolean visible) {
        if (!visible) {
            fsmButton.setVisibility(View.INVISIBLE);
            reenterButton.setVisibility(View.INVISIBLE);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.oidcActivate:
                boolean requireLogin = SettingsHelper.getManualLogin(MobileApplication.getAppContext());
                boolean frontChannelSupported = SettingsHelper.getBooleanSetting(MobileApplication.getAppContext(), SettingsHelper.OIDC_FRONTCHANNEL_LOGOUT_SUPPORTED);

                if(requireLogin) {
                    LogoutHandler logoutHandler = new LogoutHandler(this);
                    if (mAuthStateManager.getCurrent().isAuthorized() && !frontChannelSupported) {
                        mLoginAlert = new AlertDialog.Builder(this).create();
                        mLoginAlert.setTitle(AndroidResourceHelper.getMessage("Login"));
                        mLoginAlert.setMessage(AndroidResourceHelper.getMessage("LogoutVerifiedPleaseLogin"));
                        mLoginAlert.setButton(AndroidResourceHelper.getMessage("OK"), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                logoutHandler.signOut();
                            }
                        });
                        mLoginAlert.show();
                    }
                    else
                        startOIDCWorkflow();
                }
                else {
                    startOIDCWorkflow();
                }
                break;
            case R.id.linkEntry:
                registerLogoutActivityReceiver();
                Intent intent = MetrixActivityHelper.createActivityIntent(this, ActivateDeviceEntry.class);
                MetrixActivityHelper.startNewActivityAndFinish(this, intent);
                break;
            case R.id.linkFSM:
                registerLogoutActivityReceiver();
                intent = MetrixActivityHelper.createActivityIntent(this, ActivateDevice.class);
                String serviceUrl = SettingsHelper.getServiceAddress(this);
                if (MetrixStringHelper.isNullOrEmpty(serviceUrl)) {
                    serviceUrl = "http://";
                }
                intent.putExtra("ServerAddress", serviceUrl);
                MetrixActivityHelper.startNewActivityAndFinish(this, intent);
                break;
        }
    }

    /**
     * Initializes the authorization service configuration if necessary, either from the local
     * static values or by retrieving an OpenID discovery document.
     */
    @WorkerThread
    private void initializeAppAuth() {
        Log.i(TAG, "Initializing AppAuth");
        recreateAuthorizationService();

        if (mAuthStateManager.getCurrent().getAuthorizationServiceConfiguration() != null) {
            // configuration is already created, skip to client initialization
            Log.i(TAG, "auth config already established");
            initializeClient();
            return;
        }

        // if we are not using discovery, build the authorization service configuration directly
        // from the static configuration values.
        if (mConfiguration.getDiscoveryUri() == null) {
            Log.i(TAG, "Creating auth config from res/raw/auth_config.json");
            AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(
                    mConfiguration.getAuthEndpointUri(),
                    mConfiguration.getTokenEndpointUri(),
                    mConfiguration.getRegistrationEndpointUri(),
                    mConfiguration.getEndSessionEndpoint());

            mAuthStateManager.replace(new AuthState(config));
            initializeClient();
            return;
        }

        // WrongThread inference is incorrect for lambdas
        // noinspection WrongThread
        Log.i(TAG, "Retrieving discovery document");
        Log.i(TAG, "Retrieving OpenID discovery doc");
        AuthorizationServiceConfiguration.fetchFromUrl(
                mConfiguration.getDiscoveryUri(),
                this::handleConfigurationRetrievalResult,
                mConfiguration.getConnectionBuilder());
    }

    @MainThread
    private void handleConfigurationRetrievalResult(
            AuthorizationServiceConfiguration config,
            AuthorizationException ex) {
        if (config == null) {
            Log.i(TAG, "Failed to retrieve discovery document", ex);
            return;
        }

        Log.i(TAG, "Discovery document retrieved");
        mAuthStateManager.replace(new AuthState(config));
        mExecutor.submit(this::initializeClient);
    }

    /**
     * Initiates a dynamic registration request if a client ID is not provided by the static
     * configuration.
     */
    @WorkerThread
    private void initializeClient() {
        if (mConfiguration.getClientId() != null) {
            Log.i(TAG, "Using static client ID: " + mConfiguration.getClientId());
            // use a statically configured client ID
            mClientId.set(mConfiguration.getClientId());
            runOnUiThread(this::initializeAuthRequest);
            return;
        }

//        RegistrationResponse lastResponse =
//                mAuthStateManager.getCurrent().getLastRegistrationResponse();
//        if (lastResponse != null) {
//            Log.i(TAG, "Using dynamic client ID: " + lastResponse.clientId);
//            // already dynamically registered a client ID
//            mClientId.set(lastResponse.clientId);
//            runOnUiThread(this::initializeAuthRequest);
//            return;
//        }

        // WrongThread inference is incorrect for lambdas
        // noinspection WrongThread
        Log.i(TAG, "Dynamically registering client");

        RegistrationRequest registrationRequest = new RegistrationRequest.Builder(
                mAuthStateManager.getCurrent().getAuthorizationServiceConfiguration(),
                Collections.singletonList(mConfiguration.getRedirectUri()))
                .setTokenEndpointAuthenticationMethod(ClientSecretBasic.NAME)
                .build();

        mAuthService.performRegistrationRequest(
                registrationRequest,
                this::handleRegistrationResponse);
    }

    @MainThread
    private void handleRegistrationResponse(
            RegistrationResponse response,
            AuthorizationException ex) {
        mAuthStateManager.updateAfterRegistration(response, ex);
        if (response == null) {
            Log.i(TAG, "Failed to dynamically register client", ex);
            return;
        }

        Log.i(TAG, "Dynamically registered client: " + response.clientId);
        mClientId.set(response.clientId);
        initializeAuthRequest();
    }

    /**
     * Performs the authorization request, using the browser selected in the spinner,
     * and a user-provided `login_hint` if available.
     */
    // private void doAuth()
    private void startOIDCWorkflow() {
        try {
            mAuthIntentLatch.await();
        } catch (InterruptedException ex) {
            Log.w(TAG, "Interrupted while waiting for auth intent");
        }

        Intent completionIntent = new Intent(this, TokenActivity.class);
        Intent cancelIntent = new Intent(this, ActivateDeviceOIDC.class);
        cancelIntent.putExtra(EXTRA_FAILED, true);
        cancelIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (mUsePendingIntents) {
            mAuthService.performAuthorizationRequest(
                    mAuthRequest.get(),
                    PendingIntent.getActivity(this, 0, completionIntent, Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0),
                    PendingIntent.getActivity(this, 0, cancelIntent, Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0),
                    mAuthIntent.get());
        } else {
            Intent intent = mAuthService.getAuthorizationRequestIntent(
                    mAuthRequest.get(),
                    PendingIntent.getActivity(this, 0, completionIntent, Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0),
                    PendingIntent.getActivity(this, 0, cancelIntent, Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0),
                    mAuthIntent.get());
            startActivityForResult(intent, RC_AUTH);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // only for gingerbread and newer versions
                finish();
            }
            else {
                finishAffinity();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == END_SESSION_REQUEST_CODE || requestCode == END_SESSION_RELOGIN_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                LogoutHandler logoutHandler = new LogoutHandler(this);
                logoutHandler.clearState();
                logoutHandler.relogin();
            } else {
                Log.i(TAG,"Logout failed, should try it again");
            }
        }
        else {
            if (resultCode == RESULT_CANCELED) {
                //displayAuthCancelled();
                Log.i(TAG, "Authorization cancelled");
            } else {
                Intent intent = new Intent(this, TokenActivity.class);
                intent.putExtras(data.getExtras());
                startActivity(intent);
            }
        }
    }

    private void recreateAuthorizationService() {
        if (mAuthService != null) {
            Log.i(TAG, "Discarding existing AuthService instance");
            mAuthService.dispose();
        }
        mAuthService = createAuthorizationService();
        mAuthRequest.set(null);
        mAuthIntent.set(null);
    }

    private AuthorizationService createAuthorizationService() {
        Log.i(TAG, "Creating authorization service");
        AppAuthConfiguration.Builder builder = new AppAuthConfiguration.Builder();
        builder.setBrowserMatcher(mBrowserMatcher);
        builder.setConnectionBuilder(mConfiguration.getConnectionBuilder());

        return new AuthorizationService(this, builder.build());
    }

    @MainThread
    private void initializeAuthRequest() {
        createAuthRequest(mUserId);
        warmUpBrowser();
        displayAuthOptions();
    }

    @MainThread
    private void displayAuthOptions() {
        AuthState state = mAuthStateManager.getCurrent();
        AuthorizationServiceConfiguration config = state.getAuthorizationServiceConfiguration();

        String authEndpointStr;
        if (config.discoveryDoc != null) {
            authEndpointStr = "Discovered auth endpoint: \n";
        } else {
            authEndpointStr = "Static auth endpoint: \n";
        }
        authEndpointStr += config.authorizationEndpoint;

        String clientIdStr;
        if (state.getLastRegistrationResponse() != null) {
            clientIdStr = "Dynamic client ID: \n";
        } else {
            clientIdStr = "Static client ID: \n";
        }
        clientIdStr += mClientId;
    }

    private void warmUpBrowser() {
        mAuthIntentLatch = new CountDownLatch(1);
        mExecutor.execute(() -> {
            Log.i(TAG, "Warming up browser instance for auth request");
            CustomTabsIntent.Builder intentBuilder =
                    mAuthService.createCustomTabsIntentBuilder(mAuthRequest.get().toUri());
            intentBuilder.setToolbarColor(getColorCompat(R.color.IFSLightPurple));
            mAuthIntent.set(intentBuilder.build());
            mAuthIntentLatch.countDown();
        });
    }

    private void createAuthRequest(@Nullable String loginHint) {
        AuthorizationRequest.Builder authRequestBuilder = new AuthorizationRequest.Builder(
                mAuthStateManager.getCurrent().getAuthorizationServiceConfiguration(),
                mClientId.get(),
                ResponseTypeValues.CODE,
                mConfiguration.getRedirectUri())
                .setScope(mConfiguration.getScope());

        if (!TextUtils.isEmpty(loginHint)) {
            authRequestBuilder.setLoginHint(loginHint);
        }

        mAuthRequest.set(authRequestBuilder.build());
    }

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressWarnings("deprecation")
    private int getColorCompat(@ColorRes int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getColor(color);
        } else {
            return getResources().getColor(color);
        }
    }

    private void registerLogoutActivityReceiver() {
        if(mLogoutReceiverInitialized)
            return;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.package.ACTION_LOGOUT");
        mLogoutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("onReceive","Logout in progress");
                finish();
            }
        };
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(mLogoutReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            //noinspection UnspecifiedRegisterReceiverFlag
            registerReceiver(mLogoutReceiver, intentFilter);
        }
        mLogoutReceiverInitialized = true;
    }
    /**
     * Responds to changes in the login hint. After a "debounce" delay, warms up the browser
     * for a request with the new login hint; this avoids constantly re-initializing the
     * browser while the user is typing.
     */
    private final class LoginHintChangeHandler implements TextWatcher {

        private static final int DEBOUNCE_DELAY_MS = 500;

        private Handler mHandler;
        private RecreateAuthRequestTask mTask;

        LoginHintChangeHandler() {
            mHandler = new Handler(Looper.getMainLooper());
            mTask = new RecreateAuthRequestTask();
        }

        @Override
        public void beforeTextChanged(CharSequence cs, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence cs, int start, int before, int count) {
            mTask.cancel();
            mTask = new RecreateAuthRequestTask();
            mHandler.postDelayed(mTask, DEBOUNCE_DELAY_MS);
        }

        @Override
        public void afterTextChanged(Editable ed) {}
    }

    private final class RecreateAuthRequestTask implements Runnable {

        private final AtomicBoolean mCanceled = new AtomicBoolean();

        @Override
        public void run() {
            if (mCanceled.get()) {
                return;
            }

            createAuthRequest(mUserId);
            warmUpBrowser();
        }

        public void cancel() {
            mCanceled.set(true);
        }
    }
}

