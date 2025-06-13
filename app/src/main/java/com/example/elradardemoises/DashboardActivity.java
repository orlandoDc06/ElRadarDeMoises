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
import com.example.elradardemoises.models.Usuario;
import com.example.elradardemoises.utils.UserManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DashboardActivity extends AppCompatActivity {
    private static final String TAG = "DashboardActivity";

    // UI Elements
    private TextView tvUserName, tvUserEmail;
    private ImageView ivProfilePicture;

    // Firebase
    private DatabaseReference databaseReference;
    private Usuario usuarioActual;

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
        cargarDatosUsuario();
    }

    private void initializeViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
    }

    private void initializeFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    private void cargarDatosUsuario() {
        // Primero intentar obtener datos del Intent
        Intent intent = getIntent();
        String userEmail = intent.getStringExtra("user_email");
        String userName = intent.getStringExtra("user_name");

        if (userEmail != null) {
            tvUserEmail.setText(userEmail);

            if (userName != null && !userName.isEmpty()) {
                tvUserName.setText(userName);
            } else {
                // Extraer nombre del correo si no hay displayName
                String nombreExtraido = Usuario.extraerNombreDeCorreo(userEmail);
                tvUserName.setText(nombreExtraido);
            }
        } else {
            // Si no hay datos en el Intent, cargar desde Firebase
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
                            // Si no existe, crearlo usando UserManager
                            UserManager.guardarUsuarioEnBaseDatos(currentUser, databaseReference);

                            // Crear usuario temporal para mostrar
                            Usuario usuarioTemp = new Usuario();
                            usuarioTemp.setCorreo(currentUser.getEmail());
                            usuarioTemp.setNombre(obtenerNombreUsuario(currentUser));

                            // Obtener URL de foto mejorada
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

            // Si es una URL de Google, mejorar la calidad
            if (urlOriginal.contains("googleusercontent.com")) {
                // Cambiar el tamaño por defecto por uno más grande
                String urlMejorada = urlOriginal.replace("s96-c", "s400-c");

                return urlMejorada;
            }

            return urlOriginal;
        }

        return "";
    }

    private void mostrarDatosUsuario(Usuario usuario) {
        // Actualizar textos
        tvUserName.setText(usuario.getNombre());
        //tvUserEmail.setText("Email: " + usuario.getCorreo());

        // Cargar foto de perfil con debug
        Log.d(TAG, "Intentando cargar foto de perfil: " + usuario.getPp());
        cargarFotoPerfil(usuario.getPp());
    }

    private void cargarFotoPerfil(String urlFoto) {

        if (urlFoto != null && !urlFoto.isEmpty()) {

            // Configuración más robusta de Glide
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
            // Mostrar imagen por defecto
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
            // Abrir actividad de perfil (opcional)
            Toast.makeText(this, "Perfil", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_profile) {
            // Opción para recargar foto (agregar en el menú si es necesario)
            cargarFotoDirectaDesdeFirebase();
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
        // Recargar datos cuando se vuelve a la actividad
        if (usuarioActual != null) {
            mostrarDatosUsuario(usuarioActual);
        } else {
            // Si no hay datos, intentar cargar foto directamente
            cargarFotoDirectaDesdeFirebase();
        }
    }
}