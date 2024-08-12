package com.metrix.architecture.utilities;

import com.metrix.architecture.database.MobileApplication;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.os.Process;
import android.widget.Toast;

/**
 * This class can be used to track the total memory being used by the application
 * throughout it's lifetime or in response to specific events. This can be useful
 * when tracking down memory leaks and unexpectedly heavy levels of memory
 * consumption.
 * 
 * @since 5.6
 */
public class MemoryTracker {
	public static void getMemoryInfo(Context context){
		MemoryInfo mi = new MemoryInfo(); 
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(mi);
		long availableMegs = mi.availMem / 1048576L;		
		
		LogManager.getInstance().info("Available memory is "+availableMegs+"meg and memory low is "+mi.lowMemory);
	}
	
	// previously used at MetrixActivity/MetrixDesignerActivity's onStart to have running tally of stats
	public static void trackMemoryStats(Context context) {
		ActivityManager am = (ActivityManager)context.getSystemService(Activity.ACTIVITY_SERVICE);
    	long totalHeapSize = am.getMemoryClass() * 1024;
    	int processID = Process.myPid();
    	android.os.Debug.MemoryInfo mi = am.getProcessMemoryInfo(new int[] {processID})[0];
    	long totalSetAside = mi.dalvikPss;
    	long remainingGap = totalHeapSize - totalSetAside;
    	long delta = totalSetAside - MobileApplication.mLastMemoryCount; 	
    	int activitiesCount = -500;		// signify that we aren't counting activities
    	
    	/*
    	// START BLOCK
    	// only uncomment this BLOCK if you've *TEMPORARILY* added the following permission to the manifest:
    	// <uses-permission android:name="android.permission.GET_TASKS" />
    	List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(10);
    	for (ActivityManager.RunningTaskInfo info : tasks) {
    		String pkgName = info.baseActivity.getPackageName();
    		if (MetrixStringHelper.valueIsEqual(pkgName, "com.metrix.metrixmobile")) {
    			activitiesCount = info.numActivities;
    			break;
    		}
    	}	
    	// END BLOCK
    	*/
    	
    	String toastString = AndroidResourceHelper.getMessage("LimCurrPrevDelRem6Args",
    			totalHeapSize, totalSetAside, MobileApplication.mLastMemoryCount, delta, remainingGap, activitiesCount);
    	LogManager.getInstance().info(toastString);
    	Toast.makeText(context, toastString, Toast.LENGTH_LONG).show();
    	MobileApplication.mLastMemoryCount = totalSetAside;
	}
}
