package com.example.floodrescuenetmonitor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Completes a Firebase password reset entirely in-app, so the flow does not depend on
 * Firebase's externally hosted action page (whose single-use oobCode can be invalidated
 * by mail-provider link prefetching before the user opens it).
 *
 * Reached two ways:
 *  - An Android App Link from the reset email (once the continue-URL domain is verified
 *    for this app), which delivers the oobCode as an intent data query parameter.
 *  - The user pasting the reset link (or the code) from the email into this screen.
 */
public class ResetPasswordActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText etResetLink;
    private MaterialButton btnVerifyLink;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private MaterialButton btnSubmitNewPassword;
    private TextView tvBackToLogin;

    private String verifiedOobCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        auth = FirebaseAuth.getInstance();

        etResetLink = findViewById(R.id.etResetLink);
        btnVerifyLink = findViewById(R.id.btnVerifyLink);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmitNewPassword = findViewById(R.id.btnSubmitNewPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnVerifyLink.setOnClickListener(v -> verifyPastedLink());
        btnSubmitNewPassword.setOnClickListener(v -> submitNewPassword());
        tvBackToLogin.setOnClickListener(v -> goToLogin());

        String codeFromIntent = extractOobCode(getIntent());
        if (codeFromIntent != null) {
            verifyOobCode(codeFromIntent);
        }
    }

    private void verifyPastedLink() {
        String pasted = etResetLink.getText().toString().trim();
        if (TextUtils.isEmpty(pasted)) {
            etResetLink.setError(getString(R.string.error_reset_link_required));
            etResetLink.requestFocus();
            return;
        }
        String code = extractOobCode(pasted);
        if (code == null) {
            etResetLink.setError(getString(R.string.error_reset_link_invalid));
            etResetLink.requestFocus();
            return;
        }
        verifyOobCode(code);
    }

    /** Pulls the oobCode either from an incoming App Link intent or a pasted link/code string. */
    private String extractOobCode(Intent intent) {
        if (intent == null || intent.getData() == null) return null;
        return extractOobCode(intent.getData().toString());
    }

    private String extractOobCode(String linkOrCode) {
        if (TextUtils.isEmpty(linkOrCode)) return null;
        try {
            Uri uri = Uri.parse(linkOrCode.trim());
            String fromQuery = uri.getQueryParameter("oobCode");
            if (!TextUtils.isEmpty(fromQuery)) return fromQuery;
        } catch (Exception ignored) {
            // Not a URL — fall through and treat the raw input as the code itself.
        }
        // Firebase's continue-URL redirect can also arrive as a fragment (#oobCode=...).
        int fragIdx = linkOrCode.indexOf("oobCode=");
        if (fragIdx >= 0) {
            String tail = linkOrCode.substring(fragIdx + "oobCode=".length());
            int amp = tail.indexOf('&');
            return amp >= 0 ? tail.substring(0, amp) : tail;
        }
        String trimmed = linkOrCode.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void verifyOobCode(String oobCode) {
        btnVerifyLink.setEnabled(false);
        auth.verifyPasswordResetCode(oobCode)
                .addOnSuccessListener(email -> {
                    if (isFinishing() || isDestroyed()) return;
                    verifiedOobCode = oobCode;
                    btnVerifyLink.setEnabled(true);
                    etNewPassword.setEnabled(true);
                    etConfirmPassword.setEnabled(true);
                    btnSubmitNewPassword.setEnabled(true);
                    Toast.makeText(this,
                            getString(R.string.msg_reset_link_verified, email),
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnVerifyLink.setEnabled(true);
                    Toast.makeText(this,
                            getString(R.string.error_reset_link_expired),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void submitNewPassword() {
        if (verifiedOobCode == null) return;

        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 8) {
            etNewPassword.setError(getString(R.string.error_password_length));
            etNewPassword.requestFocus();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError(getString(R.string.error_password_mismatch));
            etConfirmPassword.requestFocus();
            return;
        }

        btnSubmitNewPassword.setEnabled(false);
        auth.confirmPasswordReset(verifiedOobCode, newPassword)
                .addOnSuccessListener(unused -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this,
                            getString(R.string.msg_password_reset_success),
                            Toast.LENGTH_LONG).show();
                    goToLogin();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnSubmitNewPassword.setEnabled(true);
                    Toast.makeText(this,
                            getString(R.string.error_reset_submit_failed),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
