import {chromium} from 'playwright';
import {createServer} from 'node:http';
import {readFile} from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';
import {initializeApp,deleteApp} from 'firebase/app';
import {getAuth,connectAuthEmulator,createUserWithEmailAndPassword} from 'firebase/auth';
import {getDatabase,connectDatabaseEmulator,ref,set,get,runTransaction,goOffline} from 'firebase/database';
import {initializeTestEnvironment} from '@firebase/rules-unit-testing';
const root=path.resolve(import.meta.dirname,'..'),projectId='demo-flood-web';
const config={projectId,apiKey:'emulator-test-key',databaseURL:'https://demo-flood-web.firebaseio.com',emulator:{auth:9099,database:9000}};
const env=await initializeTestEnvironment({projectId,database:{host:'127.0.0.1',port:9000,rules:await readFile(path.join(root,'../firebase/database.rules.json'),'utf8')}});
const app=initializeApp(config,'mobile-contract-test'),auth=getAuth(app);connectAuthEmulator(auth,'http://127.0.0.1:9099',{disableWarnings:true});
const email='web-'+Date.now()+'@example.test',password='EmulatorOnly-12345';
const user=(await createUserWithEmailAndPassword(auth,email,password)).user;
const db=getDatabase(app);connectDatabaseEmulator(db,'127.0.0.1',9000);
await env.withSecurityRulesDisabled(ctx=>set(ref(ctx.database(),'users/'+user.uid),{fullName:'Browser integration test',email,barangay:'Test',role:'user',verified:true,createdAt:Date.now()}));
let browser;const errors=[];let commandCount=0;
const vercel=JSON.parse(await readFile(path.join(root,'vercel.json'),'utf8'));
const csp=vercel.headers[0].headers.find(h=>h.key==='Content-Security-Policy').value.replace("connect-src 'self'", "connect-src 'self' http://127.0.0.1:9000 ws://127.0.0.1:9000 http://127.0.0.1:9099").replace('; upgrade-insecure-requests','');
const server=createServer(async(req,res)=>{try{let name=new URL(req.url,'http://localhost').pathname;if(name.endsWith('/'))name+='index.html';let body;
 if(name==='/simulator/firebase-config.js')body='window.FLOOD_FIREBASE_CONFIG='+JSON.stringify(config)+';';else body=await readFile(path.join(root,name));
 const mime={'.html':'text/html','.js':'text/javascript','.mjs':'text/javascript','.css':'text/css','.png':'image/png','.mp4':'video/mp4'}[path.extname(name)]||'application/octet-stream';res.writeHead(200,{'Content-Type':mime,'Content-Security-Policy':csp});res.end(body);
}catch{res.writeHead(404);res.end('Not found');}});await new Promise(r=>server.listen(8123,'127.0.0.1',r));
async function state(){return (await get(ref(db,'demoSessions/'+user.uid))).val();}
async function waitFor(check,message,timeout=20000){const start=Date.now();while(Date.now()-start<timeout){if(await check())return;await new Promise(r=>setTimeout(r,100));}throw Error(message);}
async function send(type,options={}){const now=Date.now();const c={id:crypto.randomUUID(),requestedBy:user.uid,target:'simulation',type,status:'pending',location:'3D browser rig',mode:'manual',durationMs:1000,thrustPercent:35,createdAt:now,expiresAt:now+30000,...options};
 const result=await runTransaction(ref(db,'demoSessions/'+user.uid+'/command'),old=>{if(old&&['pending','accepted'].includes(old.status)&&old.expiresAt>Date.now())return;return c;},{applyLocally:false});assert.ok(result.committed);commandCount++;return c;}
try{
 browser=await chromium.launch({headless:true,args:['--no-sandbox','--use-angle=swiftshader','--enable-unsafe-swiftshader']});const page=await browser.newPage({viewport:{width:1440,height:1100}});page.on('pageerror',e=>errors.push(e.message));
 await page.route('https://fonts.googleapis.com/**',r=>r.abort());await page.route('https://fonts.gstatic.com/**',r=>r.abort());
 await page.goto('http://127.0.0.1:8123/simulator/',{waitUntil:'domcontentloaded'});await page.waitForFunction(()=>window.floodPrototype?.ready,{timeout:30000});
 await page.fill('#link-email',email);await page.fill('#link-password',password);await page.click('#link-connect');
 await page.waitForFunction(()=>document.getElementById('link-status').textContent.startsWith('Linked to Firebase'),{timeout:30000});assert.equal(await page.inputValue('#link-password'),'');
 await send('SET_MODE');await waitFor(async()=>(await state())?.command?.status==='completed','Mode acknowledgement missing');assert.equal(await page.inputValue('#mode'),'manual');
 const c=await send('DEPLOY');await waitFor(async()=>(await state())?.command?.status==='accepted','Deploy acceptance missing');
 await page.waitForFunction(()=>window.floodPrototype.sim.progress>0&&window.floodPrototype.sim.net==='deploying',{timeout:15000});
 await waitFor(async()=>(await state())?.command?.status==='completed','Deployment completion missing',30000);assert.equal(await page.textContent('#progress-value'),'100%');assert.equal((await state()).deployments[c.id].status,'Success');
 await send('DIAGNOSTIC');await waitFor(async()=>(await state())?.command?.status==='completed','Diagnostic missing');assert.equal((await state()).diagnostic.resolution,'Passed');
 await page.click('[data-action="retract"]');await waitFor(async()=>(await state())?.telemetry?.net==='ready','Repack did not reach app');
 await page.locator('#water').evaluate(e=>{e.value='17';e.dispatchEvent(new Event('input',{bubbles:true}));});await waitFor(async()=>(await state())?.telemetry?.waterCm===17,'Water reading did not reach app');assert.equal((await state()).telemetry.net,'ready');
 await send('SET_MODE',{mode:'automatic'});await waitFor(async()=>(await state())?.telemetry?.net==='deploying','Automatic deployment did not start');
 await page.screenshot({path:'/tmp/flood-website-linked.png',fullPage:true});
 await page.click('#link-disconnect');await page.waitForFunction(()=>document.getElementById('link-status').textContent.startsWith('Disconnected'));
 await page.setViewportSize({width:390,height:844});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+1),'Mobile page overflow');await page.screenshot({path:'/tmp/flood-website-mobile.png',fullPage:true});
 assert.deepEqual(errors,[]);console.log('PASS: actual WebGL browser, sign-in, '+commandCount+' mobile-contract requests, animated deployment, mode/diagnostic acknowledgement, repack, water feedback, automatic deployment, disconnect and mobile layout');
}finally{await browser?.close();goOffline(db);await deleteApp(app);await env.cleanup();await new Promise(r=>server.close(r));}

// Firebase Auth's Node timers can keep the process alive after all resources close.
process.exit(0);
