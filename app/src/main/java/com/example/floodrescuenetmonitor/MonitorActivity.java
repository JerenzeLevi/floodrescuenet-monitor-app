package com.example.floodrescuenetmonitor;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.floodrescuenetmonitor.data.MonitorRepository;
import com.example.floodrescuenetmonitor.data.MonitorState;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

/** Keeps authentication, verification, listeners and freshness consistent across screens. */
public abstract class MonitorActivity extends AppCompatActivity {
    protected MonitorRepository repository;
    protected MonitorState state = new MonitorState();
    private final Handler clock = new Handler(Looper.getMainLooper());
    private DatabaseReference verified;
    private ValueEventListener verification;
    private FirebaseAuth.AuthStateListener authListener;
    private boolean started;
    private final Runnable tick = new Runnable() {
        @Override public void run() { if (started) { renderState(); clock.postDelayed(this, 1000); } }
    };
    @Override protected void onStart() {
        super.onStart(); started = true;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) { leave(); return; }
        authListener = a -> { if (started && a.getCurrentUser() == null) leave(); };
        auth.addAuthStateListener(authListener);
        verified = FirebaseDatabase.getInstance().getReference("users").child(auth.getCurrentUser().getUid()).child("verified");
        verification = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot data) {
                if (!started) return;
                if (!Boolean.TRUE.equals(data.getValue())) {
                    Toast.makeText(MonitorActivity.this, "This account needs administrator verification.", Toast.LENGTH_LONG).show();
                    auth.signOut(); return;
                }
                if (repository == null && auth.getCurrentUser() != null) {
                    repository = new MonitorRepository(auth.getCurrentUser().getUid());
                    repository.start(value -> { state = value; renderState(); });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (!started) return;
                if (repository != null) { repository.close(); repository = null; }
                state = new MonitorState();
                state.error = "Access check failed. Return to Login and retry.";
                renderState();
            }
        };
        verified.addValueEventListener(verification);
        clock.post(tick);
    }
    private void renderState() {
        if (!started || isFinishing()) return;
        TextView status = findViewById(R.id.tvConnectionStatus);
        if (status != null) status.setText(state.connectionText());
        updateAlertBadge();
        onMonitorState(state);
    }
    /** Red count on the Alerts tab: loaded events whose severity is DANGER or WARNING. */
    private void updateAlertBadge() {
        com.google.android.material.bottomnavigation.BottomNavigationView nav = findViewById(R.id.bottomNav);
        if (nav == null) return;
        int count = 0;
        for (java.util.Map<String, Object> row : state.events) {
            String severity = MonitorState.text(row, "severity", "");
            if ("DANGER".equals(severity) || "WARNING".equals(severity)) count++;
        }
        if (count == 0) { nav.removeBadge(R.id.nav_alerts); return; }
        com.google.android.material.badge.BadgeDrawable badge = nav.getOrCreateBadge(R.id.nav_alerts);
        badge.setNumber(count); badge.setMaxCharacterCount(3); badge.setVisible(true);
    }
    protected abstract void onMonitorState(MonitorState state);
    protected void message(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
    protected void logout() { FirebaseAuth.getInstance().signOut(); leave(); }
    private void leave() {
        if (isFinishing()) return;
        startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
    @Override protected void onStop() {
        started = false; clock.removeCallbacks(tick);
        if (verified != null && verification != null) verified.removeEventListener(verification);
        if (authListener != null) FirebaseAuth.getInstance().removeAuthStateListener(authListener);
        if (repository != null) { repository.close(); repository = null; }
        state = new MonitorState();
        super.onStop();
    }
}
