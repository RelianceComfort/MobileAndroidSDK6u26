package com.metrix.architecture.notification;

import androidx.annotation.NonNull;

import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class PushRegistrationManager {
    public void initNotifications() {
        if (FSMNotificationAssistant.pushNotificationIsEnabled()) {
            final Executor executor = Executors.newSingleThreadExecutor();
            PushRegistrationClient.getFcmToken(executor)
                    .flatMap((token) -> {
                        registerFcmToken(token);
                        return Single.just(token);
                    })
                    .subscribeOn(Schedulers.from(executor))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DisposableSingleObserver<String>() {
                        @Override
                        public void onSuccess(String s) {
                            dispose();
                        }

                        @Override
                        public void onError(Throwable e) {
                            LogManager.getInstance().error(e);
                            dispose();
                        }
                    });
        }
    }

    private void registerFcmToken(@NonNull String token) {
        String personId = SettingsHelper.getActivatedUser(MobileApplication.getAppContext());
        int deviceSequence = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());
        if (!MetrixStringHelper.isNullOrEmpty(personId) && deviceSequence > 0) {
            // We have an activated mobile client and a token to check, so proceed
            PushRegistrationClient pushClient = new PushRegistrationClient();
            pushClient.register(token);
        }
    }
}
