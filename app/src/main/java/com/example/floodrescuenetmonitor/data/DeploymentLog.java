package com.example.floodrescuenetmonitor.data;

public class DeploymentLog {
    public final String location;
    public final String time;
    public final String initiator;
    public final String status;
    public final String unitId;
    public final String cycleTime;
    public final String errorMessage;

    public DeploymentLog(String location, String time, String initiator, String status) {
        this(location, time, initiator, status, "", "", null);
    }

    public DeploymentLog(String location, String time, String initiator, String status, String unitId) {
        this(location, time, initiator, status, unitId, "", null);
    }

    public DeploymentLog(String location, String time, String initiator, String status, String unitId, String cycleTime) {
        this(location, time, initiator, status, unitId, cycleTime, null);
    }

    public DeploymentLog(String location, String time, String initiator, String status,
                          String unitId, String cycleTime, String errorMessage) {
        this.location = location;
        this.time = time;
        this.initiator = initiator;
        this.status = status;
        this.unitId = unitId == null ? "" : unitId;
        this.cycleTime = cycleTime == null ? "" : cycleTime;
        this.errorMessage = errorMessage;
    }

    public static DeploymentLog withError(String location, String time, String initiator, String status,
                                           String unitId, String errorMessage) {
        return new DeploymentLog(location, time, initiator, status, unitId, "", errorMessage);
    }
}
