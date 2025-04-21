package eus.ehu.tictacker;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;

public class NetworkConnectivityChecker {

    private static NetworkConnectivityChecker instance;
    private final ConnectivityManager connectivityManager;
    private NetworkCallback networkCallback;
    private boolean isConnected = false;

    public interface NetworkStateListener {
        void onNetworkStateChanged(boolean isConnected);
    }

    private NetworkStateListener listener;

    private NetworkConnectivityChecker(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        checkInitialConnectionState();
    }

    public static synchronized NetworkConnectivityChecker getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkConnectivityChecker(context.getApplicationContext());
        }
        return instance;
    }

    // Listener para notificar en caso de no tener Internet
    public void setNetworkStateListener(NetworkStateListener listener) {
        this.listener = listener;
        if (listener != null) {
            listener.onNetworkStateChanged(isConnected);
        }
    }

    public void register() {
        if (networkCallback == null) {
            networkCallback = new NetworkCallback();
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }
    }

    public void unregister() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    // Comprobación inicial de conexión a Internet
    private void checkInitialConnectionState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                isConnected = capabilities != null &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            } else {
                isConnected = false;
            }
        } else {
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            isConnected = networkInfo != null && networkInfo.isConnected();
        }
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(@NonNull Network network) {
            isConnected = true;
            if (listener != null) {
                listener.onNetworkStateChanged(true);
            }
        }

        // Si se pierde la conexión
        @Override
        public void onLost(@NonNull Network network) {
            isConnected = false;
            if (listener != null) {
                listener.onNetworkStateChanged(false);
            }
        }
    }
}