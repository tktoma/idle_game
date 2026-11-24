@echo off
rem Version 0.296 : lance les tests de l'interface (JavaFX), et seulement eux : un double-clic suffit.
rem Options transmises a Gradle : test-interface.bat --tests "*HauteurDesVuesTest"
chcp 65001 >nul
cd /d "%~dp0"
echo Lancement des tests de l'interface...
echo.
call gradlew.bat :ui:test --rerun %*
if errorlevel 1 (
    echo.
    echo ECHEC : au moins un test de l'interface ne passe pas. Le detail est au-dessus,
    echo et dans le rapport : ui\build\reports\tests\test\index.html
) else (
    echo.
    echo TOUT EST BON : les tests de l'interface passent.
)
echo.
pause
