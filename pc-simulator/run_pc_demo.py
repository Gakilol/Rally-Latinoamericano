#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Nexo SOS · Servidor Local y Aplicación Interactiva para PC y Móvil
Corredor San Rafael del Sur, Nicaragua (Masachapa -> San Rafael del Sur -> El Crucero -> Managua)
"""

import hashlib
import hmac
import http.server
import json
import mimetypes
import os
import sqlite3
import socketserver
import time
import urllib.parse
import uuid

PORT = 8080
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(BASE_DIR, "nexo_sos_pasarela.db")

HMAC_SECRET = b"NEXO-KEY-SAN-RAFAEL-DEL-SUR-2026"


def init_db():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute(
        """
        CREATE TABLE IF NOT EXISTS solicitudes_pasarela (
            uuid_evento TEXT PRIMARY KEY,
            revision INTEGER,
            nivel_actual TEXT,
            estado_general TEXT,
            necesidad TEXT,
            cantidad_solicitada INTEGER,
            cantidad_aceptada INTEGER,
            cantidad_pendiente INTEGER,
            ubicacion_ref TEXT,
            latitud REAL,
            longitud REAL,
            bytes_lora INTEGER,
            hmac_hex TEXT,
            actualizado_epoch INTEGER
        )
        """
    )
    conn.commit()
    conn.close()


def registrar_trama_sqlite(data: dict) -> dict:
    payload_str = f"{data.get('uuid')}|{data.get('rev')}|{data.get('nivel')}|{data.get('estado')}|{data.get('pendiente')}"
    digest = hmac.new(HMAC_SECRET, payload_str.encode("utf-8"), hashlib.sha256).digest()[:16]
    hmac_hex = digest.hex()
    bytes_lora = min(148, 72 + len(payload_str) // 2 + 16)

    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute(
        """
        INSERT OR REPLACE INTO solicitudes_pasarela
        (uuid_evento, revision, nivel_actual, estado_general, necesidad,
         cantidad_solicitada, cantidad_aceptada, cantidad_pendiente,
         ubicacion_ref, latitud, longitud, bytes_lora, hmac_hex, actualizado_epoch)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            data.get("uuid", str(uuid.uuid4())),
            int(data.get("rev", 1)),
            data.get("nivel", "COMARCA"),
            data.get("estado", "ENVIADO_A_RADIO"),
            data.get("necesidad", "Agua potable y alimentos"),
            int(data.get("solicitada", 10)),
            int(data.get("aceptada", 0)),
            int(data.get("pendiente", 10)),
            data.get("ubicacion", "Escuela Comarca Masachapa"),
            float(data.get("lat", 11.7809)),
            float(data.get("lon", -86.5193)),
            bytes_lora,
            hmac_hex,
            int(time.time()),
        ),
    )
    conn.commit()
    conn.close()
    return {"ok": True, "bytes_lora": bytes_lora, "hmac_hex": hmac_hex, "port_num": 256}


HTML_SIMULADOR = r"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<title>Nexo SOS · Coordinación de Emergencias y Ruta Waze | San Rafael del Sur</title>
<link rel="stylesheet" href="/vendor/leaflet.css" onerror="this.href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'" />
<script src="/vendor/leaflet.js" onerror="var s=document.createElement('script');s.src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';document.head.appendChild(s);"></script>
<style>
  :root {
    --celeste-primario: #0288D1;
    --celeste-brillante: #03A9F4;
    --celeste-neon: #00B0FF;
    --celeste-claro: #E1F5FE;
    --celeste-suave: #F0F9FF;
    --celeste-borde: #81D4FA;
    --celeste-oscuro: #01579B;
    --blanco: #FFFFFF;
    --texto-principal: #0D2538;
    --texto-secundario: #455A64;
    --rojo-alerta: #D32F2F;
    --rojo-fondo: #FFEBEE;
    --verde-ok: #2E7D32;
    --verde-fondo: #E8F5E9;
    --ambar: #EF6C00;
    --ambar-fondo: #FFF3E0;
  }
  * { box-sizing: border-box; font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; }
  body {
    margin: 0;
    background: linear-gradient(135deg, #E1F5FE 0%, #FFFFFF 52%, #B3E5FC 100%);
    color: var(--texto-principal);
    min-height: 100vh;
    display: flex;
    flex-direction: column;
  }
  /* Barra superior limpia e intuitiva */
  .top-pc-bar {
    background: linear-gradient(90deg, var(--celeste-oscuro) 0%, var(--celeste-primario) 55%, var(--celeste-brillante) 100%);
    color: var(--blanco);
    padding: 11px 22px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 3px 14px rgba(1, 87, 155, 0.22);
    flex-wrap: wrap;
    gap: 10px;
  }
  .brand-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .brand-logo {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    background: var(--blanco);
    color: var(--celeste-primario);
    font-weight: 900;
    font-size: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  }
  .top-pc-bar h1 {
    margin: 0;
    font-size: 17px;
    font-weight: 800;
    letter-spacing: 0.2px;
  }
  .top-pc-bar .subtitle {
    font-size: 11.5px;
    color: var(--celeste-claro);
    margin-top: 1px;
  }
  .top-badges {
    display: flex;
    gap: 8px;
    align-items: center;
    flex-wrap: wrap;
  }
  .badge-pc {
    background: rgba(255, 255, 255, 0.18);
    border: 1px solid rgba(255, 255, 255, 0.45);
    color: var(--blanco);
    padding: 5px 12px;
    border-radius: 999px;
    font-size: 11.5px;
    font-weight: 700;
  }
  .badge-pc.white {
    background: var(--blanco);
    color: var(--celeste-oscuro);
    border-color: var(--blanco);
  }

  /* Contenedor principal */
  .workspace {
    display: grid;
    grid-template-columns: 435px 1fr;
    gap: 18px;
    padding: 16px 20px;
    max-width: 1580px;
    width: 100%;
    margin: 0 auto;
    flex: 1;
  }
  @media (max-width: 1040px) {
    .workspace { grid-template-columns: 1fr; }
  }

  /* Dispositivo móvil Android */
  .android-phone {
    background: var(--blanco);
    border: 7px solid #0277BD;
    border-radius: 34px;
    box-shadow: 0 16px 38px rgba(1, 87, 155, 0.20);
    display: flex;
    flex-direction: column;
    height: 830px;
    overflow: hidden;
  }
  .phone-notch {
    background: var(--celeste-oscuro);
    color: var(--celeste-claro);
    font-size: 11px;
    padding: 6px 16px;
    display: flex;
    justify-content: space-between;
    font-weight: 600;
  }
  .app-header {
    background: linear-gradient(180deg, var(--celeste-primario), var(--celeste-brillante));
    color: var(--blanco);
    padding: 11px 13px;
  }
  .app-title-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .app-title-row h2 {
    margin: 0;
    font-size: 15px;
    font-weight: 800;
  }
  .corridor-pill {
    background: var(--blanco);
    color: var(--celeste-oscuro);
    padding: 3px 8px;
    border-radius: 999px;
    font-size: 10px;
    font-weight: 800;
  }
  .app-sub {
    font-size: 11px;
    color: var(--celeste-claro);
    margin-top: 2px;
  }
  .status-chips {
    display: flex;
    gap: 5px;
    flex-wrap: wrap;
    margin-top: 8px;
  }
  .chip-status {
    background: var(--blanco);
    color: var(--celeste-oscuro);
    border-radius: 7px;
    padding: 3px 8px;
    font-size: 10px;
    font-weight: 700;
  }
  .chip-status.warn {
    background: var(--rojo-fondo);
    color: #B71C1C;
  }
  .role-bar {
    display: flex;
    gap: 5px;
    overflow-x: auto;
    margin-top: 8px;
    padding-bottom: 2px;
  }
  .role-btn {
    border: 1px solid rgba(255,255,255,0.65);
    background: rgba(1, 87, 155, 0.42);
    color: var(--blanco);
    border-radius: 14px;
    padding: 4px 9px;
    font-size: 10.5px;
    cursor: pointer;
    white-space: nowrap;
    transition: all 0.15s;
  }
  .role-btn.active {
    background: var(--blanco);
    color: var(--celeste-oscuro);
    font-weight: 800;
    box-shadow: 0 2px 5px rgba(0,0,0,0.12);
  }
  .banner-msg {
    margin-top: 7px;
    background: rgba(255,255,255,0.96);
    color: var(--celeste-oscuro);
    border-radius: 8px;
    padding: 6px 9px;
    font-size: 11px;
    font-weight: 600;
    line-height: 1.3;
  }

  .phone-body {
    flex: 1;
    overflow-y: auto;
    background: var(--celeste-suave);
    padding: 11px;
  }
  .card {
    background: var(--blanco);
    border: 1px solid var(--celeste-borde);
    border-radius: 13px;
    padding: 12px;
    margin-bottom: 10px;
    box-shadow: 0 2px 7px rgba(2, 136, 209, 0.06);
  }
  .card.highlight {
    background: var(--celeste-claro);
    border: 1.5px solid var(--celeste-primario);
  }
  .card h3 {
    margin: 0 0 6px 0;
    font-size: 13.5px;
    color: var(--celeste-oscuro);
  }
  .card p {
    margin: 4px 0;
    font-size: 11.5px;
    line-height: 1.4;
    color: var(--texto-principal);
  }
  .btn-primary {
    width: 100%;
    background: var(--celeste-primario);
    color: var(--blanco);
    border: none;
    border-radius: 10px;
    padding: 9px 12px;
    font-weight: 700;
    font-size: 12px;
    cursor: pointer;
    margin-top: 6px;
    transition: background 0.15s, transform 0.08s;
  }
  .btn-primary:hover { background: var(--celeste-oscuro); }
  .btn-primary:active { transform: scale(0.99); }
  .btn-success {
    width: 100%;
    background: var(--verde-ok);
    color: var(--blanco);
    border: none;
    border-radius: 10px;
    padding: 9px 12px;
    font-weight: 700;
    font-size: 12px;
    cursor: pointer;
    margin-top: 6px;
  }
  .btn-warning {
    width: 100%;
    background: var(--ambar);
    color: var(--blanco);
    border: none;
    border-radius: 10px;
    padding: 9px 12px;
    font-weight: 700;
    font-size: 12px;
    cursor: pointer;
    margin-top: 6px;
  }
  .btn-outline {
    background: var(--blanco);
    color: var(--celeste-primario);
    border: 1px solid var(--celeste-primario);
    border-radius: 8px;
    padding: 6px 10px;
    font-size: 11px;
    font-weight: 700;
    cursor: pointer;
  }
  .btn-outline:hover { background: var(--celeste-claro); }
  .btn-danger {
    background: var(--rojo-alerta);
    color: var(--blanco);
    border: none;
    border-radius: 8px;
    padding: 6px 10px;
    font-size: 11px;
    font-weight: 700;
    cursor: pointer;
  }

  .chain-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 5px;
    margin: 8px 0;
  }
  .chain-step {
    text-align: center;
    padding: 5px 3px;
    border-radius: 7px;
    font-size: 9.5px;
    border: 1px solid var(--celeste-borde);
    background: #F5F9FC;
    color: #607D8B;
  }
  .chain-step.active {
    background: var(--celeste-primario);
    color: var(--blanco);
    font-weight: 800;
  }
  .chain-step.done {
    background: var(--celeste-claro);
    color: var(--celeste-oscuro);
    font-weight: 700;
  }

  .phone-nav {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    background: var(--blanco);
    border-top: 1px solid var(--celeste-borde);
  }
  .nav-item {
    padding: 8px 2px;
    text-align: center;
    font-size: 10px;
    color: #546E7A;
    cursor: pointer;
    border: none;
    background: none;
    font-weight: 600;
    line-height: 1.25;
  }
  .nav-item.active {
    color: var(--celeste-primario);
    background: var(--celeste-claro);
    font-weight: 800;
  }

  /* Panel derecho: Navegación Waze en vivo */
  .right-panel {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }
  .waze-banner {
    background: linear-gradient(90deg, var(--celeste-oscuro) 0%, var(--celeste-primario) 55%, var(--celeste-brillante) 100%);
    color: var(--blanco);
    border-radius: 16px;
    padding: 14px 18px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 6px 20px rgba(2, 136, 209, 0.22);
    gap: 12px;
  }
  .waze-next-turn {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .turn-icon-box {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    background: rgba(255,255,255,0.18);
    border: 1.5px solid rgba(255,255,255,0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    flex-shrink: 0;
  }
  .waze-eta-box {
    background: var(--blanco);
    color: var(--celeste-oscuro);
    border-radius: 13px;
    padding: 8px 16px;
    text-align: center;
    min-width: 130px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.12);
  }
  .waze-eta-box .big { font-size: 21px; font-weight: 900; display: block; }
  .waze-eta-box .small { font-size: 12px; font-weight: 700; color: var(--celeste-primario); }

  .map-wrapper {
    position: relative;
  }
  #mapWaze {
    width: 100%;
    height: 455px;
    border-radius: 16px;
    border: 2px solid var(--celeste-primario);
    box-shadow: 0 6px 20px rgba(1, 87, 155, 0.14);
  }
  .map-floating-bar {
    position: absolute;
    bottom: 14px;
    left: 14px;
    right: 14px;
    z-index: 500;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
    pointer-events: none;
    flex-wrap: wrap;
  }
  .map-legend-pill {
    pointer-events: auto;
    background: rgba(255, 255, 255, 0.94);
    border: 1px solid var(--celeste-borde);
    color: var(--celeste-oscuro);
    padding: 6px 12px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 700;
    box-shadow: 0 2px 10px rgba(0,0,0,0.12);
  }

  .waze-controls-grid {
    display: grid;
    grid-template-columns: 1.2fr 1fr;
    gap: 14px;
  }
  @media (max-width: 1220px) {
    .waze-controls-grid { grid-template-columns: 1fr; }
  }
  .step-item {
    background: var(--celeste-claro);
    color: var(--celeste-oscuro);
    padding: 7px 10px;
    border-radius: 8px;
    font-size: 11.5px;
    margin-bottom: 5px;
    font-weight: 600;
    border-left: 3px solid var(--celeste-primario);
  }
  .tramo-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 7px 9px;
    border-radius: 8px;
    background: #F4FAFF;
    border: 1px solid var(--celeste-borde);
    margin-bottom: 6px;
    font-size: 11px;
    gap: 8px;
  }
  .tramo-row.blocked {
    background: var(--rojo-fondo);
    border-color: #EF9A9A;
  }
  input, select, textarea {
    width: 100%;
    padding: 7px 9px;
    border: 1px solid var(--celeste-borde);
    border-radius: 8px;
    font-size: 12px;
    margin-top: 3px;
    margin-bottom: 8px;
    background: var(--blanco);
    color: var(--texto-principal);
  }
  label { font-size: 11.5px; font-weight: 700; color: var(--celeste-oscuro); }
  .badge-state {
    display: inline-block;
    padding: 3px 9px;
    border-radius: 999px;
    font-size: 10.5px;
    font-weight: 800;
  }
  .badge-state.ok { background: var(--verde-fondo); color: var(--verde-ok); }
  .badge-state.info { background: var(--celeste-claro); color: var(--celeste-oscuro); }
  .badge-state.warn { background: var(--ambar-fondo); color: var(--ambar); }
  .badge-state.danger { background: var(--rojo-fondo); color: var(--rojo-alerta); }
