import {initializeApp} from 'firebase-admin/app';
import {getDatabase} from 'firebase-admin/database';
import {onSchedule} from 'firebase-functions/v2/scheduler';
import {archiveBatch} from './archive-core.mjs';
initializeApp();
// Deployment is a separate project-owner action. No billing or cloud setup is changed by this ZIP.
export const archiveMonitorHistory=onSchedule({schedule:'every 15 minutes',timeZone:'Asia/Manila',region:'asia-southeast1',timeoutSeconds:540,maxInstances:1},async()=>{
 const database=getDatabase();const users=await database.ref('users').get();
 for(const uid of Object.keys(users.val()||{})){
  for(const branch of ['demoSessions','deviceSessions']){
   const session=database.ref(`${branch}/${uid}`);
   // Bounded work; a large backlog is drained by later scheduled runs.
   for(let batch=0;batch<10;batch++){
    const now=Date.now();let moved=0;
    const result=await session.transaction(current=>{
     const next=archiveBatch(current,now);moved=next?.archiveStatus?.movedThisBatch||0;return next;
    },undefined,false);
    if(!result.committed||moved<200)break;
   }
  }
 }
});
