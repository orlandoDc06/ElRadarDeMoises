package com.example.elradardemoises.models;

public class Viento {
    private double velocidad;
    private String timestamp;

    public Viento() {
    }

    public Viento(double velocidad, String timestamp) {
        this.velocidad = velocidad;
        this.timestamp = timestamp;
    }

    public double getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(double velocidad) {
        this.velocidad = velocidad;
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
}
