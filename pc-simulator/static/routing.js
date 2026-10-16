/* Nexo SOS · Rutas por calles reales (OSRM) con desvío automático ante bloqueos */
(function (root) {
  'use strict';

  const OSRM = 'https://router.project-osrm.org/route/v1/driving/';
  const R = 6371000;
  const rad = (d) => (d * Math.PI) / 180;
  const deg = (r) => (r * 180) / Math.PI;

  /** Distancia en metros entre dos puntos [lat, lon]. */
  function hav(a, b) {
    const dLat = rad(b[0] - a[0]);
    const dLon = rad(b[1] - a[1]);
    const x = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a[0])) * Math.cos(rad(b[0])) * Math.sin(dLon / 2) ** 2;
    return 2 * R * Math.asin(Math.sqrt(x));
  }

  /** Punto a `meters` metros desde `p` en el rumbo `bearing` (grados). */
  function offsetPoint(p, bearing, meters) {
    const br = rad(bearing);
    const d = meters / R;
    const lat1 = rad(p[0]);
    const lon1 = rad(p[1]);
    const lat2 = Math.asin(Math.sin(lat1) * Math.cos(d) + Math.cos(lat1) * Math.sin(d) * Math.cos(br));
    const lon2 = lon1 + Math.atan2(Math.sin(br) * Math.sin(d) * Math.cos(lat1), Math.cos(d) - Math.sin(lat1) * Math.sin(lat2));
    return [deg(lat2), deg(lon2)];
  }

  /** Distancia en metros de un punto a un segmento (proyección local plana). */
  function distToSegment(p, a, b) {
    const k = Math.cos(rad(p[0]));
    const ax = (a[1] - p[1]) * k * 111320, ay = (a[0] - p[0]) * 110540;
    const bx = (b[1] - p[1]) * k * 111320, by = (b[0] - p[0]) * 110540;
    const dx = bx - ax, dy = by - ay;
    const len2 = dx * dx + dy * dy;
    let t = len2 === 0 ? 0 : -(ax * dx + ay * dy) / len2;
    t = Math.max(0, Math.min(1, t));
    return Math.hypot(ax + t * dx, ay + t * dy);
  }

  function hitsBlock(route, blocks, radius) {
    radius = radius || 250;
    if (!blocks || !blocks.length) return false;
    const g = route.geometry;
    for (const b of blocks) {
      const p = [b.lat, b.lon];
      for (let i = 1; i < g.length; i++) {
        if (distToSegment(p, g[i - 1], g[i]) <= radius) return true;
      }
    }
    return false;
  }

  function fmtDist(m) {
    if (m < 950) return Math.max(10, Math.round(m / 10) * 10) + ' m';
    return (m / 1000).toFixed(m < 10000 ? 1 : 0) + ' km';
  }

  function stepText(s, forceContinue) {
    const m = s.maneuver || {};
    const name = s.name ? s.name : '';
    const via = name ? ' por ' + name : '';
    const mod = {
      left: 'a la izquierda', right: 'a la derecha', 'slight left': 'ligeramente a la izquierda',
      'slight right': 'ligeramente a la derecha', 'sharp left': 'pronunciado a la izquierda',
      'sharp right': 'pronunciado a la derecha', straight: 'de frente', uturn: 'en U'
    }[m.modifier] || '';
    if (forceContinue) return 'Continúa' + via;
    switch (m.type) {
      case 'depart': return 'Sal' + (name ? ' por ' + name : ' y sigue la ruta');
      case 'arrive': return 'Llegaste al destino';
      case 'turn': return 'Gira ' + mod + (name ? ' hacia ' + name : '');
      case 'end of road': return 'Al final de la vía gira ' + mod + (name ? ' hacia ' + name : '');
      case 'fork': return 'Toma el desvío ' + mod + via;
      case 'merge': return 'Incorpórate ' + mod + via;
      case 'on ramp': return 'Toma el acceso ' + mod + via;
      case 'off ramp': return 'Toma la salida ' + mod + via;
      case 'roundabout':
      case 'rotary':
      case 'roundabout turn': return 'En la rotonda toma la salida ' + (m.exit || '') + via;
      case 'new name': return 'Continúa' + via;
      case 'continue': return 'Continúa ' + (mod && m.modifier !== 'straight' ? mod : 'de frente') + via;
      default: return 'Continúa' + via;
    }
  }

  function parseRoute(r) {
    const geometry = r.geometry.coordinates.map((c) => [c[1], c[0]]);
    const cum = [0];
    for (let i = 1; i < geometry.length; i++) cum.push(cum[i - 1] + hav(geometry[i - 1], geometry[i]));
    const steps = [];
    let acc = 0;
    r.legs.forEach((leg, li) => {
      const last = li === r.legs.length - 1;
      leg.steps.forEach((s) => {
        const t = s.maneuver && s.maneuver.type;
        if (t === 'arrive' && !last) return;
        const cont = t === 'depart' && li > 0;
        steps.push({ text: stepText(s, cont), distance: s.distance, start: acc, type: t });
        acc += s.distance;
      });
    });
    return { geometry, cum, total: cum[cum.length - 1], distance: r.distance, duration: r.duration, steps };
  }

  async function osrm(points, alternatives) {
    const path = points.map((p) => p[1] + ',' + p[0]).join(';');
    const url = OSRM + path + '?overview=full&geometries=geojson&steps=true&alternatives=' + (alternatives ? 'true' : 'false');
    const ctrl = typeof AbortController !== 'undefined' ? new AbortController() : null;
    const to = setTimeout(() => ctrl && ctrl.abort(), 15000);
    try {
      const res = await fetch(url, ctrl ? { signal: ctrl.signal } : undefined);
      const j = await res.json();
      if (j.code !== 'Ok') return [];
      return j.routes.map(parseRoute);
    } finally {
      clearTimeout(to);
    }
  }

  function fastest(list) {
    return list.slice().sort((a, b) => a.duration - b.duration)[0];
  }

  async function inBatches(tasks, size) {
    const out = [];
    for (let i = 0; i < tasks.length; i += size) {
      const res = await Promise.all(tasks.slice(i, i + size).map((t) => t().catch(() => [])));
      out.push(...res);
    }
    return out;
  }

  /** Ruta recta de emergencia cuando no hay acceso al servicio de mapas. */
  function approxRoute(from, to) {
    const d = hav(from, to) * 1.35;
    const geometry = [from, to];
    return {
      geometry, cum: [0, hav(from, to)], total: hav(from, to), distance: d, duration: d / (40 / 3.6),
      steps: [
        { text: 'Sal en dirección al destino', distance: d, start: 0, type: 'depart' },
        { text: 'Llegaste al destino', distance: 0, start: d, type: 'arrive' }
      ]
    };
  }

  /**
   * Calcula la mejor ruta evitando los bloqueos reportados.
   * Devuelve { route, status, extraSeconds, blocksAvoided }
   * status: 'ok' | 'detour' | 'blocked' | 'approx'
   */
  async function computeRoute(from, to, blocks) {
    blocks = blocks || [];
    let routes;
    try {
      routes = await osrm([from, to], true);
    } catch (e) {
      routes = [];
    }
    if (!routes.length) return { route: approxRoute(from, to), status: 'approx', extraSeconds: 0, blocksAvoided: 0 };

    const baseline = fastest(routes);
    const free = routes.filter((r) => !hitsBlock(r, blocks));
    if (free.length) {
      const best = fastest(free);
      const hit = hitsBlock(baseline, blocks);
      return {
        route: best,
        status: hit ? 'detour' : 'ok',
        extraSeconds: hit ? Math.max(0, best.duration - baseline.duration) : 0,
        blocksAvoided: hit ? blocks.length : 0
      };
    }

    // Todas las alternativas pasan por un bloqueo: se prueban desvíos con puntos intermedios alrededor.
    const target = blocks.find((b) => hitsBlock(baseline, [b])) || blocks[0];
    const center = [target.lat, target.lon];
    for (const radius of [4000, 9000, 16000]) {
      const tasks = [];
      for (let brg = 0; brg < 360; brg += 45) {
        const wp = offsetPoint(center, brg, radius);
        tasks.push(() => osrm([from, wp, to], false));
      }
      const results = (await inBatches(tasks, 4)).flat();
      const valid = results.filter((r) => !hitsBlock(r, blocks) && r.duration < baseline.duration * 3);
      if (valid.length) {
        const best = fastest(valid);
        return { route: best, status: 'detour', extraSeconds: Math.max(0, best.duration - baseline.duration), blocksAvoided: blocks.length };
      }
    }
    return { route: baseline, status: 'blocked', extraSeconds: 0, blocksAvoided: blocks.length };
  }

  /** Posición [lat, lon] a `d` metros recorridos sobre la ruta. */
  function pointAt(route, d) {
    const { geometry, cum } = route;
    if (d <= 0) return geometry[0];
    if (d >= route.total) return geometry[geometry.length - 1];
    let lo = 0, hi = cum.length - 1;
    while (hi - lo > 1) {
      const mid = (lo + hi) >> 1;
      if (cum[mid] <= d) lo = mid; else hi = mid;
    }
    const seg = cum[hi] - cum[lo] || 1;
    const f = (d - cum[lo]) / seg;
    return [geometry[lo][0] + (geometry[hi][0] - geometry[lo][0]) * f, geometry[lo][1] + (geometry[hi][1] - geometry[lo][1]) * f];
  }

  const api = { hav, hitsBlock, computeRoute, pointAt, fmtDist, osrm, approxRoute };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  root.Routing = api;
})(typeof globalThis !== 'undefined' ? globalThis : this);
