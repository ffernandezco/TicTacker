package eus.ehu.tictacker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextInputEditText editTextName, editTextSurname, editTextEmail, editTextBirthdate;
    private MaterialButton buttonSaveProfile;
    private DatabaseHelper dbHelper;
    private SimpleDateFormat dateFormat;
    private String username;

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

        // Necesario para las fechas de nacimientos
        editTextBirthdate.setOnClickListener(v -> showDatePickerDialog());

        // Hacer que se muestren los datos guardados
        loadProfileData();

        buttonSaveProfile.setOnClickListener(v -> saveProfileData());
    }

    private void loadProfileData() {
        dbHelper.getProfile(username, profile -> {
            if (profile != null) {
                editTextName.setText(profile.name);
                editTextSurname.setText(profile.surname);
                editTextEmail.setText(profile.email);
                editTextBirthdate.setText(profile.birthdate);
            }
        });
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();

        // Fecha inicial -> Actual
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

        // Evitar menos de 18 años
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, -18);
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void saveProfileData() {
        String name = editTextName.getText().toString().trim();
        String surname = editTextSurname.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String birthdate = editTextBirthdate.getText().toString().trim();

        // Validación de campos
        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || birthdate.isEmpty()) {
            Toast.makeText(requireContext(), R.string.all_fields_required, Toast.LENGTH_SHORT).show();
            return;
        }

        // Validación de email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError(getString(R.string.invalid_email));
            return;
        }

        // Validar edad
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

            if (age < 18) {
                editTextBirthdate.setError(getString(R.string.must_be_adult));
                return;
            }
        } catch (ParseException e) {
            editTextBirthdate.setError(getString(R.string.invalid_date));
            return;
        }

        // Crear y/o guardar el perfil
        UserProfile profile = new UserProfile();
        profile.username = username;
        profile.name = name;
        profile.surname = surname;
        profile.email = email;
        profile.birthdate = birthdate;

        dbHelper.updateProfile(profile, success -> {
            if (success) {
                Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), R.string.error_saving_profile, Toast.LENGTH_SHORT).show();
            }
        });
    }
}