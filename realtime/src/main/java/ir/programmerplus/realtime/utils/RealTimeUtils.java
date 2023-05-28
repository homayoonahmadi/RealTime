package ir.programmerplus.realtime.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public abstract class RealTimeUtils {

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean manifestPermissionIsPresent(Context context, String permission) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] permissions = packageInfo.requestedPermissions;

            if (permissions == null || permissions.length == 0)
                return false;

            for (String mPermission : permissions) {
                if (permission.equals(mPermission))
                    return true;
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }
}
