package ir.programmerplus.realtime_example;


import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.multidex.MultiDexApplication;
import ir.programmerplus.realtime.RealTime;

public class Application extends MultiDexApplication {

    private static final String TAG = Application.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        Context context = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? createDeviceProtectedStorageContext() : this;

        RealTime.builder(context)
                .withGpsProvider()
                .withNtpServer("time.nist.gov")
                .withTimeServer("https://google.com")
                .setLoggingEnabled(BuildConfig.DEBUG)
                .build(date -> Log.d(TAG , "RealTime initialized, dateTime: " + date));
    }
}
