package eus.ehu.tictacker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignupActivity extends AppCompatActivity {
    private EditText editTextUsername, editTextPassword, editTextConfirmPassword;
    private Button buttonSignup;
    private TextView textViewLoginLink;
    private DatabaseHelper dbHelper;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonSignup = findViewById(R.id.buttonSignup);
        textViewLoginLink = findViewById(R.id.textViewLoginLink);

        buttonSignup.setOnClickListener(v -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

            if (validateInputs(username, password, confirmPassword)) {
                buttonSignup.setEnabled(false);

                executorService.execute(() -> {
                    final boolean success = dbHelper.addUser(username, password);
                    runOnUiThread(() -> {
                        buttonSignup.setEnabled(true);
                        if (success) {
                            Toast.makeText(SignupActivity.this, getString(R.string.registro_exitoso), Toast.LENGTH_SHORT).show();
                            SharedPreferences sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
                            sharedPreferences.edit().putString("usuario_actual", username).apply();

                            // Dialog para ompletar perfil
                            new AlertDialog.Builder(SignupActivity.this)
                                    .setTitle(R.string.complete_profile)
                                    .setMessage(R.string.complete_profile_prompt)
                                    .setPositiveButton(R.string.now, (dialog, which) -> {
                                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                                        intent.putExtra("openProfile", true);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .setNegativeButton(R.string.later, (dialog, which) -> {
                                        startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                        finish();
                                    })
                                    .show();
                        } else {
                            Toast.makeText(SignupActivity.this, getString(R.string.el_nombre_de_usuario_ya_existe), Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        });

        // Link to go back to login
        textViewLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private boolean validateInputs(String username, String password, String confirmPassword) {
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, getString(R.string.todos_los_campos_son_obligatorios), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 4) {
            editTextPassword.setError(getString(R.string.error_en_el_registro));
            return false;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError(getString(R.string.las_contrase_as_no_coinciden));
            return false;
        }

        return true;
    }
}