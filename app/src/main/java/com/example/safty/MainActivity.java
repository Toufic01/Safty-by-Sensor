package com.example.safty;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request necessary permissions
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE
        }, 1);

        // Start the Shake Detection Service
        Intent serviceIntent = new Intent(this, ShakeService.class);
        startService(serviceIntent);

        // Move the app to the background
        moveTaskToBack(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            boolean locationPermissionGranted = false;

            // Check all granted permissions
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        locationPermissionGranted = true;
                    } else {
                        Toast.makeText(this, "Location permission is required for this app to work.", Toast.LENGTH_LONG).show();
                    }
                }
            }

            if (!locationPermissionGranted) {
                // Redirect user to settings if location permission is denied
                Toast.makeText(this, "Permission Denied. Please enable it in settings.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Permissions Granted Successfully", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
