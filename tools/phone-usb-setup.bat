@echo off
title NexVoice - Phone USB setup
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0phone-usb-setup.ps1"
echo.
pause
