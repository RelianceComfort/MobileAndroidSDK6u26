package com.metrix.architecture.utilities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

/**
 * This class contains methods which allow the application to communicate 
 * with the Skype application. 
 * 
 * @since 5.6
 */
public class SkypeCommunicator {
	/**
	 * Initiate the actions encoded in the specified URI.
	 */
	public static void initiateSkypeUri(Context myContext, String mySkypeUri) {
		if (!isSkypeClientInstalled(myContext, "com.microsoft.office.lync15")) {
			goToMarket(myContext);
			return;
		}

	  // Create the Intent from our Skype URI
	  Uri skypeUri = Uri.parse(mySkypeUri);

	  Intent myIntent = new Intent(Intent.ACTION_VIEW, skypeUri);

	  // Initiate the Intent. It should never fail since we've already established the
	  // presence of its handler (although there is an extremely minute window where that
	  // handler can go away...)
	  myContext.startActivity(myIntent);
	
	  return;
	}
	

	/**
	 * Determine whether the Skype for Android client is installed on this device.
	 * @param myContext
	 * @return
	 */
    public static boolean isSkypeClientInstalled(Context myContext, String mySkypeUri) {
        try{
            PackageManager pm = myContext.getPackageManager();
            pm.getPackageInfo(mySkypeUri, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
	
	/**
	 * Install the Skype client through the market: URI scheme.
	 * @param myContext
	 */
	public static void goToMarket(Context myContext) {
	  Uri marketUri = Uri.parse("https://play.google.com/store/apps/details?id=com.microsoft.office.lync15");
	  Intent myIntent = new Intent(Intent.ACTION_VIEW, marketUri);
	  myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	  myContext.startActivity(myIntent);

	  return;
	}
}
