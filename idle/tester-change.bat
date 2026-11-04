@echo off
rem Lance les tests des modules ou quelque chose a change depuis le dernier passage vert : un double-clic suffit.
rem Gradle compare le code, les tests et les sauvegardes lues par les bots ; un module intact est saute.
rem Pour tout rejouer sans exception : tester-tout.bat
chcp 65001 >nul
cd /d "%~dp0"
echo Lancement des tests de ce qui a change...
echo.
call gradlew.bat test --continue
if errorlevel 1 (
    echo.
    echo ECHEC : au moins un test ne passe pas. Le detail est au-dessus,
    echo et dans le rapport : core\build\reports\tests\test\index.html
) else (
    echo.
    echo TOUT EST BON : les tests relances passent (les modules intacts n'ont pas ete rejoues).
)
echo.
pause