</style>
</head>
<body>

<div class="top-pc-bar">
  <div class="brand-row">
    <div class="brand-logo">SOS</div>
    <div>
      <h1>NEXO SOS · SAN RAFAEL DEL SUR</h1>
      <div class="subtitle">Coordinación de Emergencias y Navegación Waze en Tiempo Real · Masachapa · San Rafael · El Crucero · Managua</div>
    </div>
  </div>
  <div class="top-badges">
    <span class="badge-pc">📡 Red de Radio Activa (3 Torres)</span>
    <span class="badge-pc white" id="topActiveBrigade">🚑 Brigada en ruta hacia Masachapa</span>
    <a href="/descargar-apk" class="badge-pc white" style="text-decoration:none;background:#E1F5FE;color:#01579B;border:1.5px solid #FFFFFF;cursor:pointer;">📲 Descargar APK Android</a>
  </div>
</div>

<div class="workspace">
  <!-- IZQUIERDA: APLICACIÓN ANDROID -->
  <div class="android-phone">
    <div class="phone-notch">
      <span id="phoneClock">16:10 · Nexo SOS</span>
      <span>GPS Precisión ±6 m · 🔋 88%</span>
    </div>

    <div class="app-header">
      <div class="app-title-row">
        <h2>Nexo SOS · San Rafael del Sur</h2>
        <span class="corridor-pill">Corredor Costero</span>
      </div>
      <div class="app-sub" id="modoRotulo">Comunicación por radio activa entre Masachapa, San Rafael, El Crucero y Managua</div>
      <div class="status-chips">
        <span class="chip-status" id="chipInternet">Sin Internet (Modo Radio)</span>
        <span class="chip-status" id="chipRadio">Radio: Conectado (88%)</span>
        <span class="chip-status" id="chipMalla">Cobertura: 3 Torres</span>
        <span class="chip-status" id="chipManagua">Enlace Managua: Activo</span>
      </div>
      <div class="role-bar" id="roleBar"></div>
      <div class="banner-msg" id="bannerMsg">Corredor San Rafael del Sur activo. Selecciona una opción o avanza la simulación en vivo.</div>
    </div>

    <div class="phone-body" id="phoneBody"></div>

    <div class="phone-nav">
      <button class="nav-item active" onclick="switchTab(0)" id="nav0">🏠<br>Inicio</button>
      <button class="nav-item" onclick="switchTab(1)" id="nav1">🆘<br>Pedir Ayuda</button>
      <button class="nav-item" onclick="switchTab(2)" id="nav2">📋<br>Seguimiento</button>
      <button class="nav-item" onclick="switchTab(3)" id="nav3">🏛️<br>Atención</button>
      <button class="nav-item" onclick="switchTab(4)" id="nav4">📦<br>Recursos</button>
    </div>
  </div>

  <!-- DERECHA: MAPA INTERACTIVO ESTILO WAZE -->
  <div class="right-panel">
    <div class="waze-banner">
      <div class="waze-next-turn">
        <div class="turn-icon-box" id="wazeTurnIcon">↱</div>
        <div>
          <div style="font-size:11.5px;color:#E1F5FE;font-weight:700;text-transform:uppercase;letter-spacing:0.4px;">
            Navegación Waze de Emergencia · Ruta más óptima
          </div>
          <div style="font-size:18px;font-weight:900;margin-top:2px;" id="wazeTitleRoute">
            Alcaldía San Rafael del Sur → Escuela Comarca Masachapa
          </div>
          <div style="font-size:12px;margin-top:3px;color:#E1F5FE;" id="wazeAvoidText">
            ⚠️ Evitando bloqueo activo: Bajada La Sierra NIC-2 (Derrumbe en El Salto Km 34)
          </div>
        </div>
      </div>
      <div class="waze-eta-box">
        <span class="big" id="wazeEtaMin">12 min</span>
        <span class="small" id="wazeDistKm">11.5 km</span>
      </div>
    </div>

    <div class="map-wrapper">
      <div id="mapWaze"></div>
      <div class="map-floating-bar">
        <span class="map-legend-pill">━ Ruta recomendada Waze (Celeste) · ⛔ Vía cerrada (Rojo) · 📡 Torres de radio</span>
        <button class="btn-primary" style="pointer-events:auto;width:auto;margin:0;padding:7px 14px;box-shadow:0 3px 10px rgba(0,0,0,0.2);" onclick="toggleAutoPlayBrigade()" id="btnAutoBrigade">
          ▶ Animar Brigada en Vivo
        </button>
      </div>
    </div>

    <div class="waze-controls-grid">
      <div class="card" style="margin:0;">
        <h3>🧭 Indicaciones Paso a Paso hacia la Emergencia</h3>
        <div style="display:flex;gap:6px;margin-bottom:9px;flex-wrap:wrap;">
          <button class="btn-outline" onclick="setWazeOriginDest('NODO_SAN_RAFAEL','NODO_MASACHAPA')">Alcaldía → Masachapa</button>
          <button class="btn-outline" onclick="setWazeOriginDest('NODO_MANAGUA','NODO_MASACHAPA')">Managua → Masachapa</button>
          <button class="btn-outline" onclick="setWazeOriginDest('NODO_EL_CRUCERO','NODO_POCHOMIL')">El Crucero → Pochomil</button>
          <button class="btn-outline" onclick="advanceBrigadeWaze()">🚑 Avanzar +20%</button>
        </div>
        <div id="wazeStepsContainer"></div>
      </div>

      <div class="card" style="margin:0;">
        <h3>🚧 Estado de Carreteras en Vivo (Toca para reportar o despejar)</h3>
        <p style="margin-top:0;margin-bottom:8px;color:#455A64;">
          Al reportar un derrumbe o inundación, el mapa recalcula automáticamente el mejor desvío para las brigadas:
        </p>
        <div id="wazeTramosContainer"></div>
      </div>
    </div>
  </div>
</div>

