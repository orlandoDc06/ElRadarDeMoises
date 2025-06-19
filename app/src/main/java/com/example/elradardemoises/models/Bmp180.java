package com.example.elradardemoises.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Bmp180 {
    private Double altitud;
    private Double presion;
    private Double presion_nivel_mar;
    private String timestamp;

    public Bmp180() {
    }

    public Bmp180(Double altitud, Double presion, Double presion_nivel_mar, String timestamp) {
        this.altitud = altitud;
        this.presion = presion;
        this.presion_nivel_mar = presion_nivel_mar;
        this.timestamp = timestamp;
    }


    public Double getAltitud() {
        return altitud != null ? altitud : 0.0;
    }

    public Double getPresion() {
        return presion != null ? presion : 0.0;
    }

    public Double getPresion_nivel_mar() {
        return presion_nivel_mar != null ? presion_nivel_mar : 0.0;
    }

    public String getTimestamp() {
        return timestamp;
    }


    public void setAltitud(Double altitud) {
        this.altitud = altitud;
    }

    public void setPresion(Double presion) {
        this.presion = presion;
    }

    public void setPresion_nivel_mar(Double presion_nivel_mar) {
        this.presion_nivel_mar = presion_nivel_mar;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }


    public String getTimestampFormateado() {
        try {
            if (timestamp != null && !timestamp.isEmpty()) {
                if (timestamp.matches("\\d+")) {
                    long time = Long.parseLong(timestamp);
                    Date date = new Date(time);
                    return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date);
                }
                return timestamp;
            }
            return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        } catch (Exception e) {
            return timestamp != null ? timestamp : "Fecha no disponible";
        }
    }

    @Override
    public String toString() {
        return "Bmp180{" +
                "altitud=" + altitud +
                ", presion=" + presion +
                ", presion_nivel_mar=" + presion_nivel_mar +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}