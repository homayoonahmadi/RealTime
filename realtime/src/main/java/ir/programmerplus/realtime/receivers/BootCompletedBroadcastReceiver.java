package ir.programmerplus.realtime.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import ir.programmerplus.realtime.RealTime;
import ir.programmerplus.realtime.utils.LogUtils;

public class BootCompletedBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = BootCompletedBroadcastReceiver.class.getSimpleName();

    private static final String ACTION_HTC_QUICK_BOOT = "com.htc.action.QUICKBOOT_POWERON";
    private static final String ACTION_QUICK_BOOT_POWER_ON = "android.intent.action.QUICKBOOT_POWERON";

    private static final String[] BOOT_ACTIONS = new String[]{
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_REBOOT,
            ACTION_HTC_QUICK_BOOT,
            ACTION_QUICK_BOOT_POWER_ON
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.i(TAG, "RealTime broadcast receiver called.");

        if (intent != null && intent.getAction() != null && intent.getAction().matches(TextUtils.join("|", BOOT_ACTIONS))) {

            LogUtils.i(TAG, "RealTime: We have detected a boot complete broadcast. Action: " + intent.getAction());
            RealTime.clearCachedInfo();
        }
    }
}