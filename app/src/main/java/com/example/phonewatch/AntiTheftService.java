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


public class AntiTheftService extends Service implements SensorEventListener{

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    private AlarmManager alarmManager;
    private static final float THRESHOLD_ACCEL = 2.5f; //pragul de miscare (m/s^2)
    private static final float THRESHOLD_GYRO = 0.8f; //pragul de rotatie initial (rad/s)

    @Override
    public void onCreate() {
        super.onCreate();
        alarmManager = new AlarmManager(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if(accelerometer != null){
            sensorManager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI
            );
        }

        if(gyroscope != null){
            sensorManager.registerListener(
                    this,
                    gyroscope,
                    SensorManager.SENSOR_DELAY_UI
            );
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            handleAccelerometer(event);
        if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
            handleGyroscope(event);
    }

    private void handleAccelerometer(SensorEvent event){
        float x = event.values[0]; //acceleratie pe axa x (stanga/dreapta)
        float y = event.values[1]; //acceleratie pe axa y (fata/spate)
        float z = event.values[2]; //acceleratie pe axa z (sus/jos)

        float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
        float motion = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);
        if (motion > THRESHOLD_ACCEL)
            triggerAlarm();
    }

    private void handleGyroscope(SensorEvent event){
        float rotX = event.values[0]; //rotatie in jurul axei X
        float rotY = event.values[1]; //rotatie in jurul axei Y
        float rotZ = event.values[2]; //rotatie in jurul axei Z

        float rotMagnitude = (float) Math.sqrt(rotX * rotX + rotY * rotY + rotZ * rotZ);
        if(rotMagnitude > THRESHOLD_GYRO)
            triggerAlarm();
    }
    private void triggerAlarm(){
        if(!alarmManager.isActive()){
            alarmManager.startAlarm();
            showAlarmScreen();
        }
    }

    private void showAlarmScreen(){
        Intent intent = new Intent(this, AlarmActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        sensorManager.unregisterListener(this);
        alarmManager.stopAlarm();
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
