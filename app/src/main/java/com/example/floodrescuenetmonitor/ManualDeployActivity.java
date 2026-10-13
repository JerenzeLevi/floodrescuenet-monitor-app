package com.example.floodrescuenetmonitor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.example.floodrescuenetmonitor.data.*;
import com.example.floodrescuenetmonitor.ui.BottomNavHelper;
import com.example.floodrescuenetmonitor.ui.adapter.DeployLogAdapter;
import java.util.*;
import static com.example.floodrescuenetmonitor.data.MonitorState.*;

public class ManualDeployActivity extends MonitorActivity {
    private final List<MaterialCheckBox> checks=new ArrayList<>();
    private SwitchMaterial autoMode;
    private boolean rendering,sending;
    private Object previousDeployments;
    @Override protected void onStart() { sending=false; super.onStart(); }
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);setContentView(R.layout.activity_manual_deploy);
        autoMode=findViewById(R.id.switchAutoMode);
        String[] labels={"I am controlling a simulation","I reviewed the current demo reading","I confirmed the demo net is ready","I understand reset means manual repacking"};
        int[] ids={R.id.check1,R.id.check2,R.id.check3,R.id.check4};
        for(int i=0;i<ids.length;i++) {
            MaterialCheckBox box=(MaterialCheckBox)findViewById(ids[i]);box.setText(labels[i]);
            box.setOnCheckedChangeListener((button,checked)->updateEnabled());checks.add(box);
        }
        autoMode.setOnCheckedChangeListener((button,checked)->{
            if(rendering)return;
            new AlertDialog.Builder(this).setTitle("Change demo mode?")
                .setMessage("Request "+(checked?"automatic":"manual")+" mode. The switch updates after the demo confirms it.")
                .setPositiveButton("Send request",(d,w)->send("SET_MODE",checked?"automatic":"manual"))
                .setNegativeButton("Cancel",null).setOnDismissListener(d->onMonitorState(state)).show();
        });
        findViewById(R.id.btnDeploy).setOnClickListener(v->showConfirmDialog());
        RecyclerView rv=findViewById(R.id.rvDeployLogs);rv.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.tvViewAllLogs).setOnClickListener(v->startActivity(new Intent(this,RecentDeploymentsActivity.class)));
        BottomNavHelper.setup(this,findViewById(R.id.bottomNav),BottomNavHelper.Tab.DEPLOY);
        findViewById(R.id.btnDeploy).setEnabled(false);autoMode.setEnabled(false);
    }
    private void showConfirmDialog() {
        String location=((EditText)findViewById(R.id.etLocation)).getText().toString().trim();
        View content=getLayoutInflater().inflate(R.layout.dialog_confirm_deploy,null);
        ((TextView)content.findViewById(R.id.tvConfirmLocation)).setText(location.isEmpty()?"the demo net":location);
        AlertDialog dialog=new AlertDialog.Builder(this).setView(content).create();
        content.findViewById(R.id.btnConfirmCancel).setOnClickListener(x->dialog.dismiss());
        content.findViewById(R.id.btnConfirmDeploy).setOnClickListener(x->{dialog.dismiss();send("DEPLOY",state.mode());});
        if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }
    private void send(String type,String mode) {
        if(repository==null)return;
        sending=true;updateEnabled();
        String location=((EditText)findViewById(R.id.etLocation)).getText().toString();
        repository.request(type,location,mode,(ok,message)->{sending=false;message(message);onMonitorState(state);});
    }
    private void updateEnabled() {
        boolean all=true;for(MaterialCheckBox box:checks)all&=box.isChecked();
        boolean ready=repository!=null&&!sending&&state.canRequest();
        findViewById(R.id.btnDeploy).setEnabled(ready&&all&&"ready".equals(state.net()));
        autoMode.setEnabled(ready);
    }
    @Override protected void onMonitorState(MonitorState s) {
        rendering=true;autoMode.setChecked("automatic".equals(s.mode()));rendering=false;
        ((TextView)findViewById(R.id.tvUnit)).setText("DEMO · SIM-01");
        ((TextView)findViewById(R.id.tvCommandStatus)).setText(s.commandText());
        bindVital(R.id.vitalBattery,R.drawable.ic_battery_full,"BATTERY",reading(number(s.telemetry,"batteryPercent"),"%"));
        bindVital(R.id.vitalNetwork,R.drawable.ic_signal_cellular_alt,"LATENCY",reading(number(s.telemetry,"latencyMs")," ms"));
        bindVital(R.id.vitalTemp,R.drawable.ic_thermostat,"CORE TEMP",reading(number(s.telemetry,"coreTempC")," °C"));
        bindVital(R.id.vitalAlignment,R.drawable.ic_explore,"ALIGNMENT",text(s.telemetry,"alignment","Not reported"));
        if(previousDeployments!=s.deployments){previousDeployments=s.deployments;List<DeploymentLog> logs=UiData.deployments(s.deployments);((RecyclerView)findViewById(R.id.rvDeployLogs)).setAdapter(new DeployLogAdapter(logs.subList(0,Math.min(3,logs.size()))));}
        updateEnabled();
    }
    private void bindVital(int id,int icon,String label,String value) {
        View card=findViewById(id);((ImageView)card.findViewById(R.id.ivIcon)).setImageResource(icon);
        ((TextView)card.findViewById(R.id.tvTag)).setText("SIMULATION");
        ((TextView)card.findViewById(R.id.tvLabel)).setText(label);((TextView)card.findViewById(R.id.tvValue)).setText(value);
    }
}
