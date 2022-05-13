package com.example.nisim.messengerapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.nisim.messengerapp.services.ActivityCheckService;
import com.example.nisim.messengerapp.R;
import com.example.nisim.messengerapp.adapters.ChatsAdapter;
import com.example.nisim.messengerapp.classes.Chat;
import com.example.nisim.messengerapp.classes.DataHelper;
import com.example.nisim.messengerapp.classes.Helper;
import com.example.nisim.messengerapp.classes.Message;
import com.example.nisim.messengerapp.classes.User;
import com.example.nisim.messengerapp.services.MessageNotificationService;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    FirebaseDatabase firebaseDatabase;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    DatabaseReference databaseReference;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    Handler handler;

    ArrayList<Chat> chatsArrayList;
    ArrayList<Message> messagesArrayList;
    Map<String, User> usersArrayList;
    DataSnapshot lastDataSnapshot = null;
    Uri lastProfileImageUri;
    int retrieveChatsCount = 0, errorCount = 0;
    boolean retrieveFirebaseActive = false;

    FloatingActionButton btnContacts;
    ListView lvChats;
    TextView tvError;
    ImageView ivError;

    //synchronize with firebase
    Runnable firebaseSynchronize = new Runnable() {
        @Override
        public void run() {
            if (!retrieveFirebaseActive) { //no ongoing firebase process
                if (lastDataSnapshot != null) {
                    retrieveFirebaseActive = true;
                    retrieveChatsFromFirebase(lastDataSnapshot);
                    lastDataSnapshot = null;
                }
                errorCount = 0;
            } else { //active firebase process at the moment
                handler.postDelayed(firebaseSynchronize, 250);
                errorCount++;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //permissions check
        Helper.permissionCheck(this);

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        databaseReference = firebaseDatabase.getReference();
        firebaseStorage = FirebaseStorage.getInstance();
        handler = new Handler();

        //login check
        if (firebaseUser == null) {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        //references
        tvError = findViewById(R.id.tvError);
        ivError = findViewById(R.id.ivError);
        lvChats = findViewById(R.id.lvChats);
        lvChats.setOnItemClickListener(this);
        btnContacts = findViewById(R.id.btnContacts);
        btnContacts.setOnClickListener(this);

        //add a listener that will get data from firebase
        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //get dataSnapshot of whole database, and not only Messages
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        lastDataSnapshot = dataSnapshot;
                        handler.post(firebaseSynchronize);
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

        //constant synchronization with Firebase when Messages section changes
        databaseReference.child("Messages").addValueEventListener(eventListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //first synchronization with Storage
        retrieveChatsFromStorage();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public void onClick(View view) {
        if (view == btnContacts) {
            //go to ContactsActivity
            Intent intent = new Intent(MainActivity.this, ContactsActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            //go to ProfileActivity
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (adapterView == lvChats) { //clicked on a chat from ListView
            if (chatsArrayList != null && chatsArrayList.size() > i) {
                Chat chat = chatsArrayList.get(i);
                User chatPartner;
                if (!firebaseUser.getUid().equals(chat.getFromUser().getUid())) {
                    chatPartner = chat.getFromUser();
                } else {
                    chatPartner = chat.getToUser();
                }

                chatPartner.setPhoto(chat.getPhoto().toString());
                goToChat(chatPartner);
            }
        }
    }

    //go to given chat activity
    private void goToChat(User chatPartner) {
        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
        intent.putExtra("chatPartner", DataHelper.objectToJson(chatPartner));
        intent.putExtra("senderActivity", this.getClass().getSimpleName());
        startActivity(intent);
    }

    //get chats from storage and update UI
    private void retrieveChatsFromStorage() {
        messagesArrayList = DataHelper.loadMessagesFromInternalStorage(this);
        usersArrayList = DataHelper.loadUsersFromInternalStorage(this);
        chatsArrayList = getChats(messagesArrayList, usersArrayList);

        retrieveProfilePhotosFromStorage();
        updateUI(chatsArrayList);
    }

    //get chats from DataSnapshot, update UI and save them
    private void retrieveChatsFromFirebase(DataSnapshot dataSnapshot) {
        messagesArrayList = DataHelper.filterMessages(dataSnapshot);
        usersArrayList = getUsers(messagesArrayList, dataSnapshot);
        chatsArrayList = getChats(messagesArrayList, usersArrayList);

        retrieveProfilePhotosFromFirebase();
        DataHelper.saveMessagesToInternalStorage(MainActivity.this, messagesArrayList);
        DataHelper.saveUsersToInternalStorage(MainActivity.this, usersArrayList);
    }

    //load profile photos from storage
    private void retrieveProfilePhotosFromStorage() {
        for (Chat chat : chatsArrayList) {
            //get chat partner
            User chatPartner;
            if (chat.getFromUser().getUid().equals(firebaseUser.getUid())) { //user is sender
                chatPartner = chat.getToUser();
            } else {
                chatPartner = chat.getFromUser();
            }

            if (chatPartner != null) {
                lastProfileImageUri = DataHelper.loadProfilePhotoFromStorage(this, chatPartner.getUid() + ".jpg");
                if (lastProfileImageUri != null) {
                    chat.setPhoto(lastProfileImageUri);
                }
            }
        }
    }

    //load profile photos from firebase
    private void retrieveProfilePhotosFromFirebase() {
        retrieveChatsCount = 0;

        if (chatsArrayList.size() == 0) {
            retrieveFirebaseActive = false;
        }

        for (Chat chat : chatsArrayList) {
            //get chat partner
            User chatPartner;
            if (chat.getFromUser().getUid().equals(firebaseUser.getUid())) { //user is sender
                chatPartner = chat.getToUser();
            } else {
                chatPartner = chat.getFromUser();
            }

            //define photo path in firebase storage
            lastProfileImageUri = null;

            storageReference = firebaseStorage.getReference().child("images/profilePhotos/" + chatPartner.getUid());

            //create a file for the profile photo
            File profilePhotosDirectory = new File(this.getFilesDir(), "profilePhotos");
            final File localFile = new File(profilePhotosDirectory, chatPartner.getUid() + ".jpg");

            //create directory in case it's missing
            if (!profilePhotosDirectory.exists()) {
                profilePhotosDirectory.mkdirs();
            }

            //download photo to local file
            storageReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                    for (Chat chat : chatsArrayList) {
                        //get chat partner
                        User chatPartner;
                        if (chat.getFromUser().getUid().equals(firebaseUser.getUid())) { //user is sender
                            chatPartner = chat.getToUser();
                        } else {
                            chatPartner = chat.getFromUser();
                        }

                        if (chatPartner.getUid().equals(taskSnapshot.getStorage().getName())) {
                            lastProfileImageUri = Uri.fromFile(localFile);
                            DataHelper.saveProfilePhotoToStorage(MainActivity.this, chatPartner.getUid() + ".jpg", lastProfileImageUri, 50);
                            if (lastProfileImageUri != null) {
                                chat.setPhoto(lastProfileImageUri);
                            }
                            retrieveChatsCount++;

                            if (retrieveChatsCount == chatsArrayList.size()) {
                                updateUI(chatsArrayList);
                                retrieveFirebaseActive = false;
                            }
                            break;
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    retrieveChatsCount++;
                    if (retrieveChatsCount == chatsArrayList.size()) {
                        updateUI(chatsArrayList);
                        retrieveFirebaseActive = false;
                    }

                }
            });
        }
    }

    //retrieve users from a DataSnapshot to an array
    private Map<String, User> getUsers(ArrayList<Message> globalMessages, DataSnapshot dataSnapshot) {
        ArrayList<Message> messages = new ArrayList<>(globalMessages);
        Map<String, User> users = new HashMap<>();

        //add chat partners
        for (Message message : messages) {
            User fromUser = dataSnapshot.child("Users").child(message.getFromUserUid()).getValue(User.class);
            User toUser = dataSnapshot.child("Users").child(message.getToUserUid()).getValue(User.class);
            if (!fromUser.getUid().equals(firebaseUser.getUid())) {
                users.put(fromUser.getUid(), fromUser);
            } else {
                users.put(toUser.getUid(), toUser);
            }
        }

        //add self
        User self = dataSnapshot.child("Users").child(firebaseUser.getUid()).getValue(User.class);
        users.put(firebaseUser.getUid(), self);

        return users;
    }

    //retrieve chats to an array
    private ArrayList<Chat> getChats(ArrayList<Message> globalMessages, Map<String, User> users) {
        ArrayList<Message> messages = new ArrayList<>(globalMessages);
        ArrayList<Chat> chats = new ArrayList<>();
        Map<Pair<String, String>, Integer> pairsMap = removeRepeatedChatMessages(messages);
        Collections.sort(messages);

        for (Map.Entry<Pair<String, String>, Integer> entry : pairsMap.entrySet()) {
            for (Message message : messages) {
                Pair<String, String> pair = entry.getKey();
                int unreadMessages = entry.getValue();

                if ((message.getFromUserUid().equals(pair.first) && message.getToUserUid().equals(pair.second)) ||
                        (message.getToUserUid().equals(pair.first) && message.getFromUserUid().equals(pair.second))) {
                    boolean lastMessageMine = firebaseUser.getUid().equals(message.getFromUserUid());
                    User fromUser = users.get(message.getFromUserUid());
                    User toUser = users.get(message.getToUserUid());
                    String fixedMessage = Helper.minimizeMessage(message.getMessage(), 30, lastMessageMine);
                    Chat chat = new Chat(fromUser, toUser, fixedMessage, message.getTimestamp(), unreadMessages, Uri.EMPTY);
                    chats.add(chat);
                    break;
                }
            }
        }

        Collections.sort(chats);
        return chats;
    }

    //remove the repeated messages to show every chat only once
    private Map<Pair<String, String>, Integer> removeRepeatedChatMessages(ArrayList<Message> messages) {
        Map<Pair<String, String>, Integer> pairsMap = new HashMap<>();
        ArrayList<Message> tempMessages = new ArrayList<>(messages);

        for (Message message : tempMessages) {
            if (pairsMap.isEmpty()) {
                int unreadMessages = 0;
                if (!message.isSeen() && !message.getFromUserUid().equals(firebaseUser.getUid())) {
                    unreadMessages = 1;
                }
                pairsMap.put(new Pair<>(message.getFromUserUid(), message.getToUserUid()), unreadMessages);
            } else {
                boolean exists = false;
                Pair<String, String> pair, key = null;

                for (Map.Entry<Pair<String, String>, Integer> entry : pairsMap.entrySet()) {
                    pair = entry.getKey();

                    if ((message.getFromUserUid().equals(pair.first) && message.getToUserUid().equals(pair.second)) ||
                            (message.getToUserUid().equals(pair.first) && message.getFromUserUid().equals(pair.second))) {
                        if (message.getFromUserUid().equals(pair.first)) {
                            key = new Pair<>(message.getFromUserUid(), message.getToUserUid());
                        } else {
                            key = new Pair<>(message.getToUserUid(), message.getFromUserUid());
                        }
                        exists = true;
                    }
                }

                if (exists) {
                    if (!message.isSeen() && !message.getFromUserUid().equals(firebaseUser.getUid())) {
                        pairsMap.put(key, pairsMap.get(key) + 1);
                    }
                } else {
                    int unreadMessages = 0;
                    if (!message.isSeen() && !message.getFromUserUid().equals(firebaseUser.getUid())) {
                        unreadMessages = 1;
                    }
                    pairsMap.put(new Pair<>(message.getFromUserUid(), message.getToUserUid()), unreadMessages);
                }
            }
        }

        return pairsMap;
    }

    //update UI with the given chats
    private void updateUI(ArrayList<Chat> chats) {
        //create a contacts adapter
        ChatsAdapter chatsAdapter = new ChatsAdapter(MainActivity.this, chats);

        //get the current views to the adapter (if its not the first inflation)
        ArrayList<View> oldViews = new ArrayList<>();
        for (int i = 0; i < lvChats.getChildCount(); i++) {
            oldViews.add(lvChats.getChildAt(i));
        }

        //set the adapter with the current views
        chatsAdapter.setOldViews(oldViews);
        lvChats.setAdapter(chatsAdapter);

        if (chats.size() > 0) {
            lvChats.setVisibility(View.VISIBLE);
            tvError.setVisibility(View.GONE);
            ivError.setVisibility(View.GONE);
        } else { //no chats to display
            lvChats.setVisibility(View.GONE);
            tvError.setVisibility(View.VISIBLE);
            ivError.setVisibility(View.VISIBLE);
        }
    }
}