package ir.programmerplus.realtime;

import android.util.Log;

abstract class LogUtils {

    private static boolean loggingEnabled = true;

    static void setLoggingEnabled(boolean isLoggingEnabled) {
        loggingEnabled = isLoggingEnabled;
    }

    static void v(String tag, String msg) {
        if (loggingEnabled) {
            Log.v(tag, msg);
        }
    }

    static void d(String tag, String msg) {
        if (loggingEnabled) {
            Log.d(tag, msg);
        }
    }

    static void i(String tag, String msg) {
        if (loggingEnabled) {
            Log.i(tag, msg);
        }
    }

    static void w(String tag, String msg) {
        if (loggingEnabled) {
            Log.w(tag, msg);
        }
    }

    static void w(String tag, String msg, Throwable t) {
        if (loggingEnabled) {
            Log.w(tag, msg, t);
        }
    }

    static void e(String tag, String msg) {
        if (loggingEnabled) {
            Log.e(tag, msg);
        }
    }

    static void e(String tag, String msg, Throwable t) {
        if (loggingEnabled) {
            Log.e(tag, msg, t);
        }
    }
}
