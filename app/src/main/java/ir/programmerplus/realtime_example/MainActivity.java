package ir.programmerplus.realtime_example;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;

import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import ir.programmerplus.realtime.RealTime;
import ir.programmerplus.realtime_example.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestLocationPermission();

        // set click listener to clear cached info when button clicked
        binding.btnClearCache.setOnClickListener(v -> RealTime.clearCachedInfo());

        createDateTimer();
    }

    /**
     * This function will create a count down timer to show current datetime based on realtime reliable time
     */
    @SuppressLint("SetTextI18n")
    private void createDateTimer() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMMM yyyy HH:mm:ss z", Locale.ENGLISH);

        countDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

                if (RealTime.isInitialized()) {
                    binding.txtDateTime.setText(simpleDateFormat.format(RealTime.now()));
                } else {
                    binding.txtDateTime.setText("RealTime is not initialized yet.");
                }
            }

            @Override
            public void onFinish() {
            }
        };
        countDownTimer.start();
    }

    /**
     * This function requests for location permission to get time from Gps provider
     */
    private void requestLocationPermission() {
        // request for location permissions
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // stop counter
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}