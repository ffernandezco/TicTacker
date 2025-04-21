package eus.ehu.tictacker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private Spinner spinnerLanguage;
    private TextView tvHoursValue, tvMinutesValue;
    private Button btnIncreaseHours, btnDecreaseHours;
    private Button btnIncreaseMinutes, btnDecreaseMinutes;
    private Button btnSave;
    private Button btnDeleteHistory;
    private DatabaseHelper dbHelper;

    private ToggleButton toggleMonday, toggleTuesday, toggleWednesday,
            toggleThursday, toggleFriday, toggleSaturday, toggleSunday;
    private List<ToggleButton> dayToggles;

    private int selectedHours = 40;
    private int selectedMinutes = 0;
    private ToggleButton toggleReminder;
    private Button btnIncreaseReminderHour, btnDecreaseReminderHour;
    private Button btnIncreaseReminderMinute, btnDecreaseReminderMinute;
    private TextView tvReminderHourValue, tvReminderMinuteValue;
    private boolean reminderEnabled = false;
    private int reminderHour = 9;
    private int reminderMinute = 0;

    private static final int MAX_HOURS = 168;
    private static final int MIN_HOURS = 0;
    private static final int MAX_MINUTES = 55;
    private static final int MIN_MINUTES = 0;
    private static final int MINUTE_INCREMENT = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);

        spinnerLanguage = findViewById(R.id.spinnerLanguage);

        tvHoursValue = findViewById(R.id.tvHoursValue);
        tvMinutesValue = findViewById(R.id.tvMinutesValue);
        btnIncreaseHours = findViewById(R.id.btnIncreaseHours);
        btnDecreaseHours = findViewById(R.id.btnDecreaseHours);
        btnIncreaseMinutes = findViewById(R.id.btnIncreaseMinutes);
        btnDecreaseMinutes = findViewById(R.id.btnDecreaseMinutes);

        toggleReminder = findViewById(R.id.toggleReminder);
        tvReminderHourValue = findViewById(R.id.tvReminderHourValue);
        tvReminderMinuteValue = findViewById(R.id.tvReminderMinuteValue);
        btnIncreaseReminderHour = findViewById(R.id.btnIncreaseReminderHour);
        btnDecreaseReminderHour = findViewById(R.id.btnDecreaseReminderHour);
        btnIncreaseReminderMinute = findViewById(R.id.btnIncreaseReminderMinute);
        btnDecreaseReminderMinute = findViewById(R.id.btnDecreaseReminderMinute);

        setupReminderControls();

        btnDeleteHistory = findViewById(R.id.btnDeleteHistory);

        btnDeleteHistory.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });

        btnSave = findViewById(R.id.btnSaveSettings);

        initializeDayToggles();

        String[] languages = {"Español", "English"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(adapter);

        loadSavedSettings();

        setupNumberPickers();

        btnSave.setOnClickListener(v -> {
            String selectedLanguage = spinnerLanguage.getSelectedItemPosition() == 0 ? "es" : "en";
            float weeklyHours = selectedHours + (selectedMinutes / 60.0f);
            int workingDays = countSelectedDays();

            if (weeklyHours <= 0 || weeklyHours > 168 || workingDays <= 0 || workingDays > 7) {
                Toast.makeText(this, getString(R.string.invalid_settings), Toast.LENGTH_SHORT).show();
                return;
            }

            saveLanguagePreference(selectedLanguage);
            dbHelper.saveSettings(weeklyHours, workingDays, reminderEnabled, reminderHour, reminderMinute, success -> {
                if (success) {
                    setAppLocale(selectedLanguage);
                    restartApp();
                } else {
                    Toast.makeText(this, R.string.settings_save_error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // Configura el selector de horas y minutos a la semana
    private void setupNumberPickers() {
        btnIncreaseHours.setOnClickListener(v -> {
            if (selectedHours < MAX_HOURS) {
                selectedHours++;
                updateHoursDisplay();
            }
        });

        btnDecreaseHours.setOnClickListener(v -> {
            if (selectedHours > MIN_HOURS) {
                selectedHours--;
                updateHoursDisplay();
            }
        });

        // Control de minutos
        btnIncreaseMinutes.setOnClickListener(v -> {
            selectedMinutes += MINUTE_INCREMENT;
            if (selectedMinutes > MAX_MINUTES) {
                selectedMinutes = MIN_MINUTES;
                // Si los minutos exceden 60, sumar hora
                if (selectedHours < MAX_HOURS) {
                    selectedHours++;
                    updateHoursDisplay();
                }
            }
            updateMinutesDisplay();
        });

        btnDecreaseMinutes.setOnClickListener(v -> {
            selectedMinutes -= MINUTE_INCREMENT;
            if (selectedMinutes < MIN_MINUTES) {
                selectedMinutes = MAX_MINUTES;
                // Si los minutos son ya 0, restar hora
                if (selectedHours > MIN_HOURS) {
                    selectedHours--;
                    updateHoursDisplay();
                }
            }
            updateMinutesDisplay();
        });
    }

    private void updateHoursDisplay() {
        tvHoursValue.setText(String.valueOf(selectedHours));
    }

    private void updateMinutesDisplay() {
        tvMinutesValue.setText(String.format(Locale.getDefault(), "%02d", selectedMinutes));
    }

    private void initializeDayToggles() {
        toggleMonday = findViewById(R.id.toggleMonday);
        toggleTuesday = findViewById(R.id.toggleTuesday);
        toggleWednesday = findViewById(R.id.toggleWednesday);
        toggleThursday = findViewById(R.id.toggleThursday);
        toggleFriday = findViewById(R.id.toggleFriday);
        toggleSaturday = findViewById(R.id.toggleSaturday);
        toggleSunday = findViewById(R.id.toggleSunday);

        dayToggles = new ArrayList<>();
        dayToggles.add(toggleMonday);
        dayToggles.add(toggleTuesday);
        dayToggles.add(toggleWednesday);
        dayToggles.add(toggleThursday);
        dayToggles.add(toggleFriday);
        dayToggles.add(toggleSaturday);
        dayToggles.add(toggleSunday);
    }

    private int countSelectedDays() {
        int count = 0;
        for (ToggleButton toggle : dayToggles) {
            if (toggle.isChecked()) {
                count++;
            }
        }
        return count;
    }

    private void setSelectedDays(int days) {
        // Por defecto
        if (days <= 0 || days > 7) {
            days = 5;
            for (int i = 0; i < dayToggles.size(); i++) {
                dayToggles.get(i).setChecked(i < 5); // Marcar lunes-viernes
            }
            return;
        }

        // Marcar lunes-viernes
        if (days == 5) {
            for (int i = 0; i < dayToggles.size(); i++) {
                dayToggles.get(i).setChecked(i < 5);
            }
        } else if (days < 5) {
            for (int i = 0; i < dayToggles.size(); i++) {
                dayToggles.get(i).setChecked(i < days);
            }
        } else {
            for (int i = 0; i < dayToggles.size(); i++) {
                dayToggles.get(i).setChecked(i < days);
            }
        }
    }

    // Cargar configuraciones guardadas
    private void loadSavedSettings() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String lang = prefs.getString("language", "es");
        spinnerLanguage.setSelection(lang.equals("es") ? 0 : 1);

        dbHelper.getSettings(settings -> {
            selectedHours = (int) settings[0];
            selectedMinutes = Math.round((settings[0] - selectedHours) * 60 / MINUTE_INCREMENT) * MINUTE_INCREMENT;
            reminderEnabled = settings[2] > 0;
            reminderHour = (int) settings[3];
            reminderMinute = (int) settings[4];

            runOnUiThread(() -> {
                updateHoursDisplay();
                updateMinutesDisplay();
                setSelectedDays((int) settings[1]);

                // Actualizar controles de la alarma
                toggleReminder.setChecked(reminderEnabled);
                tvReminderHourValue.setText(String.format(Locale.getDefault(), "%02d", reminderHour));
                tvReminderMinuteValue.setText(String.format(Locale.getDefault(), "%02d", reminderMinute));
            });
        });
    }

    // Guardar configuración de idioma
    private void saveLanguagePreference(String lang) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("language", lang);
        editor.apply();
    }

    private void setAppLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    // Reiniciar si se modifica la configuración
    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(this, getString(R.string.settings_updated), Toast.LENGTH_LONG).show();
    }

    // Confirmación al eliminar todos los fichajes
    private void showDeleteConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_delete_title))
                .setMessage(getString(R.string.confirm_delete_message))
                .setPositiveButton(getString(R.string.confirming), (dialog, which) -> {
                    String username = dbHelper.getCurrentUsername(this);
                    dbHelper.deleteAllFichajes(username, success -> {
                        if (success) {
                            Toast.makeText(this, getString(R.string.history_deleted), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, getString(R.string.history_delete_error), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void setupReminderControls() {
        toggleReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            reminderEnabled = isChecked;
        });

        btnIncreaseReminderHour.setOnClickListener(v -> {
            if (reminderHour < 23) {
                reminderHour++;
                tvReminderHourValue.setText(String.format(Locale.getDefault(), "%02d", reminderHour));
            }
        });

        btnDecreaseReminderHour.setOnClickListener(v -> {
            if (reminderHour > 0) {
                reminderHour--;
                tvReminderHourValue.setText(String.format(Locale.getDefault(), "%02d", reminderHour));
            }
        });

        btnIncreaseReminderMinute.setOnClickListener(v -> {
            reminderMinute += 5;
            if (reminderMinute >= 60) {
                reminderMinute = 0;
            }
            tvReminderMinuteValue.setText(String.format(Locale.getDefault(), "%02d", reminderMinute));
        });

        btnDecreaseReminderMinute.setOnClickListener(v -> {
            reminderMinute -= 5;
            if (reminderMinute < 0) {
                reminderMinute = 55;
            }
            tvReminderMinuteValue.setText(String.format(Locale.getDefault(), "%02d", reminderMinute));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //dbHelper.close();
    }
}