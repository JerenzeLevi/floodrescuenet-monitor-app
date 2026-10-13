package com.example.floodrescuenetmonitor;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText etFullName;
    private EditText etBarangay;
    private EditText etRole;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private MaterialButton btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();

        etFullName = findViewById(R.id.etFullName);
        etBarangay = findViewById(R.id.etBarangay);
        etRole = findViewById(R.id.etRole);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> attemptRegister());
        findViewById(R.id.tvLoginInstead).setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String fullName = etFullName.getText().toString().trim();
        String barangay = etBarangay.getText().toString().trim();
        String role = etRole.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError(getString(R.string.error_full_name_required));
            etFullName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(barangay)) {
            etBarangay.setError(getString(R.string.error_barangay_required));
            etBarangay.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_invalid_email));
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 8) {
            etPassword.setError(getString(R.string.error_password_length));
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError(getString(R.string.error_password_mismatch));
            etConfirmPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        btnRegister.setEnabled(true);
                        return;
                    }
                    saveProfile(user, fullName, barangay, role, email);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnRegister.setEnabled(true);
                    Toast.makeText(SignUpActivity.this,
                            getString(R.string.error_registration_failed),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveProfile(FirebaseUser user, String fullName, String barangay, String role, String email) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());

        Map<String, Object> profile = new HashMap<>();
        profile.put("fullName", fullName);
        profile.put("barangay", barangay);
        profile.put("role", role);
        profile.put("email", email);
        profile.put("role", "user");
        profile.put("verified", false);
        profile.put("createdAt", com.google.firebase.database.ServerValue.TIMESTAMP);

        usersRef.setValue(profile)
                .addOnSuccessListener(unused -> {
                    auth.signOut();
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(SignUpActivity.this,
                            getString(R.string.msg_account_created),
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Roll back the just-created Auth account so we don't leave a
                    // signed-in user with no profile record and no verification gate.
                    user.delete();
                    auth.signOut();
                    if (isFinishing() || isDestroyed()) return;
                    btnRegister.setEnabled(true);
                    Toast.makeText(SignUpActivity.this,
                            getString(R.string.error_profile_save_failed),
                            Toast.LENGTH_LONG).show();
                });
    }
}
