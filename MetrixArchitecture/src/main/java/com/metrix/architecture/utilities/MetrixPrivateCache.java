package com.metrix.architecture.utilities;

import java.util.HashMap;
import java.util.Map;

//import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.constants.MetrixCacheType;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
//import com.metrix.architecture.database.MetrixDatabaseManager;
//import com.metrix.architecture.database.MobileApplication;
//import com.metrix.architecture.utilities.Global.UploadType;
import com.metrix.architecture.utilities.Global.UploadType;
import com.metrix.architecture.utilities.LogManager.Level;
import android.content.Context;
//import android.database.DatabaseUtils;

public class MetrixPrivateCache {
	private static MetrixPrivateCache instance;
	public Map<String, Object> cache;
	
	public static MetrixPrivateCache getInstance() {
	    if(instance == null) {
	    	synchronized(MetrixPrivateCache.class) { 
	    		instance = new MetrixPrivateCache();
	        }
	    }
	    
	    return instance;
	}

	private MetrixPrivateCache() {
		this.cache = new HashMap<String, Object>();
	}

	public void addItem(String name, Object value) {
		this.cache.put(name.toLowerCase(), value);
	}

	public Object getItem(String name) {
		if(this.cache.get(name.toLowerCase())==null){
			LogManager.getInstance().debug("The cached object "+name+" does not exist!");
			
			if(SettingsHelper.getLogLevel(getApplicationContext()).compareToIgnoreCase(Level.INFO.toString())==0
					||SettingsHelper.getLogLevel(getApplicationContext()).compareToIgnoreCase(Level.DEBUG.toString())==0)
				MemoryTracker.getMemoryInfo(getApplicationContext());
			
			if(name.compareToIgnoreCase(MetrixCacheType.MetrixUser)==0){
				String personId = SettingsHelper.getActivatedUser(getApplicationContext());
				User.setUser(personId, null);
				LogManager.getInstance().debug("The cached object "+name+" is resset to be used!");
				return this.cache.get(MetrixCacheType.MetrixUser);
			}
			else if(name.compareToIgnoreCase(MetrixCacheType.MetrixLocationManager)==0){
				boolean startLocationManager = MetrixLocationAssistant.startLocationManager(getApplicationContext());
				LogManager.getInstance().debug("The cached object "+name+" is "+startLocationManager+" restarted to be used!");
				return this.cache.get(MetrixCacheType.MetrixLocationManager.toLowerCase());
			}
			else if(name.compareToIgnoreCase(MetrixCacheType.METRIX_QUERY_ADAPTER)==0){
				if(MobileApplication.QueryAdapter!=null) {
					LogManager.getInstance().debug("The cached object "+name+" is retrieved!");
					this.cache.put(name.toLowerCase(), MobileApplication.QueryAdapter);
					return MobileApplication.QueryAdapter;
				}
				else {
					LogManager.getInstance().debug("The cached database adapter in MobileApplication "+name+" is Null!");
					boolean init_finished = SettingsHelper.getBooleanSetting(MobileApplication.getAppContext(), SettingsHelper.INIT_FINISHED);
					
					if(init_finished) {
						resetDatabase(false);
						LogManager.getInstance().debug("Reset database completed.");
					}
				}
			}
			else if(name.compareToIgnoreCase(MetrixCacheType.METRIX_UPDATE_ADAPTER)==0){
				if(MobileApplication.UpdateAdapter!=null) {
					LogManager.getInstance().debug("The cached object "+name+" is retrieved!");	
					this.cache.put(name.toLowerCase(), MobileApplication.UpdateAdapter);
					return MobileApplication.UpdateAdapter;
				}
				else {
					LogManager.getInstance().debug("The cached database adapter in MobileApplication "+name+" is Null!");
					boolean init_finished = SettingsHelper.getBooleanSetting(MobileApplication.getAppContext(), SettingsHelper.INIT_FINISHED);
					
					if(init_finished) {
						resetDatabase(false);
						LogManager.getInstance().debug("Reset database completed.");
					}
				}				
			}
			else if(name.compareToIgnoreCase(MetrixCacheType.METRIX_INIT_ADAPTER)==0){
				if(MobileApplication.InitAdapter!=null) {
					LogManager.getInstance().debug("The cached object "+name+" is retrieved!");
					this.cache.put(name.toLowerCase(), MobileApplication.InitAdapter);
					return MobileApplication.InitAdapter;
				}
				else {
					LogManager.getInstance().debug("The cached database adapter in MobileApplication "+name+" is Null!");
					boolean init_finished = SettingsHelper.getBooleanSetting(MobileApplication.getAppContext(), SettingsHelper.INIT_FINISHED);
					
					if(init_finished) {
						resetDatabase(false);
						LogManager.getInstance().debug("Reset database completed.");
					}
				}
			}			
		}
		return this.cache.get(name.toLowerCase());
	}
	
	public static void resetDatabase(){
		resetDatabase(true);
	}
	
	public static void resetDatabase(boolean createTable){
		LogManager.getInstance().debug("Metrix database start reloading ...");
		
//		try {
//			// Make sure database is closed first
//			MetrixDatabaseManager.closeDatabase();
//		} catch (Exception ex) {
//		}

		Context context = MobileApplication.getAppContext();

		int sys_tables = SettingsHelper.getIntegerSetting(MobileApplication.getAppContext(), SettingsHelper.SystemDatabaseId);
		int bus_tables = SettingsHelper.getIntegerSetting(MobileApplication.getAppContext(), SettingsHelper.BusinessDatabaseId);
		String password = SettingsHelper.getStringSetting(MobileApplication.getAppContext(), SettingsHelper.USER_LOGIN_PASSWORD);		
		
		String activatedUser = SettingsHelper.getActivatedUser(context);

		try {
			if(createTable)
				MetrixDatabaseManager.createDatabaseAdapters(context, MetrixApplicationAssistant.getMetaIntValue(context, "DatabaseVersion"), sys_tables, bus_tables);
			else
				MetrixDatabaseManager.createDatabaseAdapters(context, MetrixApplicationAssistant.getMetaIntValue(context, "DatabaseVersion"), 0, 0);
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
		
		LogManager.getInstance().debug("Metrix database adapters reloaded ...");		

		try {
			String sync_type = MobileApplication.getAppParam("SYNC_TYPE_MOBILE_ANDROID");
			if (MetrixStringHelper.isNullOrEmpty(sync_type)) {
				SettingsHelper.saveSyncType(context, UploadType.TRANSACTION_INDEPENDENT.toString());
			} else {
				SettingsHelper.saveSyncType(context, sync_type);
			}
			MetrixPrivateCache.getInstance().addItem("SYNC_TYPE", SettingsHelper.getSyncType(context));
			
			if(createTable)
				MobileApplication.saveTableDefinitionToCache();
			
			User.setUser(activatedUser, context);
		}
		catch(Exception ex){
			LogManager.getInstance().error(ex);
		}
	}
		
	public void removeItem(String name) {
		this.cache.remove(name.toLowerCase());
	}

	public boolean containsKey(String name) {
		return this.cache.containsKey(name.toLowerCase());
	}
	
	public Context getApplicationContext(){
		Context appContext = MobileApplication.getAppContext();
		return appContext;
	}
}
