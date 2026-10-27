package com.example.floodrescuenetmonitor;
import android.content.Intent;
import android.os.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.floodrescuenetmonitor.data.SessionSettings;
import com.example.floodrescuenetmonitor.data.MonitorState;
/** Firebase-first startup. A cloud connection is not proof of a healthy sensor. */
public class SplashActivity extends AppCompatActivity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private DatabaseReference connection;private ValueEventListener listener;private boolean routed, checking;
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);setContentView(R.layout.activity_splash);
        connection=FirebaseDatabase.getInstance().getReference(".info/connected");
        listener=new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot s){if(Boolean.TRUE.equals(s.getValue())){checkSensor();}}
            @Override public void onCancelled(@NonNull DatabaseError error){route();}
        };connection.addValueEventListener(listener);handler.postDelayed(()->route(),5000);
    }
    private void show(String text){if(!routed&&!isDestroyed())((TextView)findViewById(R.id.tvStartupStatus)).setText(text);}
    private void checkSensor(){
        if(checking||routed)return;checking=true;
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        if(user==null){show("FIREBASE CONNECTED · SIGN IN TO CHECK SENSOR LINK");handler.postDelayed(this::route,1000);return;}
        boolean device=SessionSettings.device(this,user.getUid());
        show(device?"FIREBASE CONNECTED · CHECKING ESP32 SENSOR LINK (UP TO 5s)":"FIREBASE CONNECTED · CHECKING DEMO SOURCE");
        FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("verified").get().addOnCompleteListener(profile->{
            if(routed)return;
            if(!profile.isSuccessful()||!Boolean.TRUE.equals(profile.getResult().getValue())){show("ACCOUNT VERIFICATION REQUIRED");handler.postDelayed(this::route,1000);return;}
            FirebaseDatabase.getInstance().getReference(device?"deviceSessions":"demoSessions").child(user.getUid()).child("telemetry").get().addOnCompleteListener(result->{
                if(routed)return;
                MonitorState state=new MonitorState();state.expectedSource=device?"esp32":"simulation";
                if(result.isSuccessful())state.telemetry=MonitorState.map(result.getResult().getValue());
                boolean ready=state.fresh()&&(!device||Boolean.TRUE.equals(state.telemetry.get("sensorReady")));
                show(ready?(device?"ESP32 SENSOR LINK READY":"DEMO SOURCE READY"):(device?"ESP32 SENSOR NOT READY · CHECK CONNECTION / CALIBRATION":"WAITING FOR DEMO SOURCE"));
                handler.postDelayed(this::route,1200);
            });
        });
    }
    private void route(){if(routed||isFinishing()||isDestroyed())return;routed=true;startActivity(new Intent(this,LoginActivity.class));finish();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(connection!=null&&listener!=null)connection.removeEventListener(listener);super.onDestroy();}
}
