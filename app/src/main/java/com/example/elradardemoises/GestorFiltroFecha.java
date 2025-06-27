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

    // Datos actuales para PDF
    private Dht11 datosMeteorologicosActuales;
    private Bmp180 datosBarometricosActuales;
    private LLuvia datosLluviaActuales;

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

    private File generarArchivoUnico(String fechaSeleccionada) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String nombreBase = "reporte_meteorologico_" + fechaSeleccionada;
        String nombreArchivo = nombreBase + ".pdf";
        File archivo = new File(downloadsDir, nombreArchivo);

        int contador = 1;
        while (archivo.exists()) {
            nombreArchivo = nombreBase + "_(" + contador + ").pdf";
            archivo = new File(downloadsDir, nombreArchivo);
            contador++;
        }

        return archivo;
    }

    private void generarPdfConDatos() {
        if (fechaSeleccionada == null) {
            Toast.makeText(context, "Primero selecciona una fecha", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Crear documento PDF
            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();

            // estilos
            Paint titlePaint = new Paint();
            titlePaint.setTextSize(24);
            titlePaint.setFakeBoldText(true);

            Paint headerPaint = new Paint();
            headerPaint.setTextSize(18);
            headerPaint.setFakeBoldText(true);

            Paint textPaint = new Paint();
            textPaint.setTextSize(14);

            int yPosition = 80;

            // Título
            String fechaMostrar = formatoFechaMostrar.format(formatoFechaFirebase.parse(fechaSeleccionada + " 00:00:00"));
            canvas.drawText("Reporte Meteorológico", 50, yPosition, titlePaint);
            yPosition += 40;
            canvas.drawText("Fecha: " + fechaMostrar, 50, yPosition, headerPaint);
            yPosition += 60;

            // Datos Meteorológicos
            canvas.drawText("DATOS METEOROLÓGICOS", 50, yPosition, headerPaint);
            yPosition += 30;

            if (datosMeteorologicosActuales != null) {
                canvas.drawText("• Temperatura: " + datosMeteorologicosActuales.getTemperatura() + "°C", 70, yPosition, textPaint);
                yPosition += 25;
                canvas.drawText("• Humedad: " + datosMeteorologicosActuales.getHumedad() + "%", 70, yPosition, textPaint);
                yPosition += 25;
                canvas.drawText("• Timestamp: " + datosMeteorologicosActuales.getTimestampFormateado(), 70, yPosition, textPaint);
                yPosition += 25;
            } else {
                canvas.drawText("• No hay datos disponibles para esta fecha", 70, yPosition, textPaint);
                yPosition += 25;
            }

            yPosition += 30;

            // Datos Barométricos
            canvas.drawText("DATOS BAROMÉTRICOS", 50, yPosition, headerPaint);
            yPosition += 30;

            if (datosBarometricosActuales != null) {
                canvas.drawText("• Presión: " + datosBarometricosActuales.getPresion() + " hPa", 70, yPosition, textPaint);
                yPosition += 25;
                canvas.drawText("• Altitud: " + datosBarometricosActuales.getAltitud() + " m", 70, yPosition, textPaint);
                yPosition += 25;
                canvas.drawText("• Presion Mar: " + datosBarometricosActuales.getPresion_nivel_mar() + " hPa", 70, yPosition, textPaint);
                yPosition += 25;
                canvas.drawText("• Timestamp: " + datosBarometricosActuales.getTimestampFormateado(), 70, yPosition, textPaint);
                yPosition += 25;
            } else {
                canvas.drawText("• No hay datos disponibles para esta fecha", 70, yPosition, textPaint);
                yPosition += 25;
            }

            yPosition += 30;

            // Datos de Lluvia
            canvas.drawText("DATOS DE PRECIPITACIÓN", 50, yPosition, headerPaint);
            yPosition += 30;

            if (datosLluviaActuales != null) {
                canvas.drawText("• Estado: " + datosLluviaActuales.getEstadoFormateado(), 70, yPosition, textPaint);
                yPosition += 25;
                canvas.drawText("• Timestamp: " + datosLluviaActuales.getTimestampFormateado(), 70, yPosition, textPaint);
                yPosition += 25;
            } else {
                canvas.drawText("• No hay datos disponibles para esta fecha", 70, yPosition, textPaint);
                yPosition += 25;
            }

            yPosition += 50;
            canvas.drawText("Reporte generado el: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()), 50, yPosition, textPaint);

            pdfDocument.finishPage(page);

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(new Date());
            String nombreArchivo = "reporte_meteorologico_" + fechaSeleccionada + "_" + timestamp + ".pdf";

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File archivo = new File(downloadsDir, nombreArchivo);

            FileOutputStream fos = new FileOutputStream(archivo);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();

            listener.onPdfGenerado(archivo.getAbsolutePath());
            Toast.makeText(context, "PDF generado en Descargas: " + nombreArchivo, Toast.LENGTH_LONG).show();

        } catch (IOException | ParseException e) {
            Log.e(TAG, "Error al generar PDF: " + e.getMessage());
            listener.onErrorGenerandoPdf("Error al generar PDF: " + e.getMessage());
            Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show();
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