package com.example.floodrescuenetmonitor.data;

import java.util.Arrays;
import java.util.List;

public final class SampleData {

    private SampleData() {}

    public static final List<EmergencyContact> emergencyContacts = Arrays.asList(
            new EmergencyContact("911 National Emergency", "PRIMARY EMERGENCY HOTLINE", "9-1-1"),
            new EmergencyContact("Barangay Hall", "LOCAL COORDINATION CENTER", "(02) 8123-4567"),
            new EmergencyContact("MDRRMO Office", "MUNICIPAL DISASTER RISK REDUCTION", "0917-555-0199"),
            new EmergencyContact("Local Rescue Team", "DEPLOYMENT & WATER RESCUE", "0918-777-9000")
    );

    public static final List<HistoryEntry> dashboardHistory = Arrays.asList(
            new HistoryEntry("Flood Alert", "Today, 08:45 AM", "Resolved"),
            new HistoryEntry("System Check", "Today, 07:30 AM", "Passed"),
            new HistoryEntry("Power Outage", "Today, 06:12 AM", "Pending")
    );

    public static final List<ActivityLogEntry> liveActivity = Arrays.asList(
            new ActivityLogEntry("14:35", "48 cm", "RISING"),
            new ActivityLogEntry("14:30", "46 cm", "RISING"),
            new ActivityLogEntry("14:25", "45 cm", "STABLE"),
            new ActivityLogEntry("14:20", "45 cm", "RECEDING"),
            new ActivityLogEntry("14:15", "47 cm", "RECEDING")
    );

    public static final List<AlertEvent> alertEvents = Arrays.asList(
            new AlertEvent(
                    "Critical Water Level", "DANGER", "Oct 24, 2023 • 02:15 PM",
                    "Water level reached 85cm at San Roque Bridge. Automated systems on high alert.",
                    "Danger"
            ),
            new AlertEvent(
                    "Net System Deployed", "DEPLOYED", "Oct 24, 2023 • 01:45 PM",
                    "Flood rescue net successfully deployed at Sector B. Sensors confirming tension.",
                    "Net Deployments"
            ),
            new AlertEvent(
                    "Rising Water Alert", "WARNING", "Oct 24, 2023 • 12:30 PM",
                    "Sudden increase of 15cm detected in 10 mins. Continued monitoring required.",
                    "Danger"
            ),
            new AlertEvent(
                    "System Self-Test", "SYSTEM OK", "Oct 24, 2023 • 08:00 AM",
                    "Morning ESP32 diagnostic completed. All net motors and sensors functional.",
                    "System"
            )
    );

    public static final List<DeploymentLog> deployLogs = Arrays.asList(
            new DeploymentLog("Brgy manga-1", "Today, 09:42 AM", "Manual Deployment", "SUCCESS"),
            new DeploymentLog("Brgy Pag-asa", "Yesterday, 14:20 PM", "Manual Deployment", "FAILED"),
            new DeploymentLog("Atmospheric Drone X", "2 days ago, 11:15 AM", "Manual Deployment", "SUCCESS")
    );

    public static final List<DeploymentLog> recentDeployments = Arrays.asList(
            new DeploymentLog("Zone B-12 (River North)", "10:45 AM", "Manual Deployment", "Success",
                    "FG-042", "1m 24s"),
            DeploymentLog.withError("Zone A-04 (Main Dam)", "08:12 AM", "System Deployment", "Failed",
                    "FG-018", "Motor Jammed - Mechanical Obstruction detected in Net Rail."),
            new DeploymentLog("Zone C-02 (East Creek)", "07:30 AM", "System Deployment", "Pending",
                    "FG-033"),
            new DeploymentLog("Zone B-12 (River North)", "Yesterday, 11:50 PM", "Manual Deployment", "Success",
                    "FG-042", "1m 30s")
    );
}
