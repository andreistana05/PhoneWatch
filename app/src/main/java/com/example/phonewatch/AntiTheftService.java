package com.example.phonewatch;

import android.widget.Toast;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/**
 * Serviciu de fundal care monitorizeaza senzorii telefonului.
 * Ramane activ chiar daca aplicatia este inchisa pentru a detecta miscari.
 */
public class AntiTheftService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private AlarmManager alarmManager;
    private PinManager pinManager;

    private boolean isMonitoring = false;
    
    // Praguri pentru input-urile senzorilor.
    private static final float THRESHOLD_ACCEL = 2.5f; // Prag acceleratie
    private static final float THRESHOLD_GYRO = 0.8f;  // Prag giroscop
    
    private static final String CHANNEL_ID = "AntiTheftChannel";
    public static final String ACTION_STATE_CHANGED = "com.example.phonewatch.STATE_CHANGED";

    @Override
    public void onCreate() {
        super.onCreate();
        // Initializarea serviciilor de alarma, pin si senzor.
        alarmManager = new AlarmManager(this);
        pinManager = new PinManager(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Conectarea obiectelor de tip senzor din cod cu senzorii telefonului.
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    /**
     * Inregistreaza listeners pentru fiecare dintre senzori
     */
    private void registerSensors() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
        }
    }

    /**
     * Opreste ascultarea senzorilor pentru a economisi baterie.
     */
    private void unregisterSensors() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Daca nu este pornita monitorizara, nu se intampla nimic.
        if (!isMonitoring) return;
        
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            handleAccelerometer(event);
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
            handleGyroscope(event);
    }

    /**
     * Calculeaza forta de miscare pe cele 3 axe (x, y, z).
     */
    private void handleAccelerometer(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Calculeaza forta de miscare pentru senzorul de acceleratie, scazand din magnitudine acceleratia gravitationala.
        float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
        float motion = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);
        
        if (motion > THRESHOLD_ACCEL)
            triggerAlarm();
    }

    /**
     * Calculeaza viteza de rotatie, daca depaseste pragul, se activeaza alarma.
     */
    private void handleGyroscope(SensorEvent event) {
        float rotMagnitude = (float) Math.sqrt(event.values[0] * event.values[0] + 
                                              event.values[1] * event.values[1] + 
                                              event.values[2] * event.values[2]);
        if (rotMagnitude > THRESHOLD_GYRO)
            triggerAlarm();
    }

    private void triggerAlarm() {
        if (!alarmManager.isActive()) {
            alarmManager.startAlarm();
            showAlarmScreen();
        }
    }

    /**
     * Deschide activitatea de alarma care acopera ecranul.
     */
    private void showAlarmScreen() {
        Intent intent = new Intent(this, AlarmActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /**
     * Crearea canalului de notificare. (necesar pentru versiuni noi de Android).
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AntiTheft Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Creeaza notificarea care apare cand serviciul ruleaza in fundal.
     */
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Mod anti-furt activ")
                .setContentText("Senzorii monitorizează mișcarea telefonului.")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build();
    }

    /**
     * Gestionarea comenzilor primite de la restul aplicatiei.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        if (intent != null) {
            String action = intent.getAction();
            if ("START_MONITORING".equals(action)) {
                isMonitoring = true;
                pinManager.setMonitoring(true);
                registerSensors();
                // Face serviciul vizibil si greu de oprit de catre utilizator.
                startForeground(1, createNotification());
            } else if ("STOP_MONITORING".equals(action)) {
                isMonitoring = false;
                pinManager.setMonitoring(false);
                unregisterSensors();
                alarmManager.stopAlarm();
                stopForeground(true);
                // Notifica MainActivity ca activitatea s-a oprit pentru a putea modifica butoanele.
                sendBroadcast(new Intent(ACTION_STATE_CHANGED));
                stopSelf();
            } else if ("STOP_ALARM".equals(action)) {
                alarmManager.stopAlarm();
            }
        }
        return START_STICKY; // Reporneste automat serviciul daca este oprit.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterSensors();
        alarmManager.stopAlarm();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
