package eus.ehu.tictacker.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

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
    private MapView mapView;

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
        mapView = view.findViewById(R.id.mapView);

        // Inicializar el mapView
        configureMapView();

        // Configurar listeners para botones
        btnVerMapa.setOnClickListener(v -> openMap());
        btnEditar.setOnClickListener(v -> showEditDialog());

        if (fichaje != null) {
            updateUI();
        }
    }

    private void configureMapView() {
        if (mapView != null) {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            IMapController mapController = mapView.getController();
            mapController.setZoom(16.0);
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
            mapView.setVisibility(View.GONE); // Oculta el mapa en caso de que no haya ubicación para el fichaje
        } else {
            tvLocation.setText(context.getString(R.string.ubicacion, fichaje.latitud, fichaje.longitud));
            btnVerMapa.setEnabled(true);
            mapView.setVisibility(View.VISIBLE);
            updateMapWithLocation();
        }
    }

    private void updateMapWithLocation() {
        if (mapView != null && fichaje != null && (fichaje.latitud != 0.0 || fichaje.longitud != 0.0)) {
            // Poner el mapa en la posición del fichaje
            GeoPoint point = new GeoPoint(fichaje.latitud, fichaje.longitud);
            IMapController mapController = mapView.getController();
            mapController.setCenter(point);
            mapController.setZoom(16.0);

            // Añadir chincheta de posición en la ubicación del fichaje
            mapView.getOverlays().clear();
            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(getString(R.string.ubicacion_fichaje));
            Drawable icon = ContextCompat.getDrawable(requireContext(), org.osmdroid.library.R.drawable.osm_ic_follow_me_on);
            if (icon != null) {
                marker.setIcon(icon);
            }
            mapView.getOverlays().add(marker);
            mapView.invalidate();
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
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}