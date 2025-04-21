package eus.ehu.tictacker.fragments;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import eus.ehu.tictacker.DatabaseHelper;
import eus.ehu.tictacker.Fichaje;
import eus.ehu.tictacker.FichajeEvents;
import eus.ehu.tictacker.NotificationHelper;
import eus.ehu.tictacker.R;
import eus.ehu.tictacker.WorkTimeCalculator;
import eus.ehu.tictacker.ForegroundTimeService;

public class ClockInFragment extends Fragment {
    private DatabaseHelper dbHelper;
    private TextView tvEstadoFichaje;
    private TextView tvTimeWorked;
    private TextView tvTimeRemaining;
    private Button btnFichar;

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private NotificationHelper notificationHelper;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int CALENDAR_PERMISSION_REQUEST_CODE = 1002;
    private static final String TAG = "ClockInFragment";

    private boolean notificationShownThisSession = false;
    private long lastNotificationCheckTime = 0;

    private ForegroundTimeService timeService;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ForegroundTimeService.LocalBinder binder = (ForegroundTimeService.LocalBinder) service;
            timeService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            timeService = null;
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_clock_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        notificationHelper = new NotificationHelper(requireContext());

        tvEstadoFichaje = view.findViewById(R.id.tvEstadoFichaje);
        tvTimeWorked = view.findViewById(R.id.tvTimeWorked);
        tvTimeRemaining = view.findViewById(R.id.tvTimeRemaining);
        btnFichar = view.findViewById(R.id.btnFichar);

        btnFichar.setOnClickListener(v -> checkLocationPermissionAndRegister());

