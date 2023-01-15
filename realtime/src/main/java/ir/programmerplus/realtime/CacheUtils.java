package ir.programmerplus.realtime;

import android.content.Context;
import android.content.SharedPreferences;

public abstract class CacheUtils {

    private static final String SHARED_PREF_NAME = "RealTimePreference";

    private static final String KEY_CACHED_TIME = BuildConfig.LIBRARY_PACKAGE_NAME + ".cached_time";
    private static final String KEY_CACHED_BOOT_TIME = BuildConfig.LIBRARY_PACKAGE_NAME + ".cached_boot_time";
    private static final String KEY_CACHED_DEVICE_UPTIME = BuildConfig.LIBRARY_PACKAGE_NAME + ".cached_device_uptime";

    private static SharedPreferences sharedPreferences;

    /**
     * This function will initialize sharedPreferences static variable with provided context
     *
     * @param context application context
     */
    public static void initialize(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public static long getCachedTime() {
        return sharedPreferences.getLong(KEY_CACHED_TIME, 0);
    }

    public static long getCachedDeviceUptime() {
        return sharedPreferences.getLong(KEY_CACHED_DEVICE_UPTIME, 0);
    }

    public static long getCachedBootTime() {
        return sharedPreferences.getLong(KEY_CACHED_BOOT_TIME, 0);
    }

    public static void setCachedTime(long time) {
        sharedPreferences.edit().putLong(KEY_CACHED_TIME, time).apply();
    }

    public static void setCachedDeviceUptime(long deviceUptime) {
        sharedPreferences.edit().putLong(KEY_CACHED_DEVICE_UPTIME, deviceUptime).apply();
    }

    public static void setCachedBootTime(long bootTime) {
        sharedPreferences.edit().putLong(KEY_CACHED_BOOT_TIME, bootTime).apply();
    }
}
