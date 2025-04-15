package eus.ehu.tictacker.fragments;

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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import eus.ehu.tictacker.DatabaseHelper;
import eus.ehu.tictacker.EditFichajeDialog;
import eus.ehu.tictacker.Fichaje;
import eus.ehu.tictacker.FichajeEvents;
import eus.ehu.tictacker.R;

public class FichajeDetailsFragment extends Fragment implements EditFichajeDialog.OnFichajeUpdatedListener {

    private Fichaje fichaje;
    private DatabaseHelper databaseHelper;
    private TextView tvFecha, tvEntrada, tvSalida, tvLocation;
    private Button btnVerMapa, btnEditar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(requireContext());

        // Obtener detalles del fichaje
        if (getArguments() != null) {
            int fichajeId = getArguments().getInt("fichaje_id", -1);
            String username = databaseHelper.getCurrentUsername(requireContext());

            // Obtener de la base de datos el fichaje
            if (fichajeId != -1) {
                databaseHelper.obtenerFichajePorId(fichajeId, username, obtainedFichaje -> {
                    if (obtainedFichaje != null) {
                        fichaje = obtainedFichaje;
                        updateUI();
                    }
                });
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fichaje_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar vistas
        tvFecha = view.findViewById(R.id.tvDetailFecha);
        tvEntrada = view.findViewById(R.id.tvDetailEntrada);
        tvSalida = view.findViewById(R.id.tvDetailSalida);
        tvLocation = view.findViewById(R.id.tvDetailLocation);
        btnVerMapa = view.findViewById(R.id.btnVerMapa);
        btnEditar = view.findViewById(R.id.btnEditar);

        // Configurar listeners para botones
        btnVerMapa.setOnClickListener(v -> openMap());
        btnEditar.setOnClickListener(v -> showEditDialog());

        if (fichaje != null) {
            updateUI();
        }
    }

    private void updateUI() {
        if (fichaje == null || getView() == null) return;

        Context context = requireContext();

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

    private void openMap() {
        if (fichaje != null && (fichaje.latitud != 0.0 || fichaje.longitud != 0.0)) {
            String uri = "geo:" + fichaje.latitud + "," + fichaje.longitud + "?q=" +
                    fichaje.latitud + "," + fichaje.longitud + "(" + requireContext().getString(R.string.ubicacion_fichaje) + ")";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(intent);
        }
    }

    private void showEditDialog() {
        if (fichaje != null) {
            EditFichajeDialog editDialog = new EditFichajeDialog(fichaje, this);
            editDialog.show(getParentFragmentManager(), "EditFichajeDialog");
        }
    }

    @Override
    public void onFichajeUpdated() {
        // Recargar datos del fichaje si se actualiza
        if (fichaje != null) {
            databaseHelper.obtenerFichajePorId(fichaje.id, fichaje.username, updatedFichaje -> {
                if (updatedFichaje != null) {
                    fichaje = updatedFichaje;
                    updateUI();
                }
            });
        }
        FichajeEvents.notifyFichajeChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //if (databaseHelper != null) {
            //databaseHelper.close();
        //}
    }
}