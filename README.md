# Nexo SOS · Android + Meshtastic (Reto 9 - Rally Latinoamericano de Innovación 2026)

Aplicación Android *offline-first* en **Kotlin + Jetpack Compose** (`minSdk = 26`) conectada a radios **Meshtastic (LoRa)** mediante Bluetooth BLE (o malla simulada rotulada para demostración), diseñada para registrar necesidades verificables y ejecutar la **cadena de escalamiento territorial de cuatro niveles** (`Comarca` $\rightarrow$ `Municipio` $\rightarrow$ `Cabecera Departamental` $\rightarrow$ `Sede Central Managua`) sobre un corredor piloto de **máximo tres torres (`T1`, `T2`, `T3`)**.

---

## Arquitectura Multi-Módulo (Anexo F)

| Módulo | Contenido |
| :--- | :--- |
| [`:domain`](./domain) | Las **15 entidades de negocio** del Anexo E (`Institucion`, `Territorio`, `Usuario`, `Dispositivo`, `Solicitud`, `Necesidad`, `EventoEstado`, `Asignacion`, `Reserva`, `Mensaje`, `Acuse`, `Outbox`, `Vencimiento`, `ReporteVial`, `CentroAtencion`) y el motor determinista [`EscalationEngine`](./domain/src/main/kotlin/ni/edu/rally/nexosos/domain/engine/EscalationEngine.kt). |
| [`:protocol`](./protocol) | Codificador binario compacto [`NexoPacketCodec`](./protocol/src/main/kotlin/ni/edu/rally/nexosos/protocol/NexoPacketCodec.kt) sobre `PRIVATE_APP = 256` con límite estricto $\le 160\text{ bytes}$ y autenticador **HMAC-SHA256 truncado a 16 bytes**. |
| [`:data-local`](./data-local) | Base de datos SQLite/Room [`NexoDatabase`](./data-local/src/main/kotlin/ni/edu/rally/nexosos/datalocal/db/NexoDatabase.kt) y [`NexoLocalRepository`](./data-local/src/main/kotlin/ni/edu/rally/nexosos/datalocal/repository/NexoLocalRepository.kt) con transacciones atómicas (`guardarTransicionYOutboxAtomico`) y semilla pre-provisionada del corredor piloto. |
| [`:transport-mesh`](./transport-mesh) | [`MeshTransportManager`](./transport-mesh/src/main/kotlin/ni/edu/rally/nexosos/transportmesh/MeshTransportManager.kt): escáner/cliente BLE Meshtastic (`UUID 6ba1b218-15a8-461f-9fa8-5dcae273eafd`) + transporte simulado rotulado y medidor de carga de radio (20 SOS / 10 min). |
| [`:transport-ip`](./transport-ip) | [`GatewaySyncManager`](./transport-ip/src/main/kotlin/ni/edu/rally/nexosos/transportip/GatewaySyncManager.kt): sincronización por pasarela IP y respaldo satelital Iridium SBD, simulación de `Malla aislada de Managua` y consola central alterna (`RPO 24h / RTO 4h`). |
| [`:app`](./app) | Interfaz en Jetpack Compose que implementa **RF-01 a RF-09**, [`MeshForegroundService`](./app/src/main/kotlin/ni/edu/rally/nexosos/service/MeshForegroundService.kt) (`connectedDevice`) y el **Asistente de Demostración del Rally (Anexo I y J)**. |

---

## Pantallas y Requisitos Verificables (Anexo D)

1. **RF-01 Inicio y conexión ([`TabInicioYDemoScreen.kt`](./app/src/main/kotlin/ni/edu/rally/nexosos/ui/screens/TabInicioYDemoScreen.kt)):**
   - Separa explícitamente `Internet disponible`, `Radio vinculado` y `Malla Autoridad alcanzable`.
   - Prohibido mostrar `"Enviado"` solo por guardar en SQLite: si no hay radio vinculado, registra `"Pendiente de transmisión"`.
   - Incluye el botón interactivo paso a paso para el **Recorrido de Demostración del Rally (Anexo I)**.
