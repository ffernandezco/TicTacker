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
            if (!notificationHelper.areNotificationsEnabled() ||
                    !notificationHelper.shouldSendNotification(appContext)) {
                return Result.success();
            }

            String username = dbHelper.getCurrentUsername(appContext);
            dbHelper.obtenerFichajesDeHoy(username, todaysFichajes -> {
                dbHelper.getSettings(settings -> {
                    float weeklyHours = settings[0] <= 0 ? 40 : settings[0];
                    int workingDays = (int) (settings[1] <= 0 ? 5 : settings[1]);

                    float dailyHours = WorkTimeCalculator.calculateDailyHours(weeklyHours, workingDays);
                    long[] timeWorked = WorkTimeCalculator.getTimeWorkedToday(todaysFichajes);
                    long[] timeRemaining = WorkTimeCalculator.getRemainingTime(timeWorked, dailyHours);

                    boolean isClockedIn = WorkTimeCalculator.isCurrentlyClockedIn(todaysFichajes);

                    if (isClockedIn && (timeRemaining[2] == 1 ||
                            (timeRemaining[0] == 0 && timeRemaining[1] <= 1))) {
                        notificationHelper.sendWorkCompleteNotification();
                    }
                });
            });

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error en WorkTimeCheckWorker", e);
            return Result.failure();
        }
    }
}