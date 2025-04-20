package eus.ehu.tictacker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper {
    private Context context;
    private Gson gson;
    private WorkManager workManager;

    // Interfaces de callback
    public interface FichajesCallback {
        void onFichajesReceived(List<Fichaje> fichajes);
    }

    public interface FichajeCallback {
        void onFichajeReceived(Fichaje fichaje);
    }

    public interface SettingsCallback {
        void onSettingsReceived(float[] settings);
    }

    public interface BooleanCallback {
        void onResult(boolean success);
    }

    public interface StringCallback {
        void onResult(String result);
    }

    public interface ProfileCallback {
        void onProfileReceived(UserProfile profile);
    }

    public interface JsonCallback {
        void onResponse(JsonObject response);
    }

    public DatabaseHelper(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.workManager = WorkManager.getInstance(context);
    }

    // Métodos adaptados para usar WorkManager
    public void insertarFichaje(Fichaje fichaje, BooleanCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "fichajes.php")
                .putString("action", "insert_complete")
                .putString("param_username", fichaje.username)
                .putString("param_fecha", fichaje.fecha)
                .putString("param_hora_entrada", fichaje.horaEntrada)
                .putString("param_hora_salida", fichaje.horaSalida != null ? fichaje.horaSalida : "")
                .putString("param_latitud", String.valueOf(fichaje.latitud))
                .putString("param_longitud", String.valueOf(fichaje.longitud))
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                boolean success = response != null && response.has("success") && response.get("success").getAsBoolean();
                callback.onResult(success);
            }
        });
    }

    public void obtenerTodosLosFichajes(String username, FichajesCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "fichajes.php")
                .putString("action", "get_all")
                .putString("param_username", username)
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                List<Fichaje> result = new ArrayList<>();
                if (response != null && response.has("success") && response.get("success").getAsBoolean() && response.has("data")) {
                    result = parseFichajesFromResponse(response, username);
                }
                callback.onFichajesReceived(result);
            }
        });
    }

    public void actualizarFichaje(Fichaje fichaje, BooleanCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "fichajes.php")
                .putString("action", "update")
                .putString("param_id", String.valueOf(fichaje.id))
                .putString("param_hora_salida", fichaje.horaSalida != null ? fichaje.horaSalida : "")
                .putString("param_latitud", String.valueOf(fichaje.latitud))
                .putString("param_longitud", String.valueOf(fichaje.longitud))
                .putString("param_username", fichaje.username)
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                boolean success = response != null && response.has("success") && response.get("success").getAsBoolean();
                callback.onResult(success);
            }
        });
    }

    public void obtenerUltimoFichajeDelDia(String fecha, String username, FichajeCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "fichajes.php")
                .putString("action", "get_last")
                .putString("param_username", username)
                .putString("param_fecha", fecha)
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                Fichaje fichaje = null;
                if (response != null && response.has("success") && response.get("success").getAsBoolean() && response.has("data")) {
                    JsonObject fichajeJson = response.getAsJsonObject("data");
                    fichaje = parseFichajeFromJson(fichajeJson, username);
                }
                callback.onFichajeReceived(fichaje);
            }
        });
    }

    public void obtenerFichajesDeHoy(String username, FichajesCallback callback) {
        String fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Data inputData = new Data.Builder()
                .putString("endpoint", "fichajes.php")
                .putString("action", "get_today")
                .putString("param_username", username)
                .putString("param_fecha", fechaActual)
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                List<Fichaje> result = new ArrayList<>();
                if (response != null && response.has("success") && response.get("success").getAsBoolean() && response.has("data")) {
                    result = parseFichajesFromResponse(response, username);
                }
                callback.onFichajesReceived(result);
            }
        });
    }

    public void saveSettings(float weeklyHours, int workingDays, boolean reminderEnabled,
                             int reminderHour, int reminderMinute, BooleanCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "settings.php")
                .putString("action", "save")
                .putString("param_weekly_hours", String.valueOf(weeklyHours))
                .putString("param_working_days", String.valueOf(workingDays))
                .putString("param_reminder_enabled", String.valueOf(reminderEnabled ? 1 : 0))
                .putString("param_reminder_hour", String.valueOf(reminderHour))
                .putString("param_reminder_minute", String.valueOf(reminderMinute))
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                boolean success = response != null && response.has("success") && response.get("success").getAsBoolean();

                // Guardar también en SharedPreferences
                if (success) {
                    SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("reminder_enabled", reminderEnabled);
                    editor.putInt("reminder_hour", reminderHour);
                    editor.putInt("reminder_minute", reminderMinute);
                    editor.apply();

                    // Programar o cancelar la alarma según la configuración
                    if (reminderEnabled) {
                        ClockInReminderService.scheduleReminder(context);
                    } else {
                        ClockInReminderService.cancelReminder(context);
                    }
                }

                callback.onResult(success);
            }
        });
    }

    public void getSettings(SettingsCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "settings.php")
                .putString("action", "get")
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                float[] settings = new float[]{40.0f, 5.0f, 0.0f, 9.0f, 0.0f};
                if (response != null && response.has("success") && response.get("success").getAsBoolean() && response.has("data")) {
                    JsonObject dataObj = response.getAsJsonObject("data");
                    if (dataObj.has("weekly_hours") && !dataObj.get("weekly_hours").isJsonNull()) {
                        settings[0] = dataObj.get("weekly_hours").getAsFloat();
                    }
                    if (dataObj.has("working_days") && !dataObj.get("working_days").isJsonNull()) {
                        settings[1] = dataObj.get("working_days").getAsFloat();
                    }
                    if (dataObj.has("reminder_enabled") && !dataObj.get("reminder_enabled").isJsonNull()) {
                        settings[2] = dataObj.get("reminder_enabled").getAsFloat();
                    }
                    if (dataObj.has("reminder_hour") && !dataObj.get("reminder_hour").isJsonNull()) {
                        settings[3] = dataObj.get("reminder_hour").getAsFloat();
                    }
                    if (dataObj.has("reminder_minute") && !dataObj.get("reminder_minute").isJsonNull()) {
                        settings[4] = dataObj.get("reminder_minute").getAsFloat();
                    }

                    // Actualizar preferencias locales
                    SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("reminder_enabled", settings[2] > 0);
                    editor.putInt("reminder_hour", (int)settings[3]);
                    editor.putInt("reminder_minute", (int)settings[4]);
                    editor.apply();
                }
                callback.onSettingsReceived(settings);
            }
        });
    }


    public void deleteAllFichajes(String username, BooleanCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "fichajes.php")
                .putString("action", "delete_all")
                .putString("param_username", username)
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                boolean success = response != null && response.has("success") && response.get("success").getAsBoolean();
                callback.onResult(success);
            }
        });
    }

    public void actualizarFichajeCompleto(Fichaje fichaje, BooleanCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "fichajes.php")
                .putString("action", "update_complete")
                .putString("param_id", String.valueOf(fichaje.id))
                .putString("param_hora_entrada", fichaje.horaEntrada)
                .putString("param_hora_salida", fichaje.horaSalida != null ? fichaje.horaSalida : "")
                .putString("param_latitud", String.valueOf(fichaje.latitud))
                .putString("param_longitud", String.valueOf(fichaje.longitud))
                .putString("param_username", fichaje.username)
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                boolean success = response != null && response.has("success") && response.get("success").getAsBoolean();
                callback.onResult(success);
            }
        });
    }

    public void validarUsuario(String username, String password, BooleanCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "auth_user.php")
                .putString("param_username", username)
                .putString("param_password", password)
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                boolean success = response != null && response.has("success") && response.get("success").getAsBoolean();
                callback.onResult(success);
            }
        });
    }

    public void userExists(String username, BooleanCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "users.php")
                .putString("action", "check_user")
                .putString("param_username", username)
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                boolean exists = false;
                if (response != null && response.has("success") && response.get("success").getAsBoolean() &&
                        response.has("data") && response.getAsJsonObject("data").has("exists")) {
                    exists = response.getAsJsonObject("data").get("exists").getAsBoolean();
                }
                callback.onResult(exists);
            }
        });
    }

    public void addUser(String username, String password, BooleanCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "users.php")
                .putString("action", "add_user")
                .putString("param_username", username)
                .putString("param_password", password)
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                boolean success = response != null && response.has("success") && response.get("success").getAsBoolean();
                callback.onResult(success);
            }
        });
    }

    public void getProfile(String username, ProfileCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "profile.php")
                .putString("action", "get_profile")
                .putString("param_username", username)
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                UserProfile profile = null;
                if (response != null && response.has("success") && response.get("success").getAsBoolean() && response.has("data")) {
                    JsonObject profileData = response.getAsJsonObject("data");
                    profile = new UserProfile();
                    profile.username = username;

                    if (profileData.has("name") && !profileData.get("name").isJsonNull()) {
                        profile.name = profileData.get("name").getAsString();
                    }
                    if (profileData.has("surname") && !profileData.get("surname").isJsonNull()) {
                        profile.surname = profileData.get("surname").getAsString();
                    }
                    if (profileData.has("birthdate") && !profileData.get("birthdate").isJsonNull()) {
                        profile.birthdate = profileData.get("birthdate").getAsString();
                    }
                    if (profileData.has("email") && !profileData.get("email").isJsonNull()) {
                        profile.email = profileData.get("email").getAsString();
                    }
                    if (profileData.has("profile_photo") && !profileData.get("profile_photo").isJsonNull()) {
                        profile.profilePhoto = profileData.get("profile_photo").getAsString();
                    }
                }
                callback.onProfileReceived(profile);
            }
        });
    }

    public void updateProfile(UserProfile profile, BooleanCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "profile.php")
                .putString("action", "update_profile")
                .putString("param_username", profile.username)
                .putString("param_name", profile.name != null ? profile.name : "")
                .putString("param_surname", profile.surname != null ? profile.surname : "")
                .putString("param_birthdate", profile.birthdate != null ? profile.birthdate : "")
                .putString("param_email", profile.email != null ? profile.email : "")
                .putString("param_profile_photo", profile.profilePhoto != null ? profile.profilePhoto : "")
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                boolean success = response != null && response.has("success") && response.get("success").getAsBoolean();
                callback.onResult(success);
            }
        });
    }

    public void obtenerFichajePorId(int fichajeId, String username, FichajeCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "fichajes.php")
                .putString("action", "get_by_id")
                .putString("param_id", String.valueOf(fichajeId))
                .putString("param_username", username)
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                Fichaje fichaje = null;
                if (response != null && response.has("success") && response.get("success").getAsBoolean() && response.has("data")) {
                    JsonObject fichajeJson = response.getAsJsonObject("data");
                    fichaje = parseFichajeFromJson(fichajeJson, username);
                }
                callback.onFichajeReceived(fichaje);
            }
        });
    }

    public void validateCurrentPassword(String username, String password, BooleanCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "auth_user.php")
                .putString("param_username", username)
                .putString("param_password", password)
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                boolean isValid = response != null && response.has("success") && response.get("success").getAsBoolean();
                callback.onResult(isValid);
            }
        });
    }

    public void updateUserPassword(String username, String newPassword, BooleanCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "users.php")
                .putString("action", "update_password")
                .putString("param_username", username)
                .putString("param_password", newPassword)
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                boolean success = response != null && response.has("success") && response.get("success").getAsBoolean();
                callback.onResult(success);
            }
        });
    }

    public void updateFCMToken(String username, String token, BooleanCallback callback) {
        Data inputData = new Data.Builder()
                .putString("endpoint", "fcm_tokens.php")
                .putString("action", "update_token")
                .putString("param_username", username)
                .putString("param_token", token)
                .build();

        executeWorker(inputData, new JsonCallback() {
            @Override
            public void onResponse(JsonObject response) {
                boolean success = response != null && response.has("success") && response.get("success").getAsBoolean();
                callback.onResult(success);
            }
        });
    }

    // Métodos auxiliares
    private void executeWorker(Data inputData, JsonCallback callback) {
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(DatabaseWorker.class)
                .setInputData(inputData)
                .build();

        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.post(() -> {
            final androidx.lifecycle.Observer<WorkInfo> observer = new androidx.lifecycle.Observer<WorkInfo>() {
                @Override
                public void onChanged(WorkInfo workInfo) {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            Data outputData = workInfo.getOutputData();
                            String responseStr = outputData.getString("response");
                            try {
                                JsonObject response = gson.fromJson(responseStr, JsonObject.class);
                                callback.onResponse(response);
                            } catch (Exception e) {
                                Log.e("DatabaseHelper", "Error parsing response", e);
                                callback.onResponse(null);
                            }
                        } else {
                            callback.onResponse(null);
                        }
                        workManager.getWorkInfoByIdLiveData(workRequest.getId()).removeObserver(this);
                    }
                }
            };

            workManager.getWorkInfoByIdLiveData(workRequest.getId())
                    .observeForever(observer);

            workManager.enqueue(workRequest);
        });
    }

    private List<Fichaje> parseFichajesFromResponse(JsonObject response, String username) {
        List<Fichaje> result = new ArrayList<>();
        if (response.has("data")) {
            JsonArray dataArray = response.getAsJsonArray("data");
            for (JsonElement element : dataArray) {
                JsonObject obj = element.getAsJsonObject();
                Fichaje fichaje = parseFichajeFromJson(obj, username);
                result.add(fichaje);
            }
        }
        return result;
    }

    private Fichaje parseFichajeFromJson(JsonObject obj, String username) {
        Fichaje fichaje = new Fichaje();
        fichaje.id = obj.has("id") ? obj.get("id").getAsInt() : 0;
        fichaje.fecha = obj.has("fecha") ? obj.get("fecha").getAsString() : "";
        fichaje.horaEntrada = obj.has("hora_entrada") ?
                (obj.get("hora_entrada").isJsonNull() ? null : obj.get("hora_entrada").getAsString()) : null;
        fichaje.horaSalida = obj.has("hora_salida") ?
                (obj.get("hora_salida").isJsonNull() ? null : obj.get("hora_salida").getAsString()) : null;
        fichaje.latitud = obj.has("latitud") ? obj.get("latitud").getAsDouble() : 0.0;
        fichaje.longitud = obj.has("longitud") ? obj.get("longitud").getAsDouble() : 0.0;
        fichaje.username = username;
        return fichaje;
    }

    public String getCurrentUsername(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("usuario_actual", "unknown_user");
    }
}