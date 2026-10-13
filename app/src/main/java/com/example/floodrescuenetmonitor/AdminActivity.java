package com.example.floodrescuenetmonitor;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference usersRef;
    private ValueEventListener usersListener;

    private String myUid;
    private String myRole;
    private String myBarangay;

    private LinearLayout pendingContainer;
    private LinearLayout promoteContainer;
    private TextView tvEmptyPending;
    private TextView tvEmptyPromote;
    private TextView tvAdminScope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            goToLogin();
            return;
        }
        myUid = auth.getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        pendingContainer = findViewById(R.id.pendingContainer);
        promoteContainer = findViewById(R.id.promoteContainer);
        tvEmptyPending = findViewById(R.id.tvEmptyPending);
        tvEmptyPromote = findViewById(R.id.tvEmptyPromote);
        tvAdminScope = findViewById(R.id.tvAdminScope);

        findViewById(R.id.btnGoDashboard).setOnClickListener(v ->
                startActivity(new Intent(this, DashboardActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                DataSnapshot me = snapshot.child(myUid);
                myRole = me.child("role").getValue(String.class);
                myBarangay = me.child("barangay").getValue(String.class);

                // A non-admin reaching this screen (e.g. stale deep link) has no
                // business here, regardless of what LoginActivity intended.
                if (!"admin".equals(myRole) && !"superadmin".equals(myRole)) {
                    goToDashboard();
                    return;
                }

                boolean isSuperAdmin = "superadmin".equals(myRole);
                tvAdminScope.setText(isSuperAdmin
                        ? "Scope: all barangays"
                        : "Scope: " + (myBarangay == null ? "your barangay" : myBarangay));
                findViewById(R.id.promoteSection).setVisibility(isSuperAdmin ? android.view.View.VISIBLE : android.view.View.GONE);

                renderPending(snapshot, isSuperAdmin);
                if (isSuperAdmin) {
                    renderPromotable(snapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(AdminActivity.this, R.string.admin_load_failed, Toast.LENGTH_LONG).show();
            }
        };
        usersRef.addValueEventListener(usersListener);
    }

    @Override
    protected void onStop() {
        if (usersListener != null) usersRef.removeEventListener(usersListener);
        usersListener = null;
        super.onStop();
    }

    private void renderPending(DataSnapshot snapshot, boolean isSuperAdmin) {
        pendingContainer.removeAllViews();
        int count = 0;
        for (DataSnapshot child : snapshot.getChildren()) {
            String uid = child.getKey();
            String role = child.child("role").getValue(String.class);
            boolean verified = Boolean.TRUE.equals(child.child("verified").getValue());
            String barangay = child.child("barangay").getValue(String.class);
            if (!"user".equals(role) || verified) continue;
            if (!isSuperAdmin && (barangay == null || !barangay.equals(myBarangay))) continue;

            addRow(pendingContainer, child, R.string.admin_approve, true,
                    () -> approve(uid), () -> reject(uid));
            count++;
        }
        tvEmptyPending.setVisibility(count == 0 ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void renderPromotable(DataSnapshot snapshot) {
        promoteContainer.removeAllViews();
        int count = 0;
        for (DataSnapshot child : snapshot.getChildren()) {
            String uid = child.getKey();
            String role = child.child("role").getValue(String.class);
            boolean verified = Boolean.TRUE.equals(child.child("verified").getValue());
            if (!"user".equals(role) || !verified) continue;

            addRow(promoteContainer, child, R.string.admin_promote, false,
                    () -> promote(uid), null);
            count++;
        }
        tvEmptyPromote.setVisibility(count == 0 ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private interface Action { void run(); }

    private void addRow(LinearLayout container, DataSnapshot user, int primaryLabelRes,
                         boolean showSecondary, Action onPrimary, Action onSecondary) {
        android.view.View row = LayoutInflater.from(this).inflate(R.layout.item_admin_user, container, false);
        String fullName = user.child("fullName").getValue(String.class);
        String email = user.child("email").getValue(String.class);
        String barangay = user.child("barangay").getValue(String.class);
        ((TextView) row.findViewById(R.id.tvName)).setText(fullName == null ? email : fullName);
        ((TextView) row.findViewById(R.id.tvMeta)).setText(
                (email == null ? "" : email) + (barangay == null ? "" : " • " + barangay));

        MaterialButton primary = row.findViewById(R.id.btnPrimary);
        primary.setText(primaryLabelRes);
        primary.setOnClickListener(v -> onPrimary.run());

        MaterialButton secondary = row.findViewById(R.id.btnSecondary);
        if (showSecondary && onSecondary != null) {
            secondary.setOnClickListener(v -> onSecondary.run());
        } else {
            secondary.setVisibility(android.view.View.GONE);
        }
        container.addView(row);
    }

    private void approve(String uid) {
        Map<String, Object> update = new HashMap<>();
        update.put("verified", true);
        update.put("verifiedBy", myUid);
        update.put("verifiedAt", ServerValue.TIMESTAMP);
        usersRef.child(uid).updateChildren(update)
                .addOnFailureListener(e -> showActionFailed());
    }

    private void reject(String uid) {
        // Removing the RTDB profile revokes app-level access (verified check will
        // always fail); the Firebase Auth account itself can't be deleted from a
        // client — that requires the Admin SDK / a Cloud Function, out of scope here.
        usersRef.child(uid).removeValue()
                .addOnFailureListener(e -> showActionFailed());
    }

    private void promote(String uid) {
        usersRef.child(uid).child("role").setValue("admin")
                .addOnFailureListener(e -> showActionFailed());
    }

    private void showActionFailed() {
        if (isFinishing() || isDestroyed()) return;
        Toast.makeText(this, R.string.admin_action_failed, Toast.LENGTH_LONG).show();
    }

    private void goToDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
