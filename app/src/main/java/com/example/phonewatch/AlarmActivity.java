package com.example.phonewatch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
public class AlarmActivity extends Activity{
    private static final String CORRECT_PIN = "1234";
    private AlarmManager alarmManager;
    private EditText pinInput;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        setContentView(R.layout.activity_alarm);

        alarmManager = new AlarmManager(this);
        pinInput = findViewById(R.id.pin_input);

        Button btnStop = findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(v -> checkPin());
    }

    private void checkPin(){
        String entered = pinInput.getText().toString().trim();

        if (entered.equals(CORRECT_PIN)){
            Intent stopIntent = new Intent(this, AntiTheftService.class);
            stopIntent.setAction("STOP_ALARM");
            startService(stopIntent);
            finish();
        } else {
            Toast.makeText(this, "PIN incorect!", Toast.LENGTH_SHORT).show();
            pinInput.setText("");
        }
    }
}
