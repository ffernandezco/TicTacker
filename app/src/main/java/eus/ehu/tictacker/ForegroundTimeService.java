package eus.ehu.tictacker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import android.content.pm.ServiceInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ForegroundTimeService extends Service {

    private static final String CHANNEL_ID = "foreground_service_channel";
    private static final int NOTIFICATION_ID = 1003;
    private static final long UPDATE_INTERVAL = 1000; // Actualizar cada segundo

    private DatabaseHelper dbHelper;
    private Handler handler;
    private Runnable updateRunnable;
    private Date startTime;
    private String username;
    private boolean isRunning = false;

    // Comunicación entre la actividad y el servicio
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends android.os.Binder {
        public ForegroundTimeService getService() {
            return ForegroundTimeService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DatabaseHelper(this);
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "START_FOREGROUND":
                        startForegroundService();
                        break;
                    case "STOP_FOREGROUND":
                        stopForegroundService();
                        break;
                }
            }
        }

        // Reiniciar servicio en caso de que se suprima
        return START_STICKY;
    }

    private void startForegroundService() {
        if (isRunning) return;

        username = dbHelper.getCurrentUsername(this);
        startTime = new Date();

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateNotification();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };

        // Iniciar actualizaciones periódicas de la app
        handler.post(updateRunnable);

        // Iniciar servicio en primer plano con notificación
        Notification notification = buildNotification("00:00:00", "");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        isRunning = true;
    }

    private void stopForegroundService() {
        isRunning = false;
        handler.removeCallbacks(updateRunnable);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }

        stopSelf();
    }

    private void updateNotification() {
        if (!isRunning) return;

        dbHelper.obtenerFichajesDeHoy(username, new DatabaseHelper.FichajesCallback() {
            @Override
            public void onFichajesReceived(List<Fichaje> todaysFichajes) {
                dbHelper.getSettings(new DatabaseHelper.SettingsCallback() {
                    @Override
                    public void onSettingsReceived(float[] settings) {
                        float weeklyHours = settings[0];
                        int workingDays = (int) settings[1];

                        float dailyHours = WorkTimeCalculator.calculateDailyHours(weeklyHours, workingDays);
                        long[] timeWorked = WorkTimeCalculator.getTimeWorkedToday(todaysFichajes);
                        long[] timeRemaining = WorkTimeCalculator.getRemainingTime(timeWorked, dailyHours);

                        String timeWorkedStr = WorkTimeCalculator.formatTime(timeWorked[0], timeWorked[1]);
                        String timeRemainingStr = WorkTimeCalculator.formatTime(timeRemaining[0], timeRemaining[1]);

                        // Actualizar la notificación
                        Notification notification = buildNotification(timeWorkedStr, timeRemainingStr);
                        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.notify(NOTIFICATION_ID, notification);
                    }
                });
            }
        });
    }

    private Notification buildNotification(String timeWorked, String timeRemaining) {
        // Crear un intent para ir a MainActivity
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Crea la notificación
        String contentText = getString(R.string.time_worked, timeWorked);
        if (!timeRemaining.isEmpty()) {
            contentText += " - " + getString(R.string.time_remaining, timeRemaining);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Trabajo en Primer Plano",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Canal para mostrar el tiempo trabajado");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isRunning) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void forceUpdateNotification() {
        if (isRunning) {
            updateNotification();
        }
    }
}