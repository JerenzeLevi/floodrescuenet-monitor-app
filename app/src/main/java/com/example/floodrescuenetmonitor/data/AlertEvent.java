package com.example.floodrescuenetmonitor.data;

public class AlertEvent {
    public final String title;
    public final String tag;
    public final String time;
    public final String description;
    public final String category;

    public AlertEvent(String title, String tag, String time, String description, String category) {
        this.title = title;
        this.tag = tag;
        this.time = time;
        this.description = description;
        this.category = category;
    }
}
