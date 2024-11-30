package com.example.safty;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class ShakeService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float accelerationCurrentValue;
    private float accelerationLastValue;
    private float shakeThreshold = 12.0f;
    private String emergencyNumber = "016108029701"; // Replace with the preset number
    private FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Sensor Manager and Accelerometer
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        accelerationCurrentValue = SensorManager.GRAVITY_EARTH;
        accelerationLastValue = SensorManager.GRAVITY_EARTH;

        // Initialize FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Start foreground service
        startForegroundService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "ACTION_PLAY":
                    resumeShakeDetection();
                    Toast.makeText(this, "Shake Detection Started", Toast.LENGTH_SHORT).show();
                    break;
                case "ACTION_PAUSE":
                    pauseShakeDetection();
                    Toast.makeText(this, "Shake Detection Paused", Toast.LENGTH_SHORT).show();
                    break;
                case "ACTION_STOP":
                    stopSelf();
                    Toast.makeText(this, "Shake Detection Stopped", Toast.LENGTH_SHORT).show();
                    break;
            }
        } else {
            // Default behavior: resume shake detection
            resumeShakeDetection();
        }
        return START_STICKY;
    }


    private void startForegroundService() {
        NotificationChannel channel = new NotificationChannel(
                "ShakeServiceChannel",
                "Shake Detection Service",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, "ShakeServiceChannel")
                .setContentTitle("Shake Detection")
                .setContentText("Monitoring for shakes...")
                .setSmallIcon(R.drawable.ic_notification)
                .addAction(createNotificationAction("Play", "ACTION_PLAY"))
                .addAction(createNotificationAction("Pause", "ACTION_PAUSE"))
                .addAction(createNotificationAction("Stop", "ACTION_STOP"))
                .build();

        startForeground(1, notification);
    }

    private NotificationCompat.Action createNotificationAction(String title, String action) {
        Intent intent = new Intent(this, ShakeService.class);
        intent.setAction(action);
        return new NotificationCompat.Action.Builder(
                R.drawable.ic_notification, title,
                PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        ).build();
    }

    private void resumeShakeDetection() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void pauseShakeDetection() {
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
                Toast.makeText(this, "Shake Detected! Sending Emergency Message...", Toast.LENGTH_SHORT).show();
                sendEmergencyMessage();
            }
        }
    }

    private void sendEmergencyMessage() {
        // Check if the location permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission by starting an Activity
            Intent intent = new Intent(this, PermissionRequestActivity.class);
            intent.putExtra("request_permission", true);  // Pass a flag or anything you need
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Toast.makeText(this, "Location Permission Not Granted. Requesting...", Toast.LENGTH_SHORT).show();
            return;
        }

        // If permission is granted, fetch the location
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    String locationText = "Location not available";
                    if (location != null) {
                        locationText = "http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                    }

                    SmsManager smsManager = SmsManager.getDefault();
                    String message = "Emergency! I need help. My location is: " + locationText;
                    smsManager.sendTextMessage(emergencyNumber, null, message, null, null);
                    Toast.makeText(this, "Emergency Message Sent!", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show());
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pauseShakeDetection();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
