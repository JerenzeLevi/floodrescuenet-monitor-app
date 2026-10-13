import { before, after, beforeEach, test } from 'node:test';
import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { ref, get, set, update, remove, runTransaction } from 'firebase/database';
let env;
const profile=(verified=false)=>({fullName:'Demo Student',barangay:'Classroom',role:'',email:'alice@example.test',verified,createdAt:Date.now()});
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
