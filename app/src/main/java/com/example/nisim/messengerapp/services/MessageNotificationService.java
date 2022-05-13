package com.example.nisim.messengerapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.nisim.messengerapp.R;
import com.example.nisim.messengerapp.activities.MainActivity;
import com.example.nisim.messengerapp.classes.AppClass;
import com.example.nisim.messengerapp.classes.DataHelper;
import com.example.nisim.messengerapp.classes.Helper;
import com.example.nisim.messengerapp.classes.Message;
import com.example.nisim.messengerapp.classes.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class MessageNotificationService extends Service {

    MessageNotificationThread messageNotificationThread;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (messageNotificationThread == null) {
            messageNotificationThread = new MessageNotificationThread();
            messageNotificationThread.start();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        messageNotificationThread = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class MessageNotificationThread extends Thread {
        FirebaseUser firebaseUser;
        FirebaseDatabase firebaseDatabase;
        DatabaseReference databaseReference;
        Map<String, User> usersArrayList;
        ArrayList<Message> messagesArrayList;

        @Override
        public void run() {
            super.run();

            firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            firebaseDatabase = FirebaseDatabase.getInstance();
            databaseReference = firebaseDatabase.getReference();
            usersArrayList = DataHelper.loadUsersFromInternalStorage(getApplicationContext());
            messagesArrayList = DataHelper.loadMessagesFromInternalStorage(getApplicationContext());

            databaseReference.child("Messages").addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    if (firebaseUser != null) {
                        handleNewMessage(dataSnapshot);
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        private void handleNewMessage(DataSnapshot dataSnapshot) {
            final Message msg = dataSnapshot.getValue(Message.class);
            if (messagesArrayList.contains(msg)) { //prevents bug of loading all of the messages every time app opens
                return;
            }

            if (msg.getToUserUid().equals(firebaseUser.getUid())) {
                if (!isInChat(msg.getFromUserUid())) {
                    if (usersArrayList.containsKey(msg.getFromUserUid())) {
                        handleNotification(msg, usersArrayList.get(msg.getFromUserUid()));
                    } else {
                        databaseReference.child("Users").child(msg.getFromUserUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                User user = dataSnapshot.getValue(User.class);
                                usersArrayList.put(user.getUid(), user);
                                handleNotification(msg, user);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }
        }

        private void handleNotification(Message msg, User chatPartner) {
            pushMessageNotification(chatPartner, msg.getMessage());
        }

        private void pushMessageNotification(User chatPartner, String message) {
            Context context = getApplicationContext();
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "M_CH_ID");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelId = "MessagePushChannelId";
                NotificationChannel channel = new NotificationChannel(channelId, "A new message has been received.", NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
                builder.setChannelId(channelId);
            }

            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setContentIntent(pendingIntent)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_designed_icon)
                    .setContentTitle(Helper.getContactName(getApplicationContext(), chatPartner))
                    .setContentText(message)
                    .setSound(soundUri)
                    .setAutoCancel(true);

            Notification notification = builder.build();


            if (notificationManager != null) {
                int UID = UUID.randomUUID().hashCode();
                ((AppClass) getApplication()).addNotificationUID(UID);
                notificationManager.notify(UID, notification);
            }
        }

        private boolean isInChat(String chatPartnerUid) {
            if (Helper.isAppOnForeground(getApplicationContext())) {
                Bundle bundle = ((AppClass) getApplication()).getCurrentActivity().getIntent().getExtras();
                if (bundle != null) {
                    String json = bundle.getString("chatPartner");
                    if (json != null) {
                        User chatPartner = (User) DataHelper.JsonToObject(json, "User");
                        return chatPartner.getUid().equals(chatPartnerUid);
                    }
                    return false;
                }
                return false;
            }
            return false;
        }
    }
}
