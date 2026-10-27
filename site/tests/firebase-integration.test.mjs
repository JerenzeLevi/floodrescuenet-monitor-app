import {test,before,after,beforeEach} from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {createRequire} from 'node:module';
import {initializeTestEnvironment,assertFails} from '@firebase/rules-unit-testing';
import {ref,set,get,update} from 'firebase/database';
import {PrototypeBridge} from '../simulator/firebase-bridge.mjs';
const require=createRequire(import.meta.url),Simulation=require('../simulator/controller.js');
let env,bridges=[];const database=uid=>env.authenticatedContext(uid,{email:uid+'@example.test'}).database();
const profile={fullName:'Prototype tester',barangay:'Test',email:'alice@example.test',role:'user',verified:true,createdAt:Date.now()};
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-flood-web',database:{host:'127.0.0.1',port:9000,rules:await readFile(new URL('../../firebase/database.rules.json',import.meta.url),'utf8')}});});
after(async()=>{for(const b of bridges)b.stop();await env?.cleanup();});
beforeEach(async()=>{for(const b of bridges)b.stop();bridges=[];await env.clearDatabase();await env.withSecurityRulesDisabled(c=>set(ref(c.database(),'users/alice'),profile));});
async function connect(){const sim=new Simulation(),b=new PrototypeBridge(database('alice'),'alice',sim);bridges.push(b);await b.start();clearInterval(b.timer);b.connected=true;await b.pump();return {sim,b,db:database('alice')};}
function request(type='DEPLOY',change={}){const now=Date.now();return {id:crypto.randomUUID(),requestedBy:'alice',target:'simulation',type,status:'pending',location:'3D test rig',mode:'manual',durationMs:2000,thrustPercent:40,createdAt:now,expiresAt:now+30000,...change};}
async function command(db,c){await set(ref(db,'demoSessions/alice/command'),c);}
const state=async db=>(await get(ref(db,'demoSessions/alice'))).val();
test('mobile DEPLOY drives the same model progress, then returns completion and deployment history',async()=>{
 const {sim,b,db}=await connect();const c=request();await command(db,c);await b.pump();
 assert.equal(sim.net,'deploying');assert.equal(sim.motion.durationMs,2000);assert.equal(sim.motion.thrustPercent,40);assert.equal((await state(db)).command.status,'accepted');
 for(let i=0;i<10;i++)sim.tick(.1);assert.equal(sim.progress,.5);await b.pump();assert.equal((await state(db)).telemetry.motorPowerPercent,40);
 for(let i=0;i<10;i++)sim.tick(.1);await b.pump();const s=await state(db);assert.equal(sim.progress,1);assert.equal(s.command.status,'completed');assert.equal(s.telemetry.net,'deployed');assert.equal(s.deployments[c.id].status,'Success');assert.equal(s.deployments[c.id].unitId,'WEB-3D-01');
 await b.pump();assert.equal(Object.keys((await state(db)).deployments).length,1);
});
test('mobile mode and diagnostics update the website and the app-facing retained result',async()=>{
 const {sim,b,db}=await connect();await command(db,request('SET_MODE'));await b.pump();assert.equal(sim.mode,'manual');sim.setWater(20);assert.equal(sim.net,'ready');
 await command(db,request('DIAGNOSTIC'));await b.pump();assert.equal((await state(db)).diagnostic.resolution,'Passed');
 sim.setHealth('alignment','Misaligned');await command(db,request('DIAGNOSTIC'));await b.pump();const s=await state(db);assert.equal(s.command.status,'failed');assert.equal(s.diagnostic.resolution,'Pending');
});
test('website water, health and automatic movement publish to mobile telemetry',async()=>{
 const {sim,b,db}=await connect();sim.setHealth('batteryPercent',72);sim.setHealth('rain','Heavy rain');sim.setWater(16);await b.pump();const s=await state(db);assert.equal(s.telemetry.waterCm,16);assert.equal(s.telemetry.net,'deploying');assert.equal(s.telemetry.batteryPercent,72);assert.equal(s.telemetry.rain,'Heavy rain');assert.ok(Object.keys(s.readings).length>0);
});
test('second website cannot take a live lease or publish with a foreign writer ID',async()=>{
 const {db}=await connect();const second=new PrototypeBridge(database('alice'),'alice',new Simulation());bridges.push(second);await assert.rejects(second.start(),/Another prototype/);
 const s=await state(db);await assertFails(update(ref(db,'demoSessions/alice/telemetry'),{...s.telemetry,writerId:crypto.randomUUID()}));
 await assertFails(get(ref(database('bob'),'demoSessions/alice')));
});
test('reconnect does not replay an old pending request or resume an interrupted deployment',async()=>{
 const c=request();await env.withSecurityRulesDisabled(async ctx=>{const seeded=ctx.database();await set(ref(seeded,'demoSessions/alice/command'),c);await set(ref(seeded,'demoSessions/alice/telemetry'),{schemaVersion:1,source:'simulation',waterCm:4,warningCm:10,dangerCm:16,mode:'manual',net:'deploying',updatedAt:Date.now()});});
 const {sim,b,db}=await connect();assert.equal(sim.net,'deployed');assert.equal(sim.motion,null);assert.equal((await state(db)).command.status,'failed');sim.reset();await b.pump();assert.equal((await state(db)).telemetry.net,'ready');
});
test('hidden or unavailable view neither consumes a mobile request nor refreshes telemetry',async()=>{
 const {sim,b,db}=await connect();const old=(await state(db)).telemetry.updatedAt;b.eligible=()=>false;await command(db,request());await b.pump();assert.equal(sim.net,'ready');const s=await state(db);assert.equal(s.command.status,'pending');assert.equal(s.telemetry.updatedAt,old);
});
test('reset interrupts a mobile deployment with a failed outcome',async()=>{
 const {sim,b,db}=await connect();const c=request();await command(db,c);await b.pump();sim.tick(.2);sim.reset();await b.pump();const s=await state(db);assert.equal(s.command.status,'failed');assert.equal(s.deployments[c.id].status,'Failed');assert.equal(s.telemetry.net,'ready');
});
test('unapproved account cannot connect; expired command cannot animate',async()=>{
 await env.withSecurityRulesDisabled(c=>set(ref(c.database(),'users/alice/verified'),false));const bad=new PrototypeBridge(database('alice'),'alice',new Simulation());bridges.push(bad);await assert.rejects(bad.start());
 await env.withSecurityRulesDisabled(c=>set(ref(c.database(),'users/alice/verified'),true));const {sim,b,db}=await connect();const old=request('DEPLOY',{createdAt:Date.now()-40000,expiresAt:Date.now()-10000});await env.withSecurityRulesDisabled(c=>set(ref(c.database(),'demoSessions/alice/command'),old));await b.pump();assert.equal(sim.net,'ready');assert.equal((await state(db)).command.status,'pending');
});
