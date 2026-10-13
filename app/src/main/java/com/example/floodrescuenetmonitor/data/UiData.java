package com.example.floodrescuenetmonitor.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.example.floodrescuenetmonitor.data.MonitorState.*;

public final class UiData {
    private UiData() {}
    public static List<AlertEvent> alerts(List<Map<String, Object>> rows) {
        List<AlertEvent> items = new ArrayList<>();
        for (Map<String, Object> row : rows) items.add(new AlertEvent(text(row,"title","Event"), text(row,"severity","SYSTEM"), date(time(row,"timestamp")), text(row,"description","No description"), text(row,"category","System")));
        return items;
    }
    public static List<DeploymentLog> deployments(List<Map<String, Object>> rows) {
        List<DeploymentLog> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String status = text(row,"status","Pending");
            if (!java.util.Arrays.asList("Success","Failed","Pending").contains(status)) status = "Pending";
            Double cycle = number(row,"cycleMs");
            items.add(new DeploymentLog(text(row,"location","Tabletop demo"), date(time(row,"timestamp")), text(row,"initiator","Unknown"), status, "SIM-01", cycle == null ? "" : reading(cycle / 1000," s"), text(row,"error","")));
        }
        return items;
    }
}
