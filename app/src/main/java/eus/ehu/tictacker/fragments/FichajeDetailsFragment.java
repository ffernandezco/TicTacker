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

    private Fichaje defaultFichaje = new Fichaje(-1, "00/00/0000", "00:00:00", "00:00:00", 0.0, 0.0, "demo");
    private static Fichaje cachedFichaje = null;
    private Fichaje fichaje;
    private DatabaseHelper databaseHelper;
    private TextView tvFecha, tvEntrada, tvSalida, tvLocation;
    private Button btnVerMapa, btnEditar;
    private MapView mapView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(requireContext());

        if (getArguments() != null) {
            int fichajeId = getArguments().getInt("fichaje_id", -1);
            String username = databaseHelper.getCurrentUsername(requireContext());

            // Comprobar si se tienen elementos cacheados y sino cachear
            if (cachedFichaje != null && cachedFichaje.id == fichajeId) {
                fichaje = cachedFichaje;
                updateUI();
            } else if (fichajeId != -1) {
                databaseHelper.obtenerFichajePorId(fichajeId, username, obtainedFichaje -> {
                    if (obtainedFichaje != null) {
                        fichaje = obtainedFichaje;
                        cachedFichaje = fichaje; // Almacenar en caché
                    } else {
                        fichaje = defaultFichaje;
                    }
                    updateUI();
                });
            } else {
                fichaje = defaultFichaje;
                updateUI();
            }
        } else {
            fichaje = defaultFichaje;
            updateUI();
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

        // Mostrar valores por defecto mientras se carga
        Context context = requireContext();
        tvFecha.setText(context.getString(R.string.fecha, defaultFichaje.fecha));
        tvEntrada.setText(context.getString(R.string.entrada, defaultFichaje.horaEntrada));
        tvSalida.setText(context.getString(R.string.salida, defaultFichaje.horaSalida));
        tvLocation.setText(context.getString(R.string.ubicacion_no_disponible));

        // Inicializar el mapView
        configureMapView();

        // Configurar listeners para botones
        btnVerMapa.setOnClickListener(v -> openMap());
        btnEditar.setOnClickListener(v -> showEditDialog());

        if (fichaje != null) {
            updateUI();
        }
    }

    // Configurar vista del mapa con OSMdroid
    private void configureMapView() {
        if (mapView != null) {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            IMapController mapController = mapView.getController();
            mapController.setZoom(16.0);
        }
    }

    // Actualizar la vista en caso de que haya algún cambio
    private void updateUI() {
        if (getView() == null) return;

        Context context = requireContext();

        // Utilizar los datos por defecto en caso de que no haya información disponible
        Fichaje displayFichaje = fichaje != null ? fichaje : defaultFichaje;

        tvFecha.setText(context.getString(R.string.fecha, displayFichaje.fecha));
        tvEntrada.setText(context.getString(R.string.entrada, displayFichaje.horaEntrada));

        String salida = displayFichaje.horaSalida != null ? displayFichaje.horaSalida : context.getString(R.string.pendiente);
        tvSalida.setText(context.getString(R.string.salida, salida));

        if (displayFichaje.latitud == 0.0 && displayFichaje.longitud == 0.0) {
            tvLocation.setText(context.getString(R.string.ubicacion_no_disponible));
            btnVerMapa.setEnabled(false);
            mapView.setVisibility(View.GONE);
        } else {
            tvLocation.setText(context.getString(R.string.ubicacion, displayFichaje.latitud, displayFichaje.longitud));
            btnVerMapa.setEnabled(true);
            mapView.setVisibility(View.VISIBLE);
            updateMapWithLocation(displayFichaje);
        }
    }

    // Muestra el mapa con la ubicación obtenida del fichaje si está disponible
    private void updateMapWithLocation(Fichaje displayFichaje) {
        if (mapView != null && displayFichaje != null && (displayFichaje.latitud != 0.0 || displayFichaje.longitud != 0.0)) {
            // Poner el mapa en la ubicación del fichaje
            GeoPoint point = new GeoPoint(displayFichaje.latitud, displayFichaje.longitud);
            IMapController mapController = mapView.getController();
            mapController.setCenter(point);
            mapController.setZoom(16.0);

            // Añadir chincheta de localización para el fichaje
            mapView.getOverlays().clear();
            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(getString(R.string.ubicacion_fichaje));

            mapView.getOverlays().add(marker);
            mapView.invalidate();
        }
    }

    // Intent para abrir en app de mapas propia del dispositivo
    // e.g. Google Maps
    private void openMap() {
        Fichaje displayFichaje = fichaje != null ? fichaje : defaultFichaje;

        if (displayFichaje != null && (displayFichaje.latitud != 0.0 || displayFichaje.longitud != 0.0)) {
            String uri = "geo:" + displayFichaje.latitud + "," + displayFichaje.longitud + "?q=" +
                    displayFichaje.latitud + "," + displayFichaje.longitud + "(" + requireContext().getString(R.string.ubicacion_fichaje) + ")";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(intent);
        }
    }

    // Botón para editar el fichaje guardado
    private void showEditDialog() {
        if (fichaje != null) {
            EditFichajeDialog editDialog = new EditFichajeDialog(fichaje, this);
            editDialog.show(getParentFragmentManager(), "EditFichajeDialog");
        }
    }

    // Actualizar caché y vista en caso de que haya algún cambio
    @Override
    public void onFichajeUpdated() {
        if (fichaje != null) {
            databaseHelper.obtenerFichajePorId(fichaje.id, fichaje.username, updatedFichaje -> {
                if (updatedFichaje != null) {
                    fichaje = updatedFichaje;
                    cachedFichaje = fichaje; // Actualizar caché
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