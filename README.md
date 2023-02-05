# RealTime
Reliable time library for Android. Just initialize the current time using one of several time providers like GPS, NTP servers, or your own server and get current reliable time impervious to device clock changes by the user until the next device boot.

# Introduction Video
https://user-images.githubusercontent.com/29772463/216837670-d856b038-bc81-4e70-8dcb-f737c36febe0.mp4

# Features
- RealTime provides 3 different time providers: 
  + Location providers [Using GPS] 
  + NTP servers [Network Time Protocol servers]
  + Your custom defined server [Using date header]
- RealTime detects device reboot and will reinitialize dateTime automatically after rebooting
- RealTime will detect network detection status changes and location provider ON/OFF changes and requests for current time if it has not initialized yet


# Notes
- If you want to use custom server you need to make sure that server's time is correct and reliable.
- If you are initializing shared preference inside your Application class, use this line instead of raw context instance:

```
Context context = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) ? createDeviceProtectedStorageContext() : getApplicationContext();
```
It will prevent crash after rebooting device because RealTime starts to initialize on direct boot mode event: ```android.intent.action.LOCKED_BOOT_COMPLETED```
- RealTime tries to get time using a retry with delay strategy if current network doesn't have internet connection yet.
- RealTime will not add location permissions to manifest automatically. If you want to use GPS provider, add required permissions to your manifest:

```
<manifest ... >
  <!-- Always include this permission -->
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

  <!-- Include only if your app benefits from precise location access. -->
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
</manifest>
```

