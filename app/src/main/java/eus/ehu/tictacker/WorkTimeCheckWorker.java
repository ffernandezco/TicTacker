package eus.ehu.tictacker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.util.Log;
import java.util.List;

public class WorkTimeCheckWorker extends Worker {
    private final DatabaseHelper dbHelper;
    private final NotificationHelper notificationHelper;
    private static final String TAG = "WorkTimeCheckWorker";

    public WorkTimeCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        dbHelper = new DatabaseHelper(context);
        notificationHelper = new NotificationHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context appContext = getApplicationContext();

        try {
            // Comprobar si las notificaciones están habilitadas
            if (!notificationHelper.areNotificationsEnabled()) {
                Log.d(TAG, "Notificaciones no activadas");
                return Result.success();
            }

            // Comprobar si han pasado los 10 mins
            if (!notificationHelper.shouldSendNotification(appContext)) {
                Log.d(TAG, "Notificación muy cerca de la anterior");
                return Result.success();
            }

            String username = dbHelper.getCurrentUsername(appContext);
            List<Fichaje> todaysFichajes = dbHelper.obtenerFichajesDeHoy(username);
            float[] settings = dbHelper.getSettings();
            float weeklyHours = settings[0];
            int workingDays = (int) settings[1];

            // Valores por defecto
            if (weeklyHours <= 0) weeklyHours = 40;
            if (workingDays <= 0) workingDays = 5;

            float dailyHours = WorkTimeCalculator.calculateDailyHours(weeklyHours, workingDays);
            long[] timeWorked = WorkTimeCalculator.getTimeWorkedToday(todaysFichajes);
            long[] timeRemaining = WorkTimeCalculator.getRemainingTime(timeWorked, dailyHours);

            boolean isClockedIn = WorkTimeCalculator.isCurrentlyClockedIn(todaysFichajes);

            Log.d(TAG, "Tiempo trabajado: " + timeWorked[0] + ":" + timeWorked[1]);
            Log.d(TAG, "Tiempo restante: " + timeRemaining[2] + " (-1)");
            Log.d(TAG, "Estado fichaje: " + isClockedIn);

            // Envío de notificación si se entra en horas extra (timeRemaining[2] == 1) o se alcanza tiempo (timeRemaining[0] == 0 && timeRemaining[1] == 0)
            if (isClockedIn && (timeRemaining[2] == 1 || (timeRemaining[0] == 0 && timeRemaining[1] <= 1))) {
                Log.d(TAG, "Envío de notificación");
                notificationHelper.sendWorkCompleteNotification();
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error del WorkTimeCheckWorker", e);
            return Result.failure();
        }
    }
}