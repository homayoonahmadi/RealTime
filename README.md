# RealTime

[![platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com)
[![API](https://img.shields.io/badge/API-16%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=16)
[![Jitpack](https://jitpack.io/v/homayoonahmadi/RealTime.svg)](https://jitpack.io/#homayoonahmadi/RealTime)

RealTime is a reliable time library for Android. Just initialize the current time using one of several time providers like GPS, NTP servers, or your own server and get current reliable time impervious to device clock changes by the user until the next device boot.

# Introduction Video
https://user-images.githubusercontent.com/29772463/216837670-d856b038-bc81-4e70-8dcb-f737c36febe0.mp4

# Features
- RealTime provides 3 different time providers: 
  + Location providers [Using GPS] 
  + NTP servers [Network Time Protocol servers]
  + Your custom-defined server [Using date header]
- RealTime detects device reboot and will reinitialize dateTime automatically after rebooting
- RealTime will detect network detection status changes and location provider ON/OFF changes and requests for the current time if it has not been initialized yet


# How to add dependency:

Step 1. Add the JitPack repository to your build.gradle file

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Step 2. Add the dependency

```groovy
dependencies {
    implementation 'com.github.homayoonahmadi:RealTime:1.3.0'
}
```

# How to use
Add this to `onCreate` method of your `Application` class:

```
RealTime.builder(this)
      .withGpsProvider()
      .withNtpServer("time.nist.gov")
      .withNtpServer("time.google.com")
      .withNtpServer("time.windows.com")
      .withTimeServer("https://bing.com")
      .withTimeServer("https://google.com")
      .setLoggingEnabled(BuildConfig.DEBUG)
      .setSyncBackoffDelay(30, TimeUnit.SECONDS)
      .build(date -> Log.d(TAG, "RealTime is initialized, current dateTime: " + date));
```

Then everywhere you need reliable time first you need to check if RealTime is initialized or not:

```
if (RealTime.isInitialized()) {
    // Get current dateTime:
    Date  = RealTime.now();

    // You can show it in a text view
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMMM yyyy HH:mm:ss z", Locale.ENGLISH);
    binding.txtDateTime.setText(simpleDateFormat.format(date));
} else {
    binding.txtDateTime.setText("RealTime is not initialized yet.");
}
```

# Notes
- If you want to use a custom server, ensure the server's time is correct and reliable.
- RealTime tries to get time using a retry with delay strategy if the current network doesn't have an internet connection yet.
- RealTime will not add location permissions to manifest automatically. If you want to use a GPS provider, add the required permissions to your manifest:

```
<manifest ... >
  <!-- Always include this permission -->
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

  <!-- Include only if your app benefits from precise location access. -->
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
</manifest>
```

NTP servers tested:
```
time.nist.gov
time-a.nist.gov
time.google.com
time.windows.com
1.us.pool.ntp.org
ir.pool.ntp.org
```

# Methods

+ **RealTime class methods**

| method                                        | description                                                                                                                     |
|-----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| withNtpServer(String ntpHost)                 | This function will enable and set the URL of the NTP server.                                                                    |
| withTimeServer(String serverHost)             | This function will enable and set the URL of the custom server.                                                                 |
| withGpsProvider()                             | This function enables gps provider if required permissions exist in the manifest.                                               |
| setLoggingEnabled(boolean enabled)            | Sets if logs need to be logged in.                                                                                              |
| build()                                       | Starts to initialize RealTime using enabled providers.                                                                          |
| build(OnRealTimeInitializedListener listener) | Starts to initialize RealTime using enabled providers and will call onInitializedListener's onInitialized(Date date) interface. |
| isInitialized()                               | Returns true if RealTime is initialized or false otherwise.                                                                     |
| now()                                         | Returns current reliable dateTime if the class has initialized.                                                                 |
| clearCachedInfo()                             | This function clears all cached data so RealTime tries to initialize dateTime again.                                            |
