@echo off
title Nexo SOS - Simulador Android + Pasarela San Rafael del Sur
echo Iniciando Nexo SOS (Android + Meshtastic + Mapa Waze San Rafael del Sur)...
start http://localhost:8080
python "%~dp0pc-simulator\run_pc_demo.py"
pause
