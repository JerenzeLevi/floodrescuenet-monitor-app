package com.example.floodrescuenetmonitor.data;

import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Observes an authenticated user's isolated demo session; never addresses hardware. */
public final class MonitorRepository {
    public interface Completion { void done(boolean ok, String message); }
    private final String uid;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference session;
    private final List<Runnable> subscriptions = new ArrayList<>();
    private final Map<String, String> errors = new HashMap<>();
    private final MonitorState state = new MonitorState();
    private Consumer<MonitorState> listener;
    private boolean active;
    private boolean sending;

    public MonitorRepository(String uid) {
        this.uid = uid;
        session = database.getReference("demoSessions").child(uid);
    }
    public void start(Consumer<MonitorState> listener) {
        this.listener = listener;
        active = true;
        observe("connection", database.getReference(".info/connected"), s -> state.connected = Boolean.TRUE.equals(s.getValue()));
        observe("clock", database.getReference(".info/serverTimeOffset"), s -> {
            Object n = s.getValue(); state.serverOffset = n instanceof Number ? ((Number) n).longValue() : 0;
        });
        observe("telemetry", session.child("telemetry"), s -> state.telemetry = MonitorState.map(s.getValue()));
        observe("command", session.child("command"), s -> state.command = MonitorState.map(s.getValue()));
        observe("events", session.child("events").orderByChild("timestamp").limitToLast(100), s -> state.events = MonitorState.sorted(s.getValue()));
        observe("readings", session.child("readings").orderByChild("timestamp").limitToLast(60), s -> state.readings = MonitorState.sorted(s.getValue()));
        observe("deployments", session.child("deployments").orderByChild("timestamp").limitToLast(100), s -> state.deployments = MonitorState.sorted(s.getValue()));
        observe("contacts", session.child("contacts").limitToFirst(20), s -> state.contacts = MonitorState.sorted(s.getValue()));
    }
    private void observe(String name, Query query, Consumer<DataSnapshot> apply) {
        ValueEventListener valueListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!active) return;
                errors.remove(name);
                apply.accept(snapshot);
                publish();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (!active) return;
                errors.put(name, name + ": " + error.getMessage());
                publish();
            }
        };
        query.addValueEventListener(valueListener);
        subscriptions.add(() -> query.removeEventListener(valueListener));
    }
    private void publish() {
        state.error = errors.isEmpty() ? "" : errors.values().iterator().next();
        if (active) listener.accept(state);
    }
    public void close() {
        active = false;
        for (Runnable remove : subscriptions) remove.run();
        subscriptions.clear();
    }
    public void request(String type, String location, String mode, Completion completion) {
        if (!active || sending || !state.canRequest()) {
            completion.done(false, "Wait for fresh demo data and the previous request to finish."); return;
        }
        if (!java.util.Arrays.asList("DEPLOY", "SET_MODE", "DIAGNOSTIC").contains(type)) {
            completion.done(false, "Unsupported request."); return;
        }
        if ("DEPLOY".equals(type) && !"ready".equals(state.net())) {
            completion.done(false, "The demo net is not ready. Repack it in the demo first."); return;
        }
        if (location == null || location.trim().isEmpty() || location.length() > 80
                || !java.util.Arrays.asList("automatic", "manual").contains(mode)) {
            completion.done(false, "Enter a location of 1–80 characters and a valid mode."); return;
        }
        long requestedAt = state.now();
        Map<String, Object> request = new HashMap<>();
        request.put("id", UUID.randomUUID().toString());
        request.put("requestedBy", uid);
        request.put("target", "simulation");
        request.put("type", type);
        request.put("status", "pending");
        request.put("location", location.trim());
        request.put("mode", mode);
        request.put("createdAt", ServerValue.TIMESTAMP);
        request.put("expiresAt", requestedAt + 30000);
        sending = true;
        // One command slot, with a transaction, prevents two phones from queuing duplicates.
        session.child("command").runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData data) {
                Map<String, Object> current = MonitorState.map(data.getValue());
                String status = MonitorState.text(current, "status", "");
                boolean busy = ("pending".equals(status) || "accepted".equals(status))
                        && MonitorState.time(current, "expiresAt") > state.now();
                if (busy || state.now() >= requestedAt + 30000) return Transaction.abort();
                data.setValue(request);
                return Transaction.success(data);
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                sending = false;
                if (!active) return;
                completion.done(error == null && committed, error != null ? error.getMessage()
                        : committed ? "Request saved. Waiting for the demo to acknowledge it."
                        : "Another request is active, or this request expired.");
            }
        }, false);
    }
}
