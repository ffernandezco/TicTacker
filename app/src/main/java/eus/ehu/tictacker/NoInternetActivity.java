package eus.ehu.tictacker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class NoInternetActivity extends AppCompatActivity implements NetworkConnectivityChecker.NetworkStateListener {

    private NetworkConnectivityChecker connectivityChecker;
    private String returnActivity;
    private Button retryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_internet);

        connectivityChecker = NetworkConnectivityChecker.getInstance(this);

        // Get the activity to return to when connection is available
        returnActivity = getIntent().getStringExtra("return_activity");
        if (returnActivity == null) {
            returnActivity = MainActivity.class.getName();
        }

        retryButton = findViewById(R.id.buttonRetry);
        retryButton.setOnClickListener(v -> checkConnection());
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectivityChecker.setNetworkStateListener(this);
        connectivityChecker.register();
    }

    @Override
    protected void onPause() {
        super.onPause();
        connectivityChecker.setNetworkStateListener(null);
        connectivityChecker.unregister();
    }

    private void checkConnection() {
        if (connectivityChecker.isConnected()) {
            returnToPreviousActivity();
        }
    }

    private void returnToPreviousActivity() {
        try {
            Class<?> activityClass = Class.forName(returnActivity);
            Intent intent = new Intent(this, activityClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } catch (ClassNotFoundException e) {
            // Fallback to MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onNetworkStateChanged(boolean isConnected) {
        if (isConnected) {
            returnToPreviousActivity();
        }
    }
}