package eus.ehu.tictacker.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import eus.ehu.tictacker.ClockInReminderService;
import eus.ehu.tictacker.DatabaseHelper;
import eus.ehu.tictacker.LoginActivity;
import eus.ehu.tictacker.MainActivity;
import eus.ehu.tictacker.R;

public class SettingsFragment extends Fragment {
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

    private static final int MAX_HOURS = 168;
    private static final int MIN_HOURS = 0;
    private static final int MAX_MINUTES = 55;
    private static final int MIN_MINUTES = 0;
    private static final int MINUTE_INCREMENT = 5;
    private ImageView ivLogoPreview;
    private Button btnChangeLogo, btnResetLogo;
    private Uri selectedLogoUri = null;
    private Button btnLogout;
    private TextView tvReminderHourValue, tvReminderMinuteValue;
    private Button btnIncreaseReminderHour, btnDecreaseReminderHour;
    private Button btnIncreaseReminderMinute, btnDecreaseReminderMinute;
    private ToggleButton toggleReminder;

    private int reminderHour = 9;
    private int reminderMinute = 0;
    private static final int PICK_IMAGE_REQUEST = 1001;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyLanguageFromPreferences();
    }

    private void applyLanguageFromPreferences() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", requireContext().MODE_PRIVATE);
        String lang = prefs.getString("language", "es");
        setAppLocale(lang);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());
        initializeViews(view);

        String[] languages = {"Español", "English"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(adapter);

        loadSavedSettings();
        setupNumberPickers();
        setupReminderPickers();

        btnSave.setOnClickListener(v -> saveSettings());
        btnDeleteHistory.setOnClickListener(v -> showDeleteConfirmationDialog());
        btnLogout.setOnClickListener(v -> logoutUser());

        loadCustomLogo();

        btnChangeLogo.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image)), PICK_IMAGE_REQUEST);
        });

        btnResetLogo.setOnClickListener(v -> {
            SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", requireContext().MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("custom_logo_uri");
            editor.apply();

            ivLogoPreview.setImageResource(R.mipmap.ic_launcher_adaptive_fore);
            selectedLogoUri = null;
            Toast.makeText(requireContext(), R.string.logo_reset, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //dbHelper.close();
    }

    private void initializeViews(View view) {
        spinnerLanguage = view.findViewById(R.id.spinnerLanguage);
        tvHoursValue = view.findViewById(R.id.tvHoursValue);
        tvMinutesValue = view.findViewById(R.id.tvMinutesValue);
        btnIncreaseHours = view.findViewById(R.id.btnIncreaseHours);
        btnDecreaseHours = view.findViewById(R.id.btnDecreaseHours);
        btnIncreaseMinutes = view.findViewById(R.id.btnIncreaseMinutes);
        btnDecreaseMinutes = view.findViewById(R.id.btnDecreaseMinutes);
        btnSave = view.findViewById(R.id.btnSaveSettings);
        btnDeleteHistory = view.findViewById(R.id.btnDeleteHistory);
        btnLogout = view.findViewById(R.id.btnLogout);

        toggleMonday = view.findViewById(R.id.toggleMonday);
        toggleTuesday = view.findViewById(R.id.toggleTuesday);
        toggleWednesday = view.findViewById(R.id.toggleWednesday);
        toggleThursday = view.findViewById(R.id.toggleThursday);
        toggleFriday = view.findViewById(R.id.toggleFriday);
        toggleSaturday = view.findViewById(R.id.toggleSaturday);
        toggleSunday = view.findViewById(R.id.toggleSunday);

        dayToggles = new ArrayList<>();
        dayToggles.add(toggleMonday);
        dayToggles.add(toggleTuesday);
        dayToggles.add(toggleWednesday);
        dayToggles.add(toggleThursday);
        dayToggles.add(toggleFriday);
        dayToggles.add(toggleSaturday);
        dayToggles.add(toggleSunday);

        ivLogoPreview = view.findViewById(R.id.ivLogoPreview);
        btnChangeLogo = view.findViewById(R.id.btnChangeLogo);
        btnResetLogo = view.findViewById(R.id.btnResetLogo);

        toggleReminder = view.findViewById(R.id.toggleReminder);
        tvReminderHourValue = view.findViewById(R.id.tvReminderHourValue);
        tvReminderMinuteValue = view.findViewById(R.id.tvReminderMinuteValue);
        btnIncreaseReminderHour = view.findViewById(R.id.btnIncreaseReminderHour);
        btnDecreaseReminderHour = view.findViewById(R.id.btnDecreaseReminderHour);
        btnIncreaseReminderMinute = view.findViewById(R.id.btnIncreaseReminderMinute);
        btnDecreaseReminderMinute = view.findViewById(R.id.btnDecreaseReminderMinute);
    }

    // Configurar los selectores para las horas
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

        btnIncreaseMinutes.setOnClickListener(v -> {
            selectedMinutes += MINUTE_INCREMENT;
            if (selectedMinutes > MAX_MINUTES) {
                selectedMinutes = MIN_MINUTES;
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

    // Añadir recordatorios de fichar con alarma programada
    private void setupReminderPickers() {
        // Configurar listeners para los botones de hora
        btnIncreaseReminderHour.setOnClickListener(v -> {
            if (reminderHour < 23) {
                reminderHour++;
            } else {
                reminderHour = 0;
            }
            updateReminderHourDisplay();
        });

        btnDecreaseReminderHour.setOnClickListener(v -> {
            if (reminderHour > 0) {
                reminderHour--;
            } else {
                reminderHour = 23;
            }
            updateReminderHourDisplay();
        });

        // Configurar listeners para los botones de minutos
        btnIncreaseReminderMinute.setOnClickListener(v -> {
            reminderMinute += 5;
            if (reminderMinute >= 60) {
                reminderMinute = 0;
                if (reminderHour < 23) {
                    reminderHour++;
                } else {
                    reminderHour = 0;
                }
                updateReminderHourDisplay();
            }
            updateReminderMinuteDisplay();
        });

        btnDecreaseReminderMinute.setOnClickListener(v -> {
            reminderMinute -= 5;
            if (reminderMinute < 0) {
                reminderMinute = 55;
                if (reminderHour > 0) {
                    reminderHour--;
                } else {
                    reminderHour = 23;
                }
                updateReminderHourDisplay();
            }
            updateReminderMinuteDisplay();
        });

        // Configurar el toggle del recordatorio para activar / desactivar
        toggleReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnIncreaseReminderHour.setEnabled(isChecked);
            btnDecreaseReminderHour.setEnabled(isChecked);
            btnIncreaseReminderMinute.setEnabled(isChecked);
            btnDecreaseReminderMinute.setEnabled(isChecked);

            // Actualizar el estado visual de los botones
            btnIncreaseReminderHour.setAlpha(isChecked ? 1f : 0.5f);
            btnDecreaseReminderHour.setAlpha(isChecked ? 1f : 0.5f);
            btnIncreaseReminderMinute.setAlpha(isChecked ? 1f : 0.5f);
            btnDecreaseReminderMinute.setAlpha(isChecked ? 1f : 0.5f);
        });

        // Establecer estado inicial
        boolean reminderEnabled = toggleReminder.isChecked();
        btnIncreaseReminderHour.setEnabled(reminderEnabled);
        btnDecreaseReminderHour.setEnabled(reminderEnabled);
        btnIncreaseReminderMinute.setEnabled(reminderEnabled);
        btnDecreaseReminderMinute.setEnabled(reminderEnabled);

        btnIncreaseReminderHour.setAlpha(reminderEnabled ? 1f : 0.5f);
        btnDecreaseReminderHour.setAlpha(reminderEnabled ? 1f : 0.5f);
        btnIncreaseReminderMinute.setAlpha(reminderEnabled ? 1f : 0.5f);
        btnDecreaseReminderMinute.setAlpha(reminderEnabled ? 1f : 0.5f);
    }

    private void updateReminderHourDisplay() {
        tvReminderHourValue.setText(String.format(Locale.getDefault(), "%02d", reminderHour));
    }

    private void updateReminderMinuteDisplay() {
        tvReminderMinuteValue.setText(String.format(Locale.getDefault(), "%02d", reminderMinute));
    }

    // Cargar la configuración del usuario guardada en la base de datos remota
    private void loadSavedSettings() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", requireContext().MODE_PRIVATE);
        String lang = prefs.getString("language", "es");
        spinnerLanguage.setSelection(lang.equals("es") ? 0 : 1);

        dbHelper.getSettings(new DatabaseHelper.SettingsCallback() {
            @Override
            public void onSettingsReceived(float[] settings) {
                requireActivity().runOnUiThread(() -> {
                    float weeklyHours = settings[0];
                    selectedHours = (int) weeklyHours;
                    selectedMinutes = Math.round((weeklyHours - selectedHours) * 60);

                    updateHoursDisplay();
                    updateMinutesDisplay();
                    setSelectedDays((int) settings[1]);

                    // Cargar configuración de la alarma
                    boolean reminderEnabled = settings[2] > 0;
                    reminderHour = (int) settings[3];
                    reminderMinute = (int) settings[4];

                    toggleReminder.setChecked(reminderEnabled);
                    updateReminderHourDisplay();
                    updateReminderMinuteDisplay();

                    btnIncreaseReminderHour.setEnabled(reminderEnabled);
                    btnDecreaseReminderHour.setEnabled(reminderEnabled);
                    btnIncreaseReminderMinute.setEnabled(reminderEnabled);
                    btnDecreaseReminderMinute.setEnabled(reminderEnabled);
                });
            }
        });
    }

    private void setSelectedDays(int days) {
        if (days <= 0 || days > 7) {
            days = 5;
        }
        for (int i = 0; i < dayToggles.size(); i++) {
            dayToggles.get(i).setChecked(i < days);
        }
    }

    // Guardar configuración tanto en SharedPreferences como en la base de datos remota
    private void saveSettings() {
        String selectedLanguage = spinnerLanguage.getSelectedItemPosition() == 0 ? "es" : "en";
        float weeklyHours = selectedHours + (selectedMinutes / 60.0f);
        int workingDays = countSelectedDays();
        boolean reminderEnabled = toggleReminder.isChecked();

        // Guardar en SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", requireContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("reminder_enabled", reminderEnabled);
        editor.putInt("reminder_hour", reminderHour);
        editor.putInt("reminder_minute", reminderMinute);
        editor.apply();

        if (selectedLogoUri != null) {
            editor.putString("custom_logo_uri", selectedLogoUri.toString());
            editor.apply();
        }

        // Subir cambios al servidor
        dbHelper.saveSettings(weeklyHours, workingDays, reminderEnabled, reminderHour, reminderMinute, success -> {
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    saveLanguagePreference(selectedLanguage);
                    setAppLocale(selectedLanguage);

                    // Programar la alarma después de guardar los ajustes
                    if (reminderEnabled) {
                        ClockInReminderService.scheduleReminder(requireContext());
                    } else {
                        ClockInReminderService.cancelReminder(requireContext());
                    }

                    Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();
                    restartApp();
                } else {
                    Toast.makeText(requireContext(), R.string.settings_save_error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private int countSelectedDays() {
        int count = 0;
        for (ToggleButton toggle : dayToggles) {
            if (toggle.isChecked()) count++;
        }
        return count;
    }

    private void saveLanguagePreference(String lang) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", requireContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("language", lang);
        editor.apply();
    }

    private void setAppLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = requireContext().getResources().getConfiguration();
        config.setLocale(locale);
        requireContext().getResources().updateConfiguration(config, requireContext().getResources().getDisplayMetrics());
    }

    private void restartApp() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(requireContext(), getString(R.string.settings_updated), Toast.LENGTH_LONG).show();
    }

    // Eliminar los fichajes de un usuario de la base de datos
    private void showDeleteConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.confirm_delete_title))
                .setMessage(getString(R.string.confirm_delete_message))
                .setPositiveButton(getString(R.string.confirming), (dialog, which) -> {
                    String username = dbHelper.getCurrentUsername(requireContext());
                    dbHelper.deleteAllFichajes(username, success -> {
                        if (success) {
                            Toast.makeText(requireContext(), getString(R.string.history_deleted), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.history_delete_error), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    // Mostrar logo personalizado en caso de haberlo elegido
    private void loadCustomLogo() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", requireContext().MODE_PRIVATE);
        String logoUriString = prefs.getString("custom_logo_uri", null);

        if (logoUriString != null) {
            try {
                selectedLogoUri = Uri.parse(logoUriString);
                ivLogoPreview.setImageURI(selectedLogoUri);
            } catch (Exception e) {
                e.printStackTrace();
                ivLogoPreview.setImageResource(R.mipmap.ic_launcher_adaptive_fore);
            }
        } else {
            ivLogoPreview.setImageResource(R.mipmap.ic_launcher_adaptive_fore);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedLogoUri = data.getData();

            try {
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                requireContext().getContentResolver().takePersistableUriPermission(selectedLogoUri, takeFlags);

                SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", requireContext().MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("custom_logo_uri", selectedLogoUri.toString());
                editor.apply();

                ivLogoPreview.setImageURI(selectedLogoUri);
                Toast.makeText(requireContext(), R.string.logo_changed, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Error guardando el logotipo personalizado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void logoutUser() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("app_prefs", requireContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("usuario_actual");
        editor.apply();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(requireContext(), R.string.logout_success, Toast.LENGTH_SHORT).show();
    }
}