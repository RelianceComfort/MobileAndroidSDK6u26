package com.metrix.architecture.services;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;

/**
 * The class to acquire thread information and do sleep etc.
 * 
 * @author elin
 * 
 */
public class ThreadHelper {
	/**
	 * Get current thread ID
	 * 
	 * @return
	 */
	public static long getThreadId() {
		Thread t = Thread.currentThread();
		return t.getId();
	}

	/**
	 * Get thread signature
	 * 
	 * @return
	 */
	public static String getThreadSignature() {
		Thread t = Thread.currentThread();
		long l = t.getId();
		String name = t.getName();
		long p = t.getPriority();
		String gname = t.getThreadGroup().getName();
		return AndroidResourceHelper.getMessage("ThreadSignature4Args", name, l, p, gname);
	}

	/**
	 * Log thread signature
	 * 
	 * @param tag
	 */
	public static void logThreadSignature(String tag) {
		LogManager.getInstance().debug(tag + " " + getThreadSignature());
	}

	/**
	 * Make the thread sleep for specified seconds
	 * 
	 * @param secs
	 */
	public static void sleepForInSecs(int secs) {
		try {
			Thread.sleep(secs * 1000);
		} catch (InterruptedException x) {
			throw new RuntimeException(AndroidResourceHelper.getMessage("InterruptedLCase"), x);
		}
	}
}
