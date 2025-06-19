package com.example.elradardemoises.utils;

import android.util.Log;

import com.example.elradardemoises.models.Usuario;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private static final String TAG = "UserManager";

    public static void guardarUsuarioEnBaseDatos(FirebaseUser firebaseUser, DatabaseReference databaseReference) {
        if (firebaseUser == null) {
            Log.e(TAG, "FirebaseUser es null, no se puede guardar");
            return;
        }

        if (databaseReference == null) {
            Log.e(TAG, "DatabaseReference es null, no se puede guardar");
            return;
        }

        // Crear objeto Usuario
        Usuario usuario = crearUsuarioDesdeFirebaseUser(firebaseUser);

        // Guardar en Firebase
        String userId = firebaseUser.getUid();
        databaseReference.child("usuarios").child(userId).setValue(usuario)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Usuario guardado exitosamente: " + usuario.getCorreo());
                    Log.d(TAG, "URL de foto guardada: " + usuario.getPp());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar usuario: " + e.getMessage());
                });
    }

    // Verifica si un usuario existe en la base de datos
    public static void verificarUsuarioExiste(String userId, DatabaseReference databaseReference, ListenerVerificacionUsuario listener) {
        if (userId == null || userId.isEmpty()) {
            listener.alError("ID de usuario inválido");
            return;
        }

        if (databaseReference == null) {
            listener.alError("DatabaseReference es null");
            return;
        }

        databaseReference.child("usuarios").child(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean existe = task.getResult().exists();
                        listener.alResultado(existe);
                    } else {
                        Log.e(TAG, "Error al verificar usuario: " + task.getException());
                        listener.alError("Error al verificar usuario: " + task.getException().getMessage());
                    }
                });
    }

    public static void actualizarDatosBasicosUsuario(FirebaseUser firebaseUser, DatabaseReference databaseReference) {
        if (firebaseUser == null || databaseReference == null) {
            return;
        }

        String userId = firebaseUser.getUid();

        Map<String, Object> actualizaciones = new HashMap<>();

        String nombre = obtenerNombreUsuario(firebaseUser);
        actualizaciones.put("nombre", nombre);

        String fotoPerfil = obtenerUrlFotoMejorada(firebaseUser);
        actualizaciones.put("pp", fotoPerfil);

        if (firebaseUser.getEmail() != null) {
            actualizaciones.put("correo", firebaseUser.getEmail());
        }

        databaseReference.child("usuarios").child(userId).updateChildren(actualizaciones)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Actualizados");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al actualizar : " + e.getMessage());
                });
    }

    public static void guardarUsuarioSiNoExiste(FirebaseUser firebaseUser, DatabaseReference databaseReference, ListenerGuardadoUsuario listener) {
        if (firebaseUser == null) {
            if (listener != null) {
                listener.alError("FirebaseUser es null");
            }
            return;
        }

        String userId = firebaseUser.getUid();

        verificarUsuarioExiste(userId, databaseReference, new ListenerVerificacionUsuario() {
            @Override
            public void alResultado(boolean existe) {
                if (existe) {
                    Log.d(TAG, "Usuario ya existe, actualizando solo datos básicos");
                    actualizarDatosBasicosUsuario(firebaseUser, databaseReference);
                    if (listener != null) {
                        listener.alExitoUsuarioExistente();
                    }
                } else {
                    Log.d(TAG, "Usuario nuevo, guardando completo");
                    guardarUsuarioEnBaseDatos(firebaseUser, databaseReference);
                    if (listener != null) {
                        listener.alExitoUsuarioNuevo();
                    }
                }
            }

            @Override
            public void alError(String error) {
                if (listener != null) {
                    listener.alError(error);
                }
            }
        });
    }

    private static Usuario crearUsuarioDesdeFirebaseUser(FirebaseUser firebaseUser) {
        Usuario usuario = new Usuario();

        usuario.setCorreo(firebaseUser.getEmail());

        String nombre = obtenerNombreUsuario(firebaseUser);
        usuario.setNombre(nombre);

        String fotoPerfil = obtenerUrlFotoMejorada(firebaseUser);
        usuario.setPp(fotoPerfil);

        usuario.setKey(firebaseUser.getUid());

        Log.d(TAG, "Usuario creado con foto: " + fotoPerfil);
        return usuario;
    }

    private static String obtenerNombreUsuario(FirebaseUser firebaseUser) {
        String nombre = firebaseUser.getDisplayName();

        if (nombre == null || nombre.isEmpty()) {
            if (firebaseUser.getEmail() != null) {
                nombre = Usuario.extraerNombreDeCorreo(firebaseUser.getEmail());
            } else {
                nombre = "Usuario";
            }
        }

        return nombre;
    }

    private static String obtenerUrlFotoMejorada(FirebaseUser firebaseUser) {
        if (firebaseUser.getPhotoUrl() != null) {
            String urlOriginal = firebaseUser.getPhotoUrl().toString();
            Log.d(TAG, "URL original de foto en UserManager: " + urlOriginal);

            if (urlOriginal.contains("googleusercontent.com")) {
                String urlMejorada = urlOriginal;

                if (urlMejorada.contains("s96-c")) {
                    urlMejorada = urlMejorada.replace("s96-c", "s400-c");
                }

                if (!urlMejorada.contains("=s")) {
                    if (urlMejorada.contains("?")) {
                        urlMejorada += "&sz=400";
                    } else {
                        urlMejorada += "?sz=400";
                    }
                }

                return urlMejorada;
            }

            return urlOriginal;
        }

        return "";
    }

    public static String obtenerUrlFotoAltaCalidad(FirebaseUser firebaseUser) {
        return obtenerUrlFotoMejorada(firebaseUser);
    }

    // INTERFACES
    public interface ListenerVerificacionUsuario {
        void alResultado(boolean existe);
        void alError(String error);
    }

    public interface ListenerGuardadoUsuario {
        void alExitoUsuarioNuevo();
        void alExitoUsuarioExistente();
        void alError(String error);
    }
}