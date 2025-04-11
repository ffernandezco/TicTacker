package eus.ehu.tictacker;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.icu.text.SimpleDateFormat;

import java.util.*;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "fichaje_db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_FICHAJES = "fichajes";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_FECHA = "fecha";
    private static final String COLUMN_HORA_ENTRADA = "hora_entrada";
    private static final String COLUMN_HORA_SALIDA = "hora_salida";
    private static final String COLUMN_LATITUD = "latitud";
    private static final String COLUMN_LONGITUD = "longitud";
    private static final String TABLE_SETTINGS = "settings";
    private static final String COLUMN_WEEKLY_HOURS = "weekly_hours";
    private static final String COLUMN_WORKING_DAYS = "working_days";
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabla de fichajes
        String createFichajesTable = "CREATE TABLE " + TABLE_FICHAJES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_FECHA + " TEXT, " +
                COLUMN_HORA_ENTRADA + " TEXT, " +
                COLUMN_HORA_SALIDA + " TEXT, " +
                COLUMN_LATITUD + " REAL, " +
                COLUMN_LONGITUD + " REAL, " +
                COLUMN_USERNAME + " TEXT)";
        db.execSQL(createFichajesTable);

        // Tabla de configuraciones
        String createSettingsTable = "CREATE TABLE " + TABLE_SETTINGS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_WEEKLY_HOURS + " REAL, " +
                COLUMN_WORKING_DAYS + " INTEGER)";
        db.execSQL(createSettingsTable);

        // Tabla de usuarios
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT)");

        // Usuarios por defecto
        db.execSQL("INSERT INTO " + TABLE_USERS + " (username, password) VALUES ('demo', 'demo')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FICHAJES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    public void insertarFichaje(Fichaje fichaje) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FECHA, fichaje.fecha);
        values.put(COLUMN_HORA_ENTRADA, fichaje.horaEntrada);
        values.put(COLUMN_HORA_SALIDA, fichaje.horaSalida);
        values.put(COLUMN_LATITUD, fichaje.latitud);
        values.put(COLUMN_LONGITUD, fichaje.longitud);
        values.put(COLUMN_USERNAME, fichaje.username);

        db.insert(TABLE_FICHAJES, null, values);
        db.close();
    }

    // Devuelve el listado completo, e.g. RecyclerView
    public List<Fichaje> obtenerTodosLosFichajes(String username) {
        List<Fichaje> listaFichajes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_FICHAJES +
                        " WHERE " + COLUMN_USERNAME + " = ? ORDER BY fecha DESC, hora_entrada DESC",
                new String[]{username});

        if (cursor.moveToFirst()) {
            do {
                Fichaje fichaje = new Fichaje(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getDouble(4),
                        cursor.getDouble(5),
                        cursor.getString(6)
                );
                listaFichajes.add(fichaje);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return listaFichajes;
    }

    // Devuelve el último fichaje para poderlo actualizar (trampa con limit)
    public Fichaje obtenerUltimoFichajeDelDia(String fecha, String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_FICHAJES +
                        " WHERE fecha = ? AND " + COLUMN_USERNAME + " = ? ORDER BY hora_entrada DESC LIMIT 1",
                new String[]{fecha, username});

        if (cursor.moveToFirst()) {
            Fichaje fichaje = new Fichaje(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getDouble(4),
                    cursor.getDouble(5),
                    cursor.getString(6)
            );
            cursor.close();
            db.close();
            return fichaje;
        }
        cursor.close();
        db.close();
        return null;
    }

    //Añade la hora de salida y la ubicación
    public void actualizarFichaje(Fichaje fichaje) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HORA_SALIDA, fichaje.horaSalida);
        values.put(COLUMN_LATITUD, fichaje.latitud);
        values.put(COLUMN_LONGITUD, fichaje.longitud);

        db.update(TABLE_FICHAJES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(fichaje.id)});
        db.close();
    }

    public List<Fichaje> obtenerFichajesDeHoy(String username) {
        List<Fichaje> listaFichajes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        SimpleDateFormat sdfFecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String fechaActual = sdfFecha.format(new Date());

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_FICHAJES +
                        " WHERE fecha = ? AND " + COLUMN_USERNAME + " = ? ORDER BY hora_entrada ASC",
                new String[]{fechaActual, username});

        if (cursor.moveToFirst()) {
            do {
                Fichaje fichaje = new Fichaje(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getDouble(4),
                        cursor.getDouble(5),
                        cursor.getString(6)
                );
                listaFichajes.add(fichaje);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return listaFichajes;
    }

    public void saveSettings(float weeklyHours, int workingDays) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, 1); // 1 explícitamente
        values.put(COLUMN_WEEKLY_HOURS, weeklyHours);
        values.put(COLUMN_WORKING_DAYS, workingDays);

        // Comprobar si se han guardado ajustes de forma previa
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SETTINGS + " LIMIT 1", null);
        boolean hasSettings = cursor.moveToFirst();
        cursor.close();

        if (hasSettings) {
            // Actualizar valores
            db.update(TABLE_SETTINGS, values, COLUMN_ID + " = ?", new String[]{"1"});
        } else {
            // Guardar tablas si no hay valores
            db.insert(TABLE_SETTINGS, null, values);
        }

        db.close();
    }

    public float[] getSettings() {
        SQLiteDatabase db = this.getReadableDatabase();
        float[] settings = new float[2];

        // Valores por defecto (horas semanales 0 y días a la semana 1)
        settings[0] = 40.0f;
        settings[1] = 5.0f;

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SETTINGS + " LIMIT 1", null);

        if (cursor.moveToFirst()) {
            int weeklyHoursIndex = cursor.getColumnIndex(COLUMN_WEEKLY_HOURS);
            int workingDaysIndex = cursor.getColumnIndex(COLUMN_WORKING_DAYS);

            if (weeklyHoursIndex != -1 && workingDaysIndex != -1) {
                settings[0] = cursor.getFloat(weeklyHoursIndex);
                settings[1] = cursor.getFloat(workingDaysIndex);
            }
        }

        cursor.close();
        db.close();
        return settings;
    }

    public void deleteAllFichajes(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FICHAJES, COLUMN_USERNAME + " = ?", new String[]{username}); // Solo elimina fichajes del usuario
        db.close();
    }

    public void actualizarHoraEntrada(Fichaje fichaje) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HORA_ENTRADA, fichaje.horaEntrada);

        db.update(TABLE_FICHAJES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(fichaje.id)});
        db.close();
    }

    public void actualizarFichajeCompleto(Fichaje fichaje) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HORA_ENTRADA, fichaje.horaEntrada);
        values.put(COLUMN_HORA_SALIDA, fichaje.horaSalida);

        db.update(TABLE_FICHAJES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(fichaje.id)});
        db.close();
    }

    public boolean validarUsuario(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE username=? AND password=?", new String[]{username, password});
        boolean valido = cursor.getCount() > 0;
        cursor.close();
        return valido;
    }

    public String getCurrentUsername(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("usuario_actual", "unknown_user");
    }

    public boolean userExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE username=?",
                new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean addUser(String username, String password) {
        if (userExists(username)) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result != -1;
    }
}