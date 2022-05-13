package com.example.nisim.messengerapp.services;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.nisim.messengerapp.R;
import com.example.nisim.messengerapp.classes.Helper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.List;

public class ActivityCheckService extends Service {

    ActivityCheckThread activityCheckThread;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (activityCheckThread == null) {
            activityCheckThread = new ActivityCheckThread();
            activityCheckThread.start();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activityCheckThread = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class ActivityCheckThread extends Thread {
        @Override
        public void run() {
            super.run();

            while (true) {
                try {
                    updateActivity(Helper.isAppOnForeground(getApplicationContext()));
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void updateActivity(boolean active) {
            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

            if (firebaseUser != null) {
                DatabaseReference databaseReference = firebaseDatabase.getReference("Users");
                databaseReference.child(firebaseUser.getUid()).child("active").setValue(active);
                if (active) {
                    databaseReference.child(firebaseUser.getUid()).child("lastSeen").setValue(ServerValue.TIMESTAMP);
                }
            }
        }
    }
}