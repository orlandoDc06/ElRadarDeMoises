package com.example.elradardemoises.models;

import com.example.elradardemoises.R;

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



    public int getBackgroundAnimado() {
        if (estado == null || estado.isEmpty()) {
            return android.R.drawable.screen_background_dark;
        }

        String estadoLower = estado.toLowerCase().trim();

        switch (estadoLower) {
            case "seco":
                return R.drawable.bg_sunny_animated;
            case "aguacero":
                return R.drawable.bg_rain_animated;
            case "lluvia":
                return R.drawable.bg_rain_animated;
            case "llovizna":
                return R.drawable.bg_rain_animated;
            case "tormenta":
                return R.drawable.bg_storm_animated;
            default:
                return R.drawable.bg_sunny_animated;
        }
    }

    public String getBackgroundAnimatedName() {
        if (estado == null || estado.isEmpty()) {
            return "bg_cloudy_animated";
        }

        String estadoLower = estado.toLowerCase().trim();

        switch (estadoLower) {
            case "seco":
                return "bg_sunny_animated";
            case "aguacero":
                return "bg_heavy_rain_animated";
            case "lluvia":
                return "bg_rain_animated";
            case "llovizna":
                return "bg_drizzle_animated";
            case "tormenta":
                return "bg_storm_animated";
            default:
                return "bg_cloudy_animated";
        }
    }

    public int getBackgroundAnimado(android.content.Context context) {
        String drawableName = getBackgroundAnimatedName();
        int resourceId = context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());
        return resourceId != 0 ? resourceId : android.R.drawable.screen_background_dark;
    }

    public int[] getBackgroundGradientColors() {
        if (estado == null || estado.isEmpty()) {
            return new int[]{0xFF6B7280, 0xFF374151};
        }

        String estadoLower = estado.toLowerCase().trim();

        switch (estadoLower) {
            case "seco":
                return new int[]{0xFF87CEEB, 0xFFFFD700, 0xFFFFA500};
            case "aguacero":
                return new int[]{0xFF2F4F4F, 0xFF4682B4, 0xFF191970};
            case "lluvia":
                return new int[]{0xFF708090, 0xFF4682B4, 0xFF6495ED};
            case "llovizna":
                return new int[]{0xFFB0C4DE, 0xFF87CEEB, 0xFFADD8E6};
            case "tormenta":
                return new int[]{0xFF2F2F2F, 0xFF4B0082, 0xFF191970};
            default:
                return new int[]{0xFF778899, 0xFF696969, 0xFF808080};
        }
    }

    public long getAnimationDuration() {
        if (estado == null || estado.isEmpty()) {
            return 3000;
        }

        String estadoLower = estado.toLowerCase().trim();

        switch (estadoLower) {
            case "seco":
                return 5000;
            case "aguacero":
                return 1000;
            case "lluvia":
                return 2000;
            case "llovizna":
                return 4000;
            case "tormenta":
                return 800;
            default:
                return 3000;
        }
    }


    public String getParticleType() {
        if (estado == null || estado.isEmpty()) {
            return "none";
        }

        String estadoLower = estado.toLowerCase().trim();

        switch (estadoLower) {
            case "seco":
                return "sun_rays";
            case "aguacero":
                return "heavy_rain_drops";
            case "lluvia":
                return "rain_drops";
            case "llovizna":
                return "clouds";
            case "tormenta":
                return "lightning_rain";
            default:
                return "sun_rays";
        }
    }


    public boolean shouldShowParticles() {
        //return !estado.toLowerCase().equals("seco") || estado.isEmpty();
        return true;
    }


    public float getOverlayOpacity() {
        if (estado == null || estado.isEmpty()) {
            return 0.3f;
        }

        String estadoLower = estado.toLowerCase().trim();

        switch (estadoLower) {
            case "seco":
                return 0.1f;
            case "aguacero":
                return 0.7f;
            case "lluvia":
                return 0.4f;
            case "llovizna":
                return 0.2f;
            case "tormenta":
                return 0.8f;
            default:
                return 0.3f;
        }
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