/*
 * Copyright 2015 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metrix.metrixmobile.oidc;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.metrix.architecture.actionbar.MetrixActionBarManager;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixPrivateCache;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixSecurityHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.OidcHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.MetrixAuthenticationAssistant;
import com.metrix.metrixmobile.system.ActivateDevice;
import com.metrix.metrixmobile.system.ActivateDeviceEntry;
import com.metrix.metrixmobile.system.SyncServiceMonitor;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceDiscovery;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.joda.time.format.DateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import okio.Okio;

/**
 * Displays the authorized state of the user. This activity is provided with the outcome of the
 * authorization flow, which it uses to negotiate the final authorized state,
 * by performing an authorization code exchange if necessary. After this, the activity provides
 * additional post-authorization operations if available, such as fetching user info and refreshing
 * access tokens.
 */
public class TokenActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView fsmButton, reenterButton;
    private Button activateOidcButton;

    private static final String TAG = "TokenActivity";
    private static final String KEY_USER_INFO = "userInfo";
    private static ProgressDialog progressDialog;
    private AuthorizationService mAuthService;
    private AuthStateManager mStateManager;
    private Hashtable<String, String> mLatestOidcSettings;
    private final AtomicReference<JSONObject> mUserInfoJson = new AtomicReference<>();
    private ExecutorService mExecutor;
    private Configuration mConfiguration;
    private String mAuthorizedEmail;
    protected ActionBar mSupportActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activate_device_oidc);

        activateOidcButton = (Button) findViewById(R.id.oidcActivate);
        activateOidcButton.setOnClickListener(this);

        fsmButton = (TextView) findViewById(R.id.linkFSM);
        fsmButton.setMovementMethod(LinkMovementMethod.getInstance());
        fsmButton.setOnClickListener(this);

        reenterButton = (TextView) findViewById(R.id.linkEntry);
        reenterButton.setMovementMethod(LinkMovementMethod.getInstance());
        reenterButton.setOnClickListener(this);
        MetrixAuthenticationAssistant.configureAppParams();

        mStateManager = AuthStateManager.getInstance(this);
        mExecutor = Executors.newSingleThreadExecutor();
        mLatestOidcSettings = OidcHelper.retrieveOidcSettings(this);
        mConfiguration = Configuration.getInstance(this, mLatestOidcSettings);

        mAuthService = new AuthorizationService(
                this,
                new AppAuthConfiguration.Builder()
                        .setConnectionBuilder(mConfiguration.getConnectionBuilder())
                        .build());

        if (savedInstanceState != null) {
            try {
                mUserInfoJson.set(new JSONObject(savedInstanceState.getString(KEY_USER_INFO)));
            } catch (Exception ex) {
                Log.e(TAG, "Failed to parse saved user info JSON, discarding", ex);
            }
        }
    }

    @Override
    protected void onStart() {
        AndroidResourceHelper.setResourceValues(activateOidcButton, "Activate");
        AndroidResourceHelper.setResourceValues(fsmButton, "UseFSM");
        AndroidResourceHelper.setResourceValues(reenterButton, "ReEnterUrl");
        super.onStart();
        verifyConfig();
        String currentUser = SettingsHelper.getActivatedUser(this);
        if(MetrixStringHelper.isNullOrEmpty(currentUser)){
            progressDialog = ProgressDialog.show(this, AndroidResourceHelper.getMessage("Activating"),
                    AndroidResourceHelper.getMessage("ActivationWait"), true, false);
        }else{
            progressDialog = ProgressDialog.show(this, AndroidResourceHelper.getMessage("Authenticating"),
                    AndroidResourceHelper.getMessage("PleaseWait"), true, false);
        }
        mSupportActionBar = MetrixActionBarManager.getInstance().setupActionBar(this, R.layout.action_bar, false);
        String firstGradientText = "";
        String actionBarTitle = "Field Service Management";
        MetrixActionBarManager.getInstance().setupActionBarTitle(this, R.id.action_bar_title, actionBarTitle, firstGradientText);
  //      MetrixActionBarManager.getInstance().setActionBarDefaultIcon(R.drawable.ifs_logo, mSupportActionBar, 24, 24);

        if (mExecutor.isShutdown()) {
            mExecutor = Executors.newSingleThreadExecutor();
        }

        if (mStateManager.getCurrent().isAuthorized()) {
            displayAuthorized();
            activateProcess();
            return;
        }

        // the stored AuthState is incomplete, so check if we are currently receiving the result of
        // the authorization flow from the browser.
        AuthorizationResponse response = AuthorizationResponse.fromIntent(getIntent());
        AuthorizationException ex = AuthorizationException.fromIntent(getIntent());

        if (response != null || ex != null) {
            mStateManager.updateAfterAuthorization(response, ex);
        }

        if (response != null && response.authorizationCode != null) {
            // authorization code exchange is required
            mStateManager.updateAfterAuthorization(response, ex);

            exchangeAuthorizationCode(response);
        } else if (ex != null) {
            LogManager.getInstance().error("Authorization flow failed: " + ex.getMessage());
            progressDialog.dismiss();
        } else {
            LogManager.getInstance().error("No authorization state retained - reauthorization required");
            progressDialog.dismiss();
        }
    }

    private void verifyConfig() {
        String authMethods = SettingsHelper.getStringSetting(this, SettingsHelper.AUTHENTICATION_METHODS);
        if (SettingsHelper.getStringSetting(this, SettingsHelper.OIDC_ENABLED).compareToIgnoreCase("ON") == 0) {
            fsmButton.setVisibility(View.GONE);
            reenterButton.setVisibility(View.GONE);
        } else if (!authMethods.toUpperCase().contains("FSM")) {
            fsmButton.setVisibility(View.GONE);
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
                //MetrixDialogAssistant.showAlertDialog("Activation", "Network error during activation process. Please redo authentication or use FSM authentication.", "OK", null, "Cancel", null, this);
                if (mStateManager.getCurrent().isAuthorized()) {
                    String currentUser = SettingsHelper.getActivatedUser(this);
                    if(progressDialog == null || !progressDialog.isShowing() && MetrixStringHelper.isNullOrEmpty(currentUser)){
                        progressDialog = ProgressDialog.show(this, AndroidResourceHelper.getMessage("Activating"),
                                AndroidResourceHelper.getMessage("ActivationWait"), true, false);
                    }
                    displayAuthorized();
                    activateProcess();
                }
                break;
            case R.id.linkEntry:
                registerLogoutAcvitityReceiver();
                Intent intent = MetrixActivityHelper.createActivityIntent(this, ActivateDeviceEntry.class);
                MetrixActivityHelper.startNewActivityAndFinish(this, intent);
                break;
            case R.id.linkFSM:
                registerLogoutAcvitityReceiver();
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

    private void registerLogoutAcvitityReceiver () {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.package.ACTION_LOGOUT");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("onReceive", "Logout in progress");
                finish();
            }
        };
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            //noinspection UnspecifiedRegisterReceiverFlag
            registerReceiver(receiver, intentFilter);
        }
    }

    @MainThread
    private void activateProcess() {
        String personId = SettingsHelper.getRememberMe(this);
        String password = SettingsHelper.getStringSetting(this, SettingsHelper.USER_LOGIN_PASSWORD);
        String serviceUrl = SettingsHelper.getServiceAddress(this);

        String currentUser = SettingsHelper.getActivatedUser(this);
        if(MetrixStringHelper.isNullOrEmpty(currentUser)) {
            new Thread(() -> activateDevice(progressDialog, personId, password, serviceUrl)).start();
            return;
        } else {
            try {
                String passwordUpdated = SettingsHelper.getStringSetting(this,"SETTING_PASSWORD_UPDATED");
                MetrixPrivateCache.resetDatabase();

                if(!MetrixStringHelper.isNullOrEmpty(passwordUpdated) && passwordUpdated.equalsIgnoreCase("Y")) {
                    if(MetrixStringHelper.isNullOrEmpty(password)) {
                        throw new Exception(AndroidResourceHelper.getMessage("InvalidUserPass"));
                    }
                    // clear PCHANGE settings when new password is obtained after server password change
                    MetrixAuthenticationAssistant.resetServerPasswordChanged(personId, password);
                }

                MobileApplication.ApplicationNullIfCrashed = "NOT NULL";
                SettingsHelper.saveBooleanSetting(this, SettingsHelper.FROM_LOGOUT, false);
                SettingsHelper.saveRememberMe(MobileApplication.getAppContext(), personId);

                MetrixAuthenticationAssistant.configureAppParams();
                User.setUser(personId, MobileApplication.getAppContext());

                Intent intent = MetrixActivityHelper.getInitialActivityIntent(this);
                MetrixActivityHelper.startNewActivityAndFinish(this, intent);

                mExecutor.submit(this::startSyncAfterLogin);
            }
            catch(Exception ex) {
                LogManager.getInstance().error(ex);
                MetrixUIHelper.showErrorDialogOnGuiThread(this, ex.getMessage());
            }
        }
    }

    @WorkerThread
    private void startSyncAfterLogin() {
        MobileApplication.startSync(MobileApplication.getAppContext());
    }

    /**
     * @param progressDialog
     * @param personId
     * @param password
     * @param serviceUrl
     */
    @MainThread
    private void activateDevice(ProgressDialog progressDialog, String personId, String password, String serviceUrl) {
        try {
            MetrixAuthenticationAssistant.LoginResult loginResult = MetrixAuthenticationAssistant.activateDevice(this, serviceUrl, personId, password, true);

            if (loginResult == MetrixAuthenticationAssistant.LoginResult.LICENSING_VIOLATION)
                throw new Exception((AndroidResourceHelper.getMessage("LicensingViolation")));
            else if (loginResult == MetrixAuthenticationAssistant.LoginResult.INVALID_PERSON_OR_PASSWORD)
                throw new Exception(AndroidResourceHelper.getMessage("InvalidUserPass"));
            else if (loginResult == MetrixAuthenticationAssistant.LoginResult.PASSWORD_EXPIRED)
                throw new Exception(AndroidResourceHelper.getMessage("PasswordExpired"));
            else if (loginResult == MetrixAuthenticationAssistant.LoginResult.REQUIRES_ACTIVATION)
                throw new Exception(AndroidResourceHelper.getMessage("NetworkProblem"));
            else if (loginResult != MetrixAuthenticationAssistant.LoginResult.SUCCESS)
                throw new Exception(AndroidResourceHelper.getMessage("UnknownActivationError"));

            if (progressDialog != null)
                progressDialog.dismiss();

            SettingsHelper.saveRememberMe(this, personId);
            LogManager.getInstance().delete();
            LogManager.getInstance().setMaxLogs();

            String hashPassword = "";
            if (password.endsWith("=="))
                hashPassword = password;
            else
                hashPassword = MetrixSecurityHelper.HashPassword(password);
            MetrixDatabaseManager.executeSql(String.format("insert into user_credentials (person_id, password) values ('%1$s', '%2$s')", personId, hashPassword));

            MetrixPublicCache.instance.addItem("person_id", personId);
            SettingsHelper.saveStringSetting(this, SettingsHelper.USER_LOGIN_PASSWORD, hashPassword, true);
            SettingsHelper.saveStringSetting(this, SettingsHelper.OIDC_ENABLED, "ON", true);
            SettingsHelper.saveManualLogin(this, false);

            MetrixFileHelper.deleteFiles(MetrixAttachmentManager.getInstance().getAttachmentPath());

            Intent intent = MetrixActivityHelper.createActivityIntent(this, SyncServiceMonitor.class);
            intent.putExtra("StartService", true);
            intent.putExtra("StartSync", true);
            intent.putExtra("StartLocation", true);
            intent.putExtra("ShowInitDialog", true);
            MetrixActivityHelper.startNewActivityAndFinish(this, intent);
        } catch (Exception e) {
            if (progressDialog != null)
                progressDialog.dismiss();

            SettingsHelper.removeSetting("SERVER_AUTHENTICATE_ERROR_MESSAGE");
            MetrixUIHelper.showErrorDialogOnGuiThread(this, e.getMessage());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        // user info is retained to survive activity restarts, such as when rotating the
        // device or switching apps. This isn't essential, but it helps provide a less
        // jarring UX when these events occur - data does not just disappear from the view.
        if (mUserInfoJson.get() != null) {
            state.putString(KEY_USER_INFO, mUserInfoJson.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAuthService.dispose();
        mExecutor.shutdownNow();
    }

    @MainThread
    private void displayAuthorized() {
        AuthState state = mStateManager.getCurrent();

        if (state.getAccessToken() == null) {
            LogManager.getInstance().debug("no_access_token_returned");
        } else {
            Long expiresAt = state.getAccessTokenExpirationTime();
            if (expiresAt == null) {
                LogManager.getInstance().debug("no_access_token_expiry");
            } else if (expiresAt < System.currentTimeMillis()) {
                LogManager.getInstance().debug("access_token_expired");
            } else {
                String template = "access_token_expires_at";
                LogManager.getInstance().debug(String.format(template,
                        DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss ZZ").print(expiresAt)));
            }
        }

        AuthorizationServiceDiscovery discoveryDoc =
                state.getAuthorizationServiceConfiguration().discoveryDoc;
        if ((discoveryDoc == null || discoveryDoc.getUserinfoEndpoint() == null)
                && mConfiguration.getUserInfoEndpointUri() == null) {
            LogManager.getInstance().debug("Failed to retrieve user info");
        }

        JSONObject userInfo = mUserInfoJson.get();
        if (userInfo != null) {
            try {
                String name = "???";
                if (userInfo.has("name")) {
                    name = userInfo.getString("name");
                }

                LogManager.getInstance().debug("User Name: "+name);
            } catch (JSONException ex) {
                Log.e(TAG, "Failed to read userinfo JSON", ex);
            }
        }
    }

    @MainThread
    private void refreshAccessToken() {
        performTokenRequest(
                mStateManager.getCurrent().createTokenRefreshRequest(),
                this::handleAccessTokenResponse);
    }

    @MainThread
    private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
        performTokenRequest(
                authorizationResponse.createTokenExchangeRequest(),
                this::handleCodeExchangeResponse);
    }

    @MainThread
    private void performTokenRequest(
            TokenRequest request,
            AuthorizationService.TokenResponseCallback callback) {
        ClientAuthentication clientAuthentication;
        try {
            clientAuthentication = mStateManager.getCurrent().getClientAuthentication();
        } catch (ClientAuthentication.UnsupportedAuthenticationMethod ex) {
            Log.d(TAG, "Token request cannot be made, client authentication for the token "
                            + "endpoint could not be constructed (%s)", ex);
            LogManager.getInstance().debug("Client authentication method is unsupported");
            return;
        }

        mAuthService.performTokenRequest(
                request,
                clientAuthentication,
                callback);
    }

    @WorkerThread
    private void handleAccessTokenResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
        mStateManager.updateAfterTokenResponse(tokenResponse, authException);
        runOnUiThread(this::displayAuthorized);
    }

    private String decodeToken(String token){
        if(token == null || token.length() == 0)
            return "";
        String[] tokenParts = token.split("\\.");
        return new String(Base64.decode(tokenParts[1], Base64.DEFAULT));
    }

    private String getEmailAddressFromToken(TokenResponse tokenReponse){
        String idJsonString = decodeToken(tokenReponse.idToken);
        try {
            JSONObject isJsonObject = new JSONObject(idJsonString);
            String authEmail = isJsonObject.getString("upn");
            return authEmail;
        }catch(Exception ex){
            LogManager.getInstance().error(ex);
            return null;
        }
    }

    @WorkerThread
    private void handleCodeExchangeResponse (
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
        if (tokenResponse != null)
            mAuthorizedEmail = getEmailAddressFromToken(tokenResponse);

        mStateManager.updateAfterTokenResponse(tokenResponse, authException);
        if (!mStateManager.getCurrent().isAuthorized()) {
            final String message = "Authorization Code exchange failed" + ((authException != null) ? authException.error : "");
            LogManager.getInstance().debug(message);
        } else {
            SettingsHelper.saveStringSetting(this, SettingsHelper.OIDC_ACCESS_TOKEN, tokenResponse.accessToken, true);
            SettingsHelper.saveStringSetting(this, SettingsHelper.OIDC_REFRESH_TOKEN, tokenResponse.refreshToken, true);
            SettingsHelper.saveStringSetting(this, SettingsHelper.OIDC_ID_TOKEN, tokenResponse.idToken, true);
            if (MetrixStringHelper.isNullOrEmpty(SettingsHelper.getActivatedUser(this)))
                SettingsHelper.saveManualLogin(this, false);

            String oidcSuccess = SettingsHelper.getStringSetting(this, SettingsHelper.OIDC_ENABLED);

            // It had been authenorized by OIDC process before, just need to reauthorize to start the app initial screen
            if (!MetrixStringHelper.isNullOrEmpty(oidcSuccess)&& oidcSuccess.equalsIgnoreCase("ON")) {
                String passwordChangeInProgress = SettingsHelper.getStringSetting(this, "SETTING_PASSWORD_UPDATED");
                if (!MetrixStringHelper.isNullOrEmpty(passwordChangeInProgress) && MetrixStringHelper.valueIsEqual(passwordChangeInProgress, "Y"))
                    mExecutor.submit(this::getFSMCredentials);
                else
                    runOnUiThread(this::activateProcess);
            } else {
                mExecutor.submit(this::getFSMCredentials);
            }
        }
    }


    /**
     * Performs the authorization request, using the browser selected in the spinner,
     * and a user-provided `login_hint` if available.
     */
    @WorkerThread
    private void getFSMCredentials() {
        String serviceUrl = SettingsHelper.getServiceAddress(this);
        MetrixPublicCache.instance.addItem("OIDC_USER_ID", mAuthorizedEmail);
        String accessToken = SettingsHelper.getStringSetting(this, SettingsHelper.OIDC_ACCESS_TOKEN);
        String refreshToken = SettingsHelper.getStringSetting(this, SettingsHelper.OIDC_REFRESH_TOKEN);
        String idToken = SettingsHelper.getStringSetting(this, SettingsHelper.OIDC_ID_TOKEN);

        Hashtable<String, String> personSettings = MetrixAuthenticationAssistant.getOidcPersonSetting(this, serviceUrl, refreshToken, idToken, accessToken);

        try {
            if (personSettings != null && personSettings.size() > 0) {
                String personId = personSettings.containsKey("person_id") ? personSettings.get("person_id").toString() : "";
                String hashPassword = personSettings.containsKey("password") ? personSettings.get("password").toString() : "";

                if (MetrixStringHelper.isNullOrEmpty(personId) || MetrixStringHelper.isNullOrEmpty(hashPassword)) {
                    MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, "Failed to validate user information!");
                    return;
                }

                MetrixPublicCache.instance.addItem("person_id", personId);
                SettingsHelper.saveStringSetting(this, SettingsHelper.USER_LOGIN_PASSWORD, hashPassword, true);
            } else {
                MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, "Failed to validate user information!");
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
                return;
            }
            runOnUiThread(this::activateProcess);
        }
        catch (Exception ex) {
            Log.w(TAG, "Interrupted while waiting for auth intent");
            runOnUiThread(()->{
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
            });
        }
    }

    /**
     * Demonstrates the use of {@link AuthState#performActionWithFreshTokens} to retrieve
     * user info from the IDP's user info endpoint. This callback will negotiate a new access
     * token / id token for use in a follow-up action, or provide an error if this fails.
     */
    @MainThread
    private void fetchUserInfo() {
        mStateManager.getCurrent().performActionWithFreshTokens(mAuthService, this::fetchUserInfo);
    }

    @MainThread
    private void fetchUserInfo(String accessToken, String idToken, AuthorizationException ex) {
        if (ex != null) {
            Log.e(TAG, "Token refresh failed when fetching user info");
            mUserInfoJson.set(null);
            runOnUiThread(this::displayAuthorized);
            return;
        }

        AuthorizationServiceDiscovery discovery =
                mStateManager.getCurrent()
                        .getAuthorizationServiceConfiguration()
                        .discoveryDoc;

        URL userInfoEndpoint;
        try {
            userInfoEndpoint =
                    mConfiguration.getUserInfoEndpointUri() != null
                        ? new URL(mConfiguration.getUserInfoEndpointUri().toString())
                        : new URL(discovery.getUserinfoEndpoint().toString());
        } catch (MalformedURLException urlEx) {
            Log.e(TAG, "Failed to construct user info endpoint URL", urlEx);
            mUserInfoJson.set(null);
            runOnUiThread(this::displayAuthorized);
            return;
        }

        mExecutor.submit(() -> {
            try {
                HttpURLConnection conn =
                        (HttpURLConnection) userInfoEndpoint.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setInstanceFollowRedirects(false);
                String response = Okio.buffer(Okio.source(conn.getInputStream()))
                        .readString(Charset.forName("UTF-8"));
                mUserInfoJson.set(new JSONObject(response));
            } catch (IOException ioEx) {
                Log.e(TAG, "Network error when querying userinfo endpoint", ioEx);
                showSnackbar("Fetching user info failed");
            } catch (JSONException jsonEx) {
                Log.e(TAG, "Failed to parse userinfo response");
                showSnackbar("Failed to parse user info");
            }

            runOnUiThread(this::displayAuthorized);
        });
    }

    @MainThread
    private void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.coordinator),
                message,
                Snackbar.LENGTH_SHORT)
                .show();
    }
}
