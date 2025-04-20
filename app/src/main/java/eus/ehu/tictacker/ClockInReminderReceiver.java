package eus.ehu.tictacker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClockInReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ClockInReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarma recivida en " + new Date().toString());

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        String username = dbHelper.getCurrentUsername(context);
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        dbHelper.obtenerFichajesDeHoy(username, new DatabaseHelper.FichajesCallback() {
            @Override
            public void onFichajesReceived(List<Fichaje> fichajes) {
                boolean hasClockInToday = false;
                boolean hasActiveClockIn = false;

                for (Fichaje fichaje : fichajes) {
                    if (fichaje.horaSalida == null || fichaje.horaSalida.isEmpty()) {
                        hasActiveClockIn = true;
                    }
                    if (fichaje.horaEntrada != null && !fichaje.horaEntrada.isEmpty()) {
                        hasClockInToday = true;
                    }
                }

                Log.d(TAG, "Check results - hasClockInToday: " + hasClockInToday +
                        ", hasActiveClockIn: " + hasActiveClockIn);

                // Enviar notificación si no hay fichajes hoy o hay uno activo sin finalizar
                if (!hasClockInToday || hasActiveClockIn) {
                    String currentTime = DateFormat.getTimeFormat(context).format(new Date());
                    String title = context.getString(R.string.reminder_notification_title);
                    String message = context.getString(R.string.reminder_notification_message, currentTime);

                    NotificationHelper notificationHelper = new NotificationHelper(context);
                    notificationHelper.sendClockInReminderNotification(title, message);
                    Log.d(TAG, "Notificación enviada");
                }

                // Programar la alarma para el día siguiente
                ClockInReminderService.scheduleReminder(context);
            }
        });
    }
}