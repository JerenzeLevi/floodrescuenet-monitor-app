package com.example.floodrescuenetmonitor.data;

public class HistoryEntry {
    public final String title;
    public final String time;
    public final String status;

    public HistoryEntry(String title, String time, String status) {
        this.title = title;
        this.time = time;
        this.status = status;
    }
}
