(function(global){
'use strict';
const id=()=>global.crypto?.randomUUID?.()||('sim-'+Date.now().toString(36)+'-'+Math.random().toString(36).slice(2));
class Simulation {
 constructor(){this.water=4;this.warning=10;this.danger=16;this.mode='automatic';this.net='ready';this.progress=0;this.events=[];this.outbox=[];this.droppedRecords=0;this.clock=()=>Date.now();this.lastActivityAt=this.clock();this.motion=null;this.linked=false;this.health={batteryPercent:100,coreTempC:30,rain:'Dry',alignment:'Aligned'};this.record('Simulation ready');}
 record(message,severity='SYSTEM',category='System',type='STATUS'){
  const timestamp=this.clock();this.events.unshift({time:new Date(timestamp).toISOString(),message});this.events=this.events.slice(0,30);
  this.queue('events/'+id(),{timestamp,title:message.slice(0,120),description:message,severity,category,type,resolution:['WARNING','DANGER'].includes(severity)?'Pending':'Passed'});
 }
 queue(path,value){if(this.outbox.length>=1000){this.outbox.shift();this.droppedRecords++;}this.outbox.push({path,value});}
 setHealth(key,value){if(key==='batteryPercent'&&(!Number.isFinite(value)||value<0||value>100))throw Error('Battery must be 0–100%.');if(key==='coreTempC'&&(!Number.isFinite(value)||value< -50||value>150))throw Error('Temperature must be −50 to 150 °C.');if(key==='rain'&&!['Dry','Light rain','Heavy rain'].includes(value))throw Error('Invalid rain condition.');if(key==='alignment'&&!['Aligned','Misaligned'].includes(value))throw Error('Invalid alignment.');if(!['batteryPercent','coreTempC','rain','alignment'].includes(key))throw Error('Unknown health input.');this.health[key]=value;this.record('Simulated '+key+': '+value);}
 get level(){return this.water>=this.danger?'DANGER':this.water>=this.warning?'WARNING':'NORMAL';}
 setWater(value){if(!Number.isFinite(value)||value<0||value>20)throw Error('Water must be between 0 and 20 cm.');const old=this.level;this.water=value;if(old!==this.level)this.record('Water status: '+this.level,this.level,this.level==='DANGER'?'Danger':'System');this.automatic();}
 setThresholds(w,d){if(!Number.isFinite(w)||!Number.isFinite(d)||w<.1||w>=d||d>20)throw Error('Use 0.1 ≤ warning < danger ≤ 20 cm.');this.warning=w;this.danger=d;this.record('Simulation thresholds updated');this.automatic();}
 setMode(mode){if(!['automatic','manual'].includes(mode))throw Error('Invalid mode');this.mode=mode;this.lastActivityAt=this.clock();this.record('Mode: '+mode);this.automatic();}
 automatic(){if(this.mode==='automatic'&&this.level==='DANGER'&&this.net==='ready')this.deploy('Automatic threshold');}
 deploy(source='Website command',options={},command=null){
  if(this.net!=='ready')return false;
  if(this.health.alignment!=='Aligned'||this.health.batteryPercent<=0)throw Error('Simulated alignment or battery blocks deployment');
  const durationMs=options.durationMs??5000,thrustPercent=options.thrustPercent??100;
  if(!Number.isInteger(durationMs)||durationMs<1000||durationMs>10000||!Number.isInteger(thrustPercent)||thrustPercent<10||thrustPercent>100)throw Error('Use 1–10 seconds and 10–100% power.');
  this.motion={id:command?.id||id(),command,durationMs,thrustPercent,elapsed:0,start:this.clock(),source};this.net='deploying';this.progress=0;this.lastActivityAt=this.clock();this.record(source+': deployment started','SYSTEM','Net Deployments');return true;
 }
 tick(dt){
  if(this.net!=='deploying'||!this.motion)return;
  this.motion.elapsed+=Math.max(0,Math.min(Number.isFinite(dt)?dt:0,.25))*1000;
  this.progress=Math.min(1,this.motion.elapsed/this.motion.durationMs);
  if(this.progress<1)return;
  const d=this.motion;this.net='deployed';this.motion=null;this.lastActivityAt=this.clock();
  this.queue('deployments/'+d.id,{timestamp:this.clock(),location:d.command?.location||'Website 3D prototype',initiator:d.command?'Manual':d.source==='Automatic threshold'?'System':'Manual',status:'Success',unitId:'WEB-3D-01',cycleMs:Math.round(this.clock()-d.start),thrustPercent:d.thrustPercent});
  this.ack(d.command,'completed','3D net animation completed');this.record('3D net deployed — inspection and repacking required','DEPLOYED','Net Deployments');
 }
 ack(command,status,message){if(command&&command.expiresAt>this.clock())this.queue('command',{...command,status,message,acknowledgedAt:this.clock()});}
 fail(command,message){this.ack(command,'failed',message);if(command?.type==='DEPLOY')this.queue('deployments/'+command.id,{timestamp:this.clock(),location:command.location,initiator:'Manual',status:'Failed',unitId:'WEB-3D-01',error:message});this.record(message,'WARNING','Net Deployments');}
 accept(command,uid){
  if(!command||command.target!=='simulation'||command.requestedBy!==uid||command.status!=='accepted'||command.expiresAt<=this.clock()||command.createdAt>this.clock()+5000)return false;
  try{
   if(command.type==='DEPLOY'){if(!this.deploy('Mobile app',command,command))this.fail(command,'3D net is not ready; reset and repack first');}
   else if(command.type==='SET_MODE'){this.setMode(command.mode);this.ack(command,'completed','3D prototype mode: '+this.mode);}
   else if(command.type==='DIAGNOSTIC'){
    const passed=this.health.alignment==='Aligned'&&this.health.batteryPercent>0;const value={timestamp:this.clock(),severity:passed?'SYSTEM':'WARNING',category:'System',type:'DIAGNOSTIC',resolution:passed?'Passed':'Pending',title:passed?'3D prototype diagnostic passed':'3D prototype diagnostic failed',description:passed?'Browser model and simulation state responded; this is not a physical sensor or motor test.':'Simulated battery/alignment check failed; no physical hardware was tested.'};
    this.queue('diagnostic',value);this.queue('events/'+id(),value);this.ack(command,passed?'completed':'failed',passed?value.description:'Simulated battery/alignment check failed');this.record(value.title);
   }else this.fail(command,'Unsupported prototype command');
  }catch(error){this.fail(command,error.message);}
  return true;
 }
 reset(){if(this.motion?.command)this.fail(this.motion.command,'3D prototype reset before completion');this.motion=null;this.water=4;this.net='ready';this.progress=0;this.lastActivityAt=this.clock();this.record('Demo reset: water lowered and manual repacking represented');}
 restore(previous={}){
  if(Number.isFinite(previous.waterCm)&&previous.waterCm>=0&&previous.waterCm<=20)this.water=previous.waterCm;
  if(Number.isFinite(previous.warningCm)&&Number.isFinite(previous.dangerCm)&&previous.warningCm>=.1&&previous.warningCm<previous.dangerCm&&previous.dangerCm<=20){this.warning=previous.warningCm;this.danger=previous.dangerCm;}
  for(const key of Object.keys(this.health))if(previous[key]!==undefined){try{this.setHealth(key,previous[key]);}catch{}}
  this.mode=previous.mode==='manual'?'manual':'automatic';
  // An interrupted net is conservatively treated as deployed until an explicit repack.
  this.net=['deploying','deployed'].includes(previous.net)?'deployed':'ready';this.progress=this.net==='deployed'?1:0;this.motion=null;this.lastActivityAt=this.clock();this.outbox=[];this.record('Website 3D prototype connected');
 }
 telemetry(){return {schemaVersion:1,source:'simulation',unitId:'WEB-3D-01',waterCm:this.water,warningCm:this.warning,dangerCm:this.danger,net:this.net,mode:this.mode,updatedAt:this.clock(),lastActivityAt:this.lastActivityAt,motorPowerPercent:this.motion?.thrustPercent||0,...this.health,signal:'Browser Firebase link',positionFeedback:'3D animation',droppedRecords:this.droppedRecords};}
 snapshot(){return {...this.telemetry(),level:this.level,progress:this.progress,appConnected:this.linked?null:false,firebaseLinked:this.linked};}
}
global.Simulation=Simulation;if(typeof module!=='undefined')module.exports=Simulation;
})(typeof window!=='undefined'?window:globalThis);
