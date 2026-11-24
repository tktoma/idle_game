@echo off
rem Version 0.296 : lance tous les tests des bots, campagnes comprises (24 profils, puis onze univers depuis zero).
rem Sans les onze univers : run-all-bots.bat court. Comptez plusieurs heures en tout.
chcp 65001 >nul
cd /d "%~dp0"
set ONZE=-PonzeUnivers=true
if "%1"=="court" (
    set ONZE=
    shift
)
echo Lancement de toutes les campagnes des bots...
echo.
call gradlew.bat :bots:test --rerun %ONZE%
if errorlevel 1 (
    echo.
    echo ECHEC : au moins une campagne ne passe pas. Le detail est au-dessus,
    echo et dans le rapport : bots\build\reports\tests\test\index.html
) else (
    echo.
    echo TOUT EST BON : les bots et leurs campagnes passent.
)
echo.
pause
