package eus.ehu.tictacker;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class TicTacker extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        applyLanguageFromPreferences();
    }

    private void applyLanguageFromPreferences() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String lang = prefs.getString("language", "es");
        setAppLocale(lang);
    }

    private void setAppLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}
