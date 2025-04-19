package eus.ehu.tictacker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.Manifest;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import android.content.pm.PackageManager;

import java.util.Date;
import java.util.Locale;

public class NotificationHelper {
    private static final String CHANNEL_ID = "trabajo_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int REMINDER_NOTIFICATION_ID = 1002;
    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notificaciones de trabajo";
            String description = "Notificaciones relacionadas con las horas de trabajo";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Registrar el canal
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void sendWorkCompleteNotification() {
        if (!areNotificationsEnabled()) {
            return; // No enviar notificaciones si no se acepta
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            recordNotificationSent(context); // Almacenar envío de la notificación
        } catch (SecurityException e) {
            // Si las notificaciones no están habilitadas
            e.printStackTrace();
        }
    }

    public boolean shouldSendNotification(Context context) {
        // Comprobar si las notificaciones están habilitadas
        if (!areNotificationsEnabled()) {
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String lastNotificationDate = prefs.getString("last_notification_date", "");
        String lastNotificationHour = prefs.getString("last_notification_hour", "");
        String lastNotificationMinute = prefs.getString("last_notification_minute", "");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH", Locale.getDefault());
        SimpleDateFormat minuteFormat = new SimpleDateFormat("mm", Locale.getDefault());

        String currentDate = dateFormat.format(new Date());
        String currentHour = hourFormat.format(new Date());
        String currentMinute = minuteFormat.format(new Date());

        // Enviar notificaciones si se hace en fechas distintas
        if (!currentDate.equals(lastNotificationDate)) {
            return true;
        }

        // Enviar notificaciones si se hace en horas distintas
        if (!currentHour.equals(lastNotificationHour)) {
            return true;
        }

        int lastMinute = 0;
        int currentMinuteInt = Integer.parseInt(currentMinute);

        try {
            lastMinute = Integer.parseInt(lastNotificationMinute);
        } catch (NumberFormatException e) {
            return true;
        }

        // Se envían notificaciones solo si han pasado 10 mins de la anterior
        return Math.abs(currentMinuteInt - lastMinute) >= 10;
    }

    public void recordNotificationSent(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH", Locale.getDefault());
        SimpleDateFormat minuteFormat = new SimpleDateFormat("mm", Locale.getDefault());

        editor.putString("last_notification_date", dateFormat.format(new Date()));
        editor.putString("last_notification_hour", hourFormat.format(new Date()));
        editor.putString("last_notification_minute", minuteFormat.format(new Date()));
        editor.apply();
    }

    public boolean areNotificationsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                    context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED;
        } else {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
    }

    // Enviar notificaciones a partir del broadcast de la alarma
    public void sendClockInReminderNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Crear intent para cuando el usuario pulsa la notificación
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        // Añadir botón de acción para ir directamente a la sección de ficjar
        Intent clockInIntent = new Intent(context, MainActivity.class);
        clockInIntent.putExtra("open_clock_in", true);
        clockInIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent clockInPendingIntent = PendingIntent.getActivity(
                context, 1, clockInIntent, PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(R.drawable.ic_notification, context.getString(R.string.clock_in_now), clockInPendingIntent);

        // Mostrar la notificación
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(REMINDER_NOTIFICATION_ID, builder.build());
        }
    }

}