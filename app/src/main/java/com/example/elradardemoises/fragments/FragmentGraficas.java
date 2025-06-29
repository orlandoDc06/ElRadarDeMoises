package com.example.elradardemoises.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.elradardemoises.R;
import com.example.elradardemoises.models.Bmp180;
import com.example.elradardemoises.models.Dht11;
import com.example.elradardemoises.models.Mq2;
import com.example.elradardemoises.models.Suelo;
import com.example.elradardemoises.models.Viento;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.components.YAxis;
import com.example.elradardemoises.models.LLuvia;
import com.github.mikephil.charting.charts.BarChart;
import com.example.elradardemoises.models.Luz;
public class FragmentGraficas extends Fragment {

    private LineChart lineChart;
    private DatabaseReference databaseRef;
    private Button btnAltitud, btnPresion, btnPresionMar;
    private List<Bmp180> datosActuales;
    private String metricaActual = "altitud";
    private CombinedChart combinedChartDht11;
    private List<Dht11> datosActualesDht11;
    private CombinedChart combinedChartMq2;
    private List<Mq2> datosActualesMq2;
    private CombinedChart combinedChartLuz;
    private List<Luz> datosActualesLuz;
    private CombinedChart combinedChartSuelo;
    private List<Suelo> datosActualesSuelo;
    private LineChart lineChartViento;
    private List<Viento> datosActualesViento;

    public FragmentGraficas() {
    }


    public static FragmentGraficas newInstance(String param1, String param2) {
        FragmentGraficas fragment = new FragmentGraficas();
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

        View view = inflater.inflate(R.layout.fragment_graficas, container, false);

        inicializarVistas(view);
        configurarGrafica();
        cargarDatosAltitud();
        cargarDatosDht11();
        cargarDatosLuz();
        cargarDatosMq2();
        cargarDatosSuelo();
        cargarDatosViento();
        
        return view;
    }

    private void inicializarVistas(View view) {
        //Bmp180
        lineChart = view.findViewById(R.id.lineChart);
        btnAltitud = view.findViewById(R.id.btnAltitud);
        btnPresion = view.findViewById(R.id.btnPresion);
        btnPresionMar = view.findViewById(R.id.btnPresionMar);
        combinedChartDht11 = view.findViewById(R.id.combinedChartDht11);
        databaseRef = FirebaseDatabase.getInstance().getReference();
        combinedChartLuz = view.findViewById(R.id.combinedChartLuz);
        combinedChartMq2 = view.findViewById(R.id.combinedChartMq2);
        combinedChartSuelo = view.findViewById(R.id.combinedChartSuelo);
        lineChartViento = view.findViewById(R.id.lineChartViento);

        configurarGraficaViento();
        configurarGraficaSuelo();
        configurarGraficaMq2();
        configurarGraficaLuz();
        configurarBotones();
        configurarGraficaCombinada();
    }

