package com.example.floodrescuenetmonitor;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.floodrescuenetmonitor.data.*;
import com.example.floodrescuenetmonitor.ui.adapter.*;
import java.util.*;
/** Full, paginated source history. Refresh starts a new snapshot; latest screens remain live. */
public class HistoryBrowserActivity extends MonitorActivity {
    private HistoryPager pager;private MonitorRepository bound;
    private RecyclerView list;private Button more;private TextView status;private String kind;
    private List<Map<String,Object>> rows=new ArrayList<>();private String filter="All";private boolean end;private boolean archived;private Button archiveToggle;
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);kind=getIntent().getStringExtra("kind");
        if(!Arrays.asList("readings","events","deployments").contains(kind))kind="events";
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(20,30,20,20);page.setFitsSystemWindows(true);page.setBackgroundColor(getColor(R.color.surface_light));
        Button back=new Button(this);back.setText("‹ Back · "+kind);back.setOnClickListener(v->finish());page.addView(back);
        if("events".equals(kind)){
            Spinner filters=new Spinner(this);String[] names={"All","Danger","Net Deployments","System"};filters.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
            filters.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int n,long id){filter=names[n];render();}});page.addView(filters);
        }
        status=new TextView(this);status.setPadding(12,12,12,12);page.addView(status);
        list=new RecyclerView(this);list.setLayoutManager(new LinearLayoutManager(this));page.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        archiveToggle=new Button(this);archiveToggle.setText("Open cloud archive (older than 24h)");archiveToggle.setOnClickListener(v->{
            if(repository==null||!state.connected)return;archived=!archived;archiveToggle.setText(archived?"Return to current history":"Open cloud archive (older than 24h)");startPager();
        });page.addView(archiveToggle);
        more=new Button(this);more.setText("Load older records");more.setOnClickListener(v->{if(pager!=null&&state.connected){more.setEnabled(false);pager.next();}});page.addView(more);
        Button refresh=new Button(this);refresh.setText("Refresh history");refresh.setOnClickListener(v->{if(repository!=null&&state.connected)startPager();});page.addView(refresh);setContentView(page);
    }
    private void startPager(){
        if(pager!=null)pager.close();rows=new ArrayList<>();end=false;render();
        pager=new HistoryPager(repository.session().child(archived?"archive/"+kind:kind),(values,finished,error)->{
            if(!state.connected){status.setText("Offline — reconnect and refresh to verify the full history.");more.setEnabled(false);return;}
            rows=values;end=finished;render();
            if(!error.isEmpty())status.setText("Could not load history: "+error+". Retry when connected.");
        });more.setEnabled(false);pager.next();
    }
    private void render(){
        if(list==null)return;
        android.os.Parcelable scroll=list.getLayoutManager().onSaveInstanceState();
        if("events".equals(kind)){AlertAdapter a=new AlertAdapter(UiData.alerts(rows));a.filter(filter);list.setAdapter(a);}
        else if("deployments".equals(kind))list.setAdapter(new DeploymentLogAdapter(UiData.deployments(rows)));
        else {List<ActivityLogEntry> entries=new ArrayList<>();for(Map<String,Object> row:rows)entries.add(new ActivityLogEntry(MonitorState.date(MonitorState.time(row,"timestamp")),MonitorState.reading(MonitorState.number(row,"waterCm")," cm"),MonitorState.text(row,"trend","—")));list.setAdapter(new ActivityLogAdapter(entries));}
        list.getLayoutManager().onRestoreInstanceState(scroll);
        status.setText(rows.size()+" records loaded · "+(end?(archived?"End of archived records":"End of current records; see cloud archive"):"More records available")+" · "+state.sourceLabel());
        more.setVisibility(end?View.GONE:View.VISIBLE);more.setEnabled(state.connected);
    }
    @Override protected void onMonitorState(MonitorState s){if(repository!=null && s.connected && repository!=bound){bound=repository;startPager();}if(more!=null)more.setEnabled(s.connected);if(status!=null&&!s.connected)status.setText("Offline — reconnect and refresh history.");}
    @Override protected void onStop(){if(pager!=null)pager.close();bound=null;super.onStop();}
}
