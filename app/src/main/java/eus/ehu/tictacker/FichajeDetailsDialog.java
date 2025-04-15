package eus.ehu.tictacker;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class FichajeDetailsDialog extends DialogFragment implements EditFichajeDialog.OnFichajeUpdatedListener {

    private Fichaje fichaje;
    private OnFichajeUpdatedListener listener;
    private DatabaseHelper databaseHelper;
    private MapView mapView;

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
        // Configuración OSMdroid para mapa
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());

        View view = inflater.inflate(R.layout.dialog_fichaje_details, container, false);
        databaseHelper = new DatabaseHelper(requireContext());

        // Initializar componentes y configurar mapa
        mapView = view.findViewById(R.id.mapView);
        if (mapView != null) {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
        }

        TextView tvFecha = view.findViewById(R.id.tvDetailFecha);
        TextView tvEntrada = view.findViewById(R.id.tvDetailEntrada);
        TextView tvSalida = view.findViewById(R.id.tvDetailSalida);
        TextView tvLocation = view.findViewById(R.id.tvDetailLocation);
        Button btnVerMapa = view.findViewById(R.id.btnVerMapa);
        Button btnEditar = view.findViewById(R.id.btnEditar);
        Button btnCerrar = view.findViewById(R.id.btnCerrar);

        updateUI(view);

        Context context = view.getContext();
        setupMap();
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

    private void setupMap() {
        if (fichaje.latitud == 0.0 && fichaje.longitud == 0.0) { //Comprobar que la ubicación existe
            mapView.setVisibility(View.GONE);
            return;
        }

        if (!hasLocationPermissions()) {
            mapView.setVisibility(View.GONE);
            return;
        }

        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        // Configurar OSMdroid
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        mapView.setVisibility(View.VISIBLE);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        mapView.setMultiTouchControls(true);

        // Asegurar variables por defecto
        mapView.setBuiltInZoomControls(true);
        mapView.setMinZoomLevel(4.0);
        mapView.setMaxZoomLevel(19.0);

        // Mover mapa a ubicación del fichaje
        GeoPoint fichajePoint = new GeoPoint(fichaje.latitud, fichaje.longitud);
        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);
        mapController.setCenter(fichajePoint);

        // Poner chincheta en ubicación y eliminar anteriores
        mapView.getOverlays().clear();
        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(fichajePoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle(requireContext().getString(R.string.ubicacion_fichaje));
        mapView.getOverlays().add(startMarker);

        mapView.invalidate();
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
            mapView.setVisibility(View.GONE);
        } else {
            tvLocation.setText(context.getString(R.string.ubicacion, fichaje.latitud, fichaje.longitud));
            btnVerMapa.setEnabled(true);
            mapView.setVisibility(View.VISIBLE);
            setupMap(); // Si previamente no se ha configurado el mapa
        }
    }

    @Override
    public void onFichajeUpdated() {
        // Actualizar la vista con los nuevos datos
        if (getView() != null) {
            updateUI(getView());
        }

        // Notificar al listener (HistoryFragment) para que actualice su lista
        if (listener != null) {
            listener.onFichajeUpdated();
        }

        // Notificar a cualquier otro observador del cambio
        FichajeEvents.notifyFichajeChanged();
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
            mapView = null;  // Evitar exceso memoria
        }
        //if (databaseHelper != null) {
            //databaseHelper.close();
            //databaseHelper = null;  // Evitar exceso memoria
        //}
    }

    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}