    private void configurarGrafica() {
        lineChart.getDescription().setEnabled(true);

        Description description = new Description();
        description.setText("Datos últimos 7 días");
        description.setTextSize(14f);
        description.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextPrimary));

        description.setPosition(450f, 30f);
        lineChart.setDescription(description);

        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.glass_card_background));
        lineChart.setExtraOffsets(16f,16f,16f,40f);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setDrawGridLines(false);
    }


    private void configurarBotones() {
        btnAltitud.setOnClickListener(v -> {
            metricaActual = "altitud";
            actualizarSeleccionBoton();
            if (datosActuales != null) {
                actualizarGraficaPorMetrica(datosActuales);
            }
        });

        btnPresion.setOnClickListener(v -> {
            metricaActual = "presion";
            actualizarSeleccionBoton();
            if (datosActuales != null) {
                actualizarGraficaPorMetrica(datosActuales);
            }
        });

        btnPresionMar.setOnClickListener(v -> {
            metricaActual = "presion_mar";
            actualizarSeleccionBoton();
            if (datosActuales != null) {
                actualizarGraficaPorMetrica(datosActuales);
            }
        });

        actualizarSeleccionBoton();
    }

    private void actualizarSeleccionBoton() {
        btnAltitud.setAlpha(0.5f);
        btnPresion.setAlpha(0.5f);
        btnPresionMar.setAlpha(0.5f);

        switch (metricaActual) {
            case "altitud":
                btnAltitud.setAlpha(1.0f);
                break;
            case "presion":
                btnPresion.setAlpha(1.0f);
                break;
            case "presion_mar":
                btnPresionMar.setAlpha(1.0f);
                break;
        }
    }

    private void configurarGraficaCombinada() {

        combinedChartDht11.getDescription().setEnabled(true);

        Description description = new Description();
        description.setText("Datos últimos 7 días");
        description.setTextSize(14f);
        description.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextPrimary));

        description.setPosition(450f, 30f);
        combinedChartDht11.setDescription(description);
        combinedChartDht11.getDescription().setEnabled(true);
        combinedChartDht11.setTouchEnabled(true);
        combinedChartDht11.setDragEnabled(true);
        combinedChartDht11.setScaleEnabled(true);
        combinedChartDht11.setPinchZoom(true);
        combinedChartDht11.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.glass_card_background));


        combinedChartDht11.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.BAR,
                CombinedChart.DrawOrder.LINE
        });

        //eje X
        XAxis xAxis = combinedChartDht11.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "Día " + (int) value;
            }
        });

        //ejes Y
        YAxis leftAxis = combinedChartDht11.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);

        YAxis rightAxis = combinedChartDht11.getAxisRight();
        rightAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        rightAxis.setAxisMinimum(-10f);
        rightAxis.setAxisMaximum(50f);

        xAxis.setDrawGridLines(false);
        leftAxis.setDrawGridLines(false);
        rightAxis.setDrawGridLines(false);
    }

    private void cargarDatosAltitud() {
        databaseRef.child("bmp180")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<Bmp180> datos = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Bmp180 bmp = snapshot.getValue(Bmp180.class);
                            if (bmp != null) {
                                datos.add(bmp);
                            }
                        }
                        datosActuales = datos;
                        actualizarGraficaPorMetrica(datos);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void cargarDatosDht11() {
        databaseRef.child("dht11")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<Dht11> datos = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Dht11 dht = snapshot.getValue(Dht11.class);
                            if (dht != null) {
                                datos.add(dht);
                            }
                        }
                        datosActualesDht11 = datos;
                        actualizarGraficaCombinada(datos);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void actualizarGraficaCombinada(List<Dht11> datos) {
        // Ordenar datos por timestamp
        datos.sort((a, b) -> {
            try {
                long timeA = Long.parseLong(a.getTimestamp());
                long timeB = Long.parseLong(b.getTimestamp());
                return Long.compare(timeA, timeB);
            } catch (Exception e) {
                return 0;
            }
        });

        List<Dht11> ultimosDatos = datos.size() > 7 ?
                datos.subList(datos.size() - 7, datos.size()) :
                datos;

        ArrayList<Entry> temperatureEntries = new ArrayList<>();
        ArrayList<BarEntry> humidityEntries = new ArrayList<>();


        for (int i = 0; i < ultimosDatos.size(); i++) {
            Dht11 dht = ultimosDatos.get(i);
            temperatureEntries.add(new Entry(i + 1, (float) dht.getTemperatura()));
            humidityEntries.add(new BarEntry(i + 1, (float) dht.getHumedad()));
        }

        // Configurar temperatura
        LineDataSet temperatureDataSet = new LineDataSet(temperatureEntries, "Temperatura (°C)");
        temperatureDataSet.setColor(getResources().getColor(R.color.temp_orange));
        temperatureDataSet.setCircleColor(getResources().getColor(R.color.temp_orange));
        
        temperatureDataSet.setLineWidth(3f);
        temperatureDataSet.setCircleRadius(5f);
        temperatureDataSet.setDrawCircleHole(false);
        temperatureDataSet.setValueTextSize(10f);
        temperatureDataSet.setValueTextColor(getResources().getColor(R.color.colorTextPrimary));
        temperatureDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);

        // Configurar humedad
        BarDataSet humidityDataSet = new BarDataSet(humidityEntries, "Humedad (%)");
        humidityDataSet.setColor(getResources().getColor(R.color.colorSurface));
        humidityDataSet.setValueTextSize(10f);
        humidityDataSet.setValueTextColor(getResources().getColor(R.color.colorTextPrimary));
        humidityDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);

        //datos combinados
        CombinedData combinedData = new CombinedData();
        combinedData.setData(new LineData(temperatureDataSet));
        combinedData.setData(new BarData(humidityDataSet));

        //eje X
        XAxis xAxis = combinedChartDht11.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(ultimosDatos.size());

        combinedChartDht11.setData(combinedData);
        combinedChartDht11.invalidate();
    }

    private void actualizarGraficaPorMetrica(List<Bmp180> datos) {

        datos.sort((a, b) -> {
            try {
                long timeA = Long.parseLong(a.getTimestamp());
                long timeB = Long.parseLong(b.getTimestamp());
                return Long.compare(timeA, timeB);
            } catch (Exception e) {
                return 0;
            }
        });

        List<Bmp180> ultimosDatos = datos.size() > 7 ?
                datos.subList(datos.size() - 7, datos.size()) :
                datos;

        ArrayList<Entry> entries = new ArrayList<>();
        String etiqueta = "";
        int color = R.color.verde;

        for (int i = 0; i < ultimosDatos.size(); i++) {
            Bmp180 bmp = ultimosDatos.get(i);
            float valor = 0f;

            switch (metricaActual) {
                case "altitud":
                    valor = bmp.getAltitud().floatValue();
                    etiqueta = "Altitud (m)";
                    color = R.color.verde;
                    lineChart.setExtraOffsets(16f,16f,16f,40f);
                    break;
                case "presion":
                    valor = bmp.getPresion().floatValue();
                    etiqueta = "Presión (Pa)";
                    color = R.color.colorPrimary;
                    lineChart.setExtraOffsets(16f,16f,16f,40f);
                    break;
                case "presion_mar":
                    valor = bmp.getPresion_nivel_mar().floatValue();
                    etiqueta = "Presión N.Mar (Pa)";
                    color = R.color.colorSurface;
                    lineChart.setExtraOffsets(16f,16f,16f,40f);
                    break;
            }

            entries.add(new Entry(i + 1, valor));
        }

        LineDataSet dataSet = new LineDataSet(entries, etiqueta);
        dataSet.setColor(getResources().getColor(color));
        dataSet.setCircleColor(getResources().getColor(color));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(getResources().getColor(R.color.colorTextPrimary));

        // Configurar eje X
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "Día " + (int) value;
            }
        });
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(ultimosDatos.size());

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }


    private void configurarGraficaLuz() {
        combinedChartLuz.getDescription().setEnabled(true);

        Description description = new Description();
        description.setText("Datos últimos 7 días");
        description.setTextSize(14f);
        description.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextPrimary));
        description.setPosition(450f, 30f);
        combinedChartLuz.setDescription(description);

        combinedChartLuz.setTouchEnabled(true);
        combinedChartLuz.setDragEnabled(true);
        combinedChartLuz.setScaleEnabled(true);
        combinedChartLuz.setPinchZoom(true);
        combinedChartLuz.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.glass_card_background));
        combinedChartLuz.setExtraOffsets(16f, 16f, 16f, 40f);

        combinedChartLuz.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.BAR,
                CombinedChart.DrawOrder.LINE
        });

        // eje X
        XAxis xAxis = combinedChartLuz.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "Día " + (int) value;
            }
        });

        YAxis leftAxis = combinedChartLuz.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(5f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                switch ((int) value) {
                    case 1: return "Opaco";
                    case 2: return "Nublado";
                    case 3: return "Soleado";
                    case 4: return "Muy Soleado";
                    default: return "";
                }
            }
        });
        xAxis.setDrawGridLines(false);

        leftAxis.setDrawGridLines(false);
        YAxis rightAxis = combinedChartLuz.getAxisRight();
        rightAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        rightAxis.setAxisMinimum(0f);
        rightAxis.setDrawGridLines(false);
    }

    private void cargarDatosLuz() {
        databaseRef.child("luz")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<Luz> datos = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Luz luz = snapshot.getValue(Luz.class);
                            if (luz != null) {
                                datos.add(luz);
                            }
                        }
                        datosActualesLuz = datos;
                        actualizarGraficaLuz(datos);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void actualizarGraficaLuz(List<Luz> datos) {

        datos.sort((a, b) -> {
            try {
                long timeA = Long.parseLong(a.getTimestamp());
                long timeB = Long.parseLong(b.getTimestamp());
                return Long.compare(timeA, timeB);
            } catch (Exception e) {
                return 0;
            }
        });

        List<Luz> ultimosDatos = datos.size() > 7 ?
                datos.subList(datos.size() - 7, datos.size()) :
                datos;

        ArrayList<Entry> illuminanceEntries = new ArrayList<>();

        ArrayList<BarEntry> stateEntries = new ArrayList<>();
        ArrayList<Integer> barColors = new ArrayList<>();

        double maxIluminancia = 0;
        for (Luz luz : ultimosDatos) {
            if (luz.getIluminancia() > maxIluminancia) {
                maxIluminancia = luz.getIluminancia();
            }
        }

        // eje y
        YAxis rightAxis = combinedChartLuz.getAxisRight();
        rightAxis.setAxisMaximum((float) (maxIluminancia * 1.1));

        for (int i = 0; i < ultimosDatos.size(); i++) {
            Luz luz = ultimosDatos.get(i);

            illuminanceEntries.add(new Entry(i + 1, (float) luz.getIluminancia()));


            String estado = luz.getEstado().toLowerCase().trim();
            float valorEstado = getValorEstadoLuz(estado);
            stateEntries.add(new BarEntry(i + 1, valorEstado));
            barColors.add(getColorEstadoLuz(estado));
        }

        LineDataSet illuminanceDataSet = new LineDataSet(illuminanceEntries, "Iluminancia (lux)");
        illuminanceDataSet.setColor(getResources().getColor(R.color.temp_yellow));
        illuminanceDataSet.setCircleColor(getResources().getColor(R.color.temp_yellow));
        illuminanceDataSet.setLineWidth(3f);
        illuminanceDataSet.setCircleRadius(5f);
        illuminanceDataSet.setDrawCircleHole(false);
        illuminanceDataSet.setValueTextSize(10f);
        illuminanceDataSet.setValueTextColor(getResources().getColor(R.color.colorTextPrimary));
        illuminanceDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);


        BarDataSet stateDataSet = new BarDataSet(stateEntries, "Estado Climático");
        stateDataSet.setColors(barColors);
        stateDataSet.setValueTextSize(10f);
        stateDataSet.setValueTextColor(getResources().getColor(R.color.colorTextPrimary));
        stateDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        stateDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return getEstadoPorValorLuz(value);
            }
        });

        CombinedData combinedData = new CombinedData();
        combinedData.setData(new LineData(illuminanceDataSet));
        combinedData.setData(new BarData(stateDataSet));

        // ancho de barras
        BarData barData = combinedData.getBarData();
        if (barData != null) {
            barData.setBarWidth(0.4f);
        }

        combinedChartLuz.setData(combinedData);
        combinedChartLuz.invalidate();
    }

    private float getValorEstadoLuz(String estado) {
        switch (estado) {
            case "opaco": return 1f;
            case "nublado": return 2f;
            case "soleado": return 3f;
            case "muy soleado": return 4f;
            default: return 1f;
        }
    }

    private int getColorEstadoLuz(String estado) {
        switch (estado) {
            case "opaco": return getResources().getColor(R.color.blue_grey_600);
            case "nublado": return getResources().getColor(R.color.blue_grey_400);
            case "soleado": return getResources().getColor(R.color.orange_500);
            case "muy soleado": return getResources().getColor(R.color.orange_700);
            default: return getResources().getColor(R.color.colorAccent);
        }
    }

    private String getEstadoPorValorLuz(float valor) {
        switch ((int) valor) {
            case 1: return "Opaco";
            case 2: return "Nublado";
            case 3: return "Soleado";
            case 4: return "Muy Soleado";
            default: return "";
        }
    }

    private void configurarGraficaMq2() {
        combinedChartMq2.getDescription().setEnabled(true);

        Description description = new Description();
        description.setText("Datos últimos 7 días");
        description.setTextSize(14f);
        description.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextPrimary));
        description.setPosition(450f, 30f);
        combinedChartMq2.setDescription(description);

        combinedChartMq2.setTouchEnabled(true);
        combinedChartMq2.setDragEnabled(true);
        combinedChartMq2.setScaleEnabled(true);
        combinedChartMq2.setPinchZoom(true);
        combinedChartMq2.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.glass_card_background));
        combinedChartMq2.setExtraOffsets(16f, 16f, 16f, 40f);

        combinedChartMq2.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.BAR,
                CombinedChart.DrawOrder.LINE
        });

        // eje X
        XAxis xAxis = combinedChartMq2.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "Día " + (int) value;
            }
        });

        // Eje Y
        YAxis leftAxis = combinedChartMq2.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(4f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                switch ((int) value) {
                    case 1: return "Limpio";
                    case 2: return "Precaución";
                    case 3: return "Contaminado";
                    default: return "";
                }
            }
        });

        // Eje Y
        YAxis rightAxis = combinedChartMq2.getAxisRight();
        rightAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        rightAxis.setAxisMinimum(0f);
        rightAxis.setAxisMaximum(100f);

        xAxis.setDrawGridLines(false);
        leftAxis.setDrawGridLines(false);
        rightAxis.setDrawGridLines(false);
    }

    private void cargarDatosMq2() {
        databaseRef.child("mq2")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<Mq2> datos = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Mq2 mq2 = snapshot.getValue(Mq2.class);
                            if (mq2 != null) {
                                datos.add(mq2);
                            }
                        }
                        datosActualesMq2 = datos;
                        actualizarGraficaMq2(datos);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void actualizarGraficaMq2(List<Mq2> datos) {
        datos.sort((a, b) -> {
            try {
                long timeA = Long.parseLong(a.getTimestamp());
                long timeB = Long.parseLong(b.getTimestamp());
                return Long.compare(timeA, timeB);
            } catch (Exception e) {
                return 0;
            }
        });

        List<Mq2> ultimosDatos = datos.size() > 7 ?
                datos.subList(datos.size() - 7, datos.size()) :
                datos;

        ArrayList<Entry> porcentajeEntries = new ArrayList<>();
        ArrayList<BarEntry> estadoEntries = new ArrayList<>();
        ArrayList<Integer> barColors = new ArrayList<>();

        for (int i = 0; i < ultimosDatos.size(); i++) {
            Mq2 mq2 = ultimosDatos.get(i);

            porcentajeEntries.add(new Entry(i + 1, (float) mq2.getPorcentaje()));

            String estado = mq2.getEstado().toLowerCase().trim();
            float valorEstado = getValorEstadoMq2(estado);
            estadoEntries.add(new BarEntry(i + 1, valorEstado));
            barColors.add(getColorEstadoMq2(estado));
        }

        LineDataSet porcentajeDataSet = new LineDataSet(porcentajeEntries, "Porcentaje (%)");
        porcentajeDataSet.setColor(getResources().getColor(R.color.temp_orange));
        porcentajeDataSet.setCircleColor(getResources().getColor(R.color.temp_orange));
        porcentajeDataSet.setLineWidth(3f);
        porcentajeDataSet.setCircleRadius(5f);
        porcentajeDataSet.setDrawCircleHole(false);
        porcentajeDataSet.setValueTextSize(10f);
        porcentajeDataSet.setValueTextColor(getResources().getColor(R.color.colorTextPrimary));
        porcentajeDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);

        BarDataSet estadoDataSet = new BarDataSet(estadoEntries, "Estado del Aire");
        estadoDataSet.setColors(barColors);
        estadoDataSet.setValueTextSize(10f);
        estadoDataSet.setValueTextColor(getResources().getColor(R.color.colorTextPrimary));
        estadoDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        estadoDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return getEstadoPorValorMq2(value);
            }
        });

        CombinedData combinedData = new CombinedData();
        combinedData.setData(new LineData(porcentajeDataSet));
        combinedData.setData(new BarData(estadoDataSet));

        //ancho de barras
        BarData barData = combinedData.getBarData();
        if (barData != null) {
            barData.setBarWidth(0.4f);
        }

        combinedChartMq2.setData(combinedData);
        combinedChartMq2.invalidate();
    }

    private float getValorEstadoMq2(String estado) {
        switch (estado) {
            case "limpio": return 1f;
            case "precaucion": return 2f;
            case "contaminado": return 3f;
            default: return 1f;
        }
    }

    private int getColorEstadoMq2(String estado) {
        switch (estado) {
            case "limpio": return getResources().getColor(R.color.verde);
            case "precaucion": return getResources().getColor(R.color.temp_yellow);
            case "contaminado": return getResources().getColor(R.color.temp_red);
            default: return getResources().getColor(R.color.colorDivider);
        }
    }

    private String getEstadoPorValorMq2(float valor) {
        switch ((int) valor) {
            case 1: return "Limpio";
            case 2: return "Precaución";
            case 3: return "Contaminado";
            default: return "";
        }
    }
    private void configurarGraficaSuelo() {
        combinedChartSuelo.getDescription().setEnabled(true);

        Description description = new Description();
        description.setText("Datos últimos 7 días");
        description.setTextSize(14f);
        description.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextPrimary));
        description.setPosition(450f, 30f);
        combinedChartSuelo.setDescription(description);

        combinedChartSuelo.setTouchEnabled(true);
        combinedChartSuelo.setDragEnabled(true);
        combinedChartSuelo.setScaleEnabled(true);
        combinedChartSuelo.setPinchZoom(true);
        combinedChartSuelo.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.glass_card_background));
        combinedChartSuelo.setExtraOffsets(16f, 16f, 16f, 40f);

        combinedChartSuelo.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.BAR,
                CombinedChart.DrawOrder.LINE
        });

        // eje X
        XAxis xAxis = combinedChartSuelo.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "Día " + (int) value;
            }
        });

        // Eje Y
        YAxis leftAxis = combinedChartSuelo.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(3f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                switch ((int) value) {
                    case 1: return "Seco";
                    case 2: return "Húmedo";
                    default: return "";
                }
            }
        });

        // Eje Y
        YAxis rightAxis = combinedChartSuelo.getAxisRight();
        rightAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        rightAxis.setAxisMinimum(0f);
        rightAxis.setAxisMaximum(100f);

        xAxis.setDrawGridLines(false);
        leftAxis.setDrawGridLines(false);
        rightAxis.setDrawGridLines(false);
    }

    private void cargarDatosSuelo() {
        databaseRef.child("suelo")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        List<Suelo> datos = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Suelo suelo = snapshot.getValue(Suelo.class);
                            if (suelo != null) {
                                datos.add(suelo);
                            }
                        }
                        datosActualesSuelo = datos;
                        actualizarGraficaSuelo(datos);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void actualizarGraficaSuelo(List<Suelo> datos) {


        datos.sort((a, b) -> {
            try {
                long timeA = Long.parseLong(a.getTimestamp());
                long timeB = Long.parseLong(b.getTimestamp());
                return Long.compare(timeA, timeB);
            } catch (Exception e) {
                return 0;
            }
        });

        List<Suelo> ultimosDatos = datos.size() > 7 ?
                datos.subList(datos.size() - 7, datos.size()) :
                datos;

        ArrayList<Entry> porcentajeEntries = new ArrayList<>();
        ArrayList<BarEntry> estadoEntries = new ArrayList<>();
        ArrayList<Integer> barColors = new ArrayList<>();

        for (int i = 0; i < ultimosDatos.size(); i++) {
            Suelo suelo = ultimosDatos.get(i);

            porcentajeEntries.add(new Entry(i + 1, (float) suelo.getPorcentaje()));

            String estado = suelo.getEstado().toLowerCase().trim();
            float valorEstado = getValorEstadoSuelo(estado);
            estadoEntries.add(new BarEntry(i + 1, valorEstado));
            barColors.add(getColorEstadoSuelo(estado));
        }


        LineDataSet porcentajeDataSet = new LineDataSet(porcentajeEntries, "Humedad del Suelo (%)");
        porcentajeDataSet.setColor(getResources().getColor(R.color.colorPrimary));
        porcentajeDataSet.setCircleColor(getResources().getColor(R.color.colorPrimary));
        porcentajeDataSet.setLineWidth(3f);
        porcentajeDataSet.setCircleRadius(5f);
        porcentajeDataSet.setDrawCircleHole(false);
        porcentajeDataSet.setValueTextSize(10f);
        porcentajeDataSet.setValueTextColor(getResources().getColor(R.color.colorTextPrimary));
        porcentajeDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);


        BarDataSet estadoDataSet = new BarDataSet(estadoEntries, "Estado del Suelo");
        estadoDataSet.setColors(barColors);
        estadoDataSet.setValueTextSize(10f);
        estadoDataSet.setValueTextColor(getResources().getColor(R.color.colorTextPrimary));
        estadoDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        estadoDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return getEstadoPorValorSuelo(value);
            }
        });


        CombinedData combinedData = new CombinedData();
        combinedData.setData(new LineData(porcentajeDataSet));
        combinedData.setData(new BarData(estadoDataSet));

        // ancho de barras
        BarData barData = combinedData.getBarData();
        if (barData != null) {
            barData.setBarWidth(0.4f);
        }

        combinedChartSuelo.setData(combinedData);
        combinedChartSuelo.invalidate();
    }

    private float getValorEstadoSuelo(String estado) {
        switch (estado) {
            case "seco": return 1f;
            case "humedo": return 2f;
            default: return 1f;
        }
    }

    private int getColorEstadoSuelo(String estado) {
        switch (estado) {
            case "seco": return getResources().getColor(R.color.orange_700);
            case "humedo": return getResources().getColor(R.color.colorPrimary);
            default: return getResources().getColor(R.color.colorAccent);
        }
    }

    private String getEstadoPorValorSuelo(float valor) {
        switch ((int) valor) {
            case 1: return "Seco";
            case 2: return "Húmedo";
            default: return "";
        }
    }

    private void configurarGraficaViento() {
        lineChartViento.getDescription().setEnabled(true);

        Description description = new Description();
        description.setText("Datos últimos 7 días");
        description.setTextSize(14f);
        description.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextPrimary));
        description.setPosition(450f, 30f);
        lineChartViento.setDescription(description);

        lineChartViento.setTouchEnabled(true);
        lineChartViento.setDragEnabled(true);
        lineChartViento.setScaleEnabled(true);
        lineChartViento.setPinchZoom(true);
        lineChartViento.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.glass_card_background));
        lineChartViento.setExtraOffsets(16f, 16f, 16f, 40f);

        // eje X
        XAxis xAxis = lineChartViento.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "Día " + (int) value;
            }
        });

        //ejes Y
        YAxis leftAxis = lineChartViento.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(false);

        YAxis rightAxis = lineChartViento.getAxisRight();
        rightAxis.setDrawGridLines(false);
    }

    private void cargarDatosViento() {
        databaseRef.child("viento")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<Viento> datos = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Viento viento = snapshot.getValue(Viento.class);
                            if (viento != null) {
                                datos.add(viento);
                            }
                        }
                        datosActualesViento = datos;
                        actualizarGraficaViento(datos);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void actualizarGraficaViento(List<Viento> datos) {
        datos.sort((a, b) -> {
            try {
                long timeA = Long.parseLong(a.getTimestamp());
                long timeB = Long.parseLong(b.getTimestamp());
                return Long.compare(timeA, timeB);
            } catch (Exception e) {
                return 0;
            }
        });

        // últimos 7 datos
        List<Viento> ultimosDatos = datos.size() > 7 ?
                datos.subList(datos.size() - 7, datos.size()) :
                datos;

        ArrayList<Entry> velocidadEntries = new ArrayList<>();

        // Crear entradas para la gráfica
        for (int i = 0; i < ultimosDatos.size(); i++) {
            Viento viento = ultimosDatos.get(i);
            velocidadEntries.add(new Entry(i + 1, (float) viento.getVelocidad()));
        }

        LineDataSet velocidadDataSet = new LineDataSet(velocidadEntries, "Velocidad del Viento (Km/h)");
        velocidadDataSet.setColor(getResources().getColor(R.color.colorSurface));
        velocidadDataSet.setCircleColor(getResources().getColor(R.color.colorSurface));
        velocidadDataSet.setLineWidth(3f);
        velocidadDataSet.setCircleRadius(5f);
        velocidadDataSet.setDrawCircleHole(false);
        velocidadDataSet.setValueTextSize(10f);
        velocidadDataSet.setValueTextColor(getResources().getColor(R.color.colorTextPrimary));

        XAxis xAxis = lineChartViento.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(ultimosDatos.size());
        
        LineData lineData = new LineData(velocidadDataSet);
        lineChartViento.setData(lineData);
        lineChartViento.invalidate();
    }

}