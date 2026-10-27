package com.example.floodrescuenetmonitor.data;
import androidx.annotation.NonNull;
import com.google.firebase.database.*;
import java.util.*;
/** Stable timestamp + key cursor: equal-time records cannot disappear between pages. */
public final class HistoryPager {
    public interface Result { void received(List<Map<String,Object>> rows, boolean end, String error); }
    private final DatabaseReference ref; private final Result result;
    private final LinkedHashMap<String,Map<String,Object>> loaded=new LinkedHashMap<>();
    private String cursor;private double timestamp;private boolean busy,end,closed;
    public HistoryPager(DatabaseReference ref,Result result){this.ref=ref;this.result=result;}
    public void close(){closed=true;}
    public void next(){
        if(busy||end||closed)return;busy=true;
        Query q=ref.orderByChild("timestamp");
        if(cursor!=null)q=q.endAt(timestamp,cursor);
        final String previous=cursor;
        q.limitToLast(previous==null?101:102).get().addOnSuccessListener(snapshot->{
            if(closed)return;
            List<DataSnapshot> batch=new ArrayList<>();
            for(DataSnapshot item:snapshot.getChildren())if(!item.getKey().equals(previous))batch.add(item);
            end= batch.size()<=100;
            while(batch.size()>100)batch.remove(0); // one look-ahead row remains for the next page
            if(!batch.isEmpty()) {DataSnapshot oldest=batch.get(0);cursor=oldest.getKey();timestamp=MonitorState.time(MonitorState.map(oldest.getValue()),"timestamp");}
            Collections.reverse(batch);
            for(DataSnapshot item:batch)loaded.put(item.getKey(),MonitorState.map(item.getValue()));
            busy=false;result.received(new ArrayList<>(loaded.values()),end,"");
        }).addOnFailureListener(e->{if(!closed){busy=false;result.received(new ArrayList<>(loaded.values()),false,e.getMessage());}});
    }
}
