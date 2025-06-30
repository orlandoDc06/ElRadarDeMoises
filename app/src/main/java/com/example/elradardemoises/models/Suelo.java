package com.example.elradardemoises.models;

public class Suelo {
    private String estado;
    private int porcentaje;
    private String timestamp;

    public Suelo() {
    }

    public Suelo(String estado, int porcentaje, String timestamp) {
        this.estado = estado;
        this.porcentaje = porcentaje;
        this.timestamp = timestamp;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(int porcentaje) {
        this.porcentaje = porcentaje;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getEstadoFormateado() {
        if (estado == null || estado.isEmpty()) {
            return "Sin datos";
        }

        String estadoLower = estado.toLowerCase().trim();

        switch (estadoLower) {
            case "seco":
                return "Seco";
            case "precaucion":
                return "Precaucion";
            case "contaminado":
                return "Contaminado";
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
                return "🏜️";
            case "humedo":
                return "💧";
            default:
                return "❓";
        }
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
}
