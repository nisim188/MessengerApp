package com.example.nisim.messengerapp.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.nisim.messengerapp.R;
import com.example.nisim.messengerapp.adapters.ContactsAdapter;
import com.example.nisim.messengerapp.classes.Contact;
import com.example.nisim.messengerapp.classes.DataHelper;
import com.example.nisim.messengerapp.classes.Helper;
import com.example.nisim.messengerapp.classes.User;
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
import java.util.HashMap;
import java.util.Map;

public class ContactsActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    final int REQUEST_GO_TO_CHAT = 1;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    ArrayList<Contact> registeredContacts;
    Uri lastProfileImageUri;
    int retrieveContactsCount = 0;

    ListView lvContacts;
    ImageView ivError;
    TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        //permissions check
        Helper.permissionCheck(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        //references
        lvContacts = findViewById(R.id.lvContacts);
        ivError = findViewById(R.id.ivError);
        tvError = findViewById(R.id.tvError);

        //set listener
        lvContacts.setOnItemClickListener(this);

        //add back toolbar button
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getContacts();
    }

    //go into a specific chat with a contact when clicking on his name in the contacts list
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (adapterView == lvContacts) {
            final Contact contact = registeredContacts.get(i);

            //attach contact with firebase user, in order to enter the conversation
            DatabaseReference refUsers = firebaseDatabase.getReference().child("Users").child(contact.getUserUid());
            refUsers.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User chatPartner = dataSnapshot.getValue(User.class);
                    chatPartner.setPhoto(contact.getPhoto().toString());
                    goToChat(chatPartner);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    //go to given chat activity
    private void goToChat(User chatPartner) {
        Intent intent = new Intent(ContactsActivity.this, ChatActivity.class);
        intent.putExtra("chatPartner", DataHelper.objectToJson(chatPartner));
        intent.putExtra("senderActivity", this.getClass().getSimpleName());
        startActivityForResult(intent, REQUEST_GO_TO_CHAT);
    }

    //load contacts from phone
    private void getContacts() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //get registered contacts into an array
                registeredContacts = new ArrayList<>();
                Map<String, String> contactsTemp = new HashMap<>();
                Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

                //iterate all contacts, and add them into the HashMap
                while (phones.moveToNext()) {
                    String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phoneNumber = phoneNumber.replaceAll("[^0-9\\+]", "");

                    //prevent double contacts with the same phone number
                    if (!contactsTemp.containsValue(phoneNumber)) {
                        contactsTemp.put(name, phoneNumber);
                    }
                }
                phones.close();

                //iterate the given contacts HashMap, and get their User data from firebase
                for (Map.Entry<String, String> entry : contactsTemp.entrySet()) {
                    String name = entry.getKey();
                    String phoneNumber = entry.getValue();

                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        User user = data.getValue(User.class);
                        if (user.getPhone().equals(Helper.addAreaCode(phoneNumber)) && !user.getUid().equals(firebaseUser.getUid())) {
                            registeredContacts.add(new Contact(name, phoneNumber, user.getUid(), Uri.EMPTY));
                        }
                    }
                }

                getSupportActionBar().setTitle("Select a contact");
                getSupportActionBar().setSubtitle(registeredContacts.size() + " Contacts");

                retrieveProfilePhotosFromStorage();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //load profile photos from storage and update UI
    private void retrieveProfilePhotosFromStorage() {
        for (Contact contact : registeredContacts) {
            lastProfileImageUri = DataHelper.loadProfilePhotoFromStorage(this, contact.getUserUid() + ".jpg");
            if (lastProfileImageUri != null) {
                contact.setPhoto(lastProfileImageUri);
            }
        }

        updateUI();
        retrieveProfilePhotosFromFirebase();
    }

    //load profile photos from firebase and update UI
    private void retrieveProfilePhotosFromFirebase() {
        retrieveContactsCount = 0;

        for (Contact contact : registeredContacts) {
            lastProfileImageUri = null;
            storageReference = firebaseStorage.getReference().child("images/profilePhotos/" + contact.getUserUid());

            //create a file for the profile photo
            File profilePhotosDirectory = new File(this.getFilesDir(), "profilePhotos");
            final File localFile = new File(profilePhotosDirectory, contact.getUserUid() + ".jpg");

            //create directory in case it's missing
            if (!profilePhotosDirectory.exists()) {
                profilePhotosDirectory.mkdirs();
            }

            //download photo to local file
            storageReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    for (Contact contact : registeredContacts) {
                        if (contact.getUserUid().equals(taskSnapshot.getStorage().getName())) {
                            lastProfileImageUri = Uri.fromFile(localFile);

                            DataHelper.saveProfilePhotoToStorage(ContactsActivity.this, contact.getUserUid() + ".jpg", lastProfileImageUri, 50);
                            if (lastProfileImageUri != null) {
                                contact.setPhoto(lastProfileImageUri);
                                retrieveContactsCount++;

                                if (retrieveContactsCount == registeredContacts.size()) {
                                    updateUI();
                                }
                            }
                            break;
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });
        }
    }

    //show contacts list or show an message if there are no contacts
    private void updateUI() {
        if (!registeredContacts.isEmpty()) {
            //create a contacts adapter
            ContactsAdapter contactsAdapter = new ContactsAdapter(ContactsActivity.this, registeredContacts);

            //get the current views to the adapter (if its not the first inflation)
            ArrayList<View> oldViews = new ArrayList<>();
            for (int i = 0; i < lvContacts.getChildCount(); i++) {
                oldViews.add(lvContacts.getChildAt(i));
            }

            //set the adapter with the current views
            contactsAdapter.setOldViews(oldViews);
            lvContacts.setAdapter(contactsAdapter);
        } else {
            tvError.setVisibility(View.VISIBLE);
            ivError.setVisibility(View.VISIBLE);
            lvContacts.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_GO_TO_CHAT) {
            if (resultCode == RESULT_OK) {
                if (data.getExtras().getBoolean("finish")) {
                    finish();
                }

            }
        }
    }
}