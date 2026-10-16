# RESILIX-LATAM: Sistema Nodal Autónomo de Detección, Gestión de Rescate y Escalamiento Jerárquico en la Post-Emergencia

> **Rally Latinoamericano de Innovación — Desafío 9: Optimización y priorización de recursos y esfuerzos en la post-emergencia**

Plataforma ciberfísica e integral de respuesta táctica en las **primeras 72 horas críticas post-desastre**, que articula **Torres Sensoriales Autónomas de Ultra-Largo Alcance (15 a 40 km)**, un **Servidor Cloud Distribuido por Nodos** y un **Algoritmo de Escalamiento Jerárquico Territorial** (Comunidad ➔ Municipio ➔ Departamento ➔ Capital/Nacional).

---

## 🎯 Arquitectura de Respuesta en la Post-Emergencia

```
                             [ EVENTO CATASTRÓFICO CESADO ]
                                           │
                                           ▼
                      [ TORRE SENSORIAL DE LARGO ALCANCE (15 - 40 km) ]
            • Barrido térmico MWIR (FLIR Ranger HDC): Detección de víctimas a 16 km
            • Radar AESA (SPEXER 2000): Conteo de sobrevivientes y vías a 18-36 km
            • Telemetría LoRaWAN (50 km): Estado bombas ENACAL, postes ENATREL y botones SOS
                                           │
                                           ▼
                            [ PAQUETE SOS POST-DESASTRE ]
               (JSON/CAP · Life Threat Score · Mapa de Calor Satelital)
                                           │
      ┌────────────────────────┬───────────┴───────────┬────────────────────────┐
      ▼                        ▼                       ▼                        ▼
[ MINSA / SALUD ]       [ CODE / RESCATE ]      [ AGUA POTABLE ]         [ MTI / POLICÍA ]
Alistamiento camas      Despacho de botes y     Corte preventivo bomba   Cierre de puentes
trauma y sueros         brigadas acuáticas      y camiones cisterna      y rutas transitables
                                           │
                                           ▼
                    [ ESCALAMIENTO JERÁRQUICO EN CASCADA ]
            Comunidad  ──>  Municipio  ──>  Departamento  ──>  Capital / Nacional
          (Si el puesto    (Si la alcaldía   (Si el CODE dept.   (SINAPRED Central
           local colapsa)   está rebasada)    requiere apoyo)     activa ejército y aire)
```

---

## 🚀 Puntos Clave de la Solución

1. **Enfoque 100% Post-Emergencia:** Optimiza la toma de decisiones en el momento de mayor incertidumbre (horas 0 a 72 tras el impacto), cuando las comunicaciones terrestres y la electricidad están caídas.
2. **Cobertura Territorial Masiva:** Sensores térmicos criogénicos y radar AESA que cubren de **15 a 40 km de radio por torre**, reduciendo drásticamente la cantidad de infraestructura necesaria.
3. **Distribución Multiactor Especializada:** La alerta no va a un buzón genérico; se canaliza automáticamente a **MINSA** (salud), **COMUPRED/SINAPRED** (rescate), **ENACAL** (agua potable), **ENATREL** (energía) y **MTI** (vías).
4. **Escalamiento Jerárquico Automático:** Temporizador estricto de 15 minutos que escala la solicitud de ayuda al nivel superior si el centro de auxilio local fue destruido o desbordado.

---

## 📄 Documentación Completa

Para conocer todos los detalles técnicos, fórmulas del *Life Threat Score*, parametrización institucional y hardware de largo alcance:
- 📖 Consulta la [Propuesta Técnica Detallada](PROPUESTA_TECNICA.md).
