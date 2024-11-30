package com.example.safty;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public class PermissionRequestActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request the location permission when the activity starts
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with sending message
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }

            // Close this activity after handling permission
            finish();
        }
    }
}