2. **RF-02 Crear SOS y RF-03 Ubicación ([`TabCrearSosScreen.kt`](./app/src/main/kotlin/ni/edu/rally/nexosos/ui/screens/TabCrearSosScreen.kt)):**
   - Selección de necesidad, cantidad, urgencia, personas afectadas y ubicación por GNSS (precisión y antigüedad), mapa local o código de sitio comunitario.
   - Nunca utiliza automáticamente la ubicación de la torre (`T1-T3`) como la ubicación de la víctima.
3. **RF-04 Mis solicitudes y RF-08 Seguimiento y cierre ([`TabMisSolicitudesScreen.kt`](./app/src/main/kotlin/ni/edu/rally/nexosos/ui/screens/TabMisSolicitudesScreen.kt)):**
   - Cola local `Outbox` con tamaño en bytes ($\le 160\text{ B}$), cronología completa de estados mostrando quién confirmó cada paso, relojes de 60 s y 180 s en vivo, y seguimiento (`En camino`, `Atendido`, `Cancelado con motivo`).
4. **RF-05 Bandeja de autoridad ([`TabBandejaAutoridadScreen.kt`](./app/src/main/kotlin/ni/edu/rally/nexosos/ui/screens/TabBandejaAutoridadScreen.kt)):**
   - Activa únicamente la bandeja de decisión del nivel responsable actual (`Comarca`, `Municipio`, `Cabecera Departamental`, `Sede Central Managua`).
   - Soporta emitir `RECIBIDO_AUTORIDAD`, **aceptación parcial** (ej. aceptar 4 de 10 unidades en Municipio y escalar automáticamente 6 a Cabecera Departamental conservando el mismo UUID), declaración de capacidad insuficiente con motivo obligatorio y simulación de vencimientos de 60 s / 180 s.
5. **RF-06 Mapa y accesos, RF-07 Recursos y RF-09 Configuración protegida ([`TabMapaYConfigScreen.kt`](./app/src/main/kotlin/ni/edu/rally/nexosos/ui/screens/TabMapaYConfigScreen.kt)):**
   - Croquis offline del corredor con las 3 torres (`T1`, `T2`, `T3`), puntos de reunión, centros receptores y vías (`Comprobada`, `Candidata`, `Bloqueada`).
   - Reserva de inventario propio por institución impidiendo doble reserva local.
   - Directorio territorial protegido, claves HMAC pre-provisionadas y bitácora de seguridad.

---

## 🚀 Cómo Probar y Ejecutar

### 1. Probar en la PC (Simulador Web Interactivo)
Ejecuta el script de inicio rápido:
```cmd
PROBAR_EN_PC.bat
```
Abre en tu navegador `http://localhost:8080/` para acceder a la consola interactiva con mapa tipo Waze de San Rafael del Sur, inyección de mensajes SOS desde las 3 torres (`T1 Masachapa`, `T2 San Rafael del Sur`, `T3 El Crucero`), simulación de bloqueos viales y descarga directa del APK.

### 2. Compilar el APK Android
Para compilar la aplicación Android nativa de forma automatizada:
```cmd
COMPILAR_APK.bat
```
El archivo binario compilado queda listo en:
- [`NexoSOS-SanRafaelDelSur.apk`](./NexoSOS-SanRafaelDelSur.apk) (16.35 MB, instalable en Android 8.0+ / API 26+).

---

## 📑 Documentación Ejecutiva y Presupuesto

- **Documento Ejecutivo, Presupuesto y Retorno de Inversión (ROI / Payback):**  
  👉 Consulta [`DOCUMENTO_EJECUTIVO_Y_PRESUPUESTO_NEXO_SOS.md`](./DOCUMENTO_EJECUTIVO_Y_PRESUPUESTO_NEXO_SOS.md) con el análisis financiero completo (CAPEX, OPEX, Payback de 2.3 a 8 meses y justificación técnica para el Reto 9).
- **Propuesta Técnica Institucional Original:**  
  👉 Consulta [`PROPUESTA_TECNICA.md`](./PROPUESTA_TECNICA.md) con la parametrización de SINAPRED, COMUPRED, MINSA, ENACAL, ENATREL y MTI.

