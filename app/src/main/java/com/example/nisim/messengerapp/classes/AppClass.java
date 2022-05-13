package com.example.nisim.messengerapp.classes;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;

import com.example.nisim.messengerapp.activities.MainActivity;
import com.example.nisim.messengerapp.services.ActivityCheckService;
import com.example.nisim.messengerapp.services.MessageNotificationService;

import java.util.ArrayList;

public class AppClass extends Application {
    private Activity currentActivity;
    private ArrayList<Integer> notificationUIDs = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks();

        startService(new Intent(this, ActivityCheckService.class));
        startService(new Intent(this, MessageNotificationService.class));
    }

    private void registerActivityLifecycleCallbacks() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                currentActivity = activity;

                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                for (int UID : notificationUIDs) {
                    notificationManager.cancel(UID);
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                currentActivity = null;
            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public void addNotificationUID(int UID) {
        this.notificationUIDs.add(UID);
    }
}
