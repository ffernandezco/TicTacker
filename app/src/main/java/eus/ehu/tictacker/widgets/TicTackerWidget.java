package eus.ehu.tictacker.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

public class TicTackerWidget extends AppWidgetProvider {
    private static final String TAG = "TicTackerWidget";
    public static final String ACTION_UPDATE_WIDGET = "eus.ehu.tictacker.UPDATE_WIDGET";
    public static final String ACTION_CLOCK_IN_OUT = "eus.ehu.tictacker.CLOCK_IN_OUT";

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
                    // Fichar
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
                    });
                } else {
                    // Salida
                    ultimoFichaje.horaSalida = horaActual;

                    dbHelper.actualizarFichaje(ultimoFichaje, success -> {
                        // Actualizar el contenido del widget
                        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                        updateAppWidget(context, appWidgetManager, widgetId);
                    });
                }
            }
        });
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Actualizar vistas de widgets
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.tictacker_widget);

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

                        String timeWorkedStr = WorkTimeCalculator.formatTime(timeWorked[0], timeWorked[1]);

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
                        if (isClockedIn) {
                            views.setTextViewText(R.id.tv_widget_status,
                                    context.getString(R.string.estado_fichado, lastClockInTime));
                            views.setTextViewText(R.id.btn_widget_fichar,
                                    context.getString(R.string.fichar_salida));
                        } else {
                            views.setTextViewText(R.id.tv_widget_status,
                                    context.getString(R.string.estado_no_fichado));
                            views.setTextViewText(R.id.btn_widget_fichar,
                                    context.getString(R.string.fichar_entrada));
                        }

                        views.setTextViewText(R.id.tv_widget_time_worked,
                                context.getString(R.string.time_worked, timeWorkedStr));

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

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }
}