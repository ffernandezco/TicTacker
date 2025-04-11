package eus.ehu.tictacker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {
    private EditText editTextUsername, editTextPassword, editTextConfirmPassword;
    private Button buttonSignup;
    private TextView textViewLoginLink;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        dbHelper = new DatabaseHelper(this);

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
                if (dbHelper.addUser(username, password)) {
                    Toast.makeText(SignupActivity.this, getString(R.string.registro_exitoso), Toast.LENGTH_SHORT).show();
                    // Redirigir al login
                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(SignupActivity.this, getString(R.string.el_nombre_de_usuario_ya_existe), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Link para volver a login
        textViewLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
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