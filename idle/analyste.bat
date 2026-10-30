@echo off
rem Version 0.161 : le bot analyste. Un double-clic lance une partie de onze univers ; le rapport s'ouvre a la fin.
rem Avec des options : analyste.bat -Punivers=1 -Pdossier=sorties/essai (le dossier se compte depuis idle\bots).
chcp 65001 >nul
cd /d "%~dp0"
echo Le bot analyste joue (onze univers : comptez une a trois heures)...
echo.
call gradlew.bat :bots:analyste %*
if errorlevel 1 (
    echo.
    echo ECHEC : le bot n'a pas pu finir. Le detail est au-dessus.
) else (
    echo.
    echo Fini. Le rapport : bots\sorties\analyste\rapport.html ^(et journal.txt, achats.csv, paliers.csv^).
    if exist bots\sorties\analyste\rapport.html start "" bots\sorties\analyste\rapport.html
)
echo.
pause
