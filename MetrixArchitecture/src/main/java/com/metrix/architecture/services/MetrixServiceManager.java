package com.metrix.architecture.services;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;

import android.content.Context;
import android.os.PowerManager;

/*
 * 
 * What is a MetrixServiceManager
 * *******************************************************************
 * like a service Manager that leads the caller to the service
 * it will start the weak lock when service get started and queued
 * The last one to finish the service will release the lock.
 * 
 * The manager does not know that the client is scheduled to come,
 * might destroy the service and release the lock.
 * 
 */
public class MetrixServiceManager {
	// debugging tag
	private static String tag = "MetrixMetrixServiceManager";

	// *************************************************
	// * A Public static interface
	// * static members: Purely helper methods
	// * Delegates to the underlying singleton object
	// *************************************************
	public static void setup(Context inCtx) {
		if (mMetrixServiceManager == null) {
			LogManager.getInstance().debug(MetrixServiceManager.tag + 
					" Creating service wrapper and start the weak lock");
			mMetrixServiceManager = new MetrixServiceManager(inCtx);
			mMetrixServiceManager.initWork();
		}
	}

	public static boolean isSetup() {
		return (mMetrixServiceManager != null) ? true : false;
	}

	public static int enterService() {
		assertSetup();
		return mMetrixServiceManager.increaseCounter();
	}

	public static int leaveService() {
		assertSetup();
		return mMetrixServiceManager.decreaseCounter();
	}

	public static void registerServiceClient() {
		assertSetup();
		mMetrixServiceManager.registerClient();
		return;
	}

	public static void unregisterServiceClient() {
		assertSetup();
		mMetrixServiceManager.unregisterClient();
		return;
	}

	private static void assertSetup() {
		if (MetrixServiceManager.mMetrixServiceManager == null) {
			LogManager.getInstance().warn(MetrixServiceManager.tag + " You need to call setup first");
			throw new RuntimeException(AndroidResourceHelper.getMessage("YouNeedToSetupFirst1Args", "MetrixServiceManager"));
		}
	}

	// *************************************************
	// * A pure private implementation
	// *************************************************

	// Keep count of service callers to know the last caller
	// On destroy set the count to zero to clean the resource.
	private int mCount;

	// Needed to create the wake lock
	// Our switch
	PowerManager.WakeLock mWakeLock = null;

	// Multi-client support
	private int mClientCount = 0;

	/*
	 * This is expected to be a singleton. One could potentially make the
	 * constructor private I suppose.
	 */
	private MetrixServiceManager(Context inContext) {
		mWakeLock = this.createWakeLock(inContext);
	}

	/*
	 * Setting up the service caller using a static method. This has to be
	 * called before calling any other methods. what it does: 1. Instantiate the
	 * object 2. acquire the lock to do the work Assumption: It is not required
	 * to be synchronized because it will be called from the main thread. (Could
	 * be wrong. need to validate this!!)
	 */
	private static MetrixServiceManager mMetrixServiceManager = null;

	/*
	 * The methods "enter" and "leave" are expected to be called in tandem.
	 * 
	 * On "enter" increment the count. Just increment the count to know when the
	 * last caller leaves.
	 * 
	 * This is a synchronized method as multiple threads will be entering and
	 * leaving.
	 */
	synchronized private int increaseCounter() {
		mCount++;
		LogManager.getInstance().debug("A new service caller: count:" + mCount);
		return mCount;
	}

	/*
	 * The methods "enter" and "leave" are expected to be called in tandem.
	 * 
	 * On "leave" decrement the count.
	 * 
	 * If the count reaches zero release lock.
	 * 
	 * This is a synchronized method as multiple threads will be entering and
	 * leaving.
	 */
	synchronized private int decreaseCounter() {
		LogManager.getInstance().debug(tag + " Caller finished the service call: count at the call:"
				+ mCount);
		// if the count is already zero
		// just leave.
		if (mCount == 0) {
			LogManager.getInstance().warn(tag + " Count is zero.");
			return mCount;
		}
		mCount--;
		if (mCount == 0) {
			// Last caller and release lock
			offWork();
		}
		return mCount;
	}

	/*
	 * acquire the wake lock to start the work it is up to other synchronized
	 * methods to call this at the appropriate time.
	 */
	private void initWork() {
		LogManager.getInstance().debug(tag + " Start weak lock. Count:" + mCount);
		this.mWakeLock.acquire();
	}

	/*
	 * Release the wake lock to finish calling the service. it is up to other
	 * synchronized methods to call this at the appropriate time.
	 */
	private void offWork() {
		if (this.mWakeLock.isHeld()) {
			LogManager.getInstance().debug(tag + " Releasing wake lock. No more callers");
			this.mWakeLock.release();
		}
	}

	/*
	 * Standard code to create a partial wake lock
	 */
	private PowerManager.WakeLock createWakeLock(Context inCtx) {
		PowerManager pm = (PowerManager) inCtx
				.getSystemService(Context.POWER_SERVICE);

		PowerManager.WakeLock wl = pm.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, tag);
		return wl;
	}

	private int registerClient() {
		ThreadHelper.logThreadSignature(tag);
		this.mClientCount++;
		LogManager.getInstance().debug(tag + " registering a new client:count:" + mClientCount);
		return mClientCount;
	}

	private int unregisterClient() {
		ThreadHelper.logThreadSignature(tag);
		LogManager.getInstance().debug(tag + " unregistering a client:count:" + mClientCount);
		if (mClientCount == 0) {
			LogManager.getInstance().warn(tag + " There are no clients to unregister.");
			return 0;
		}
		// clientCount is not zero
		mClientCount--;
		if (mClientCount == 0) {
			cleanResource();
		}
		return mClientCount;
	}

	/**
	 * Release the wake lock and reset the counter
	 */
	synchronized private void cleanResource() {
		LogManager.getInstance().debug(tag + " All caller finished the service calls.");
		mCount = 0;
		this.offWork();
	}
}
