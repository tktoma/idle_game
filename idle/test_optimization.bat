@echo off
SET JAVA_HOME=C:\Program Files\Java\jdk-21.0.12
SET PATH=%JAVA_HOME%\bin;%PATH%
cd /d C:\Users\thoma\IdeaProjects\idle
call gradlew compileJava --no-daemon
if errorlevel 1 (
    echo ERROR: Compilation echouee!
    pause
) else (
    echo OK: Compilation reussie!
)
