# Sistema Autónomo de Detección, Gestión de Rescate y Alerta Temprana Post-Desastre para Zonas Vulnerables (RESILIX-LATAM)

**Desafío Oficial:** Desafío 9 — Optimización y priorización de recursos y esfuerzos en la post-emergencia  
**Ámbito de Aplicación:** Zonas vulnerables de Latinoamérica (con foco en regiones aisladas, cuencas hídricas y Costa Caribe)  
**Disciplina:** Ingeniería en Computación / Sistemas Distribuidos / Internet de las Cosas (IoT) e Inteligencia Artificial  

---

## 1. Introducción y Justificación

Tras el embate de fenómenos hidrometeorológicos y geológicos extremos (huracanes categoría 4-5, inundaciones súbitas, deslaves y tormentas tropicales recurrentes en Centroamérica y el Caribe), las comunidades periurbanas, rurales e indígenas experimentan un **aislamiento crítico**. 

### Diagnóstico de la Problemática
1. **Colapso de Telecomunicaciones:** Caída inmediata de antenas celulares comerciales y corte de tendido eléctrico, generando un "apagón informativo" durante las primeras 24 a 72 horas (la *Ventana de Supervivencia* o *Golden 72 Hours*).
2. **Asimetría de Recursos:** Carencia de brigadas permanentes de rescate en localidades remotas.
3. **Rescate a Ciegas:** Las autoridades centrales (SINAPRED, Defensa Civil, Comités Municipales COMUPRED, Cruz Roja) se ven desbordadas con reportes fragmentados, duplicados o falsos rumores, enviando cuadrillas a zonas accesibles mientras comunidades bajo el agua permanecen invisibles.

### Propósito del Proyecto
Desarrollar un sistema ciberfísico autónomo que combine **infraestructura sensorial fija (Torres y Faros inteligentes)**, una **plataforma centralizada de toma de decisiones algorítmicas** y una **red de alerta comunitaria en cascada (Mesh SOS)** para garantizar que ninguna vida quede desatendida.

---

## 2. Descripción del Proceso y Arquitectura Tecnológica

El sistema articula tres componentes interconectados bajo estándares de tolerancia a fallos (*Offline-First* y *Delay-Tolerant Networking*):

```
┌────────────────────────────────────────────────────────────────────────┐
│                   A. DETECCIÓN Y MONITOREO EN BORDE                     │
│  Torres Inteligentes / Faros Costeros con Visión Térmica y Presencia   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Telemetría / Coordenadas térmicas
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│              B. PLATAFORMA CENTRALIZADA DE GESTIÓN Y MANDO             │
│   Triage Algorítmico · Optimización de Rutas · Dashboard SINAPRED/COE  │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ Paquetes SOS en Cascada
                                    │
┌───────────────────────────────────┴────────────────────────────────────┐
│              C. RED DE ALERTA SOS COMUNITARIA EN CASCADA                │
│    Comunidad A ──(Mesh/LoRa/SMS)──> Comunidad B ──> Nodo Central       │
└────────────────────────────────────────────────────────────────────────┘
```

---

### A. Detección y Monitoreo con Torres Inteligentes (Edge Computing)

* **Infraestructura Física Resistente:**
  - Red de torres autosuficientes (aprovechamiento de faros costeros, torres de telecomunicación existentes o postes modulares de celosía galvanizada con grado de protección IP67/NEMA 4X contra vientos huracanados).
  - Alimentación autónoma 24/7 mediante celdas fotovoltaicas de alta eficiencia acopladas a bancos de baterías de Litio-Ferrofosfato (LiFePO4) de descarga profunda.
* **Módulo Óptico y Térmico:**
  - Cámaras térmicas de longitud de onda infrarroja (LWIR) montadas en domos giratorios PTZ con escaneo perimetral continuo de 360 grados.
  - **Procesamiento de Imágenes en el Borde (Edge AI):** Modelos de visión artificial (*Tiny-YOLOv8*) ejecutados directamente en microcomputadores industriales en la torre para detectar firmas térmicas humanas vivas (36°C - 37.5°C), diferenciándolas de fuentes de calor inanimadas o ganado.
