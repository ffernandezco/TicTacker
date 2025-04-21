package eus.ehu.tictacker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class DatabaseWorker extends Worker {
    private static final String TAG = "DatabaseWorker";
    private static final String BASE_URL = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ffernandez032/WEB/"; // Dirección URL / IP del servidor

    public DatabaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();
        String endpoint = inputData.getString("endpoint");
        String action = inputData.getString("action");
        Map<String, String> parameters = new HashMap<>();

        // Extraer todos los parámetros del input
        for (String key : inputData.getKeyValueMap().keySet()) {
            if (key.startsWith("param_")) {
                parameters.put(key.substring(6), inputData.getString(key));
            }
        }

        try {
            String response = makeHttpRequest(endpoint, action, parameters);

            // Construir respuesta del JSON
            Data outputData = new Data.Builder()
                    .putString("response", response)
                    .build();

            return Result.success(outputData);
        } catch (Exception e) {
            Log.e(TAG, "Error en DatabaseWorker", e);

            // Errores JSON
            try {
                JSONObject errorJson = new JSONObject();
                errorJson.put("success", false);
                errorJson.put("error", e.getMessage());

                Data outputData = new Data.Builder()
                        .putString("response", errorJson.toString())
                        .build();

                return Result.failure(outputData);
            } catch (JSONException je) {
                return Result.failure();
            }
        }
    }

    // Realiza la petición HTTP POST al servidor
    private String makeHttpRequest(String endpoint, String action, Map<String, String> parameters) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String response;

        try {
            URL url = new URL(BASE_URL + endpoint);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            urlConnection.setDoOutput(true);

            // Todos los PHP de la app devuelven objetos JSON
            urlConnection.setRequestProperty("Content-Type", "application/json");

            // Crear objetos JSON desde los parámetros
            JSONObject jsonParams = new JSONObject();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                jsonParams.put(entry.getKey(), entry.getValue());
            }

            // Añadir acción al JSON
            if (action != null && !action.isEmpty()) {
                jsonParams.put("action", action);
            }

            Log.d(TAG, "Enviando a " + endpoint + ": " + jsonParams.toString());

            // Escribir salida JSON
            PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
            out.print(jsonParams.toString());
            out.close();

            // Leer respuesta
            int statusCode = urlConnection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                StringBuilder responseBuilder = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                response = responseBuilder.toString();

                Log.d(TAG, "Respuesta de " + endpoint + ": " + response);
            } else {
                throw new IOException("Código de respuesta HTTP: " + statusCode);
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error cerrando el reader", e);
                }
            }
        }

        return response;
    }
}