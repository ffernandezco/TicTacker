package eus.ehu.tictacker;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditFichajeDialog extends DialogFragment {

    private Fichaje fichaje;
    private DatabaseHelper databaseHelper;
    private TextView tvEntrada, tvSalida;
    private OnFichajeUpdatedListener listener;

    public interface OnFichajeUpdatedListener {
        void onFichajeUpdated();
    }

    public EditFichajeDialog(Fichaje fichaje, OnFichajeUpdatedListener listener) {
        this.fichaje = fichaje;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_fichaje, container, false);

        databaseHelper = new DatabaseHelper(requireContext());

        TextView tvFecha = view.findViewById(R.id.tvEditFecha);
        tvEntrada = view.findViewById(R.id.tvEditEntrada);
        tvSalida = view.findViewById(R.id.tvEditSalida);
        Button btnChangeEntrada = view.findViewById(R.id.btnChangeEntrada);
        Button btnChangeSalida = view.findViewById(R.id.btnChangeSalida);
        Button btnGuardar = view.findViewById(R.id.btnGuardar);
        Button btnCancelar = view.findViewById(R.id.btnCancelar);

        Context context = view.getContext();

        tvFecha.setText(context.getString(R.string.fecha, fichaje.fecha));
        tvEntrada.setText(fichaje.horaEntrada);

        String salida = fichaje.horaSalida != null ? fichaje.horaSalida : context.getString(R.string.pendiente);
        tvSalida.setText(salida);

        btnChangeEntrada.setOnClickListener(v -> showTimePickerDialog(true));
        btnChangeSalida.setOnClickListener(v -> showTimePickerDialog(false));

        btnGuardar.setOnClickListener(v -> {
            // Actualizar el fichaje
            fichaje.horaEntrada = tvEntrada.getText().toString();
            String salidaText = tvSalida.getText().toString();
            if (!salidaText.equals(context.getString(R.string.pendiente))) {
                fichaje.horaSalida = salidaText;
            }

            // Guardar datos en la DB (local)
            updateFichajeInDb();

            // Evento
            FichajeEvents.notifyFichajeChanged();

            // Si el listener está activo, actualizar
            if (listener != null) {
                listener.onFichajeUpdated();
            }
            dismiss();
            Toast.makeText(context, context.getString(R.string.fichaje_updated), Toast.LENGTH_SHORT).show();
        });

        btnCancelar.setOnClickListener(v -> dismiss());

        return view;
    }

    private void showTimePickerDialog(boolean isEntrada) {
        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String timeStr = isEntrada ? fichaje.horaEntrada :
                    (fichaje.horaSalida != null ? fichaje.horaSalida : "12:00:00");

            // Comprobar que el tiempo está bien
            Date time;
            try {
                time = sdf.parse(timeStr);
            } catch (ParseException e) {
                // Si no tiene segundos asignados
                SimpleDateFormat sdfNoSeconds = new SimpleDateFormat("HH:mm", Locale.getDefault());
                time = sdfNoSeconds.parse(timeStr);
            }

            if (time != null) {
                calendar.setTime(time);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minuteOfDay) -> {
                    // Añadir segundos para que sea compatible con el resto de la vista
                    String timeStr = String.format(Locale.getDefault(), "%02d:%02d:00", hourOfDay, minuteOfDay);
                    if (isEntrada) {
                        tvEntrada.setText(timeStr);
                    } else {
                        tvSalida.setText(timeStr);
                    }
                },
                hour,
                minute,
                true
        );

        timePickerDialog.show();
    }

    private void updateFichajeInDb() {
        if (fichaje.horaSalida != null) {
            // Se ha entrado y salido
            databaseHelper.actualizarFichajeCompleto(fichaje);
        } else {
            // Solo se ha fichado
            databaseHelper.actualizarHoraEntrada(fichaje);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
    }
}