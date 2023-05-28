package ir.programmerplus.realtime_example;


import android.util.Log;

import java.util.concurrent.TimeUnit;

import androidx.multidex.MultiDexApplication;
import ir.programmerplus.realtime.RealTime;

public class Application extends MultiDexApplication {

    private static final String TAG = Application.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        RealTime.builder(this)
                .withGpsProvider()
                .withNtpServer("time.nist.gov")
                .withNtpServer("time.google.com")
                .withNtpServer("time.windows.com")
                .withTimeServer("https://time.ir")
                .withTimeServer("https://google.com")
                .setLoggingEnabled(BuildConfig.DEBUG)
                .setSyncBackoffDelay(30, TimeUnit.SECONDS)
                .build(date -> Log.d(TAG, "RealTime is initialized, current dateTime: " + date));
    }
}
