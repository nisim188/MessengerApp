package com.example.nisim.messengerapp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.example.nisim.messengerapp.classes.Helper;

public class BroadcastReceiverInternet extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Helper.isOnline(context)) {
            Toast.makeText(context, "Internet connection is now available.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "No internet connection.", Toast.LENGTH_SHORT).show();
        }
    }
}
