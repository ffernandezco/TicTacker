package eus.ehu.tictacker;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.content.SharedPreferences;
import android.util.Log;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCM_Service";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo token FCM: " + token);

        // Guardar el nuevo token en SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("fcm_token", token);
        editor.apply();

        // Actualizar token en el servidor si el usuario ha iniciado sesión
        String currentUser = sharedPreferences.getString("usuario_actual", null);
        if (currentUser != null) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            dbHelper.updateFCMToken(currentUser, token, success -> {
                if (success) {
                    Log.d(TAG, "Token actualizado en el servidor");
                } else {
                    Log.e(TAG, "Error al actualizar el token en el servidor");
                }
            });
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Mensaje recibido de: " + remoteMessage.getFrom());

        String title = null;
        String body = null;

        // Intentar obtener título y cuerpo desde la notificación recibida
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Notificación recibida: " + title + " - " + body);
        }

        // Si falta dato, intentar obtener del cuerpo del mensaje
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Datos: " + remoteMessage.getData());

            // Solo usar datos si no tenemos valores de notificación
            if (title == null) {
                title = remoteMessage.getData().get("title");
            }
            if (body == null) {
                body = remoteMessage.getData().get("body");
            }
        }

        // En caso de no tener datos poner información por defecto
        title = title != null ? title : "Notificación";
        body = body != null ? body : "Has recibido una nueva notificación";

        // Mostrar notificación manualmente
        // Usado en caso de que la aplicación se encuentre abierta en primer plano
        NotificationHelper.showNotification(this, title, body);
    }
}