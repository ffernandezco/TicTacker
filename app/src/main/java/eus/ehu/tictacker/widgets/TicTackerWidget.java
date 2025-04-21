package eus.ehu.tictacker.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eus.ehu.tictacker.DatabaseHelper;
import eus.ehu.tictacker.Fichaje;
import eus.ehu.tictacker.R;
import eus.ehu.tictacker.WorkTimeCalculator;
import eus.ehu.tictacker.MainActivity;
import eus.ehu.tictacker.ForegroundTimeService;

public class TicTackerWidget extends AppWidgetProvider {
    private static final String TAG = "TicTackerWidget";
    public static final String ACTION_UPDATE_WIDGET = "eus.ehu.tictacker.UPDATE_WIDGET";
    public static final String ACTION_CLOCK_IN_OUT = "eus.ehu.tictacker.CLOCK_IN_OUT";
    public static final String ACTION_MINUTE_UPDATE = "eus.ehu.tictacker.MINUTE_UPDATE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Actualizar todos los widgets
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_UPDATE_WIDGET.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, TicTackerWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        } else if (ACTION_CLOCK_IN_OUT.equals(intent.getAction())) {
            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            handleClockInOut(context, widgetId);
        } else if (ACTION_MINUTE_UPDATE.equals(intent.getAction())) {
            // Actualizar los widgets según el tiempo
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, TicTackerWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    private void handleClockInOut(Context context, int widgetId) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        String username = dbHelper.getCurrentUsername(context);

        // Comprobar que el usuario ha iniciado sesión
        if (username == null || username.equals("unknown_user")) {
            // Si no ha iniciado sesión, llevar a pantalla principal
            Intent openApp = new Intent(context, MainActivity.class);
            openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(openApp);
            return;
        }

        SimpleDateFormat sdfFecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        String fechaActual = sdfFecha.format(new Date());
        String horaActual = sdfHora.format(new Date());

        // Obtener fichaje actual
        dbHelper.obtenerUltimoFichajeDelDia(fechaActual, username, new DatabaseHelper.FichajeCallback() {
            @Override
            public void onFichajeReceived(Fichaje ultimoFichaje) {
                if (ultimoFichaje == null || ultimoFichaje.horaSalida != null) {
                    // Fichar entrada
                    Fichaje nuevoFichaje = new Fichaje();
                    nuevoFichaje.fecha = fechaActual;
                    nuevoFichaje.horaEntrada = horaActual;
                    nuevoFichaje.horaSalida = null;
                    nuevoFichaje.latitud = 0.0; // No se puede registrar la ubicación desde el widget
                    nuevoFichaje.longitud = 0.0;
                    nuevoFichaje.username = username;

                    dbHelper.insertarFichaje(nuevoFichaje, success -> {
                        // Actualizar el widget una vez se ha fichado
                        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                        updateAppWidget(context, appWidgetManager, widgetId);

                        // Actualizar el widget cada minuto si se ha fichado
                        scheduleMinuteUpdates(context, true);

                        // Iniciar el servicio en primer plano
                        startForegroundService(context);
                    });
                } else {
                    // Salida
                    ultimoFichaje.horaSalida = horaActual;

                    dbHelper.actualizarFichaje(ultimoFichaje, success -> {
                        // Actualizar el contenido del widget
                        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                        updateAppWidget(context, appWidgetManager, widgetId);

                        // Quitar actualizaciones cada minuto si se sale del fichaje para ahorrar recursos
                        scheduleMinuteUpdates(context, false);

                        // Comprobar si hay otros fichajes activos, si no, detener el servicio
                        checkForActiveClockInsAndStopService(context, ultimoFichaje);
                    });
                }
            }
        });
    }

    // Método para comprobar si hay fichajes activos y detener el servicio si no hay ninguno
    private void checkForActiveClockInsAndStopService(Context context, Fichaje currentFichaje) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        String username = dbHelper.getCurrentUsername(context);

        dbHelper.obtenerFichajesDeHoy(username, new DatabaseHelper.FichajesCallback() {
            @Override
            public void onFichajesReceived(List<Fichaje> todaysFichajes) {
                boolean hasActiveEntries = false;
                for (Fichaje fichaje : todaysFichajes) {
                    if (fichaje.id != currentFichaje.id &&
                            fichaje.horaEntrada != null &&
                            (fichaje.horaSalida == null || fichaje.horaSalida.isEmpty())) {
                        hasActiveEntries = true;
                        break;
                    }
                }

                if (!hasActiveEntries) {
                    // Detener servicio si no hay entradas activas
                    stopForegroundService(context);
                }
            }
        });
    }

    /*
    private void startForegroundService(Context context) {
        Intent serviceIntent = new Intent(context, ForegroundTimeService.class);
        serviceIntent.setAction("START_FOREGROUND");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        Log.d(TAG, "Servicio ForegroundTimeService iniciado desde el widget");
    }
     */

    // Método para detener el servicio en primer plano
    private void stopForegroundService(Context context) {
        Intent serviceIntent = new Intent(context, ForegroundTimeService.class);
        serviceIntent.setAction("STOP_FOREGROUND");
        context.startService(serviceIntent);

        Log.d(TAG, "Servicio ForegroundTimeService detenido desde el widget");
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Actualizar vistas de widgets
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.tictacker_widget);

        // Primero cargar datos en caché para mostrar algo inmediatamente
        loadCachedWidgetData(context, views);

        // Actualizar inmediatamente con valores de caché
        appWidgetManager.updateAppWidget(appWidgetId, views);

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        String username = dbHelper.getCurrentUsername(context);

        // Comprobar si el usuario ha iniciado sesión
        if (username == null || username.equals("unknown_user")) {
            // Si el usuario no ha iniciado sesión
            views.setTextViewText(R.id.tv_widget_status, context.getString(R.string.not_logged_in));
            views.setTextViewText(R.id.tv_widget_time_worked, "");
            views.setTextViewText(R.id.btn_widget_fichar, context.getString(R.string.open_app));

            Intent openAppIntent = new Intent(context, MainActivity.class);
            PendingIntent openAppPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.btn_widget_fichar, openAppPendingIntent);

            // Actualizar el widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        // Obtener estado actual
        dbHelper.obtenerFichajesDeHoy(username, new DatabaseHelper.FichajesCallback() {
            @Override
            public void onFichajesReceived(List<Fichaje> todaysFichajes) {
                // Extraer configuraciones para conocer tiempo de trabajo
                dbHelper.getSettings(new DatabaseHelper.SettingsCallback() {
                    @Override
                    public void onSettingsReceived(float[] settings) {
                        // Cálculos de horas
                        float weeklyHours = settings[0];
                        int workingDays = (int) settings[1];

                        float dailyHours = WorkTimeCalculator.calculateDailyHours(weeklyHours, workingDays);
                        long[] timeWorked = WorkTimeCalculator.getTimeWorkedToday(todaysFichajes);
                        long[] timeRemaining = WorkTimeCalculator.getRemainingTime(timeWorked, dailyHours);

                        // Comprobamos si hay algún fichaje en curso
                        boolean isClockedIn = false;
                        String lastClockInTime = "";

                        for (Fichaje fichaje : todaysFichajes) {
                            if (fichaje.horaEntrada != null && (fichaje.horaSalida == null || fichaje.horaSalida.isEmpty())) {
                                isClockedIn = true;
                                lastClockInTime = fichaje.horaEntrada != null ? fichaje.horaEntrada : "";
                                break;
                            }
                        }

                        // Actualizar el widget según el estado actual
                        String statusText;
                        String timeMessage = "";

                        if (isClockedIn) {
                            statusText = context.getString(R.string.estado_fichado, lastClockInTime);
                            views.setTextViewText(R.id.tv_widget_status, statusText);
                            views.setTextViewText(R.id.btn_widget_fichar,
                                    context.getString(R.string.fichar_salida));

                            // Mostrar tiempo sin los segundos
                            if (timeRemaining[2] == 1) { // Horas extra
                                long extraHours = timeWorked[0] / 60;
                                long extraMinutes = timeWorked[0] % 60;

                                if (extraHours > 0) {
                                    if (extraMinutes > 0) {
                                        timeMessage = "+" + extraHours + "h " + extraMinutes + "m extra";
                                    } else {
                                        timeMessage = "+" + extraHours + "h extra";
                                    }
                                } else {
                                    timeMessage = "+" + extraMinutes + "m extra";
                                }
                            } else { // Tiempo restante
                                long remainingHours = timeRemaining[0] / 60;
                                long remainingMinutes = timeRemaining[0] % 60;

                                if (remainingHours > 0) {
                                    if (remainingMinutes > 0) {
                                        timeMessage = "Te quedan " + remainingHours + "h " + remainingMinutes + "m";
                                    } else {
                                        timeMessage = "Te quedan " + remainingHours + "h";
                                    }
                                } else if (remainingMinutes > 0) {
                                    timeMessage = "Te quedan " + remainingMinutes + "m";
                                } else {
                                    timeMessage = "¡Tiempo completado!";
                                }
                            }

                            views.setTextViewText(R.id.tv_widget_time_worked, timeMessage);

                            // Actualizar cada minuto
                            scheduleMinuteUpdates(context, true);

                            // Asegurarse de que el servicio de primer plano esté activo
                            startForegroundService(context);

                            // Guardar en caché
                            cacheWidgetData(context, true, statusText, timeMessage);
                        } else {
                            statusText = context.getString(R.string.estado_no_fichado);
                            views.setTextViewText(R.id.tv_widget_status, statusText);
                            views.setTextViewText(R.id.btn_widget_fichar,
                                    context.getString(R.string.fichar_entrada));

                            // Si no hay fichaje activo, mostrar tiempo trabajado
                            long hoursWorked = timeWorked[0] / 60;
                            long minutesWorked = timeWorked[0] % 60;

                            if (hoursWorked > 0 || minutesWorked > 0) {
                                String timeWorkedStr = "";
                                if (hoursWorked > 0) {
                                    timeWorkedStr += hoursWorked + "h ";
                                }
                                if (minutesWorked > 0) {
                                    timeWorkedStr += minutesWorked + "m";
                                }
                                timeMessage = context.getString(R.string.time_worked, timeWorkedStr);
                                views.setTextViewText(R.id.tv_widget_time_worked, timeMessage);
                            } else {
                                views.setTextViewText(R.id.tv_widget_time_worked, "");
                            }

                            // Quitar actualizaciones cada minuto para ahorrar recursos
                            scheduleMinuteUpdates(context, false);

                            // Guardar en caché
                            cacheWidgetData(context, false, statusText, timeMessage);
                        }

                        // Configuración del botón
                        Intent clockInOutIntent = new Intent(context, TicTackerWidget.class);
                        clockInOutIntent.setAction(ACTION_CLOCK_IN_OUT);
                        clockInOutIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        PendingIntent clockInOutPendingIntent = PendingIntent.getBroadcast(
                                context, appWidgetId, clockInOutIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        views.setOnClickPendingIntent(R.id.btn_widget_fichar, clockInOutPendingIntent);

                        // Actualización del widget
                        new Handler(Looper.getMainLooper()).post(() -> {
                            appWidgetManager.updateAppWidget(appWidgetId, views);
                        });
                    }
                });
            }
        });
    }

    // Actualizaciones cada minuto para el widget mediante alarma
    private static void scheduleMinuteUpdates(Context context, boolean enable) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TicTackerWidget.class);
        intent.setAction(ACTION_MINUTE_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (enable) {
            // Programar actualizaciones por minuto
            long startTime = System.currentTimeMillis();
            long intervalMillis = 60 * 1000; // 1 minuto

            alarmManager.setRepeating(
                    AlarmManager.RTC,
                    startTime + intervalMillis,
                    intervalMillis,
                    pendingIntent);

            Log.d(TAG, "Actualizando widget cada minuto");
        } else {
            // Cancelar actualización cada minuto
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();

            Log.d(TAG, "Desactivada alarma del widget");
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        updateActiveClockInStatus(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        scheduleMinuteUpdates(context, false);
    }

    // Actualizar alarmas del widget según estado del fichaje
    private void updateActiveClockInStatus(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        String username = dbHelper.getCurrentUsername(context);

        if (username != null && !username.equals("unknown_user")) { // Si se ha iniciado sesión
            dbHelper.obtenerFichajesDeHoy(username, fichajes -> {
                boolean hasActiveClockIn = false;
                for (Fichaje fichaje : fichajes) {
                    if (fichaje.horaEntrada != null && (fichaje.horaSalida == null || fichaje.horaSalida.isEmpty())) {
                        hasActiveClockIn = true;
                        break;
                    }
                }
                scheduleMinuteUpdates(context, hasActiveClockIn);

                // Si hay un fichaje activo, asegurarse de que el servicio está funcionando
                if (hasActiveClockIn) {
                    startForegroundService(context);
                }
            });
        }
    }

    private static void startForegroundService(Context context) {
        Intent serviceIntent = new Intent(context, ForegroundTimeService.class);
        serviceIntent.setAction("START_FOREGROUND");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        Log.d(TAG, "Servicio ForegroundTimeService iniciado desde método estático");
    }

    private static void cacheWidgetData(Context context, boolean isClockedIn, String statusText, String timeWorkedText) {
        SharedPreferences prefs = context.getSharedPreferences("WidgetCache", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isClockedIn", isClockedIn);
        editor.putString("statusText", statusText);
        editor.putString("timeWorkedText", timeWorkedText);
        editor.putLong("lastUpdated", System.currentTimeMillis());
        editor.apply();
    }

    private static void loadCachedWidgetData(Context context, RemoteViews views) {
        SharedPreferences prefs = context.getSharedPreferences("WidgetCache", Context.MODE_PRIVATE);
        boolean isClockedIn = prefs.getBoolean("isClockedIn", false);
        String statusText = prefs.getString("statusText", context.getString(R.string.cargando));
        String timeWorkedText = prefs.getString("timeWorkedText", "");
        long lastUpdated = prefs.getLong("lastUpdated", 0);

        // Usar datos solo si se han obtenido hace menos de 5 minutos
        if (System.currentTimeMillis() - lastUpdated < 5 * 60 * 1000) {
            views.setTextViewText(R.id.tv_widget_status, statusText);
            views.setTextViewText(R.id.tv_widget_time_worked, timeWorkedText);
            views.setTextViewText(R.id.btn_widget_fichar,
                    isClockedIn ? context.getString(R.string.fichar_salida) : context.getString(R.string.fichar_entrada));
        } else {
            // Valores por defecto en caso de no tener caché
            views.setTextViewText(R.id.tv_widget_status, context.getString(R.string.cargando));
            views.setTextViewText(R.id.tv_widget_time_worked, "");
            views.setTextViewText(R.id.btn_widget_fichar, context.getString(R.string.fichar_entrada));
        }
    }
}