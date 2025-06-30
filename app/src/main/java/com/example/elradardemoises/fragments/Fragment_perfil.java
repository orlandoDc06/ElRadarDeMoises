package com.example.elradardemoises.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.elradardemoises.R;
import com.example.elradardemoises.models.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Fragment_perfil extends Fragment {
    private static final String TAG = "Fragment_perfil";

    // Views
    private ImageView ivEditMode;
    private ImageView ivFotoPerfil;
    private TextView tvCambiarFoto;
    private TextInputLayout tilNombre, tilPassword, tilConfirmPassword, tilEmail;
    private TextInputEditText etNombre, etPassword, etConfirmPassword, etEmail;
    private LinearLayout llBotonesAccion;
    private MaterialButton btnCancelar, btnGuardar;
    private TextView tvFechaRegistro;

    // Firebase
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;

    // Estado
    private boolean modoEdicion = false;
    private Usuario usuarioActual;
    private String nombreOriginal;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        currentUser = firebaseAuth.getCurrentUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inicializarVistas(view);
        configurarEventos();
        cargarDatosUsuario();
    }

    private void inicializarVistas(View view) {
        // Header
        ivEditMode = view.findViewById(R.id.iv_edit_mode);

        // Foto de perfil
        ivFotoPerfil = view.findViewById(R.id.iv_foto_perfil);


        // Campos de texto
        tilNombre = view.findViewById(R.id.til_nombre);
        tilPassword = view.findViewById(R.id.til_password);
        tilConfirmPassword = view.findViewById(R.id.til_confirm_password);
        tilEmail = view.findViewById(R.id.til_email);

        etNombre = view.findViewById(R.id.et_nombre);
        etPassword = view.findViewById(R.id.et_password);
        etConfirmPassword = view.findViewById(R.id.et_confirm_password);
        etEmail = view.findViewById(R.id.et_email);

        // Botones
        llBotonesAccion = view.findViewById(R.id.ll_botones_accion);
        btnCancelar = view.findViewById(R.id.btn_cancelar);
        btnGuardar = view.findViewById(R.id.btn_guardar);

        // Información adicional
        tvFechaRegistro = view.findViewById(R.id.tv_fecha_registro);
    }

    private void configurarEventos() {
        // Botón de editar
        ivEditMode.setOnClickListener(v -> toggleModoEdicion());

        // Botones de acción
        btnCancelar.setOnClickListener(v -> cancelarEdicion());
        btnGuardar.setOnClickListener(v -> guardarCambios());

        // Ocultar cambiar foto inicialmente
        if (tvCambiarFoto != null) {
            tvCambiarFoto.setVisibility(View.GONE);
        }
    }

    private void cargarDatosUsuario() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        mostrarDatosBasicos();

        String userId = currentUser.getUid();
        databaseReference.child("usuarios").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    usuarioActual = snapshot.getValue(Usuario.class);
                    if (usuarioActual != null) {
                        actualizarInterfaz();
                    }
                } else {
                    // Si no existe el usuario en la BD, crear uno básico
                    crearUsuarioBasico();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar datos del usuario: " + error.getMessage());
                Toast.makeText(getContext(), "Error al cargar datos del perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDatosBasicos() {
        etEmail.setText(currentUser.getEmail());

        String nombre = currentUser.getDisplayName();
        if (TextUtils.isEmpty(nombre)) {
            nombre = Usuario.extraerNombreDeCorreo(currentUser.getEmail());
        }
        etNombre.setText(nombre);
        nombreOriginal = nombre;

        cargarFotoPerfil();

        mostrarFechaRegistro();
    }

    private void actualizarInterfaz() {
        if (usuarioActual != null) {
            if (!TextUtils.isEmpty(usuarioActual.getNombre())) {
                if (etNombre != null) {
                    etNombre.setText(usuarioActual.getNombre());
                    nombreOriginal = usuarioActual.getNombre();
                }
            }

            if (!TextUtils.isEmpty(usuarioActual.getPp())) {
                cargarFotoPerfilDesdeUrl(usuarioActual.getPp());
            }
        }
    }
    private void crearUsuarioBasico() {
        usuarioActual = new Usuario();
        usuarioActual.setCorreo(currentUser.getEmail());
        usuarioActual.setNombre(nombreOriginal);
        usuarioActual.setKey(currentUser.getUid());

        if (currentUser.getPhotoUrl() != null) {
            usuarioActual.setPp(currentUser.getPhotoUrl().toString());
        } else {
            usuarioActual.setPp("");
        }
    }

    private void cargarFotoPerfil() {
        if (currentUser.getPhotoUrl() != null) {
            cargarFotoPerfilDesdeUrl(currentUser.getPhotoUrl().toString());
        } else {
            ivFotoPerfil.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private void cargarFotoPerfilDesdeUrl(String url) {
        if (getContext() != null && !TextUtils.isEmpty(url)) {
            Glide.with(getContext())
                    .load(url)
                    .apply(new RequestOptions()
                            .transform(new CircleCrop())
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery))
                    .into(ivFotoPerfil);
        }
    }

    private void mostrarFechaRegistro() {
        if (currentUser.getMetadata() != null) {
            long fechaCreacion = currentUser.getMetadata().getCreationTimestamp();
            Date fecha = new Date(fechaCreacion);
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));
            tvFechaRegistro.setText(sdf.format(fecha));
        }
    }

    private void toggleModoEdicion() {
        modoEdicion = !modoEdicion;
        configurarModoEdicion(modoEdicion);
    }

    private void configurarModoEdicion(boolean edicion) {
        etNombre.setEnabled(edicion);
        etPassword.setEnabled(edicion);
        etConfirmPassword.setEnabled(edicion);

        llBotonesAccion.setVisibility(edicion ? View.VISIBLE : View.GONE);

        ivEditMode.setImageResource(edicion ?
                android.R.drawable.ic_menu_close_clear_cancel :
                android.R.drawable.ic_menu_edit);

        if (!edicion) {
            etPassword.setText("");
            etConfirmPassword.setText("");

            etNombre.setText(nombreOriginal);
        }
    }

    private void cancelarEdicion() {
        configurarModoEdicion(false);
        modoEdicion = false;
    }

    private void guardarCambios() {
        if (!validarCampos()) {
            return;
        }

        String nuevoNombre = etNombre.getText().toString().trim();
        String nuevaPassword = etPassword.getText().toString().trim();

        btnGuardar.setEnabled(false);
        btnCancelar.setEnabled(false);

        if (!TextUtils.isEmpty(nuevaPassword)) {
            actualizarPassword(nuevaPassword, () -> {
                actualizarNombre(nuevoNombre);
            });
        } else {
            actualizarNombre(nuevoNombre);
        }
    }

    private boolean validarCampos() {
        String nombre = etNombre.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(nombre)) {
            tilNombre.setError("El nombre es requerido");
            return false;
        } else {
            tilNombre.setError(null);
        }

        if (!TextUtils.isEmpty(password) || !TextUtils.isEmpty(confirmPassword)) {
            if (password.length() < 6) {
                tilPassword.setError("La contraseña debe tener al menos 6 caracteres");
                return false;
            } else {
                tilPassword.setError(null);
            }

            if (!password.equals(confirmPassword)) {
                tilConfirmPassword.setError("Las contraseñas no coinciden");
                return false;
            } else {
                tilConfirmPassword.setError(null);
            }
        } else {
            tilPassword.setError(null);
            tilConfirmPassword.setError(null);
        }

        return true;
    }

    private void actualizarPassword(String nuevaPassword, Runnable onSuccess) {
        currentUser.updatePassword(nuevaPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Contraseña actualizada exitosamente");
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    } else {
                        Log.e(TAG, "Error al actualizar contraseña", task.getException());
                        Toast.makeText(getContext(), "Error al actualizar contraseña: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        habilitarBotones();
                    }
                });
    }

    private void actualizarNombre(String nuevoNombre) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(nuevoNombre)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Perfil de Firebase Auth actualizado");
                        actualizarEnBaseDatos(nuevoNombre);
                    } else {
                        Log.e(TAG, "Error al actualizar perfil de Auth", task.getException());
                        Toast.makeText(getContext(), "Error al actualizar perfil", Toast.LENGTH_SHORT).show();
                        habilitarBotones();
                    }
                });
    }

    private void actualizarEnBaseDatos(String nuevoNombre) {
        String userId = currentUser.getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("nombre", nuevoNombre);

        databaseReference.child("usuarios").child(userId).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    habilitarBotones();

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Datos actualizados en la base de datos");
                        Toast.makeText(getContext(), "Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show();

                        nombreOriginal = nuevoNombre;
                        if (usuarioActual != null) {
                            usuarioActual.setNombre(nuevoNombre);
                        }

                        configurarModoEdicion(false);
                        modoEdicion = false;

                    } else {
                        Log.e(TAG, "Error al actualizar en la base de datos", task.getException());
                        Toast.makeText(getContext(), "Error al guardar cambios", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void habilitarBotones() {
        if (btnGuardar != null && btnCancelar != null) {
            btnGuardar.setEnabled(true);
            btnCancelar.setEnabled(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ivEditMode = null;
        ivFotoPerfil = null;
        tvCambiarFoto = null;
        tilNombre = null;
        tilPassword = null;
        tilConfirmPassword = null;
        tilEmail = null;
        etNombre = null;
        etPassword = null;
        etConfirmPassword = null;
        etEmail = null;
        llBotonesAccion = null;
        btnCancelar = null;
        btnGuardar = null;
        tvFechaRegistro = null;
    }
}