package com.example.nisim.messengerapp.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.nisim.messengerapp.R;
import com.example.nisim.messengerapp.classes.Helper;

public class PermissionsActivity extends AppCompatActivity implements View.OnClickListener {

    final int REQUEST_PERMISSIONS = 1, REQUEST_PERMISSIONS_FROM_SETTINGS = 2;

    LinearLayout llRequest;
    TextView tvAllow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        //references
        llRequest = findViewById(R.id.llRequest);
        tvAllow = findViewById(R.id.tvAllow);

        //set listener
        tvAllow.setOnClickListener(this);

        //hide request
        llRequest.setVisibility(View.INVISIBLE);

        checkPermissions();
    }

    @Override
    public void onClick(View view) {
        if (view == tvAllow) {
            openApplicationSettings();
        }
    }

    //check permissions availability
    private void checkPermissions() {
        String[] missingPermissions = Helper.getMissingPermissions(this, Helper.getApplicationNeededPermissions());
        if (missingPermissions != null) {
            ActivityCompat.requestPermissions(this, missingPermissions, REQUEST_PERMISSIONS);
        } else {
            finish();
        }
    }

    //check if all permission granted in given results
    private boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    //open application settings
    private void openApplicationSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", this.getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, REQUEST_PERMISSIONS_FROM_SETTINGS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (hasAllPermissionsGranted(grantResults)) {
                finish();
            } else {
                llRequest.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PERMISSIONS_FROM_SETTINGS) {
            checkPermissions();
        }
    }

    @Override
    public void onBackPressed() {
        //prevent user to go back
    }
}