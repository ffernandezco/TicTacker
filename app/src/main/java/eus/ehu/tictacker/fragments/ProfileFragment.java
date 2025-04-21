package eus.ehu.tictacker.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import eus.ehu.tictacker.DatabaseHelper;
import eus.ehu.tictacker.LoginActivity;
import eus.ehu.tictacker.MainActivity;
import eus.ehu.tictacker.R;
import eus.ehu.tictacker.UserProfile;

public class ProfileFragment extends Fragment {

    private TextInputEditText editTextName, editTextSurname, editTextEmail, editTextBirthdate;
    private MaterialButton buttonSaveProfile, buttonLogout;
    private DatabaseHelper dbHelper;
    private SimpleDateFormat dateFormat;
    private String username;
    private TextView textViewProfileTitle;
    private ShapeableImageView imageViewProfile;
    private FloatingActionButton fabChangePhoto;
    private Uri currentPhotoUri;
    private String base64Image;
    private TextInputEditText editTextCurrentPassword, editTextNewPassword, editTextConfirmNewPassword;
    private MaterialButton buttonChangePassword;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;

    // ActivityResultLaunchers para poder usar cámara
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        username = dbHelper.getCurrentUsername(requireContext());

        editTextName = view.findViewById(R.id.editTextName);
        editTextSurname = view.findViewById(R.id.editTextSurname);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextBirthdate = view.findViewById(R.id.editTextBirthdate);
        buttonSaveProfile = view.findViewById(R.id.buttonSaveProfile);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        textViewProfileTitle = view.findViewById(R.id.textViewProfileTitle);

        textViewProfileTitle.setText(getString(R.string.profile_of, username));

        editTextBirthdate.setOnClickListener(v -> showDatePickerDialog());

        imageViewProfile = view.findViewById(R.id.imageViewProfile);
        fabChangePhoto = view.findViewById(R.id.fabChangePhoto);

        // Selector de imágenes
        registerImagePickerLaunchers();

        fabChangePhoto.setOnClickListener(v -> showImagePickerOptions());

        editTextCurrentPassword = view.findViewById(R.id.editTextCurrentPassword);
        editTextNewPassword = view.findViewById(R.id.editTextNewPassword);
        editTextConfirmNewPassword = view.findViewById(R.id.editTextConfirmNewPassword);
        buttonChangePassword = view.findViewById(R.id.buttonChangePassword);

        buttonChangePassword.setOnClickListener(v -> changePassword());

        loadProfileData();

