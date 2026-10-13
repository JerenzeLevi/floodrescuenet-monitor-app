package com.example.floodrescuenetmonitor.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Shared presentation state. Missing measurements stay unknown, never zero. */
public final class MonitorState {
    public static final long STALE_MS = 15000;
    public boolean connected;
    public long serverOffset;
    public String error = "";
    public Map<String, Object> telemetry = Collections.emptyMap();
    public Map<String, Object> command = Collections.emptyMap();
    public List<Map<String, Object>> events = new ArrayList<>();
    public List<Map<String, Object>> readings = new ArrayList<>();
    public List<Map<String, Object>> deployments = new ArrayList<>();
    public List<Map<String, Object>> contacts = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }
    public static String text(Map<String, Object> value, String key, String fallback) {
        Object item = value.get(key);
        return item instanceof String && !((String) item).isEmpty() ? (String) item : fallback;
    }
    public static Double number(Map<String, Object> value, String key) {
        Object item = value.get(key);
        if (!(item instanceof Number)) return null;
        double n = ((Number) item).doubleValue();
        return Double.isFinite(n) ? n : null;
    }
    public static long time(Map<String, Object> value, String key) {
        Double n = number(value, key);
        return n == null || n <= 0 ? 0 : n.longValue();
    }
    public static String date(long value) {
        return value <= 0 ? "Not reported" : new SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()).format(new Date(value));
    }
    public long now() { return System.currentTimeMillis() + serverOffset; }
    public Double water() {
        Double water = number(telemetry, "waterCm");
        return water != null && water >= 0 && water <= 20 ? water : null;
    }
    public double warning() { Double n = number(telemetry, "warningCm"); return n == null ? 10 : n; }
    public double danger() { Double n = number(telemetry, "dangerCm"); return n == null ? 16 : n; }
    public boolean valid() {
        return water() != null && "simulation".equals(text(telemetry, "source", ""))
                && Double.valueOf(1).equals(number(telemetry, "schemaVersion"))
                && warning() > 0 && warning() < danger() && danger() <= 20
                && java.util.Arrays.asList("ready", "deploying", "deployed").contains(net())
                && java.util.Arrays.asList("automatic", "manual").contains(mode());
    }
    public String net() { return text(telemetry, "net", "unknown"); }
    public String mode() { return text(telemetry, "mode", "unknown"); }
    public long updatedAt() { return time(telemetry, "updatedAt"); }
    public boolean freshAt(long now) {
        long age = now - updatedAt();
        return valid() && updatedAt() > 0 && age >= -5000 && age <= STALE_MS;
    }
    public boolean fresh() { return freshAt(now()); }
    public String level() {
        if (!valid()) return "UNKNOWN";
        return water() >= danger() ? "DANGER" : water() >= warning() ? "WARNING" : "NORMAL";
    }
    public boolean pendingAt(long now) {
        String status = text(command, "status", "");
        return ("pending".equals(status) || "accepted".equals(status)) && time(command, "expiresAt") > now;
    }
    public boolean canRequest() { return connected && fresh() && error.isEmpty() && !pendingAt(now()); }
    public String connectionText() {
        if (!error.isEmpty()) return "DEMO · " + error;
        if (!connected) return "DEMO · Offline — commands unavailable";
        if (telemetry.isEmpty()) return "DEMO · Connected; waiting for a data source";
        if (!valid()) return "DEMO · Invalid or unsupported telemetry";
        if (!fresh()) return "DEMO · Stale reading — last update " + date(updatedAt());
        return "DEMO · Receiving updates · " + date(updatedAt());
    }
    public String commandText() {
        if (command.isEmpty()) return "No request sent";
        String status = text(command, "status", "unknown");
        if (("pending".equals(status) || "accepted".equals(status)) && !pendingAt(now())) status = "timed out — result not confirmed";
        return text(command, "type", "Request") + " · " + status + "\n" + text(command, "message", "");
    }
    public String trend() {
        if (readings.size() < 2) return "NO TREND";
        Map<String, Object> latest = readings.get(0), previous = readings.get(1);
        Double a = number(latest, "waterCm"), b = number(previous, "waterCm");
        long dt = time(latest, "timestamp") - time(previous, "timestamp");
        if (a == null || b == null || dt <= 0 || dt > 60000) return "NO TREND";
        double change = (a - b) * 60000 / dt;
        return change > .3 ? "RISING" : change < -.3 ? "RECEDING" : "STABLE";
    }
    public static List<Map<String, Object>> sorted(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : map(value).entrySet()) {
            Map<String, Object> item = map(entry.getValue());
            if (!item.isEmpty()) result.add(item);
        }
        result.sort(Comparator.comparingLong((Map<String, Object> item) -> time(item, "timestamp")).reversed());
        return result;
    }
    public static String reading(Double value, String unit) {
        return value == null ? "Not reported" : String.format(Locale.getDefault(), "%.1f%s", value, unit);
    }
}
