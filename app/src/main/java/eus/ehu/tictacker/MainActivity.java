package eus.ehu.tictacker;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.navigation.NavigationView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1002;
    private static final String WORK_TAG = "work_time_checker";
    private SharedPreferences sharedPreferences;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyLanguageFromPreferences();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        super.onCreate(savedInstanceState);

        // Comprobar si se ha iniciado sesión
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String usuarioActual = sharedPreferences.getString("usuario_actual", null);

        if (usuarioActual == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary_dark));

        // Solicitar permisos
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permiso otorgado
                        scheduleWorkTimeCheck();
                    }
                }
        );

        checkNotificationPermission();

        // Crear canal para notificaciones
        new NotificationHelper(this);

        // Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));

        // DrawerLayout y NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        applyCustomLogoToNavHeader(navigationView);

        // Configura NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Configura AppBarConfiguration
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_clockin,
                    R.id.nav_history,
                    R.id.nav_settings)
                    .setOpenableLayout(drawerLayout)
                    .build();

            // Vincula la Toolbar y el NavigationView asociado con NavController
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);
        }
    }



    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

    private void applyCustomLogoToNavHeader(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        ImageView navHeaderLogo = headerView.findViewById(R.id.nav_header_logo);

        // Logo personalizado
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String logoUriString = prefs.getString("custom_logo_uri", null);

        if (logoUriString != null) {
            try {
                Uri logoUri = Uri.parse(logoUriString);
                navHeaderLogo.setImageURI(logoUri);
            } catch (Exception e) {
                e.printStackTrace();
                // Logo por defecto si hay un error
                navHeaderLogo.setImageResource(R.mipmap.ic_launcher_adaptive_fore);
            }
        }
    }

    //No necesario
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void applyLanguageFromPreferences() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String lang = prefs.getString("language", "es");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                scheduleWorkTimeCheck();
            }
        } else {
            scheduleWorkTimeCheck(); // Si la versión de Android es antigua
        }
    }


    private void scheduleWorkTimeCheck() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                WorkTimeCheckWorker.class,
                5, TimeUnit.MINUTES) //Comprueba notificaciones cada 5 minutos
                .build();

        WorkManager.getInstance(this).enqueue(workRequest);
    }
}