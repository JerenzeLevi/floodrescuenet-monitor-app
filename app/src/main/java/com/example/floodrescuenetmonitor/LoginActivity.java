package com.example.floodrescuenetmonitor;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText etUsername;
    private EditText etPassword;
    private MaterialButton btnLogin;
    private TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> attemptLogin());
        findViewById(R.id.tvSignUp).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));
        tvForgotPassword.setOnClickListener(v -> sendPasswordReset());
        findViewById(R.id.tvHaveResetLink).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() != null) {
            checkVerifiedAndProceed(auth.getCurrentUser().getUid());
        }
    }

    private void attemptLogin() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etUsername.setError(getString(R.string.error_invalid_email));
            etUsername.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.error_password_required));
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (isFinishing() || isDestroyed()) return;
                    String uid = result.getUser() != null ? result.getUser().getUid() : null;
                    if (uid == null) {
                        btnLogin.setEnabled(true);
                        return;
                    }
                    checkVerifiedAndProceed(uid);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this,
                            getString(R.string.error_login_failed),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void checkVerifiedAndProceed(String uid) {
        FirebaseDatabase.getInstance().getReference("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (isFinishing() || isDestroyed()) return;
                        if (!snapshot.exists()) {
                            // No profile row for this auth uid — don't distinguish this
                            // from a bad password in the UI, same generic failure message.
                            btnLogin.setEnabled(true);
                            auth.signOut();
                            Toast.makeText(LoginActivity.this,
                                    getString(R.string.error_login_failed),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        boolean verified = Boolean.TRUE.equals(snapshot.child("verified").getValue());
                        if (!verified) {
                            btnLogin.setEnabled(true);
                            auth.signOut();
                            Toast.makeText(LoginActivity.this,
                                    getString(R.string.error_account_pending_verification),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        String role = snapshot.child("role").getValue(String.class);
                        if ("admin".equals(role) || "superadmin".equals(role)) {
                            goToAdmin();
                        } else {
                            goToDashboard();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        if (isFinishing() || isDestroyed()) return;
                        btnLogin.setEnabled(true);
                        auth.signOut();
                        Toast.makeText(LoginActivity.this,
                                getString(R.string.error_login_failed),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendPasswordReset() {
        String email = etUsername.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etUsername.setError(getString(R.string.error_email_before_reset));
            etUsername.requestFocus();
            Toast.makeText(this, R.string.error_email_before_reset, Toast.LENGTH_LONG).show();
            return;
        }
        tvForgotPassword.setEnabled(false);
        ActionCodeSettings settings = ActionCodeSettings.newBuilder()
                .setUrl("https://floodrescue-app.web.app/reset")
                .setHandleCodeInApp(true)
                .setAndroidPackageName(getPackageName(), false, null)
                .build();
        auth.sendPasswordResetEmail(email, settings)
                .addOnSuccessListener(unused -> {
                    if (isFinishing() || isDestroyed()) return;
                    tvForgotPassword.setEnabled(true);
                    Toast.makeText(LoginActivity.this,
                            getString(R.string.msg_reset_email_sent, email), Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    tvForgotPassword.setEnabled(true);
                    Toast.makeText(LoginActivity.this,
                            getString(R.string.error_reset_failed),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void goToDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToAdmin() {
        Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
