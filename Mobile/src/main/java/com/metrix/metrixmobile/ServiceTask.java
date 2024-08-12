package com.metrix.metrixmobile;

import org.json.JSONException;

import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.services.RemoteMessagesHandler.HandlerException;
import com.metrix.architecture.utilities.LogManager;

import android.app.Activity;
import android.os.AsyncTask;

public class ServiceTask extends AsyncTask<String, Void, Boolean> {
    private Activity activity;

    public ServiceTask(Activity activity) {
        this.activity = activity;
    }

    /** progress dialog to show user that the backup is processing. */

    protected void onPreExecute() {
//        this.dialog.setMessage("Your device is being activated. Please wait.");
//        this.dialog.show();
    }

    @Override
    protected void onPostExecute(final Boolean success) {
 
    }

    protected Boolean doInBackground(final String... args) {
    	if(args.length>=2) {
    		return serviceExecutePost(args[0], args[1]);
    	}    	
		return serviceExecuteGet(args[0]);
    }
    
	/**
	 * @param serviceUrl
	 */
	private boolean serviceExecuteGet(String serviceUrl) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(activity, 5);
		
		try {
			String response = remote.executeGet(serviceUrl).replace("\\", "");
			if (response != null) {
				if (response.contains("true")) {
					return true;
				}
			}
			
			return false;
		} catch (HandlerException ex) {
			LogManager.getInstance().error(ex);
			return false;
		} catch (JSONException ex) {
			LogManager.getInstance().error(ex);
			return false;
		}
	}
	
	/**
	 * @param serviceUrl
	 */
	private boolean serviceExecutePost(String serviceUrl, String content) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(activity, 5);
		
		try {
			String contentType = "application/xml; charset=utf-8";
			String response = remote.executePost(serviceUrl, contentType, content).replace("\\", "");
			if (response != null) {
				if (response.contains("true")) {
					return true;
				}
			}
			
			return false;
		} catch (HandlerException ex) {
			LogManager.getInstance().error("MetrixAuthenticationAssistant.encodeUrlParam", ex.getMessage());
			return false;
		} catch (Exception ex) {
			LogManager.getInstance().error("MetrixAuthenticationAssistant.encodeUrlParam", ex.getMessage());
			return false;
		}
	}	
}