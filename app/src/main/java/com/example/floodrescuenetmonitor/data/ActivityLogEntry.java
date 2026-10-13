package com.example.floodrescuenetmonitor.data;

public class ActivityLogEntry {
    public final String time;
    public final String level;
    public final String trend;

    public ActivityLogEntry(String time, String level, String trend) {
        this.time = time;
        this.level = level;
        this.trend = trend;
    }
}
