package eus.ehu.tictacker;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper {
    private Context context;
    private Gson gson;

    public DatabaseHelper(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    public void insertarFichaje(Fichaje fichaje) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", "insert");
        data.put("username", fichaje.username);
        data.put("fecha", fichaje.fecha);
        data.put("hora_entrada", fichaje.horaEntrada);
        data.put("latitud", fichaje.latitud);
        data.put("longitud", fichaje.longitud);

        try {
            JsonObject response = ApiClient.getInstance().post("fichajes.php", data);
            if (!response.get("success").getAsBoolean()) {
                Log.e("DatabaseHelper", "No se pudo insertar el fichaje");
            }
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Error al insertar el fichaje", e);
        }
    }

    public List<Fichaje> obtenerTodosLosFichajes(String username) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", "get_all");
        data.put("username", username);

        try {
            String response = String.valueOf(ApiClient.getInstance().post("fichajes.php", data));
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            if (jsonResponse.get("success").getAsBoolean()) {
                Type listType = new TypeToken<ArrayList<Fichaje>>(){}.getType();
                return gson.fromJson(jsonResponse.get("data"), listType);
            }
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Error obteniendo los fichajes", e);
        }
        return new ArrayList<>();
    }

    public Fichaje obtenerUltimoFichajeDelDia(String fecha, String username) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", "get_last");
        data.put("username", username);
        data.put("fecha", fecha);

        try {
            String response = String.valueOf(ApiClient.getInstance().post("fichajes.php", data));
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            if (jsonResponse.get("success").getAsBoolean()) {
                return gson.fromJson(jsonResponse.get("data"), Fichaje.class);
            }
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Error obteniendo el último fichaje", e);
        }
        return null;
    }

    public void actualizarFichaje(Fichaje fichaje) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", "update");
        data.put("id", fichaje.id);
        data.put("hora_salida", fichaje.horaSalida);
        data.put("latitud", fichaje.latitud);
        data.put("longitud", fichaje.longitud);

        try {
            JsonObject response = ApiClient.getInstance().post("fichajes.php", data);
            if (!response.get("success").getAsBoolean()) {
                Log.e("DatabaseHelper", "Error al actualizar fichaje");
            }
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Error actualizando el fichaje", e);
        }
    }

    public List<Fichaje> obtenerFichajesDeHoy(String username) {
        SimpleDateFormat sdfFecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String fechaActual = sdfFecha.format(new Date());

        Map<String, Object> data = new HashMap<>();
        data.put("action", "get_today");
        data.put("username", username);
        data.put("fecha", fechaActual);

        try {
            String response = String.valueOf(ApiClient.getInstance().post("fichajes.php", data));
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            if (jsonResponse.get("success").getAsBoolean()) {
                Type listType = new TypeToken<ArrayList<Fichaje>>(){}.getType();
                return gson.fromJson(jsonResponse.get("data"), listType);
            }
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Error obteniendo los fichajes del día", e);
        }
        return new ArrayList<>();
    }

    public void saveSettings(float weeklyHours, int workingDays) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", "save");
        data.put("weekly_hours", weeklyHours);
        data.put("working_days", workingDays);

        try {
            JsonObject response = ApiClient.getInstance().post("settings.php", data);
            if (!response.get("success").getAsBoolean()) {
                Log.e("DatabaseHelper", "Error al guardar la configuración");
            }
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Error guardando la configuración", e);
        }
    }

    public float[] getSettings() {
        Map<String, Object> data = new HashMap<>();
        data.put("action", "get");

        try {
            String response = String.valueOf(ApiClient.getInstance().post("settings.php", data));
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            if (jsonResponse.get("success").getAsBoolean()) {
                float[] settings = new float[2];
                settings[0] = jsonResponse.getAsJsonObject("data").get("weekly_hours").getAsFloat();
                settings[1] = jsonResponse.getAsJsonObject("data").get("working_days").getAsFloat();
                return settings;
            }
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Error obteniendo configuraciones", e);
        }
        return new float[]{40.0f, 5.0f}; // Por defecto
    }

    public void deleteAllFichajes(String username) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", "delete_all");
        data.put("username", username);

        try {
            JsonObject response = ApiClient.getInstance().post("fichajes.php", data);
            if (!response.get("success").getAsBoolean()) {
                Log.e("DatabaseHelper", "Error al eliminar fichaje");
            }
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Error eliminando fichajes", e);
        }
    }

    public void actualizarHoraEntrada(Fichaje fichaje) {
        actualizarFichajeCompleto(fichaje); // Utilizamos directamente el completo en la nueva versión, más simple al ser remoto
    }

    public void actualizarFichajeCompleto(Fichaje fichaje) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", "update");
        data.put("id", fichaje.id);
        data.put("hora_entrada", fichaje.horaEntrada);
        data.put("hora_salida", fichaje.horaSalida);

        try {
            JsonObject response = ApiClient.getInstance().post("fichajes.php", data);
            if (!response.get("success").getAsBoolean()) {
                Log.e("DatabaseHelper", "Error al actualizar el fichaje");
            }
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Error actualizando el fichaje", e);
        }
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

    public boolean userExists(String username) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", "check_user");
        data.put("username", username);

        try {
            String response = String.valueOf(ApiClient.getInstance().post("users.php", data));
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            return jsonResponse.getAsJsonObject("data").get("exists").getAsBoolean();
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Error comprobando si el usuario existe", e);
            return false;
        }
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