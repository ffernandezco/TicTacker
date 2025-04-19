package eus.ehu.tictacker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class ClockInReminderService {
    private static final String TAG = "ClockInReminderService";
    private static final int REMINDER_REQUEST_CODE = 1234;

    // Configurar alarma según preferencias del usuario
    public static void scheduleReminder(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean reminderEnabled = prefs.getBoolean("reminder_enabled", false);

        if (!reminderEnabled) {
            cancelReminder(context);
            return;
        }

        // Verificar permiso en Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                // Mostrar explicación y redirigir a configuración
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            }
        }

        int reminderHour = prefs.getInt("reminder_hour", 9);
        int reminderMinute = prefs.getInt("reminder_minute", 0);

        // Configurar calendario para disparar alarma
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, reminderHour);
        calendar.set(Calendar.MINUTE, reminderMinute);
        calendar.set(Calendar.SECOND, 0);

        // Si el tiempo ha pasado, lanzar la alarma el siguiente día
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Enviar alarma al broadcast receiver
        Intent intent = new Intent(context, ClockInReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Configuración de la alarma
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // Cancelar alarmas existentes
            alarmManager.cancel(pendingIntent);

            // Programar nueva alarma
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }

            Log.d(TAG, "Reminder scheduled for " + calendar.getTime().toString());
        }
    }

    // Cancelar recordatorio existente
    public static void cancelReminder(Context context) {
        Intent intent = new Intent(context, ClockInReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Reminder cancelled");
        }
    }
}