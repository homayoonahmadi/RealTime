package ir.programmerplus.realtime;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;
import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ProcessLifecycleOwner;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import ir.programmerplus.realtime.interfaces.EnhancedLocationListener;
import ir.programmerplus.realtime.interfaces.OnRealTimeInitializedListener;
import ir.programmerplus.realtime.network.NetworkState;
import ir.programmerplus.realtime.network.RetryDelayStrategy;
import ir.programmerplus.realtime.network.RetryWithDelay;
import ir.programmerplus.realtime.utils.CacheUtils;
import ir.programmerplus.realtime.utils.LogUtils;
import ir.programmerplus.realtime.utils.RealTimeUtils;

/**
 * Using RealTime class, you only need to initialize current reliable time once using multiple providers like
 * GPS providers, NTP servers or even date header of your own server, and use the reliable current time until
 * next boot of device.
 * <p/>
 * Author: Homayoon Ahmadi
 * <br/>
 * Email: homayoon.ahmadi8@gmail.com
 */
public class RealTime implements LifecycleEventObserver {

    private static final String TAG = RealTime.class.getSimpleName();

    private long backoffDelay;
    private boolean ntpServerEnabled = false;
    private boolean timeServerEnabled = false;
    private boolean gpsProviderEnabled = false;

    private final LinkedHashSet<String> ntpServerHosts = new LinkedHashSet<>();
    private final LinkedHashSet<String> timeServerHosts = new LinkedHashSet<>();

    private final Context context;
    private NTPUDPClient timeClient;
    private HttpURLConnection urlConnection;
    private final LocationManager locationManager;
    private final NetworkState networkStateLiveData;
    private OnRealTimeInitializedListener initializedListener;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private static final ObservableBoolean INITIALIZED = new ObservableBoolean();

    private static RealTime instance;


