import com.example.floodrescuenetmonitor.data.*;
import java.util.*;
public class MonitorLogicTest {
    static int checks;
    static void check(boolean condition){checks++;if(!condition)throw new AssertionError("Check "+checks);}
    static MonitorState state(String source){MonitorState s=new MonitorState();s.expectedSource=source;s.telemetry=new HashMap<>();s.telemetry.put("source",source);s.telemetry.put("schemaVersion",1);s.telemetry.put("waterCm",10);s.telemetry.put("warningCm",10);s.telemetry.put("dangerCm",16);s.telemetry.put("net","ready");s.telemetry.put("mode","manual");s.telemetry.put("updatedAt",100000L);return s;}
    public static void main(String[] args){
        MonitorState demo=state("simulation");check(demo.valid());check(demo.level().equals("WARNING"));check(demo.freshAt(115000));check(!demo.freshAt(115001));check(!demo.freshAt(94000));
        demo.telemetry.put("source","esp32");check(!demo.valid());
        MonitorState device=state("esp32");check(device.valid());device.telemetry.put("source","simulation");check(!device.valid());
        device.telemetry.put("source","esp32");device.telemetry.put("net","retracting");check(device.valid());device.telemetry.put("net","nonsense");check(!device.valid());
        for(int[] valid:new int[][]{{1000,10},{10000,100},{5000,50}}){DeploymentOptions o=new DeploymentOptions(valid[0],valid[1]);check(o.durationMs==valid[0]);}
        for(int[] invalid:new int[][]{{999,50},{10001,50},{5000,9},{5000,101}}){try{new DeploymentOptions(invalid[0],invalid[1]);throw new AssertionError("Accepted invalid configuration");}catch(IllegalArgumentException expected){checks++;}}
        long midnight=java.time.Instant.parse("2026-10-12T16:00:00Z").toEpochMilli();
        check(MonitorState.dayStart(midnight)==midnight);check(MonitorState.dayStart(midnight+86399999)==midnight);check(MonitorState.dayStart(midnight-1)==midnight-86400000L);
        Map<String,Object> row=new HashMap<>();row.put("unitId","ESP32-01");row.put("status","Failed");row.put("error","Limit switch timeout");
        DeploymentLog log=UiData.deployments(Collections.singletonList(row)).get(0);check(log.unitId.equals("ESP32-01"));check(log.errorMessage.equals("Limit switch timeout"));
        device.telemetry.put("net","unarmed");check(device.valid());device.telemetry.put("net","diagnosing");check(device.valid());device.telemetry.put("net","ready");device.telemetry.put("updatedAt",System.currentTimeMillis());device.connected=true;check(!device.canRequest());device.telemetry.put("sensorReady",true);check(device.canRequest());
        device.command=new HashMap<>();device.command.put("status","pending");device.command.put("expiresAt",100100L);check(device.pendingAt(100099));check(!device.pendingAt(100100));
        System.out.println("PASS: "+checks+" source, timing, options, history and expiry checks");
    }
}
