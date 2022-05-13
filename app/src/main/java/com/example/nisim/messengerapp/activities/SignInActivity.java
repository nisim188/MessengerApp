package com.example.nisim.messengerapp.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nisim.messengerapp.R;
import com.example.nisim.messengerapp.classes.Helper;
import com.example.nisim.messengerapp.classes.PhotoHelper;
import com.example.nisim.messengerapp.classes.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class SignInActivity extends AppCompatActivity implements View.OnClickListener {

    final int SHOW_UI_PHONE = 1, SHOW_UI_VERIFICATION = 2, SHOW_UI_REGISTER = 3;
    final int REQUEST_SIGNED_OUT = 1;
    String phoneNumber, sentVerificationCode;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    FirebaseStorage firebaseStorage;
    FirebaseDatabase firebaseDatabase;
    ProgressDialog progressDialog;

    FloatingActionButton btnSendVerificationCode, btnSignIn, btnRegister;
    TextView tvLabelVerificationCode, tvLabelPhoneNumber, tvLabelName;
    EditText etPhoneNumber, etVerificationCode, etName;

    //Phone-Verification callback handler
    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
            //verification completed automatically without SMS (through google-play services).

            signInWithPhoneAuthCredential(phoneAuthCredential);
            progressDialog.setMessage("Verifying...");
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            //verification failed to verify.

            progressDialog.dismiss();
            Toast.makeText(SignInActivity.this, "Error: Failed to send verification SMS.", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            //succeeded sending verification SMS.
            super.onCodeSent(s, forceResendingToken);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();
                    updateUI(SHOW_UI_VERIFICATION);
                }
            }, 1000);

            sentVerificationCode = s;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        //permissions check
        Helper.permissionCheck(this);

        progressDialog = new ProgressDialog(this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseStorage = FirebaseStorage.getInstance();

        //login check
        if (firebaseUser != null) {
            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        //references
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etVerificationCode = findViewById(R.id.etVerificationCode);
        etName = findViewById(R.id.etName);
        tvLabelVerificationCode = findViewById(R.id.tvLabelVerificationCode);
        tvLabelPhoneNumber = findViewById(R.id.tvLabelPhoneNumber);
        tvLabelName = findViewById(R.id.tvLabelName);
        btnSendVerificationCode = findViewById(R.id.btnSendVerificationCode);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnRegister = findViewById(R.id.btnRegister);

        //set listeners
        btnSendVerificationCode.setOnClickListener(this);
        btnSignIn.setOnClickListener(this);
        btnRegister.setOnClickListener(this);

        updateUI(SHOW_UI_PHONE);
    }

    @Override
    public void onClick(View view) {
        if (view == btnSendVerificationCode) {
            sendVerificationCode();
        } else if (view == btnSignIn) {
            verifySignInCode();
        } else if (view == btnRegister) {
            register();
        }
    }

    //send verification code
    private void sendVerificationCode() {
        //get input
        phoneNumber = etPhoneNumber.getText().toString().trim();

        //validate phone number
        if (phoneNumber.isEmpty()) {
            etPhoneNumber.setError("Phone number is required.");
            etPhoneNumber.requestFocus();
            return;
        } else if (phoneNumber.length() < 10) {
            etPhoneNumber.setError("Phone number must be valid.");
            etPhoneNumber.requestFocus();
            return;
        }

        //format phone number
        phoneNumber = Helper.addAreaCode(phoneNumber);

        //send SMS verification
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,               // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,         // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks

        //show progress dialog
        progressDialog.setMessage("Sending Verification SMS...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    //check verification code
    private void verifySignInCode() {
        //validate verification code
        if (etVerificationCode.getText().toString().isEmpty()) {
            etVerificationCode.setError("Verification code is required.");
            etVerificationCode.requestFocus();
            return;
        }

        //start verifying
        String code = etVerificationCode.getText().toString();
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(sentVerificationCode, code);
        signInWithPhoneAuthCredential(credential);

        //show progress dialog
        progressDialog.setMessage("Verifying...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    //attempt to sign in
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            signInCompleted();
                        } else {
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(SignInActivity.this, "Verification code is incorrect.", Toast.LENGTH_SHORT).show();
                                progressDialog.cancel();
                            }
                        }
                    }
                });
    }

    //successfully signed in
    private void signInCompleted() {
        //get current user
        firebaseUser = firebaseAuth.getCurrentUser();
        DatabaseReference usersRef = firebaseDatabase.getReference("Users");

        //check user in firebase
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.child(firebaseUser.getUid()).exists()) { //registered user
                    progressDialog.cancel();
                    //go to MainActivity
                    Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else { //new user
                    updateUI(SHOW_UI_REGISTER);
                    progressDialog.cancel();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //register new user
    private void register() {
        //validate name
        if (etName.getText().toString().isEmpty()) {
            etName.setError("Name is required.");
            etName.requestFocus();
            return;
        }

        //get user data
        firebaseUser = firebaseAuth.getCurrentUser();
        String uid = firebaseUser.getUid();
        String name = etName.getText().toString();
        String phone = firebaseUser.getPhoneNumber();
        User user = new User(uid, name, phone, 0);
        firebaseDatabase.getReference("Users").child(uid).setValue(user);

        File emptyFile = PhotoHelper.createCacheImageFile(this);
        firebaseStorage.getReference().child("images/profilePhotos/" + firebaseUser.getUid()).putFile(Uri.fromFile(emptyFile));

        //go to MainActivity
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateUI(int show) {
        //hide all
        tvLabelPhoneNumber.setVisibility(View.GONE);
        etPhoneNumber.setVisibility(View.GONE);
        btnSendVerificationCode.setVisibility(View.GONE);
        tvLabelVerificationCode.setVisibility(View.GONE);
        etVerificationCode.setVisibility(View.GONE);
        btnSignIn.setVisibility(View.GONE);
        tvLabelName.setVisibility(View.GONE);
        etName.setVisibility(View.GONE);
        btnRegister.setVisibility(View.GONE);

        //show specific
        switch (show) {
            case SHOW_UI_PHONE:
                tvLabelPhoneNumber.setVisibility(View.VISIBLE);
                etPhoneNumber.setVisibility(View.VISIBLE);
                btnSendVerificationCode.setVisibility(View.VISIBLE);
                break;
            case SHOW_UI_VERIFICATION:
                tvLabelVerificationCode.setVisibility(View.VISIBLE);
                etVerificationCode.setVisibility(View.VISIBLE);
                btnSignIn.setVisibility(View.VISIBLE);
                break;
            case SHOW_UI_REGISTER:
                tvLabelName.setVisibility(View.VISIBLE);
                etName.setVisibility(View.VISIBLE);
                btnRegister.setVisibility(View.VISIBLE);
                break;
        }
    }
}