        buttonSaveProfile.setOnClickListener(v -> saveProfileData());
        buttonLogout.setOnClickListener(v -> logoutUser());
    }

    // Completar campos a partir de la información guardada en la base de datos
    private void loadProfileData() {
        dbHelper.getProfile(username, profile -> {
            if (profile != null) {
                editTextName.setText(profile.name);
                editTextSurname.setText(profile.surname);
                editTextEmail.setText(profile.email);
                editTextBirthdate.setText(profile.birthdate);

                // Cargar imagen de perfil si está disponible
                if (profile.profilePhoto != null && !profile.profilePhoto.isEmpty()) {
                    // Cargar imagen desde el Base64
                    try {
                        byte[] decodedString = Base64.decode(profile.profilePhoto, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        imageViewProfile.setImageBitmap(bitmap);
                        base64Image = profile.profilePhoto;
                    } catch (Exception e) {
                        Log.e("ProfileFragment", "Error al extraer imagen base64", e);
                    }
                }
            }
        });
    }

    // Mostrar selector de fechas para la fecha de nacimiento
    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();

        String currentDate = editTextBirthdate.getText().toString();
        if (!currentDate.isEmpty()) {
            try {
                Date date = dateFormat.parse(currentDate);
                calendar.setTime(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(selectedYear, selectedMonth, selectedDay);
                    editTextBirthdate.setText(dateFormat.format(calendar.getTime()));
                },
                year, month, day);

        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, -18);
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        datePickerDialog.show();
    }

    // Guardar información del perfil comprobando campos y subir a la base de datos
    private void saveProfileData() {
        String name = editTextName.getText().toString().trim();
        String surname = editTextSurname.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String birthdate = editTextBirthdate.getText().toString().trim();

        // Comprobar que se hayan completado todos los campos
        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || birthdate.isEmpty()) {
            Toast.makeText(requireContext(), R.string.all_fields_required, Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar que el email sea correcto
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError(getString(R.string.invalid_email));
            return;
        }

        try {
            Date birthdateDate = dateFormat.parse(birthdate);
            Calendar dob = Calendar.getInstance();
            dob.setTime(birthdateDate);

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

            if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH) ||
                    (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) &&
                            today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
                age--;
            }

            // Comprobar que el usuario tenga al menos 18 años

            if (age < 18) {
                editTextBirthdate.setError(getString(R.string.must_be_adult));
                return;
            }
        } catch (ParseException e) {
            editTextBirthdate.setError(getString(R.string.invalid_date));
            return;
        }

        UserProfile profile = new UserProfile();
        profile.username = username;
        profile.name = name;
        profile.surname = surname;
        profile.email = email;
        profile.birthdate = birthdate;
        profile.profilePhoto = base64Image;

        // Subir cambios a la base de datos
        dbHelper.updateProfile(profile, success -> {
            if (success) {
                ((MainActivity) requireActivity()).refreshProfileImage();
                Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).popBackStack();
            } else {
                Toast.makeText(requireContext(), R.string.error_saving_profile, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Foto de perfil
    private void registerImagePickerLaunchers() {
        // Lanzar cámara
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (currentPhotoUri != null) {
                            try {
                                // Cargar la imagen con opciones de muestreo para evitar OutOfMemoryError
                                Bitmap bitmap = loadSampledBitmapFromUri(currentPhotoUri);
                                if (bitmap != null) {
                                    imageViewProfile.setImageBitmap(bitmap);
                                    base64Image = bitmapToBase64(bitmap);
                                } else {
                                    Toast.makeText(requireContext(),
                                            R.string.error_loading_image, Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Log.e("ProfileFragment", "Error processing camera image", e);
                                Toast.makeText(requireContext(),
                                        R.string.error_loading_image, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        // Lanzar selector de imagen de la galería
        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        try {
                            // Cargar la imagen con opciones de muestreo para evitar OutOfMemoryError
                            Bitmap bitmap = loadSampledBitmapFromUri(uri);
                            if (bitmap != null) {
                                imageViewProfile.setImageBitmap(bitmap);
                                base64Image = bitmapToBase64(bitmap);
                            } else {
                                Toast.makeText(requireContext(),
                                        R.string.error_loading_image, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("ProfileFragment", "Error processing gallery image", e);
                            Toast.makeText(requireContext(),
                                    R.string.error_loading_image, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    // Dar a elegir entre hacer foto o elegir de la galería
    private void showImagePickerOptions() {
        String[] options = {getString(R.string.take_photo), getString(R.string.choose_from_gallery)};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.change_profile_photo)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Tomar fotografía con cámara
                            checkCameraPermissionAndLaunch();
                            break;
                        case 1: // Elegir fotografía de la galería
                            pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                    .build());
                            break;
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            takePicture();
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    takePicture();
                } else {
                    Toast.makeText(requireContext(),
                            R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
                }
            });

    // Hacer foto
    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Crear fichero para la imagen
        File photoFile = null;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            Toast.makeText(requireContext(),
                    R.string.error_creating_image_file, Toast.LENGTH_SHORT).show();
            return;
        }

        // Si ha ido bien
        if (photoFile != null) {
            currentPhotoUri = FileProvider.getUriForFile(requireContext(),
                    "eus.ehu.tictacker.fileprovider", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
            takePictureLauncher.launch(takePictureIntent);
        }
    }

    // Guardar imagen en base64 y comprimir para que entre en la base de datos
    private String bitmapToBase64(Bitmap bitmap) {
        // Reducir tamaño a 160px para evitar restricciones de Base64
        Bitmap resizedBitmap = getResizedBitmap(bitmap, 160);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Si es demasiado, se empieza hasta llegar a 9000 bytes
        // Tener en cuenta que por restricción tiene que ser inferior a 10240 bytes en BD
        int quality = 50; // Reducir calidad (50 %)
        int maxSize = 9000;

        while (true) {
            byteArrayOutputStream.reset(); // Eliminar intento anterior
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            if (byteArray.length <= maxSize) {
                // Encontrar tamaño adecuado
                return Base64.encodeToString(byteArray, Base64.DEFAULT);
            }

            // Casos extremos en los que debe reducirse drásticamente calidad
            if (quality > 10) {
                // Probar con menor calidad de JPEG
                quality -= 10;
            } else {
                // Si aún no ocupa menos, reducir dimensiones (aún más)
                int newSize = (int)(resizedBitmap.getWidth() * 0.8f);
                if (newSize < 50) {
                    // Crear imagen en Base64 con reducción de dimensiones y calidad mínima
                    byteArrayOutputStream.reset();
                    getResizedBitmap(bitmap, 50).compress(Bitmap.CompressFormat.JPEG, 10, byteArrayOutputStream);
                    byte[] finalByteArray = byteArrayOutputStream.toByteArray();
                    return Base64.encodeToString(finalByteArray, Base64.DEFAULT);
                }
                resizedBitmap = getResizedBitmap(bitmap, newSize);
            }
        }
    }

    // Redimensionar imagen para cumplir con requisitos de tamaño máximo
    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    // Cambiar la contraseña de un usuario
    private void changePassword() {
        String currentPassword = editTextCurrentPassword.getText().toString().trim();
        String newPassword = editTextNewPassword.getText().toString().trim();
        String confirmNewPassword = editTextConfirmNewPassword.getText().toString().trim();

        // Validar la nueva contraseña
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            Toast.makeText(requireContext(), R.string.all_fields_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 4) {
            editTextNewPassword.setError(getString(R.string.password_too_short));
            return;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            editTextConfirmNewPassword.setError(getString(R.string.passwords_dont_match));
            return;
        }

        // Comprobar contraseña original
        dbHelper.validateCurrentPassword(username, currentPassword, new DatabaseHelper.BooleanCallback() {
            @Override
            public void onResult(boolean passwordCorrect) {
                if (!passwordCorrect) {
                    requireActivity().runOnUiThread(() -> {
                        editTextCurrentPassword.setError(getString(R.string.current_password_incorrect));
                    });
                    return;
                }

                // Actualizar la contraseña
                dbHelper.updateUserPassword(username, newPassword, new DatabaseHelper.BooleanCallback() {
                    @Override
                    public void onResult(boolean success) {
                        requireActivity().runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(requireContext(), R.string.password_changed, Toast.LENGTH_SHORT).show();

                                // Limpiar los campos por seguridad
                                editTextCurrentPassword.setText("");
                                editTextNewPassword.setText("");
                                editTextConfirmNewPassword.setText("");
                            } else {
                                Toast.makeText(requireContext(), R.string.password_change_error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
    }

    // Cargar imagen con muestreo para reducir el uso de memoria con imágenes grandes
    private Bitmap loadSampledBitmapFromUri(Uri uri) throws IOException {
        // Primero, obtenemos las dimensiones del bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        try (java.io.InputStream input = requireContext().getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, options);
        }

        // Calcular el factor de muestreo
        int targetSize = 1024; // Tamaño objetivo máximo
        int scaleFactor = 1;

        if (options.outHeight > targetSize || options.outWidth > targetSize) {
            scaleFactor = Math.max(
                    Math.round((float) options.outHeight / targetSize),
                    Math.round((float) options.outWidth / targetSize)
            );
        }

        // Decodificar el bitmap con el factor de muestreo
        options = new BitmapFactory.Options();
        options.inSampleSize = scaleFactor;
        options.inJustDecodeBounds = false;

        try (java.io.InputStream input = requireContext().getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(input, null, options);
        }
    }

    // Permite cerrar sesión
    private void logoutUser() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("app_prefs", requireContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("usuario_actual");
        editor.apply();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(requireContext(), R.string.logout_success, Toast.LENGTH_SHORT).show();
    }
}