<script>
// ============================================================================
// PUNTOS Y CARRETERAS REALES DE SAN RAFAEL DEL SUR (CON GEOMETRÍA CURVA REAL)
// ============================================================================
const NODOS_SRS = {
  NODO_MANAGUA:     { id: "NODO_MANAGUA", nombre: "Sede Central Managua (7 Sur)", lat: 12.1180, lon: -86.2890, rol: "4. Sede Central Managua", torre: null },
  NODO_EL_CRUCERO:  { id: "NODO_EL_CRUCERO", nombre: "Cumbre El Crucero / Las Nubes", lat: 11.9921, lon: -86.3085, rol: "3. Coordinación Departamental · Torre El Crucero", torre: "Torre El Crucero" },
  NODO_STA_RITA:    { id: "NODO_STA_RITA", nombre: "Empalme Santa Rita (Ctra. Vieja a León)", lat: 11.9820, lon: -86.4620, rol: "Desvío Carretera Vieja a León", torre: null },
  NODO_EL_SALTO:    { id: "NODO_EL_SALTO", nombre: "Comarca El Salto (Km 34)", lat: 11.8920, lon: -86.3880, rol: "Comarca Interior", torre: null },
  NODO_SAN_BARTOLO: { id: "NODO_SAN_BARTOLO", nombre: "Comarca San Bartolo / Chilamatillo", lat: 11.8840, lon: -86.4450, rol: "Enlace Vial Alterno", torre: null },
  NODO_SAN_RAFAEL:  { id: "NODO_SAN_RAFAEL", nombre: "Alcaldía de San Rafael del Sur", lat: 11.8465, lon: -86.4389, rol: "2. Coordinación Municipal · Torre San Rafael", torre: "Torre San Rafael" },
  NODO_MONTELIMAR:  { id: "NODO_MONTELIMAR", nombre: "Cruce Costero Montelimar", lat: 11.8125, lon: -86.5110, rol: "Comarca Costera", torre: null },
  NODO_MASACHAPA:   { id: "NODO_MASACHAPA", nombre: "Escuela Comarca Masachapa", lat: 11.7809, lon: -86.5193, rol: "1. Comarca Masachapa · Torre Costera", torre: "Torre Masachapa" },
  NODO_POCHOMIL:    { id: "NODO_POCHOMIL", nombre: "Centro Comunitario Pochomil", lat: 11.7724, lon: -86.5044, rol: "Albergue Costero Pochomil", torre: null }
};

let tramosWaze = [
  {
    id: "T1", codigo: "NIC-2", nombre: "Carretera Sur (Managua 7 Sur ↔ El Crucero)",
    u: "NODO_MANAGUA", v: "NODO_EL_CRUCERO", km: 21.5, min: 24, bloqueado: false,
    alerta: "Paso vehicular fluido",
    path: [[12.1180,-86.2890],[12.0890,-86.2980],[12.0540,-86.3040],[12.0210,-86.3070],[11.9921,-86.3085]]
  },
  {
    id: "T2", codigo: "NIC-2", nombre: "Bajada La Sierra (El Crucero ↔ El Salto Km 34)",
    u: "NODO_EL_CRUCERO", v: "NODO_EL_SALTO", km: 13.8, min: 16, bloqueado: true,
    alerta: "⛔ Derrumbe de talud en El Salto Km 34",
    path: [[11.9921,-86.3085],[11.9680,-86.3320],[11.9410,-86.3550],[11.9150,-86.3740],[11.8920,-86.3880]]
  },
  {
    id: "T3", codigo: "NIC-2", nombre: "El Salto ↔ Casco Urbano San Rafael del Sur",
    u: "NODO_EL_SALTO", v: "NODO_SAN_RAFAEL", km: 8.4, min: 10, bloqueado: false,
    alerta: "Tránsito habilitado",
    path: [[11.8920,-86.3880],[11.8740,-86.4090],[11.8590,-86.4250],[11.8465,-86.4389]]
  },
  {
    id: "T4", codigo: "NIC-12", nombre: "Carretera Vieja a León (Managua ↔ Santa Rita)",
    u: "NODO_MANAGUA", v: "NODO_STA_RITA", km: 24.0, min: 26, bloqueado: false,
    alerta: "Vía alterna pavimentada despejada",
    path: [[12.1180,-86.2890],[12.0960,-86.3390],[12.0610,-86.3880],[12.0190,-86.4310],[11.9820,-86.4620]]
  },
  {
    id: "T5", codigo: "NIC-12B", nombre: "Empalme Santa Rita ↔ San Bartolo",
    u: "NODO_STA_RITA", v: "NODO_SAN_BARTOLO", km: 11.2, min: 14, bloqueado: false,
    alerta: "Puente y calzada verificados",
    path: [[11.9820,-86.4620],[11.9510,-86.4580],[11.9180,-86.4510],[11.8840,-86.4450]]
  },
  {
    id: "T6", codigo: "SIERRA", nombre: "Camino El Crucero ↔ San Bartolo",
    u: "NODO_EL_CRUCERO", v: "NODO_SAN_BARTOLO", km: 15.0, min: 22, bloqueado: false,
    alerta: "Transitable para camionetas y brigadas",
    path: [[11.9921,-86.3085],[11.9640,-86.3590],[11.9260,-86.4040],[11.8840,-86.4450]]
  },
  {
    id: "T7", codigo: "NIC-2N", nombre: "San Bartolo ↔ Alcaldía San Rafael del Sur",
    u: "NODO_SAN_BARTOLO", v: "NODO_SAN_RAFAEL", km: 6.5, min: 8, bloqueado: false,
    alerta: "Acceso norte despejado",
    path: [[11.8840,-86.4450],[11.8650,-86.4420],[11.8465,-86.4389]]
  },
  {
    id: "T8", codigo: "COSTA-1", nombre: "Vía Directa San Rafael del Sur ↔ Masachapa",
    u: "NODO_SAN_RAFAEL", v: "NODO_MASACHAPA", km: 11.5, min: 12, bloqueado: false,
    alerta: "Puente Río Masachapa habilitado",
    path: [[11.8465,-86.4389],[11.8260,-86.4660],[11.8030,-86.4930],[11.7809,-86.5193]]
  },
  {
    id: "T9", codigo: "MONTE", nombre: "Desvío San Rafael del Sur ↔ Montelimar",
    u: "NODO_SAN_RAFAEL", v: "NODO_MONTELIMAR", km: 10.2, min: 13, bloqueado: false,
    alerta: "Ruta alterna por Montelimar",
    path: [[11.8465,-86.4389],[11.8350,-86.4750],[11.8125,-86.5110]]
  },
  {
    id: "T10", codigo: "COSTA-N", nombre: "Montelimar ↔ Comarca Masachapa",
    u: "NODO_MONTELIMAR", v: "NODO_MASACHAPA", km: 4.6, min: 6, bloqueado: false,
    alerta: "Acceso costero norte libre",
    path: [[11.8125,-86.5110],[11.7960,-86.5155],[11.7809,-86.5193]]
  },
  {
    id: "T11", codigo: "POCHO", nombre: "San Rafael del Sur ↔ Pochomil",
    u: "NODO_SAN_RAFAEL", v: "NODO_POCHOMIL", km: 13.0, min: 15, bloqueado: false,
    alerta: "Vía hacia albergue costero libre",
    path: [[11.8465,-86.4389],[11.8150,-86.4690],[11.7724,-86.5044]]
  },
  {
    id: "T12", codigo: "COSTA-S", nombre: "Bulevar Costero Masachapa ↔ Pochomil",
    u: "NODO_MASACHAPA", v: "NODO_POCHOMIL", km: 2.8, min: 4, bloqueado: false,
    alerta: "Conexión costera habilitada",
    path: [[11.7809,-86.5193],[11.7765,-86.5120],[11.7724,-86.5044]]
  }
];

const ROLES = [
  { id: "COMUNIDAD", titulo: "Brigada / Comunidad", nivel: "COMARCA" },
  { id: "COMARCA", titulo: "1. Comarca Masachapa", nivel: "COMARCA" },
  { id: "MUNICIPIO", titulo: "2. Alcaldía San Rafael", nivel: "MUNICIPIO" },
  { id: "DEPARTAMENTO", titulo: "3. Coord. Departamental", nivel: "CABECERA_DEPARTAMENTAL" },
  { id: "MANAGUA", titulo: "4. Sede Central Managua", nivel: "SEDE_CENTRAL_MANAGUA" }
];

const NOMBRES_NIVEL = {
  COMARCA: "Comarca Masachapa",
  MUNICIPIO: "Alcaldía de San Rafael del Sur",
  CABECERA_DEPARTAMENTAL: "Coordinación Departamental (El Crucero)",
  SEDE_CENTRAL_MANAGUA: "Sede Central Managua"
};

const ETIQUETAS_ESTADO = {
  PENDIENTE_TRANSMISION: { texto: "Pendiente de envío (sin radio)", clase: "warn" },
  ENVIADO_A_RADIO:       { texto: "Enviado por radio", clase: "info" },
  ACK_TECNICO_RADIO:     { texto: "Señal repetida por torre · Esperando autoridad", clase: "info" },
  RECIBIDO_AUTORIDAD:    { texto: "Recibido por la autoridad", clase: "ok" },
  ACEPTADO_PARCIAL:      { texto: "Atendido parcialmente · Resto en apoyo superior", clase: "warn" },
  ESCALADO:              { texto: "Derivado al siguiente nivel", clase: "info" },
  SIN_CONFIRMACION:      { texto: "Derivado automáticamente por tiempo agotado", clase: "warn" },
  EN_CAMINO:             { texto: "Brigada en camino por ruta Waze", clase: "ok" },
  ATENDIDO:              { texto: "Atendido y completado", clase: "ok" },
  CANCELADO:             { texto: "Cancelado", clase: "danger" }
};

let state = {
  tab: 0,
  rolIdx: 0,
  radioVinculado: true,
  internetDisponible: false,
  managuaAlcanzable: true,
  pasoSimulacion: 0,
  wazeOrigen: "NODO_MANAGUA",
  wazeDestino: "NODO_MASACHAPA",
  brigadaPct: 0.28,
  autoPlayTimer: null,
  txCount: 2,
  rxCount: 2,
  mensajesTorre: [
    {
      hora: new Date().toLocaleTimeString(),
      torre: "Torre 1 · Masachapa (!a1b201)",
      senal: "-67 dBm · SNR +9.5 dB",
      resumen: "SOS-0104 recibido desde Escuela Pública de Comarca Masachapa (10 kits de agua y alimentos)"
    }
  ],
  inventario: [
    { id: "RES-COM", nivel: "COMARCA", codigo: "MAS-KIT-01", nombre: "Kits de emergencia en Puesto Masachapa", total: 2, reservado: 2, resp: "María López (Comarca Masachapa)" },
    { id: "RES-MUN", nivel: "MUNICIPIO", codigo: "SRS-KIT-04", nombre: "Kits de agua y alimentos en Alcaldía San Rafael", total: 4, reservado: 0, resp: "Ing. Carlos Rivas (San Rafael del Sur)" },
    { id: "RES-DEP", nivel: "CABECERA_DEPARTAMENTAL", codigo: "DEP-KIT-06", nombre: "Reserva Departamental El Crucero", total: 3, reservado: 0, resp: "Dra. Elena Morales (Coordinación Depto.)" },
    { id: "RES-MGA", nivel: "SEDE_CENTRAL_MANAGUA", codigo: "MGA-KIT-20", nombre: "Bodega Nacional de Respuesta Managua", total: 25, reservado: 0, resp: "Dir. Roberto Blandón (Sede Central)" }
  ],
  solicitudes: [
    {
      uuid: "8f91c2a0-2026-4b10-9a11-masachapa001",
      folio: "SOS-0104",
      rev: 1,
      nivelActual: "COMARCA",
      estado: "ENVIADO_A_RADIO",
      necesidad: "Agua potable y alimentos",
      unidad: "kits",
      solicitada: 10,
      aceptada: 0,
      pendiente: 10,
      urgencia: "Urgencia crítica · Riesgo vital",
      personas: 42,
      ubicacion: "Escuela Pública de Comarca Masachapa",
      lat: 11.7809,
      lon: -86.5193,
      acuse60Recibido: false,
      seg60: 54,
      seg180: 174,
      asignaciones: [],
      historial: [
        {
          hora: new Date().toLocaleTimeString(),
          estado: "Enviado por radio",
          autor: "Brigada Costera Masachapa",
          detalle: "Solicitud creada por 10 kits para 42 personas evacuadas en la Escuela de Masachapa y transmitida a la torre local."
        }
      ]
    }
  ]
};

