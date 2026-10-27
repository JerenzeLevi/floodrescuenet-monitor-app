package com.example.floodrescuenetmonitor.data;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
/** Device-local bounded audit. No email, password or token is stored. Not tamper-proof. */
public final class SecurityAudit {
    public static synchronized void record(Context c,String reason) {
        try {
            JSONArray previous=new JSONArray(c.getSharedPreferences("access_audit",0).getString("events","[]"));
            JSONArray next=new JSONArray();
            next.put(new JSONObject().put("timestamp",System.currentTimeMillis()).put("reason",reason));
            for(int i=0;i<Math.min(previous.length(),199);i++)next.put(previous.get(i));
            c.getSharedPreferences("access_audit",0).edit().putString("events",next.toString()).apply();
        } catch(org.json.JSONException ignored) { }
    }
    public static String summary(Context c) {
        StringBuilder out=new StringBuilder("Last 200 access events on this phone. Device-local; clearing app data removes them. No credentials are recorded.\n\n");
        try {
            JSONArray rows=new JSONArray(c.getSharedPreferences("access_audit",0).getString("events","[]"));
            if(rows.length()==0)out.append("No recorded attempts.");
            for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);out.append(MonitorState.date(row.getLong("timestamp"))).append(" · ").append(row.getString("reason")).append("\n");}
        } catch(org.json.JSONException ignored) {out.append("Could not read local audit.");}
        return out.toString();
    }
}