    /**
     * The constructor will initialize all needed classes
     *
     * @param context application context
     */
    private RealTime(Context context) {
        context = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? context.createDeviceProtectedStorageContext() : context;

        CacheUtils.initialize(context);

        this.context = context.getApplicationContext();
        this.networkStateLiveData = new NetworkState(context);
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        initRxJavaErrorHandler();
        initRealTimeStatusObservable();

        INITIALIZED.set(isInitialized());

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_START:
                LogUtils.i(TAG, "Application is in foreground");

                if (isInitialized() && cachedTimeIsValid(backoffDelay)) {
                    LogUtils.v(TAG, "RealTime cached time is valid. No need to resynchronize RealTime at this time.");
                } else {
                    LogUtils.v(TAG, "RealTime cached time is NOT valid. Trying to resynchronize RealTime...");
                    build();
                }
                break;

            case ON_STOP:
                LogUtils.i(TAG, "Application is in background");
                break;
        }
    }

    /**
     * This function will initialize a singleton instance of the RealTime class
     *
     * @param context application context
     * @return RealTime instance
     */
    public static RealTime builder(Context context) {

        synchronized (RealTime.class) {
            if (instance == null) {
                instance = new RealTime(context);
            }
        }

        return instance;
    }

    /**
     * This function will set an error handler for rxJava to catch Undeliverable Exceptions,
     * Otherwise, it will throw that kind of Exception
     */
    private void initRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler(throwable -> {
            if (!(throwable instanceof UndeliverableException)) {
                Thread thread = Thread.currentThread();

                Thread.UncaughtExceptionHandler exceptionHandler = thread.getUncaughtExceptionHandler();
                if (exceptionHandler != null)
                    exceptionHandler.uncaughtException(thread, throwable);
            }
        });
    }

    /**
     * This method will set an observable on RealTime initialize state
     * if class is not initialized or cached data are cleared, we try to
     * reinitialize the RealTime again.
     */
    private void initRealTimeStatusObservable() {

        INITIALIZED.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                LogUtils.v(TAG, "RealTime " + (INITIALIZED.get() ? "is" : "is NOT") + " initialized.");

                if (!INITIALIZED.get()) {
                    long cachedTime = CacheUtils.getCachedTime();
                    long cachedBootTime = CacheUtils.getCachedBootTime();
                    long cachedDeviceUptime = CacheUtils.getCachedDeviceUptime();

                    if (cachedTime == 0 || cachedBootTime == 0 || cachedDeviceUptime == 0) {
                        LogUtils.d(TAG, "Cached data are unavailable. Try to reinitialize RealTime...");
                        build();
                    }
                }
            }
        });
    }

    /**
     * This method will enable NTP server provider and set NTP server host
     *
     * @param ntpHost NTP server
     * @return RealTime instance
     */
    public RealTime withNtpServer(String ntpHost) {
        ntpServerHosts.add(ntpHost);
        ntpServerEnabled = true;
        return this;
    }

    /**
     * This method will enable Time server provider. Using this function, you
     * can get current server time using "Date" header of response.
     * <p>
     * Make sure the server you provide has a correct Date in response header.
     *
     * @param serverHost server url
     * @return RealTime instance
     */
    public RealTime withTimeServer(String serverHost) {
        this.timeServerHosts.add(serverHost);
        timeServerEnabled = true;
        return this;
    }

    /**
     * This method enables GPS provider. Using this function, you can get current time from GPS satellites.
     * <p>
     * You have to add either of {@link android.Manifest.permission#ACCESS_FINE_LOCATION} or
     * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} permissions in your manifest
     * and make sure to get permissions in runtime from the user.
     *
     * @return RealTime instance
     */
    public RealTime withGpsProvider() {
        // check if we have at least one of location permissions in the manifest
        if (!RealTimeUtils.manifestPermissionIsPresent(context, Manifest.permission.ACCESS_FINE_LOCATION) &&
                !RealTimeUtils.manifestPermissionIsPresent(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {

            throw new IllegalStateException("You need to add location permissions to your manifest.");
        }

        gpsProviderEnabled = true;
        return this;
    }

    /**
     * This function enables and disables logging information to logcat
     *
     * @param enabled sets weather logs must be enabled or not
     * @return RealTime instance
     */
    public RealTime setLoggingEnabled(boolean enabled) {
        LogUtils.setLoggingEnabled(enabled);

        return this;
    }

    /**
     * Sets the backoff delay for re-syncing RealTime with time providers.
     *
     * @param backoffDelay the duration of the backoff delay
     * @param unit         the unit of time for the backoff delay
     * @return the RealTime instance with the updated backoff delay
     * @throws NullPointerException if the unit parameter is null
     */
    public RealTime setSyncBackoffDelay(long backoffDelay, @NonNull TimeUnit unit) {
        this.backoffDelay = TimeUnit.MILLISECONDS.convert(backoffDelay, unit);

        return this;
    }

    /**
     * This function will set onInitializeListener and build the RealTime and starts
     * to sync with requested providers
     *
     * @param onInitializedListener listener to get notified when initialized
     */
    public void build(OnRealTimeInitializedListener onInitializedListener) {
        this.initializedListener = onInitializedListener;

        if (isInitialized() && onInitializedListener != null) {
            onInitializedListener.onInitialized(now());
        }

        build();
    }

    /**
     * This function will build the RealTime class and starts to sync time using
     * requested providers.
     */
    public void build() {

        LogUtils.v(TAG, "Starting to build RealTime...");

        if (gpsProviderEnabled) {
            requestLocationUpdates();
        }

        if (timeServerEnabled || ntpServerEnabled) {
            networkStateLiveData.observeForever(networkObserver);
        }
    }

    private boolean cachedTimeIsValid(long backoffDelay) {
        long cachedTime = CacheUtils.getCachedTime();
        long timeNow = now().getTime();

        return (timeNow - cachedTime) < backoffDelay;
    }

    /**
     * This method will show that RealTime class is initialized with a reliable time or not
     *
     * @return is RealTime initialized or not
     */
    public static boolean isInitialized() {
        return isCachedTimeValid();
    }

    /**
     * This function returns current reliable datetime if the class was initialized before
     *
     * @return current reliable date object
     * @throws IllegalStateException if the class is not initialized yet
     */
    public static Date now() throws IllegalStateException {
        if (!isInitialized()) {
            throw new IllegalStateException("You need to init RealTime at least once.");
        }

        long cachedTime = CacheUtils.getCachedTime();
        long cachedDeviceUptime = CacheUtils.getCachedDeviceUptime();
        long deviceUptime = SystemClock.elapsedRealtime();
        long now = cachedTime + (deviceUptime - cachedDeviceUptime);

        return new Date(now);
    }

    /**
     * This function requests headers from provided server url and extracts Date header from response.
     * We use RxJava to manage http request calls and use retry capability
     */
    private void requestTimeServer(String timeServerHost) {
        RetryWithDelay retryWithDelay = RetryWithDelay.builder()
                .retryDelayStrategy(RetryDelayStrategy.CONSTANT_DELAY_TIMES_RETRY_COUNT)
                .maxRetries(Integer.MAX_VALUE)
                .retryDelaySeconds(1)
                .mexDelaySeconds(30)
                .host(timeServerHost)
                .build();

        Disposable disposable = Single
                .fromCallable(() -> fetchTimeServer(timeServerHost))
                .retryWhen(retryWithDelay)
                .doOnDispose(() -> {
                    LogUtils.d(TAG, "Canceling request from time server...");

                    if (urlConnection != null)
                        urlConnection.disconnect();

                    LogUtils.d(TAG, "Time server request canceled.");
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setTime, throwable -> LogUtils.w(TAG, "Exception while requesting time from server: ", throwable));

        disposables.add(disposable);
    }

    /**
     * This function will fetch time from requests headers from provided server url
     * and extracts Date header from response.
     *
     * @return current time of server
     * @throws IOException    throws IOException if we couldn't connect to server
     * @throws ParseException throws ParseException if date header is not formed
     *                        in correct datetime format
     */
    private Long fetchTimeServer(String timeServerHost) throws IOException, ParseException {
        LogUtils.d(TAG, "Fetching time from time server: " + timeServerHost + " ...");

        try {
            URL url = new URL(timeServerHost);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(10 * 1000);
            Map<String, List<String>> headers = urlConnection.getHeaderFields();

            List<String> dateHeader = headers.get("date");

            if (dateHeader != null && !dateHeader.isEmpty()) {
                SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
                Date date = format.parse(dateHeader.get(0));

                if (date != null) {
                    LogUtils.i(TAG, "Time from " + timeServerHost + ": " + date);
                    return date.getTime();
                }
            }

        } catch (Exception e) {
            LogUtils.w(TAG, e.getClass().getCanonicalName() + ":" + e.getMessage());
            throw e;
        }

        throw new IOException();
    }

    /**
     * This function requests current time from provided NTP server using NTPUDPClient.
     * We use RxJava to manage http request calls and use retry capability
     */
    private void requestNtpTime(String ntpServerHost) {
        RetryWithDelay retryWithDelay = RetryWithDelay.builder()
                .retryDelayStrategy(RetryDelayStrategy.CONSTANT_DELAY_TIMES_RETRY_COUNT)
                .maxRetries(Integer.MAX_VALUE)
                .retryDelaySeconds(1)
                .mexDelaySeconds(30)
                .host(ntpServerHost)
                .build();

        Disposable disposable = Single
                .fromCallable(() -> fetchNtpTime(ntpServerHost))
                .retryWhen(retryWithDelay)
                .subscribeOn(Schedulers.io())
                .doOnDispose(() -> {
                    LogUtils.d(TAG, "Canceling Ntp request...");

                    if (timeClient != null && timeClient.isOpen())
                        timeClient.close();

                    LogUtils.d(TAG, "Ntp request canceled.");
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setTime, throwable -> LogUtils.w(TAG, "Exception while requesting Ntp time: ", throwable));

        disposables.add(disposable);
    }

    /**
     * This function will fetch time from NTP server url and gets date.
     *
     * @return current time we got from NTP server
     * @throws IOException throws IOException if we couldn't connect to server
     */
    private Long fetchNtpTime(String ntpServerHost) throws IOException {
        LogUtils.d(TAG, "Fetching time from Ntp server: " + ntpServerHost + " ...");

        timeClient = new NTPUDPClient();
        timeClient.setDefaultTimeout(10 * 1000);

        InetAddress inetAddress;

        try {
            inetAddress = InetAddress.getByName(ntpServerHost);
        } catch (UnknownHostException e) {
            LogUtils.w(TAG, e.getClass().getCanonicalName() + ":" + e.getMessage());
            throw e;
        }

        TimeInfo timeInfo = timeClient.getTime(inetAddress);
        long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();

        LogUtils.i(TAG, "Time from " + ntpServerHost + ": " + new Date(returnTime));

        try {
            timeClient.close();
        } catch (Exception ignored) {
        }

        return returnTime;
    }


    /**
     * In this function we request location updates if we have location permission and gps provider
     * is enabled by user.
     */
    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LogUtils.i(TAG, "Location provider is enabled.");
            }

            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, locationListener);
            LogUtils.d(TAG, "Requesting time from location provider...");
        }
    }

    /**
     * This function will check if we have a valid cached time
     *
     * @return true if cache is valid, false otherwise
     */
    private static boolean isCachedTimeValid() {
        long cachedBootTime = CacheUtils.getCachedBootTime();
        if (cachedBootTime == 0) {
            return false;
        }

        // has boot time changed (simple check)
        boolean bootTimeChanged = SystemClock.elapsedRealtime() < CacheUtils.getCachedDeviceUptime();
        return !bootTimeChanged;
    }


    /**
     * This function clears all cached data. We use this function to clear expired datetime
     * after reboot and try to reinitialize the RealTime.
     */
    public static void clearCachedInfo() {
        CacheUtils.setCachedTime(0L);
        CacheUtils.setCachedBootTime(0);
        CacheUtils.setCachedDeviceUptime(0);

        LogUtils.d(TAG, "RealTime disk cache cleared.");

        INITIALIZED.set(false);
    }

    /**
     * This function will set time and cache needed data to preferences.
     *
     * @param time reliable current time
     */
    @SuppressLint("MissingPermission")
    private void setTime(Long time) {
        if (time == null || time == 0) return;

        long deviceUptime = SystemClock.elapsedRealtime();
        long bootTime = time - deviceUptime;

        // write data to cache
        CacheUtils.setCachedTime(time);
        CacheUtils.setCachedBootTime(bootTime);
        CacheUtils.setCachedDeviceUptime(deviceUptime);

        // disable network connection state callback if exists
        if (networkStateLiveData != null) {
            networkStateLiveData.removeObserver(networkObserver);
        }

        // Unsubscribe from all network providers
        disposables.clear();

        // Unsubscribe from location providers
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
            LogUtils.d(TAG, "Location updates stopped.");
        }

        INITIALIZED.set(true);

        // populate results
        if (initializedListener != null)
            initializedListener.onInitialized(new Date(time));
    }

    /**
     * Here we implement a location listener to get date from location provider
     */
    private final LocationListener locationListener = new EnhancedLocationListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onLocationChanged(@NonNull Location location, long gpsTime) {
            LogUtils.i(TAG, "Time from location provider: " + new Date(gpsTime));

            setTime(gpsTime);

            if (locationManager != null) {
                locationManager.removeUpdates(this);
            }
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            LogUtils.v(TAG, "Location provider: " + provider + " is disabled.");
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            LogUtils.v(TAG, "Location provider enabled.");

            if (locationManager != null && !isInitialized()) {
                requestLocationUpdates();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // we need to implement this empty method to prevent crash on API 21 (Android 5)
        }
    };

    /**
     * Here we define a network callback to notify when network state changes. If we have a reliable
     * network, we try to request time from NTP or time servers.
     */
    Observer<Boolean> networkObserver = isConnected -> {
        if (isConnected) {
            LogUtils.i(TAG, "Network connection is available.");

            if (ntpServerEnabled) {
                for (String ntpServerHost : ntpServerHosts) {
                    requestNtpTime(ntpServerHost);
                }
            }

            if (timeServerEnabled) {
                for (String timeServerHost : timeServerHosts) {
                    requestTimeServer(timeServerHost);
                }
            }

        } else {
            LogUtils.i(TAG, "Network connection has lost.");

            disposables.clear();
        }
    };

}
