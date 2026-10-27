import test from 'node:test';import assert from 'node:assert/strict';import {archiveBatch,RETENTION_MS} from './archive-core.mjs';
const now=RETENTION_MS*3;
test('24-hour boundary moves old records without losing today or live state',()=>{
 const input={telemetry:{net:'ready'},command:{status:'pending'},events:{old:{timestamp:now-RETENTION_MS},fresh:{timestamp:now-RETENTION_MS+1}},readings:{r:{timestamp:now-RETENTION_MS-1}},deployments:{d:{timestamp:now-RETENTION_MS-2,status:'Failed',error:'Jam'}},diagnostic:{timestamp:1}};
 const out=archiveBatch(input,now);assert.equal(out.archiveStatus.movedThisBatch,3);assert.ok(out.events.fresh);assert.equal(out.events.old,undefined);assert.equal(out.archive.deployments.d.error,'Jam');assert.deepEqual(out.telemetry,input.telemetry);assert.deepEqual(out.command,input.command);assert.ok(out.diagnostic);assert.ok(input.events.old);
});
test('retries are idempotent and a bounded backlog can be drained',()=>{
 let current={events:{}};for(let i=0;i<451;i++)current.events['e'+i]={timestamp:1};
 current=archiveBatch(current,now);assert.equal(Object.keys(current.events).length,251);
 current=archiveBatch(current,now);current=archiveBatch(current,now);assert.equal(Object.keys(current.archive.events).length,451);assert.equal(Object.keys(current.events).length,0);
 const again=archiveBatch(current,now);assert.equal(again.archiveStatus.movedThisBatch,0);assert.deepEqual(again.archive,current.archive);
});
test('conflicting archived IDs do not cause silent deletion',()=>{
 const out=archiveBatch({events:{e:{timestamp:1,title:'updated'}},archive:{events:{e:{timestamp:1,title:'original'}}}},now);assert.equal(out.events.e.title,'updated');assert.equal(out.archive.events.e.title,'original');assert.equal(out.archiveStatus.movedThisBatch,0);
});
test('invalid timestamps and missing sessions are not archived',()=>{
 assert.equal(archiveBatch(null,now),undefined);const out=archiveBatch({readings:{bad:{timestamp:'1'},future:{timestamp:now+1}}},now);assert.equal(out.archiveStatus.movedThisBatch,0);assert.ok(out.readings.bad);
});
