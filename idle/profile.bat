@echo off
rem Version 0.296 : profile le moteur avec Java Flight Recorder, sur une partie du bot analyste.
rem   profile.bat        le premier univers depuis zero
rem   profile.bat 3      jusqu'a trois univers
rem L'enregistrement : bots\sorties\profil\partie.jfr (a ouvrir dans JDK Mission Control).
chcp 65001 >nul
cd /d "%~dp0"
set U=%1
if "%U%"=="" set U=1
if not exist bots\sorties\profil mkdir bots\sorties\profil
if exist bots\sorties\profil\partie.jfr del bots\sorties\profil\partie.jfr
call gradlew.bat :bots:analyste -Punivers=%U% -Petsi=false -Pdossier=sorties/profil -Pjfr=sorties/profil/partie.jfr
echo.
where jfr >nul 2>nul && jfr view --width 160 hot-methods bots\sorties\profil\partie.jfr
echo L'enregistrement : bots\sorties\profil\partie.jfr
pause
