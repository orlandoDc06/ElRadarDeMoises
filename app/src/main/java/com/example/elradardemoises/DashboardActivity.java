package com.example.elradardemoises;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.elradardemoises.models.Dht11;
import com.example.elradardemoises.models.Bmp180;
import com.example.elradardemoises.models.Usuario;
import com.example.elradardemoises.utils.UserManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {
    private static final String TAG = "DashboardActivity";

    private TextView tvUserName, tvUserEmail;
    private ImageView ivProfilePicture;
    private MaterialButton btnLogout;

    // DHT11 Views
    private TextView tvTemperatura, tvHumedad, tvTimestamp;

    // BMP180 Views
    private TextView tvAltitud, tvPresion, tvPresionNivelMar, tvTimestampBmp;

    private MaterialButton btnRefreshWeather;

    private DatabaseReference databaseReference;
    private Usuario usuarioActual;
    private Dht11 datosMeteorologicos;
    private Bmp180 datosBarometricos;

    private ValueEventListener weatherListener;
    private ValueEventListener bmpListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        initializeFirebase();
        setupClickListeners();
        cargarDatosUsuario();
        cargarDatosMeteorologicos();
        cargarDatosBarometricos();
    }

    private void initializeViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        btnLogout = findViewById(R.id.btnLogout);

        // DHT11 Views
        tvTemperatura = findViewById(R.id.tvTemperatura);
        tvHumedad = findViewById(R.id.tvHumedad);
        tvTimestamp = findViewById(R.id.tvTimestamp);

        // BMP180 Views
        tvAltitud = findViewById(R.id.tvAltitud);
        tvPresion = findViewById(R.id.tvPresion);
        tvPresionNivelMar = findViewById(R.id.tvPresionNivelMar);
        tvTimestampBmp = findViewById(R.id.tvTimestampBmp);

        btnRefreshWeather = findViewById(R.id.btnRefreshWeather);
    }

    private void initializeFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> mostrarDialogoConfirmacion());

        btnRefreshWeather.setOnClickListener(v -> {
            btnRefreshWeather.animate().rotation(360).setDuration(500).start();
            cargarDatosMeteorologicos();
            cargarDatosBarometricos();
            Toast.makeText(this, "Actualizando datos...", Toast.LENGTH_SHORT).show();
        });
    }

    private void mostrarDialogoConfirmacion() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> cerrarSesion())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cargarDatosMeteorologicos() {
        if (weatherListener != null) {
            databaseReference.child("dht11").removeEventListener(weatherListener);
        }

        weatherListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Dht11 ultimosDatos = null;

                    if (snapshot.hasChild("actual")) {
                        ultimosDatos = snapshot.child("actual").getValue(Dht11.class);
                    } else {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ultimosDatos = child.getValue(Dht11.class);
                        }
                    }

                    if (ultimosDatos != null) {
                        datosMeteorologicos = ultimosDatos;
                        mostrarDatosMeteorologicos(ultimosDatos);
                    }
                } else {
                    Log.d(TAG, "No hay datos meteorológicos DHT11 disponibles");
                    mostrarDatosVaciosDht11();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos DHT11: " + error.getMessage());
                Toast.makeText(DashboardActivity.this,
                        "Error al cargar datos DHT11", Toast.LENGTH_SHORT).show();
                mostrarDatosVaciosDht11();
            }
        };

        databaseReference.child("dht11").addValueEventListener(weatherListener);
    }

    private void cargarDatosBarometricos() {
        if (bmpListener != null) {
            databaseReference.child("bmp180").removeEventListener(bmpListener);
        }

        bmpListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Bmp180 ultimosDatos = null;

                    if (snapshot.hasChild("actual")) {
                        ultimosDatos = snapshot.child("actual").getValue(Bmp180.class);
                    } else {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ultimosDatos = child.getValue(Bmp180.class);
                        }
                    }

                    if (ultimosDatos != null) {
                        datosBarometricos = ultimosDatos;
                        mostrarDatosBarometricos(ultimosDatos);
                    }
                } else {
                    Log.d(TAG, "No hay datos barométricos BMP180 disponibles");
                    mostrarDatosVaciosBmp180();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos BMP180: " + error.getMessage());
                Toast.makeText(DashboardActivity.this,
                        "Error al cargar datos BMP180", Toast.LENGTH_SHORT).show();
                mostrarDatosVaciosBmp180();
            }
        };

        databaseReference.child("bmp180").addValueEventListener(bmpListener);
    }

    private void mostrarDatosMeteorologicos(Dht11 datos) {
        if (datos != null) {
            double temperatura = datos.getTemperatura();
            if (temperatura != 0) {
                tvTemperatura.setText(String.format("%.1f°C", temperatura));
            } else {
                tvTemperatura.setText("-- °C");
            }

            int humedad = datos.getHumedad();
            if (humedad != 0) {
                tvHumedad.setText(humedad + "%");
            } else {
                tvHumedad.setText("-- %");
            }

            String timestamp = datos.getTimestamp();
            if (timestamp != null && !timestamp.isEmpty()) {
                tvTimestamp.setText("Última actualización: " + datos.getTimestampFormateado());
            } else {
                tvTimestamp.setText("Última actualización: " +
                        new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
            }
        }
    }

    private void mostrarDatosBarometricos(Bmp180 datos) {
        if (datos != null) {
            double altitud = datos.getAltitud();
            if (altitud != 0) {
                tvAltitud.setText(String.format("%.1f m", altitud));
            } else {
                tvAltitud.setText("-- m");
            }

            double presion = datos.getPresion();
            if (presion != 0) {
                tvPresion.setText(String.format("%.1f hPa", presion));
            } else {
                tvPresion.setText("-- hPa");
            }

            double presionNivelMar = datos.getPresion_nivel_mar();
            if (presionNivelMar != 0) {
                tvPresionNivelMar.setText(String.format("%.1f hPa", presionNivelMar));
            } else {
                tvPresionNivelMar.setText("-- hPa");
            }

            String timestamp = datos.getTimestamp();
            if (timestamp != null && !timestamp.isEmpty()) {
                tvTimestampBmp.setText("Última actualización: " + datos.getTimestampFormateado());
            } else {
                tvTimestampBmp.setText("Última actualización: " +
                        new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
            }
        }
    }

    private void mostrarDatosVaciosDht11() {
        tvTemperatura.setText("-- °C");
        tvHumedad.setText("-- %");
        tvTimestamp.setText("Sin datos disponibles");
    }

    private void mostrarDatosVaciosBmp180() {
        tvAltitud.setText("-- m");
        tvPresion.setText("-- hPa");
        tvPresionNivelMar.setText("-- hPa");
        tvTimestampBmp.setText("Sin datos disponibles");
    }

    // El resto del código permanece igual...
    private void cargarDatosUsuario() {
        Intent intent = getIntent();
        String userEmail = intent.getStringExtra("user_email");
        String userName = intent.getStringExtra("user_name");

        if (userEmail != null) {
            tvUserEmail.setText(userEmail);

            if (userName != null && !userName.isEmpty()) {
                tvUserName.setText(userName);
            } else {
                String nombreExtraido = Usuario.extraerNombreDeCorreo(userEmail);
                tvUserName.setText(nombreExtraido);
            }
        } else {
            cargarDatosDesdeFirebase();
        }
    }

    private void cargarDatosDesdeFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No hay usuario autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        databaseReference.child("usuarios").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Usuario usuario = snapshot.getValue(Usuario.class);
                            if (usuario != null) {
                                usuario.setKey(uid);
                                usuarioActual = usuario;
                                mostrarDatosUsuario(usuario);
                            }
                        } else {
                            UserManager.guardarUsuarioEnBaseDatos(currentUser, databaseReference);

                            Usuario usuarioTemp = new Usuario();
                            usuarioTemp.setCorreo(currentUser.getEmail());
                            usuarioTemp.setNombre(obtenerNombreUsuario(currentUser));

                            String urlFoto = obtenerUrlFotoMejorada(currentUser);
                            usuarioTemp.setPp(urlFoto);
                            usuarioTemp.setKey(uid);

                            usuarioActual = usuarioTemp;
                            mostrarDatosUsuario(usuarioTemp);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error al cargar datos del usuario: " + error.getMessage());
                        Toast.makeText(DashboardActivity.this,
                                "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String obtenerNombreUsuario(FirebaseUser firebaseUser) {
        String nombre = firebaseUser.getDisplayName();

        if (nombre == null || nombre.isEmpty()) {
            if (firebaseUser.getEmail() != null) {
                nombre = Usuario.extraerNombreDeCorreo(firebaseUser.getEmail());
            } else {
                nombre = "Usuario";
            }
        }

        return nombre;
    }

    private String obtenerUrlFotoMejorada(FirebaseUser firebaseUser) {
        if (firebaseUser.getPhotoUrl() != null) {
            String urlOriginal = firebaseUser.getPhotoUrl().toString();
            Log.d(TAG, "URL original de foto: " + urlOriginal);

            if (urlOriginal.contains("googleusercontent.com")) {
                String urlMejorada = urlOriginal.replace("s96-c", "s400-c");
                return urlMejorada;
            }

            return urlOriginal;
        }

        return "";
    }

    private void mostrarDatosUsuario(Usuario usuario) {
        tvUserName.setText(usuario.getNombre());
        Log.d(TAG, "Intentando cargar foto de perfil: " + usuario.getPp());
        cargarFotoPerfil(usuario.getPp());
    }

    private void cargarFotoPerfil(String urlFoto) {
        if (urlFoto != null && !urlFoto.isEmpty()) {
            RequestOptions requestOptions = new RequestOptions()
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_person_placeholder)
                    .error(R.drawable.ic_person_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false);

            Glide.with(this)
                    .load(urlFoto)
                    .apply(requestOptions)
                    .into(ivProfilePicture);

            Log.d(TAG, "Glide load iniciado para: " + urlFoto);
        } else {
            Log.d(TAG, "URL de foto vacía o null");
            ivProfilePicture.setImageResource(R.drawable.ic_person_placeholder);
        }
    }

    private void cargarFotoDirectaDesdeFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getPhotoUrl() != null) {
            String urlFoto = obtenerUrlFotoMejorada(currentUser);
            Log.d(TAG, "Cargando foto directamente desde Firebase: " + urlFoto);
            cargarFotoPerfil(urlFoto);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            cerrarSesion();
            return true;
        } else if (id == R.id.action_profile) {
            Toast.makeText(this, "Perfil", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (usuarioActual != null) {
            mostrarDatosUsuario(usuarioActual);
        } else {
            cargarFotoDirectaDesdeFirebase();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (weatherListener != null && databaseReference != null) {
            databaseReference.child("dht11").removeEventListener(weatherListener);
        }
        if (bmpListener != null && databaseReference != null) {
            databaseReference.child("bmp180").removeEventListener(bmpListener);
        }
    }
}