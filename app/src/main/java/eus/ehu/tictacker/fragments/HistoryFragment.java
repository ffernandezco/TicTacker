package eus.ehu.tictacker.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eus.ehu.tictacker.DatabaseHelper;
import eus.ehu.tictacker.Fichaje;
import eus.ehu.tictacker.FichajeAdapter;
import eus.ehu.tictacker.FichajeDetailsDialog;
import eus.ehu.tictacker.R;

public class HistoryFragment extends Fragment implements FichajeDetailsDialog.OnFichajeUpdatedListener {
    private DatabaseHelper dbHelper;
    private FichajeAdapter adapter;
    private TextView tvEmptyHistory;
    private RecyclerView recyclerView;
    private Button btnExport, btnImport;

    private ActivityResultLauncher<Intent> importFileLauncher;
    private ActivityResultLauncher<Intent> exportFileLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmptyHistory = view.findViewById(R.id.tvEmptyHistory);
        btnExport = view.findViewById(R.id.btnExport);
        btnImport = view.findViewById(R.id.btnImport);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FichajeAdapter(this::showFichajeDetails);
        recyclerView.setAdapter(adapter);

        setupActivityResultLaunchers();

        btnExport.setOnClickListener(v -> exportHistory());
        btnImport.setOnClickListener(v -> importHistory());

        actualizarLista();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dbHelper.close();
    }

    private void setupActivityResultLaunchers() {
        importFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            processImportFile(uri);
                        }
                    }
                }
        );

        exportFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            processExportFile(uri);
                        }
                    }
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizarLista();
    }

    private void actualizarLista() {
        String username = dbHelper.getCurrentUsername(requireContext());
        dbHelper.obtenerTodosLosFichajes(username, fichajes -> {
            adapter.setFichajes(fichajes);

            if (fichajes.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                tvEmptyHistory.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                tvEmptyHistory.setVisibility(View.GONE);
            }
        });
    }

    private void showFichajeDetails(Fichaje fichaje) {
        new FichajeDetailsDialog(fichaje, this)
                .show(getParentFragmentManager(), "FichajeDetailsDialog");
    }

    @Override
    public void onFichajeUpdated() {
        // Actualizar lista cuando se modifica un fichaje
        actualizarLista();
    }

    private void exportHistory() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = "fichajes_" + sdf.format(new Date()) + ".csv";
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        exportFileLauncher.launch(intent);
    }

    private void importHistory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");

        importFileLauncher.launch(intent);
    }

    private void processExportFile(Uri uri) {
        String username = dbHelper.getCurrentUsername(requireContext());
        dbHelper.obtenerTodosLosFichajes(username, fichajes -> {
            try {
                FileOutputStream fos = (FileOutputStream) requireContext().getContentResolver().openOutputStream(uri);
                OutputStreamWriter osw = new OutputStreamWriter(fos);

                osw.write("ID,Fecha,Hora Entrada,Hora Salida,Latitud,Longitud,Username\n");

                for (Fichaje fichaje : fichajes) {
                    osw.write(String.format(Locale.US,  // Usar Locale.US para puntos decimales
                            "%d,%s,%s,%s,%f,%f,%s\n",
                            fichaje.id,
                            fichaje.fecha,
                            fichaje.horaEntrada != null ? fichaje.horaEntrada : "",
                            fichaje.horaSalida != null ? fichaje.horaSalida : "null",  // Usar null para valores nulos
                            fichaje.latitud,
                            fichaje.longitud,
                            fichaje.username));
                }

                osw.close();
                fos.close();

                Toast.makeText(requireContext(), R.string.export_success, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), R.string.export_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processImportFile(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(requireContext(), R.string.file_not_found, Toast.LENGTH_SHORT).show();
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            reader.readLine(); // Saltar cabecera

            List<Fichaje> fichajesImportados = new ArrayList<>();
            String username = dbHelper.getCurrentUsername(requireContext());

            while ((line = reader.readLine()) != null) {
                try {
                    // Usar expresión regular para dividir correctamente los campos
                    String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                    if (values.length >= 7) {
                        // Corregir formato de números decimales (reemplazar comas por puntos)
                        String latStr = values[4].replace(",", ".");
                        String lonStr = values[5].replace(",", ".");

                        String horaSalida = values[3].trim().equalsIgnoreCase("null") ? null : values[3];

                        Fichaje fichaje = new Fichaje(
                                Integer.parseInt(values[0]),
                                values[1],
                                values[2],
                                horaSalida,
                                Double.parseDouble(latStr),
                                Double.parseDouble(lonStr),
                                values[6]
                        );
                        fichajesImportados.add(fichaje);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ImportError", "Error procesando línea: " + line, e);
                }
            }

            reader.close();
            inputStream.close();

            // Insertar los fichajes uno por uno para manejar mejor los errores
            int[] contador = {0};
            for (Fichaje fichaje : fichajesImportados) {
                dbHelper.insertarFichaje(fichaje, success -> {
                    contador[0]++;
                    if (contador[0] == fichajesImportados.size()) {
                        requireActivity().runOnUiThread(() -> {
                            actualizarLista();
                            Toast.makeText(requireContext(),
                                    getString(R.string.import_success) + " (" + fichajesImportados.size() + ")",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), R.string.import_error, Toast.LENGTH_SHORT).show();
        }
    }
}