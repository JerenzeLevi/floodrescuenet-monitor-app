// Pure transaction transformation: copies current records and removes originals atomically.
export const RETENTION_MS=24*60*60*1000;
export function archiveBatch(session,now,limit=200){
 if(!session || typeof session!=='object')return undefined;
 const next=structuredClone(session);let moved=0;
 for(const kind of ['events','readings','deployments']){
  const eligible=Object.entries(next[kind]||{}).filter(([,v])=>Number.isFinite(v?.timestamp)&&v.timestamp>0&&v.timestamp<=now-RETENTION_MS).sort((a,b)=>a[1].timestamp-b[1].timestamp||a[0].localeCompare(b[0]));
  for(const [id,value] of eligible){
   if(moved>=limit)break;
   next.archive??={};next.archive[kind]??={};
   // Existing archived ID must match before removing a duplicate; conflicts stay visible.
   if(next.archive[kind][id] && JSON.stringify(next.archive[kind][id])!==JSON.stringify(value))continue;
   next.archive[kind][id]=value;delete next[kind][id];moved++;
  }
 }
 next.archiveStatus={lastRun:now,cutoff:now-RETENTION_MS,movedThisBatch:moved,retentionHours:24};
 return next;
}
