package com.example.floodrescuenetmonitor.ui;

import android.app.Activity;
import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.floodrescuenetmonitor.AlertsHistoryActivity;
import com.example.floodrescuenetmonitor.DashboardActivity;
import com.example.floodrescuenetmonitor.LiveMonitoringActivity;
import com.example.floodrescuenetmonitor.ManualDeployActivity;
import com.example.floodrescuenetmonitor.R;

/**
 * Wires a BottomNavigationView to the Activity-swap navigation pattern used by
 * Dashboard / LiveMonitoring / AlertsHistory / ManualDeploy, replicating the exact
 * asymmetric finish() behavior from the original Compose navigation graph.
 */
public final class BottomNavHelper {

    public enum Tab { HOME, LIVE, ALERTS, DEPLOY, PROFILE }

    private BottomNavHelper() {}

    public static void setup(Activity activity, BottomNavigationView nav, Tab current) {
        switch (current) {
            case HOME: nav.setSelectedItemId(R.id.nav_home); break;
            case LIVE: nav.setSelectedItemId(R.id.nav_live); break;
            case ALERTS: nav.setSelectedItemId(R.id.nav_alerts); break;
            case DEPLOY: nav.setSelectedItemId(R.id.nav_deploy); break;
            case PROFILE: nav.getMenu().setGroupCheckable(0, false, true); break; // not a tab: nothing highlighted
        }

        // Only Dashboard (Home) keeps itself on the back stack when navigating away; the other
        // three tabs replace themselves (finish) when switching, per the original graph.
        boolean selfFinishes = current != Tab.HOME;

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                if (current != Tab.HOME) {
                    activity.startActivity(new Intent(activity, DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    if (selfFinishes) activity.finish();
                }
                return true;
            } else if (id == R.id.nav_live) {
                if (current != Tab.LIVE) {
                    activity.startActivity(new Intent(activity, LiveMonitoringActivity.class));
                    if (selfFinishes) activity.finish();
                }
                return true;
            } else if (id == R.id.nav_alerts) {
                if (current != Tab.ALERTS) {
                    activity.startActivity(new Intent(activity, AlertsHistoryActivity.class));
                    if (selfFinishes) activity.finish();
                }
                return true;
            } else if (id == R.id.nav_deploy) {
                if (current != Tab.DEPLOY) {
                    activity.startActivity(new Intent(activity, ManualDeployActivity.class));
                    if (selfFinishes) activity.finish();
                }
                return true;
            }
            return false;
        });
    }
}
