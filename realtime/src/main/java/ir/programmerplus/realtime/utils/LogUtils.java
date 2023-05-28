package ir.programmerplus.realtime.utils;

import android.util.Log;

@SuppressWarnings("unused")
public abstract class LogUtils {

    private static boolean loggingEnabled = true;

    public static void setLoggingEnabled(boolean isLoggingEnabled) {
        loggingEnabled = isLoggingEnabled;
    }

    public static void v(String tag, String msg) {
        if (loggingEnabled) {
            Log.v(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (loggingEnabled) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (loggingEnabled) {
            Log.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (loggingEnabled) {
            Log.w(tag, msg);
        }
    }

    public static void w(String tag, String msg, Throwable t) {
        if (loggingEnabled) {
            Log.w(tag, msg, t);
        }
    }

    public static void e(String tag, String msg) {
        if (loggingEnabled) {
            Log.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable t) {
        if (loggingEnabled) {
            Log.e(tag, msg, t);
        }
    }
}
