package com.metrix.metrixmobile.oidc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.SettingsHelper;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.EndSessionRequest;
import net.openid.appauth.browser.AnyBrowserMatcher;
import androidx.annotation.AnyThread;

import static com.metrix.metrixmobile.global.MobileGlobal.END_SESSION_RELOGIN_CODE;
import static com.metrix.metrixmobile.global.MobileGlobal.END_SESSION_REQUEST_CODE;

public class LogoutHandler {
    private final Context mContext;
    private AuthStateManager mStateManager;
    private Configuration mConfiguration;
    private AuthorizationService mAuthService;

     @AnyThread
    public LogoutHandler(Activity context) {
        mContext = context;
        mStateManager = AuthStateManager.getInstance(mContext);
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        configurationManager.refreshConfiguration();
        mConfiguration = configurationManager.getCurrent();
    }

    /**
     * Authentication logout and close Application.
     *
     */
    public void logout() {
        boolean frontChannelSupported = SettingsHelper.getBooleanSetting(mContext, SettingsHelper.OIDC_FRONTCHANNEL_LOGOUT_SUPPORTED);

        // There is no frontchannel_logout supported by AppAuth library, clear authState to logout and use login prompt to relogin
        if(frontChannelSupported) {
            clearState();
            closeApp();
        }
        else {
            this.endSession();
        }
    }

    /**
     * Authentication logout and enter login page.
     *
     */
    public void signOut() {
        boolean frontChannelSupported = SettingsHelper.getBooleanSetting(mContext, SettingsHelper.OIDC_FRONTCHANNEL_LOGOUT_SUPPORTED);

        // There is no frontchannel_logout supported by AppAuth library, clear authState to logout and use login prompt to relogin
        if(frontChannelSupported) {
            clearState();
            relogin();
        }
        else {
            this.clearSession(END_SESSION_RELOGIN_CODE);
        }
    }

    /**
     *  Send end session request, save FROM_LOGOUT setting and close app
     *
     */
    private void endSession() {
        mStateManager = AuthStateManager.getInstance(mContext);
        AuthState currentState = mStateManager.getCurrent();
        AuthorizationServiceConfiguration config =
                currentState.getAuthorizationServiceConfiguration();
        mAuthService = createAuthorizationService();
        if (config.endSessionEndpoint != null) {
            SettingsHelper.saveBooleanSetting(mContext, SettingsHelper.FROM_LOGOUT, true);
            Intent endSessionIntent = mAuthService.getEndSessionRequestIntent(
                    new EndSessionRequest.Builder(config)
                            .setIdTokenHint(currentState.getIdToken())
                            .setPostLogoutRedirectUri(mConfiguration.getEndSessionRedirectUri()) // use SettingsHelper.getStringSetting(MobileApplication.getAppContext(), SettingsHelper.OIDC_REDIRECT_URI) to replace getEndSessionRedirectUri
                            .build());
            ((Activity)mContext).startActivityForResult(endSessionIntent, END_SESSION_REQUEST_CODE);
        } else {
            clearState();
            closeApp();
        }
    }

    /**
    *  Send end session request and process result
    *
    */
    public void clearSession (int requestCode) {
        mStateManager = AuthStateManager.getInstance(mContext);
        AuthState currentState = mStateManager.getCurrent();
        AuthorizationServiceConfiguration config =
                currentState.getAuthorizationServiceConfiguration();
        mAuthService = createAuthorizationService();
        if (config.endSessionEndpoint != null) {
            Intent endSessionIntent = mAuthService.getEndSessionRequestIntent(
                    new EndSessionRequest.Builder(config)
                            .setIdTokenHint(currentState.getIdToken())
                            .setPostLogoutRedirectUri(mConfiguration.getEndSessionRedirectUri()) // use SettingsHelper.getStringSetting(MobileApplication.getAppContext(), SettingsHelper.OIDC_REDIRECT_URI) to replace getEndSessionRedirectUri
                            .build());
            ((Activity)mContext).startActivityForResult(endSessionIntent, requestCode);
        }
    }

    @SuppressWarnings("deprecation")
    public static void clearCookies(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            CookieSyncManager cookieSyncMngr= CookieSyncManager.createInstance(context);
            cookieSyncMngr.startSync();
            CookieManager cookieManager= CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }
    }

    /*
    * This method is to clear local authentication status and close the app
    * */
    public void clientLogout() {
        clearState();
        closeApp();
    }

    public void clearState() {
        SettingsHelper.saveStringSetting(mContext, SettingsHelper.OIDC_REFRESH_TOKEN,"",true);
        SettingsHelper.saveStringSetting(mContext, SettingsHelper.OIDC_ACCESS_TOKEN,"",true);

        // discard the authorization and token state, but retain the configuration and
        // dynamic client registration (if applicable), to save from retrieving them again.
        mStateManager = AuthStateManager.getInstance(mContext);
        AuthState currentState = mStateManager.getCurrent();
        AuthState clearedState =
                new AuthState(currentState.getAuthorizationServiceConfiguration());
        if (currentState.getLastRegistrationResponse() != null) {
            clearedState.update(currentState.getLastRegistrationResponse());
        }
        mStateManager.replace(clearedState);
    }

    public void closeApp() {
        SettingsHelper.saveBooleanSetting(mContext, SettingsHelper.FROM_LOGOUT, true);

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("com.package.ACTION_LOGOUT");
        mContext.sendBroadcast(broadcastIntent);

        ((Activity)mContext).finishAffinity();
    }

    public void relogin() {
        //SettingsHelper.saveBooleanSetting(mContext, SettingsHelper.FROM_LOGOUT, true);

        try {
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("com.package.ACTION_LOGOUT");
            mContext.sendBroadcast(broadcastIntent);

            Intent intent = new Intent();
            intent.setClass(mContext, Class.forName("com.metrix.metrixmobile.system.Login"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mContext.startActivity(intent);
            ((Activity)mContext).finish();
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private AuthorizationService createAuthorizationService() {
        try {
            AppAuthConfiguration.Builder builder = new AppAuthConfiguration.Builder();
            builder.setBrowserMatcher(AnyBrowserMatcher.INSTANCE); // it should match the same BrowserMatcher in ActiveDeviceOIDC class
            builder.setConnectionBuilder(mConfiguration.getConnectionBuilder());

            return new AuthorizationService(mContext, builder.build());
        }
        catch(Exception ex) {
            LogManager.getInstance().error(ex);
        }

        return null;
    }
}
