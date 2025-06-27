package com.example.elradardemoises.fragments;

import static android.content.Intent.getIntent;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.elradardemoises.DashboardActivity;
import com.example.elradardemoises.GestorFiltroFecha;
import com.example.elradardemoises.MainActivity;
import com.example.elradardemoises.R;
import com.example.elradardemoises.Ubicacion;
import com.example.elradardemoises.models.Bmp180;
import com.example.elradardemoises.models.Dht11;
import com.example.elradardemoises.models.LLuvia;
import com.example.elradardemoises.models.Usuario;
import com.example.elradardemoises.utils.UserManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class Fragment_principal extends Fragment implements GestorFiltroFecha.FiltroFechaListener,
        Ubicacion.LocationCallback{

    private static final String TAG = "DashboardActivity";

    private TextView tvUserName, tvUserEmail;
    private ImageView ivProfilePicture;
    private MaterialButton btnLogout;

    // DHT11
    private TextView tvTemperatura, tvHumedad, tvTimestamp;

    // BMP180
    private TextView tvAltitud, tvPresion, tvPresionNivelMar, tvTimestampBmp;

    // LLUVIA
    private TextView tvLluvia;

    private MaterialButton btnRefreshWeather;

    // Nuevos elementos para filtro de fecha
    private MaterialButton btnFiltrarFecha, btnLimpiarFiltro;
    private TextView tvFechaSeleccionada;

    private DatabaseReference databaseReference;
    private Usuario usuarioActual;
    private Dht11 datosMeteorologicos;
    private Bmp180 datosBarometricos;
    private LLuvia datosLluvia;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView lblUbicacion;

    private ValueEventListener weatherListener;
    private ValueEventListener bmpListener;
    private ValueEventListener lluviaListener;

    private GestorFiltroFecha gestorFiltroFecha;
    private Ubicacion ubicacion;

    public Fragment_principal() {

    }

    public static Fragment_principal newInstance(String param1, String param2) {
        Fragment_principal fragment = new Fragment_principal();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_principal, container, false);

        initializeViews(view);
        initializeFirebase();
        setupClickListeners();
        cargarDatosUsuario();
        cargarDatosMeteorologicos();
        cargarDatosBarometricos();
        cargarDatosLluvia();
        initializeLocation();

        gestorFiltroFecha.inicializarVistas(view);
        gestorFiltroFecha.configurarClickListeners();
        return view;
    }

    private void initializeViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        btnLogout = view.findViewById(R.id.btnLogout);

        // DHT11 Views
        tvTemperatura = view.findViewById(R.id.tvTemperatura);
        tvHumedad = view.findViewById(R.id.tvHumedad);
        tvTimestamp = view.findViewById(R.id.tvTimestamp);

        // BMP180 Views
        tvAltitud = view.findViewById(R.id.tvAltitud);
        tvPresion = view.findViewById(R.id.tvPresion);
        tvPresionNivelMar = view.findViewById(R.id.tvPresionNivelMar);
        tvTimestampBmp = view.findViewById(R.id.tvTimestampBmp);

        // LLUVIA View
        tvLluvia = view.findViewById(R.id.tvLluvia);

        lblUbicacion = view.findViewById(R.id.lblUbicacion);

        btnRefreshWeather = view.findViewById(R.id.btnRefreshWeather);

    }

    private void initializeFirebase() {
      //  databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> mostrarDialogoConfirmacion());

        btnRefreshWeather.setOnClickListener(v -> {
            btnRefreshWeather.animate().rotation(0).setDuration(500).start();
            if (gestorFiltroFecha.getFechaSeleccionada() != null) {
                gestorFiltroFecha.cargarDatosPorFecha(gestorFiltroFecha.getFechaSeleccionada());
            } else {
                cargarDatosMeteorologicos();
                cargarDatosBarometricos();
                cargarDatosLluvia();
            }
            Toast.makeText(requireContext(), "Actualizando datos...", Toast.LENGTH_SHORT).show();
        });

    }

    @Override
    public void onFechaSeleccionada(String fecha) {
        Log.d("Fragment", "Fecha seleccionada: " + fecha);
    }

    @Override
    public void onFiltroLimpiado() {
        cargarDatosMeteorologicos();
        cargarDatosBarometricos();
        cargarDatosLluvia();
    }

    public void onDatosMeteorologicos(Dht11 datos) {
        datosMeteorologicos = datos;
        mostrarDatosMeteorologicos(datos);
    }


    @Override
    public void onDatosBarometricos(Bmp180 datos) {
        datosBarometricos = datos;
        mostrarDatosBarometricos(datos);
    }

    @Override
    public void onDatosLluvia(LLuvia datos) {
        datosLluvia = datos;
        mostrarDatosLluvia(datos);
    }


    @Override
    public void onDatosVaciosMeteorologicos() {
        mostrarDatosVaciosDht11();
    }

    @Override
    public void onDatosVaciosBarometricos() {
        mostrarDatosVaciosBmp180();
    }

    @Override
    public void onDatosVaciosLluvia() {
        mostrarDatosVaciosLluvia();
    }

    @Override
    public Context getContext() {
        return super.getContext();
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        gestorFiltroFecha = new GestorFiltroFecha(this);
    }

    @Override
    public DatabaseReference getDatabaseReference() {
        return databaseReference;
    }

    private void mostrarDialogoConfirmacion() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> cerrarSesion())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void initializeLocation() {
        ubicacion = new Ubicacion(this, lblUbicacion);
        ubicacion.initializeLocation();
    }

    @Override
    public void onLocationObtained(String locationText) {
        Log.d("Fragment", "Ubicación obtenida: " + locationText);
    }

    @Override
    public void onLocationError(String error) {
        Log.e("Fragment", "Error de ubicación: " + error);
    }

    @Override
    public void onPermissionDenied() {
        // Manejar cuando el usuario niega los permisos
        Log.w("Fragment", "Permisos de ubicación denegados");
    }

    public void refreshLocation() {
        if (ubicacion != null) {
            ubicacion.refreshLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        ubicacion.handlePermissionResult(requestCode, permissions, grantResults);
    }

    private void cargarDatosLluvia() {
        if (lluviaListener != null) {
            databaseReference.child("lluvia").removeEventListener(lluviaListener);
        }

        lluviaListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    LLuvia ultimosDatos = null;

                    if (snapshot.hasChild("actual")) {
                        ultimosDatos = snapshot.child("actual").getValue(LLuvia.class);
                    } else {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ultimosDatos = child.getValue(LLuvia.class);
                        }
                    }

                    if (ultimosDatos != null) {
                        datosLluvia = ultimosDatos;
                        mostrarDatosLluvia(ultimosDatos);
                    }
                } else {
                    Log.d(TAG, "No hay datos de lluvia disponibles");
                    mostrarDatosVaciosLluvia();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos de lluvia: " + error.getMessage());
                Toast.makeText(requireContext(),
                        "Error al cargar datos de lluvia", Toast.LENGTH_SHORT).show();
                mostrarDatosVaciosLluvia();
            }
        };

        databaseReference.child("lluvia").addValueEventListener(lluviaListener);
    }

    private void mostrarDatosLluvia(LLuvia datos) {
        if (datos != null && datos.getEstado() != null) {
            TextView tvEmoji = getView().findViewById(R.id.tvEmojiLluvia);
            if (tvEmoji != null) {
                tvEmoji.setText(datos.getEmojiEstado());
            }

            tvLluvia.setText(datos.getEstadoFormateado());
        } else {
            mostrarDatosVaciosLluvia();
        }
    }

    private void mostrarDatosVaciosLluvia() {
        tvLluvia.setText("Sin datos");
        TextView tvEmoji = getView().findViewById(R.id.tvEmojiLluvia);
        if (tvEmoji != null) {
            tvEmoji.setText("❓");
        }
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
                Toast.makeText(requireContext(),
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
                Toast.makeText(requireContext(),
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
        Intent intent = requireActivity().getIntent();
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
            Toast.makeText(requireContext(), "No hay usuario autenticado", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(requireContext(),
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

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        requireActivity().finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            cerrarSesion();
            return true;
        } else if (id == R.id.action_profile) {
            Toast.makeText(requireContext(), "Perfil", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (usuarioActual != null) {
            mostrarDatosUsuario(usuarioActual);
        } else {
            cargarFotoDirectaDesdeFirebase();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (weatherListener != null && databaseReference != null) {
            databaseReference.child("dht11").removeEventListener(weatherListener);
        }
        if (bmpListener != null && databaseReference != null) {
            databaseReference.child("bmp180").removeEventListener(bmpListener);
        }
        if (lluviaListener != null && databaseReference != null) {
            databaseReference.child("lluvia").removeEventListener(lluviaListener);
        }
        if (gestorFiltroFecha != null) {
            gestorFiltroFecha.limpiarListeners();
        }
    }
}