* **Extensión Anfibia Opcional:** En zonas costeras o fluviales, boyas marinas/fluviales conectadas por radiofrecuencia a la torre para medir marea de tormenta y altura de inundación.

---

### B. Plataforma Centralizada de Gestión y Toma de Decisiones

* **Centro de Control Digital (Dashboard GIS):**
  - Mapeo geoespacial interactivo en tiempo real con integración de capas de riesgo (OpenStreetMap, cuencas hídricas, estado de puentes y carreteras).
  - Unificación de datos provenientes tanto de las torres térmicas como de los mensajes SOS de las comunidades.
* **Algoritmo de Priorización y Triage de Rescate:**
  - El sistema calcula dinámicamente el índice **Life Threat Score (LTS)**:
    $$\text{LTS} = (V_{\text{térmico}} \times 0.40) + (\text{Tiempo}_{\text{aislamiento}} \times 0.25) + (\text{Población}_{\text{vulnerable}} \times 0.20) + (\text{Ruta}_{\text{bloqueada}} \times 0.15)$$
  - Ordena automáticamente las solicitudes de auxilio por orden estricto de riesgo vital.
* **Planificación Logística de Despacho:**
  - Algoritmo de optimización de ruteo con restricciones de capacidad de vehículos (*Vehicle Routing Problem - VRP*).
  - Recomienda si el acceso debe ser por vía terrestre (camión 4x4), fluvial (lancha rápida militar) o puente aéreo (helicóptero).
* **Interoperabilidad Institucional:**
  - Diseñada bajo el estándar internacional **CAP (Common Alerting Protocol)** y **HXL (Humanitarian Exchange Language)**, compatible con los protocolos de Defensa Civil, SINAPRED, alcaldías y organismos internacionales (OCHA, Cruz Roja).

---

### C. Red de Alerta SOS Distribuida (Mesh / Cascada Comunitaria)

Para comunidades ultra-alejadas sin presencia de torres ni brigadas fijas:

* **Mecanismo de Activación Comunitaria:**
  - Terminales comunitarios de emergencia (botones de pánico físicos en escuelas/casas comunales o formularios ultra-ligeros por app móvil sin necesidad de internet).
  - Admite canales de contingencia: SMS comprimido de 64 bytes, ráfagas de radiofrecuencia LoRa (915 MHz) o enlace satelital oportunista.
* **Topología de Retransmisión en Cascada (Store-and-Forward Mesh):**
  - Si la Comunidad A queda incomunicada y no tiene alcance directo al COE central, la señal salta automáticamente a la Comunidad B (la más cercana en línea de vista o por proximidad geográfica).
  - La Comunidad B valida y retransmite hacia la Comunidad C, hasta que el paquete alcanza una torre sensorial o un puesto con conectividad.
  - **Solidaridad Activa:** Alerta a los líderes de comunidades vecinas para organizar primeras respuestas comunitarias (evacuación preventiva a albergues elevados) mientras el equipo especializado se desplaza.

---

## 3. Beneficios Esperados e Impacto

1. **Reducción Drástica del Tiempo de Respuesta:** Pasa de una incertidumbre de 48 horas a un diagnóstico de situación en menos de **30 a 60 minutos** post-evento.
2. **Optimización de Recursos Críticos:** Cero desplazamientos fallidos hacia rutas bloqueadas; despacho de socorristas exactamente a las coordenadas geográficas donde hay firmas térmicas confirmadas.
3. **Inclusión Total de Zonas Históricamente Aisladas:** Ningún caserío o comunidad ribereña queda fuera del radar gracias al protocolo en cascada de bajo costo.
4. **Sostenibilidad Económica y Escalabilidad:** Arquitectura de software libre con hardware modular, replicable en cualquier país de la cuenca del Caribe, Centroamérica y la región andina.
