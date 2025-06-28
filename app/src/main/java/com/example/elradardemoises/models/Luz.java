package com.example.elradardemoises.models;

public class Luz {
    private String estado;
    private double iluminancia;
    private String timestamp;

    public Luz() {
    }

    public Luz(String estado, double iluminancia, String timestamp) {
        this.estado = estado;
        this.iluminancia = iluminancia;
        this.timestamp = timestamp;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public double getIluminancia() {
        return iluminancia;
    }

    public void setIluminancia(double iluminancia) {
        this.iluminancia = iluminancia;
    }

    public String getTimestamp() {
        return timestamp;
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
            case "Nublado":
                return "Nublado";
            case "Opaco":
                return "Opaco";
            case "Soleado":
                return "Soleado";
            case "Muy soleado":
                return "Muy Soleado";
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
            case "Nublado":
                return "☁️";
            case "Opaco":
                return "🌧️";
            case "Soleado":
                return "☀️";
            case "Muy soleado":
                return "☀️";
            default:
                return "💧";
        }
    }
}
