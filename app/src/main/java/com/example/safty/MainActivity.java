package com.example.safty;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float accelerationCurrentValue;
    private float accelerationLastValue;
    private float shakeThreshold = 12.0f;
    private String emergencyNumber = "01722536524"; // Replace with the preset number
    private TextView statusTextView;
    private FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.textView);

        // Initialize Sensor Manager and Accelerometer
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        accelerationCurrentValue = SensorManager.GRAVITY_EARTH;
        accelerationLastValue = SensorManager.GRAVITY_EARTH;

        // Initialize FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Request Permissions
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE
        }, 1);

        // Check if Location Services Are Enabled
        checkLocationEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            accelerationLastValue = accelerationCurrentValue;
            accelerationCurrentValue = (float) Math.sqrt((x * x) + (y * y) + (z * z));
            float delta = accelerationCurrentValue - accelerationLastValue;

            if (delta > shakeThreshold) {
                statusTextView.setText("Shake Detected! Sending Emergency Message...");
                Toast.makeText(this, "Shake Detected! Attempting to Get Location...", Toast.LENGTH_SHORT).show();
                sendEmergencyMessage();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this app
    }

    private void sendEmergencyMessage() {
        // Check Location Permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location Permission Not Granted", Toast.LENGTH_SHORT).show();
            return;
        }

        // Request Current Location
        fusedLocationProviderClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        String locationText = "Location not available";

                        if (location != null) {
                            locationText = "http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                        }

                        // Send SMS
                        SmsManager smsManager = SmsManager.getDefault();
                        String message = "Emergency! I need help. My location is: " + locationText;

                        try {
                            smsManager.sendTextMessage(emergencyNumber, null, message, null, null);
                            Toast.makeText(MainActivity.this, "Emergency Message Sent!", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Failed to Send SMS", Toast.LENGTH_LONG).show();
                        }

                        // Optional: Make a Call
                        Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setData(android.net.Uri.parse("tel:" + emergencyNumber));
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            startActivity(callIntent);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Failed to get location", Toast.LENGTH_SHORT).show());
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void checkLocationEnabled() {
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    // Handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
