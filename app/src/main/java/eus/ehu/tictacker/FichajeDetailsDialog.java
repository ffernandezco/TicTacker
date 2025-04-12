package eus.ehu.tictacker;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class FichajeDetailsDialog extends DialogFragment implements EditFichajeDialog.OnFichajeUpdatedListener {

    private Fichaje fichaje;
    private OnFichajeUpdatedListener listener;
    private DatabaseHelper databaseHelper;

    public interface OnFichajeUpdatedListener {
        void onFichajeUpdated();
    }

    public FichajeDetailsDialog(Fichaje fichaje, OnFichajeUpdatedListener listener) {
        this.fichaje = fichaje;
        this.listener = listener;
    }

    public FichajeDetailsDialog(Fichaje fichaje) {
        this.fichaje = fichaje;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fichaje_details, container, false);
        databaseHelper = new DatabaseHelper(requireContext());

        TextView tvFecha = view.findViewById(R.id.tvDetailFecha);
        TextView tvEntrada = view.findViewById(R.id.tvDetailEntrada);
        TextView tvSalida = view.findViewById(R.id.tvDetailSalida);
        TextView tvLocation = view.findViewById(R.id.tvDetailLocation);
        Button btnVerMapa = view.findViewById(R.id.btnVerMapa);
        Button btnEditar = view.findViewById(R.id.btnEditar);
        Button btnCerrar = view.findViewById(R.id.btnCerrar);

        Context context = view.getContext();

        updateUI(view);

        btnVerMapa.setOnClickListener(v -> {
            if (fichaje.latitud != 0.0 || fichaje.longitud != 0.0) {
                String uri = "geo:" + fichaje.latitud + "," + fichaje.longitud + "?q=" +
                        fichaje.latitud + "," + fichaje.longitud + "(" + context.getString(R.string.ubicacion_fichaje) + ")";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
            }
        });

        btnEditar.setOnClickListener(v -> {
            EditFichajeDialog editDialog = new EditFichajeDialog(fichaje, this);
            editDialog.show(getParentFragmentManager(), "EditFichajeDialog");
        });

        btnCerrar.setOnClickListener(v -> dismiss());

        return view;
    }

    private void updateUI(View view) {
        Context context = view.getContext();
        TextView tvFecha = view.findViewById(R.id.tvDetailFecha);
        TextView tvEntrada = view.findViewById(R.id.tvDetailEntrada);
        TextView tvSalida = view.findViewById(R.id.tvDetailSalida);
        TextView tvLocation = view.findViewById(R.id.tvDetailLocation);
        Button btnVerMapa = view.findViewById(R.id.btnVerMapa);

        tvFecha.setText(context.getString(R.string.fecha, fichaje.fecha));
        tvEntrada.setText(context.getString(R.string.entrada, fichaje.horaEntrada));

        String salida = fichaje.horaSalida != null ? fichaje.horaSalida : context.getString(R.string.pendiente);
        tvSalida.setText(context.getString(R.string.salida, salida));

        if (fichaje.latitud == 0.0 && fichaje.longitud == 0.0) {
            tvLocation.setText(context.getString(R.string.ubicacion_no_disponible));
            btnVerMapa.setEnabled(false);
        } else {
            tvLocation.setText(context.getString(R.string.ubicacion, fichaje.latitud, fichaje.longitud));
            btnVerMapa.setEnabled(true);
        }
    }

    @Override
    public void onFichajeUpdated() {
        // Refresh the data from database
        databaseHelper.obtenerUltimoFichajeDelDia(fichaje.fecha, fichaje.username, updatedFichaje -> {
            if (updatedFichaje != null && updatedFichaje.id == fichaje.id) {
                fichaje = updatedFichaje;
                if (getView() != null) {
                    updateUI(getView());
                }
            }

            FichajeEvents.notifyFichajeChanged();

            if (listener != null) {
                listener.onFichajeUpdated();
            }
        });
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        databaseHelper.close();
    }
}