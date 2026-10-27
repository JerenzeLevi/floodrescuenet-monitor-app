import {initializeApp,deleteApp} from 'firebase/app';
import {getAuth,setPersistence,inMemoryPersistence,signInWithEmailAndPassword,signOut,connectAuthEmulator} from 'firebase/auth';
import {getDatabase,connectDatabaseEmulator,goOffline} from 'firebase/database';
import {PrototypeBridge} from './firebase-bridge.mjs';
const $=id=>document.getElementById(id);let bridge,app,auth,db,connecting=false;
const controls=()=>{const on=!!bridge?.active;$('link-connect').disabled=connecting||on;$('link-disconnect').disabled=connecting||!app;};
function status(text){$('link-status').textContent=text;document.querySelector('.connection').textContent=text;document.querySelector('.local-badge').textContent=bridge?.active&&bridge.connected?'LINKED':'LOCAL';controls();}
async function disconnect(){bridge?.stop();if(auth)await signOut(auth).catch(()=>{});if(db)goOffline(db);if(app)await deleteApp(app);bridge=app=auth=db=null;window.floodPrototype?.sync();status('Disconnected — local preview only. The app will mark the source stale.');}
$('app-link-form').addEventListener('submit',async e=>{
 e.preventDefault();if(connecting||bridge?.active)return;connecting=true;controls();status('Connecting to the prototype session…');
 try{
  const runtime=window.floodPrototype;if(!runtime?.ready)throw Error('The 3D view must finish loading before connecting.');
  const config=window.FLOOD_FIREBASE_CONFIG;app=initializeApp(config,'prototype-'+Date.now());auth=getAuth(app);db=getDatabase(app);
  if(config.emulator&&['localhost','127.0.0.1'].includes(location.hostname)){
   connectAuthEmulator(auth,'http://127.0.0.1:'+config.emulator.auth,{disableWarnings:true});connectDatabaseEmulator(db,'127.0.0.1',config.emulator.database);
  }
  await setPersistence(auth,inMemoryPersistence);const credential=await signInWithEmailAndPassword(auth,$('link-email').value.trim(),$('link-password').value);$('link-password').value='';
  bridge=new PrototypeBridge(db,credential.user.uid,runtime.sim,{eligible:()=>runtime.ready&&runtime.canRun(),changed:()=>runtime.sync(),status});
  runtime.remoteReady=()=>!!bridge?.active&&bridge.connected;await bridge.start();$('warning').value=runtime.sim.warning;$('danger').value=runtime.sim.danger;for(const input of document.querySelectorAll('[data-health]'))input.value=runtime.sim.health[input.dataset.health];
 }catch(error){await disconnect();status('Could not connect: '+error.message);}
 finally{$('link-password').value='';connecting=false;controls();}
});
$('link-disconnect').addEventListener('click',()=>disconnect());
document.addEventListener('visibilitychange',()=>{bridge?.report();if(!document.hidden)bridge?.pump();});
window.addEventListener('prototype-unavailable',()=>bridge?.stop('3D view unavailable — reconnect after refreshing the page.'));
window.addEventListener('pagehide',()=>{bridge?.stop();if(db)goOffline(db);});
