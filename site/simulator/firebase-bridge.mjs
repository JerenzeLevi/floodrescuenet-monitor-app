import {ref,get,onValue,runTransaction,update} from 'firebase/database';
const randomId=()=>globalThis.crypto.randomUUID();
/** Same demoSessions contract as MonitorRepository. No physical-device branch is accessed. */
export class PrototypeBridge {
 constructor(db,uid,sim,{eligible=()=>true,changed=()=>{},status=()=>{},leaseId=randomId()}={}){
  Object.assign(this,{db,uid,sim,eligible,changed,status,leaseId});this.base=ref(db,'demoSessions/'+uid);this.offset=0;this.closed=false;this.active=false;this.connected=false;this.busy=false;this.unsubs=[];this.now=()=>Date.now()+this.offset;sim.clock=this.now;
 }
 async start(){
  const profile=(await get(ref(this.db,'users/'+this.uid))).val();if(profile?.verified!==true)throw Error('This account needs administrator approval in the mobile app.');
  this.unsubs.push(onValue(ref(this.db,'.info/serverTimeOffset'),s=>{this.offset=s.val()||0;}));
  const claimed=await this.lease();if(!claimed)throw Error('Another prototype is using this account. Disconnect it or wait 15 seconds after closing it.');
  if(this.closed)throw Error('Connection cancelled');
  const snapshot=(await get(this.base)).val()||{};if(this.closed)throw Error('Connection cancelled');this.sim.restore(snapshot.telemetry);this.started=this.now();
  const old=snapshot.command;if(old&&['pending','accepted'].includes(old.status))await this.ack({...old,status:'failed',message:'Website reconnected; send a fresh request',acknowledgedAt:this.now()});
  this.active=true;this.sim.linked=true;
  this.unsubs.push(onValue(ref(this.db,'.info/connected'),s=>{this.connected=s.val()===true;this.report();}));
  this.unsubs.push(onValue(ref(this.db,'users/'+this.uid+'/verified'),s=>{if(s.val()!==true){this.stop('Account approval was removed.');}},e=>this.stop(e.message)));
  this.timer=setInterval(()=>this.pump(),1000);await this.pump();this.changed();
 }
 report(){this.status(!this.active?'Disconnected':!this.eligible()?'Paused — keep the 3D prototype tab visible':!this.connected?'Offline — waiting to reconnect':'Linked to Firebase · use the same account in the app');}
 async lease(){
  const start=Date.now();const now=this.now();const result=await runTransaction(ref(this.db,'demoSessions/'+this.uid+'/producer'),v=>{
   if(v&&v.id!==this.leaseId&&v.expiresAt>now)return;
   return {id:this.leaseId,expiresAt:now+12000};
  },{applyLocally:false});this.latency=Date.now()-start;return result.committed;
 }
 async ack(value){
  const path=ref(this.db,'demoSessions/'+this.uid+'/command');const observed=(await get(path)).val();
  if(!observed)return;
  return runTransaction(path,latest=>{
   const current=latest??observed;
   if(!current||current.id!==value.id||!['pending','accepted'].includes(current.status)||current.expiresAt<=this.now())return;
   return {...current,status:value.status,message:value.message,acknowledgedAt:this.now()};
  },{applyLocally:false});
 }
 async consume(){
  if(!this.eligible()||!this.active)return;
  const path=ref(this.db,'demoSessions/'+this.uid+'/command');const snapshot=await get(path);const c=snapshot.val();const now=this.now();
  if(!c||c.status!=='pending'||c.target!=='simulation'||c.requestedBy!==this.uid||c.expiresAt<=now||c.createdAt< this.started||c.createdAt>now+5000)return;
  const claimed=await runTransaction(path,latest=>{
   const current=latest??c;
   if(!this.active||!this.eligible()||!current||current.id!==c.id||current.status!=='pending'||current.expiresAt<=this.now())return;
   return {...current,status:'accepted',message:'Website 3D prototype received the request',acknowledgedAt:this.now()};
  },{applyLocally:false});
  if(!claimed.committed)return;
  if(!this.active||!this.eligible()){await this.ack({...c,status:'failed',message:'3D view paused before execution'});return;}
  this.sim.accept(claimed.snapshot.val(),this.uid);this.changed();
 }
 async flush(){
  const records=this.sim.outbox.slice();const patch={telemetry:{...this.sim.telemetry(),latencyMs:Math.min(60000,this.latency||0),writerId:this.leaseId}};
  const readingId=randomId();patch['readings/'+readingId]={timestamp:this.now(),waterCm:this.sim.water,trend:this.lastWater==null?'STABLE':this.sim.water>this.lastWater?'RISING':this.sim.water<this.lastWater?'RECEDING':'STABLE'};
  for(const record of records)if(record.path!=='command')patch[record.path]=record.value;
  await update(this.base,patch);this.lastWater=this.sim.water;
  // Remove only this successfully persisted batch; newly generated frame events stay queued.
  for(const record of records)if(record.path==='command')await this.ack(record.value);
  this.sim.outbox=this.sim.outbox.filter(r=>!records.includes(r));
 }
 async pump(){
  if(this.closed||!this.active||this.busy)return;this.busy=true;
  try{
   if(!this.connected||!this.eligible()){this.report();return;}
   if(!await this.lease()){this.stop('Another prototype took over this account. Reconnect to continue.');return;}
   if(!this.active||!this.eligible())return;
   await this.consume();if(!this.active||!this.eligible())return;await this.flush();this.report();
  }catch(e){this.status('Connection error: '+e.message+' · requests are not confirmed');}
  finally{this.busy=false;}
 }
 stop(message='Disconnected — local preview only'){
  this.active=false;this.closed=true;this.sim.linked=false;clearInterval(this.timer);for(const unsub of this.unsubs)unsub();this.unsubs=[];this.status(message);
  // Lease expires within 12 seconds. Avoid an offline release overwriting a new owner.
 }
}
