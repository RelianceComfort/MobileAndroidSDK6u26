package com.metrix.architecture.utilities;

import android.content.Context;
//import android.database.DatabaseUtils;

public class MetrixPublicCache {
	public static MetrixPrivateCache instance = MetrixPrivateCache.getInstance();

	private MetrixPublicCache() {
	}

	@SuppressWarnings("static-access")
	public void addItem(String name, Object value) {
		this.instance.cache.put(name.toLowerCase(), value);
	}

	@SuppressWarnings("static-access")
	public Object getItem(String name) {
		return this.instance.cache.get(name.toLowerCase());
	}
		
	@SuppressWarnings("static-access")
	public void removeItem(String name) {
		this.instance.cache.remove(name.toLowerCase());
	}

	@SuppressWarnings("static-access")
	public boolean containsKey(String name) {
		return this.instance.cache.containsKey(name.toLowerCase());
	}
	
	/**
	 * This method returns the application context.
	 * @return The application context.
	 * @since 5.6
	 */
	public Context getApplicationContext(){
		Context appContext = (Context)getItem(Global.MobileApplication);
		
		if(appContext == null) {
			try {
				LogManager.getInstance().error("Application context is null!");
				throw new Exception("Application context is null!");
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		}
		
		return appContext;
	}
}
