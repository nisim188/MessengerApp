package com.example.nisim.messengerapp.activities;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.nisim.messengerapp.R;
import com.example.nisim.messengerapp.classes.DataHelper;
import com.example.nisim.messengerapp.classes.GlideApp;
import com.example.nisim.messengerapp.classes.Helper;
import com.example.nisim.messengerapp.classes.Message;
import com.example.nisim.messengerapp.classes.PhotoHelper;
import com.example.nisim.messengerapp.classes.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener, View.OnScrollChangeListener {

    final int MESSAGES_FIRST_LOAD_AMOUNT = 40;
    final int MESSAGES_CONTINUAL_LOAD_AMOUNT = 15;
    final double MESSAGE_HEIGHT_DPS = 38.5;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    ArrayList<Message> messagesArrayList = new ArrayList<>();
    ArrayList<Message> conversationArrayList = new ArrayList<>();
    ArrayList<UIMessage> pendingMessages = new ArrayList<>();
    boolean lastSenderSelf = false, first = true, chatPartnerActive = false;
    int pendingMessagesDrawnCount = 0, loadMoreMessagesActiveCount = 0, scrollUpAccumulative = 0, totalMessagesLoaded = 0;
    long chatPartnerLastSeen = -1;
    User chatPartner;
    Handler handler;

    FloatingActionButton btnSendMessage, btnBack, btnScrollDown;
    LinearLayout llMessages;
    ScrollView svMessages;
    TextView tvUserName, tvStatus;
    EditText etMessage;
    ImageView ivProfilePhoto;

    //scrolls the ScrollView to bottom
    Runnable scrollDown = new Runnable() {
        @Override
        public void run() {
            svMessages.fullScroll(ScrollView.FOCUS_DOWN);
            scrollUpAccumulative = 0;
        }
    };

    //loads earlier messages in the chat
    Runnable loadMoreMessages = new Runnable() {
        @Override
        public void run() {
            if (pendingMessages.size() > 0) //not all of the messages loaded yet
            {
                int loadAmount = Math.min(pendingMessages.size(), MESSAGES_CONTINUAL_LOAD_AMOUNT);
                loadMoreMessagesActiveCount = loadAmount;
                loadMessagesLimited(loadAmount, true);
                scrollUpAccumulative -= MESSAGES_CONTINUAL_LOAD_AMOUNT / 2;
                svMessages.scrollBy(0, PhotoHelper.dpsToPixels(getApplicationContext(), MESSAGE_HEIGHT_DPS) * loadAmount);
            }
        }
    };

    //checks if chat partner has been active in the last 10 seconds and updates the firebase.
    //this will make sure that if the partner's activity service was shut down unexpectedly (for example, ran out of battery)
    //the activity status will not be wrong, and the firebase will be immediately updated.
    Runnable activityAssurance = new Runnable() {
        @Override
        public void run() {
            if ((System.currentTimeMillis() - chatPartnerLastSeen) > 10 * 1000 && chatPartnerLastSeen != -1) {
                chatPartnerActive = false;
                tvStatus.setText("Last seen " + Helper.getLastSeen(chatPartnerLastSeen));
            }
            handler.postDelayed(activityAssurance, 10 * 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //permissions check
        Helper.permissionCheck(this);

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        databaseReference = firebaseDatabase.getReference();
        handler = new Handler();

        //set the background to the windows itself, to prevent changes of it by the swift keyboard
        getWindow().setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.bg, null));

        //references
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvUserName = findViewById(R.id.tvUserName);
        tvStatus = findViewById(R.id.tvStatus);
        etMessage = findViewById(R.id.etMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnBack = findViewById(R.id.btnBack);
        btnScrollDown = findViewById(R.id.btnScrollDown);
        llMessages = findViewById(R.id.llMessages);
        svMessages = findViewById(R.id.svMessages);
        btnSendMessage.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        btnScrollDown.setOnClickListener(this);
        etMessage.setOnClickListener(this);
        svMessages.setOnScrollChangeListener(this);
        //todo MIN API 23

        //add a listener that will get data from firebase
        ValueEventListener eventListenerMessages = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //get dataSnapshot of whole database, and not only Messages
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        retrieveMessagesFromFirebase(dataSnapshot);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        //add a listener that will get data from firebase
        ValueEventListener eventListenerActivity = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                updateActivityStatus(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        //get current chat data
        String json = getIntent().getExtras().getString("chatPartner");
        chatPartner = (User) DataHelper.JsonToObject(json, "User");
        updateToolbarUI(chatPartner);

        //first synchronization with Firebase & Storage
        retrieveMessagesFromStorage();

        //constant synchronization with Firebase (Messages)
        databaseReference.child("Messages").addValueEventListener(eventListenerMessages);

        //constant synchronization with Firebase (Activity status)
        databaseReference.child("Users").child(chatPartner.getUid()).addValueEventListener(eventListenerActivity);

        //activate activity assurance checks
        handler.post(activityAssurance);
    }

    @Override
    public void onClick(View view) {
        if (view == btnSendMessage) {
            sendMessage();
        } else if (view == btnBack) {
            goBack();
        } else if (view == etMessage) {
            if (svMessages.getChildAt(0).getBottom() - PhotoHelper.dpsToPixels(this, 50) <= (svMessages.getHeight() + svMessages.getScrollY())) {
                //wait the softKeyboard to show before scrolling down
                handler.postDelayed(scrollDown, 250);
            }
        } else if (view == btnScrollDown) {
            handler.post(scrollDown);
        }
    }

    //go back to the activity you came from
    private void goBack() {
        switch (getIntent().getExtras().getString("senderActivity")) {
            case "MainActivity":
                finish();
                break;
            case "ContactsActivity":
                getIntent().putExtra("finish", true);
                setResult(RESULT_OK, getIntent());
                finish();
                break;
        }
    }

    //get messages from storage and update UI
    private void retrieveMessagesFromStorage() {
        messagesArrayList = DataHelper.loadMessagesFromInternalStorage(this);
        conversationArrayList = getConversation(messagesArrayList);
        updateChatUI(conversationArrayList);

        for (Message msg : conversationArrayList) {
            if (msg.getToUserUid().equals(firebaseUser.getUid()) && !msg.isSeen()) {
                DatabaseReference ref = firebaseDatabase.getReference("Messages").child(msg.getKey()).child("seen");
                ref.setValue(true);
            }
        }
    }

    //get messages from firebase, update UI and save them
    private void retrieveMessagesFromFirebase(DataSnapshot dataSnapshot) {
        messagesArrayList = DataHelper.filterMessages(dataSnapshot);
        ArrayList<Message> updatedConversationArrayList = getConversation(messagesArrayList);

        if (!conversationArrayList.equals(updatedConversationArrayList)) {
            ArrayList<Message> newMessages = new ArrayList<>();
            for (Message message : updatedConversationArrayList) {
                Message tempSeen = new Message(message);
                tempSeen.setSeen(true);
                Message tempUnSeen = new Message(message);
                tempUnSeen.setSeen(false);

                if (!conversationArrayList.contains(tempSeen) && !conversationArrayList.contains(tempUnSeen)) {
                    newMessages.add(message);
                }
            }

            for (Message message : newMessages) {
                conversationArrayList.add(message);
                DataHelper.saveMessagesToInternalStorage(ChatActivity.this, messagesArrayList);
                updateChatUI(message);
            }
        }

        DataHelper.saveMessagesToInternalStorage(ChatActivity.this, messagesArrayList);
    }

    //filter messages according to this conversation only
    private ArrayList<Message> getConversation(ArrayList<Message> messages) {
        ArrayList<Message> conversation = new ArrayList<>();
        for (Message message : messages) {
            if (chatPartner.getUid().equals(message.getFromUserUid()) ||
                    chatPartner.getUid().equals(message.getToUserUid())) {
                conversation.add(message);
            }
        }

        return conversation;
    }

    //update toolbar UI with the given chat partner
    private void updateToolbarUI(User chatPartner) {
        String name = Helper.getContactName(this, chatPartner);

        tvStatus.setVisibility(View.INVISIBLE);
        tvUserName.setText(name);
        if (chatPartner.getPhoto() != null && !isFinishing()) {
            GlideApp.with(this)
                    .load(Uri.parse(chatPartner.getPhoto()))
                    .dontAnimate()
                    .placeholder(R.drawable.ic_no_profile)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true).into(ivProfilePhoto);
        }
    }

    //get messages from firebase, update UI and save them
    private void updateActivityStatus(DataSnapshot dataSnapshot) {
        chatPartnerActive = dataSnapshot.child("active").getValue(Boolean.class);
        chatPartnerLastSeen = dataSnapshot.child("lastSeen").getValue(Long.class);
        tvStatus.setVisibility(View.VISIBLE);

        if (chatPartnerActive && ((System.currentTimeMillis() - chatPartnerLastSeen) < 10 * 1000 || chatPartnerLastSeen == -1)) {
            tvStatus.setText("Active");
        } else {
            tvStatus.setText("Last seen " + Helper.getLastSeen(chatPartnerLastSeen));
        }
    }

    //update chat UI with the given message (from firebase/self)
    private void updateChatUI(Message message) {
        boolean currentSenderSelf;
        boolean senderSwitch = false;

        if (chatPartner.getUid().equals(message.getToUserUid())) { //user is sender
            currentSenderSelf = true;
        } else { //user is receiver
            currentSenderSelf = false;
        }

        if (!first) {
            senderSwitch = (currentSenderSelf != lastSenderSelf);
        }
        first = false;

        @SuppressLint("SimpleDateFormat") DateFormat sdf = new SimpleDateFormat("HH:mm");
        String timeStamp = Long.toString(message.getTimestamp());
        String time = sdf.format(Long.valueOf(timeStamp));

        if (timeStamp.equals("0")) {
            try {
                Date date = sdf.parse(message.getTime());
                time = sdf.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        //get current open tasks, in order to check if user currently in current chat, in order to check new messages as read
        ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName runningActivity = activityManager.getRunningTasks(1).get(0).topActivity;
        ComponentName chatActivity = new ComponentName(this, ChatActivity.class);

        if (message.getToUserUid().equals(firebaseUser.getUid()) && chatActivity.equals(runningActivity)) {
            DatabaseReference ref = firebaseDatabase.getReference("Messages").child(message.getKey()).child("seen");
            ref.setValue(true);
        }

        addMessageToUIBelow(currentSenderSelf, senderSwitch, message.getMessage().trim(), time);
        lastSenderSelf = currentSenderSelf;
    }

    //update chat UI with the given messages (from storage)
    private void updateChatUI(ArrayList<Message> messages) {
        boolean currentSenderSelf;
        boolean senderSwitch = false;
        llMessages.removeAllViews();

        for (Message message : messages) {
            if (chatPartner.getUid().equals(message.getToUserUid())) { //user is sender
                currentSenderSelf = true;
            } else { //user is receiver
                currentSenderSelf = false;
            }

            if (!first) {
                senderSwitch = (currentSenderSelf != lastSenderSelf);
            }
            first = false;

            @SuppressLint("SimpleDateFormat") DateFormat sdf = new SimpleDateFormat("HH:mm");
            String timeStamp = Long.toString(message.getTimestamp());
            String time = sdf.format(Long.valueOf(timeStamp));

            if (timeStamp.equals("0")) {
                try {
                    Date date = sdf.parse(message.getTime());
                    time = sdf.format(date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            pendingMessages.add(new UIMessage(currentSenderSelf, senderSwitch, message.getMessage().trim(), time));
            lastSenderSelf = currentSenderSelf;
        }

        loadMessagesLimited(MESSAGES_FIRST_LOAD_AMOUNT, false);
    }

    //load a limited amount of messages, above or below the current messages in UI
    private void loadMessagesLimited(int limit, boolean above) {
        ArrayList<UIMessage> temp = new ArrayList<>(pendingMessages);
        if (pendingMessages.size() > 0) {
            int startIndex = Math.max(pendingMessages.size() - limit, 0);
            int endIndex = pendingMessages.size();
            pendingMessagesDrawnCount = 0;

            for (UIMessage uiMessage : pendingMessages.subList(startIndex, endIndex)) {
                if (above) {
                    addMessageToUIAbove(uiMessage.senderSelf, uiMessage.senderSwitch, uiMessage.message, uiMessage.time);
                } else {
                    addMessageToUIBelow(uiMessage.senderSelf, uiMessage.senderSwitch, uiMessage.message, uiMessage.time);
                }
                temp.remove(uiMessage);
            }
            pendingMessages = temp;
        }
    }

    //add a single message to UI below the other messages
    private void addMessageToUIBelow(boolean senderSelf, boolean senderSwitch, String message, String time) {
        llMessages.addView(addMessageToUI(senderSelf, senderSwitch, message, time));
        handler.postDelayed(scrollDown, 100);
        totalMessagesLoaded++;
    }

    //add a single message to UI above the other messages
    private void addMessageToUIAbove(boolean senderSelf, boolean senderSwitch, String message, String time) {
        llMessages.addView(addMessageToUI(senderSelf, senderSwitch, message, time), pendingMessagesDrawnCount);
        pendingMessagesDrawnCount++;
        loadMoreMessagesActiveCount--;
        totalMessagesLoaded++;
    }

    //get a View of message in UI according to parameters
    private LinearLayout addMessageToUI(boolean senderSelf, boolean senderSwitch, String message, String time) {
        //LinearLayout (container)
        LinearLayout linearLayout = new LinearLayout(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        if (!senderSelf) {
            layoutParams.gravity = Gravity.END;
        }
        if (senderSwitch) {
            layoutParams.setMargins(0, PhotoHelper.dpsToPixels(this, 10), 0, 0);
        } else {
            layoutParams.setMargins(0, PhotoHelper.dpsToPixels(this, 3), 0, 0);
        }
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setPaddingRelative(PhotoHelper.dpsToPixels(this, 9), PhotoHelper.dpsToPixels(this, 7),
                PhotoHelper.dpsToPixels(this, 9), PhotoHelper.dpsToPixels(this, 7));
        linearLayout.setBackgroundResource(R.drawable.rounded_chat_message);
        String color;
        if (senderSelf) {
            color = "#77A0C7AB";
        } else {
            color = "#7798C1B6";
        }
        linearLayout.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(color)));
        //todo MIN API 21

        //TextView - message
        TextView textViewMessage = new TextView(this);
        LinearLayout.LayoutParams layoutParamsMessage = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        textViewMessage.setLayoutParams(layoutParamsMessage);
        textViewMessage.setMaxWidth(PhotoHelper.dpsToPixels(this, 250));
        textViewMessage.setTextColor(Color.parseColor("#323232"));
        textViewMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textViewMessage.setText(message);

        //TextView - time
        TextView textViewTime = new TextView(this);
        LinearLayout.LayoutParams layoutParamsTime = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        layoutParamsTime.gravity = Gravity.BOTTOM;
        textViewTime.setLayoutParams(layoutParamsTime);
        textViewTime.setPaddingRelative(PhotoHelper.dpsToPixels(this, 7), 0, 0, 0);
        textViewTime.setTextColor(Color.parseColor("#696969"));
        textViewTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textViewTime.setText(time);

        linearLayout.addView(textViewMessage);
        linearLayout.addView(textViewTime);

        return linearLayout;
    }

    //send a new message
    private void sendMessage() {
        //prevent sending empty messages
        if (etMessage.getText().toString().trim().length() == 0) {
            etMessage.setText("");
            return;
        }

        //prevent sending message when offline
        if (!Helper.isOnline(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Sending Failed");
            builder.setMessage("You are not connected to the internet.");
            builder.setCancelable(true);
            builder.setNeutralButton("OK", null);
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }

        DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS, dd.MM.yyyy");
        String currentTime = df.format(Calendar.getInstance().getTime());

        Message message = new Message(
                firebaseUser.getUid(),
                chatPartner.getUid(),
                currentTime,
                etMessage.getText().toString().trim(),
                "text"
        );

        messagesArrayList.add(message);
        conversationArrayList.add(message);

        updateChatUI(message);
        etMessage.setText("");

        handler.post(new UploadRunnable(message));
        DataHelper.saveMessagesToInternalStorage(ChatActivity.this, messagesArrayList);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    //detect scrolling, in order to show scrollDownButton, and in order to load more earlier messages in chat (when user reach the top)
    @Override
    public void onScrollChange(View view, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        scrollUpAccumulative += (oldScrollY - scrollY);

        if (scrollUpAccumulative > PhotoHelper.dpsToPixels(getApplicationContext(), MESSAGE_HEIGHT_DPS) * Math.min(pendingMessages.size(), MESSAGES_FIRST_LOAD_AMOUNT / 1.5) && loadMoreMessagesActiveCount == 0) {
            handler.post(loadMoreMessages);
        }

        if (svMessages.getChildAt(0).getBottom() - PhotoHelper.dpsToPixels(this, 50) <=
                (svMessages.getHeight() + svMessages.getScrollY()) || totalMessagesLoaded <= 15) {
            btnScrollDown.setVisibility(View.INVISIBLE);
        } else {
            btnScrollDown.setVisibility(View.VISIBLE);
        }
    }

    //uploads a message to UI
    public class UploadRunnable implements Runnable {
        private Message message;

        public UploadRunnable(Message message) {
            this.message = message;
        }

        @Override
        public void run() {
            svMessages.fullScroll(ScrollView.FOCUS_DOWN);
            scrollUpAccumulative = 0;
            Message.uploadMessageToFirebase(getApplicationContext(), message);
        }
    }

    //private class that defines the properties of a message which will be drawn to UI
    private class UIMessage {
        public boolean senderSelf;
        public boolean senderSwitch;
        public String message;
        public String time;

        public UIMessage(boolean senderSelf, boolean senderSwitch, String message, String time) {
            this.senderSelf = senderSelf;
            this.senderSwitch = senderSwitch;
            this.message = message;
            this.time = time;
        }
    }
}
