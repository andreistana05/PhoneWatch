package com.example.phonewatch;

import android.media.AudioAttributes;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Gestionarea sistemului de alarma.
 * Se ocupa de redarea sunetului la maxim a telefonului si vibratiile.
 */
public class AlarmManager {
    private Context context;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private AudioManager audioManager;
    private int previousVolume;
    private boolean isAlarmActive = false;

    public AlarmManager(Context context){
        this.context = context;
        // Obtinem serviciile de sistem pentru a controla sunetul si vibratiile.
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    /**
     * Activeaza alarma: da volumul telefonului la maxim, porneste sunetul alarmei si vibratiile.
     */
    public void startAlarm(){
        if(isAlarmActive)
            return;
        isAlarmActive = true;
        
        setMaxVolume(); // Punem volumul la maxim
        startSound();   // Pornim sunetul alarmei
        startVibration(); // Pornim vibratiile.
    }

    /**
     * Opreste alarma si reseteaza volumul la cel setat initial de utilizator.
     */
    public void stopAlarm(){
        if (!isAlarmActive)
            return;
        isAlarmActive = false;
        
        stopSound();      // Oprim muzica
        stopVibration();  // Oprim vibratiile
        restoreVolume();  // Revenim la volumul pe care-l avea inainte de alarma utilizatorul.
    }

    public boolean isActive(){
        return isAlarmActive;
    }

    /**
     * Salveaza volumul curent al telefonului si seteaza volumul telefonului la maxim
     */
    private void setMaxVolume(){
        // Salvam volumul curent pentru a-l putea pune inapoi ulterior.
        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        // Obtinem volumul maxim permis de telefon.
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);

        // Setam volumul la maxim.
        audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                maxVolume,
                0
                );
    }

    /**
     * Revine la volumul pe care utilizatorul il avea inainte de alarma.
     */
    private void restoreVolume(){
        audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                previousVolume,
                0
        );
    }

    /**
     * Configureaza si porneste redarea fisierului audio de alarma.
     */
    private void startSound() {
        stopSound(); // Curatam orice instanta veche
        try {
            // Configurarea atributelor audio pentru versiuni noi de Android
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes aa = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();

                // Cream player-ul folosind fisierul media .wav.
                mediaPlayer = MediaPlayer.create(context, R.raw.alarm, aa, audioManager.generateAudioSessionId());
            } else {
                // Metoda pentru versiuni mai vechi de Android
                mediaPlayer = MediaPlayer.create(context, R.raw.alarm);
                if(mediaPlayer != null) {
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                }
            }
            
            if(mediaPlayer != null) {
                mediaPlayer.setLooping(true); // Alarma va suna continuu pana la introducerea pin-ului corect.
                mediaPlayer.start();
            } else {
                android.util.Log.e("AlarmManager", "Eroare: Nu s-a putut încărca sunetul de alarmă."); // Pentru debugging.
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Opreste redarea sunetului de alarma si elibereaza resursele de memorie
     */
    private void stopSound() {
        if(mediaPlayer != null) {
            try {
                if(mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception e) {
                // Player-ul era deja oprit
            } finally {
                mediaPlayer.release(); // Eliberam resursele hardware.
                mediaPlayer = null;
            }
        }
    }

    /**
     * Initializeaza un model de vibratie intermitent.
     */
    private void startVibration(){
        // Model: [pauză, vibrație, pauză, vibrație...] in milisecunde.
        long[] pattern = {0, 500, 300, 500, 300, 500};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            // 0 inseamna ca modelul se repeta de la inceput pana la infinit.
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        } else {
            vibrator.vibrate(pattern, 0);
        }
    }

    /**
     * Opreste vibratiile.
     */
    private void stopVibration(){
        if(vibrator != null) {
            vibrator.cancel();
        }
    }
}
