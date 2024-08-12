package com.metrix.architecture.services;

import com.metrix.architecture.services.IPostListener;

/**
 * Add the interface to operate callback listener so that the UI thread can
 * add/remove listener when bind the service
 * 
 * @author elin
 * 
 */
public interface IPostMonitor {
	/**
	 * add callback listener
	 * 
	 * @param callback
	 */
	void registerListener(IPostListener callback);

	/**
	 * Remove callback listener
	 * 
	 * @param callback
	 */
	void removeListener(IPostListener callback);
}