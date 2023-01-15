package ir.programmerplus.realtime_example;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import ir.programmerplus.realtime.RealTime;
import ir.programmerplus.realtime_example.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // request for location permissions
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);

        // set click listener to clear cached info when button clicked
        binding.btnClearCache.setOnClickListener(v -> RealTime.clearCachedInfo());

        // create a timer to show current time
        CountDownTimer cdt = new CountDownTimer(Long.MAX_VALUE, 1000) {

            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {

                if (RealTime.isInitialized()) {
                    binding.txtDateTime.setText(RealTime.now().toString());
                } else {
                    binding.txtDateTime.setText("RealTime is Not initialized yet.");
                }
            }

            @Override
            public void onFinish() {
            }
        };
        cdt.start();
    }
}