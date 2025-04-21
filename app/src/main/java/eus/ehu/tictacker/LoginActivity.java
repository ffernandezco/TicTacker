package eus.ehu.tictacker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private EditText editTextUsername, editTextPassword;
    private Button buttonLogin;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private TextView textViewSignupLink;
    private NetworkConnectivityChecker connectivityChecker;
    //private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectivityChecker = NetworkConnectivityChecker.getInstance(this);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        //executorService = Executors.newSingleThreadExecutor();

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);

        textViewSignupLink = findViewById(R.id.textViewSignupLink);

        textViewSignupLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });

        buttonLogin = findViewById(R.id.buttonLogin);

        buttonLogin.setOnClickListener(v -> {
            String username = editTextUsername.getText().toString();
            String password = editTextPassword.getText().toString();

            buttonLogin.setEnabled(false);

            // Comprobar si se cuenta con conexión a Internet
            if (!connectivityChecker.isConnected()) {
                Intent intent = new Intent(LoginActivity.this, NoInternetActivity.class);
                intent.putExtra("return_activity", LoginActivity.class.getName());
                startActivity(intent);
                return;
            }

            dbHelper.validarUsuario(username, password, new DatabaseHelper.BooleanCallback() {
                @Override
                public void onResult(boolean isValid) {
                    runOnUiThread(() -> {
                        buttonLogin.setEnabled(true);
                        if (isValid) {
                            sharedPreferences.edit().putString("usuario_actual", username).apply();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        });
    }

    // Revisar que se tenga Internet para conectar con la BD remota
    private void checkNetworkConnection() {
        connectivityChecker.setNetworkStateListener(new NetworkConnectivityChecker.NetworkStateListener() {
            @Override
            public void onNetworkStateChanged(boolean isConnected) {
                if (!isConnected) {
                    Intent intent = new Intent(LoginActivity.this, NoInternetActivity.class);
                    intent.putExtra("return_activity", LoginActivity.class.getName());
                    startActivity(intent);
                }
            }
        });
        connectivityChecker.register();

        // Comprobación inicial
        if (!connectivityChecker.isConnected()) {
            Intent intent = new Intent(this, NoInternetActivity.class);
            intent.putExtra("return_activity", LoginActivity.class.getName());
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //if (executorService != null) {
            //executorService.shutdown();
        //}
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkNetworkConnection();
    }

    @Override
    protected void onPause() {
        super.onPause();
        connectivityChecker.setNetworkStateListener(null);
        connectivityChecker.unregister();
    }
}