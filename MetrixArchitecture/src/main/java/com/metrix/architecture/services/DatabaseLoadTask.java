package com.metrix.architecture.services;

import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixPublicCache;
import android.os.AsyncTask;

public class DatabaseLoadTask extends AsyncTask<MetrixSyncManager, Integer, Boolean> {
	public DatabaseLoadTask() { 
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
    	MetrixSyncManager syncManager = params[0];		
				
		try {					
			// Temporally disallow the sync since the database is reloading. 
			syncManager.mAllowSync = false;
            MetrixPublicCache.instance.addItem("EnableSyncProcess", false);
			MetrixPublicCache.instance.addItem("DatabaseDownloaded", "N");

			MetrixAttachmentReceiver.processDatabase();
			return true;
		}
		catch(Exception ex){
			LogManager.getInstance().error(ex);
		}	
		finally {
			// allow sync after the database is reloaded
			syncManager.mAllowSync = true;
            MetrixPublicCache.instance.addItem("EnableSyncProcess", true);
		}			
    	
		return true; 
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
    	LogManager.getInstance().info("com.metrix.metrixmobile.onProgressUpdate(): " + String.valueOf( values[0] ) );
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