// ============================================================================
// CÁLCULO DE LA RUTA MÁS ÓPTIMA ESTILO WAZE
// ============================================================================
function obtenerPuntosTramoEnOrden(tramo, desdeId) {
  const pts = tramo.path || [
    [NODOS_SRS[tramo.u].lat, NODOS_SRS[tramo.u].lon],
    [NODOS_SRS[tramo.v].lat, NODOS_SRS[tramo.v].lon]
  ];
  return tramo.u === desdeId ? pts : pts.slice().reverse();
}

function calcularRutaOptimaWaze(origenId, destinoId) {
  const dist = {};
  const prevNodo = {};
  const prevTramo = {};
  const visitados = new Set();

  Object.keys(NODOS_SRS).forEach(k => dist[k] = Infinity);
  dist[origenId] = 0;

  while (visitados.size < Object.keys(NODOS_SRS).length) {
    let u = null;
    let best = Infinity;
    for (const k of Object.keys(NODOS_SRS)) {
      if (!visitados.has(k) && dist[k] < best) {
        best = dist[k];
        u = k;
      }
    }
    if (!u || best === Infinity || u === destinoId) break;
    visitados.add(u);

    for (const tr of tramosWaze) {
      if (tr.bloqueado) continue;
      if (tr.u !== u && tr.v !== u) continue;
      const vecino = tr.u === u ? tr.v : tr.u;
      if (visitados.has(vecino)) continue;
      const alt = dist[u] + tr.min;
      if (alt < dist[vecino]) {
        dist[vecino] = alt;
        prevNodo[vecino] = u;
        prevTramo[vecino] = tr;
      }
    }
  }

  const bloqueados = tramosWaze.filter(t => t.bloqueado);
  if (dist[destinoId] === Infinity) {
    return {
      disponible: false,
      nodos: [NODOS_SRS[origenId]],
      tramos: [],
      geometria: [[NODOS_SRS[origenId].lat, NODOS_SRS[origenId].lon]],
      bloqueados,
      km: 0,
      min: 0,
      pasos: ["⛔ Alerta: Todos los accesos terrestres hacia " + NODOS_SRS[destinoId].nombre + " se encuentran cerrados."]
    };
  }

  const caminoNodos = [];
  const caminoTramos = [];
  let cur = destinoId;
  while (cur) {
    caminoNodos.unshift(NODOS_SRS[cur]);
    if (prevTramo[cur]) caminoTramos.unshift(prevTramo[cur]);
    cur = prevNodo[cur];
  }

  const geometria = [];
  for (let i = 0; i < caminoTramos.length; i++) {
    const segPts = obtenerPuntosTramoEnOrden(caminoTramos[i], caminoNodos[i].id);
    segPts.forEach((pt, idx) => {
      if (i > 0 && idx === 0) return;
      geometria.push(pt);
    });
  }

  const totalKm = Math.round(caminoTramos.reduce((a, b) => a + b.km, 0) * 10) / 10;
  const totalMin = Math.round(caminoTramos.reduce((a, b) => a + b.min, 0));
  const pasos = [`🚑 Salida de brigada desde ${NODOS_SRS[origenId].nombre}`];
  if (bloqueados.length > 0) {
    pasos.push(`⚡ Ruta recalculada evitando: ${bloqueados.map(b => b.nombre).join(", ")}`);
  }
  caminoTramos.forEach((t, idx) => {
    const nextNode = caminoNodos[idx + 1];
    pasos.push(`${idx + 1}. Continuar por [${t.codigo}] ${t.nombre} hacia ${nextNode.nombre} (${t.km} km · ~${t.min} min)`);
  });
  pasos.push(`🏁 Llegada al punto de emergencia: ${NODOS_SRS[destinoId].nombre}`);

  return { disponible: true, nodos: caminoNodos, tramos: caminoTramos, geometria, bloqueados, km: totalKm, min: totalMin, pasos };
}

// ============================================================================
// MAPA INTERACTIVO ESTILO WAZE (LEAFLET)
// ============================================================================
let map, layerGroup;
function initMap() {
  if (typeof L === 'undefined') {
    setTimeout(initMap, 250);
    return;
  }
  map = L.map('mapWaze').setView([11.92, -86.41], 10);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 18,
    attribution: '© OpenStreetMap · Nexo SOS San Rafael del Sur'
  }).addTo(map);
  layerGroup = L.layerGroup().addTo(map);
  renderWazeMap();
}

function renderWazeMap() {
  const ruta = calcularRutaOptimaWaze(state.wazeOrigen, state.wazeDestino);
  const tramosOptimosIds = new Set(ruta.tramos.map(t => t.id));

  if (layerGroup) {
    layerGroup.clearLayers();

    // 1. Dibujar carreteras con sus curvas reales
    tramosWaze.forEach(tr => {
      const coords = tr.path || [[NODOS_SRS[tr.u].lat, NODOS_SRS[tr.u].lon], [NODOS_SRS[tr.v].lat, NODOS_SRS[tr.v].lon]];
      const esOptimo = tramosOptimosIds.has(tr.id);

      if (esOptimo) {
        L.polyline(coords, {
          color: '#01579B', weight: 13, opacity: 0.32, lineCap: 'round'
        }).addTo(layerGroup);
      }

      const color = tr.bloqueado ? '#D32F2F' : (esOptimo ? '#00B0FF' : '#78909C');
      const dashArray = tr.bloqueado ? '9, 9' : null;
      const poly = L.polyline(coords, {
        color, weight: esOptimo ? 7 : 4, opacity: 0.95, dashArray, lineCap: 'round'
      }).addTo(layerGroup);

      poly.on('click', () => toggleTramoWaze(tr.id));
      poly.bindPopup(`<b>[${tr.codigo}] ${tr.nombre}</b><br>${tr.km} km · ~${tr.min} min<br>${tr.alerta}<br><i>(Haz clic en la vía para bloquear o despejar)</i>`);

      if (tr.bloqueado) {
        const mid = coords[Math.floor(coords.length / 2)];
        L.circleMarker(mid, {
          radius: 10, fillColor: '#D32F2F', color: '#FFFFFF', weight: 2.5, fillOpacity: 1
        }).addTo(layerGroup).bindPopup(`<b>⛔ VÍA BLOQUEADA</b><br>${tr.nombre}<br>${tr.alerta}`);
      }
    });

    // 2. Dibujar puntos del corredor y cobertura de las 3 torres
    Object.values(NODOS_SRS).forEach(n => {
      if (n.torre) {
        L.circle([n.lat, n.lon], {
          radius: 3600, color: '#0288D1', weight: 1, fillColor: '#03A9F4', fillOpacity: 0.10
        }).addTo(layerGroup);
      }
      const esDest = n.id === state.wazeDestino;
      const esOrig = n.id === state.wazeOrigen;
      const fill = esDest ? '#D50000' : (esOrig ? '#00C853' : (n.torre ? '#0288D1' : '#37474F'));

      L.circleMarker([n.lat, n.lon], {
        radius: n.torre ? 9 : 7,
        fillColor: fill,
        color: '#FFFFFF',
        weight: 2.5,
        fillOpacity: 1
      }).addTo(layerGroup).bindPopup(`<b>${n.nombre}</b><br>${n.rol}`);
    });

    // 3. Dibujar vehículo de la brigada avanzando sobre la geometría real de la ruta
    if (ruta.geometria && ruta.geometria.length >= 2) {
      const segs = ruta.geometria.length - 1;
      const scaled = Math.min(0.999, Math.max(0, state.brigadaPct)) * segs;
      const idx = Math.floor(scaled);
      const frac = scaled - idx;
      const pA = ruta.geometria[idx];
      const pB = ruta.geometria[idx + 1];
      const vLat = pA[0] + (pB[0] - pA[0]) * frac;
      const vLon = pA[1] + (pB[1] - pA[1]) * frac;

      L.circleMarker([vLat, vLon], {
        radius: 15, fillColor: '#0288D1', color: '#FFFFFF', weight: 2, fillOpacity: 0.25
      }).addTo(layerGroup);

      L.circleMarker([vLat, vLon], {
        radius: 10, fillColor: '#00B0FF', color: '#FFFFFF', weight: 3, fillOpacity: 1
      }).addTo(layerGroup).bindPopup(`<b>🚑 Brigada en camino (${Math.round(state.brigadaPct*100)}%)</b><br>Destino: ${NODOS_SRS[state.wazeDestino].nombre}`);
    }
  }

  // Actualizar textos de navegación Waze
  const origNombre = NODOS_SRS[state.wazeOrigen].nombre.split(' (')[0];
  const destNombre = NODOS_SRS[state.wazeDestino].nombre.split(' (')[0];
  document.getElementById('wazeTitleRoute').textContent = `${origNombre} → ${destNombre}`;
  document.getElementById('topActiveBrigade').textContent = `🚑 Brigada: ${origNombre} → ${destNombre} (${Math.round(state.brigadaPct*100)}%)`;
  document.getElementById('wazeEtaMin').textContent = ruta.disponible ? `${ruta.min} min` : 'Sin paso';
  document.getElementById('wazeDistKm').textContent = `${ruta.km} km`;
  document.getElementById('wazeAvoidText').textContent = ruta.bloqueados.length > 0
    ? `⚠️ Desvío activo evitando: ${ruta.bloqueados.map(b => b.nombre).join(' | ')}`
    : `✅ Todas las carreteras del corredor San Rafael del Sur están despejadas`;

  document.getElementById('wazeStepsContainer').innerHTML =
    ruta.pasos.map(p => `<div class="step-item">${p}</div>`).join('');

  document.getElementById('wazeTramosContainer').innerHTML =
    tramosWaze.map(t => `
      <div class="tramo-row ${t.bloqueado ? 'blocked' : ''}">
        <div>
          <b>[${t.codigo}] ${t.nombre}</b> (${t.km} km · ~${t.min} min)<br>
          <span style="color:${t.bloqueado ? '#C62828' : '#0277BD'};font-weight:600;">${t.alerta}</span>
        </div>
        <button class="${t.bloqueado ? 'btn-outline' : 'btn-danger'}" onclick="toggleTramoWaze('${t.id}')">
          ${t.bloqueado ? 'Despejar vía' : 'Reportar bloqueo'}
        </button>
      </div>
    `).join('');
}

