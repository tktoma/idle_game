@echo off
rem Version 0.296 : mesure un seul univers avec le bot analyste, depuis le jalon de l'univers d'avant.
rem   measure-univers.bat 5                    l'univers 5, depuis bots\sorties\jalons\apres_univers_4.save
rem   measure-univers.bat 5 -Psans=atom_keep   le meme univers sans une amelioration
rem Les jalons viennent de : gradlew.bat :bots:jalons -Psortie=sorties/jalons -Punivers=11
chcp 65001 >nul
cd /d "%~dp0"
if "%1"=="" (
    echo Usage : measure-univers.bat ^<numero de l'univers, 1 a 11^> [options de l'analyste]
    pause
    exit /b 2
)
set N=%1
shift
set /a AVANT=%N%-1
set DEPART=
if %N% GTR 1 (
    if not exist bots\sorties\jalons\apres_univers_%AVANT%.save (
        echo Il manque le jalon bots\sorties\jalons\apres_univers_%AVANT%.save. Pour ecrire les jalons :
        echo   gradlew.bat :bots:jalons -Psortie=sorties/jalons -Punivers=11
        pause
        exit /b 1
    )
    set DEPART=-Pdepart=sorties/jalons/apres_univers_%AVANT%.save
)
call gradlew.bat :bots:analyste %DEPART% -Punivers=%N% -Petsi=false -Pdossier=sorties/univers-%N% %1 %2 %3 %4
echo.
echo Le rapport : bots\sorties\univers-%N%\rapport.html
pause
