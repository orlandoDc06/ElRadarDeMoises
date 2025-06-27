package com.example.elradardemoises;

import android.app.DatePickerDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.elradardemoises.models.Bmp180;
import com.example.elradardemoises.models.Dht11;
import com.example.elradardemoises.models.LLuvia;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class GestorFiltroFecha {

    private static final String TAG = "GestorFiltroFecha";

    // Interface para callbacks
    public interface FiltroFechaListener {
        void onFechaSeleccionada(String fecha);
        void onFiltroLimpiado();
        void onDatosMeteorologicos(Dht11 datos);
        void onDatosBarometricos(Bmp180 datos);
        void onDatosLluvia(LLuvia datos);
        void onDatosVaciosMeteorologicos();
        void onDatosVaciosBarometricos();
        void onDatosVaciosLluvia();
        Context getContext();
        DatabaseReference getDatabaseReference();
    }

    // Variables privadas
    private Context context;
    private FiltroFechaListener listener;
    private DatabaseReference databaseReference;

    // Elementos de la interfaz
    private MaterialButton btnFiltrarFecha, btnLimpiarFiltro;
    private TextView tvFechaSeleccionada;

    // Variables de datos
    private String fechaSeleccionada;
    private SimpleDateFormat formatoFechaMostrar;
    private SimpleDateFormat formatoFechaFirebase;

    // Firebase listeners
    private ValueEventListener weatherListener;
    private ValueEventListener bmpListener;
    private ValueEventListener lluviaListener;

    // Constructor
    public GestorFiltroFecha(FiltroFechaListener listener) {
        this.listener = listener;
        this.context = listener.getContext();
        this.databaseReference = listener.getDatabaseReference();

        // Inicializar formatos de fecha
        formatoFechaMostrar = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        formatoFechaFirebase = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    public void inicializarVistas(View view) {
        btnFiltrarFecha = view.findViewById(R.id.btnFiltrarFecha);
        btnLimpiarFiltro = view.findViewById(R.id.btnLimpiarFiltro);
        tvFechaSeleccionada = view.findViewById(R.id.tvFechaSeleccionada);
    }

    public void configurarClickListeners() {
        btnFiltrarFecha.setOnClickListener(v -> mostrarSelectorFecha());
        btnLimpiarFiltro.setOnClickListener(v -> limpiarFiltroFecha());
    }

    public String getFechaSeleccionada() {
        return fechaSeleccionada;
    }

    public void cargarDatosPorFecha(String fecha) {
        cargarDatosMeteorologicosPorFecha(fecha);
        cargarDatosBarometricosPorFecha(fecha);
        cargarDatosLluviaPorFecha(fecha);
    }

    public void limpiarListeners() {
        if (weatherListener != null) {
            databaseReference.child("dht11").removeEventListener(weatherListener);
        }
        if (bmpListener != null) {
            databaseReference.child("bmp180").removeEventListener(bmpListener);
        }
        if (lluviaListener != null) {
            databaseReference.child("lluvia").removeEventListener(lluviaListener);
        }
    }


    private void mostrarSelectorFecha() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                (view, year, month, dayOfMonth) -> {
                    Calendar fechaSeleccionadaCal = Calendar.getInstance();
                    fechaSeleccionadaCal.set(year, month, dayOfMonth);

                    fechaSeleccionada = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                            year, month + 1, dayOfMonth);

                    String fechaMostrar = formatoFechaMostrar.format(fechaSeleccionadaCal.getTime());
                    tvFechaSeleccionada.setText(fechaMostrar);
                    tvFechaSeleccionada.setVisibility(View.VISIBLE);
                    btnLimpiarFiltro.setVisibility(View.VISIBLE);

                    cargarDatosPorFecha(fechaSeleccionada);
                    listener.onFechaSeleccionada(fechaSeleccionada);

                    Toast.makeText(context,
                            "Mostrando datos del " + fechaMostrar, Toast.LENGTH_SHORT).show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }


    private void limpiarFiltroFecha() {
        fechaSeleccionada = null;
        tvFechaSeleccionada.setVisibility(View.GONE);
        btnLimpiarFiltro.setVisibility(View.GONE);

        listener.onFiltroLimpiado();

        Toast.makeText(context, "Filtro de fecha eliminado", Toast.LENGTH_SHORT).show();
    }

    private void cargarDatosMeteorologicosPorFecha(String fecha) {
        if (weatherListener != null) {
            databaseReference.child("dht11").removeEventListener(weatherListener);
        }

        // Crear el rango de fechas para el día completo
        String fechaInicio = fecha + " 00:00:00";
        String fechaFin = fecha + " 23:59:59";

        weatherListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Dht11 datosFiltrados = null;

                    // Buscar datos dentro del rango de fecha
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Dht11 datos = child.getValue(Dht11.class);
                        if (datos != null && datos.getTimestamp() != null) {
                            if (estaEnRangoFecha(datos.getTimestamp(), fechaInicio, fechaFin)) {
                                datosFiltrados = datos; // Tomar el último del día
                            }
                        }
                    }

                    if (datosFiltrados != null) {
                        listener.onDatosMeteorologicos(datosFiltrados);
                    } else {
                        Log.d(TAG, "No hay datos DHT11 para la fecha: " + fecha);
                        listener.onDatosVaciosMeteorologicos();
                    }
                } else {
                    Log.d(TAG, "No hay datos meteorológicos DHT11 disponibles");
                    listener.onDatosVaciosMeteorologicos();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos DHT11 por fecha: " + error.getMessage());
                Toast.makeText(context,
                        "Error al cargar datos DHT11", Toast.LENGTH_SHORT).show();
                listener.onDatosVaciosMeteorologicos();
            }
        };

        databaseReference.child("dht11").addValueEventListener(weatherListener);
    }

    private void cargarDatosBarometricosPorFecha(String fecha) {
        if (bmpListener != null) {
            databaseReference.child("bmp180").removeEventListener(bmpListener);
        }

        String fechaInicio = fecha + " 00:00:00";
        String fechaFin = fecha + " 23:59:59";

        bmpListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Bmp180 datosFiltrados = null;

                    for (DataSnapshot child : snapshot.getChildren()) {
                        Bmp180 datos = child.getValue(Bmp180.class);
                        if (datos != null && datos.getTimestamp() != null) {
                            if (estaEnRangoFecha(datos.getTimestamp(), fechaInicio, fechaFin)) {
                                datosFiltrados = datos;
                            }
                        }
                    }

                    if (datosFiltrados != null) {
                        listener.onDatosBarometricos(datosFiltrados);
                    } else {
                        Log.d(TAG, "No hay datos BMP180 para la fecha: " + fecha);
                        listener.onDatosVaciosBarometricos();
                    }
                } else {
                    Log.d(TAG, "No hay datos barométricos BMP180 disponibles");
                    listener.onDatosVaciosBarometricos();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos BMP180 por fecha: " + error.getMessage());
                Toast.makeText(context,
                        "Error al cargar datos BMP180", Toast.LENGTH_SHORT).show();
                listener.onDatosVaciosBarometricos();
            }
        };

        databaseReference.child("bmp180").addValueEventListener(bmpListener);
    }

    private void cargarDatosLluviaPorFecha(String fecha) {
        if (lluviaListener != null) {
            databaseReference.child("lluvia").removeEventListener(lluviaListener);
        }

        String fechaInicio = fecha + " 00:00:00";
        String fechaFin = fecha + " 23:59:59";

        lluviaListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    LLuvia datosFiltrados = null;

                    for (DataSnapshot child : snapshot.getChildren()) {
                        LLuvia datos = child.getValue(LLuvia.class);
                        if (datos != null && datos.getTimestamp() != null) {
                            if (estaEnRangoFecha(datos.getTimestamp(), fechaInicio, fechaFin)) {
                                datosFiltrados = datos;
                            }
                        }
                    }

                    if (datosFiltrados != null) {
                        listener.onDatosLluvia(datosFiltrados);
                    } else {
                        Log.d(TAG, "No hay datos de lluvia para la fecha: " + fecha);
                        listener.onDatosVaciosLluvia();
                    }
                } else {
                    Log.d(TAG, "No hay datos de lluvia disponibles");
                    listener.onDatosVaciosLluvia();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos de lluvia por fecha: " + error.getMessage());
                Toast.makeText(context,
                        "Error al cargar datos de lluvia", Toast.LENGTH_SHORT).show();
                listener.onDatosVaciosLluvia();
            }
        };

        databaseReference.child("lluvia").addValueEventListener(lluviaListener);
    }

    private boolean estaEnRangoFecha(String timestamp, String fechaInicio, String fechaFin) {
        try {
            Date fechaTimestamp = formatoFechaFirebase.parse(timestamp);
            Date fechaInicioDate = formatoFechaFirebase.parse(fechaInicio);
            Date fechaFinDate = formatoFechaFirebase.parse(fechaFin);

            return fechaTimestamp != null && fechaInicioDate != null && fechaFinDate != null &&
                    fechaTimestamp.compareTo(fechaInicioDate) >= 0 &&
                    fechaTimestamp.compareTo(fechaFinDate) <= 0;
        } catch (ParseException e) {
            Log.e(TAG, "Error al parsear fechas: " + e.getMessage());
            return false;
        }
    }
}