package eus.ehu.tictacker;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    private static final String BASE_URL = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ffernandez032/WEB/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static ApiClient instance;
    private OkHttpClient client;
    private Gson gson;
    private ExecutorService executorService;
    private Handler mainHandler;

    private ApiClient() {
        client = new OkHttpClient.Builder()
                .build();
        gson = new Gson();
        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    // Métodos síncronos clásicos - Pueden hacer que la app falle
    public JsonObject post(String endpoint, Object data) throws IOException {
        String json = gson.toJson(data);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Código inesperado: " + response);
            }
            String responseBody = response.body().string();
            return gson.fromJson(responseBody, JsonObject.class);
        }
    }

    // Métodos asíncronos para conexiones a BD
    public interface ApiCallback<T> {
        void onResponse(T result);
        void onFailure(Exception e);
    }

    public void postAsync(String endpoint, Object data, ApiCallback<JsonObject> callback) {
        executorService.execute(() -> {
            try {
                JsonObject result = post(endpoint, data);
                mainHandler.post(() -> callback.onResponse(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }

    public <T> void postAndGetListAsync(String endpoint, Object data, Class<T> type, ApiCallback<List<T>> callback) {
        postAsync(endpoint, data, new ApiCallback<JsonObject>() {
            @Override
            public void onResponse(JsonObject response) {
                try {
                    Type listType = TypeToken.getParameterized(List.class, type).getType();
                    List<T> list = gson.fromJson(response.get("data"), listType);
                    callback.onResponse(list);
                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public <T> void postAndGetObjectAsync(String endpoint, Object data, Class<T> type, ApiCallback<T> callback) {
        postAsync(endpoint, data, new ApiCallback<JsonObject>() {
            @Override
            public void onResponse(JsonObject response) {
                try {
                    T object = gson.fromJson(response.get("data"), type);
                    callback.onResponse(object);
                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    // Para mejorar compatibilidad
    public <T> List<T> postAndGetList(String endpoint, Object data, Class<T> type) throws IOException {
        JsonObject response = post(endpoint, data);
        Type listType = TypeToken.getParameterized(List.class, type).getType();
        return gson.fromJson(response.get("data"), listType);
    }

    public <T> T postAndGetObject(String endpoint, Object data, Class<T> type) throws IOException {
        JsonObject response = post(endpoint, data);
        return gson.fromJson(response.get("data"), type);
    }
}