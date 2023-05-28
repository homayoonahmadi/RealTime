package ir.programmerplus.realtime.interfaces;

import android.location.Location;
import android.location.LocationListener;

import androidx.annotation.NonNull;

public abstract class EnhancedLocationListener implements LocationListener {

    public abstract void onLocationChanged(@NonNull Location location, long timeInMs);

    @Override
    public final void onLocationChanged(@NonNull Location location) {
        long gpsTime = location.getTime();

        // Adding 1024 weeks to fix Week Number Rollover issue for old GPS chips
        // 619315200000L = 1024 * 7 * 24 * 60 * 60 * 1000   ->  means 1024 weeks
        // Note: This fix works for next 20 years since provided timestamp below
        // See: https://stackoverflow.com/questions/56147606/
        // See: https://www.cisa.gov/gps-week-number-roll-over
        if (gpsTime > 0 && gpsTime < 1673000000000L)
            gpsTime += 619315200000L;

        location.setTime(gpsTime);

        onLocationChanged(location, gpsTime);
    }
}
