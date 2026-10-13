package com.example.floodrescuenetmonitor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.floodrescuenetmonitor.data.*;
import com.example.floodrescuenetmonitor.ui.*;
import com.example.floodrescuenetmonitor.ui.adapter.ActivityLogAdapter;
import java.util.*;
import static com.example.floodrescuenetmonitor.data.MonitorState.*;

public class LiveMonitoringActivity extends MonitorActivity {
    private Object previousReadings;
    private boolean sending;
    @Override protected void onStart() { sending=false; super.onStart(); }
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);setContentView(R.layout.activity_live_monitoring);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((WaterLevelGauge)findViewById(R.id.waterGauge)).setFilled(true);
        ((RecyclerView)findViewById(R.id.rvActivity)).setLayoutManager(new LinearLayoutManager(this));
        for(int id:new int[]{R.id.tvHistory,R.id.tvFullLogs}) findViewById(id).setOnClickListener(v -> startActivity(new Intent(this,AlertsHistoryActivity.class)));
        findViewById(R.id.btnDiagnostic).setOnClickListener(v -> {
            if(repository == null) return;
            sending=true;onMonitorState(state);
            repository.request("DIAGNOSTIC","Tabletop demo",state.mode(),(ok,message)->{sending=false;message(message);onMonitorState(state);});
        });
        BottomNavHelper.setup(this,findViewById(R.id.bottomNav),BottomNavHelper.Tab.LIVE);
    }
    private void label(int id,String value) { ((TextView)findViewById(id)).setText(value); }
    @Override protected void onMonitorState(MonitorState s) {
        label(R.id.tvWaterCm,s.valid()?String.format(Locale.getDefault(),"%.1f",s.water()):"—");
        label(R.id.tvUpdatedAt,date(s.updatedAt()));
        label(R.id.tvSensorStatus,"SIMULATED TELEMETRY");
        label(R.id.tvTrend,s.fresh()&&s.connected?s.trend():"NO LIVE TREND");label(R.id.tvTrendSource,"DEMO");
        label(R.id.tvNetState,s.valid()?s.net().toUpperCase(Locale.ROOT):"UNKNOWN");
        label(R.id.tvNetActivity,date(time(s.telemetry,"lastActivityAt")));
        label(R.id.tvCommandStatus,s.commandText());
        String tier=s.fresh()&&s.connected?s.level():"UNKNOWN";
        int color="DANGER".equals(tier)?R.color.red_danger:"WARNING".equals(tier)?R.color.amber_warning:"NORMAL".equals(tier)?R.color.green_success:R.color.text_secondary;
        ((TextView)findViewById(R.id.tvWaterCm)).setTextColor(getColor(color));
        ((WaterLevelGauge)findViewById(R.id.waterGauge)).setLevel(s.valid()?s.water():null,tier);
        boolean live=s.fresh()&&s.connected;
        TextView livePill=findViewById(R.id.pillLive);
        livePill.setBackground(null);livePill.setPadding(0,0,0,0);livePill.setText(live?"● DEMO LIVE":"NO LIVE DATA");
        livePill.setTextColor(getColor(live?R.color.red_danger:R.color.text_secondary));
        TextView trend=findViewById(R.id.tvTrend);
        String trendText=trend.getText().toString();
        trend.setTextColor(getColor(!live?R.color.text_secondary:trendText.startsWith("RISING")||trendText.startsWith("Rising")?R.color.red_danger:trendText.startsWith("RECEDING")||trendText.startsWith("Receding")?R.color.green_success:R.color.text_primary));
        bindChip(R.id.chipSignal,R.drawable.ic_wifi,"SIGNAL",text(s.telemetry,"signal","Not reported"));
        bindChip(R.id.chipBattery,R.drawable.ic_battery_full,"BATTERY",reading(number(s.telemetry,"batteryPercent"),"%"));
        bindChip(R.id.chipRain,R.drawable.ic_umbrella,"RAIN",text(s.telemetry,"rain","Not reported"));
        findViewById(R.id.warningBanner).setVisibility("WARNING".equals(tier)||"DANGER".equals(tier)?View.VISIBLE:View.GONE);
        label(R.id.tvWarningTitle,tier+" · simulated water level");
        label(R.id.tvWarningDetail,"Warning ≥ "+s.warning()+" cm; danger ≥ "+s.danger()+" cm.");
        findViewById(R.id.btnDiagnostic).setEnabled(!sending&&repository!=null&&s.canRequest());
        if(previousReadings!=s.readings) {
            previousReadings=s.readings;List<ActivityLogEntry> items=new ArrayList<>();
            for(Map<String,Object> row:s.readings) items.add(new ActivityLogEntry(date(time(row,"timestamp")),reading(number(row,"waterCm")," cm"),text(row,"trend","—")));
            if(items.isEmpty())items.add(new ActivityLogEntry("Waiting for readings","—","NO DATA"));
            ((RecyclerView)findViewById(R.id.rvActivity)).setAdapter(new ActivityLogAdapter(items));
        }
    }
    private void bindChip(int id,int icon,String label,String value) {
        View chip=findViewById(id);((ImageView)chip.findViewById(R.id.ivIcon)).setImageResource(icon);
        ((TextView)chip.findViewById(R.id.tvLabel)).setText(label);((TextView)chip.findViewById(R.id.tvValue)).setText(value);
    }
}
