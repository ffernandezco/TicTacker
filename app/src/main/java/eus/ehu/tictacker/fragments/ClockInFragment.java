package eus.ehu.tictacker.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import eus.ehu.tictacker.DatabaseHelper;
import eus.ehu.tictacker.Fichaje;
import eus.ehu.tictacker.FichajeEvents;
import eus.ehu.tictacker.NotificationHelper;
import eus.ehu.tictacker.R;
import eus.ehu.tictacker.WorkTimeCalculator;

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
    private static final String TAG = "ClockInFragment";

    private boolean notificationShownThisSession = false;
    private long lastNotificationCheckTime = 0;

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
        dbHelper.close();
        FichajeEvents.setListener(null);
    }

    private void checkLocationPermissionAndRegister() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
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

                    dbHelper.insertarFichaje(nuevoFichaje, success -> {
                        if (success) {
                            getActivity().runOnUiThread(() -> {
                                FichajeEvents.notifyFichajeChanged();
                                actualizarEstadoUI();
                                Toast.makeText(requireContext(),
                                        "Entrada registrada: " + horaActual,
                                        Toast.LENGTH_SHORT).show();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndRegister();
            }
        }
    }
}