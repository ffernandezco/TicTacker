package eus.ehu.tictacker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class UserProfile {
    public String username;
    public String name;
    public String surname;
    public String birthdate;
    public String email;
    public String profilePhoto; // Foto de perfil (base64 - LONGTEXT)

    public UserProfile() {
        // Constructor por defecto
    }

    public UserProfile(String username, String name, String surname, String birthdate, String email) {
        this.username = username;
        this.name = name;
        this.surname = surname;
        this.birthdate = birthdate;
        this.email = email;
    }

    // Comprobar que el usuario sea mayor de edad
    public boolean isAdult() {
        if (birthdate == null || birthdate.isEmpty()) {
            return false;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date birthDate = sdf.parse(birthdate);
            Calendar dob = Calendar.getInstance();
            dob.setTime(birthDate);

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

            if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH) ||
                    (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) &&
                            today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
                age--;
            }

            return age >= 18;
        } catch (Exception e) {
            return false;
        }
    }
}