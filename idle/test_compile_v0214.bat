@echo off
SET JAVA_HOME=C:\Program Files\Java\jdk-21.0.12
SET PATH=%JAVA_HOME%\bin;%PATH%
cd /d C:\Users\thoma\IdeaProjects\idle

echo "=== Compilation du projet Idle (v0.214) ==="
echo.

echo "Nettoyage..."
gradlew.bat clean --no-daemon > build.log 2>&1

echo "Compilation du module core..."
gradlew.bat :core:compileJava --no-daemon >> build.log 2>&1

if exist %TEMP%\build_temp.txt (
    findstr /C:"error:" %TEMP%\build_temp.txt > compile_errors.txt 2>nul || echo "Aucune erreur de compilation dans le cache"
)

echo.
echo "=== Vérification de la compilation ==="

gradlew.bat :core:compileJava --no-daemon >> build.log 2>&1

findstr /C:"BUILD SUCCESS" build.log >nul 2>&1 && (
    echo [OK] Compilation réussie !
    goto end
)

if exist compile_errors.txt (
    echo [ERREUR] Problèmes de compilation :
    type compile_errors.txt
) else (
    echo.
    tail -50 build.log | find /C "error" >nul 2>&1
    set err_count=%errorlevel%
    if %err_count% gtr 0 echo [ERREUR] Des erreurs de compilation ont été détectées. Voir build.log
)

:end
echo.
echo La compilation est terminée. Voir build.log pour plus de détails.
