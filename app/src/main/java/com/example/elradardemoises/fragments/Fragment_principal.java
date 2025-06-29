package com.example.elradardemoises.fragments;

import static android.content.Intent.getIntent;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
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
import com.example.elradardemoises.models.Luz;
import com.example.elradardemoises.models.Mq2;
import com.example.elradardemoises.models.Suelo;
import com.example.elradardemoises.models.Usuario;
import com.example.elradardemoises.models.Viento;
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

import java.io.File;
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

    // Viento
    private TextView tvViento;

    // Gases
    private TextView tvGases, tvPorcentaje;

    //Suelo
    private TextView tvSuelo, tvPorcentajeSuelo;

    //Luz
    private TextView tvLuz, tvIluminancia;

    private MaterialButton btnRefreshWeather;

    private DatabaseReference databaseReference;
    private Usuario usuarioActual;
    private Dht11 datosMeteorologicos;
    private Bmp180 datosBarometricos;
    private LLuvia datosLluvia;
    private Viento datosViento;
    private Mq2 datosGases;
    private Suelo datosSuelo;
    private Luz datosLuz;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView lblUbicacion;

    private ValueEventListener weatherListener;
    private ValueEventListener bmpListener;
    private ValueEventListener lluviaListener;
    private ValueEventListener vientoListener;
    private ValueEventListener gasesListener;
    private ValueEventListener sueloListener;
    private ValueEventListener luzListener;

    private GestorFiltroFecha gestorFiltroFecha;
    private Ubicacion ubicacion;
    private static final int PERMISSION_REQUEST_CODE = 1001;

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
        cargarDatosViento();
        cargarDatosGases();
        cargarDatosSuelo();
        cargarDatosLuz();
        initializeLocation();
        solicitarPermisos();

        gestorFiltroFecha.inicializarVistas(view);
        gestorFiltroFecha.configurarClickListeners();
        return view;
    }

    private void initializeViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        btnLogout = view.findViewById(R.id.btnLogout);

        // DHT11
        tvTemperatura = view.findViewById(R.id.tvTemperatura);
        tvHumedad = view.findViewById(R.id.tvHumedad);
        tvTimestamp = view.findViewById(R.id.tvTimestamp);

        // BMP180
        tvAltitud = view.findViewById(R.id.tvAltitud);
        tvPresion = view.findViewById(R.id.tvPresion);
        tvPresionNivelMar = view.findViewById(R.id.tvPresionNivelMar);
        tvTimestampBmp = view.findViewById(R.id.tvTimestampBmp);

        // LLUVIA
        tvLluvia = view.findViewById(R.id.tvLluvia);

        //Viento
        tvViento = view.findViewById(R.id.tvViento);

        // Gases
        tvGases = view.findViewById(R.id.tvGases);
        tvPorcentaje = view.findViewById(R.id.tvPorcentaje);

        //Suelo
        tvSuelo = view.findViewById(R.id.tvSuelo);
        tvPorcentajeSuelo = view.findViewById(R.id.tvPorcentajeSuelo);

        //Luz
        tvLuz = view.findViewById(R.id.tvLuz);
        tvIluminancia = view.findViewById(R.id.tvIluminancia);

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
                cargarDatosViento();
                cargarDatosGases();
                cargarDatosSuelo();
                cargarDatosLuz();
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
        cargarDatosViento();
        cargarDatosGases();
        cargarDatosSuelo();
        cargarDatosLuz();
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
    public void onDatosViento(Viento datos) {
        datosViento = datos;
        mostrarDatosViento(datos);
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
    public void onDatosVaciosViento() {
        mostrarDatosVaciosViento();
    }

    @Override
    public void onDatosGases(Mq2 datos) {
        datosGases = datos;
        mostrarDatosGases(datos);
    }

    @Override
    public void onDatosVaciosGases() {
        mostrarDatosVaciosGases();
    }

    @Override
    public void onDatosSuelo(Suelo datos) {
        datosSuelo = datos;
        mostrarDatosSuelo(datos);
    }

    @Override
    public void onDatosVaciosSuelo() {
        mostrarDatosVaciosSuelo();
    }

    @Override
    public void onDatosLuz(Luz datos) {
        datosLuz = datos;
        mostrarDatosLuz(datos);
    }

    @Override
    public void onDatosVaciosLuz() {
        mostrarDatosVaciosLuz();
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
        Log.w("Fragment", "Permisos de ubicación denegados");
    }

    public void refreshLocation() {
        if (ubicacion != null) {
            ubicacion.refreshLocation();
        }
    }



    @Override
    public void onPdfGenerado(String rutaArchivo) {
        Log.d("Fragment", "PDF generado en: " + rutaArchivo);

    }

    @Override
    public void onErrorGenerandoPdf(String error) {
        Log.e("Fragment", "Error generando PDF: " + error);
        Toast.makeText(getContext(), "Error al generar PDF: " + error, Toast.LENGTH_LONG).show();
    }


    private void abrirPdf(String rutaArchivo) {
        try {
            File archivo = new File(rutaArchivo);
            if (archivo.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = FileProvider.getUriForFile(getContext(),
                        getContext().getPackageName() + ".fileprovider", archivo);
                intent.setDataAndType(uri, "application/pdf");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "No se puede abrir el PDF", Toast.LENGTH_SHORT).show();
        }
    }
    private void solicitarPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Permisos concedidos", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Permisos necesarios para generar PDF",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void cargarDatosViento(){
        if (vientoListener != null){
            databaseReference.child("viento").removeEventListener(vientoListener);
        }

        vientoListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getView() == null || isDetached()) {
                    return;
                }
                if (snapshot.exists()){
                    Viento ultimosDatos = null;

                    if (snapshot.hasChild("actual")){
                        ultimosDatos = snapshot.child("actual").getValue(Viento.class);
                    }
                    else{
                        for (DataSnapshot child : snapshot.getChildren()){
                            ultimosDatos = child.getValue(Viento.class);
                        }
                    }
                    if (ultimosDatos != null) {
                        datosViento = ultimosDatos;
                        mostrarDatosViento(ultimosDatos);
                    }
                }
                else {
                    mostrarDatosVaciosViento();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(),
                        "Error al cargar datos de viento", Toast.LENGTH_SHORT).show();
                mostrarDatosVaciosViento();
            }
        };
        databaseReference.child("viento").addValueEventListener(vientoListener);
    }

    private void mostrarDatosViento(Viento datos) {
        if (!isAdded() || getView() == null || isDetached()) {
            return;
        }
        if (datos != null && String.valueOf(datos.getVelocidad()) != null) {

            tvViento.setText(datos.getVelocidad() + " Km/h");
        } else {
            mostrarDatosVaciosLluvia();
        }
    }

    private void mostrarDatosVaciosViento() {
        if (!isAdded() || getView() == null || isDetached()) {
            return;
        }
        tvViento.setText("Sin datos");

    }

    public void cargarDatosSuelo(){
        if(sueloListener != null){
            databaseReference.child("suelo").removeEventListener(gasesListener);
        }

        sueloListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getView() == null || isDetached()) {
                    return;
                }
                if (snapshot.exists()) {
                    Suelo ultimosDatos = null;

                    if (snapshot.hasChild("actual")) {
                        ultimosDatos = snapshot.child("actual").getValue(Suelo.class);
                    } else {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ultimosDatos = child.getValue(Suelo.class);
                        }
                    }
                    if (ultimosDatos != null) {
                        datosSuelo = ultimosDatos;
                        mostrarDatosSuelo(ultimosDatos);
                    }
                } else {
                    Log.d(TAG, "No hay datos de suelo disponibles");
                    mostrarDatosVaciosSuelo();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(),
                        "Error al cargar datos de suelo", Toast.LENGTH_SHORT).show();
                mostrarDatosVaciosSuelo();
            }
        };
        databaseReference.child("suelo").addValueEventListener(sueloListener);
    }

    public void mostrarDatosSuelo(Suelo datos){
        if (!isAdded() || getView() == null || isDetached()) {
            return;
        }
        if (datos != null && datos.getEstado() != null) {
            TextView tvEmoji = getView().findViewById(R.id.tvEmojiSuelo);
            if (tvEmoji != null) {
                tvEmoji.setText(datos.getEmojiEstado());
            }

            tvSuelo.setText(datos.getEstadoFormateado());
            tvPorcentajeSuelo.setText(datos.getPorcentaje() + " %");
        } else {
            mostrarDatosVaciosSuelo();
        }
    }

    public void mostrarDatosVaciosSuelo(){
        if (!isAdded() || getView() == null || isDetached()) {
            return;
        }
        tvSuelo.setText("Sin datos");
        TextView tvEmoji = getView().findViewById(R.id.tvEmojiSuelo);
        if (tvEmoji != null) {
            tvEmoji.setText("❓");
        }
        tvPorcentajeSuelo.setText("--%");
    }

    private void cargarDatosGases(){
        if(gasesListener != null){
            databaseReference.child("mq2").removeEventListener(gasesListener);
        }

        gasesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getView() == null || isDetached()) {
                    return;
                }
                if (snapshot.exists()) {
                    Mq2 ultimosDatos = null;

                    if (snapshot.hasChild("actual")) {
                        ultimosDatos = snapshot.child("actual").getValue(Mq2.class);
                    } else {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ultimosDatos = child.getValue(Mq2.class);
                        }
                    }
                    if (ultimosDatos != null) {
                        datosGases = ultimosDatos;
                        mostrarDatosGases(ultimosDatos);
                    }
                } else {
                    Log.d(TAG, "No hay datos de gases disponibles");
                    mostrarDatosVaciosGases();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(),
                        "Error al cargar datos de gases", Toast.LENGTH_SHORT).show();
                mostrarDatosVaciosGases();
            }
        };
        databaseReference.child("mq2").addValueEventListener(gasesListener);
    }

    public void mostrarDatosGases(Mq2 datos){
        if (!isAdded() || getView() == null || isDetached()) {
            return;
        }

        if (datos != null && datos.getEstado() != null) {
            TextView tvEmoji = getView().findViewById(R.id.tvEmojiGases);
            if (tvEmoji != null) {
                tvEmoji.setText(datos.getEmojiEstado());
            }

            tvGases.setText(datos.getEstadoFormateado());
            tvPorcentaje.setText(datos.getPorcentaje() + " %");
        } else {
            mostrarDatosVaciosGases();
        }
    }

    public void mostrarDatosVaciosGases(){
        if (!isAdded() || getView() == null || isDetached()) {
            return;
        }
        tvGases.setText("Sin datos");
        TextView tvEmoji = getView().findViewById(R.id.tvEmojiGases);
        if (tvEmoji != null) {
            tvEmoji.setText("❓");
        }
        tvPorcentaje.setText("--%");
    }

    private void cargarDatosLuz(){
        if (luzListener != null) {
            databaseReference.child("luz").removeEventListener(luzListener);
        }

        luzListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getView() == null || isDetached()) {
                    return;
                }

                if (snapshot.exists()) {
                    Luz ultimosDatos = null;

                    if (snapshot.hasChild("actual")) {
                        ultimosDatos = snapshot.child("actual").getValue(Luz.class);
                    } else {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ultimosDatos = child.getValue(Luz.class);
                        }
                    }

                    if (ultimosDatos != null) {
                        datosLuz = ultimosDatos;
                        mostrarDatosLuz(ultimosDatos);
                    }
                } else {
                    Log.d(TAG, "No hay datos de luz disponibles");
                    mostrarDatosVaciosLuz();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos de luz: " + error.getMessage());
                Toast.makeText(requireContext(),
                        "Error al cargar datos de luz", Toast.LENGTH_SHORT).show();
                mostrarDatosVaciosLuz();
            }
        };
        databaseReference.child("luz").addValueEventListener(luzListener);
    }

    private void mostrarDatosLuz(Luz datos){
        if (!isAdded() || getView() == null || isDetached()) {
            return;
        }
        if (datos != null && datos.getEstado() != null) {
            TextView tvEmoji = getView().findViewById(R.id.tvEmojiLuz);
            if (tvEmoji != null) {
                tvEmoji.setText(datos.getEmojiEstado());
            }

            tvLuz.setText(datos.getEstadoFormateado());
            tvIluminancia.setText(String.valueOf(datos.getIluminancia()));
        } else {
            mostrarDatosVaciosLuz();
        }
    }

    public void mostrarDatosVaciosLuz(){
        if (!isAdded() || getView() == null || isDetached()) {
            return;
        }
        tvLuz.setText("Sin datos");
        tvIluminancia.setText("Sin datos");
        TextView tvEmoji = getView().findViewById(R.id.tvEmojiLuz);
        if (tvEmoji != null) {
            tvEmoji.setText("❓");
        }
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
                if (!isAdded() || getView() == null || isDetached()) {
                    return;
                }
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
                if (!isAdded() || getView() == null || isDetached()) {
                    return;
                }
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
        if (!isAdded() || getView() == null || isDetached()) {
            return;
        }
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
        if (!isAdded() || getView() == null || isDetached()) {
            return;
        }
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
        if (!isAdded() || getView() == null || isDetached()) {
            return;
        }
        tvTemperatura.setText("-- °C");
        tvHumedad.setText("-- %");
        tvTimestamp.setText("Sin datos disponibles");
    }

    private void mostrarDatosVaciosBmp180() {
        if (!isAdded() || getView() == null || isDetached()) {
            return;
        }
        tvAltitud.setText("-- m");
        tvPresion.setText("-- hPa");
        tvPresionNivelMar.setText("-- hPa");
        tvTimestampBmp.setText("Sin datos disponibles");
    }

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