function toggleTramoWaze(id) {
  const tr = tramosWaze.find(t => t.id === id);
  if (!tr) return;
  tr.bloqueado = !tr.bloqueado;
  tr.alerta = tr.bloqueado
    ? "⛔ Vía cerrada (Reporte en vivo: Derrumbe / Inundación)"
    : "✅ Vía despejada y verificada por brigada";
  const r = calcularRutaOptimaWaze(state.wazeOrigen, state.wazeDestino);
  setBanner(r.disponible
    ? `Ruta Waze recalculada automáticamente: ${r.km} km · Tiempo estimado ${r.min} min.`
    : `Alerta vial: no hay paso disponible hacia el destino seleccionado.`);
  renderWazeMap();
}

function setWazeOriginDest(orig, dest) {
  state.wazeOrigen = orig;
  state.wazeDestino = dest;
  state.brigadaPct = 0.15;
  const r = calcularRutaOptimaWaze(orig, dest);
  setBanner(`Ruta seleccionada: ${NODOS_SRS[orig].nombre} → ${NODOS_SRS[dest].nombre} (${r.km} km · ${r.min} min).`);
  renderWazeMap();
}

function advanceBrigadeWaze() {
  state.brigadaPct += 0.20;
  if (state.brigadaPct > 1.0) state.brigadaPct = 0.05;
  setBanner(`Brigada avanzando hacia ${NODOS_SRS[state.wazeDestino].nombre} (${Math.round(state.brigadaPct*100)}% del recorrido completado).`);
  renderWazeMap();
}

function toggleAutoPlayBrigade() {
  const btn = document.getElementById('btnAutoBrigade');
  if (state.autoPlayTimer) {
    clearInterval(state.autoPlayTimer);
    state.autoPlayTimer = null;
    btn.textContent = "▶ Animar Brigada en Vivo";
  } else {
    btn.textContent = "⏸ Pausar Brigada";
    state.autoPlayTimer = setInterval(() => {
      state.brigadaPct += 0.04;
      if (state.brigadaPct > 1.0) state.brigadaPct = 0.0;
      renderWazeMap();
    }, 450);
  }
}

// ============================================================================
// INTERFAZ INTUITIVA DEL TELÉFONO ANDROID
// ============================================================================
function setBanner(msg) {
  document.getElementById('bannerMsg').textContent = msg;
}

function renderRoleBar() {
  document.getElementById('roleBar').innerHTML = ROLES.map((r, i) => `
    <button class="role-btn ${state.rolIdx === i ? 'active' : ''}" onclick="selectRole(${i})">${r.titulo}</button>
  `).join('');
}

function selectRole(idx) {
  state.rolIdx = idx;
  setBanner(`Vista activa: ${ROLES[idx].titulo}`);
  renderRoleBar();
  renderPhoneBody();
}

function switchTab(t) {
  state.tab = t;
  for (let i = 0; i < 5; i++) {
    document.getElementById('nav' + i).className = 'nav-item' + (i === t ? ' active' : '');
  }
  renderPhoneBody();
}

function updateTopChips() {
  const cInt = document.getElementById('chipInternet');
  cInt.textContent = state.internetDisponible ? "Internet: Conectado" : "Sin Internet (Modo Radio)";
  cInt.className = 'chip-status';

  const cRad = document.getElementById('chipRadio');
  cRad.textContent = state.radioVinculado ? "Radio: Conectado (88%)" : "Radio: Desconectado";
  cRad.className = 'chip-status' + (state.radioVinculado ? '' : ' warn');

  const cMga = document.getElementById('chipManagua');
  cMga.textContent = state.managuaAlcanzable ? "Enlace Managua: Activo" : "Managua: Sin enlace";
  cMga.className = 'chip-status' + (state.managuaAlcanzable ? '' : ' warn');
}

function ejecutarPasoSimulacion() {
  const sol = state.solicitudes[0];
  const ahora = new Date().toLocaleTimeString();

  if (state.pasoSimulacion === 0) {
    state.rolIdx = 1; // Comarca Masachapa
    sol.rev++;
    sol.acuse60Recibido = true;
    sol.estado = "ESCALADO";
    sol.nivelActual = "MUNICIPIO";
    sol.historial.unshift({
      hora: ahora,
      estado: "Derivado a San Rafael del Sur",
      autor: "María López (Comarca Masachapa)",
      detalle: "El puesto de Masachapa confirmó recepción y reportó falta de suministros locales. La solicitud pasa a la Alcaldía de San Rafael del Sur."
    });
    state.pasoSimulacion = 1;
    setBanner("Paso 1 completado: Comarca Masachapa sin suministros disponibles → Solicitud derivada a San Rafael del Sur.");
  } else if (state.pasoSimulacion === 1) {
    state.rolIdx = 2; // Municipio San Rafael del Sur
    sol.rev++;
    sol.aceptada = 4;
    sol.pendiente = 6;
    sol.estado = "ACEPTADO_PARCIAL";
    sol.nivelActual = "CABECERA_DEPARTAMENTAL";
    state.inventario[1].reservado = 4;
    sol.asignaciones.push("✓ Alcaldía de San Rafael del Sur: 4 kits enviados con brigada municipal (Ing. Carlos Rivas)");
    sol.historial.unshift({
      hora: ahora,
      estado: "Atendido parcialmente (4 de 10 kits)",
      autor: "Ing. Carlos Rivas (Alcaldía San Rafael del Sur)",
      detalle: "San Rafael del Sur envía 4 kits disponibles en bodega municipal y solicita apoyo por los 6 kits restantes a la Coordinación Departamental."
    });
    setWazeOriginDest('NODO_SAN_RAFAEL', 'NODO_MASACHAPA');
    state.pasoSimulacion = 2;
    setBanner("Paso 2 completado: San Rafael del Sur aporta 4 kits y deriva los 6 restantes a la Coordinación Departamental.");
  } else if (state.pasoSimulacion === 2) {
    state.rolIdx = 3; // Coordinación Departamental
    sol.rev++;
    sol.estado = "ESCALADO";
    sol.nivelActual = "SEDE_CENTRAL_MANAGUA";
    sol.historial.unshift({
      hora: ahora,
      estado: "Derivado a Sede Central Managua",
      autor: "Dra. Elena Morales (Coordinación Departamental)",
      detalle: "Por derrumbe en la bajada de El Salto, se solicita despacho directo de los 6 kits restantes desde la Sede Central en Managua por ruta alterna."
    });
    setWazeOriginDest('NODO_MANAGUA', 'NODO_MASACHAPA');
    state.pasoSimulacion = 3;
    setBanner("Paso 3 completado: Coordinación Departamental traslada el pedido de los 6 kits restantes a Managua.");
  } else {
    state.rolIdx = 4; // Sede Central Managua
    sol.rev++;
    sol.aceptada = 10;
    sol.pendiente = 0;
    sol.estado = "ATENDIDO";
    state.inventario[3].reservado = Math.min(state.inventario[3].total, state.inventario[3].reservado + 6);
    sol.asignaciones.push("✓ Sede Central Managua: 6 kits despachados por ruta Waze (Santa Rita → San Bartolo → San Rafael → Masachapa)");
    sol.historial.unshift({
      hora: ahora,
      estado: "Atendido y completado (10 de 10 kits)",
      autor: "Dir. Roberto Blandón (Sede Central Managua)",
      detalle: "Managua despachó los 6 kits restantes evitando el derrumbe de El Salto. Entrega confirmada en la Escuela de Masachapa."
    });
    state.brigadaPct = 1.0;
    state.pasoSimulacion = 0;
    setBanner("Atención completada: 10 de 10 kits entregados en Comarca Masachapa por la ruta óptima Waze.");
  }

  sincronizarPasarelaPython(sol);
  renderRoleBar();
  renderWazeMap();
  renderPhoneBody();
}

function sincronizarPasarelaPython(sol) {
  fetch('/api/pasarela/sos', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      uuid: sol.uuid,
      rev: sol.rev,
      nivel: sol.nivelActual,
      estado: sol.estado,
      necesidad: sol.necesidad,
      solicitada: sol.solicitada,
      aceptada: sol.aceptada,
      pendiente: sol.pendiente,
      ubicacion: sol.ubicacion,
      lat: sol.lat,
      lon: sol.lon
    })
  }).catch(() => {});
}

