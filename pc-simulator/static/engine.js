/* Nexo SOS · lógica de la aplicación (sin DOM): solicitudes, niveles, plazos e inventario */
'use strict';

const ACK_MS = 60000;      // plazo para que una autoridad confirme que recibió el mensaje
const DECIDE_MS = 180000;  // plazo para que tome una decisión desde el primer envío al nivel
const STORE_KEY = 'nexosos.app.v3';

const LEVELS = [
  { id: 0, key: 'COMARCA', short: 'Comarca', name: 'Comarca Masachapa', resp: 'María López', tower: 'T1', lat: 11.7809, lon: -86.5193 },
  { id: 1, key: 'MUNICIPIO', short: 'Municipio', name: 'Alcaldía de San Rafael del Sur', resp: 'Carlos Rivas', tower: 'T2', lat: 11.8465, lon: -86.4389 },
  { id: 2, key: 'DEPARTAMENTO', short: 'Departamento', name: 'Coordinación Departamental', resp: 'Elena Morales', tower: 'T3', lat: 11.9894, lon: -86.3075 },
  { id: 3, key: 'CENTRAL', short: 'Sede Central', name: 'Sede Central Managua', resp: 'Roberto Blandón', tower: null, lat: 12.118, lon: -86.289 }
];

const NEEDS = [
  { id: 'agua', name: 'Agua y comida', unit: 'kits', icon: 'droplet' },
  { id: 'rescate', name: 'Rescate', unit: 'brigadas', icon: 'life' },
  { id: 'medica', name: 'Atención médica', unit: 'botiquines', icon: 'heart' },
  { id: 'transporte', name: 'Transporte', unit: 'vehículos', icon: 'truck' },
  { id: 'refugio', name: 'Refugio', unit: 'carpas', icon: 'home' },
  { id: 'via', name: 'Despeje de vía', unit: 'cuadrillas', icon: 'tool' }
];
const NEEDS_BY_ID = Object.fromEntries(NEEDS.map((n) => [n.id, n]));

const URGENCY = [
  { id: 3, name: 'Riesgo vital', desc: 'Peligro inmediato' },
  { id: 2, name: 'Alta', desc: 'Atención pronta' },
  { id: 1, name: 'Moderada', desc: 'Puede esperar' }
];

const SITES = [
  { id: 'mas', name: 'Escuela de Masachapa', lat: 11.7809, lon: -86.5193 },
  { id: 'poch', name: 'Centro de Pochomil', lat: 11.7724, lon: -86.5044 },
  { id: 'srs', name: 'Casco urbano de San Rafael del Sur', lat: 11.8465, lon: -86.4389 },
  { id: 'cru', name: 'El Crucero', lat: 11.9894, lon: -86.3075 }
];

const BASES = [
  { id: 'srs', name: 'Alcaldía de San Rafael del Sur', lat: 11.8465, lon: -86.4389, level: 1 },
  { id: 'cru', name: 'Torre de El Crucero', lat: 11.9894, lon: -86.3075, level: 2 },
  { id: 'mga', name: 'Sede Central Managua', lat: 12.118, lon: -86.289, level: 3 }
];

const PROFILES = [
  { id: 'comunidad', name: 'Brigada comunitaria', role: 'Pide ayuda y da seguimiento', level: null, short: 'BC' },
  { id: 'l0', name: 'María López', role: 'Responsable de Comarca Masachapa', level: 0, short: 'ML' },
  { id: 'l1', name: 'Carlos Rivas', role: 'Coordinación Municipal · San Rafael del Sur', level: 1, short: 'CR' },
  { id: 'l2', name: 'Elena Morales', role: 'Coordinación Departamental', level: 2, short: 'EM' },
  { id: 'l3', name: 'Roberto Blandón', role: 'Sede Central Managua', level: 3, short: 'RB' }
];

