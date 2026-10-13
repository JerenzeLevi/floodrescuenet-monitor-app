package com.example.floodrescuenetmonitor;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.floodrescuenetmonitor.data.*;
import com.example.floodrescuenetmonitor.ui.adapter.DeploymentLogAdapter;
import java.util.List;

public class RecentDeploymentsActivity extends MonitorActivity {
    private Object previousDeployments;
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);setContentView(R.layout.activity_recent_deployments);
        findViewById(R.id.btnBack).setOnClickListener(v->finish());
        ((RecyclerView)findViewById(R.id.rvDeployments)).setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.tvRefresh).setOnClickListener(v->recreate());
    }
    @Override protected void onMonitorState(MonitorState s) {
        if(previousDeployments==s.deployments)return;
        previousDeployments=s.deployments;List<DeploymentLog> logs=UiData.deployments(s.deployments);
        int success=0,failed=0;
        for(DeploymentLog log:logs){if("Success".equals(log.status))success++;if("Failed".equals(log.status))failed++;}
        bindSummary(R.id.summaryTotal,"TOTAL",logs.size());bindSummary(R.id.summarySuccess,"SUCCESS",success);bindSummary(R.id.summaryFailed,"FAILED",failed);
        ((TextView)findViewById(R.id.tvPeriod)).setText(logs.isEmpty()?"No deployments recorded":"Loaded records (up to 100)");
        ((RecyclerView)findViewById(R.id.rvDeployments)).setAdapter(new DeploymentLogAdapter(logs));
    }
    private void bindSummary(int id,String label,int number) {
        View card=findViewById(id);((TextView)card.findViewById(R.id.tvLabel)).setText(label);((TextView)card.findViewById(R.id.tvValue)).setText(String.valueOf(number));
        boolean failed=id==R.id.summaryFailed,total=id==R.id.summaryTotal;
        com.google.android.material.card.MaterialCardView cv=(com.google.android.material.card.MaterialCardView)card;
        cv.setCardBackgroundColor(getColor(failed?R.color.red_danger_bg:total?R.color.ad_hoc_f3f3f8:R.color.white));
        cv.setStrokeWidth((int)getResources().getDisplayMetrics().density);
        cv.setStrokeColor(getColor(failed?R.color.red_danger_bg:R.color.card_border));
        if(failed){((TextView)card.findViewById(R.id.tvLabel)).setTextColor(getColor(R.color.red_danger));((TextView)card.findViewById(R.id.tvValue)).setTextColor(getColor(R.color.red_danger));}
    }
}
