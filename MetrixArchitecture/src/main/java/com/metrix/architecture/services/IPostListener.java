package com.metrix.architecture.services;

import com.metrix.architecture.utilities.Global.ActivityType;

public interface IPostListener {
	void newSyncStatus(ActivityType activityType, String message);
}