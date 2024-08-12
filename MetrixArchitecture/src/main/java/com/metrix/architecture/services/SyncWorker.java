package com.metrix.architecture.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.RxWorker;
import androidx.work.WorkerParameters;

import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.Single;

public class SyncWorker extends RxWorker {
    private static final String TAG = "FSM_Sync";
    private static final String REPEAT_INDICATOR = "REPEAT_SYNC";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Single<Result> createWork() {
        final Context context = MobileApplication.getAppContext();
        final String serviceUrl = SettingsHelper.getServiceAddress(context);
        final String user = SettingsHelper.getActivatedUser(context);

        if (SettingsHelper.getSyncPause(context) || MetrixStringHelper.isNullOrEmpty(serviceUrl) || MetrixStringHelper.isNullOrEmpty(user)) {
            Log.i(TAG, "Sync cannot run. Exiting...");
            return Single.just(Result.success()); // Cannot do sync. Return success to indicate that the worker has finished its job
        }

        // TODO: How to ensure the database is in correct state?  Is this actually a problem?

        final MetrixRemoteExecutor remoteExecutor = new MetrixRemoteExecutor(context);
        final int deviceSequence = SettingsHelper.getDeviceSequence(context);
        final MetrixSyncManager syncManager = new MetrixSyncManager(serviceUrl, user, deviceSequence, remoteExecutor, true);

        return Single.fromCallable(syncManager::sync)
                .flatMap(result -> {
                    Log.i(TAG, "Sync cycle completed");
                    // Throw a customised error to indicate that a sync should be repeated
                    if (result == MetrixSyncManager.SyncStatus.KEEP_RUNNING)
                        return Single.error(new IllegalStateException(REPEAT_INDICATOR));
                    else
                        return Single.just(Result.success()); // No need to run sync cycle again.
                })
                .retryWhen(errors -> errors.flatMap(error -> {
                    if (error instanceof IllegalStateException && REPEAT_INDICATOR.equals(error.getMessage())) {
                        Log.i(TAG, "Sync repeat requested...");
                        return Flowable.timer(5, TimeUnit.SECONDS)
                                .flatMap(l -> Flowable.just(new Object())); // Repeat after 5 seconds
                    } else {
                        Log.i(TAG, "Error encountered propagating...");
                        return Flowable.error(error); // Error is something else. Handle down the stream
                    }
                }))
                .onErrorReturn((error) -> {
                    Log.i(TAG, "Error occurred. Failing...");
                    return Result.failure();
                });
    }
}
