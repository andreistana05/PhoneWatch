package com.example.phonewatch;

import android.media.AudioAttributes;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
public class AlarmManager {
    private Context context;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private AudioManager audioManager;
    private int previousVolume;
    private boolean isAlarmActive = false;

    public AlarmManager(Context context){
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
    public void startAlarm(){
        if(isAlarmActive)
            return;
        isAlarmActive = true;
        setMaxVolume();
        startSound();
        startVibration();
    }
    public void stopAlarm(){
        if (!isAlarmActive)
            return;
        isAlarmActive = false;
        stopSound();
        stopVibration();
        restoreVolume();
    }
    public boolean isActive(){
        return isAlarmActive;
    }
    private void setMaxVolume(){
        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);

        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);

        audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                maxVolume,
                0
                );
    }
    private void restoreVolume(){
        audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                previousVolume,
                0
        );
    }

    private void startSound() {
        stopSound();
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes aa = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();

                mediaPlayer = MediaPlayer.create(context, R.raw.alarm, aa, audioManager.generateAudioSessionId());
            } else {
                mediaPlayer = MediaPlayer.create(context, R.raw.alarm);
                if(mediaPlayer != null) {
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                }
            }
            if(mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            } else {
                android.util.Log.e("AlarmManager", "Nu s-a putut crea MediaPlayer");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopSound() {
        if(mediaPlayer != null) {
            try {
                if(mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception e) {

            } finally {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }

    private void startVibration(){
        long[] pattern = {0, 500, 300, 500, 300, 500};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        } else {
            vibrator.vibrate(pattern, 0);
        }
    }

    private void stopVibration(){
        if(vibrator != null) {
            vibrator.cancel();
        }
    }
}
