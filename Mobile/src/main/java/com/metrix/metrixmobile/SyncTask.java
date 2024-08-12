package com.metrix.metrixmobile;

import android.os.AsyncTask;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.services.MetrixSyncManager;
import com.metrix.architecture.services.ThreadHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.SettingsHelper;

/**
 * Inherit AsyncTask for manually syncing the Mobile Service in Background task
 */
public class SyncTask extends AsyncTask<MetrixSyncManager, Integer, Boolean>
{
	private int maxInteration = 0;
	
	public SyncTask() { 
	}
	
	public SyncTask(int interation) {
		maxInteration = interation; 
	}
	
    /* (non-Javadoc)
     * @see android.os.AsyncTask#doInBackground(Params[])
     * run intensive processes here
     * notice that the datatype of the first param in the class definition matches the param passed to this method 
     * and that the datatype of the last param in the class definition matches the return type of this mehtod
     */
    @Override
    protected Boolean doInBackground( MetrixSyncManager ... params ) 
    {
    	// It will try 3 times to do a force sync 
    	for(int i=0; i< 3; i++){
			if(params[0].getIsSyncRunning()== false){
				boolean pauseSync = SettingsHelper.getSyncPause(MetrixPublicCache.instance.getApplicationContext());
				
				try {										
					if(!pauseSync) {
						MobileApplication.stopSync(MetrixPublicCache.instance.getApplicationContext());
						
						params[0].sync(); // first time to send message to Mobile Service to process.
						int syncInterval = SettingsHelper.getSyncInterval(MetrixPublicCache.instance.getApplicationContext());
						
						if(syncInterval >= 30) {
							syncInterval = 30;							
						}
						
						int interval = 10; // retry sync every 10 seconds 
						int numberOfRetry = syncInterval/interval; 
						
						if(numberOfRetry > maxInteration)
							numberOfRetry = maxInteration; 
						
						for(int k=0; k<numberOfRetry; k++) {
							ThreadHelper.sleepForInSecs(interval); 
							params[0].sync(); // this sync is happening to receive the message processed by M5 if it is not fast enough to get first time.
						}
					}
					return true;
				}
				catch(Exception ex){
					LogManager.getInstance().error(ex);
				}	
				finally {
					if(!pauseSync)
						MobileApplication.resumeSync(MetrixPublicCache.instance.getApplicationContext());
				}
			}
			else {
				ThreadHelper.sleepForInSecs(5);
			}
    	}
		return false; 
    }
    
    /* (non-Javadoc)
     * @see android.os.AsyncTask#onPreExecute()
     * gets called just before thread begins
     */
    @Override
    protected void onPreExecute() {
    	LogManager.getInstance().info("com.metrix.metrixmobile.onPreExecute()" );
        super.onPreExecute();   
    }
    
    /* (non-Javadoc)
     * @see android.os.AsyncTask#onProgressUpdate(Progress[])
     * called from the publish progress 
     * notice that the datatype of the second param gets passed to this method
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
    	super.onProgressUpdate(values);
    	LogManager.getInstance().info("com.metrix.metrixmobile.onProgressUpdate(): " +  String.valueOf( values[0] ) );
    }
    
    /* called if the cancel button is pressed
     * (non-Javadoc)
     * @see android.os.AsyncTask#onCancelled()
     */
    @Override
    protected void onCancelled()
    {
            super.onCancelled();
    }

    /*
     *  called as soon as doInBackground method completes(non-Javadoc)
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     *  notice that the third param gets passed to this method
     */  
    @Override
    protected void onPostExecute( Boolean result) {
    	super.onPostExecute(result);
    	LogManager.getInstance().info("com.metrix.metrixmobile.onPostExecute(): " + result );
    }
}    		