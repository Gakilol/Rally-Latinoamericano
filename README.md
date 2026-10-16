# RESILIX-LATAM: Sistema Nodal Autónomo de Detección, Gestión de Rescate y Escalamiento Jerárquico Post-Desastre

> **Rally Latinoamericano de Innovación — Desafío 9: Optimización y priorización de recursos y esfuerzos en la post-emergencia**

Plataforma ciberfísica e integral que articula **Torres Sensoriales Autónomas**, un **Servidor Cloud Distribuido por Nodos** y un **Algoritmo de Escalamiento Jerárquico Territorial** (Comunidad ➔ Municipio ➔ Departamento ➔ Capital) para la gestión del riesgo en sus tres fases: **Pre, Durante y Post-Emergencia**.

---

## 🎯 Arquitectura General

```
           [ FASE PRE ]                [ FASE DURANTE ]               [ FASE POST ]
     • Marea y río ultrasónico       • Registro anegamiento     • Mapa térmico FLIR (personas)
     • Presión barométrica           • Resistencia climática    • Estado de plantas de bombeo
     • Alerta temprana preventiva    • Almacenamiento Edge      • Disparo SOS y Triage
                   │                          │                          │
                   └──────────────────────────┴──────────────────────────┘
                                              ▼
                             [ SERVIDOR CLOUD POR NODOS ]
                  (Telemetría JSON/CAP · Life Threat Score · GIS)
                                              ▼
        ┌───────────────────┬───────────────────┬───────────────────┐
        ▼                   ▼                   ▼                   ▼
   [ HOSPITALES ]        [ CODE ]         [ AGUA POTABLE ]       [ MTI / POLICÍA ]
   Camas de trauma       Lanchas /        Corte preventivo /     Despeje de vías /
   y sueros médicos      rescate bomberos cisternas urgentes     resguardo albergues
                                              ▼
                    [ ESCALAMIENTO JERÁRQUICO EN CASCADA ]
            Comunidad  ──>  Municipio  ──>  Departamento  ──>  Capital / Nacional
```

---

## 🚀 Puntos Clave de la Solución

1. **Monitoreo Continuo (Pre, Durante y Post):** Mide la amenaza antes del impacto, resiste la catástrofe y entrega el mapa térmico de sobrevivientes inmediatamente después.
2. **Distribución Multiactor Especializada:** La alerta no va a un buzón genérico; se divide y orienta a:
   - **Hospitales y Centros de Salud:** Para preparar personal y suministros según víctimas detectadas.
   - **Centros de Operaciones de Emergencia (CODE/COMUPRED):** Para despacho de socorristas y botes.
   - **Plantas de Bombeo de Agua Potable:** Para prevenir la contaminación del agua y corte de motores.
3. **Escalamiento Jerárquico Territorial:** Si el puesto de salud o municipio local está inundado o no responde en 15 minutos, el sistema escala de forma automática al Departamento y a la Capital (SINAPRED Central) para movilizar recursos de mayor envergadura.

---

## 📄 Documentación Completa

Para conocer todos los detalles técnicos, fórmulas algorítmicas, especificaciones de dispositivos físicos y presupuesto:
- 📖 Consulta la [Propuesta Técnica Detallada](PROPUESTA_TECNICA.md).
