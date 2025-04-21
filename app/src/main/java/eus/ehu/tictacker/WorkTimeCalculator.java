package eus.ehu.tictacker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WorkTimeCalculator {

    // Calcula los minutos y segundos fichados en la fecha actual
    public static long[] getTimeWorkedToday(List<Fichaje> todaysFichajes) {
        long totalSeconds = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        for (Fichaje fichaje : todaysFichajes) {
            if (fichaje.horaEntrada != null && fichaje.horaSalida != null) {
                try {
                    Date entrada = sdf.parse(ensureTimeFormat(fichaje.horaEntrada));
                    Date salida = sdf.parse(ensureTimeFormat(fichaje.horaSalida));

                    long diffInMillis = salida.getTime() - entrada.getTime();
                    long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis);

                    totalSeconds += diffInSeconds;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        // Suma el tiempo de los fichajes sin finalizar (en curso)
        Fichaje activeFichaje = getActiveFichaje(todaysFichajes);
        if (activeFichaje != null) {
            try {
                Date entrada = sdf.parse(activeFichaje.horaEntrada);
                Date now = sdf.parse(sdf.format(new Date())); // Coge la hora actual

                long diffInMillis = now.getTime() - entrada.getTime();
                long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis);

                totalSeconds += diffInSeconds;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Devuelve el tiempo en formato minutos / segundos
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return new long[]{minutes, seconds};
    }

    // Devolver solo los minutos trabajados. Lo usan otros métodos internos
    public static long getMinutesWorkedToday(List<Fichaje> todaysFichajes) {
        long[] time = getTimeWorkedToday(todaysFichajes);
        return time[0];
    }

    // Adaptar a hh:mm:ss para visualizar por pantalla
    public static String formatTime(long minutes, long seconds) {
        long hours = minutes / 60;
        long mins = minutes % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, seconds);
    }

    // Lo mismo pero en minutos para métodos internos
    public static String formatMinutes(long minutes) {
        long hours = minutes / 60;
        long mins = minutes % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", hours, mins);
    }

    // Mismo para milisegundos
    public static String formatTimeMillis(long timeMillis) {
        long hours = TimeUnit.MILLISECONDS.toHours(timeMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis) - TimeUnit.HOURS.toSeconds(hours) - TimeUnit.MINUTES.toSeconds(minutes);

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    // Horas por semana / días que se trabajan
    public static float calculateDailyHours(float weeklyHours, int workingDays) {
        if (workingDays <= 0) return 0;
        return weeklyHours / workingDays;
    }

    // Cálculo de los segundos restantes
    public static long[] getRemainingTime(long[] timeWorked, float dailyHours) {
        long dailySeconds = (long)(dailyHours * 60 * 60);
        long workedSeconds = timeWorked[0] * 60 + timeWorked[1];
        long remainingSeconds = dailySeconds - workedSeconds;

        // Calculo de minutos y segundos
        long minutes = Math.abs(remainingSeconds) / 60;
        long seconds = Math.abs(remainingSeconds) % 60;

        // Devuelve minutos, segundos y si se está en horas extra
        return new long[]{minutes, seconds, remainingSeconds < 0 ? 1 : 0};
    }

    public static String getLastClockInTime(List<Fichaje> fichajes) {
        for (int i = fichajes.size() - 1; i >= 0; i--) {
            if (fichajes.get(i).horaSalida == null) {
                return fichajes.get(i).horaEntrada;
            }
        }
        return "N/A"; // Si no hay fichaje activo
    }

    // Auxiliar para fichaje activo
    public static Fichaje getActiveFichaje(List<Fichaje> todaysFichajes) {
        for (Fichaje fichaje : todaysFichajes) {
            if (fichaje.horaEntrada != null && fichaje.horaSalida == null) {
                return fichaje;
            }
        }
        return null;
    }

    // Auxiliar para saber si hay un fichaje activo
    public static boolean isCurrentlyClockedIn(List<Fichaje> todaysFichajes) {
        return getActiveFichaje(todaysFichajes) != null;
    }

    // Auxiliar para tiempos sin segundos
    public static String ensureTimeFormat(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return "00:00:00";
        }

        // Añade 00 a los segundos
        if (timeStr.matches("\\d{2}:\\d{2}")) {
            return timeStr + ":00";
        }
        return timeStr;
    }

    public static Fichaje getCurrentActiveFichaje(List<Fichaje> fichajes) {
        if (fichajes == null || fichajes.isEmpty()) {
            return null;
        }

        for (Fichaje fichaje : fichajes) {
            if (fichaje.horaEntrada != null && (fichaje.horaSalida == null || fichaje.horaSalida.isEmpty())) {
                return fichaje;
            }
        }

        return null;
    }

    public static long calculateTimeWorkedForActiveFichaje(Fichaje fichaje) {
        if (fichaje == null || fichaje.horaEntrada == null) {
            return 0;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Date startTime = sdf.parse(fichaje.horaEntrada);
            Date currentTime = new Date();

            // Calcular tiempo fichado
            return currentTime.getTime() - startTime.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }


}