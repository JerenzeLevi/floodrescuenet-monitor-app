package com.example.floodrescuenetmonitor.data;
import android.content.Context;
/** Source selection is per signed-in account, never inferred from incoming values. */
public final class SessionSettings {
    public static boolean device(Context c, String uid) { return c.getSharedPreferences("monitor_sources",0).getBoolean(uid,false); }
    public static void setDevice(Context c,String uid,boolean device) { c.getSharedPreferences("monitor_sources",0).edit().putBoolean(uid,device).apply(); }
}
