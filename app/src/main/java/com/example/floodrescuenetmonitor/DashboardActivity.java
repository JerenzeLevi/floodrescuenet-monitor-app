package com.example.floodrescuenetmonitor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.floodrescuenetmonitor.data.*;
import com.example.floodrescuenetmonitor.ui.*;
import com.example.floodrescuenetmonitor.ui.adapter.*;
import java.util.*;
import static com.example.floodrescuenetmonitor.data.MonitorState.*;

public class DashboardActivity extends MonitorActivity {
    private Object previousEvents, previousContacts;
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved); setContentView(R.layout.activity_dashboard);
        list(R.id.rvHistory); list(R.id.rvContacts);
        bindLegendDot(R.id.legendNormal,"Normal",R.color.green_success);
        bindLegendDot(R.id.legendWarning,"Warning",R.color.amber_warning);
        bindLegendDot(R.id.legendDanger,"Danger",R.color.red_danger);
        findViewById(R.id.tvOpenLive).setOnClickListener(v -> startActivity(new Intent(this, LiveMonitoringActivity.class)));
        findViewById(R.id.tvViewHistory).setOnClickListener(v -> startActivity(new Intent(this, AlertsHistoryActivity.class)));
        findViewById(R.id.avatarButton).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        BottomNavHelper.setup(this,findViewById(R.id.bottomNav),BottomNavHelper.Tab.HOME);
    }
    @Override protected void onStart() {
        super.onStart();
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("fullName")
            .get().addOnSuccessListener(snap -> {
                String name = snap.getValue(String.class);
                if (name != null && !name.trim().isEmpty()) label(R.id.tvAvatarInitial, name.trim().substring(0,1).toUpperCase(Locale.ROOT));
            });
    }
    private void list(int id) { ((RecyclerView)findViewById(id)).setLayoutManager(new LinearLayoutManager(this)); }
    private void label(int id,String value) { ((TextView)findViewById(id)).setText(value); }
    @Override protected void onMonitorState(MonitorState s) {
        String value = s.valid() ? String.format(Locale.getDefault(),"%.1f",s.water()) : "—";
        label(R.id.tvWaterCm,value); label(R.id.tvMiniWater,value);
        label(R.id.tvUpdatedAt,"UPDATED: " + date(s.updatedAt()));
        label(R.id.tvNetState,s.valid() ? s.net().toUpperCase(Locale.ROOT) : "UNKNOWN");
        String tier = s.fresh() && s.connected ? s.level() : "NO LIVE DATA";
        label(R.id.tvMiniTier,tier);
        int color = "DANGER".equals(tier) ? R.color.red_danger : "WARNING".equals(tier) ? R.color.amber_warning : "NORMAL".equals(tier) ? R.color.green_success : R.color.text_secondary;
        ((TextView)findViewById(R.id.tvMiniTier)).setTextColor(getColor(color));
        StatusPill.bind(findViewById(R.id.pillWaterLevel),tier,getColor(R.color.card_border),getColor(color));
        StatusPill.bind(findViewById(R.id.pillNetActive),"● "+s.sourceLabel(),getColor(R.color.white),getColor(R.color.green_success));
        ((WaterLevelGauge)findViewById(R.id.dashGauge)).setLevel(s.valid()?s.water():null,tier);
        ((TrendView)findViewById(R.id.trendChart)).setReadings(s.readings);
        if (previousEvents != s.events) {
            previousEvents = s.events;
            List<HistoryEntry> history = new ArrayList<>();
            for (int i=0; i<Math.min(3,s.events.size());i++) {
                Map<String,Object> row=s.events.get(i);
                history.add(new HistoryEntry(text(row,"title","Event"),date(time(row,"timestamp")),text(row,"resolution",text(row,"severity","SYSTEM"))));
            }
            if(history.isEmpty()) history.add(new HistoryEntry("No events yet","Waiting for the selected source",""));
            ((RecyclerView)findViewById(R.id.rvHistory)).setAdapter(new HistoryAdapter(history));
        }
        label(R.id.tvSelfCheck,"No diagnostic result reported"); label(R.id.tvSelfCheckTime,"A request needs a source response");
        for (Map<String,Object> row:diagnosticRows(s)) if("DIAGNOSTIC".equals(text(row,"type",""))) {
            label(R.id.tvSelfCheck,text(row,"title","Diagnostic result"));label(R.id.tvSelfCheckTime,date(time(row,"timestamp")));break;
        }
        if(previousContacts != s.contacts) {
            previousContacts=s.contacts; List<EmergencyContact> contacts=new ArrayList<>();
            for(Map<String,Object> row:s.contacts) contacts.add(new EmergencyContact(text(row,"name","Contact"),text(row,"subtitle",""),text(row,"number","")));
            if(contacts.isEmpty()) contacts.add(new EmergencyContact("No contacts configured","Add verified numbers to your session", ""));
            ((RecyclerView)findViewById(R.id.rvContacts)).setAdapter(new ContactAdapter(contacts));
        }
    }
    private java.util.List<Map<String,Object>> diagnosticRows(MonitorState s) {
        if(!s.diagnostic.isEmpty())return java.util.Collections.singletonList(s.diagnostic);
        return s.events;
    }
    private void bindLegendDot(int id,String label,int color) {
        android.view.View row=findViewById(id);
        row.findViewById(R.id.dot).setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(color)));
        ((TextView)row.findViewById(R.id.label)).setText(label);
    }
}
