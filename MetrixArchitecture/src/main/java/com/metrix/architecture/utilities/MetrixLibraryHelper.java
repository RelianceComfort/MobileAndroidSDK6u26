package com.metrix.architecture.utilities;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import java.util.List;

public class MetrixLibraryHelper {
	public static boolean googleMapsIsInstalled(Context context) {
		String libraryName = "com.google.android.maps";
		if(Build.VERSION.SDK_INT >= 30)
			libraryName = "com.google.android.apps.maps";

		return sharedLibraryIsInstalled(context, libraryName);
	}
	
	public static boolean sharedLibraryIsInstalled(Context context, String libraryName) {
		boolean hasLibraryInstalled = false;
		if (!TextUtils.isEmpty(libraryName)) {
			if (Build.VERSION.SDK_INT >= 30) {
				final PackageManager packageManager = context.getPackageManager();
				List<PackageInfo> info = packageManager.getInstalledPackages(0);
				if (info == null || info.isEmpty()) {
					hasLibraryInstalled = false;
					return hasLibraryInstalled; 
				}
				for (int i = 0; i < info.size(); i++) {
					if (info.get(i).packageName.contains(libraryName)) {
						hasLibraryInstalled = true;
						break;
					}
				}
			}
			else {
				String[] installedLibraries = context.getPackageManager().getSystemSharedLibraryNames();

				if (installedLibraries != null) {
					for (String s : installedLibraries) {
						if (libraryName.equals(s)) {
							hasLibraryInstalled = true;
							break;
						}
					}
				}
			}
		}

		return hasLibraryInstalled;
	}
}
