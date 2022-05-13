package com.example.nisim.messengerapp.activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.nisim.messengerapp.R;
import com.example.nisim.messengerapp.classes.DataHelper;
import com.example.nisim.messengerapp.classes.GlideApp;
import com.example.nisim.messengerapp.classes.Helper;
import com.example.nisim.messengerapp.classes.PhotoHelper;
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
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    final int REQUEST_PHOTO_FROM_CAMERA = 1, REQUEST_PHOTO_FROM_GALLERY = 2;
    final int SHOW_UI_NO_PHOTO = 1, SHOW_UI_PHOTO = 2;
    final int REQUEST_SIGNED_OUT = 1;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    ProgressDialog progressDialog;
    Uri lastProfileImageUri = null;
    String currentTakenPhotoPath;

    FloatingActionButton btnSave, btnCamera, btnGallery;
    TextView tvLabelName, tvLabelPhone, tvName, tvPhone;
    ImageView ivProfilePhoto;
    CardView cvProfilePhoto;
    ImageButton ibPhoto;
    Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //permissions check
        Helper.permissionCheck(this);

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        progressDialog = new ProgressDialog(this);

        //references
        ibPhoto = findViewById(R.id.ibPhoto);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        cvProfilePhoto = findViewById(R.id.cvProfilePhoto);
        tvLabelName = findViewById(R.id.tvLabelName);
        tvLabelPhone = findViewById(R.id.tvLabelPhone);
        tvName = findViewById(R.id.tvName);
        tvPhone = findViewById(R.id.tvPhone);
        btnSave = findViewById(R.id.btnSave);

        //set listeners
        btnSave.setOnClickListener(this);
        ibPhoto.setOnClickListener(this);
        ivProfilePhoto.setOnClickListener(this);
        ivProfilePhoto.setOnLongClickListener(this);

        //preventing bugs with Glide load
        if (getCacheDir().exists()) {
            this.getCacheDir().delete();
        }

        //add back toolbar button
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //set toolbar title
        getSupportActionBar().setTitle("Profile");

        updatePhotoUI(SHOW_UI_NO_PHOTO);
        updateDataUI();

        retrieveProfilePhotoFromStorage();
    }

    @Override
    public void onClick(View view) {
        if (view == btnSave) {
            saveProfilePhoto();
        } else if (view == ibPhoto || view == ivProfilePhoto) {
            openPhotoDialog();
        } else if (view == btnCamera) {
            takePhotoFromCamera();
            dialog.dismiss();
        } else if (view == btnGallery) {
            selectPhotoFromGallery();
            dialog.dismiss();
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view == ivProfilePhoto) {
            openRemovePhotoDialog();
            return true;
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        if (item.getItemId() == R.id.action_logout) {
            openLogoutDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    //open a logout dialog
    private void openLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setTitle("Log out");
        builder.setMessage("Are you sure you want to log out?");
        builder.setCancelable(true);

        //set positive button
        builder.setPositiveButton("Log out", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                logOut();
            }
        });

        //set negative button
        builder.setNegativeButton("Cancel", null);

        //show dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //open a photo dialog
    private void openPhotoDialog() {
        //create a new Dialog
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.add_photo_dialog);
        dialog.setCancelable(true);

        //references from Dialog
        btnCamera = dialog.findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(this);
        btnGallery = dialog.findViewById(R.id.btnGallery);
        btnGallery.setOnClickListener(this);

        //show dialog
        dialog.show();
    }

    //log out from current user
    private void logOut() {
        progressDialog.setMessage("Signing out...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        DataHelper.deleteDataFromInternalStorage(this);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();

        Intent intent = new Intent(ProfileActivity.this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, REQUEST_SIGNED_OUT);
    }


    //select photo from gallery
    private void selectPhotoFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PHOTO_FROM_GALLERY);
    }

    //take photo from camera
    private void takePhotoFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {

            //create an image file which will store the taken photo
            File photoFile;
            photoFile = PhotoHelper.createCacheImageFile(this);
            currentTakenPhotoPath = photoFile.getAbsolutePath();

            Uri photoURI = FileProvider.getUriForFile(this, "com.example.nisim.messengerapp.fileprovider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(intent, REQUEST_PHOTO_FROM_CAMERA);
        }
    }

    //open a photo-remove dialog
    private void openRemovePhotoDialog() {
        //create a new Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setTitle("Remove Photo");
        builder.setMessage("Do you want to remove this photo?");
        builder.setCancelable(true);

        //set positive button
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                lastProfileImageUri = null;
                updatePhotoUI(SHOW_UI_NO_PHOTO);
                btnSave.setVisibility(View.VISIBLE);
            }
        });

        //set negative button
        builder.setNegativeButton("Cancel", null);

        //show dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //save changes to firebase&storage
    private void saveProfilePhoto() {
        //save changes to firebase
        StorageReference storageReference = firebaseStorage.getReference();
        if (lastProfileImageUri != null) {
            storageReference.child("images/profilePhotos/" + firebaseUser.getUid()).putFile(lastProfileImageUri);
        } else {
            File emptyFile = PhotoHelper.createCacheImageFile(this);
            storageReference.child("images/profilePhotos/" + firebaseUser.getUid()).putFile(Uri.fromFile(emptyFile));
        }

        //save changes to storage
        if (lastProfileImageUri != null) {
            DataHelper.saveProfilePhotoToStorage(this, firebaseUser.getUid() + ".jpg", lastProfileImageUri, 200);
        } else {
            DataHelper.deleteProfilePhotoFromStorage(this, firebaseUser.getUid() + ".jpg");
        }

        finish();
    }

    //load profile photo from storage and update UI
    private void retrieveProfilePhotoFromStorage() {
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        lastProfileImageUri = DataHelper.loadProfilePhotoFromStorage(this, firebaseUser.getUid() + ".jpg");
        if (lastProfileImageUri != null && !isFinishing()) {
            GlideApp.with(ProfileActivity.this)
                    .load(lastProfileImageUri)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            retrieveProfilePhotoFromFirebase();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            updatePhotoUI(SHOW_UI_PHOTO);
                            retrieveProfilePhotoFromFirebase();
                            return false;
                        }
                    })
                    .dontAnimate()
                    .placeholder(R.drawable.ic_add_photo)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true).into(ivProfilePhoto);
        } else {
            retrieveProfilePhotoFromFirebase();
        }
    }

    //get profile photo from firebase and update UI
    private void retrieveProfilePhotoFromFirebase() {
        //create a file for the profile photo
        storageReference = firebaseStorage.getReference().child("images/profilePhotos/" + firebaseUser.getUid());
        File profilePhotosDirectory = new File(this.getFilesDir(), "profilePhotos");
        final File localFile = new File(profilePhotosDirectory, firebaseUser.getUid() + ".jpg");

        //create directory in case it's missing
        if (!profilePhotosDirectory.exists()) {
            profilePhotosDirectory.mkdirs();
        }

        //download photo to local file
        storageReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                lastProfileImageUri = Uri.fromFile(localFile);
                Drawable currentDrawable = ivProfilePhoto.getDrawable(); //prevent "blinking" when loading photo
                DataHelper.saveProfilePhotoToStorage(ProfileActivity.this, firebaseUser.getUid() + ".jpg", lastProfileImageUri, 200);

                if (!isFinishing()) {
                    GlideApp.with(ProfileActivity.this)
                            .load(lastProfileImageUri)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    updatePhotoUI(SHOW_UI_PHOTO);
                                    return false;
                                }
                            })
                            .dontAnimate()
                            .placeholder(currentDrawable)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true).into(ivProfilePhoto);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });
    }

    //update photo UI
    private void updatePhotoUI(int show) {
        //hide all
        ibPhoto.setVisibility(View.GONE);
        cvProfilePhoto.setVisibility(View.GONE);

        //show specific
        switch (show) {
            case SHOW_UI_NO_PHOTO:
                ibPhoto.setVisibility(View.VISIBLE);
                break;
            case SHOW_UI_PHOTO:
                cvProfilePhoto.setVisibility(View.VISIBLE);
                break;
        }
    }

    //update data UI
    private void updateDataUI() {
        DatabaseReference ref = firebaseDatabase.getReference().child("Users").child(firebaseUser.getUid());
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    tvName.setText(user.getName());
                    tvPhone.setText(Helper.removeAreaCode(user.getPhone()));
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PHOTO_FROM_GALLERY) {
                //get photo URI from intent result
                lastProfileImageUri = PhotoHelper.getUriRealPath(this, data.getData());
                updatePhotoUI(SHOW_UI_PHOTO);
                btnSave.setVisibility(View.VISIBLE);
                if (!isFinishing()) {
                    GlideApp.with(this)
                            .load(lastProfileImageUri)
                            .dontAnimate()
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true).into(ivProfilePhoto);
                }
            } else if (requestCode == REQUEST_PHOTO_FROM_CAMERA) {
                //get photo file from cache
                File file = new File(currentTakenPhotoPath);

                Bitmap bitmap;
                try {
                    //get bitmap from cache
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.fromFile(file));

                    //fix photo rotation
                    bitmap = PhotoHelper.fixImageRotation(currentTakenPhotoPath, bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                updatePhotoUI(SHOW_UI_PHOTO);
                btnSave.setVisibility(View.VISIBLE);
                if (!isFinishing()) {
                    GlideApp.with(this)
                            .load(bitmap)
                            .dontAnimate()
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true).into(ivProfilePhoto);
                }
                lastProfileImageUri = Uri.fromFile(PhotoHelper.saveBitmapToCache(this, bitmap));
            }
        }
    }
}