function renderPhoneBody() {
  const el = document.getElementById('phoneBody');
  const sol = state.solicitudes[0];
  const infoEstado = ETIQUETAS_ESTADO[sol.estado] || { texto: sol.estado, clase: "info" };

  if (state.tab === 0) {
    const textosBoton = [
      "▶ Paso 1: Masachapa sin suministros → Derivar a San Rafael",
      "▶ Paso 2: San Rafael aporta 4 de 10 kits y deriva 6 → Depto.",
      "▶ Paso 3: Coordinación Depto. deriva los 6 restantes → Managua",
      "▶ Paso 4: Managua envía los 6 kits por ruta Waze y completa (10/10)"
    ];
    el.innerHTML = `
      <div class="card highlight">
        <h3>⚡ Simulación Guiada de Emergencia · San Rafael del Sur</h3>
        <p>Observa cómo se coordina una emergencia real entre <b>Masachapa</b>, <b>San Rafael del Sur</b>, <b>El Crucero</b> y <b>Managua</b> mientras el mapa calcula la mejor ruta:</p>
        <button class="btn-primary" onclick="ejecutarPasoSimulacion()">${textosBoton[state.pasoSimulacion]}</button>
      </div>

      <div class="card">
        <h3>📡 Recepción en Vivo de Alertas SOS desde las Torres</h3>
        <p>Simula la llegada de un mensaje SOS transmitido por radio desde una comunidad hacia cualquiera de las 3 torres repetidoras:</p>
        <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:6px;margin-top:6px;margin-bottom:8px;">
          <button class="btn-primary" style="margin:0;padding:7px 4px;font-size:10.5px;" onclick="recibirSosDesdeTorre(1)">📡 Torre 1<br>Masachapa</button>
          <button class="btn-primary" style="margin:0;padding:7px 4px;font-size:10.5px;" onclick="recibirSosDesdeTorre(2)">📡 Torre 2<br>San Rafael</button>
          <button class="btn-primary" style="margin:0;padding:7px 4px;font-size:10.5px;" onclick="recibirSosDesdeTorre(3)">📡 Torre 3<br>El Crucero</button>
        </div>
        ${state.mensajesTorre.slice(0, 3).map(m => `
          <div style="background:#E1F5FE;border-left:3px solid #0288D1;border-radius:6px;padding:6px 8px;margin-bottom:5px;">
            <div style="font-size:10.5px;font-weight:800;color:#01579B;">📥 ${m.hora} · ${m.torre} (${m.senal})</div>
            <div style="font-size:11px;color:#0D2538;margin-top:2px;">${m.resumen}</div>
          </div>
        `).join('')}
      </div>

      <div class="card">
        <h3>📻 Estado del Radio y Conexiones</h3>
        <p><b>Equipo conectado:</b> Radio Comunitario Masachapa · <b>Batería:</b> 88%</p>
        <p><b>Torres en línea:</b> Masachapa (Costa) ↔ San Rafael del Sur ↔ El Crucero</p>
        <p><b>Modo sin internet:</b> Las solicitudes viajan directamente por radio entre comunidades y alcaldía.</p>
        <div style="display:flex;gap:6px;flex-wrap:wrap;margin-top:9px;">
          <button class="btn-outline" onclick="toggleRadio()">${state.radioVinculado ? 'Desconectar radio' : 'Conectar radio'}</button>
          <button class="btn-outline" onclick="toggleInternet()">${state.internetDisponible ? 'Pasar a modo sin internet' : 'Simular internet activo'}</button>
          <button class="btn-outline" onclick="toggleManagua()">${state.managuaAlcanzable ? 'Simular corte con Managua' : 'Restaurar enlace con Managua'}</button>
        </div>
      </div>

      <div class="card">
        <h3>🛡️ Verificación del Sistema</h3>
        <p>Mensajes enviados: <b>${state.txCount}</b> · Recibidos de torres: <b>${state.rxCount}</b> · Integridad verificada</p>
        <div style="display:flex;gap:6px;margin-top:6px;flex-wrap:wrap;">
          <button class="btn-outline" onclick="probarAckRadioSinAutoridad()">Simular señal de torre repetidora</button>
          <button class="btn-outline" onclick="probarCarga20Sos()">Probar envío de 20 solicitudes</button>
        </div>
      </div>
    `;
  } else if (state.tab === 1) {
    el.innerHTML = `
      <div class="card highlight">
        <h3>🆘 Solicitar Ayuda de Emergencia</h3>
        <p>${state.radioVinculado
          ? '✅ Radio conectado: tu solicitud se enviará de inmediato al puesto de Comarca Masachapa.'
          : '⚠️ Radio desconectado: tu solicitud quedará guardada en el teléfono como Pendiente de envío y se transmitirá automáticamente en cuanto te conectes.'}</p>
      </div>
      <div class="card">
        <label>1. ¿Qué tipo de ayuda necesitas?</label>
        <select id="inNec">
          <option value="Agua potable y alimentos|kits">Agua potable y alimentos (kits)</option>
          <option value="Brigada de rescate y evacuación|brigadas">Brigada de rescate y evacuación (brigadas)</option>
          <option value="Atención médica y traslado|botiquines">Atención médica y traslado (botiquines)</option>
          <option value="Maquinaria para despeje de vía|cuadrillas">Maquinaria para despeje de vía (cuadrillas)</option>
        </select>

        <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
          <div>
            <label>Cantidad necesaria:</label>
            <input id="inCant" type="number" min="1" value="10" />
          </div>
          <div>
            <label>Personas afectadas:</label>
            <input id="inPers" type="number" min="1" value="42" />
          </div>
        </div>

        <label>2. Nivel de urgencia:</label>
        <select id="inUrg">
          <option value="Urgencia crítica · Riesgo vital">Urgencia crítica · Riesgo vital</option>
          <option value="Urgencia alta · Atención pronta">Urgencia alta · Atención pronta</option>
          <option value="Urgencia moderada">Urgencia moderada</option>
        </select>

        <label>3. Ubicación exacta del incidente en San Rafael del Sur:</label>
        <select id="inSitio">
          <option value="Escuela Pública de Comarca Masachapa|11.7809|-86.5193">Comarca Masachapa (Escuela Pública · 11.7809, -86.5193)</option>
          <option value="Centro Comunitario de Pochomil|11.7724|-86.5044">Comarca Pochomil (Centro Comunitario · 11.7724, -86.5044)</option>
          <option value="Comarca El Salto Km 34|11.8920|-86.3880">Comarca El Salto (Bajada La Sierra Km 34 · 11.8920, -86.3880)</option>
          <option value="Cruce Costero Montelimar|11.8125|-86.5110">Comarca Montelimar (Cruce Costero · 11.8125, -86.5110)</option>
        </select>

        <label>4. Nota adicional para los brigadistas:</label>
        <input id="inNota" type="text" value="Familias evacuadas por inundación costera; requieren agua potable y raciones." />

        <button class="btn-primary" style="padding:12px;font-size:13px;" onclick="crearNuevoSos()">ENVIAR SOLICITUD DE AYUDA</button>
      </div>
    `;
  } else if (state.tab === 2) {
    const niveles = ["COMARCA", "MUNICIPIO", "CABECERA_DEPARTAMENTAL", "SEDE_CENTRAL_MANAGUA"];
    const etiquetas = ["1. Masachapa", "2. San Rafael", "3. Depto.", "4. Managua"];
    const idxNivel = niveles.indexOf(sol.nivelActual);

    el.innerHTML = `
      <div class="card">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
          <h3 style="margin:0;">📋 Solicitud ${sol.folio}</h3>
          <span class="badge-state ${infoEstado.clase}">${infoEstado.texto}</span>
        </div>
        <p style="color:#455A64;font-size:11px;margin-top:0;">Atendiendo actualmente en: <b>${NOMBRES_NIVEL[sol.nivelActual]}</b></p>

        <div class="chain-row">
          ${etiquetas.map((et, i) => `
            <div class="chain-step ${i === idxNivel && sol.estado !== 'ATENDIDO' ? 'active' : (i < idxNivel || sol.estado === 'ATENDIDO' ? 'done' : '')}">
              ${et}<br>${i === idxNivel && sol.estado !== 'ATENDIDO' ? 'EN TURNO' : (i < idxNivel || sol.estado === 'ATENDIDO' ? 'LISTO' : 'ESPERA')}
            </div>
          `).join('')}
        </div>

        <div style="background:#F0F9FF;border:1px solid #BAE6FD;border-radius:9px;padding:9px;margin:8px 0;">
          <p style="margin:2px 0;"><b>Ayuda solicitada:</b> ${sol.necesidad} (${sol.personas} personas)</p>
          <p style="margin:2px 0;">
            <b>Pedido total:</b> ${sol.solicitada} ${sol.unidad} ·
            <b style="color:#2E7D32;">Comprometido: ${sol.aceptada} ${sol.unidad}</b> ·
            <b style="color:${sol.pendiente > 0 ? '#D32F2F' : '#2E7D32'};">Falta por cubrir: ${sol.pendiente} ${sol.unidad}</b>
          </p>
          <p style="margin:2px 0;"><b>Lugar:</b> ${sol.ubicacion} (${sol.lat}, ${sol.lon})</p>
          ${sol.estado !== 'ATENDIDO' ? `
            <p style="margin:4px 0 0 0;font-size:11px;color:#01579B;">
              ⏱️ Tiempo para confirmar recepción: <b>${sol.acuse60Recibido ? 'Confirmado' : sol.seg60 + ' s'}</b> ·
              Tiempo para asignar recursos: <b>${sol.seg180} s</b>
            </p>
          ` : ''}
        </div>

        ${sol.asignaciones.length > 0 ? `
          <p style="font-weight:700;margin-top:6px;margin-bottom:3px;color:#01579B;">Recursos ya comprometidos:</p>
          ${sol.asignaciones.map(a => `<p style="color:#1B5E20;font-weight:600;margin:2px 0;">${a}</p>`).join('')}
        ` : ''}

        <div style="display:flex;gap:6px;margin-top:9px;flex-wrap:wrap;">
          <button class="btn-outline" onclick="cambiarEstadoDirecto('EN_CAMINO')">🚑 Marcar En Camino</button>
          <button class="btn-outline" onclick="cambiarEstadoDirecto('ATENDIDO')">✅ Marcar Atendido</button>
        </div>
      </div>

      <div class="card">
        <h3>🕒 Historial de Seguimiento</h3>
        ${sol.historial.map(h => `
          <div style="border-left:3px solid #0288D1;padding-left:9px;margin-bottom:8px;">
            <div style="font-size:11px;font-weight:800;color:#01579B;">${h.hora} · ${h.estado}</div>
            <div style="font-size:10.5px;font-weight:700;color:#455A64;">Por: ${h.autor}</div>
            <div style="font-size:11px;color:#0D2538;margin-top:2px;">${h.detalle}</div>
          </div>
        `).join('')}
      </div>
    `;
  } else if (state.tab === 3) {
    const invNivel = state.inventario.find(i => i.nivel === sol.nivelActual) || state.inventario[1];
    const dispNivel = Math.max(0, invNivel.total - invNivel.reservado);
    const sugerido = Math.min(sol.pendiente, Math.max(1, dispNivel || 4));

    el.innerHTML = `
      <div class="card highlight">
        <h3>🏛️ Coordinación y Atención de Solicitudes</h3>
        <p>Nivel responsable actual: <b>${NOMBRES_NIVEL[sol.nivelActual]}</b></p>
        <p>Falta por cubrir en <b>${sol.folio}</b>: <b style="color:#D32F2F;">${sol.pendiente} de ${sol.solicitada} ${sol.unidad}</b> (${sol.necesidad})</p>
        <p>Disponible en este nivel (${invNivel.nombre}): <b>${dispNivel} ${sol.unidad}</b></p>
        <div style="border-top:1px solid #81D4FA;margin-top:8px;padding-top:7px;">
          <div style="font-size:11px;font-weight:700;color:#01579B;margin-bottom:5px;">📡 Recibir nuevo SOS entrante desde torre:</div>
          <div style="display:flex;gap:5px;">
            <button class="btn-outline" style="flex:1;padding:5px;" onclick="recibirSosDesdeTorre(1)">Torre Masachapa</button>
            <button class="btn-outline" style="flex:1;padding:5px;" onclick="recibirSosDesdeTorre(2)">Torre San Rafael</button>
            <button class="btn-outline" style="flex:1;padding:5px;" onclick="recibirSosDesdeTorre(3)">Torre El Crucero</button>
          </div>
        </div>
      </div>

      <div class="card">
        <h3>1. Confirmar Recepción</h3>
        <p>Notifica a la comunidad que su solicitud ya está siendo revisada por el responsable de turno:</p>
        <button class="btn-primary" onclick="confirmarRecibidoApp()">✓ Confirmar que recibí la solicitud</button>
      </div>

      <div class="card">
        <h3>2. Asignar Recursos de Este Nivel (Total o Parcial)</h3>
        <p>Si solo puedes cubrir una parte (por ejemplo, 4 de 10 kits), el sistema compromete tu aporte y envía automáticamente el resto al siguiente nivel:</p>
        <label>Cantidad a aportar desde ${NOMBRES_NIVEL[sol.nivelActual]}:</label>
        <input id="inCantAceptar" type="number" min="1" max="${sol.pendiente}" value="${sugerido}" />
        <button class="btn-success" onclick="aceptarParcialOTotalManual()">
          Comprometer cantidad y derivar el resto si falta
        </button>
      </div>

      <div class="card">
        <h3>3. Derivar por Falta de Capacidad</h3>
        <label>Motivo por el que se solicita apoyo al siguiente nivel:</label>
        <select id="inMotivoEscalar">
          <option value="Suministros agotados en este puesto">Suministros agotados en este puesto</option>
          <option value="Falta de vehículo o transporte disponible">Falta de vehículo o transporte disponible</option>
          <option value="Paso bloqueado por derrumbe en la vía">Paso bloqueado por derrumbe en la vía</option>
          <option value="Todas las brigadas locales están en otra emergencia">Todas las brigadas locales están en otra emergencia</option>
        </select>
        <button class="btn-warning" onclick="escalarManualSiguienteNivel()">
          ↗ Derivar solicitud al siguiente nivel
        </button>
        <div style="display:flex;gap:6px;margin-top:8px;">
          <button class="btn-outline" style="flex:1;" onclick="vencerPlazo(60)">Simular demora de 60 s</button>
          <button class="btn-outline" style="flex:1;" onclick="vencerPlazo(180)">Simular demora de 180 s</button>
        </div>
      </div>
    `;
  } else {
    el.innerHTML = `
      <div class="card highlight">
        <h3>📦 Bodegas y Recursos Disponibles</h3>
        <p>Inventario actualizado por nivel territorial en el corredor de San Rafael del Sur:</p>
      </div>
      ${state.inventario.map((inv, idx) => `
        <div class="card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;">${inv.nombre}</h3>
            <span class="corridor-pill">${inv.codigo}</span>
          </div>
          <p><b>Nivel:</b> ${NOMBRES_NIVEL[inv.nivel]} · <b>Responsable:</b> ${inv.resp}</p>
          <p>
            <b>Total:</b> ${inv.total} kits ·
            <b>Comprometido:</b> ${inv.reservado} kits ·
            <b style="color:#0288D1;">Disponible: ${inv.total - inv.reservado} kits</b>
          </p>
          <button class="btn-outline" onclick="reabastecerBodega(${idx})">Reabastecer / Liberar stock</button>
        </div>
      `).join('')}
    `;
  }
}

