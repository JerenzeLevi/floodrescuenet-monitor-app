package com.example.floodrescuenetmonitor.data;
/** Bounded classroom prototype settings. Device firmware must impose its own mechanical limits. */
public final class DeploymentOptions {
    public final int durationMs, thrustPercent;
    public DeploymentOptions(int durationMs,int thrustPercent) {
        if(durationMs<1000 || durationMs>10000 || thrustPercent<10 || thrustPercent>100)
            throw new IllegalArgumentException("Duration must be 1–10 seconds; thrust must be 10–100%.");
        this.durationMs=durationMs;this.thrustPercent=thrustPercent;
    }
}