        actualizarEstadoUI();

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                actualizarEstadoUI();
                checkWorkTimeCompleted();
                timerHandler.postDelayed(this, 1000);
            }
        };

        timerHandler.postDelayed(timerRunnable, 1000);

        notificationShownThisSession = false;

        Intent serviceIntent = new Intent(requireContext(), ForegroundTimeService.class);
        requireContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        FichajeEvents.setListener(new FichajeEvents.FichajeChangeListener() {
            @Override
            public void onFichajeChanged() {
                if (isAdded() && getContext() != null) {
                    timerHandler.removeCallbacks(timerRunnable);
                    actualizarEstadoUI();
                    timerHandler.postDelayed(timerRunnable, 1000);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizarEstadoUI();
        timerHandler.postDelayed(timerRunnable, 1000);
        notificationShownThisSession = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (serviceBound) {
            requireContext().unbindService(serviceConnection);
            serviceBound = false;
        }
        FichajeEvents.setListener(null);
    }

    private void checkLocationPermissionAndRegister() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            checkCalendarPermissionAndRegister();
        }
    }

    private void checkCalendarPermissionAndRegister() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.WRITE_CALENDAR,
                            Manifest.permission.READ_CALENDAR,
                            Manifest.permission.GET_ACCOUNTS
                    },
                    CALENDAR_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocationAndRegister();
        }
    }

    private void getCurrentLocationAndRegister() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    double latitude = 0.0;
                    double longitude = 0.0;

                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                    }

                    registrarFichaje(latitude, longitude);
                });
    }

    private void registrarFichaje(double latitude, double longitude) {
        SimpleDateFormat sdfFecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        String fechaActual = sdfFecha.format(new Date());
        String horaActual = sdfHora.format(new Date());
        String username = dbHelper.getCurrentUsername(requireContext());

        dbHelper.obtenerUltimoFichajeDelDia(fechaActual, username, new DatabaseHelper.FichajeCallback() {
            @Override
            public void onFichajeReceived(Fichaje ultimoFichaje) {
                if (ultimoFichaje == null || ultimoFichaje.horaSalida != null) {
                    // Entrada
                    Fichaje nuevoFichaje = new Fichaje();
                    nuevoFichaje.fecha = fechaActual;
                    nuevoFichaje.horaEntrada = horaActual;
                    nuevoFichaje.horaSalida = null;
                    nuevoFichaje.latitud = latitude;
                    nuevoFichaje.longitud = longitude;
                    nuevoFichaje.username = username;

                    // Solo guardar el fichaje, sin crear evento de calendario todavía
                    dbHelper.insertarFichaje(nuevoFichaje, success -> {
                        if (success) {
                            getActivity().runOnUiThread(() -> {
                                FichajeEvents.notifyFichajeChanged();
                                actualizarEstadoUI();
                                Toast.makeText(requireContext(),
                                        "Entrada registrada: " + horaActual,
                                        Toast.LENGTH_SHORT).show();
                                startForegroundService();
                            });
                        } else {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(),
                                        "Error al registrar la entrada",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                } else {
                    // Salida
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

                                    if (timeRemaining[2] == 0 && (timeRemaining[0] > 0 || timeRemaining[1] > 0)) {
                                        showConfirmClockOutDialog(ultimoFichaje, horaActual, latitude, longitude);
                                    } else {
                                        completeClockOut(ultimoFichaje, horaActual, latitude, longitude);
                                        boolean hasActiveEntries = false;
                                        for (Fichaje f : todaysFichajes) {
                                            if (f.id != ultimoFichaje.id &&
                                                    f.horaEntrada != null &&
                                                    (f.horaSalida == null || f.horaSalida.isEmpty())) {
                                                hasActiveEntries = true;
                                                break;
                                            }
                                        }

                                        if (!hasActiveEntries) {
                                            // Detener el servicio
                                            stopForegroundService();
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void actualizarEstadoUI() {
        String username = dbHelper.getCurrentUsername(requireContext());
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

                        // Declarar como final las variables que se usarán en el lambda
                        final boolean[] isClockedIn = {false};
                        final String[] lastClockInTime = {""};

                        // Buscar el fichaje activo
                        for (Fichaje fichaje : todaysFichajes) {
                            if (fichaje.horaEntrada != null && (fichaje.horaSalida == null || fichaje.horaSalida.isEmpty())) {
                                isClockedIn[0] = true;
                                lastClockInTime[0] = fichaje.horaEntrada != null ? fichaje.horaEntrada : "";
                                break;
                            }
                        }

                        requireActivity().runOnUiThread(() -> {
                            if (isClockedIn[0]) {
                                tvEstadoFichaje.setText(getString(R.string.estado_fichado, lastClockInTime[0]));
                                btnFichar.setText(getString(R.string.fichar_salida));
                            } else {
                                tvEstadoFichaje.setText(getString(R.string.estado_no_fichado));
                                btnFichar.setText(getString(R.string.fichar_entrada));
                            }

                            tvTimeWorked.setText(getString(R.string.time_worked, timeWorkedStr));

                            int color = timeRemaining[2] == 0 ?
                                    ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark) :
                                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark);

                            tvTimeRemaining.setText(timeRemaining[2] == 0 ?
                                    getString(R.string.time_remaining, timeRemainingStr) :
                                    getString(R.string.overtime, timeRemainingStr));
                            tvTimeRemaining.setTextColor(color);
                        });
                    }
                });
            }
        });
    }

    private void completeClockOut(Fichaje fichaje, String horaSalida, double latitude, double longitude) {
        fichaje.horaSalida = horaSalida;
        fichaje.latitud = latitude;
        fichaje.longitud = longitude;

        dbHelper.actualizarFichaje(fichaje, success -> {
            if (success) {
                getActivity().runOnUiThread(() -> {
                    FichajeEvents.notifyFichajeChanged();
                    actualizarEstadoUI();
                    Toast.makeText(requireContext(),
                            "Salida registrada: " + horaSalida,
                            Toast.LENGTH_SHORT).show();
                    checkForActiveClockInsAndStopService(fichaje);

                    // Añadir al calendario un único evento que abarca desde la entrada hasta la salida
                    try {
                        String username = dbHelper.getCurrentUsername(requireContext());
                        String eventTitle = getString(R.string.work_session_title, username);
                        String eventDescription = getString(R.string.work_session_description,
                                fichaje.horaEntrada, horaSalida);

                        // Crear un evento que abarque desde la entrada hasta la salida
                        addWorkSessionToCalendar(fichaje.fecha, fichaje.horaEntrada, horaSalida,
                                eventTitle, eventDescription);
                    } catch (Exception e) {
                        Log.e(TAG, "Error al añadir evento de trabajo al calendario", e);
                    }
                });
            } else {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            "Error al registrar la salida",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private long addWorkSessionToCalendar(String date, String startTime, String endTime,
                                          String title, String description) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            // Parse start and end times
            Date eventStartDate = sdf.parse(date + " " + startTime);
            Date eventEndDate = sdf.parse(date + " " + endTime);

            if (eventStartDate == null || eventEndDate == null) return -1;

            long startTimeMillis = eventStartDate.getTime();
            long endTimeMillis = eventEndDate.getTime();

            // If end time is before start time (overnight shift), add a day to end time
            if (endTimeMillis < startTimeMillis) {
                endTimeMillis += 24 * 60 * 60 * 1000; // Add a day
            }

            // Find an editable calendar
            long calendarId = getCalendarId();
            if (calendarId == -1) {
                Log.e(TAG, "No hay ningún calendario editable");
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "No hay ningún calendario disponible", Toast.LENGTH_SHORT).show());
                return -1;
            }

            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, startTimeMillis);
            values.put(CalendarContract.Events.DTEND, endTimeMillis);
            values.put(CalendarContract.Events.TITLE, title);
            values.put(CalendarContract.Events.DESCRIPTION, description);
            values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

            // Hacer que se muestre como ocupado
            values.put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);
            // Evitar alerta
            values.put(CalendarContract.Events.HAS_ALARM, 0);

            Uri uri = requireContext().getContentResolver().insert(CalendarContract.Events.CONTENT_URI, values);
            if (uri != null) {
                long eventId = ContentUris.parseId(uri);
                Log.d(TAG, "Sesión de trabajo añadida al calendario (ID " + eventId + ")");
                return eventId;
            } else {
                Log.e(TAG, "Error al añadir evento al calendario");
                return -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al añadir evento al calendario", e);
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Error añadiendo al calendario: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return -1;
        }
    }

    private long getCalendarId() {
        // Comprobar si existe el calendario
        long ticTackerCalendarId = findTicTackerCalendar();
        if (ticTackerCalendarId != -1) {
            return ticTackerCalendarId;
        }

        // Comprobar si hay algún calendario editable
        String[] projection = new String[] {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.OWNER_ACCOUNT,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        };

        // Calendarios con opción de escritura
        String selection = "(" +
                CalendarContract.Calendars.VISIBLE + " = 1) AND (" +
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= " +
                CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR + ")";

        try (Cursor cursor = requireContext().getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                null,
                null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndex(CalendarContract.Calendars._ID);
                int nameColumn = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME);
                int accountColumn = cursor.getColumnIndex(CalendarContract.Calendars.OWNER_ACCOUNT);

                do {
                    long calendarId = cursor.getLong(idColumn);
                    String displayName = cursor.getString(nameColumn);
                    String account = cursor.getString(accountColumn);

                    Log.d(TAG, "Calendario editable encontrado: " + displayName + " (" + account + "), ID: " + calendarId);

                    // Obtener el primer calendario que puede editarse
                    return calendarId;
                } while (cursor.moveToNext());
            } else {
                Log.e(TAG, "No se han encontrado calendarios editables");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener ID del calendario", e);
        }

        // Intentar crear nuevo calendario en caso de no encontrar ninguno
        return createTicTackerCalendar();
    }

    private long findTicTackerCalendar() {
        String[] projection = new String[] {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        };

        String selection = CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " = 'TicTacker'";

        try (Cursor cursor = requireContext().getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                null,
                null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndex(CalendarContract.Calendars._ID);
                long calendarId = cursor.getLong(idColumn);
                Log.d(TAG, "Calendario TicTacker: " + calendarId);
                return calendarId;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al buscar calendario TicTacker", e);
        }
        return -1;
    }

    private long createTicTackerCalendar() {
        // Obtener las cuentas disponibles del AccountManager
        try {
            // Obtener cuenta de email primaria
            android.accounts.AccountManager accountManager = android.accounts.AccountManager.get(requireContext());
            android.accounts.Account[] accounts = accountManager.getAccountsByType("com.google");

            if (accounts.length == 0) {
                Log.e(TAG, "No se encontró ninguna cuenta Google");
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "No hay cuentas Google disponibles para crear calendario", Toast.LENGTH_SHORT).show());
                return -1;
            }

            String accountName = accounts[0].name;

            // Configurar los valores del calendario
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Calendars.ACCOUNT_NAME, accountName);
            values.put(CalendarContract.Calendars.ACCOUNT_TYPE, "com.google");
            values.put(CalendarContract.Calendars.NAME, "TicTacker");
            values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "TicTacker");
            values.put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF4285F4); // Color azul de Google
            values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
            values.put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName);
            values.put(CalendarContract.Calendars.VISIBLE, 1);
            values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);

            // Añadir el calendario
            Uri.Builder builder = CalendarContract.Calendars.CONTENT_URI.buildUpon();
            builder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true");
            builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName);
            builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, "com.google");

            Uri calendarUri = requireContext().getContentResolver().insert(builder.build(), values);
            if (calendarUri != null) {
                long calendarId = ContentUris.parseId(calendarUri);
                Log.d(TAG, "Nuevo calendario TicTacker: " + calendarId);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Calendario TicTacker creado", Toast.LENGTH_SHORT).show());
                return calendarId;
            } else {
                Log.e(TAG, "Error al crear calendario TicTacker");
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error al crear calendario TicTacker", Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creando el calendario TicTacker", e);
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
        return -1;
    }

    private void showConfirmClockOutDialog(Fichaje fichaje, String horaSalida, double latitude, double longitude) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setMessage(R.string.confirm_clock_out_message)
                .setPositiveButton(R.string.yes, (dialog, id) -> {
                    completeClockOut(fichaje, horaSalida, latitude, longitude);
                })
                .setNegativeButton(R.string.no, (dialog, id) -> dialog.dismiss())
                .create()
                .show();
    }

    private void checkWorkTimeCompleted() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationCheckTime < 5000) {
            return;
        }
        lastNotificationCheckTime = currentTime;

        if (!notificationHelper.areNotificationsEnabled() ||
                !notificationHelper.shouldSendNotification(requireContext()) ||
                notificationShownThisSession) {
            return;
        }

        String username = dbHelper.getCurrentUsername(requireContext());
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

                        boolean isClockedIn = WorkTimeCalculator.isCurrentlyClockedIn(todaysFichajes);

                        if (isClockedIn && (timeRemaining[2] == 1 || (timeRemaining[0] == 0 && timeRemaining[1] <= 1))) {
                            notificationHelper.sendWorkCompleteNotification();
                            notificationShownThisSession = true;
                        }
                    }
                });
            }
        });
    }

    private void checkForActiveClockInsAndStopService(Fichaje currentFichaje) {
        String username = dbHelper.getCurrentUsername(requireContext());
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
                    // Detener servicio
                    stopForegroundService();
                }
            }
        });
    }

    private void startForegroundService() {
        Intent serviceIntent = new Intent(requireContext(), ForegroundTimeService.class);
        serviceIntent.setAction("START_FOREGROUND");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }
    }

    private void stopForegroundService() {
        Intent serviceIntent = new Intent(requireContext(), ForegroundTimeService.class);
        serviceIntent.setAction("STOP_FOREGROUND");
        requireContext().startService(serviceIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
                checkCalendarPermissionAndRegister();
            } else if (requestCode == CALENDAR_PERMISSION_REQUEST_CODE) {
                getCurrentLocationAndRegister();
            }
        }
    }
}