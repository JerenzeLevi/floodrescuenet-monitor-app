package com.example.floodrescuenetmonitor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.example.floodrescuenetmonitor.data.MonitorState;
import com.example.floodrescuenetmonitor.ui.BottomNavHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Locale;

/**
 * Profile screen (Visily "Profile"). Name, office, role and email come from users/{uid}.
 * Notification choices are stored on this device only; nothing is delivered from them yet.
 */
public class ProfileActivity extends MonitorActivity {
    private static final String PREFS = "profile_prefs";
    private DatabaseReference profileRef;
    private ValueEventListener profileListener;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_profile);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSignOut).setOnClickListener(v -> logout());
        findViewById(R.id.rowAdmin).setOnClickListener(v -> startActivity(new Intent(this, AdminActivity.class)));
        findViewById(R.id.rowSecurity).setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Security")
            .setItems(new String[]{"Reset password","Access activity on this phone"},(d,which)->{
                if(which==0)sendPasswordReset();else new AlertDialog.Builder(this).setTitle("Local access activity").setMessage(com.example.floodrescuenetmonitor.data.SecurityAudit.summary(this)).setPositiveButton("Close",null).show();
            }).show());
        findViewById(R.id.btnDataSource).setOnClickListener(v->{
            FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();if(user==null)return;
            boolean device=com.example.floodrescuenetmonitor.data.SessionSettings.device(this,user.getUid());
            new AlertDialog.Builder(this).setTitle("Monitoring source")
                .setSingleChoiceItems(new String[]{"Simulation","ESP32 device (requires provisioning)"},device?1:0,(d,which)->{
                    com.example.floodrescuenetmonitor.data.SessionSettings.setDevice(this,user.getUid(),which==1);d.dismiss();
                    startActivity(new Intent(this,DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));finish();
                }).setNegativeButton("Cancel",null).show();
        });
        findViewById(R.id.rowHelp).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Help & Support")
                .setMessage("• Home shows the latest water level and net status.\n• Live shows telemetry and recent readings.\n• Alerts lists events; Deploy sends a manual request.\n• Accounts must be approved by an administrator before monitoring is available.\n\nFor account problems, contact your barangay DRRM administrator.")
                .setPositiveButton("Close", null).show());
        bindPref(R.id.checkPush, "push");
        bindPref(R.id.checkEmail, "email");
        bindPref(R.id.checkSms, "sms");
        BottomNavHelper.setup(this, findViewById(R.id.bottomNav), BottomNavHelper.Tab.PROFILE);
    }

    private void bindPref(int id, String key) {
        CompoundButton box = findViewById(id);
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        box.setChecked(prefs.getBoolean(key, !"email".equals(key)));
        box.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean(key, checked).apply());
    }

    private void sendPasswordReset() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        String email = user.getEmail();
        new AlertDialog.Builder(this).setTitle("Change password?")
                .setMessage("Send a password reset email to " + email + "?")
                .setPositiveButton("Send", (d, w) -> FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnSuccessListener(x -> message("Password reset email sent to " + email))
                        .addOnFailureListener(e -> message(getString(R.string.error_reset_failed))))
                .setNegativeButton("Cancel", null).show();
    }

    @Override protected void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        ((TextView) findViewById(R.id.tvProfileEmail)).setText(user.getEmail() == null ? "—" : user.getEmail());
        profileRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        profileListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) { render(s); }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };
        profileRef.addValueEventListener(profileListener);
    }

    @Override protected void onStop() {
        if (profileRef != null && profileListener != null) profileRef.removeEventListener(profileListener);
        super.onStop();
    }

    private void render(DataSnapshot s) {
        String name = s.child("fullName").getValue(String.class);
        String office = s.child("barangay").getValue(String.class);
        String role = s.child("role").getValue(String.class);
        boolean admin = "admin".equals(role) || "superadmin".equals(role);
        if (name == null || name.trim().isEmpty()) name = "Unnamed user";
        ((TextView) findViewById(R.id.tvProfileName)).setText(name);
        ((TextView) findViewById(R.id.tvProfileInitial)).setText(name.substring(0, 1).toUpperCase(Locale.ROOT));
        ((TextView) findViewById(R.id.tvProfileOffice)).setText(office == null || office.isEmpty() ? "Not set" : office);
        ((TextView) findViewById(R.id.tvProfileRole)).setText(admin ? ("superadmin".equals(role) ? "Super Administrator" : "Administrator") : "Authorized Official");
        ((TextView) findViewById(R.id.tvAccessLevel)).setText(admin ? "Administrator Access Level" : "Official Access Level");
        ((TextView) findViewById(R.id.tvAccessDesc)).setText(admin
                ? "Monitoring, manual deployment and approval of barangay accounts."
                : "Monitoring and manual deployment of the rescue net.");
        ChipGroup chips = findViewById(R.id.permissionChips);
        chips.removeAllViews();
        String[] perms = admin
                ? new String[]{"Live Monitoring", "Manual Deploy", "System Logs", "Account Approval"}
                : new String[]{"Live Monitoring", "Manual Deploy", "System Logs"};
        for (String p : perms) {
            Chip chip = new Chip(this);
            chip.setText(p); chip.setTextSize(11); chip.setClickable(false); chip.setCheckable(false);
            chip.setChipBackgroundColorResource(R.color.ad_hoc_f5f5fa);
            chip.setTextColor(getColor(R.color.text_secondary));
            chips.addView(chip);
        }
        findViewById(R.id.rowAdmin).setVisibility(admin ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    @Override protected void onMonitorState(MonitorState state) { /* profile needs no telemetry */ }
}
