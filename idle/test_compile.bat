@echo off
SET JAVA_HOME=C:\Program Files\Java\jdk-21.0.12
SET PATH=%JAVA_HOME%\bin;%PATH%
cd /d C:\Users\thoma\IdeaProjects\idle
call gradlew compileJava compileTestJava --no-daemon
