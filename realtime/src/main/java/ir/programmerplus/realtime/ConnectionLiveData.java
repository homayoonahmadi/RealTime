package ir.programmerplus.realtime;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;

public class ConnectionLiveData extends LiveData<Boolean> {

    private final Context context;
    private final ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback connectivityManagerCallback;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private final NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI);


    public ConnectionLiveData(Context context) {
        this.context = context;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    protected void onActive() {
        super.onActive();

        updateConnection();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) connectivityManager.registerDefaultNetworkCallback(getConnectivityMarshmallowManagerCallback());
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) marshmallowNetworkAvailableRequest();
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) lollipopNetworkAvailableRequest();
        else {
            context.registerReceiver(networkReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        }
    }

    @Override
    protected void onInactive() {
        super.onInactive();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityManager.unregisterNetworkCallback(connectivityManagerCallback);
        } else {
            context.unregisterReceiver(networkReceiver);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void lollipopNetworkAvailableRequest() {
        connectivityManager.registerNetworkCallback(networkRequestBuilder.build(), getConnectivityLollipopManagerCallback());
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void marshmallowNetworkAvailableRequest() {
        connectivityManager.registerNetworkCallback(networkRequestBuilder.build(), getConnectivityMarshmallowManagerCallback());
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private ConnectivityManager.NetworkCallback getConnectivityLollipopManagerCallback() {
        connectivityManagerCallback = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(@NonNull Network network) {
                postValue(true);
            }

            @Override
            public void onLost(@NonNull Network network) {
                postValue(false);
            }

        };

        return connectivityManagerCallback;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private ConnectivityManager.NetworkCallback getConnectivityMarshmallowManagerCallback() {

        connectivityManagerCallback = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                ) {
                    postValue(true);
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                postValue(false);
            }
        };

        return connectivityManagerCallback;
    }

    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateConnection();
        }
    };

    private void updateConnection() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null) {
            postValue(activeNetwork.isConnected());
        }
    }

    @Override
    protected void postValue(Boolean value) {
        if (value != getValue())
            super.postValue(value);
    }
}
