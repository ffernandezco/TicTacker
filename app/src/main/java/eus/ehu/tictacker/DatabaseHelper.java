package eus.ehu.tictacker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseHelper {
    private Context context;
    private Gson gson;
    private ExecutorService executorService;
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

    public DatabaseHelper(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void close() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    public void insertarFichaje(Fichaje fichaje, BooleanCallback callback) {
        executorService.execute(() -> {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "insert");
            data.put("username", fichaje.username);
            data.put("fecha", fichaje.fecha);
            data.put("hora_entrada", fichaje.horaEntrada);
            data.put("latitud", fichaje.latitud);
            data.put("longitud", fichaje.longitud);

            try {
                JsonObject response = ApiClient.getInstance().post("fichajes.php", data);
                boolean success = response.get("success").getAsBoolean();
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(success));
            } catch (IOException e) {
                Log.e("DatabaseHelper", "Error al insertar el fichaje", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(false));
            }
        });
    }

    public void obtenerTodosLosFichajes(String username, FichajesCallback callback) {
        executorService.execute(() -> {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "get_all");
            data.put("username", username);

            try {
                JsonObject jsonResponse = ApiClient.getInstance().post("fichajes.php", data);
                if (jsonResponse != null && jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                    Type listType = new TypeToken<ArrayList<Fichaje>>(){}.getType();
                    List<Fichaje> result = jsonResponse.has("data") ?
                            gson.fromJson(jsonResponse.get("data"), listType) : new ArrayList<>();
                    new Handler(Looper.getMainLooper()).post(() -> callback.onFichajesReceived(result));
                    return;
                }
            } catch (IOException e) {
                Log.e("DatabaseHelper", "Error obteniendo los fichajes", e);
            }
            new Handler(Looper.getMainLooper()).post(() -> callback.onFichajesReceived(new ArrayList<>()));
        });
    }

    public void actualizarFichaje(Fichaje fichaje, BooleanCallback callback) {
        executorService.execute(() -> {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "update");
            data.put("id", fichaje.id);
            data.put("hora_salida", fichaje.horaSalida != null ? fichaje.horaSalida : "");
            data.put("latitud", fichaje.latitud);
            data.put("longitud", fichaje.longitud);
            data.put("username", fichaje.username);

            try {
                JsonObject response = ApiClient.getInstance().post("fichajes.php", data);
                boolean success = response.get("success").getAsBoolean();
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(success));
            } catch (IOException e) {
                Log.e("DatabaseHelper", "Error actualizando el fichaje", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(false));
            }
        });
    }

    public void obtenerUltimoFichajeDelDia(String fecha, String username, FichajeCallback callback) {
        executorService.execute(() -> {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "get_last");
            data.put("username", username);
            data.put("fecha", fecha);

            try {
                JsonObject jsonResponse = ApiClient.getInstance().post("fichajes.php", data);
                if (jsonResponse.get("success").getAsBoolean() && jsonResponse.has("data")) {
                    JsonObject fichajeJson = jsonResponse.getAsJsonObject("data");
                    Fichaje fichaje = new Fichaje();
                    fichaje.id = fichajeJson.has("id") ? fichajeJson.get("id").getAsInt() : 0;
                    fichaje.fecha = fichajeJson.has("fecha") ? fichajeJson.get("fecha").getAsString() : "";
                    fichaje.horaEntrada = fichajeJson.has("hora_entrada") ?
                            (fichajeJson.get("hora_entrada").isJsonNull() ? null : fichajeJson.get("hora_entrada").getAsString()) : null;
                    fichaje.horaSalida = fichajeJson.has("hora_salida") ?
                            (fichajeJson.get("hora_salida").isJsonNull() ? null : fichajeJson.get("hora_salida").getAsString()) : null;
                    fichaje.latitud = fichajeJson.has("latitud") ? fichajeJson.get("latitud").getAsDouble() : 0.0;
                    fichaje.longitud = fichajeJson.has("longitud") ? fichajeJson.get("longitud").getAsDouble() : 0.0;
                    fichaje.username = username;

                    new Handler(Looper.getMainLooper()).post(() -> callback.onFichajeReceived(fichaje));
                    return;
                }
            } catch (IOException e) {
                Log.e("DatabaseHelper", "Error obteniendo el último fichaje", e);
            }
            new Handler(Looper.getMainLooper()).post(() -> callback.onFichajeReceived(null));
        });
    }

    public void obtenerFichajesDeHoy(String username, FichajesCallback callback) {
        executorService.execute(() -> {
            String fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            Map<String, Object> data = new HashMap<>();
            data.put("action", "get_today");
            data.put("username", username);
            data.put("fecha", fechaActual);

            try {
                JsonObject jsonResponse = ApiClient.getInstance().post("fichajes.php", data);
                if (jsonResponse.get("success").getAsBoolean() && jsonResponse.has("data")) {
                    List<Fichaje> result = new ArrayList<>();

                    for (JsonElement element : jsonResponse.getAsJsonArray("data")) {
                        JsonObject obj = element.getAsJsonObject();
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

                        result.add(fichaje);
                    }

                    new Handler(Looper.getMainLooper()).post(() -> callback.onFichajesReceived(result));
                    return;
                }
            } catch (IOException e) {
                Log.e("DatabaseHelper", "Error obteniendo los fichajes del día", e);
            }
            new Handler(Looper.getMainLooper()).post(() -> callback.onFichajesReceived(new ArrayList<>()));
        });
    }

    public void saveSettings(float weeklyHours, int workingDays, BooleanCallback callback) {
        executorService.execute(() -> {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "save");
            data.put("weekly_hours", weeklyHours);
            data.put("working_days", workingDays);

            try {
                JsonObject response = ApiClient.getInstance().post("settings.php", data);
                boolean success = response.get("success").getAsBoolean();
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(success));
            } catch (IOException e) {
                Log.e("DatabaseHelper", "Error guardando la configuración", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(false));
            }
        });
    }

    public void getSettings(SettingsCallback callback) {
        executorService.execute(() -> {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "get");

            try {
                JsonObject jsonResponse = ApiClient.getInstance().post("settings.php", data);
                if (jsonResponse != null && jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                    JsonObject dataObj = jsonResponse.has("data") ? jsonResponse.getAsJsonObject("data") : null;
                    float weeklyHours = 40.0f;
                    float workingDays = 5.0f;

                    if (dataObj != null) {
                        if (dataObj.has("weekly_hours") && !dataObj.get("weekly_hours").isJsonNull()) {
                            weeklyHours = dataObj.get("weekly_hours").getAsFloat();
                        }
                        if (dataObj.has("working_days") && !dataObj.get("working_days").isJsonNull()) {
                            workingDays = dataObj.get("working_days").getAsFloat();
                        }
                    }

                    float[] settings = new float[]{weeklyHours, workingDays};
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSettingsReceived(settings));
                    return;
                }
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error obteniendo configuraciones", e);
            }
            new Handler(Looper.getMainLooper()).post(() -> callback.onSettingsReceived(new float[]{40.0f, 5.0f}));
        });
    }

    public void deleteAllFichajes(String username, BooleanCallback callback) {
        executorService.execute(() -> {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "delete_all");
            data.put("username", username);

            try {
                JsonObject response = ApiClient.getInstance().post("fichajes.php", data);
                boolean success = response.get("success").getAsBoolean();
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(success));
            } catch (IOException e) {
                Log.e("DatabaseHelper", "Error eliminando fichajes", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(false));
            }
        });
    }

    public void actualizarFichajeCompleto(Fichaje fichaje, BooleanCallback callback) {
        executorService.execute(() -> {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "update");
            data.put("id", fichaje.id);
            data.put("hora_entrada", fichaje.horaEntrada);
            data.put("hora_salida", fichaje.horaSalida);

            try {
                JsonObject response = ApiClient.getInstance().post("fichajes.php", data);
                boolean success = response.get("success").getAsBoolean();
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(success));
            } catch (IOException e) {
                Log.e("DatabaseHelper", "Error actualizando el fichaje", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(false));
            }
        });
    }

    public boolean validarUsuario(String username, String password) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);

        try {
            String response = String.valueOf(ApiClient.getInstance().post("auth_user.php", data));
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            return jsonResponse.get("success").getAsBoolean();
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Error validando el usuario", e);
            return false;
        }
    }

    public String getCurrentUsername(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("usuario_actual", "unknown_user");
    }

    public void userExists(String username, BooleanCallback callback) {
        executorService.execute(() -> {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "check_user");
            data.put("username", username);

            try {
                String response = String.valueOf(ApiClient.getInstance().post("users.php", data));
                JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                boolean exists = jsonResponse.getAsJsonObject("data").get("exists").getAsBoolean();
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(exists));
            } catch (IOException e) {
                Log.e("DatabaseHelper", "Error comprobando si el usuario existe", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(false));
            }
        });
    }

    public boolean addUser(String username, String password) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", "add_user");
        data.put("username", username);
        data.put("password", password);

        try {
            String response = String.valueOf(ApiClient.getInstance().post("users.php", data));
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            return jsonResponse.get("success").getAsBoolean();
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Error añadiendo el usuario", e);
            return false;
        }
    }
}