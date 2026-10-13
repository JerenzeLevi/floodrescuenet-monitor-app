import test from 'node:test';
import assert from 'node:assert/strict';
import { DemoEngine } from './demo-engine.mjs';
const NOW = 100000;
const command = (change={}) => ({id:'test-command-0001',requestedBy:'alice',target:'simulation',type:'DEPLOY',status:'pending',location:'Classroom demo',mode:'manual',createdAt:NOW,expiresAt:NOW+30000,...change});

test('threshold boundaries and input validation',()=>{
  const e = new DemoEngine({mode:'manual'},NOW);
  for (const [water,tier] of [[0,'NORMAL'],[9.9,'NORMAL'],[10,'WARNING'],[15.9,'WARNING'],[16,'DANGER'],[20,'DANGER']]) {
    e.waterLevel(water,NOW);assert.equal(e.tier,tier);
  }
  for(const invalid of [-1,21,NaN,Infinity,'16']) assert.throws(()=>e.waterLevel(invalid,NOW));
  assert.equal(new DemoEngine({waterCm:200},NOW).water,4);
});
test('automatic deployment completes once and stays deployed when water recedes',()=>{
  const e = new DemoEngine({},NOW);e.drain();e.waterLevel(16,NOW);
  assert.equal(e.net,'deploying');e.tick(NOW+4999);assert.equal(e.net,'deploying');
  e.tick(NOW+5000);assert.equal(e.net,'deployed');
  e.waterLevel(4,NOW+6000);e.tick(NOW+60000);assert.equal(e.net,'deployed');
  assert.equal(e.drain().filter(r=>r.path.startsWith('deployments/')).length,1);
  e.reset(NOW+61000);assert.equal(e.net,'ready');assert.equal(e.water,4);
});
test('restored danger reading starts automatic demo on the first tick',()=>{
  const e = new DemoEngine({waterCm:17,mode:'automatic'},NOW);e.tick(NOW);assert.equal(e.net,'deploying');
});
test('manual mode does not deploy from water changes',()=>{
  const e = new DemoEngine({mode:'manual'},NOW);e.waterLevel(20,NOW);e.tick(NOW+10000);assert.equal(e.net,'ready');
});
test('manual command is acknowledged, completes and cannot be replayed',()=>{
  const e = new DemoEngine({mode:'manual'},NOW);e.drain();const c=command();
  assert.equal(e.accept(c,'alice',NOW),true);assert.equal(e.accept(c,'alice',NOW),false);
  assert.equal(e.drain().find(r=>r.path==='command').value.status,'accepted');
  e.tick(NOW+5000);const records=e.drain();
  assert.equal(records.find(r=>r.path==='command').value.status,'completed');
  assert.equal(records.find(r=>r.path==='deployments/'+c.id).value.status,'Success');
});
test('foreign, physical-target, expired and previously accepted commands are ignored',()=>{
  for(const change of [{requestedBy:'bob'},{target:'hardware'},{expiresAt:NOW},{createdAt:NOW+6000},{expiresAt:NOW+40000},{status:'accepted'}]) {
    const e=new DemoEngine({},NOW);e.drain();assert.equal(e.accept(command(change),'alice',NOW),false);
    assert.equal(e.net,'ready');assert.equal(e.drain().length,0);
  }
});
test('a second deployment requires a repack and records a failed request',()=>{
  const e=new DemoEngine({net:'deployed'},NOW);e.drain();e.accept(command(),'alice',NOW);
  const r=e.drain();assert.equal(r.find(r=>r.path==='command').value.status,'failed');
  assert.equal(r.find(r=>r.path.startsWith('deployments/')).value.status,'Failed');assert.equal(e.net,'deployed');
});
test('explicit reset during a demo reports failure instead of success',()=>{
  const e=new DemoEngine({mode:'manual'},NOW);e.accept(command(),'alice',NOW);e.drain();e.reset(NOW+1000);
  const r=e.drain();assert.equal(r.find(r=>r.path==='command').value.status,'failed');
  assert.equal(r.find(r=>r.path.startsWith('deployments/')).value.status,'Failed');
  e.tick(NOW+6000);assert.equal(e.net,'ready');assert.equal(e.drain().length,0);
});
test('mode changes and diagnostic commands produce real acknowledgements and events',()=>{
  const e=new DemoEngine({mode:'manual',waterCm:17},NOW);e.drain();
  e.accept(command({type:'DIAGNOSTIC'}),'alice',NOW);
  assert.match(e.drain().find(r=>r.path==='command').value.message,/hardware not tested/);
  e.accept(command({id:'test-command-0002',type:'SET_MODE',mode:'automatic'}),'alice',NOW);
  assert.equal(e.net,'deploying');assert.equal(e.mode,'automatic');
});
test('expired completion does not acknowledge a timed-out command',()=>{
  const e=new DemoEngine({mode:'manual'},NOW);e.accept(command({expiresAt:NOW+3000}),'alice',NOW);e.drain();
  e.tick(NOW+5000);const r=e.drain();assert.equal(r.some(r=>r.path==='command'),false);
  assert.equal(e.net,'deployed'); // Telemetry reports the result; the request itself remains unconfirmed.
});
test('a pending request from before restart is failed and never executed by the next poll',()=>{
  const e=new DemoEngine({mode:'manual'},NOW);e.drain();const c=command();
  e.discardPrevious(c,NOW);assert.equal(e.accept(c,'alice',NOW+10),false);
  assert.equal(e.net,'ready');assert.equal(e.drain().find(r=>r.path==='command').value.status,'failed');
});
