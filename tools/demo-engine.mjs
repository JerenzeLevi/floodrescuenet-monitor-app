import { randomUUID } from 'node:crypto';

// Deterministic classroom state machine. It does not run or validate hardware.
export class DemoEngine {
  constructor(previous = {}, now = Date.now()) {
    this.water = Number.isFinite(previous.waterCm) && previous.waterCm >= 0 && previous.waterCm <= 20 ? previous.waterCm : 4;
    this.mode = previous.mode === 'manual' ? 'manual' : 'automatic';
    this.net = previous.net === 'deployed' ? 'deployed' : 'ready';
    this.warning = 10; this.danger = 16;
    this.lastActivityAt = previous.lastActivityAt || now;
    this.deployment = null;
    this.seen = new Set();
    this.outbox = [];
    this.event('SYSTEM', 'Demo feeder connected', 'Simulated data source; hardware is not connected.', 'System', now);
  }
  get tier() { return this.water >= this.danger ? 'DANGER' : this.water >= this.warning ? 'WARNING' : 'NORMAL'; }
  event(severity, title, description, category, now, type = 'STATUS') {
    this.outbox.push({ path: 'events/' + randomUUID(), value: { severity, title, description, category, type, resolution: severity === 'DANGER' || severity === 'WARNING' ? 'Pending' : 'Passed', timestamp: now } });
  }
  waterLevel(value, now = Date.now()) {
    if (!Number.isFinite(value) || value < 0 || value > 20) throw Error('Use a water level from 0 to 20 cm.');
    const old = this.tier; this.water = value;
    if (old !== this.tier) this.event(this.tier, 'Water level: ' + this.tier, `Demo water is ${value.toFixed(1)} cm.`, this.tier === 'DANGER' ? 'Danger' : 'System', now);
    this.automatic(now);
  }
  automatic(now) { if (this.mode === 'automatic' && this.tier === 'DANGER' && this.net === 'ready') this.deploy(null, now); }
  deploy(command, now) {
    this.net = 'deploying'; this.lastActivityAt = now;
    this.deployment = { id: command?.id || randomUUID(), start: now, command, durationMs: command?.durationMs ?? 5000, thrustPercent: command?.thrustPercent ?? 100 };
    this.event('SYSTEM', 'Demo deployment started', `Simulated drive for ${this.deployment.durationMs / 1000}s at ${this.deployment.thrustPercent}% commanded power; no physical thrust is measured.`, 'Net Deployments', now);
  }
  accept(command, uid, now = Date.now()) {
    if (!command || command.status !== 'pending' || this.seen.has(command.id)) return false;
    if (typeof command.id !== 'string' || command.requestedBy !== uid || command.target !== 'simulation'
        || !Number.isFinite(command.createdAt) || command.createdAt > now + 5000
        || !Number.isFinite(command.expiresAt) || command.expiresAt <= now
        || command.expiresAt - command.createdAt > 35000) return false;
    this.seen.add(command.id);
    // The consumer does not replay accepted requests after a restart.
    let error = '';
    if (!['DEPLOY','SET_MODE','DIAGNOSTIC'].includes(command.type)) error = 'Unsupported request';
    if (typeof command.location !== 'string' || !command.location.trim() || command.location.length > 80) error = 'Invalid demo location';
    if (!['automatic','manual'].includes(command.mode)) error = 'Invalid mode';
    if (command.durationMs !== undefined && (!Number.isInteger(command.durationMs) || command.durationMs < 1000 || command.durationMs > 10000)) error = 'Duration must be 1000–10000 ms';
    if (command.thrustPercent !== undefined && (!Number.isInteger(command.thrustPercent) || command.thrustPercent < 10 || command.thrustPercent > 100)) error = 'Thrust must be 10–100%';
    if (command.type === 'DEPLOY' && this.net !== 'ready') error = 'Demo net is not ready; a manual repack is required';
    if (error) {
      this.ack(command, 'failed', error, now);
      if (command.type === 'DEPLOY') this.failedDeployment(command, error, now);
      return true;
    }
    if (command.type === 'DEPLOY') { this.ack(command,'accepted','Configured demo started',now);this.deploy(command,now); }
    else if (command.type === 'SET_MODE') {
      this.mode = command.mode;this.lastActivityAt = now;
      this.ack(command,'completed','Demo mode is now '+this.mode,now);
      this.event('SYSTEM','Mode changed',this.mode,'System',now);this.automatic(now);
    } else {
      this.ack(command,'completed','Demo heartbeat and state passed; hardware not tested',now);
      const diagnostic={severity:'SYSTEM',title:'Demo diagnostic completed',description:'Data feeder and state machine responded. No sensor or motor self-test was performed.',category:'System',timestamp:now,type:'DIAGNOSTIC',resolution:'Passed'};
      this.outbox.push({path:'diagnostic',value:diagnostic});
      this.event('SYSTEM',diagnostic.title,diagnostic.description,'System',now,'DIAGNOSTIC');
    }
    return true;
  }
  ack(command,status,message,now) {
    this.outbox.push({ path:'command',value:{...command,status,message,acknowledgedAt:now} });
  }
  discardPrevious(command, now = Date.now()) {
    if (!command || !['pending','accepted'].includes(command.status)) return;
    this.seen.add(command.id);
    if (command.expiresAt > now) this.ack(command,'failed','Demo feeder restarted; send a fresh request after it connects',now);
  }
  failedDeployment(command, error, now) {
    this.outbox.push({ path:'deployments/'+command.id, value:{timestamp:now,location:command.location || 'Tabletop demo',initiator:'Manual',status:'Failed',unitId:'SIM-01',error} });
    this.event('WARNING','Demo request did not complete',error,'Net Deployments',now);
  }
  tick(now = Date.now()) {
    this.automatic(now);
    if (this.deployment && now - this.deployment.start >= this.deployment.durationMs) {
      const d = this.deployment;
      this.net = 'deployed';this.lastActivityAt = now;this.deployment = null;
      if (d.command && d.command.expiresAt > now) this.ack(d.command,'completed','Simulated net deployed',now);
      this.outbox.push({ path:'deployments/'+d.id, value:{timestamp:now,location:d.command?.location||'Tabletop demo',initiator:d.command?'Manual':'System',status:'Success',unitId:'SIM-01',cycleMs:d.durationMs,thrustPercent:d.thrustPercent} });
      this.event('DEPLOYED','Simulated net deployed','The demo completed; reset represents manual repacking.','Net Deployments',now);
    }
  }
  reset(now = Date.now()) {
    if (this.deployment?.command) {
      const error = 'Demo manually reset before completion';
      if (this.deployment.command.expiresAt > now) this.ack(this.deployment.command,'failed',error,now);
      this.failedDeployment(this.deployment.command,error,now);
    }
    this.deployment = null;this.net = 'ready';this.water = 4;this.lastActivityAt = now;
    this.event('SYSTEM','Demo manually repacked','Water returned to 4 cm and the simulated net is ready.','System',now);
  }
  telemetry(now = Date.now()) {
    return {schemaVersion:1,source:'simulation',unitId:'SIM-01',motorPowerPercent:this.deployment?.thrustPercent || 0,waterCm:this.water,warningCm:this.warning,dangerCm:this.danger,net:this.net,mode:this.mode,updatedAt:now,lastActivityAt:this.lastActivityAt};
  }
  drain() { const records = this.outbox;this.outbox = [];return records; }
}