const STATUS = {
  GUARDADO: { label: 'Guardado, sin enviar', tone: 'wait' },
  ENVIADO: { label: 'Enviado por radio', tone: 'info' },
  ACK_RADIO: { label: 'El radio confirmó, falta la autoridad', tone: 'info' },
  RECIBIDO: { label: 'Recibido por la autoridad', tone: 'ok' },
  PARCIAL: { label: 'Atendido en parte', tone: 'wait' },
  ESCALADO: { label: 'Derivado al siguiente nivel', tone: 'info' },
  ACEPTADO: { label: 'Aceptado', tone: 'ok' },
  EN_CAMINO: { label: 'Ayuda en camino', tone: 'ok' },
  EN_SITIO: { label: 'Ayuda en el lugar', tone: 'ok' },
  ATENDIDO: { label: 'Atendido', tone: 'done' },
  APOYO_CENTRAL: { label: 'Esperando apoyo central', tone: 'danger' },
  CANCELADO: { label: 'Cancelado', tone: 'neutral' }
};
const FINAL = new Set(['ATENDIDO', 'CANCELADO']);

const REASONS = [
  { id: 'brigada', text: 'No hay brigada disponible' },
  { id: 'transporte', text: 'No hay transporte' },
  { id: 'suministros', text: 'No hay suficientes suministros' },
  { id: 'centro', text: 'El centro receptor está lleno' },
  { id: 'vital', text: 'Riesgo vital: avisar también al siguiente nivel' }
];

let S = null;
const hooks = { change() {}, toast() {} };

/* ------------------------------ utilidades ------------------------------ */

function uuid() {
  try { if (globalThis.crypto && crypto.randomUUID) return crypto.randomUUID(); } catch (e) { /* contexto no seguro */ }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 3) | 8).toString(16);
  });
}

function now() { return Date.now() + (S ? S.timeOffset : 0); }
function profile() { return PROFILES.find((p) => p.id === S.profile) || PROFILES[0]; }
function nextLevel(l) { return l < 3 ? LEVELS[l + 1] : null; }
function pendingQty(sol) { return sol.qty - sol.accepted; }
function isActive(sol) { return !FINAL.has(sol.status); }
function avail(level, need) { const r = S.inv[level][need]; return r ? r.total - r.reserved : 0; }
function newDeadlines() { const t = now(); return { ackBy: t + ACK_MS, decideBy: t + DECIDE_MS, ackDone: false }; }
function keyIdFor(level) { return [102, 201, 301, 401][level]; }
function selected() { return S.solicitudes.find((s) => s.id === S.selectedId) || null; }

function estBytes(sol) {
  return Math.min(160, 71 + Math.min(24, sol.place.name.length) + 16 + 8);
}

function defaultInv() {
  const t = (total, reserved = 0) => ({ total, reserved });
  return [
    { agua: t(2, 2), medica: t(2), refugio: t(3) },
    { agua: t(4), rescate: t(2), medica: t(3), transporte: t(2), refugio: t(5), via: t(1) },
    { agua: t(3), rescate: t(3), medica: t(4), transporte: t(3), refugio: t(6), via: t(2) },
    { agua: t(25), rescate: t(8), medica: t(20), transporte: t(10), refugio: t(40), via: t(6) }
  ];
}

/* ------------------------------- estado -------------------------------- */

function freshState() {
  S = {
    v: 3, nextId: 1, profile: 'comunidad', radio: true, internet: false, managua: true, timeOffset: 0,
    blocks: [], solicitudes: [], inv: defaultInv(), selectedId: null, base: 'srs'
  };
  const sol = createSol({
    need: 'agua', qty: 10, people: 42, urgency: 3,
    place: { name: 'Escuela de Masachapa', lat: 11.7809, lon: -86.5193, source: 'site' },
    note: 'Familias evacuadas por inundación costera.'
  }, 'Brigada comunitaria', true);
  S.selectedId = sol.id;
  save();
}

function load() {
  try {
    const raw = typeof localStorage !== 'undefined' && localStorage.getItem(STORE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed && parsed.v === 3) { S = parsed; return; }
    }
  } catch (e) { /* datos corruptos: se reinicia */ }
  freshState();
}

