package com.example.elradardemoises.utils;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.Executors;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private static final String SERVER_CLIENT_ID = "842542493069-limvbf9v9o0f0s65popi8tcr3dj4er56.apps.googleusercontent.com";

    private Context context;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private CredentialManager credentialManager;
    private CallbackAutenticacion authCallback;
    private Handler mainHandler;

    public interface CallbackAutenticacion {
        void alExitoAutenticacion(FirebaseUser user);
        void alErrorAutenticacion(String error);
        void alEnviarVerificacionEmail();
        void alCancelarAutenticacion();
    }

    public AuthManager(Context context, CallbackAutenticacion callback) {
        this.context = context;
        this.authCallback = callback;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
        this.credentialManager = CredentialManager.create(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void registrarConEmailYPassword(String email, String password) {
        if (email.isEmpty() || email.isBlank() || password.isBlank() || password.isEmpty()) {
            authCallback.alErrorAutenticacion("Debe rellenar todos los datos");
            return;
        }

        if (password.length() < 6) {
            authCallback.alErrorAutenticacion("La contraseña debe tener al menos 6 caracteres");
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser userCreate = firebaseAuth.getCurrentUser();
                        if (userCreate != null) {
                            // Guardar usuario en la base de datos (nuevo usuario)
                            UserManager.guardarUsuarioEnBaseDatos(userCreate, databaseReference);

                            // Enviar verificación por email
                            userCreate.sendEmailVerification()
                                    .addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            Log.d(TAG, "Email de verificación enviado correctamente");
                                            // Cerrar sesión después de enviar verificación
                                            firebaseAuth.signOut();
                                            authCallback.alEnviarVerificacionEmail();
                                        } else {
                                            Log.e(TAG, "Error al enviar email de verificación: " +
                                                    emailTask.getException().getMessage());
                                            firebaseAuth.signOut();
                                            authCallback.alErrorAutenticacion("Error al enviar email de verificación");
                                        }
                                    });
                        }

                        mainHandler.post(() -> {
                            Toast.makeText(context, "Usuario creado correctamente", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        String error = obtenerMensajeErrorRegistro(task.getException());
                        authCallback.alErrorAutenticacion(error);
                    }
                });
    }

    public void iniciarSesionConEmailYPassword(String email, String password) {
        if (email.isEmpty() || email.isBlank() || password.isBlank() || password.isEmpty()) {
            authCallback.alErrorAutenticacion("Debe rellenar todos los datos");
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser userLogeado = firebaseAuth.getCurrentUser();

                        if (userLogeado != null) {
                            // Recargar usuario para obtener el estado más reciente
                            userLogeado.reload().addOnCompleteListener(reloadTask -> {
                                if (reloadTask.isSuccessful()) {
                                    if (userLogeado.isEmailVerified()) {
                                        authCallback.alExitoAutenticacion(userLogeado);
                                    } else {
                                        firebaseAuth.signOut();
                                        authCallback.alErrorAutenticacion("Debe verificar su correo electrónico antes de iniciar sesión");
                                    }
                                } else {
                                    authCallback.alErrorAutenticacion("Error al verificar el estado del usuario");
                                }
                            });
                        }
                    } else {
                        String error = obtenerMensajeErrorLogin(task.getException());
                        authCallback.alErrorAutenticacion(error);
                    }
                });
    }

    public void iniciarSesionConGoogle() {
        GetGoogleIdOption getGoogleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(SERVER_CLIENT_ID)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(getGoogleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                context,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse getCredentialResponse) {
                        mainHandler.post(() -> {
                            manejarInicioSesionGoogle(getCredentialResponse.getCredential());
                        });
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        mainHandler.post(() -> {
                            manejarErrorGoogle(e);
                        });
                    }
                }
        );
    }

    private void manejarErrorGoogle(GetCredentialException e) {
        if (e instanceof GetCredentialCancellationException) {
            if (authCallback != null) {
                authCallback.alCancelarAutenticacion();
            }
        } else if (e instanceof NoCredentialException) {
            authCallback.alErrorAutenticacion("No hay cuentas de Google disponibles");
        } else {
            authCallback.alErrorAutenticacion("Error al iniciar sesión con Google: " + e.getMessage());
        }
    }

    private void manejarInicioSesionGoogle(Credential credential) {
        if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
                try {
                    Bundle credentialData = customCredential.getData();
                    GoogleIdTokenCredential googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credentialData);
                    autenticarFirebaseConGoogle(googleIdTokenCredential.getIdToken());
                } catch (Exception e) {
                    authCallback.alErrorAutenticacion("Error al procesar credencial de Google");
                }
            } else {
                authCallback.alErrorAutenticacion("Tipo de credencial no soportado");
            }
        } else {
            authCallback.alErrorAutenticacion("Credencial inválida");
        }
    }

    private void autenticarFirebaseConGoogle(String idToken) {
        if (idToken == null || idToken.isEmpty()) {
            authCallback.alErrorAutenticacion("Token de autenticación inválido");
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Usuario autenticado con Google exitosamente");
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            verificarYGuardarUsuarioGoogle(user);
                        }
                    } else {
                        String errorMsg = task.getException() != null ?
                                task.getException().getMessage() : "Error desconocido";
                        Log.w(TAG, "Fallo la autenticación con Firebase: " + errorMsg);
                        authCallback.alErrorAutenticacion("Error al iniciar sesión con Google: " + errorMsg);
                    }
                });
    }

    private void verificarYGuardarUsuarioGoogle(FirebaseUser user) {
        String userId = user.getUid();
        databaseReference.child("usuarios").child(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            // Usuario existente, actualizar datos básicos
                            UserManager.actualizarDatosBasicosUsuario(user, databaseReference);
                            authCallback.alExitoAutenticacion(user);
                        } else {
                            // Usuario nuevo, guardarlo
                            Log.d(TAG, "Usuario nuevo con Google, guardando en base de datos");
                            UserManager.guardarUsuarioEnBaseDatos(user, databaseReference);
                            authCallback.alExitoAutenticacion(user);
                        }
                    } else {
                        // En caso de error, continuar con la autenticación
                        Log.w(TAG, "Error al verificar usuario en BD, continuando: " + task.getException());
                        authCallback.alExitoAutenticacion(user);
                    }
                });
    }

    public void verificarSesionActiva() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            // Verificar si es usuario de Google
            boolean esGoogle = false;
            for (int i = 0; i < currentUser.getProviderData().size(); i++) {
                if ("google.com".equals(currentUser.getProviderData().get(i).getProviderId())) {
                    esGoogle = true;
                    break;
                }
            }

            if (currentUser.isEmailVerified() || esGoogle) {
                authCallback.alExitoAutenticacion(currentUser);
            } else {
                // Usuario no verificado, cerrar sesión
                firebaseAuth.signOut();
            }
        }
    }

    public void cerrarSesion() {
        firebaseAuth.signOut();
    }

    private String obtenerMensajeErrorRegistro(Exception exception) {
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            return "La contraseña es muy débil";
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return "El email no es válido";
        } else if (exception instanceof FirebaseAuthUserCollisionException) {
            return "Ya existe una cuenta con este email";
        } else {
            return "Error al crear la cuenta: " + (exception != null ? exception.getMessage() : "Error desconocido");
        }
    }

    private String obtenerMensajeErrorLogin(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return "Email o contraseña incorrectos";
        } else {
            return "Error al iniciar sesión: " + (exception != null ? exception.getMessage() : "Error desconocido");
        }
    }
}