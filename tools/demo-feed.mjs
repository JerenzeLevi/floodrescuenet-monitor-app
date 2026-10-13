import { readFile } from 'node:fs/promises';
import { createInterface } from 'node:readline';
import { randomUUID } from 'node:crypto';
import { DemoEngine } from './demo-engine.mjs';

// Authenticated RTDB test-data source. Never prints credentials or credential URLs.
const configuration = JSON.parse(await readFile(new URL('../app/google-services.json',import.meta.url),'utf8'));
const client = configuration.client.find(c => c.client_info.android_client_info.package_name === 'com.example.floodrescuenetmonitor');
const apiKey = client?.api_key?.[0]?.current_key;
const databaseURL = configuration.project_info.firebase_url;
const email = process.env.FLOOD_EMAIL, password = process.env.FLOOD_PASSWORD;
if (!email || !password || !apiKey || !databaseURL) {
  console.error('Set FLOOD_EMAIL and FLOOD_PASSWORD to a verified demo account. Keep app/google-services.json in place.');process.exit(1);
}
let auth, expiresAt = 0;
async function jsonRequest(url, options = {}) {
  const response = await fetch(url,{...options,signal:AbortSignal.timeout(10000),headers:{'Content-Type':'application/json',...options.headers}});
  const value = await response.json();
  if (!response.ok) throw Error('Firebase request failed ('+response.status+'). Check authentication, verification, rules and network.');
  return value;
}
async function signIn() {
  auth = await jsonRequest('https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key='+encodeURIComponent(apiKey),{method:'POST',body:JSON.stringify({email,password,returnSecureToken:true})});
  expiresAt = Date.now() + Number(auth.expiresIn)*1000;
}
async function request(path, method='GET', value) {
  if (Date.now() > expiresAt - 60000) await signIn();
  const url = databaseURL.replace(/\/$/,'')+'/'+path+'.json?auth='+encodeURIComponent(auth.idToken);
  return jsonRequest(url,{method,body:value===undefined?undefined:JSON.stringify(value)});
}
await signIn();
const uid = auth.localId;
if (await request('users/'+uid+'/verified') !== true) throw Error('An administrator must verify this demo account first.');
const root = 'demoSessions/'+uid;
const previous = await request(root+'/telemetry') || {};
if (previous.net === 'deploying' && !process.argv.includes('--repack')) throw Error('The previous demo stopped while deploying. Re-run with --repack to explicitly start from a manually repacked state.');
const engine = new DemoEngine(previous);
if (process.argv.includes('--repack')) engine.reset();
const existing = await request(root+'/command');
// Mark the old ID seen before the first poll, so a pending request cannot replay.
engine.discardPrevious(existing);
console.log('Demo feeder connected. Commands: water 17 | reset | status | quit');
console.log('Sign into the Android app with the same account. Only this account’s demo session is used.');
const input = createInterface({input:process.stdin,output:process.stdout});
let stopped = false, previousWater = engine.water, lastReading = 0;
input.on('line',line=>{
  const [verb,arg] = line.trim().split(/\s+/);
  try {
    if (verb==='water') engine.waterLevel(arg===undefined?NaN:Number(arg));
    else if (verb==='reset') engine.reset();
    else if (verb==='status') console.log({waterCm:engine.water,net:engine.net,mode:engine.mode});
    else if (verb==='quit') {stopped=true;input.close();}
    else console.log('Use: water 0..20 | reset | status | quit');
  } catch(error) { console.error(error.message); }
});
input.on('close',()=>{stopped=true;});
let pendingRecords = [];
while (!stopped) {
  try {
    const command = await request(root+'/command');
    engine.accept(command,uid);
    engine.tick();
    const now = Date.now();
    pendingRecords.push(...engine.drain());
    const updates = {telemetry:engine.telemetry(now)};
    if (now-lastReading >= 2000) {
      updates['readings/'+randomUUID()]={timestamp:now,waterCm:engine.water,trend:engine.water>previousWater?'RISING':engine.water<previousWater?'RECEDING':'STABLE'};
      previousWater=engine.water;lastReading=now;
    }
    // Fixed event IDs make retries idempotent. Do not overwrite a newer/expired command.
    for (const record of pendingRecords) {
      if (record.path==='command') {
        if (record.value.id!==command?.id || record.value.expiresAt <= now
            || command.status==='completed' || command.status==='failed') continue;
      }
      updates[record.path] = record.value;
    }
    await request(root,'PATCH',updates);
    pendingRecords=[];
  } catch(error) { console.error(error.message); }
  await new Promise(resolve=>setTimeout(resolve,2000));
}
console.log('Demo feeder stopped. The app will mark readings stale when updates stop.');
