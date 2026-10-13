package com.example.floodrescuenetmonitor;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.floodrescuenetmonitor.data.*;
import com.example.floodrescuenetmonitor.ui.BottomNavHelper;
import com.example.floodrescuenetmonitor.ui.adapter.AlertAdapter;
import java.util.*;

public class AlertsHistoryActivity extends MonitorActivity {
    private AlertAdapter adapter;
    private String filter="All";
    private Object previousEvents;
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);setContentView(R.layout.activity_alerts_history);
        if(saved!=null)filter=saved.getString("filter","All");
        adapter=new AlertAdapter(new ArrayList<>());
        RecyclerView rv=findViewById(R.id.rvAlerts);rv.setLayoutManager(new LinearLayoutManager(this));rv.setAdapter(adapter);
        LinearLayout row=findViewById(R.id.filterRow);
        for(String category:Arrays.asList("All","Danger","Net Deployments","System")) {
            TextView chip=(TextView)LayoutInflater.from(this).inflate(R.layout.item_filter_chip,row,false);
            chip.setText(category);chip.setOnClickListener(v->{filter=category;renderFilter();});row.addView(chip);
        }
        findViewById(R.id.btnLiveData).setOnClickListener(v->startActivity(new Intent(this,LiveMonitoringActivity.class)));
        BottomNavHelper.setup(this,findViewById(R.id.bottomNav),BottomNavHelper.Tab.ALERTS);
        renderFilter();
    }
    @Override protected void onSaveInstanceState(Bundle out){out.putString("filter",filter);super.onSaveInstanceState(out);}
    @Override protected void onMonitorState(MonitorState s) {
        if(previousEvents!=s.events){previousEvents=s.events;adapter.setItems(UiData.alerts(s.events));renderFilter();}
    }
    private void renderFilter() {
        adapter.filter(filter);
        ((TextView)findViewById(R.id.tvEventsCount)).setText("RECENT EVENTS ("+adapter.getVisibleCount()+")");
        ((TextView)findViewById(R.id.tvEmptyEvents)).setText(adapter.getVisibleCount()==0?"No events match this filter":"Latest matching events from up to 100 loaded records");
        LinearLayout row=findViewById(R.id.filterRow);
        for(int i=0;i<row.getChildCount();i++) {
            TextView chip=(TextView)row.getChildAt(i);boolean selected=filter.contentEquals(chip.getText());
            chip.setBackgroundResource(selected?R.drawable.bg_chip_filter_active:R.drawable.bg_chip_filter_inactive);
            chip.setTextColor(getColor(selected?android.R.color.white:R.color.text_secondary));chip.setSelected(selected);
        }
    }
}
