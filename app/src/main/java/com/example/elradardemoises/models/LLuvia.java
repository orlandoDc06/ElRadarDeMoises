package com.example.elradardemoises.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LLuvia {
    private String estado;
    private String timestamp;

    public LLuvia() {
    }

    public LLuvia(String estado, String timestamp) {
        this.estado = estado;
        this.timestamp = timestamp;
    }

    public String getEstado() {
        return estado;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }


    public String getTimestampFormateado() {
        if (timestamp == null || timestamp.isEmpty()) {
            return "Sin fecha";
        }

        try {
            if (timestamp.matches("\\d+")) {
                long time = Long.parseLong(timestamp);
                java.util.Date date = new java.util.Date(time);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                return sdf.format(date);
            }
            return timestamp;
        } catch (Exception e) {
            return timestamp;
        }
    }


    public String getEstadoFormateado() {
        if (estado == null || estado.isEmpty()) {
            return "Sin datos";
        }

        String estadoLower = estado.toLowerCase().trim();

        switch (estadoLower) {
            case "seco":
                return "Soleado";
            case "aguacero":
                return "Diluvio";
            case "lluvia":
                return "Lluvia";
            case "llovizna":
                return "Llovizna";
            case "tormenta":
                return "Tormenta";
            default:
                return estado.substring(0, 1).toUpperCase() + estado.substring(1).toLowerCase();
        }
    }

    public String getEmojiEstado() {
        if (estado == null || estado.isEmpty()) {
            return "❓";
        }

        String estadoLower = estado.toLowerCase().trim();

        switch (estadoLower) {
            case "seco":
                return "☀️";
            case "aguacero":
                return "🌧️";
            case "lluvia":
                return "🌦️";
            case "llovizna":
                return "🌦️";
            case "tormenta":
                return "⛈️";
            case "nublado":
                return "☁️";
            default:
                return "💧";
        }
    }



    @Override
    public String toString() {
        return "LLuvia{" +
                "estado='" + estado + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        LLuvia lluvia = (LLuvia) obj;

        if (estado != null ? !estado.equals(lluvia.estado) : lluvia.estado != null) return false;
        return timestamp != null ? timestamp.equals(lluvia.timestamp) : lluvia.timestamp == null;
    }


    @Override
    public int hashCode() {
        int result = estado != null ? estado.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }
}