function save() {
  try { if (typeof localStorage !== 'undefined') localStorage.setItem(STORE_KEY, JSON.stringify(S)); } catch (e) { /* sin almacenamiento */ }
}

function resetAll() {
  try { if (typeof localStorage !== 'undefined') localStorage.removeItem(STORE_KEY); } catch (e) { /* nada */ }
  freshState();
  hooks.change();
}

function log(sol, text, by, kind) {
  sol.history.push({ t: now(), text, by: by || 'Sistema', kind: kind || 'info' });
}

function commit(sol) {
  save();
  if (sol) syncGateway(sol);
  hooks.change();
}

/* ----------------------- pasarela institucional (Python) ---------------- */

function syncGateway(sol) {
  if (typeof fetch !== 'function' || typeof location === 'undefined' || location.protocol === 'file:') return;
  const lvl = LEVELS[sol.level];
  const body = {
    uuid: sol.uuid, rev: sol.rev, nivel: lvl.key, estado: sol.status, necesidad: NEEDS_BY_ID[sol.need].name,
    solicitada: sol.qty, aceptada: sol.accepted, pendiente: pendingQty(sol), ubicacion: sol.place.name,
    lat: sol.place.lat, lon: sol.place.lon, personas: sol.people, urgencia: sol.urgency, key_id: keyIdFor(sol.level)
  };
  fetch('/api/pasarela/sos', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
    .then((r) => r.json())
    .then((j) => {
      if (j && j.ok) { sol.bytes = j.bytes_lora; sol.sig = (j.hmac_hex || '').slice(0, 8); save(); hooks.change(); }
    })
    .catch(() => {});
}

/* ----------------------------- operaciones ------------------------------ */

function createSol(p, byName, silent) {
  const id = S.nextId++;
  const sol = {
    id, ref: 'SOS-' + String(id).padStart(4, '0'), uuid: uuid(), rev: 1, createdAt: now(),
    need: p.need, qty: p.qty, accepted: 0, people: p.people, urgency: p.urgency, place: p.place, note: p.note || '',
    level: 0, status: S.radio ? 'ENVIADO' : 'GUARDADO', deadlines: S.radio ? newDeadlines() : null,
    waiting: false, assigned: [], history: [], bytes: 0, sig: ''
  };
  sol.bytes = estBytes(sol);
  log(sol, S.radio
    ? 'SOS guardado en el teléfono y enviado por radio a ' + LEVELS[0].name + '.'
    : 'SOS guardado en el teléfono. Se enviará cuando se conecte el radio.', byName || profile().name, 'info');
  S.solicitudes.unshift(sol);
  S.selectedId = sol.id;
  if (!silent) commit(sol); else syncGateway(sol);
  return sol;
}

function moveToNext(sol, status, reasonText, by) {
  const nxt = nextLevel(sol.level);
  if (!nxt) {
    sol.status = 'APOYO_CENTRAL';
    sol.deadlines = null;
    log(sol, reasonText + ' La Sede Central no tiene más niveles: queda pendiente de apoyo central (no se cierra).', by, 'danger');
    return;
  }
  sol.rev++;
  sol.level = nxt.id;
  sol.status = status;
  if (nxt.id === 3 && !S.managua) {
    sol.waiting = true;
    sol.deadlines = null;
    log(sol, reasonText + ' Pasa a ' + nxt.name + ', pero no hay enlace con Managua: queda pendiente de entrega.', by, 'warn');
  } else {
    sol.waiting = false;
    sol.deadlines = newDeadlines();
    log(sol, reasonText + ' Pasa a ' + nxt.name + '.', by, 'warn');
  }
}

function confirmReceipt(sol) {
  if (sol.deadlines) sol.deadlines.ackDone = true;
  sol.rev++;
  sol.status = 'RECIBIDO';
  const l = LEVELS[sol.level];
  log(sol, l.name + ' recibió la solicitud. Ahora debe decidir (plazo de 3 minutos desde el primer envío).', l.resp, 'ok');
  commit(sol);
}

function accept(sol, n) {
  const lvl = sol.level;
  const l = LEVELS[lvl];
  const pending = pendingQty(sol);
  n = Math.min(Math.floor(n), pending);
  const a = avail(lvl, sol.need);
  if (!(n > 0)) return { ok: false, msg: 'Indica una cantidad mayor que cero.' };
  if (n > a) return { ok: false, msg: 'Solo hay ' + a + ' disponible(s) en ' + l.short + '.' };
  const need = NEEDS_BY_ID[sol.need];
  S.inv[lvl][sol.need].reserved += n;
  sol.accepted += n;
  sol.rev++;
  if (sol.deadlines) sol.deadlines.ackDone = true;
  sol.assigned.push({ level: lvl, qty: n, unit: need.unit, by: l.resp, at: now() });
  const rest = pendingQty(sol);
  if (rest === 0) {
    sol.status = 'ACEPTADO';
    sol.deadlines = null;
    log(sol, l.name + ' acepta ' + n + ' ' + need.unit + ' y cubre toda la solicitud (' + sol.accepted + ' de ' + sol.qty + ').', l.resp, 'ok');
  } else {
    log(sol, l.name + ' acepta ' + n + ' de ' + (n + rest) + ' ' + need.unit + '. Reserva esa cantidad y envía el resto (' + rest + ').', l.resp, 'ok');
    moveToNext(sol, 'PARCIAL', 'Quedan ' + rest + ' ' + need.unit + ' sin cubrir.', l.resp);
  }
  commit(sol);
  return { ok: true };
}

function noCapacity(sol, reasonId, note) {
  const l = LEVELS[sol.level];
  const r = REASONS.find((x) => x.id === reasonId) || REASONS[2];
  if (sol.deadlines) sol.deadlines.ackDone = true;
  moveToNext(sol, 'ESCALADO', l.name + ' no puede atender: ' + r.text.toLowerCase() + (note ? ' (' + note + ').' : '.'), l.resp);
  commit(sol);
}

function setTracking(sol, status, reason) {
  const l = LEVELS[sol.level];
  if (status === 'CANCELADO' && !(reason || '').trim()) return { ok: false, msg: 'Escribe el motivo de la cancelación.' };
  if (status === 'ATENDIDO' && pendingQty(sol) > 0) return { ok: false, msg: 'Aún faltan ' + pendingQty(sol) + ' por cubrir.' };
  sol.rev++;
  sol.status = status;
  if (status === 'ATENDIDO' || status === 'CANCELADO') sol.deadlines = null;
  const text = {
    EN_CAMINO: 'La ayuda salió hacia el lugar.',
    EN_SITIO: 'La brigada llegó al lugar y está atendiendo.',
    ATENDIDO: 'Solicitud atendida y cerrada. Se confirmó el regreso al lugar de origen.',
    CANCELADO: 'Solicitud cancelada. Motivo: ' + (reason || '').trim()
  }[status];
  log(sol, text, status === 'CANCELADO' ? profile().name : l.resp, status === 'CANCELADO' ? 'warn' : 'ok');
  commit(sol);
  return { ok: true };
}

function setRadio(on) {
  S.radio = on;
  if (on) {
    let n = 0;
    S.solicitudes.forEach((s) => {
      if (s.status === 'GUARDADO') {
        s.status = 'ENVIADO'; s.deadlines = newDeadlines(); n++;
        log(s, 'Radio conectado: el mensaje guardado se envió desde la cola, sin crear otra solicitud.', 'Sistema', 'ok');
        syncGateway(s);
      }
    });
    hooks.toast(n ? n + ' mensaje(s) pendiente(s) enviados por radio.' : 'Radio conectado.', 'ok');
  } else {
    hooks.toast('Radio desconectado. Los SOS nuevos se guardarán en el teléfono.', 'warn');
  }
  save(); hooks.change();
}

function setManagua(on) {
  S.managua = on;
  if (on) {
    S.solicitudes.forEach((s) => {
      if (s.waiting) {
        s.waiting = false; s.deadlines = newDeadlines();
        log(s, 'Enlace con Managua restablecido: la solicitud se entregó a la Sede Central.', 'Sistema', 'ok');
        syncGateway(s);
      }
    });
    hooks.toast('Enlace con Managua restablecido.', 'ok');
  } else {
    hooks.toast('Sin enlace con Managua. Lo que llegue a la Sede Central quedará pendiente de entrega.', 'warn');
  }
  save(); hooks.change();
}

/* ------------------------------- plazos --------------------------------- */

function checkDeadlines() {
  const t = now();
  let changed = false;
  for (const sol of S.solicitudes) {
    if (FINAL.has(sol.status) || !sol.deadlines || sol.waiting) continue;
    const d = sol.deadlines;
    const l = LEVELS[sol.level];
    if (!d.ackDone && t >= d.ackBy) {
      log(sol, 'Sin confirmación de ' + l.name + ' en 60 segundos. No significa que falten recursos: se consulta al siguiente nivel.', 'Sistema', 'warn');
      moveToNext(sol, 'ESCALADO', 'Sin respuesta a tiempo.', 'Sistema');
      syncGateway(sol); changed = true;
    } else if (d.ackDone && t >= d.decideBy) {
      log(sol, l.name + ' no decidió en 3 minutos.', 'Sistema', 'warn');
      moveToNext(sol, 'ESCALADO', 'Sin decisión a tiempo.', 'Sistema');
      syncGateway(sol); changed = true;
    }
  }
  if (changed) { save(); hooks.change(); }
  return changed;
}

/* ------------------------- herramientas de prueba ----------------------- */

function toolRadioAck(sol) {
  if (!sol || !['ENVIADO', 'ACK_RADIO'].includes(sol.status)) return { ok: false, msg: 'Esa solicitud ya no está esperando confirmación de radio.' };
  sol.status = 'ACK_RADIO';
  log(sol, 'Un radio vecino confirmó que recibió el mensaje. Todavía NO significa que una autoridad lo haya recibido: los plazos siguen corriendo.', 'Torre ' + (LEVELS[sol.level].tower || 'T1'), 'info');
  commit(sol);
  return { ok: true };
}

function toolDuplicate(sol) {
  if (!sol) return { ok: false, msg: 'Elige una solicitud.' };
  log(sol, 'Llegó otra copia del mismo mensaje (revisión ' + sol.rev + '). Se ignoró: no se duplica la solicitud ni la reserva.', 'Sistema', 'info');
  commit(sol);
  return { ok: true };
}

function toolLate(sol) {
  if (!sol) return { ok: false, msg: 'Elige una solicitud.' };
  log(sol, 'Llegó una respuesta tardía de un nivel anterior. Se guardó en el historial sin cambiar la decisión vigente.' + (sol.accepted > 0 ? ' Hay reservas: revisar por si se duplicó la ayuda.' : ''), 'Sistema', 'warn');
  commit(sol);
  return { ok: true };
}

function toolAdvance(ms) {
  S.timeOffset += ms;
  save();
  if (!checkDeadlines()) hooks.change();
}

function toolLoadTest() {
  const total = 20;
  let delivered = 0, retries = 0, bytes = 0;
  const times = [];
  for (let i = 0; i < total; i++) {
    let ok = false;
    for (let a = 0; a < 3 && !ok; a++) {
      bytes += 108;
      if (Math.random() > 0.08) { ok = true; times.push(8 + Math.random() * 40 + a * 25); } else retries++;
    }
    if (ok) delivered++;
  }
  const within = times.filter((x) => x <= 120).length;
  const avg = times.length ? times.reduce((a, b) => a + b, 0) / times.length : 0;
  return { total, delivered, within, retries, bytes, avg: avg.toFixed(1), airtime: Math.min(38, (bytes * 8 / 1200 / 6)).toFixed(1) };
}
