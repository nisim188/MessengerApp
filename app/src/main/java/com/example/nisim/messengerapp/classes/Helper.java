package com.example.nisim.messengerapp.classes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Pair;

import com.example.nisim.messengerapp.R;
import com.example.nisim.messengerapp.activities.ChatActivity;
import com.example.nisim.messengerapp.activities.MainActivity;
import com.example.nisim.messengerapp.activities.PermissionsActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Helper {
    //converts a phone number to global format, adding +972 instead of 0.
    public static String addAreaCode(String phoneNumber) {
        String startString = phoneNumber.substring(0, 4);
        if (startString.equals("+972")) //already converted
        {
            return phoneNumber;
        }

        String fixedPhoneNumber;
        String areaCode = "+972";
        fixedPhoneNumber = areaCode + phoneNumber.substring(1); //substring used to remove the "0" digit

        return fixedPhoneNumber;
    }

    //converts a phone number to local format, adding 0 instead of +972.
    public static String removeAreaCode(String phoneNumber) {
        String startString = phoneNumber.substring(0, 4);
        if (!startString.equals("+972")) //already converted
        {
            return phoneNumber;
        }

        String fixedPhoneNumber;
        fixedPhoneNumber = "0" + phoneNumber.substring(4); //substring used to remove the "+972"

        return fixedPhoneNumber;
    }

    //get user phone contacts
    public static ArrayList<Pair<String, String>> getContacts(Context context) {
        ArrayList<Pair<String, String>> contacts = new ArrayList<>();
        Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (phones.moveToNext()) {
            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            contacts.add(new Pair<>(name, phoneNumber));
        }
        phones.close();

        return contacts;
    }

    //minimizing a text message according to limitation
    public static String minimizeMessage(String message, int maxLength, boolean lastMessageMine) {
        if (lastMessageMine) {
            message = "You: " + message;
        }

        if (message.length() > maxLength) {
            return message.substring(0, maxLength).trim() + "...";
        }

        return message;
    }

    //returns array with the missing permissions
    public static String[] getMissingPermissions(Context context, ArrayList<String> neededPermissions) {
        ArrayList<String> permissionsMissing = new ArrayList<>();

        //iterate all permissions
        for (String permission : neededPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsMissing.add(permission);
            }
        }

        //return missing permissions
        String[] permissionsArray = permissionsMissing.toArray(new String[0]);
        if (permissionsArray.length > 0) {
            return permissionsArray;
        } else {
            return null;
        }
    }

    //returns if needed permissions are missing
    private static boolean isMissingPermissions(Context context) {
        return getMissingPermissions(context, getApplicationNeededPermissions()) != null;
    }

    //returns application needed permissions
    public static ArrayList<String> getApplicationNeededPermissions() {
        ArrayList<String> neededPermissions = new ArrayList<>();
        neededPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        neededPermissions.add(Manifest.permission.READ_CONTACTS);
        neededPermissions.add(Manifest.permission.INTERNET);
        neededPermissions.add(Manifest.permission.CAMERA);
        return neededPermissions;
    }

    public static void permissionCheck(Context context) {
        if (isMissingPermissions(context)) {
            Intent intent = new Intent(context, PermissionsActivity.class);
            context.startActivity(intent);
        }
    }

    public static String getLastSeen(long lastSeen) {
        Date messageTime = new Date(lastSeen);
        Date currentTime = Calendar.getInstance().getTime();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(messageTime);
        int messageDay = calendar.get(Calendar.DATE);
        calendar.setTime(currentTime);
        int currentDay = calendar.get(Calendar.DATE);

        if (messageDay == currentDay) { //messages from today
            @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("HH:mm");
            return "at " + df.format(messageTime);
        } else {
            @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            return "on " + df.format(messageTime);
        }
    }

    public static String getContactName(Context context, User user) {
        String name = user.getName(); //default name

        //check if in contacts, to show his contact name instead
        for (Pair<String, String> contact : Helper.getContacts(context)) {
            if (Helper.addAreaCode(contact.second).equals(user.getPhone())) //user in contacts
            {
                name = contact.first;
                break;
            }
        }

        return name;
    }

    public static boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }

        String packageName = context.getString(R.string.packageName);
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(packageName) && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}