function recibirSosDesdeTorre(numTorre) {
  const ahora = new Date().toLocaleTimeString();
  const folioNum = 105 + state.solicitudes.length;
  const folio = "SOS-0" + folioNum;

  let cfg;
  if (numTorre === 1) {
    cfg = {
      torre: "Torre 1 · Masachapa (!a1b201)",
      senal: "-65 dBm · SNR +10.2 dB",
      nivel: "COMARCA",
      rolIdx: 1,
      necesidad: "Agua potable y alimentos",
      unidad: "kits",
      cant: 8,
      personas: 35,
      ubicacion: "Centro Comunitario de Pochomil",
      lat: 11.7724,
      lon: -86.5044,
      wazeOrig: "NODO_SAN_RAFAEL",
      wazeDest: "NODO_POCHOMIL"
    };
  } else if (numTorre === 2) {
    cfg = {
      torre: "Torre 2 · San Rafael del Sur (!a1b202)",
      senal: "-71 dBm · SNR +8.4 dB",
      nivel: "MUNICIPIO",
      rolIdx: 2,
      necesidad: "Brigada de rescate y evacuación",
      unidad: "brigadas",
      cant: 2,
      personas: 18,
      ubicacion: "Comarca El Salto Km 34",
      lat: 11.8920,
      lon: -86.3880,
      wazeOrig: "NODO_SAN_RAFAEL",
      wazeDest: "NODO_EL_SALTO"
    };
  } else {
    cfg = {
      torre: "Torre 3 · El Crucero (!a1b203)",
      senal: "-76 dBm · SNR +6.8 dB",
      nivel: "CABECERA_DEPARTAMENTAL",
      rolIdx: 3,
      necesidad: "Atención médica y traslado",
      unidad: "botiquines",
      cant: 5,
      personas: 24,
      ubicacion: "Cruce Costero Montelimar",
      lat: 11.8125,
      lon: -86.5110,
      wazeOrig: "NODO_EL_CRUCERO",
      wazeDest: "NODO_MONTELIMAR"
    };
  }

  const nuevo = {
    uuid: crypto.randomUUID ? crypto.randomUUID() : "sos-torre-" + Date.now(),
    folio,
    rev: 1,
    nivelActual: cfg.nivel,
    estado: "ENVIADO_A_RADIO",
    necesidad: cfg.necesidad,
    unidad: cfg.unidad,
    solicitada: cfg.cant,
    aceptada: 0,
    pendiente: cfg.cant,
    urgencia: "Urgencia crítica · Riesgo vital",
    personas: cfg.personas,
    ubicacion: cfg.ubicacion,
    lat: cfg.lat,
    lon: cfg.lon,
    acuse60Recibido: false,
    seg60: 60,
    seg180: 180,
    asignaciones: [],
    historial: [
      {
        hora: ahora,
        estado: `Recibido desde ${cfg.torre}`,
        autor: cfg.torre,
        detalle: `Señal recibida (${cfg.senal}): ${cfg.personas} personas en ${cfg.ubicacion} solicitan ${cfg.cant} ${cfg.unidad} (${cfg.necesidad}).`
      }
    ]
  };

  state.solicitudes.unshift(nuevo);
  state.rxCount++;
  state.rolIdx = cfg.rolIdx;
  state.wazeOrigen = cfg.wazeOrig;
  state.wazeDestino = cfg.wazeDest;
  state.brigadaPct = 0.15;
  state.mensajesTorre.unshift({
    hora: ahora,
    torre: cfg.torre,
    senal: cfg.senal,
    resumen: `${folio} recibido desde ${cfg.ubicacion} (${cfg.cant} ${cfg.unidad} · ${cfg.personas} personas)`
  });

  sincronizarPasarelaPython(nuevo);
  setBanner(`📡 Nuevo ${folio} recibido desde ${cfg.torre} (${cfg.senal}) en ${cfg.ubicacion}.`);
  renderRoleBar();
  renderWazeMap();
  renderPhoneBody();
}

function toggleRadio() {
  state.radioVinculado = !state.radioVinculado;
  updateTopChips();
  const sol = state.solicitudes[0];
  if (state.radioVinculado && sol.estado === "PENDIENTE_TRANSMISION") {
    sol.estado = "ENVIADO_A_RADIO";
    sol.historial.unshift({
      hora: new Date().toLocaleTimeString(),
      estado: "Enviado automáticamente al conectar radio",
      autor: "Transmisor de Radio",
      detalle: "Al reconectar el radio, la solicitud pendiente se envió automáticamente a Comarca Masachapa."
    });
    setBanner("Radio conectado: la solicitud pendiente se envió automáticamente.");
  } else {
    setBanner(state.radioVinculado
      ? "Radio conectado y listo para transmitir."
      : "Radio desconectado: las nuevas solicitudes se guardarán como 'Pendiente de envío'.");
  }
  renderPhoneBody();
}

function toggleInternet() {
  state.internetDisponible = !state.internetDisponible;
  updateTopChips();
  setBanner(state.internetDisponible
    ? "Internet local disponible (la red de radio sigue activa como respaldo)."
    : "Modo sin internet activo: toda la coordinación opera por radio local.");
}

function toggleManagua() {
  state.managuaAlcanzable = !state.managuaAlcanzable;
  updateTopChips();
  setBanner(state.managuaAlcanzable
    ? "Enlace con Sede Central Managua restaurado y sincronizado."
    : "Sin enlace directo con Managua: Masachapa y San Rafael siguen operando localmente.");
}

function probarAckRadioSinAutoridad() {
  const sol = state.solicitudes[0];
  sol.estado = "ACK_TECNICO_RADIO";
  sol.historial.unshift({
    hora: new Date().toLocaleTimeString(),
    estado: "Señal repetida por torre",
    autor: "Torre Masachapa",
    detalle: "La torre de radio recibió la señal, pero aún se espera que el responsable de turno confirme la recepción."
  });
  setBanner("La torre de radio confirmó la señal; esperando confirmación de la autoridad de turno.");
  switchTab(2);
}

function probarCarga20Sos() {
  state.txCount += 20;
  state.rxCount += 20;
  setBanner("Prueba completada: 20 solicitudes transmitidas con éxito y verificadas sin duplicados.");
  renderPhoneBody();
}

function crearNuevoSos() {
  const necRaw = document.getElementById('inNec').value.split('|');
  const cant = Math.max(1, parseInt(document.getElementById('inCant').value || '10', 10));
  const pers = Math.max(1, parseInt(document.getElementById('inPers').value || '30', 10));
  const urg = document.getElementById('inUrg').value;
  const sitioParts = document.getElementById('inSitio').value.split('|');
  const nota = document.getElementById('inNota').value;
  const estadoIni = state.radioVinculado ? "ENVIADO_A_RADIO" : "PENDIENTE_TRANSMISION";
  const folioNum = 105 + state.solicitudes.length;

  const nuevo = {
    uuid: crypto.randomUUID ? crypto.randomUUID() : "sos-srs-" + Date.now(),
    folio: "SOS-0" + folioNum,
    rev: 1,
    nivelActual: "COMARCA",
    estado: estadoIni,
    necesidad: necRaw[0],
    unidad: necRaw[1] || "kits",
    solicitada: cant,
    aceptada: 0,
    pendiente: cant,
    urgencia: urg,
    personas: pers,
    ubicacion: sitioParts[0],
    lat: parseFloat(sitioParts[1]),
    lon: parseFloat(sitioParts[2]),
    acuse60Recibido: false,
    seg60: 60,
    seg180: 180,
    asignaciones: [],
    historial: [
      {
        hora: new Date().toLocaleTimeString(),
        estado: ETIQUETAS_ESTADO[estadoIni].texto,
        autor: ROLES[state.rolIdx].titulo,
        detalle: state.radioVinculado
          ? `Solicitud enviada por radio hacia Comarca Masachapa. Nota: ${nota}`
          : `Guardada en el teléfono como Pendiente de envío (sin radio conectado). Nota: ${nota}`
      }
    ]
  };
  state.solicitudes.unshift(nuevo);
  state.pasoSimulacion = 0;
  state.txCount++;
  sincronizarPasarelaPython(nuevo);
  setBanner(`Solicitud ${nuevo.folio} registrada en ${sitioParts[0]}.`);
  switchTab(2);
}

