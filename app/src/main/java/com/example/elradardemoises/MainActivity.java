package com.example.elradardemoises;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.elradardemoises.utils.AuthManager;
import com.google.firebase.auth.FirebaseUser;


import com.example.elradardemoises.R;


public class MainActivity extends AppCompatActivity implements AuthManager.CallbackAutenticacion {
    private static final String TAG = "MainActivity";

    // UI Elements
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister, btnGoogleLogin;
    private TextView tvForgotPassword, tvSwitchMode;
    private ProgressBar progressBar;

    // Auth Manager
    private AuthManager authManager;

    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        initializeAuthManager();
        setupClickListeners();


        authManager.verificarSesionActiva();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        //btnRegister = findViewById(R.id.btnRegister);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSwitchMode = findViewById(R.id.tvSwitchMode);
        progressBar = findViewById(R.id.progressBar);

        updateUIForCurrentMode();
    }

    private void initializeAuthManager() {
        authManager = new AuthManager(this, this);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            if (isLoginMode) {
                iniciarSesion();
            } else {
                registrarUsuario();
            }
        });

        if (btnGoogleLogin != null) {
            btnGoogleLogin.setOnClickListener(v -> iniciarSesionConGoogle());
        }

        tvSwitchMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUIForCurrentMode();
        });

        tvForgotPassword.setOnClickListener(v -> mostrarDialogoRecuperarPassword());
    }

    private void updateUIForCurrentMode() {
        if (isLoginMode) {
            btnLogin.setText("Iniciar Sesión");
            tvSwitchMode.setText("¿No tienes cuenta? Regístrate");
            tvForgotPassword.setVisibility(View.VISIBLE);
        } else {
            btnLogin.setText("Registrarse");
            tvSwitchMode.setText("¿Ya tienes cuenta? Inicia Sesión");
            tvForgotPassword.setVisibility(View.GONE);
        }
    }

    private void iniciarSesion() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (validarCampos(email, password)) {
            mostrarCargando(true);
            authManager.iniciarSesionConEmailYPassword(email, password);
        }
    }

    private void registrarUsuario() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (validarCampos(email, password)) {
            if (password.length() < 6) {
                mostrarError("La contraseña debe tener al menos 6 caracteres");
                return;
            }

            mostrarCargando(true);
            authManager.registrarConEmailYPassword(email, password);
        }
    }

    private void iniciarSesionConGoogle() {
        mostrarCargando(true);
        authManager.iniciarSesionConGoogle();
    }

    private boolean validarCampos(String email, String password) {
        if (email.isEmpty()) {
            etEmail.setError("El email es requerido");
            etEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email inválido");
            etEmail.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            etPassword.setError("La contraseña es requerida");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void mostrarDialogoRecuperarPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recuperar Contraseña");

        final EditText input = new EditText(this);
        input.setHint("Ingresa tu email");
        builder.setView(input);

        builder.setPositiveButton("Enviar", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                enviarRecuperacionPassword(email);
            } else {
                Toast.makeText(this, "Email inválido", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void enviarRecuperacionPassword(String email) {
        com.google.firebase.auth.FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Email de recuperación enviado", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Error al enviar email de recuperación", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void mostrarCargando(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!mostrar);
        if (btnGoogleLogin != null) {
            btnGoogleLogin.setEnabled(!mostrar);
        }
    }

    private void mostrarError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        mostrarCargando(false);
    }

    private void navegarADashboard(FirebaseUser user) {
        Intent intent = new Intent(this, MainDashboardActivity.class);
        intent.putExtra("user_email", user.getEmail());
        intent.putExtra("user_name", user.getDisplayName());
        intent.putExtra("user_id", user.getUid());

        startActivity(intent);
        finish();
    }

    @Override
    public void alExitoAutenticacion(FirebaseUser user) {
        mostrarCargando(false);
        navegarADashboard(user);
    }

    @Override
    public void alErrorAutenticacion(String error) {
        mostrarError(error);
    }

    @Override
    public void alEnviarVerificacionEmail() {
        mostrarCargando(false);
        new AlertDialog.Builder(this)
                .setTitle("Verificación de Email")
                .setMessage("Se ha enviado un email de verificación. Por favor, verifica tu email antes de iniciar sesión.")
                .setPositiveButton("OK", null)
                .show();

        isLoginMode = true;
        updateUIForCurrentMode();

        etEmail.setText("");
        etPassword.setText("");
    }

    @Override
    public void alCancelarAutenticacion() {
        mostrarCargando(false);
        Toast.makeText(this, "Autenticación cancelada", Toast.LENGTH_SHORT).show();
    }
}