@echo off
setlocal
cd /d "%~dp0"

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\run-him-path.ps1"
if errorlevel 1 (
  echo Him-Path stopped with an error.
  pause
)
