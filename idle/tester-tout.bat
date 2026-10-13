@echo off
rem Lance tous les tests du jeu, ceux d'aujourd'hui et ceux qu'on ajoutera : un double-clic suffit.
rem Gradle trouve seul chaque test dans src/test/java de chaque module ; il n'y a rien à mettre à jour ici.
chcp 65001 >nul
cd /d "%~dp0"
echo Lancement de tous les tests...
echo.
call gradlew.bat toutTester --continue
if errorlevel 1 (
    echo.
    echo ECHEC : au moins un test ne passe pas. Le detail est au-dessus,
    echo et dans le rapport : core\build\reports\tests\test\index.html
) else (
    echo.
    echo TOUT EST BON : tous les tests passent.
)
echo.
pause
