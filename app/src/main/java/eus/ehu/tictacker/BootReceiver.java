package eus.ehu.tictacker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

// Se utiliza para configurar las alarmas de recordatorios de fichaje tras el reinicio del dispositivo
// https://developer.android.com/develop/background-work/services/alarms/schedule

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "Comprobando opciones de alarma");

            SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            boolean reminderEnabled = prefs.getBoolean("reminder_enabled", false);

            if (reminderEnabled) {
                Log.d(TAG, "Alarma activada");
                ClockInReminderService.scheduleReminder(context);
            }
        }
    }
}