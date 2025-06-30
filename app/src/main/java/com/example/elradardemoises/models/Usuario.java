package com.example.elradardemoises.models;

import java.io.Serializable;

public class Usuario implements Serializable {
    private String correo;
    private String nombre;
    private String pp; //Profile Photo(Foto de perfil)
    private String key;

    public Usuario() {
    }

    public Usuario(String correo, String nombre, String pp, String key) {
        this.correo = correo;
        this.nombre = nombre;
        this.pp = pp;
        this.key = key;
    }


    public static String extraerNombreDeCorreo(String correo) {
        if (correo == null || correo.isEmpty()) {
            return "Usuario";
        }

        // Extraer la parte antes del @
        String parteAntes = correo.split("@")[0];

        // Reemplazar puntos y guiones bajos con espacios
        String nombreLimpio = parteAntes.replace(".", " ")
                .replace("_", " ")
                .replace("-", " ");

        // Capitalizar primera letra de cada palabra
        StringBuilder nombreCapitalizado = new StringBuilder();
        String[] palabras = nombreLimpio.split(" ");

        for (String palabra : palabras) {
            if (!palabra.isEmpty()) {
                if (nombreCapitalizado.length() > 0) {
                    nombreCapitalizado.append(" ");
                }
                nombreCapitalizado.append(palabra.substring(0, 1).toUpperCase())
                        .append(palabra.substring(1).toLowerCase());
            }
        }

        return nombreCapitalizado.length() > 0 ? nombreCapitalizado.toString() : "Usuario";
    }

    // Getters y Setters
    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPp() {
        return pp;
    }

    public void setPp(String pp) {
        this.pp = pp;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "correo='" + correo + '\'' +
                ", nombre='" + nombre + '\'' +
                ", pp='" + pp + '\'' +
                ", key='" + key + '\'' +
                '}';
    }
}