function confirmarRecibidoApp() {
  const sol = state.solicitudes[0];
  sol.rev++;
  sol.acuse60Recibido = true;
  sol.estado = "RECIBIDO_AUTORIDAD";
  sol.historial.unshift({
    hora: new Date().toLocaleTimeString(),
    estado: "Recibido por la autoridad",
    autor: NOMBRES_NIVEL[sol.nivelActual],
    detalle: "El responsable de turno confirmó la recepción y está evaluando los recursos disponibles."
  });
  setBanner(`Recepción confirmada por ${NOMBRES_NIVEL[sol.nivelActual]}.`);
  sincronizarPasarelaPython(sol);
  switchTab(2);
}

function aceptarParcialOTotalManual() {
  const sol = state.solicitudes[0];
  const cantInput = document.getElementById('inCantAceptar');
  const cant = Math.max(1, Math.min(sol.pendiente, parseInt(cantInput ? cantInput.value : sol.pendiente, 10) || 1));
  const orden = ["COMARCA", "MUNICIPIO", "CABECERA_DEPARTAMENTAL", "SEDE_CENTRAL_MANAGUA"];
  const idx = orden.indexOf(sol.nivelActual);
  const sig = orden[Math.min(orden.length - 1, idx + 1)];

  sol.rev++;
  sol.aceptada += cant;
  sol.pendiente = Math.max(0, sol.solicitada - sol.aceptada);

  const invNivel = state.inventario.find(i => i.nivel === sol.nivelActual);
  if (invNivel) {
    invNivel.reservado = Math.min(invNivel.total, invNivel.reservado + cant);
  }

  sol.asignaciones.push(`✓ ${NOMBRES_NIVEL[sol.nivelActual]}: ${cant} ${sol.unidad} comprometidos`);

  if (sol.pendiente > 0 && sol.nivelActual !== "SEDE_CENTRAL_MANAGUA") {
    const nivelAnterior = NOMBRES_NIVEL[sol.nivelActual];
    sol.estado = "ACEPTADO_PARCIAL";
    sol.nivelActual = sig;
    sol.historial.unshift({
      hora: new Date().toLocaleTimeString(),
      estado: `Atendido parcialmente (${cant} ${sol.unidad}) y derivado (${sol.pendiente} ${sol.unidad})`,
      autor: nivelAnterior,
      detalle: `${nivelAnterior} comprometió ${cant} ${sol.unidad} y derivó los ${sol.pendiente} ${sol.unidad} restantes a ${NOMBRES_NIVEL[sig]}.`
    });
    setBanner(`${nivelAnterior} aportó ${cant} ${sol.unidad} y derivó ${sol.pendiente} ${sol.unidad} a ${NOMBRES_NIVEL[sig]}.`);
  } else {
    sol.estado = "EN_CAMINO";
    sol.historial.unshift({
      hora: new Date().toLocaleTimeString(),
      estado: `Totalidad cubierta (${sol.aceptada}/${sol.solicitada} ${sol.unidad}) · En camino`,
      autor: NOMBRES_NIVEL[sol.nivelActual],
      detalle: `Se completaron los ${sol.solicitada} ${sol.unidad} requeridos. La brigada va en camino por la ruta óptima Waze.`
    });
    setBanner(`¡Recursos completos (${sol.solicitada}/${sol.solicitada})! Brigada en camino hacia ${sol.ubicacion}.`);
  }

  sincronizarPasarelaPython(sol);
  renderWazeMap();
  switchTab(2);
}

function escalarManualSiguienteNivel() {
  const sol = state.solicitudes[0];
  const motivoEl = document.getElementById('inMotivoEscalar');
  const motivo = motivoEl ? motivoEl.value : "Capacidad local insuficiente";
  const orden = ["COMARCA", "MUNICIPIO", "CABECERA_DEPARTAMENTAL", "SEDE_CENTRAL_MANAGUA"];
  const idx = orden.indexOf(sol.nivelActual);
  const nivelOrigen = NOMBRES_NIVEL[sol.nivelActual];
  const sig = orden[Math.min(orden.length - 1, idx + 1)];

  sol.rev++;
  sol.nivelActual = sig;
  sol.estado = "ESCALADO";
  sol.historial.unshift({
    hora: new Date().toLocaleTimeString(),
    estado: `Derivado a ${NOMBRES_NIVEL[sig]}`,
    autor: nivelOrigen,
    detalle: `Motivo: ${motivo}. Se traslada la atención de ${sol.pendiente} ${sol.unidad} pendientes a ${NOMBRES_NIVEL[sig]}.`
  });
  setBanner(`Solicitud derivada a ${NOMBRES_NIVEL[sig]} (${motivo}).`);
  sincronizarPasarelaPython(sol);
  switchTab(2);
}

function cambiarEstadoDirecto(nuevoEstado) {
  const sol = state.solicitudes[0];
  sol.rev++;
  sol.estado = nuevoEstado;
  if (nuevoEstado === "ATENDIDO") {
    sol.aceptada = sol.solicitada;
    sol.pendiente = 0;
    state.brigadaPct = 1.0;
  }
  sol.historial.unshift({
    hora: new Date().toLocaleTimeString(),
    estado: ETIQUETAS_ESTADO[nuevoEstado].texto,
    autor: ROLES[state.rolIdx].titulo,
    detalle: nuevoEstado === "ATENDIDO"
      ? `Entrega verificada y completada en ${sol.ubicacion}.`
      : `La brigada reporta avance en ruta hacia ${sol.ubicacion}.`
  });
  setBanner(`Estado actualizado a: ${ETIQUETAS_ESTADO[nuevoEstado].texto}`);
  sincronizarPasarelaPython(sol);
  renderWazeMap();
  renderPhoneBody();
}

function reabastecerBodega(idx) {
  const inv = state.inventario[idx];
  if (!inv) return;
  inv.reservado = 0;
  setBanner(`Inventario restablecido en ${inv.nombre} (${inv.total} kits disponibles).`);
  renderPhoneBody();
}

function vencerPlazo(seg) {
  const sol = state.solicitudes[0];
  const orden = ["COMARCA", "MUNICIPIO", "CABECERA_DEPARTAMENTAL", "SEDE_CENTRAL_MANAGUA"];
  const idx = orden.indexOf(sol.nivelActual);
  const sig = orden[Math.min(orden.length - 1, idx + 1)];
  sol.rev++;
  sol.nivelActual = sig;
  sol.estado = seg === 60 ? "SIN_CONFIRMACION" : "ESCALADO";
  sol.historial.unshift({
    hora: new Date().toLocaleTimeString(),
    estado: `Derivado automáticamente a ${NOMBRES_NIVEL[sig]}`,
    autor: "Control Automático de Tiempos",
    detalle: seg === 60
      ? `Transcurrieron 60 segundos sin confirmación de recepción. La solicitud pasa automáticamente a ${NOMBRES_NIVEL[sig]}.`
      : `Transcurrieron 180 segundos sin asignación de recursos. La solicitud pasa automáticamente a ${NOMBRES_NIVEL[sig]}.`
  });
  setBanner(`Tiempo de espera (${seg} s) agotado → Derivado automáticamente a ${NOMBRES_NIVEL[sig]}.`);
  sincronizarPasarelaPython(sol);
  switchTab(2);
}

setInterval(() => {
  const ahora = new Date();
  const clockEl = document.getElementById('phoneClock');
  if (clockEl) {
    clockEl.textContent = `${ahora.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})} · Nexo SOS`;
  }
  const sol = state.solicitudes[0];
  if (sol && sol.estado !== "ATENDIDO") {
    if (!sol.acuse60Recibido && sol.seg60 > 0) sol.seg60--;
    if (sol.seg180 > 0) sol.seg180--;
    if (state.tab === 2) renderPhoneBody();
  }
}, 1000);

window.onload = () => {
  renderRoleBar();
  updateTopChips();
  renderPhoneBody();
  initMap();
};
</script>
</body>
</html>
"""


class NexoPcHandler(http.server.BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        pass

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path in ("/", "/index.html"):
            body = HTML_SIMULADOR.encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif parsed.path == "/api/pasarela/status":
            conn = sqlite3.connect(DB_PATH)
            c = conn.cursor()
            c.execute("SELECT COUNT(*) FROM solicitudes_pasarela")
            total = c.fetchone()[0]
            conn.close()
            resp = json.dumps({"ok": True, "sqlite_db": DB_PATH, "registros_persistidos": total}).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(resp)))
            self.end_headers()
            self.wfile.write(resp)
        elif parsed.path == "/descargar-apk":
            apk_candidates = [
                os.path.abspath(os.path.join(BASE_DIR, "..", "NexoSOS-SanRafaelDelSur.apk")),
                os.path.abspath(os.path.join(BASE_DIR, "..", "app", "build", "outputs", "apk", "debug", "app-debug.apk")),
            ]
            apk_path = next((p for p in apk_candidates if os.path.isfile(p)), None)
            if apk_path:
                with open(apk_path, "rb") as f:
                    data = f.read()
                self.send_response(200)
                self.send_header("Content-Type", "application/vnd.android.package-archive")
                self.send_header("Content-Disposition", 'attachment; filename="NexoSOS-SanRafaelDelSur.apk"')
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
            else:
                msg = "El archivo APK se está compilando. Ejecuta COMPILAR_APK.bat o recarga en unos segundos.".encode("utf-8")
                self.send_response(404)
                self.send_header("Content-Type", "text/plain; charset=utf-8")
                self.send_header("Content-Length", str(len(msg)))
                self.end_headers()
                self.wfile.write(msg)
        elif parsed.path.startswith("/vendor/") or parsed.path.startswith("/static/"):
            rel = parsed.path.lstrip("/").replace("/", os.sep)
            file_path = os.path.abspath(os.path.join(BASE_DIR, rel))
            if file_path.startswith(BASE_DIR) and os.path.isfile(file_path):
                ctype, _ = mimetypes.guess_type(file_path)
                with open(file_path, "rb") as f:
                    data = f.read()
                self.send_response(200)
                self.send_header("Content-Type", ctype or "application/octet-stream")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
            else:
                self.send_error(404)
        else:
            self.send_error(404)

    def do_POST(self):
        if self.path == "/api/pasarela/sos":
            length = int(self.headers.get("Content-Length", "0"))
            raw = self.rfile.read(length).decode("utf-8") if length > 0 else "{}"
            data = json.loads(raw)
            res = registrar_trama_sqlite(data)
            resp = json.dumps(res).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(resp)))
            self.end_headers()
            self.wfile.write(resp)
        else:
            self.send_error(404)


if __name__ == "__main__":
    init_db()
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("0.0.0.0", PORT), NexoPcHandler) as httpd:
        print(f"Nexo SOS activo en: http://localhost:{PORT}")
        httpd.serve_forever()
