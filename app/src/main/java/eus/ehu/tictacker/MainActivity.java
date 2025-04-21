package eus.ehu.tictacker;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1002;
    private static final String WORK_TAG = "work_time_checker";
    private SharedPreferences sharedPreferences;
    private DatabaseHelper dbHelper;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    private ForegroundTimeService timeService;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ForegroundTimeService.LocalBinder binder = (ForegroundTimeService.LocalBinder) service;
            timeService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dbHelper = new DatabaseHelper(this);
        applyLanguageFromPreferences();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        super.onCreate(savedInstanceState);

        // Comprobar si se ha iniciado sesión
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String usuarioActual = sharedPreferences.getString("usuario_actual", null);

        bindTimeService();

        if (usuarioActual == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        } else {
            checkNetworkConnection();
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
        setupNavigationView(navigationView);

        // Configura NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                // Mostrar u ocultar el botón de retroceso según el destino
                if (destination.getId() == R.id.nav_profile || destination.getId() == R.id.nav_fichaje_details) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
                } else {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                }
            });

            // Configurar AppBar
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_clockin,
                    R.id.nav_history,
                    R.id.nav_settings)
                    .setOpenableLayout(drawerLayout)
                    .build();

            // Vincula la Toolbar y el NavigationView asociado con NavController
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);

            View headerView = navigationView.getHeaderView(0);
            ImageView profileIcon = headerView.findViewById(R.id.nav_header_profile_image);

            profileIcon.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                navController.navigate(R.id.nav_profile);
            });
        }

        // Actualizar / Registrar token FCM en el servidor
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Error al obtener el token FCM", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d("FCM", "Token: " + token);

                    // Guardar el token en SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                    prefs.edit().putString("fcm_token", token).apply();

                    // Subir token a la base de datos
                    dbHelper.updateFCMToken(usuarioActual, token, success -> {
                        if (success) {
                            Log.d("FCM", "Token actualizado en la base de datos remota");
                        } else {
                            Log.e("FCM", "Error al actualizar el token en el servidor");
                        }
                    });
                });
    }

    private void bindTimeService() {
        Intent serviceIntent = new Intent(this, ForegroundTimeService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
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
                // Try to take permission if needed
                try {
                    getContentResolver().takePersistableUriPermission(
                            logoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception e) {
                    Log.e("MainActivity", "Error obteniendo permiso de URI", e);
                }
                navHeaderLogo.setImageURI(null);  // Quitar logo anterior
                navHeaderLogo.setImageURI(logoUri);
            } catch (Exception e) {
                e.printStackTrace();
                // Logo por defecto si hay un error
                navHeaderLogo.setImageResource(R.mipmap.ic_launcher_adaptive_fore);
            }
        } else {
            // Use default logo if no custom logo is set
            navHeaderLogo.setImageResource(R.mipmap.ic_launcher_adaptive_fore);
        }
    }

    public void refreshProfileImage() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        setupNavigationView(navigationView);
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

    private void initializeClockInReminder() {
        ClockInReminderService.scheduleReminder(this);
    }

    private void setupNavigationView(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        CircleImageView profileImage = headerView.findViewById(R.id.nav_header_profile_image);
        TextView usernameText = headerView.findViewById(R.id.nav_header_username);

        String username = dbHelper.getCurrentUsername(this);
        usernameText.setText(username);

        dbHelper.getProfile(username, profile -> {
            if (profile != null && profile.name != null && !profile.name.isEmpty()) {
                usernameText.setText(getString(R.string.hello_user, profile.name));
            }

            if (profile != null && profile.profilePhoto != null && !profile.profilePhoto.isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(profile.profilePhoto, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    profileImage.setImageBitmap(bitmap);
                } catch (Exception e) {
                    profileImage.setImageResource(R.drawable.ic_profile);
                }
            }
        });

        // Configurar el listener del menú de navegación
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Cerrar el drawer cuando se selecciona un ítem
            drawerLayout.closeDrawer(GravityCompat.START);

            // Verificar si ya estamos en el destino seleccionado
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() == itemId) {
                return false;
            }

            // Navegar al destino seleccionado
            navController.navigate(itemId);
            return true;
        });

        // Configurar el clic en la imagen de perfil
        profileImage.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);

            // Verificar si ya estamos en el perfil
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() != R.id.nav_profile) {
                navController.navigate(R.id.nav_profile);
            }
        });
    }

    private void checkNetworkConnection() {
        NetworkConnectivityChecker connectivityChecker = NetworkConnectivityChecker.getInstance(this);
        connectivityChecker.setNetworkStateListener(new NetworkConnectivityChecker.NetworkStateListener() {
            @Override
            public void onNetworkStateChanged(boolean isConnected) {
                if (!isConnected) {
                    Intent intent = new Intent(MainActivity.this, NoInternetActivity.class);
                    intent.putExtra("return_activity", MainActivity.class.getName());
                    startActivity(intent);
                }
            }
        });
        connectivityChecker.register();

        // Comprobación inicial
        if (!connectivityChecker.isConnected()) {
            Intent intent = new Intent(this, NoInternetActivity.class);
            intent.putExtra("return_activity", MainActivity.class.getName());
            startActivity(intent);
        }
    }

    public void setSelectedMenuItem(int menuItemId) {
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setCheckedItem(menuItemId);
    }

    private void checkReminderPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.permission_required))
                        .setMessage(getString(R.string.exact_alarm_permission_message))
                        .setPositiveButton(getString(R.string.settings), (dialog, which) -> {
                            Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkReminderPermissions();
        if (!serviceBound) {
            bindTimeService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkNetworkConnection();
        NavigationView navigationView = findViewById(R.id.nav_view);
        applyCustomLogoToNavHeader(navigationView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        NetworkConnectivityChecker connectivityChecker = NetworkConnectivityChecker.getInstance(this);
        connectivityChecker.setNetworkStateListener(null);
        connectivityChecker.unregister();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

}