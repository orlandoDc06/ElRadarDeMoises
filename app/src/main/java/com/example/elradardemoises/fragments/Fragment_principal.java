package com.example.elradardemoises.fragments;

import static android.content.Intent.getIntent;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
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
import com.example.elradardemoises.MainActivity;
import com.example.elradardemoises.R;
import com.example.elradardemoises.models.Bmp180;
import com.example.elradardemoises.models.Dht11;
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

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_principal#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_principal extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    //Declaraciones de variables
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


    public Fragment_principal() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Fragment_principal.
     */
    // TODO: Rename and change types and number of parameters
    public static Fragment_principal newInstance(String param1, String param2) {
        Fragment_principal fragment = new Fragment_principal();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_principal, container, false);
        View view = inflater.inflate(R.layout.fragment_principal, container, false);

        initializeViews(view);
        initializeFirebase();
        setupClickListeners();
        cargarDatosUsuario();
        cargarDatosMeteorologicos();
        cargarDatosBarometricos();


        return view;
    }

    ///
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

        btnRefreshWeather = view.findViewById(R.id.btnRefreshWeather);
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
            Toast.makeText(requireContext(), "Actualizando datos...", Toast.LENGTH_SHORT).show();
        });
    }

    private void mostrarDialogoConfirmacion() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
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

    ///

//    @Override
//    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
//        inflater.inflate(R.menu.dashboard_menu, menu);
//        super.onCreateOptionsMenu(menu, inflater);
//    }

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
        super.onResume(); // Esto es opcional
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
    }


    //
}