import { before, after, beforeEach, test } from 'node:test';
import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { ref, get, set, update, remove, runTransaction, query, orderByChild, equalTo, endAt, startAt, limitToLast } from 'firebase/database';
let env;
const profile=(verified=false)=>({fullName:'Demo Student',barangay:'Classroom',role:'user',email:'alice@example.test',verified,createdAt:Date.now()});
const telemetry=()=>({schemaVersion:1,source:'simulation',waterCm:4,warningCm:10,dangerCm:16,net:'ready',mode:'manual',updatedAt:Date.now()});
const command=(change={})=>({id:'test-command-0001',requestedBy:'alice',target:'simulation',type:'DEPLOY',status:'pending',location:'Demo',mode:'manual',createdAt:Date.now(),expiresAt:Date.now()+30000,...change});
const db=(uid='alice')=>env.authenticatedContext(uid,{email:uid+'@example.test'}).database();
const session=(database=db(),path='telemetry')=>ref(database,'demoSessions/alice/'+path);
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-flood-monitor',database:{host:'127.0.0.1',port:9000,rules:await readFile(new URL('../firebase/database.rules.json',import.meta.url),'utf8')}});});
after(async()=>{await env?.cleanup();});
beforeEach(async()=>{await env.clearDatabase();await env.withSecurityRulesDisabled(c=>set(ref(c.database(),'users/alice'),profile(true)));});

test('verified owner can write and read a demo reading',async()=>{await assertSucceeds(set(session(),telemetry()));await assertSucceeds(get(session()));});
test('unauthenticated, foreign and unverified clients cannot read or write the session',async()=>{
  await set(session(),telemetry());
  for(const client of [env.unauthenticatedContext().database(),db('bob')]) {
    await assertFails(get(session(client)));await assertFails(set(session(client),telemetry()));
  }
  await env.withSecurityRulesDisabled(c=>set(ref(c.database(),'users/alice/verified'),false));
  await assertFails(get(session()));await assertFails(set(session(),telemetry()));
});
test('new profile can be created but cannot self-approve or modify verification',async()=>{
  await env.withSecurityRulesDisabled(c=>remove(ref(c.database(),'users/alice')));
  await assertFails(set(ref(db(),'users/alice'),profile(true)));
  await assertSucceeds(set(ref(db(),'users/alice'),profile(false)));
  await assertFails(set(ref(db(),'users/alice/verified'),true));await assertFails(remove(ref(db(),'users/alice')));
});
test('invalid readings, unsafe namespaces, extra fields and deletes are rejected',async()=>{
  for(const invalid of [{waterCm:-1},{waterCm:21},{waterCm:'17'},{source:'hardware'},{warningCm:17,dangerCm:16},{net:'missing'},{motor:true},{updatedAt:Date.now()+60000}]) {
    await assertFails(set(session(),{...telemetry(),...invalid}));
  }
  await set(session(),telemetry());await assertFails(remove(session()));
  await assertFails(set(ref(db(),'devices/test/command'),command()));
});
test('one pending command cannot be replaced by a duplicate request',async()=>{
  await assertSucceeds(set(session(db(),'command'),command()));
  await assertFails(set(session(db(),'command'),command({id:'test-command-0002'})));
});
test('acknowledgements keep identity immutable and terminal requests allow a new ID',async()=>{
  const c=command();const r=session(db(),'command');await set(r,c);
  await assertFails(set(r,{...c,status:'accepted',location:'Changed'}));
  await assertSucceeds(set(r,{...c,status:'accepted',acknowledgedAt:Date.now()}));
  await assertSucceeds(set(r,{...c,status:'completed',acknowledgedAt:Date.now()}));
  await assertFails(set(r,{...c,status:'pending'}));
  await assertSucceeds(set(r,command({id:'test-command-0002'})));
});
test('expired command can be replaced but not acknowledged',async()=>{
  const c=command({createdAt:Date.now()-40000,expiresAt:Date.now()-10000});
  await env.withSecurityRulesDisabled(ctx=>set(session(ctx.database(),'command'),c));
  await assertFails(set(session(db(),'command'),{...c,status:'completed'}));
  await assertSucceeds(set(session(db(),'command'),command({id:'test-command-0002'})));
});
test('command target, requester and lifetime are enforced',async()=>{
  for(const invalid of [{target:'hardware'},{requestedBy:'bob'},{expiresAt:Date.now()+60000},{status:'completed'}]) {
    await assertFails(set(session(db(),'command'),command(invalid)));
  }
});
test('atomic feeder updates accept telemetry, history and a command acknowledgement together',async()=>{
  const c=command();await set(session(db(),'command'),c);
  await assertSucceeds(update(ref(db(),'demoSessions/alice'),{
    telemetry:{...telemetry(),net:'deployed'},command:{...c,status:'completed',acknowledgedAt:Date.now()},
    'readings/test':{timestamp:Date.now(),waterCm:4,trend:'STABLE'},
    'events/test':{timestamp:Date.now(),severity:'DEPLOYED',category:'Net Deployments',title:'Demo complete',description:'Simulated result',type:'STATUS'},
    ['deployments/'+c.id]:{timestamp:Date.now(),location:'Demo',initiator:'Manual',status:'Success',cycleMs:5000}
  }));
});
test('configured contacts allow dialable text and reject URL payloads',async()=>{
  await assertSucceeds(set(session(db(),'contacts/test'),{name:'Demo contact',number:'+1 202 555 0100'}));
  await assertFails(set(session(db(),'contacts/test'),{name:'Bad',number:'https://example.com'}));
});

