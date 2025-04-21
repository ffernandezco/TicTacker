package eus.ehu.tictacker;

import android.app.DatePickerDialog;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignupActivity extends AppCompatActivity {
    private EditText editTextUsername, editTextPassword, editTextConfirmPassword;
    private EditText editTextName, editTextEmail, editTextBirthdate;
    private Button buttonSignup;
    private TextView textViewLoginLink;
    private DatabaseHelper dbHelper;
    private NetworkConnectivityChecker connectivityChecker;
    private ExecutorService executorService;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connectivityChecker = NetworkConnectivityChecker.getInstance(this);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        setContentView(R.layout.activity_signup);

        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();

        // Campos de cuenta de usuario
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);

        // Campos de información personal
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextBirthdate = findViewById(R.id.editTextBirthdate);

        buttonSignup = findViewById(R.id.buttonSignup);
        textViewLoginLink = findViewById(R.id.textViewLoginLink);

        // Configurar selector de fechas
        editTextBirthdate.setOnClickListener(v -> showDatePickerDialog());

        buttonSignup.setOnClickListener(v -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();
            String name = editTextName.getText().toString().trim();
            String email = editTextEmail.getText().toString().trim();
            String birthdate = editTextBirthdate.getText().toString().trim();

            if (!connectivityChecker.isConnected()) {
                Intent intent = new Intent(SignupActivity.this, NoInternetActivity.class);
                intent.putExtra("return_activity", SignupActivity.class.getName());
                startActivity(intent);
                return;
            }

            if (validateInputs(username, password, confirmPassword, name, email, birthdate)) {
                buttonSignup.setEnabled(false);

                // Registrar usuario y crear perfil
                dbHelper.addUser(username, password, new DatabaseHelper.BooleanCallback() {
                    @Override
                    public void onResult(boolean success) {
                        if (success) {
                            // Crear perfil con la información
                            UserProfile profile = new UserProfile();
                            profile.username = username;
                            profile.name = name;
                            profile.email = email;
                            profile.birthdate = birthdate;

                            dbHelper.updateProfile(profile, new DatabaseHelper.BooleanCallback() {
                                @Override
                                public void onResult(boolean profileSuccess) {
                                    runOnUiThread(() -> {
                                        buttonSignup.setEnabled(true);
                                        if (profileSuccess) {
                                            Toast.makeText(SignupActivity.this, getString(R.string.registro_exitoso), Toast.LENGTH_SHORT).show();

                                            // Guardar datos del usuario actual
                                            SharedPreferences sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
                                            sharedPreferences.edit().putString("usuario_actual", username).apply();

                                            // Redirigir a mainactivity
                                            startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(SignupActivity.this, getString(R.string.error_saving_profile), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            });
                        } else {
                            runOnUiThread(() -> {
                                buttonSignup.setEnabled(true);
                                Toast.makeText(SignupActivity.this, getString(R.string.el_nombre_de_usuario_ya_existe), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            }
        });

        // Volver al inicio de sesión
        textViewLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    // Mostrar selector de fechas para la fecha de nacimiento
    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();

        // Poner, por defecto, la edad mínima
        calendar.add(Calendar.YEAR, -18);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(selectedYear, selectedMonth, selectedDay);
                    editTextBirthdate.setText(dateFormat.format(calendar.getTime()));
                },
                year, month, day);

        // Configurar, como máximo, la fecha actual
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        datePickerDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
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

    private void checkNetworkConnection() {
        connectivityChecker.setNetworkStateListener(new NetworkConnectivityChecker.NetworkStateListener() {
            @Override
            public void onNetworkStateChanged(boolean isConnected) {
                if (!isConnected) {
                    Intent intent = new Intent(SignupActivity.this, NoInternetActivity.class);
                    intent.putExtra("return_activity", SignupActivity.class.getName());
                    startActivity(intent);
                }
            }
        });
        connectivityChecker.register();

        // Comprobación inicial
        if (!connectivityChecker.isConnected()) {
            Intent intent = new Intent(this, NoInternetActivity.class);
            intent.putExtra("return_activity", SignupActivity.class.getName());
            startActivity(intent);
        }
    }

    // Validación de cuenta
    private boolean validateInputs(String username, String password, String confirmPassword,
                                   String name, String email, String birthdate) {
        // Validación de cuenta
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, getString(R.string.todos_los_campos_son_obligatorios), Toast.LENGTH_SHORT).show();
            return false;
        }

        // 4+ carácteres en la contraseña
        if (password.length() < 4) {
            editTextPassword.setError(getString(R.string.error_en_el_registro));
            return false;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError(getString(R.string.las_contrase_as_no_coinciden));
            return false;
        }

        // Validación de campos
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(birthdate)) {
            Toast.makeText(this, getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validar formato del email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError(getString(R.string.invalid_email));
            return false;
        }

        // Validación > 18 años
        try {
            Date birthdateDate = dateFormat.parse(birthdate);
            Calendar dob = Calendar.getInstance();
            dob.setTime(birthdateDate);

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

            if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH) ||
                    (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) &&
                            today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
                age--;
            }

            if (age < 18) {
                editTextBirthdate.setError(getString(R.string.must_be_adult));
                return false;
            }
        } catch (ParseException e) {
            editTextBirthdate.setError(getString(R.string.invalid_date));
            return false;
        }

        return true;
    }
}