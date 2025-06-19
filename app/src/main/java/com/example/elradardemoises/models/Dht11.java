package com.example.elradardemoises.models;

public class Dht11 {
    private int humedad;
    private double temperatura;
    private String timestamp;

    public Dht11() {
    }

    public Dht11(int humedad, double temperatura, String timestamp) {
        this.humedad = humedad;
        this.temperatura = temperatura;
        this.timestamp = timestamp;
    }

    public int getHumedad() {
        return humedad;
    }

    public void setHumedad(int humedad) {
        this.humedad = humedad;
    }

    public double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(double temperatura) {
        this.temperatura = temperatura;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getTemperaturaString() {
        return String.valueOf(temperatura);
    }

    public String getHumedadString() {
        return String.valueOf(humedad);
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

    @Override
    public String toString() {
        return "Dht11{" +
                "humedad=" + humedad +
                ", temperatura=" + temperatura +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}