async function seed(path,value){await env.withSecurityRulesDisabled(c=>set(ref(c.database(),path),value));}
async function adminFixtures(){
  await seed('users/admin',{...profile(true),email:'admin@example.test',role:'admin'});
  await seed('users/super',{...profile(true),email:'super@example.test',role:'superadmin'});
  await seed('users/bob',{...profile(false),email:'bob@example.test'});
  await seed('users/other',{...profile(false),email:'other@example.test',barangay:'Other'});
}
test('admin directory query is restricted to its barangay; superadmin can list',async()=>{
  await adminFixtures();await assertFails(get(ref(db('admin'),'users')));
  await assertSucceeds(get(query(ref(db('admin'),'users'),orderByChild('barangay'),equalTo('Classroom'))));
  await assertFails(get(query(ref(db('admin'),'users'),orderByChild('barangay'),equalTo('Other'))));
  await assertSucceeds(get(ref(db('super'),'users')));
});
test('admin approves another email without profile escalation and cannot approve foreign barangay',async()=>{
  await adminFixtures();await assertSucceeds(update(ref(db('admin'),'users/bob'),{verified:true,verifiedBy:'admin',verifiedAt:Date.now()}));
  await assertFails(set(ref(db('admin'),'users/bob/role'),'admin'));
  await assertFails(update(ref(db('admin'),'users/other'),{verified:true,verifiedBy:'admin',verifiedAt:Date.now()}));
});
test('device producer alone publishes telemetry and acknowledges owner commands',async()=>{
  await seed('deviceLinks/alice','device1');const path='deviceSessions/alice/';
  await assertFails(set(ref(db(),path+'telemetry'),{...telemetry(),source:'esp32'}));
  await assertSucceeds(set(ref(db('device1'),path+'telemetry'),{...telemetry(),source:'esp32'}));
  await assertFails(set(ref(db('device2'),path+'telemetry'),{...telemetry(),source:'esp32'}));
  const c=command({target:'esp32',durationMs:3000,thrustPercent:70});
  await assertSucceeds(set(ref(db(),path+'command'),c));
  await assertFails(set(ref(db(),path+'command'),{...c,status:'completed'}));
  await assertFails(set(ref(db('device1'),path+'command'),{...c,status:'accepted',thrustPercent:100}));
  await assertSucceeds(set(ref(db('device1'),path+'command'),{...c,status:'accepted',acknowledgedAt:Date.now()}));
  await assertFails(set(ref(db('device1'),path+'command'),command({id:'test-command-0002',target:'esp32'})));
});

test('full history paging includes all equal-timestamp rows and has a truthful final page',async()=>{
  const all={};for(let i=0;i<251;i++)all['r'+String(i).padStart(4,'0')]={timestamp:100000+Math.floor(i/130),waterCm:4};
  await seed('demoSessions/alice/readings',all);
  let cursor=null,time=0,end=false;const seen=new Set();let pages=0;
  while(!end){
    const constraints=[orderByChild('timestamp')];if(cursor)constraints.push(endAt(time,cursor));constraints.push(limitToLast(cursor?102:101));
    const snap=await get(query(session(db(),'readings'),...constraints));const batch=[];
    snap.forEach(row=>{if(row.key!==cursor)batch.push(row);});end=batch.length<=100;while(batch.length>100)batch.shift();
    if(batch.length){cursor=batch[0].key;time=batch[0].val().timestamp;}
    for(const row of batch){if(seen.has(row.key))throw Error('Duplicate record');seen.add(row.key);}
    if(++pages>4)throw Error('Paging did not terminate');
  }
  if(seen.size!==251 || pages!==3)throw Error('History was truncated');
});
test('today query includes more than 100 records and excludes both day boundaries',async()=>{
  const start=Date.UTC(2026,9,12,16),all={};
  for(let i=0;i<125;i++)all['d'+i]={timestamp:start+i,location:'Test',initiator:'System',status:'Success'};
  all.old={...all.d0,timestamp:start-1};all.next={...all.d0,timestamp:start+86400000};
  await seed('demoSessions/alice/deployments',all);
  const snap=await get(query(session(db(),'deployments'),orderByChild('timestamp'),startAt(start),endAt(start+86400000-1)));
  if(snap.size!==125)throw Error('Wrong daily total: '+snap.size);
});
test('duration and thrust cannot be changed by an acknowledgement or sent out of range',async()=>{
  for(const invalid of [{durationMs:999},{durationMs:10001},{thrustPercent:9},{thrustPercent:101}])await assertFails(set(session(db(),'command'),command(invalid)));
  const c=command({durationMs:3000,thrustPercent:50});await set(session(db(),'command'),c);
  await assertFails(set(session(db(),'command'),{...c,status:'accepted',thrustPercent:80}));
});


test('archive is readable only by its approved owner and cannot be forged by clients',async()=>{
 const record={timestamp:Date.now()-86400001,waterCm:4};
 for(const branch of ['demoSessions','deviceSessions']){
  const path=branch+'/alice/archive/readings/old';await seed(path,record);
  await assertSucceeds(get(ref(db(),path)));await assertFails(get(ref(db('bob'),path)));
  await assertFails(set(ref(db(),path),{...record,waterCm:10}));
  await seed('deviceLinks/alice','device1');await assertFails(set(ref(db('device1'),path),record));
 }
});
test('producer can report boot and diagnostic states with sensor readiness',async()=>{
 await seed('deviceLinks/alice','device1');
 for(const net of ['unarmed','diagnosing','retracting','fault'])await assertSucceeds(set(ref(db('device1'),'deviceSessions/alice/telemetry'),{...telemetry(),source:'esp32',net,sensorReady:false,positionFeedback:'limit switches',droppedRecords:0}));
});
