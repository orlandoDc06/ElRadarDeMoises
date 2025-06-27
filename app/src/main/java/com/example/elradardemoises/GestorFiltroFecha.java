package com.example.elradardemoises;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.elradardemoises.models.Bmp180;
import com.example.elradardemoises.models.Dht11;
import com.example.elradardemoises.models.LLuvia;
import com.example.elradardemoises.models.Mq2;
import com.example.elradardemoises.models.Suelo;
import com.example.elradardemoises.models.Viento;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class GestorFiltroFecha {

    private static final String TAG = "GestorFiltroFecha";

    // Interface para 3
    public interface FiltroFechaListener {
        void onFechaSeleccionada(String fecha);
        void onFiltroLimpiado();
        void onDatosMeteorologicos(Dht11 datos);
        void onDatosBarometricos(Bmp180 datos);
        void onDatosLluvia(LLuvia datos);
        void onDatosViento(Viento datos);
        void onDatosGases(Mq2 datos);
        void onDatosSuelo(Suelo datos);
        void onDatosVaciosMeteorologicos();
        void onDatosVaciosBarometricos();
        void onDatosVaciosLluvia();
        void onDatosVaciosViento();
        void onDatosVaciosGases();
        void onDatosVaciosSuelo();
        void onPdfGenerado(String rutaArchivo);
        void onErrorGenerandoPdf(String error);
        Context getContext();
        DatabaseReference getDatabaseReference();
    }

    // Variables privadas
    private Context context;
    private FiltroFechaListener listener;
    private DatabaseReference databaseReference;

    // Elementos de la interfaz
    private MaterialButton btnFiltrarFecha, btnLimpiarFiltro, btnGenerarPdf;
    private TextView tvFechaSeleccionada;

    // Variables de datos
    private String fechaSeleccionada;
    private SimpleDateFormat formatoFechaMostrar;
    private SimpleDateFormat formatoFechaFirebase;

    // Firebase listeners
    private ValueEventListener weatherListener;
    private ValueEventListener bmpListener;
    private ValueEventListener lluviaListener;
    private ValueEventListener vientoListener;
    private ValueEventListener gasesListener;
    private ValueEventListener sueloListener;

    // Datos actuales para PDF
    private Dht11 datosMeteorologicosActuales;
    private Bmp180 datosBarometricosActuales;
    private LLuvia datosLluviaActuales;
    private Viento datosVientoActuales;
    private Mq2 datosGasesActuales;
    private Suelo datosSueloActuales;

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
        btnGenerarPdf = view.findViewById(R.id.btnGenerarPdf);
        tvFechaSeleccionada = view.findViewById(R.id.tvFechaSeleccionada);
    }

    public void configurarClickListeners() {
        btnFiltrarFecha.setOnClickListener(v -> mostrarSelectorFecha());
        btnLimpiarFiltro.setOnClickListener(v -> limpiarFiltroFecha());
        btnGenerarPdf.setOnClickListener(v -> generarPdfConDatos());
    }

    public String getFechaSeleccionada() {
        return fechaSeleccionada;
    }

    public void cargarDatosPorFecha(String fecha) {
        cargarDatosMeteorologicosPorFecha(fecha);
        cargarDatosBarometricosPorFecha(fecha);
        cargarDatosLluviaPorFecha(fecha);
        cargarDatosVientoPorFecha(fecha);
        cargarDatosGasesPorFecha(fecha);
        cargarDatosSueloPorFecha(fecha);
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
        if (vientoListener != null){
            databaseReference.child("viento").removeEventListener(vientoListener);
        }
        if (gasesListener != null){
            databaseReference.child("mq2").removeEventListener(gasesListener);
        }
        if (sueloListener != null){
            databaseReference.child("suelo").removeEventListener(sueloListener);
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
                    btnGenerarPdf.setVisibility(View.VISIBLE);

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
        btnGenerarPdf.setVisibility(View.GONE);

        // Limpiar datos actuales
        datosMeteorologicosActuales = null;
        datosBarometricosActuales = null;
        datosLluviaActuales = null;

        listener.onFiltroLimpiado();

    }

    private void cargarDatosMeteorologicosPorFecha(String fecha) {
        if (weatherListener != null) {
            databaseReference.child("dht11").removeEventListener(weatherListener);
        }

        String fechaInicio = fecha + " 00:00:00";
        String fechaFin = fecha + " 23:59:59";

        weatherListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Dht11 datosFiltrados = null;

                    for (DataSnapshot child : snapshot.getChildren()) {
                        Dht11 datos = child.getValue(Dht11.class);
                        if (datos != null && datos.getTimestamp() != null) {
                            if (estaEnRangoFecha(datos.getTimestamp(), fechaInicio, fechaFin)) {
                                datosFiltrados = datos;
                            }
                        }
                    }

                    if (datosFiltrados != null) {
                        datosMeteorologicosActuales = datosFiltrados;
                        listener.onDatosMeteorologicos(datosFiltrados);
                    } else {
                        Log.d(TAG, "No hay datos DHT11 para la fecha: " + fecha);
                        datosMeteorologicosActuales = null;
                        listener.onDatosVaciosMeteorologicos();
                    }
                } else {
                    Log.d(TAG, "No hay datos meteorológicos DHT11 disponibles");
                    datosMeteorologicosActuales = null;
                    listener.onDatosVaciosMeteorologicos();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos DHT11 por fecha: " + error.getMessage());
                Toast.makeText(context,
                        "Error al cargar datos DHT11", Toast.LENGTH_SHORT).show();
                datosMeteorologicosActuales = null;
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
                        datosBarometricosActuales = datosFiltrados;
                        listener.onDatosBarometricos(datosFiltrados);
                    } else {
                        Log.d(TAG, "No hay datos BMP180 para la fecha: " + fecha);
                        datosBarometricosActuales = null;
                        listener.onDatosVaciosBarometricos();
                    }
                } else {
                    Log.d(TAG, "No hay datos barométricos BMP180 disponibles");
                    datosBarometricosActuales = null;
                    listener.onDatosVaciosBarometricos();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos BMP180 por fecha: " + error.getMessage());
                Toast.makeText(context,
                        "Error al cargar datos BMP180", Toast.LENGTH_SHORT).show();
                datosBarometricosActuales = null;
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
                        datosLluviaActuales = datosFiltrados;
                        listener.onDatosLluvia(datosFiltrados);
                    } else {
                        Log.d(TAG, "No hay datos de lluvia para la fecha: " + fecha);
                        datosLluviaActuales = null;
                        listener.onDatosVaciosLluvia();
                    }
                } else {
                    Log.d(TAG, "No hay datos de lluvia disponibles");
                    datosLluviaActuales = null;
                    listener.onDatosVaciosLluvia();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos de lluvia por fecha: " + error.getMessage());
                Toast.makeText(context,
                        "Error al cargar datos de lluvia", Toast.LENGTH_SHORT).show();
                datosLluviaActuales = null;
                listener.onDatosVaciosLluvia();
            }
        };

        databaseReference.child("lluvia").addValueEventListener(lluviaListener);
    }

    public void cargarDatosVientoPorFecha(String fecha){
        if (vientoListener != null) {
            databaseReference.child("viento").removeEventListener(vientoListener);
        }

        String fechaInicio = fecha + " 00:00:00";
        String fechaFin = fecha + " 23:59:59";

        vientoListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Viento datosFiltrados = null;

                    for (DataSnapshot child : snapshot.getChildren()) {
                        Viento datos = child.getValue(Viento.class);
                        if (datos != null && datos.getTimestamp() != null) {
                            if (estaEnRangoFecha(datos.getTimestamp(), fechaInicio, fechaFin)) {
                                datosFiltrados = datos;
                            }
                        }
                    }

                    if (datosFiltrados != null) {
                        datosVientoActuales = datosFiltrados;
                        listener.onDatosViento(datosFiltrados);
                    } else {
                        Log.d(TAG, "No hay datos de viento para la fecha: " + fecha);
                        datosVientoActuales = null;
                        listener.onDatosVaciosViento();
                    }
                } else {
                    Log.d(TAG, "No hay datos de viento disponibles");
                    datosVientoActuales = null;
                    listener.onDatosVaciosViento();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos de viento por fecha: " + error.getMessage());
                Toast.makeText(context,
                        "Error al cargar datos de viento", Toast.LENGTH_SHORT).show();
                datosVientoActuales = null;
                listener.onDatosVaciosViento();
            }
        };
        databaseReference.child("viento").addValueEventListener(vientoListener);
    }

    private void cargarDatosGasesPorFecha(String fecha){
        if (gasesListener != null) {
            databaseReference.child("mq2").removeEventListener(gasesListener);
        }

        String fechaInicio = fecha + " 00:00:00";
        String fechaFin = fecha + " 23:59:59";

        gasesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Mq2 datosFiltrados = null;

                    for (DataSnapshot child : snapshot.getChildren()) {
                        Mq2 datos = child.getValue(Mq2.class);
                        if (datos != null && datos.getTimestamp() != null) {
                            if (estaEnRangoFecha(datos.getTimestamp(), fechaInicio, fechaFin)) {
                                datosFiltrados = datos;
                            }
                        }
                    }

                    if (datosFiltrados != null) {
                        datosGasesActuales = datosFiltrados;
                        listener.onDatosGases(datosFiltrados);
                    } else {
                        Log.d(TAG, "No hay datos de gases para la fecha: " + fecha);
                        datosGasesActuales = null;
                        listener.onDatosVaciosGases();
                    }
                } else {
                    Log.d(TAG, "No hay datos de gases disponibles");
                    datosGasesActuales = null;
                    listener.onDatosVaciosGases();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos de gases por fecha: " + error.getMessage());
                Toast.makeText(context,
                        "Error al cargar datos de gases", Toast.LENGTH_SHORT).show();
                datosGasesActuales = null;
                listener.onDatosVaciosGases();
            }
        };
        databaseReference.child("mq2").addValueEventListener(gasesListener);
    }

    public void cargarDatosSueloPorFecha(String fecha){
        if (sueloListener != null) {
            databaseReference.child("suelo").removeEventListener(sueloListener);
        }

        String fechaInicio = fecha + " 00:00:00";
        String fechaFin = fecha + " 23:59:59";

        sueloListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Suelo datosFiltrados = null;

                    for (DataSnapshot child : snapshot.getChildren()) {
                        Suelo datos = child.getValue(Suelo.class);
                        if (datos != null && datos.getTimestamp() != null) {
                            if (estaEnRangoFecha(datos.getTimestamp(), fechaInicio, fechaFin)) {
                                datosFiltrados = datos;
                            }
                        }
                    }

                    if (datosFiltrados != null) {
                        datosSueloActuales = datosFiltrados;
                        listener.onDatosSuelo(datosFiltrados);
                    } else {
                        Log.d(TAG, "No hay datos de suelo para la fecha: " + fecha);
                        datosSueloActuales = null;
                        listener.onDatosVaciosSuelo();
                    }
                } else {
                    Log.d(TAG, "No hay datos de suelo disponibles");
                    datosSueloActuales = null;
                    listener.onDatosVaciosSuelo();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos de suelo por fecha: " + error.getMessage());
                Toast.makeText(context,
                        "Error al cargar datos de suelo", Toast.LENGTH_SHORT).show();
                datosSueloActuales = null;
                listener.onDatosVaciosSuelo();
            }
        };
        databaseReference.child("suelo").addValueEventListener(sueloListener);
    }


    private void generarPdfConDatos() {
        if (fechaSeleccionada == null) {
            Toast.makeText(context, "Primero selecciona una fecha", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int alturaCalculada = calcularAlturaContenido();
            PdfDocument pdfDocument = new PdfDocument();
            int anchoTicket = 204;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(anchoTicket, alturaCalculada, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            Canvas canvas = page.getCanvas();

            Paint titlePaint = new Paint();
            titlePaint.setTextSize(14);
            titlePaint.setFakeBoldText(true);
            titlePaint.setTextAlign(Paint.Align.CENTER);

            Paint headerPaint = new Paint();
            headerPaint.setTextSize(11);
            headerPaint.setFakeBoldText(true);

            Paint textPaint = new Paint();
            textPaint.setTextSize(9);

            Paint separatorPaint = new Paint();
            separatorPaint.setStrokeWidth(1);

            int yPosition = 20;
            int margen = 10;
            int lineHeight = 15;

            // Título
            String fechaMostrar = formatoFechaMostrar.format(formatoFechaFirebase.parse(fechaSeleccionada + " 00:00:00"));
            canvas.drawText("REPORTE METEOROLOGICO", anchoTicket / 2f, yPosition, titlePaint);
            yPosition += 20;

            canvas.drawLine(margen, yPosition, anchoTicket - margen, yPosition, separatorPaint);
            yPosition += 15;

            canvas.drawText("Fecha: " + fechaMostrar, margen, yPosition, headerPaint);
            yPosition += 20;

            canvas.drawLine(margen, yPosition, anchoTicket - margen, yPosition, separatorPaint);
            yPosition += 15;

            // Datos Meteorológicos
            canvas.drawText("DATOS METEOROLOGICOS", margen, yPosition, headerPaint);
            yPosition += lineHeight;
            canvas.drawLine(margen, yPosition, anchoTicket - margen, yPosition, separatorPaint);
            yPosition += 10;

            if (datosMeteorologicosActuales != null) {
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Temperatura: " + datosMeteorologicosActuales.getTemperatura() + "°C", margen, yPosition, anchoTicket - (margen * 2), lineHeight);
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Humedad: " + datosMeteorologicosActuales.getHumedad() + "%", margen, yPosition, anchoTicket - (margen * 2), lineHeight);
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Hora: " + datosMeteorologicosActuales.getTimestampFormateado(), margen, yPosition, anchoTicket - (margen * 2), lineHeight);
            } else {
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Sin datos disponibles", margen, yPosition, anchoTicket - (margen * 2), lineHeight);
            }
            yPosition += 10;

            // Datos Barométricos
            canvas.drawText("DATOS BAROMETRICOS", margen, yPosition, headerPaint);
            yPosition += lineHeight;
            canvas.drawLine(margen, yPosition, anchoTicket - margen, yPosition, separatorPaint);
            yPosition += 10;

            if (datosBarometricosActuales != null) {
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Presion: " + datosBarometricosActuales.getPresion() + " hPa", margen, yPosition, anchoTicket - (margen * 2), lineHeight);
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Altitud: " + datosBarometricosActuales.getAltitud() + " m", margen, yPosition, anchoTicket - (margen * 2), lineHeight);
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Presion Mar: " + datosBarometricosActuales.getPresion_nivel_mar() + " hPa", margen, yPosition, anchoTicket - (margen * 2), lineHeight);
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Hora: " + datosBarometricosActuales.getTimestampFormateado(), margen, yPosition, anchoTicket - (margen * 2), lineHeight);
            } else {
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Sin datos disponibles", margen, yPosition, anchoTicket - (margen * 2), lineHeight);
            }
            yPosition += 10;

            // Datos de Lluvia
            canvas.drawText("DATOS DE PRECIPITACION", margen, yPosition, headerPaint);
            yPosition += lineHeight;
            canvas.drawLine(margen, yPosition, anchoTicket - margen, yPosition, separatorPaint);
            yPosition += 10;

            if (datosLluviaActuales != null) {
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Estado: " + datosLluviaActuales.getEstadoFormateado(), margen, yPosition, anchoTicket - (margen * 2), lineHeight);
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Hora: " + datosLluviaActuales.getTimestampFormateado(), margen, yPosition, anchoTicket - (margen * 2), lineHeight);
            } else {
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Sin datos disponibles", margen, yPosition, anchoTicket - (margen * 2), lineHeight);
            }
            yPosition += 10;

            // Datos Viento
            canvas.drawText("DATOS DE VIENTO", margen, yPosition, headerPaint);
            yPosition += lineHeight;
            canvas.drawLine(margen, yPosition, anchoTicket - margen, yPosition, separatorPaint);
            yPosition += 10;

            if (datosVientoActuales != null) {
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Velocidad: " + datosVientoActuales.getVelocidad(), margen, yPosition, anchoTicket - (margen * 2), lineHeight);
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Hora: " + datosVientoActuales.getTimestampFormateado(), margen, yPosition, anchoTicket - (margen * 2), lineHeight);
            } else {
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Sin datos disponibles", margen, yPosition, anchoTicket - (margen * 2), lineHeight);
            }
            yPosition += 10;

            // Datos Gases
            canvas.drawText("DATOS DE GASES", margen, yPosition, headerPaint);
            yPosition += lineHeight;
            canvas.drawLine(margen, yPosition, anchoTicket - margen, yPosition, separatorPaint);
            yPosition += 10;

            if (datosGasesActuales != null) {
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Estado: " + datosGasesActuales.getEstado(), margen, yPosition, anchoTicket - (margen * 2), lineHeight);
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Porcentaje: " + datosGasesActuales.getPorcentaje() + "%", margen, yPosition, anchoTicket - (margen * 2), lineHeight);
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Hora: " + datosGasesActuales.getTimestampFormateado(), margen, yPosition, anchoTicket - (margen * 2), lineHeight);
            } else {
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Sin datos disponibles", margen, yPosition, anchoTicket - (margen * 2), lineHeight);
            }
            yPosition += 10;

            // Datos Suelo
            canvas.drawText("DATOS DE SUELO", margen, yPosition, headerPaint);
            yPosition += lineHeight;
            canvas.drawLine(margen, yPosition, anchoTicket - margen, yPosition, separatorPaint);
            yPosition += 10;

            if (datosSueloActuales != null) {
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Estado: " + datosSueloActuales.getEstado(), margen, yPosition, anchoTicket - (margen * 2), lineHeight);
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Humedad Suelo: " + datosSueloActuales.getPorcentaje() + "%", margen, yPosition, anchoTicket - (margen * 2), lineHeight);
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Hora: " + datosSueloActuales.getTimestampFormateado(), margen, yPosition, anchoTicket - (margen * 2), lineHeight);
            } else {
                yPosition = dibujarTextoConSalto(canvas, textPaint, "Sin datos disponibles", margen, yPosition, anchoTicket - (margen * 2), lineHeight);
            }
            yPosition += 15;

            // Línea final
            canvas.drawLine(margen, yPosition, anchoTicket - margen, yPosition, separatorPaint);
            yPosition += 15;

            // Footer
            Paint footerPaint = new Paint();
            footerPaint.setTextSize(8);
            footerPaint.setTextAlign(Paint.Align.CENTER);

            String fechaGeneracion = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
            canvas.drawText("Generado: " + fechaGeneracion, anchoTicket / 2f, yPosition, footerPaint);

            pdfDocument.finishPage(page);

            // Guardar
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String nombreArchivo = "reporte_meteorologico_" + fechaSeleccionada + "_" + timestamp + ".pdf";

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File archivo = new File(downloadsDir, nombreArchivo);

            FileOutputStream fos = new FileOutputStream(archivo);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();

            listener.onPdfGenerado(archivo.getAbsolutePath());
            Toast.makeText(context, "PDF generado: " + nombreArchivo, Toast.LENGTH_LONG).show();

        } catch (IOException | ParseException e) {
            Log.e(TAG, "Error al generar PDF: " + e.getMessage());
            listener.onErrorGenerandoPdf("Error al generar PDF: " + e.getMessage());
            Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private int calcularAlturaContenido() {
        int alturaBase = 100;
        int alturaSeccion = 60;
        int numSecciones = 5;


        int alturaAdicional = 0;
        if (datosMeteorologicosActuales != null) alturaAdicional += 45;
        if (datosBarometricosActuales != null) alturaAdicional += 60;
        if (datosLluviaActuales != null) alturaAdicional += 30;
        if (datosVientoActuales != null) alturaAdicional += 30;
        if (datosGasesActuales != null) alturaAdicional += 45;
        if (datosSueloActuales != null) alturaAdicional += 45;

        return alturaBase + (numSecciones * alturaSeccion) + alturaAdicional;
    }

    private int dibujarTextoConSalto(Canvas canvas, Paint paint, String texto, int x, int y, int anchoMaximo, int alturaLinea) {

        float anchoTexto = paint.measureText(texto);

        if (anchoTexto <= anchoMaximo) {
            canvas.drawText(texto, x, y, paint);
            return y + alturaLinea;
        } else {
            String[] palabras = texto.split(" ");
            StringBuilder lineaActual = new StringBuilder();
            int yActual = y;

            for (String palabra : palabras) {
                String lineaTest = lineaActual.length() > 0 ? lineaActual + " " + palabra : palabra;
                float anchoTest = paint.measureText(lineaTest);

                if (anchoTest <= anchoMaximo) {
                    lineaActual.append(lineaActual.length() > 0 ? " " + palabra : palabra);
                } else {
                    if (lineaActual.length() > 0) {
                        canvas.drawText(lineaActual.toString(), x, yActual, paint);
                        yActual += alturaLinea;
                    }
                    lineaActual = new StringBuilder(palabra);
                }
            }

            if (lineaActual.length() > 0) {
                canvas.drawText(lineaActual.toString(), x, yActual, paint);
                yActual += alturaLinea;
            }

            return yActual;
        }
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